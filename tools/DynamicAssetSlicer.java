import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

/**
 * Rebuilds generated high-native asset tiles from source sprite sheets with bleed-safe dynamic bounds.
 *
 * <p>Default mode is dry-run only. It reports planned output paths without writing images.</p>
 *
 * <p>Run from the repository root:</p>
 * <pre>javac tools/DynamicAssetSlicer.java</pre>
 * <pre>java -cp tools DynamicAssetSlicer</pre>
 *
 * <p>Write mode:</p>
 * <pre>java -cp tools DynamicAssetSlicer --write</pre>
 *
 * <p>Optional arguments:</p>
 * <pre>java -cp tools DynamicAssetSlicer --source assets/graphics/source/sheets --output assets/graphics/generated/high_native --cols 5 --rows 5 --tile 251 --trim 2 --write</pre>
 */
public final class DynamicAssetSlicer {
    private static final Path DEFAULT_SOURCE_ROOT = Paths.get("assets", "graphics", "source", "sheets");
    private static final Path DEFAULT_OUTPUT_ROOT = Paths.get("assets", "graphics", "generated", "high_native");
    private static final int DEFAULT_COLUMNS = 5;
    private static final int DEFAULT_ROWS = 5;
    private static final int DEFAULT_OUTPUT_TILE_SIZE = 251;
    private static final int DEFAULT_EDGE_TRIM_PIXELS = 2;

    private DynamicAssetSlicer() { }

    public static void main(String[] args) throws IOException {
        Config config = Config.from(args);
        if (!Files.isDirectory(config.sourceRoot)) {
            throw new IOException("Missing source sheet directory: " + config.sourceRoot.toAbsolutePath());
        }

        List<Path> sheets = findPngSheets(config.sourceRoot);
        System.out.println("DynamicAssetSlicer");
        System.out.println("Mode: " + (config.write ? "write images" : "dry run"));
        System.out.println("Source root: " + config.sourceRoot.toAbsolutePath());
        System.out.println("Output root: " + config.outputRoot.toAbsolutePath());
        System.out.println("Sheets found: " + sheets.size());

        int plannedTiles = 0;
        for (Path sheet : sheets) {
            plannedTiles += sliceSheet(sheet, config);
        }
        System.out.println((config.write ? "Wrote" : "Planned") + " tiles: " + plannedTiles);
    }

