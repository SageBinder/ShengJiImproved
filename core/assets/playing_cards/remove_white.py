# This script removes the white squares which exist around the symbols of some cards

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
            if pixel[0] > 240 and pixel[1] > 240 and pixel[2] > 240 and pixel[3] > 240:
                new_data.append((0, 0, 0, 0))
            else:
                new_data.append((pixel[0], pixel[1], pixel[2], pixel[3]))
        img.putdata(new_data)
        img.save(file, "PNG")
        print("Saving image to " + file)