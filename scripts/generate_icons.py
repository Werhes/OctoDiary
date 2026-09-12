from PIL import Image
import os

SRC = r"C:\Users\WerhesDev\Downloads\90847608 (1).png"
BASE = r"c:\Users\WerhesDev\Documents\GitHub\OctoDiary"

img = Image.open(SRC).convert("RGBA")

# iOS app icon (1024x1024)
ios_dir = os.path.join(
    BASE, "iosApp", "iosApp", "Assets.xcassets", "AppIcon.appiconset"
)
os.makedirs(ios_dir, exist_ok=True)
img.resize((1024, 1024), Image.LANCZOS).save(
    os.path.join(ios_dir, "app-icon-1024.png")
)

# Android legacy launcher icons
sizes = {
    "mipmap-mdpi": 48,
    "mipmap-hdpi": 72,
    "mipmap-xhdpi": 96,
    "mipmap-xxhdpi": 144,
    "mipmap-xxxhdpi": 192,
}
for d, s in sizes.items():
    dpath = os.path.join(BASE, "composeApp", "src", "androidMain", "res", d)
    os.makedirs(dpath, exist_ok=True)
    resized = img.resize((s, s), Image.LANCZOS)
    resized.save(os.path.join(dpath, "ic_launcher.png"))
    resized.save(os.path.join(dpath, "ic_launcher_round.png"))

print("icons generated")