    private static List<Path> findPngSheets(Path sourceRoot) throws IOException {
        List<Path> sheets = new ArrayList<>();
        try (Stream<Path> stream = Files.walk(sourceRoot)) {
            stream.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".png"))
                    .sorted(Comparator.naturalOrder())
                    .forEach(sheets::add);
        }
        return sheets;
    }

    private static int sliceSheet(Path sheet, Config config) throws IOException {
        BufferedImage image = ImageIO.read(sheet.toFile());
        if (image == null) {
            System.out.println("Skipping unreadable image: " + sheet);
            return 0;
        }

        String groupName = cleanBaseName(fileBaseName(sheet));
        Path groupOutput = config.outputRoot.resolve(groupName);
        int rawCellWidth = image.getWidth() / config.columns;
        int rawCellHeight = image.getHeight() / config.rows;
        int count = 0;

        for (int row = 0; row < config.rows; row++) {
            for (int col = 0; col < config.columns; col++) {
                int left = col * rawCellWidth;
                int top = row * rawCellHeight;
                int right = (col == config.columns - 1) ? image.getWidth() : (col + 1) * rawCellWidth;
                int bottom = (row == config.rows - 1) ? image.getHeight() : (row + 1) * rawCellHeight;

                Bounds bounds = trimBounds(left, top, right, bottom, config.edgeTrimPixels);
                BufferedImage tile = cropCentered(image, bounds, config.outputTileSize);
                Path output = groupOutput.resolve(groupName + "_r" + (row + 1) + "c" + (col + 1) + ".png");

                if (config.write) {
                    Files.createDirectories(output.getParent());
                    ImageIO.write(tile, "png", output.toFile());
                }
                System.out.println((config.write ? "Wrote " : "Would write ") + output);
                count++;
            }
        }
        return count;
    }

    private static Bounds trimBounds(int left, int top, int right, int bottom, int trim) {
        int safeLeft = Math.min(right - 1, left + Math.max(0, trim));
        int safeTop = Math.min(bottom - 1, top + Math.max(0, trim));
        int safeRight = Math.max(safeLeft + 1, right - Math.max(0, trim));
        int safeBottom = Math.max(safeTop + 1, bottom - Math.max(0, trim));
        return new Bounds(safeLeft, safeTop, safeRight, safeBottom);
    }

    private static BufferedImage cropCentered(BufferedImage source, Bounds bounds, int outputTileSize) {
        BufferedImage crop = source.getSubimage(bounds.left, bounds.top, bounds.width(), bounds.height());
        BufferedImage output = new BufferedImage(outputTileSize, outputTileSize, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = output.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            double scale = Math.min((double) outputTileSize / crop.getWidth(), (double) outputTileSize / crop.getHeight());
            int drawWidth = Math.max(1, (int) Math.round(crop.getWidth() * scale));
            int drawHeight = Math.max(1, (int) Math.round(crop.getHeight() * scale));
            int x = (outputTileSize - drawWidth) / 2;
            int y = (outputTileSize - drawHeight) / 2;
            g.drawImage(crop, x, y, drawWidth, drawHeight, null);
        } finally {
            g.dispose();
        }
        return output;
    }

    private static String fileBaseName(Path path) {
        String name = path.getFileName().toString();
        int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(0, dot) : name;
    }

    private static String cleanBaseName(String name) {
        return name.replaceAll("[^A-Za-z0-9_-]", "_");
    }

    private record Bounds(int left, int top, int right, int bottom) {
        int width() { return right - left; }
        int height() { return bottom - top; }
    }

    private record Config(Path sourceRoot, Path outputRoot, int columns, int rows, int outputTileSize, int edgeTrimPixels, boolean write) {
        static Config from(String[] args) {
            Path sourceRoot = DEFAULT_SOURCE_ROOT;
            Path outputRoot = DEFAULT_OUTPUT_ROOT;
            int columns = DEFAULT_COLUMNS;
            int rows = DEFAULT_ROWS;
            int outputTileSize = DEFAULT_OUTPUT_TILE_SIZE;
            int edgeTrimPixels = DEFAULT_EDGE_TRIM_PIXELS;
            boolean write = false;

            for (int i = 0; i < args.length; i++) {
                String arg = args[i];
                if ("--write".equalsIgnoreCase(arg)) {
                    write = true;
                } else if ("--source".equalsIgnoreCase(arg) && i + 1 < args.length) {
                    sourceRoot = Paths.get(args[++i]);
                } else if ("--output".equalsIgnoreCase(arg) && i + 1 < args.length) {
                    outputRoot = Paths.get(args[++i]);
                } else if ("--cols".equalsIgnoreCase(arg) && i + 1 < args.length) {
                    columns = Integer.parseInt(args[++i]);
                } else if ("--rows".equalsIgnoreCase(arg) && i + 1 < args.length) {
                    rows = Integer.parseInt(args[++i]);
                } else if ("--tile".equalsIgnoreCase(arg) && i + 1 < args.length) {
                    outputTileSize = Integer.parseInt(args[++i]);
                } else if ("--trim".equalsIgnoreCase(arg) && i + 1 < args.length) {
                    edgeTrimPixels = Integer.parseInt(args[++i]);
                }
            }

            if (columns <= 0 || rows <= 0 || outputTileSize <= 0 || edgeTrimPixels < 0) {
                throw new IllegalArgumentException("Invalid slicer configuration.");
            }
            return new Config(sourceRoot, outputRoot, columns, rows, outputTileSize, edgeTrimPixels, write);
        }
    }
}
