from PIL import Image, ImageFilter
print "asd"
im = Image.open("3.jpg").convert("L")
im1 = Image.open("4.jpg").convert("L")
p1 = im.load()
p2 = im1.load()

for tup in [(x, y) for x in xrange(im.size[0]) for y in xrange(im.size[1])]:
    p1[tup] = 255 if (abs(p1[tup] - p2[tup]) > 100) else 0
im = im.filter(ImageFilter.ModeFilter(size=10))
im.save("out.jpg")
