package mechanist;

import javax.swing.SwingWorker;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/** Packages editor-created mods as zip archives or, when a verified Steamworks wrapper exists, dispatches Workshop publication off the EDT. */
final class ModDeploymentManager {
    static final String VERSION = "mod-deployment-manager-0.9.10hu";
    private final EditorEventBus bus;

    ModDeploymentManager(EditorEventBus bus) { this.bus = bus; }

    SwingWorker<DeploymentResult, DeploymentProgress> deployAsync(DeploymentRequest request) {
        Objects.requireNonNull(request, "request");
        SwingWorker<DeploymentResult, DeploymentProgress> worker = new SwingWorker<>() {
            @Override protected DeploymentResult doInBackground() throws DeploymentException {
                return deploy(request, progress -> {
                    publish(progress);
                    setProgress(Math.max(0, Math.min(100, progress.percent())));
                });
            }
            @Override protected void process(List<DeploymentProgress> chunks) {
                for (DeploymentProgress p : chunks) if (bus != null) bus.publish(new EditorEvent.DeploymentProgress(p.stage(), p.percent(), p.detail()));
            }
            @Override protected void done() {
                try {
                    DeploymentResult result = get();
                    if (bus != null) bus.publish(new EditorEvent.DeploymentFinished(result.summary(), result.outputPath()));
                } catch (java.util.concurrent.ExecutionException ex) {
                    Throwable cause = ex.getCause() == null ? ex : ex.getCause();
                    if (bus != null) bus.publish(new EditorEvent.DeploymentFailed("Mod deployment failed.", cause.getMessage()));
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    if (bus != null) bus.publish(new EditorEvent.DeploymentFailed("Mod deployment interrupted.", ex.getMessage()));
                }
            }
        };
        worker.execute();
        return worker;
    }

    DeploymentResult deploy(DeploymentRequest request, ProgressSink sink) throws DeploymentException {
        validate(request);
        ProgressSink progress = sink == null ? ProgressSink.ignored() : sink;
        progress.report(new DeploymentProgress("prepare", 3, "Validated mod metadata and selected editor entities."));
        SteamIntegrationStatus steam = SteamWorkshopBridge.detect();
        Path staging = null;
        try {
            staging = Files.createTempDirectory("mechanist-mod-staging-");
            ModManifest manifest = stageMod(request, staging, progress);
            if (request.preferSteamWorkshop() && steam.steamReady()) {
                progress.report(new DeploymentProgress("steam", 64, "Steam runtime and wrapper detected; beginning Workshop update path."));
                return SteamWorkshopBridge.upload(request, manifest, staging, progress);
            }
            if (request.preferSteamWorkshop()) {
                progress.report(new DeploymentProgress("fallback", 67, "Steam Workshop route declined: " + steam.humanSummary() + "; writing zip archive instead."));
            }
            DeploymentResult zip = writeZipArchive(request, staging, manifest, progress);
            progress.report(new DeploymentProgress("complete", 100, zip.summary()));
            return zip;
        } catch (IOException ex) {
            throw new DeploymentException("Could not stage or archive mod: " + ex.getMessage(), ex);
        } finally {
            if (staging != null) deleteTreeQuietly(staging);
        }
    }

    private static void validate(DeploymentRequest request) throws DeploymentException {
        if (request.metadata().name().isBlank()) throw new DeploymentException("Mod name is required.");
        if (request.metadata().version().isBlank()) throw new DeploymentException("Mod version is required.");
        if (request.entities().isEmpty()) throw new DeploymentException("At least one editor entity must be bound into the mod package scope.");
        if (request.zipDestination() == null && !request.preferSteamWorkshop()) throw new DeploymentException("A zip destination is required for fallback packaging.");
    }

