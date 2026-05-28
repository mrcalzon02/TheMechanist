from pathlib import Path
from PIL import Image, ImageFilter, ImageEnhance, ImageCms, ImageDraw, ImageFont
from collections import deque
import zipfile
import shutil
import math

SOURCE_PATH = Path("/mnt/data/ChatGPT Image May 13, 2026, 08_47_47 PM (4).png")
OUTPUT_DIR = Path("/mnt/data/java_linux_icon_pack")
ZIP_PATH = Path("/mnt/data/java_linux_icon_pack.zip")

RESOLUTIONS = [512, 256, 128, 64, 48, 32, 16]
PADDING_RATIO = 0.12


def is_background_checker_pixel(px):
    """Detect only the light neutral checkerboard background."""
    r, g, b, a = px
    return (
        a > 0
        and r >= 214 and g >= 214 and b >= 214
        and (max(r, g, b) - min(r, g, b) <= 18)
    )


def remove_border_connected_checkerboard(src):
    """
    Removes the visible checkerboard by flood-filling only background-colored
    pixels connected to the image border. This preserves bright icon details
    inside the frame because they are not connected to the outside background.
    """
    img = src.convert("RGBA")
    w, h = img.size
    pix = img.load()
    visited = bytearray(w * h)
    queue = deque()

    def idx(x, y):
        return y * w + x

    for x in range(w):
        for y in (0, h - 1):
            i = idx(x, y)
            if not visited[i] and is_background_checker_pixel(pix[x, y]):
                visited[i] = 1
                queue.append((x, y))

    for y in range(h):
        for x in (0, w - 1):
            i = idx(x, y)
            if not visited[i] and is_background_checker_pixel(pix[x, y]):
                visited[i] = 1
                queue.append((x, y))

    while queue:
        x, y = queue.popleft()

        for nx, ny in ((x + 1, y), (x - 1, y), (x, y + 1), (x, y - 1)):
            if 0 <= nx < w and 0 <= ny < h:
                i = idx(nx, ny)
                if not visited[i] and is_background_checker_pixel(pix[nx, ny]):
                    visited[i] = 1
                    queue.append((nx, ny))

    data = list(img.getdata())
    cleaned = []
    for i, (r, g, b, a) in enumerate(data):
        if visited[i]:
            cleaned.append((r, g, b, 0))
        else:
            cleaned.append((r, g, b, a))

    img.putdata(cleaned)
    return img


def export_icon_size(src, size, padding_ratio=PADDING_RATIO):
    """
    Crops the transparent master to its visible silhouette, scales it into
    the safe area, and centers it on a transparent square canvas.

    The padding is strict: at least ceil(size * 0.12) pixels on every side.
    """
    bbox = src.getbbox()
    if bbox is None:
        raise RuntimeError("No visible icon content remained after transparency cleanup.")

    obj = src.crop(bbox)

    safe_pad = int(math.ceil(size * padding_ratio))
    max_content = size - (safe_pad * 2)

    scale = min(max_content / obj.width, max_content / obj.height)
    new_w = max(1, min(max_content, int(math.floor(obj.width * scale))))
    new_h = max(1, min(max_content, int(math.floor(obj.height * scale))))

    resized = obj.resize((new_w, new_h), Image.Resampling.LANCZOS)

    if size <= 48:
        alpha = resized.getchannel("A")
        rgb = resized.convert("RGB")

        contrast = 1.18 if size >= 32 else 1.28
        sharpness = 1.35 if size >= 32 else 1.65
        unsharp_percent = 135 if size >= 32 else 180

        rgb = ImageEnhance.Contrast(rgb).enhance(contrast)
        rgb = ImageEnhance.Sharpness(rgb).enhance(sharpness)
        rgb = rgb.filter(
            ImageFilter.UnsharpMask(
                radius=0.6,
                percent=unsharp_percent,
                threshold=2,
            )
        )

        resized = Image.merge("RGBA", (*rgb.split(), alpha))

    canvas = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    x = (size - resized.width) // 2
    y = (size - resized.height) // 2
    canvas.alpha_composite(resized, (x, y))
    return canvas


