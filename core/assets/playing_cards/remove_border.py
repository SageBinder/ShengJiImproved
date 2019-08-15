# This script removes the black borders on the cards

from PIL import Image
import os

border_size = 10

for file in os.listdir(".\\"):
    ext = os.path.splitext(file)[-1].lower()
    if ext == ".png":
        print("Opening " + file)
        img = Image.open(file).convert("RGBA")
        pixels = img.load()
        width, height = img.size

        for x in range(width):
            for y in range(height):
                if x < border_size or x > width - border_size or y < border_size or y > height - border_size:
                    print("\tFor pixel (" + str(x) + ", " + str(y) + "), putting transparent pixel")
                    pixels[x, y] = (0, 0, 0, 0)

        # img.putdata(new_data)
        img.save(file, "PNG")
        print("Saving image to " + file)