    private static ModManifest stageMod(DeploymentRequest request, Path staging, ProgressSink progress) throws IOException, DeploymentException {
        Path content = staging.resolve("content");
        Files.createDirectories(content.resolve("data"));
        Files.createDirectories(content.resolve("docs"));
        Files.writeString(content.resolve("docs/README.txt"), readmeFor(request), StandardCharsets.UTF_8);
        Map<String, String> stagedFiles = new LinkedHashMap<>();
        int index = 0;
        for (SimulationEditorRepository.ScopedEntity entity : request.entities()) {
            String folder = SimulationEditorRepository.slug(entity.editorName());
            Path out = content.resolve("data").resolve(folder).resolve(SimulationEditorRepository.slug(entity.id()) + ".json");
            Files.createDirectories(out.getParent());
            Files.writeString(out, entityJson(entity), StandardCharsets.UTF_8);
            stagedFiles.put(content.relativize(out).toString().replace('\\', '/'), sha256Hex(out));
            int pct = 8 + (int)Math.round((Math.min(1.0, (++index) / (double)Math.max(1, request.entities().size()))) * 34.0);
            progress.report(new DeploymentProgress("stage", pct, "Wrote " + entity.editorName() + " entity " + entity.name() + "."));
        }
        Path readme = content.resolve("docs/README.txt");
        stagedFiles.put(content.relativize(readme).toString().replace('\\', '/'), sha256Hex(readme));
        ModManifest manifest = new ModManifest(
                request.metadata().name(), request.metadata().version(), request.metadata().author(), request.metadata().description(),
                request.metadata().tags(), request.metadata().dependencies(), Instant.now().toString(), request.entities().size(), stagedFiles);
        Path manifestPath = content.resolve("manifest.json");
        Files.writeString(manifestPath, manifest.toJson(), StandardCharsets.UTF_8);
        progress.report(new DeploymentProgress("manifest", 52, "Generated manifest.json with " + stagedFiles.size() + " checksummed content files."));
        return manifest;
    }

    private static DeploymentResult writeZipArchive(DeploymentRequest request, Path staging, ModManifest manifest, ProgressSink progress) throws IOException, DeploymentException {
        Path content = staging.resolve("content");
        Path manifestPath = content.resolve("manifest.json");
        Files.writeString(manifestPath, manifest.toJson(), StandardCharsets.UTF_8);
        Path destination = request.zipDestination();
        if (destination == null) throw new DeploymentException("Zip destination was not selected.");
        Path destParent = destination.toAbsolutePath().getParent();
        if (destParent != null) Files.createDirectories(destParent);
        List<Path> files = listRegularFiles(content);
        try (OutputStream fileOut = Files.newOutputStream(destination);
             BufferedOutputStream buffered = new BufferedOutputStream(fileOut);
             ZipOutputStream zip = new ZipOutputStream(buffered, StandardCharsets.UTF_8)) {
            int i = 0;
            for (Path file : files) {
                String name = content.relativize(file).toString().replace('\\', '/');
                ZipEntry entry = new ZipEntry(name);
                entry.setTime(Files.getLastModifiedTime(file).toMillis());
                zip.putNextEntry(entry);
                Files.copy(file, zip);
                zip.closeEntry();
                int pct = 70 + (int)Math.round((++i / (double)Math.max(1, files.size())) * 25.0);
                progress.report(new DeploymentProgress("zip", pct, "Archived " + name + "."));
            }
        }
        long bytes = Files.size(destination);
        return new DeploymentResult(DeploymentMode.ZIP_ARCHIVE, destination, "Wrote mod archive " + destination.toAbsolutePath() + " (" + bytes + " bytes).", false, 0L);
    }

    private static List<Path> listRegularFiles(Path root) throws IOException {
        ArrayList<Path> files = new ArrayList<>();
        try (java.util.stream.Stream<Path> stream = Files.walk(root)) {
            stream.filter(Files::isRegularFile).sorted(Comparator.comparing(Path::toString)).forEach(files::add);
        }
        return files;
    }

    private static String readmeFor(DeploymentRequest request) {
        return request.metadata().name() + "\n"
                + "Version: " + request.metadata().version() + "\n"
                + "Author: " + request.metadata().author() + "\n\n"
                + request.metadata().description() + "\n\n"
                + "This folder was generated by The Mechanist Simulation Editor Suite.\n"
                + "Entity count: " + request.entities().size() + "\n";
    }

    private static String entityJson(SimulationEditorRepository.ScopedEntity entity) {
        StringBuilder out = new StringBuilder(512);
        out.append("{\n");
        jsonPair(out, "editor", entity.editorName(), true);
        jsonPair(out, "id", entity.id(), true);
        jsonPair(out, "name", entity.name(), true);
        out.append("  \"properties\": {\n");
        int i = 0;
        for (Map.Entry<String, Object> e : entity.properties().entrySet()) {
            out.append("    \"").append(jsonEscape(e.getKey())).append("\": ").append(jsonValue(e.getValue()));
            if (++i < entity.properties().size()) out.append(',');
            out.append('\n');
        }
        out.append("  }\n");
        out.append("}\n");
        return out.toString();
    }

