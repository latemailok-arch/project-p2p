#!/usr/bin/env python3
"""
Generate PingChat launcher icons for Android app.
Creates icons with purple background (#7C3AED) and white "P" character.
"""

from PIL import Image, ImageDraw, ImageFont
import os

# Configuration
PURPLE_BG = "#7C3AED"
WHITE_FG = "#FFFFFF"

# Icon sizes for different density buckets
ICON_SIZES = {
    "mdpi": 48,
    "hdpi": 72,
    "xhdpi": 96,
    "xxhdpi": 144,
    "xxxhdpi": 192,
}

# Adaptive icon sizes (108x108 with 18dp safe zone)
ADAPTIVE_SIZES = {
    "mdpi": 108,
    "hdpi": 162,
    "xhdpi": 216,
    "xxhdpi": 324,
    "xxxhdpi": 432,
}

def create_icon_with_p(size, bg_color, fg_color, is_adaptive=False):
    """Create an icon with centered 'P' character."""
    # Create image with background
    img = Image.new('RGBA', (size, size), bg_color)
    draw = ImageDraw.Draw(img)

    # Calculate font size (approximately 60% of icon size for good proportions)
    font_size = int(size * 0.6)

    # Try to use a bold system font
    font = None
    font_paths = [
        "C:\\Windows\\Fonts\\arialbd.ttf",  # Arial Bold
        "C:\\Windows\\Fonts\\calibrib.ttf",  # Calibri Bold
        "C:\\Windows\\Fonts\\segoeuib.ttf",  # Segoe UI Bold
        "/System/Library/Fonts/Helvetica.ttc",  # macOS
        "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf",  # Linux
    ]

    for font_path in font_paths:
        if os.path.exists(font_path):
            try:
                font = ImageFont.truetype(font_path, font_size)
                break
            except:
                continue

    if font is None:
        # Fallback to default font
        font = ImageFont.load_default()

    # Get text bounding box to center it
    text = "P"
    bbox = draw.textbbox((0, 0), text, font=font)
    text_width = bbox[2] - bbox[0]
    text_height = bbox[3] - bbox[1]

    # Calculate position to center the text
    x = (size - text_width) // 2 - bbox[0]
    y = (size - text_height) // 2 - bbox[1]

    # Draw the text
    draw.text((x, y), text, fill=fg_color, font=font)

    return img

def create_solid_background(size, color):
    """Create a solid color background for adaptive icons."""
    img = Image.new('RGBA', (size, size), color)
    return img

def create_foreground_p(size, color):
    """Create just the 'P' character on transparent background for adaptive icons."""
    img = Image.new('RGBA', (size, size), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)

    # Calculate font size
    font_size = int(size * 0.4)  # Slightly smaller for adaptive icons

    # Try to use a bold system font
    font = None
    font_paths = [
        "C:\\Windows\\Fonts\\arialbd.ttf",
        "C:\\Windows\\Fonts\\calibrib.ttf",
        "C:\\Windows\\Fonts\\segoeuib.ttf",
        "/System/Library/Fonts/Helvetica.ttc",
        "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf",
    ]

    for font_path in font_paths:
        if os.path.exists(font_path):
            try:
                font = ImageFont.truetype(font_path, font_size)
                break
            except:
                continue

    if font is None:
        font = ImageFont.load_default()

    # Get text bounding box to center it
    text = "P"
    bbox = draw.textbbox((0, 0), text, font=font)
    text_width = bbox[2] - bbox[0]
    text_height = bbox[3] - bbox[1]

    # Calculate position to center the text
    x = (size - text_width) // 2 - bbox[0]
    y = (size - text_height) // 2 - bbox[1]

    # Draw the text
    draw.text((x, y), text, fill=color, font=font)

    return img

def main():
    base_path = r"C:\Users\Tanishq\Desktop\project-p2p\app\src\main\res"

    print("Generating PingChat launcher icons...")

    # Step 1: Remove old BitChat icons
    print("\n1. Removing old BitChat icons...")
    for density in ICON_SIZES.keys():
        mipmap_dir = os.path.join(base_path, f"mipmap-{density}")
        if os.path.exists(mipmap_dir):
            for filename in os.listdir(mipmap_dir):
                if filename.startswith("ic_launcher"):
                    file_path = os.path.join(mipmap_dir, filename)
                    os.remove(file_path)
                    print(f"   Removed: {file_path}")

    # Step 2: Generate regular launcher icons
    print("\n2. Generating regular launcher icons...")
    for density, size in ICON_SIZES.items():
        mipmap_dir = os.path.join(base_path, f"mipmap-{density}")
        os.makedirs(mipmap_dir, exist_ok=True)

        # Create ic_launcher.png
        icon = create_icon_with_p(size, PURPLE_BG, WHITE_FG)
        icon_path = os.path.join(mipmap_dir, "ic_launcher.png")
        icon.save(icon_path, "PNG")
        print(f"   Created: {icon_path} ({size}x{size}px)")

    # Step 3: Generate adaptive icon components
    print("\n3. Generating adaptive icon components...")
    for density, size in ADAPTIVE_SIZES.items():
        mipmap_dir = os.path.join(base_path, f"mipmap-{density}")
        os.makedirs(mipmap_dir, exist_ok=True)

        # Create ic_launcher_adaptive_back.png (solid purple background)
        bg = create_solid_background(size, PURPLE_BG)
        bg_path = os.path.join(mipmap_dir, "ic_launcher_adaptive_back.png")
        bg.save(bg_path, "PNG")
        print(f"   Created: {bg_path} ({size}x{size}px)")

        # Create ic_launcher_adaptive_fore.png (white P on transparent)
        fg = create_foreground_p(size, WHITE_FG)
        fg_path = os.path.join(mipmap_dir, "ic_launcher_adaptive_fore.png")
        fg.save(fg_path, "PNG")
        print(f"   Created: {fg_path} ({size}x{size}px)")

    print("\n4. Adaptive icon XML files already configured.")
    print("\n✓ All PingChat launcher icons generated successfully!")
    print(f"\nIcon color scheme:")
    print(f"  - Background: {PURPLE_BG}")
    print(f"  - Foreground: {WHITE_FG}")

if __name__ == "__main__":
    main()
