# This script fixes the weird gamma data that the svg files spit out for some cards.
# Basically it just loads each image and writes back the RGBA values, ignoring everything else.

from PIL import Image
import os

for file in os.listdir(".\\"):
    ext = os.path.splitext(file)[-1].lower()
    if ext == ".png":
        new_data = []
        print("Opening " + file)
        img = Image.open(file).convert("RGBA")
        data = img.getdata()
        for pixel in data:
            new_data.append((pixel[0], pixel[1], pixel[2], pixel[3]))
        img.putdata(new_data)
        img.save(file, "PNG")
        print("Saving image to " + file)