    private static void jsonPair(StringBuilder out, String key, String value, boolean comma) {
        out.append("  \"").append(jsonEscape(key)).append("\": \"").append(jsonEscape(value)).append('"');
        if (comma) out.append(',');
        out.append('\n');
    }

    static String jsonValue(Object value) {
        if (value == null) return "null";
        if (value instanceof Boolean b) return String.valueOf(b);
        if (value instanceof Number n) return String.valueOf(n);
        return "\"" + jsonEscape(String.valueOf(value)) + "\"";
    }

    static String jsonArray(List<String> values) {
        StringBuilder out = new StringBuilder("[");
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) out.append(", ");
            out.append("\"").append(jsonEscape(values.get(i))).append("\"");
        }
        return out.append(']').toString();
    }

    static String jsonEscape(String value) {
        if (value == null) return "";
        StringBuilder out = new StringBuilder(value.length() + 16);
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '\\' -> out.append("\\\\");
                case '"' -> out.append("\\\"");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (c < 32) out.append(String.format(Locale.ROOT, "\\u%04x", (int)c));
                    else out.append(c);
                }
            }
        }
        return out.toString();
    }

    static String sha256Hex(Path file) throws IOException, DeploymentException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (DigestInputStream in = new DigestInputStream(Files.newInputStream(file), digest)) {
                byte[] buffer = new byte[8192];
                while (in.read(buffer) >= 0) { }
            }
            byte[] bytes = digest.digest();
            StringBuilder out = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) out.append(String.format(Locale.ROOT, "%02x", b & 0xff));
            return out.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new DeploymentException("SHA-256 is unavailable in this JVM.", ex);
        }
    }

    private static void deleteTreeQuietly(Path root) {
        if (root == null || !Files.exists(root)) return;
        try {
            Files.walkFileTree(root, new SimpleFileVisitor<>() {
                @Override public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException { Files.deleteIfExists(file); return FileVisitResult.CONTINUE; }
                @Override public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException { Files.deleteIfExists(dir); return FileVisitResult.CONTINUE; }
            });
        } catch (IOException ignored) { }
    }

    static SteamIntegrationStatus steamStatus() { return SteamWorkshopBridge.detect(); }

    enum DeploymentMode { ZIP_ARCHIVE, STEAM_WORKSHOP }
    enum SteamPublicationMode { CREATE_NEW_ITEM, UPDATE_EXISTING_ITEM }

    @FunctionalInterface interface ProgressSink {
        void report(DeploymentProgress progress) throws DeploymentException;
        static ProgressSink ignored() { return progress -> { }; }
    }

    record DeploymentProgress(String stage, int percent, String detail) { }
    record DeploymentResult(DeploymentMode mode, Path outputPath, String summary, boolean workshopLegalAgreementRequired, long publishedFileId) { }

    record DeploymentRequest(SimulationEditorRepository.ModMetadata metadata,
                             List<SimulationEditorRepository.ScopedEntity> entities,
                             Path zipDestination,
                             boolean preferSteamWorkshop,
                             SteamPublicationMode steamMode,
                             long steamAppId,
                             long publishedFileId,
                             Path previewImage,
                             String changeNote) {
        DeploymentRequest {
            metadata = Objects.requireNonNull(metadata, "metadata");
            entities = Collections.unmodifiableList(new ArrayList<>(Objects.requireNonNull(entities, "entities")));
            steamMode = steamMode == null ? SteamPublicationMode.CREATE_NEW_ITEM : steamMode;
            steamAppId = Math.max(0L, steamAppId);
            publishedFileId = Math.max(0L, publishedFileId);
            changeNote = changeNote == null ? "Mechanist editor export." : changeNote.trim();
        }
    }

    record ModManifest(String name,
                       String version,
                       String author,
                       String description,
                       List<String> tags,
                       List<String> dependencies,
                       String generatedAt,
                       int entityCount,
                       Map<String, String> checksums) {
        ModManifest {
            tags = Collections.unmodifiableList(new ArrayList<>(tags == null ? List.of() : tags));
            dependencies = Collections.unmodifiableList(new ArrayList<>(dependencies == null ? List.of() : dependencies));
            checksums = Collections.unmodifiableMap(new LinkedHashMap<>(checksums == null ? Map.of() : checksums));
        }
        ModManifest withAdditionalFile(String path, String checksum) {
            LinkedHashMap<String, String> next = new LinkedHashMap<>(checksums);
            next.put(path, checksum);
            return new ModManifest(name, version, author, description, tags, dependencies, generatedAt, entityCount, next);
        }
        String toJson() {
            StringBuilder out = new StringBuilder(1024);
            out.append("{\n");
            jsonPair(out, "name", name, true);
            jsonPair(out, "version", version, true);
            jsonPair(out, "author", author, true);
            jsonPair(out, "description", description, true);
            out.append("  \"tags\": ").append(jsonArray(tags)).append(",\n");
            out.append("  \"dependencies\": ").append(jsonArray(dependencies)).append(",\n");
            jsonPair(out, "generatedAt", generatedAt, true);
            out.append("  \"entityCount\": ").append(entityCount).append(",\n");
            out.append("  \"checksums\": {\n");
            int i = 0;
            for (Map.Entry<String, String> e : checksums.entrySet()) {
                out.append("    \"").append(jsonEscape(e.getKey())).append("\": \"").append(jsonEscape(e.getValue())).append("\"");
                if (++i < checksums.size()) out.append(',');
                out.append('\n');
            }
            out.append("  }\n}");
            return out.toString();
        }
    }

    record SteamIntegrationStatus(boolean steamLaunchEnvironment,
                                  boolean wrapperAvailable,
                                  boolean steamClientRunning,
                                  boolean ugcClassAvailable,
                                  String detail) {
        boolean steamReady() { return steamLaunchEnvironment && wrapperAvailable && steamClientRunning && ugcClassAvailable; }
        String humanSummary() {
            return "env=" + steamLaunchEnvironment + " wrapper=" + wrapperAvailable + " client=" + steamClientRunning + " ugc=" + ugcClassAvailable + " detail=" + detail;
        }
    }

    static final class DeploymentException extends Exception {
        DeploymentException(String message) { super(message); }
        DeploymentException(String message, Throwable cause) { super(message, cause); }
    }

    private static final class SteamWorkshopBridge {
        private SteamWorkshopBridge() { }

        static SteamIntegrationStatus detect() {
            boolean steamEnv = System.getenv("SteamAppId") != null || System.getenv("SteamGameId") != null || Files.exists(Path.of("steam_appid.txt"));
            Class<?> api = findClass("com.codedisaster.steamworks.SteamAPI").orElse(null);
            boolean wrapper = api != null;
            boolean running = false;
            String detail = "Steam wrapper absent.";
            if (api != null) {
                detail = "SteamAPI class present.";
                try {
                    Method isSteamRunning = api.getMethod("isSteamRunning");
                    Object value = isSteamRunning.invoke(null);
                    running = value instanceof Boolean b && b;
                    detail = "SteamAPI.isSteamRunning=" + running;
                } catch (ReflectiveOperationException | LinkageError ex) {
                    detail = "SteamAPI present but runtime query failed: " + ex.getClass().getSimpleName() + ": " + ex.getMessage();
                }
            }
            boolean ugc = findClass("com.codedisaster.steamworks.SteamUGC").isPresent();
            return new SteamIntegrationStatus(steamEnv, wrapper, running, ugc, detail);
        }

        static DeploymentResult upload(DeploymentRequest request, ModManifest manifest, Path staging, ProgressSink progress) throws DeploymentException {
            try {
                Class<?> steamApi = Class.forName("com.codedisaster.steamworks.SteamAPI");
                Method isSteamRunning = steamApi.getMethod("isSteamRunning");
                if (!(Boolean)isSteamRunning.invoke(null)) throw new DeploymentException("Steam API wrapper is present but Steam client is not running.");
                progress.report(new DeploymentProgress("steam", 70, "Steam client is running; resolving SteamUGC callback interface."));
                Class<?> ugcClass = Class.forName("com.codedisaster.steamworks.SteamUGC");
                Class<?> callbackClass = Class.forName("com.codedisaster.steamworks.SteamUGCCallback");
                SteamUploadCallbackState callbackState = new SteamUploadCallbackState();
                Object callbackProxy = Proxy.newProxyInstance(callbackClass.getClassLoader(), new Class<?>[]{callbackClass}, callbackState);
                Object ugc = instantiateUgc(ugcClass, callbackClass, callbackProxy);
                long publishedId = request.publishedFileId();
                if (request.steamMode() == SteamPublicationMode.CREATE_NEW_ITEM || publishedId <= 0L) {
                    progress.report(new DeploymentProgress("steam", 74, "Requesting new Workshop item handle from SteamUGC."));
                    invokeCompatible(ugc, "createItem", request.steamAppId(), workshopFileTypeCommunity());
                    pumpSteamCallbacks(steamApi, callbackState, 20, TimeUnit.SECONDS, progress);
                    publishedId = callbackState.publishedFileId > 0L ? callbackState.publishedFileId : publishedId;
                    if (publishedId <= 0L) throw new DeploymentException("Steam CreateItem callback did not return a published file id within the callback window.");
                }
                progress.report(new DeploymentProgress("steam", 80, "Starting Workshop item update for published file " + publishedId + "."));
                Object updateHandle = invokeCompatible(ugc, "startItemUpdate", request.steamAppId(), publishedId);
                invokeCompatible(ugc, "setItemTitle", updateHandle, request.metadata().name());
                invokeCompatible(ugc, "setItemDescription", updateHandle, request.metadata().description());
                invokeCompatible(ugc, "setItemMetadata", updateHandle, manifest.toJson());
                tryInvokeCompatible(ugc, "setItemTags", updateHandle, request.metadata().tags());
                invokeCompatible(ugc, "setItemContent", updateHandle, staging.resolve("content").toAbsolutePath().toString());
                if (request.previewImage() != null && Files.isRegularFile(request.previewImage())) invokeCompatible(ugc, "setItemPreview", updateHandle, request.previewImage().toAbsolutePath().toString());
                progress.report(new DeploymentProgress("steam", 90, "Submitting Workshop item update to SteamUGC."));
                invokeCompatible(ugc, "submitItemUpdate", updateHandle, request.changeNote());
                pumpSteamCallbacks(steamApi, callbackState, 60, TimeUnit.SECONDS, progress);
                disposeQuietly(ugc);
                return new DeploymentResult(DeploymentMode.STEAM_WORKSHOP, staging.resolve("content"), "Steam Workshop upload submitted for published file " + publishedId + ".", callbackState.legalAgreementRequired, publishedId);
            } catch (DeploymentException ex) {
                throw ex;
            } catch (ReflectiveOperationException | LinkageError ex) {
                throw new DeploymentException("Steam Workshop publication path could not complete: " + ex.getMessage(), ex);
            }
        }

        private static Object instantiateUgc(Class<?> ugcClass, Class<?> callbackClass, Object callbackProxy) throws ReflectiveOperationException, DeploymentException {
            for (Constructor<?> ctor : ugcClass.getConstructors()) {
                Class<?>[] types = ctor.getParameterTypes();
                if (types.length == 1 && types[0].isAssignableFrom(callbackClass)) return ctor.newInstance(callbackProxy);
            }
            throw new DeploymentException("SteamUGC constructor accepting SteamUGCCallback was not found.");
        }

        private static Object workshopFileTypeCommunity() {
            Optional<Class<?>> enumClass = findClass("com.codedisaster.steamworks.SteamRemoteStorage$WorkshopFileType");
            if (enumClass.isPresent() && enumClass.get().isEnum()) {
                Object[] constants = enumClass.get().getEnumConstants();
                for (Object c : constants) if (String.valueOf(c).toLowerCase(Locale.ROOT).contains("community")) return c;
                if (constants.length > 0) return constants[0];
            }
            return 0;
        }

        private static boolean tryInvokeCompatible(Object target, String methodName, Object... args) throws ReflectiveOperationException {
            try { invokeCompatible(target, methodName, args); return true; }
            catch (DeploymentException ex) { return false; }
        }

        private static Object invokeCompatible(Object target, String methodName, Object... args) throws ReflectiveOperationException, DeploymentException {
            Method selected = null;
            for (Method m : target.getClass().getMethods()) {
                if (!m.getName().equals(methodName) || m.getParameterCount() != args.length) continue;
                if (compatible(m.getParameterTypes(), args)) { selected = m; break; }
            }
            if (selected == null) throw new DeploymentException("SteamUGC method not found or incompatible: " + methodName + " args=" + args.length);
            Object[] converted = convertArgs(selected.getParameterTypes(), args);
            return selected.invoke(target, converted);
        }

        private static boolean compatible(Class<?>[] types, Object[] args) {
            for (int i = 0; i < types.length; i++) {
                if (args[i] == null) continue;
                Class<?> type = types[i];
                Object arg = args[i];
                if (type.isPrimitive() && arg instanceof Number) continue;
                if (type == boolean.class && arg instanceof Boolean) continue;
                if (type.isArray() && type.getComponentType() == String.class && arg instanceof List<?>) continue;
                if (type.isInstance(arg)) continue;
                if (type.isEnum() && arg.getClass().isEnum()) continue;
                return false;
            }
            return true;
        }

        private static Object[] convertArgs(Class<?>[] types, Object[] args) {
            Object[] out = new Object[args.length];
            for (int i = 0; i < args.length; i++) {
                Object arg = args[i];
                Class<?> type = types[i];
                if (type == int.class || type == Integer.class) out[i] = arg instanceof Number n ? n.intValue() : arg;
                else if (type == long.class || type == Long.class) out[i] = arg instanceof Number n ? n.longValue() : arg;
                else if (type == short.class || type == Short.class) out[i] = arg instanceof Number n ? n.shortValue() : arg;
                else if (type.isArray() && type.getComponentType() == String.class && arg instanceof List<?> list) out[i] = list.stream().map(String::valueOf).toArray(String[]::new);
                else out[i] = arg;
            }
            return out;
        }

        private static void pumpSteamCallbacks(Class<?> steamApi, SteamUploadCallbackState callbackState, long timeout, TimeUnit unit, ProgressSink progress) throws ReflectiveOperationException, DeploymentException {
            Method runCallbacks = steamApi.getMethod("runCallbacks");
            long end = System.nanoTime() + unit.toNanos(timeout);
            int tick = 0;
            while (System.nanoTime() < end && !callbackState.finished.getCountReached()) {
                runCallbacks.invoke(null);
                if (++tick % 10 == 0) progress.report(new DeploymentProgress("steam-callback", Math.min(96, 90 + tick / 20), "Waiting for Steam callback loop."));
                try { Thread.sleep(50L); } catch (InterruptedException ex) { Thread.currentThread().interrupt(); throw new DeploymentException("Interrupted while waiting for Steam callbacks.", ex); }
            }
        }

        private static void disposeQuietly(Object interfaceInstance) {
            if (interfaceInstance == null) return;
            try {
                Method dispose = interfaceInstance.getClass().getMethod("dispose");
                dispose.invoke(interfaceInstance);
            } catch (ReflectiveOperationException | LinkageError ignored) { }
        }

        private static Optional<Class<?>> findClass(String name) {
            try { return Optional.of(Class.forName(name)); }
            catch (ClassNotFoundException | LinkageError ex) { return Optional.empty(); }
        }
    }

    private static final class SteamUploadCallbackState implements InvocationHandler {
        final CountingLatch finished = new CountingLatch(1);
        volatile long publishedFileId = 0L;
        volatile boolean legalAgreementRequired = false;

        @Override public Object invoke(Object proxy, Method method, Object[] args) {
            String name = method.getName().toLowerCase(Locale.ROOT);
            if (name.contains("createitem") || name.contains("submititemupdate")) {
                if (args != null) {
                    for (Object arg : args) inspectCallbackPayload(arg);
                }
                finished.countDown();
            }
            return defaultReturn(method.getReturnType());
        }

        private void inspectCallbackPayload(Object payload) {
            if (payload == null) return;
            for (Method m : payload.getClass().getMethods()) {
                if (m.getParameterCount() != 0) continue;
                try {
                    String n = m.getName().toLowerCase(Locale.ROOT);
                    Object value = m.invoke(payload);
                    if (n.contains("publishedfileid") && value instanceof Number num) publishedFileId = num.longValue();
                    if (n.contains("legal") && value instanceof Boolean b) legalAgreementRequired = b;
                } catch (ReflectiveOperationException | LinkageError ignored) { }
            }
        }

        private Object defaultReturn(Class<?> type) {
            if (type == Void.TYPE) return null;
            if (type == Boolean.TYPE) return false;
            if (type == Integer.TYPE) return 0;
            if (type == Long.TYPE) return 0L;
            if (type == Float.TYPE) return 0f;
            if (type == Double.TYPE) return 0d;
            return null;
        }
    }

    private static final class CountingLatch {
        private final CountDownLatch latch;
        CountingLatch(int count) { latch = new CountDownLatch(count); }
        void countDown() { latch.countDown(); }
        boolean getCountReached() { return latch.getCount() == 0; }
    }
}
