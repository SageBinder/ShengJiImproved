# This script changed all the red on the small_joker image to gray

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
                if pixels[x, y][0] > 200 and pixels[x, y][1] < 150 and pixels[x, y][2] < 150:
                    print("\tFor pixel (" + str(x) + ", " + str(y) + "), putting gray pixel")
                    pixels[x, y] = (211, 211, 211, 255)

        # img.putdata(new_data)
        img.save(file, "PNG")
        print("Saving image to " + file)