def make_contact_sheet(icon_paths, dst):
    """Creates a visual verification sheet only; exported icons remain transparent."""
    cell_icon_size = 160
    cell_pad = 16
    label_h = 28
    cols = 4
    font = ImageFont.load_default()

    cells = []

    for path in icon_paths:
        icon = Image.open(path).convert("RGBA")
        resample = Image.Resampling.NEAREST if icon.size[0] <= 48 else Image.Resampling.LANCZOS
        display = icon.resize((cell_icon_size, cell_icon_size), resample)

        cell_w = cell_icon_size + (cell_pad * 2)
        cell_h = cell_icon_size + (cell_pad * 2) + label_h
        cell = Image.new("RGBA", (cell_w, cell_h), (255, 255, 255, 0))

        draw = ImageDraw.Draw(cell)
        checker = 8
        for yy in range(0, cell_h, checker):
            for xx in range(0, cell_w, checker):
                color = (230, 230, 230, 255) if ((xx // checker + yy // checker) % 2 == 0) else (250, 250, 250, 255)
                draw.rectangle((xx, yy, xx + checker - 1, yy + checker - 1), fill=color)

        cell.alpha_composite(display, (cell_pad, cell_pad))
        draw.rectangle((0, cell_h - label_h, cell_w, cell_h), fill=(0, 0, 0, 180))
        draw.text((cell_pad, cell_h - label_h + 8), path.name, fill=(255, 255, 255, 255), font=font)

        cells.append(cell)

    rows = math.ceil(len(cells) / cols)
    sheet = Image.new("RGBA", (cols * cells[0].width, rows * cells[0].height), (255, 255, 255, 0))

    for i, cell in enumerate(cells):
        sheet.alpha_composite(cell, ((i % cols) * cell.width, (i // cols) * cell.height))

    sheet.save(dst)


def main():
    if OUTPUT_DIR.exists():
        shutil.rmtree(OUTPUT_DIR)
    OUTPUT_DIR.mkdir(parents=True, exist_ok=True)

    source = Image.open(SOURCE_PATH).convert("RGBA")
    transparent_master = remove_border_connected_checkerboard(source)

    srgb_profile = ImageCms.ImageCmsProfile(ImageCms.createProfile("sRGB"))
    srgb_bytes = srgb_profile.tobytes()

    export_paths = []

    for size in RESOLUTIONS:
        icon = export_icon_size(transparent_master, size)
        out_path = OUTPUT_DIR / f"icon_{size}.png"
        icon.save(out_path, "PNG", optimize=True, icc_profile=srgb_bytes)
        export_paths.append(out_path)

    make_contact_sheet(export_paths, OUTPUT_DIR / "icon_production_sheet.png")

    readme = """Java/Linux Icon Export Pack

Generated files:
- icon_512.png
- icon_256.png
- icon_128.png
- icon_64.png
- icon_48.png
- icon_32.png
- icon_16.png
- icon_production_sheet.png

Processing notes:
- The light checkerboard reference background was removed and replaced with true alpha transparency.
- Each icon has a strict 12% transparent safety border on every canvas edge.
- PNGs are RGBA and saved with an embedded sRGB ICC profile.
- Downscaling uses Pillow LANCZOS interpolation.
- 48px, 32px, and 16px exports receive mild contrast and sharpening to keep the core skull/screen silhouette readable.
"""
    (OUTPUT_DIR / "README.txt").write_text(readme, encoding="utf-8")

    if ZIP_PATH.exists():
        ZIP_PATH.unlink()

    with zipfile.ZipFile(ZIP_PATH, "w", compression=zipfile.ZIP_DEFLATED) as archive:
        for path in sorted(OUTPUT_DIR.iterdir()):
            archive.write(path, arcname=path.name)


if __name__ == "__main__":
    main()
