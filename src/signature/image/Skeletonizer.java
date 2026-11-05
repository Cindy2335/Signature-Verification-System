package signature.image;

import signature.dataStructures.DList;
import signature.graph.PixelPoint;

public class Skeletonizer {
    private Skeletonizer() {
    }

    public static BinaryImage skeletonize(BinaryImage image) {
        if (image.width() < 3 || image.height() < 3) {
            return image;
        }

        boolean[][] pixels = image.copyPixels();
        boolean changed = true;

        while (changed) {
            changed = false;

            DList<PixelPoint> firstList = findPixelsToRemove(pixels, 1);
            if (!firstList.isEmpty()) {
                removePixels(pixels, firstList);
                changed = true;
            }

            DList<PixelPoint> secondList = findPixelsToRemove(pixels, 2);
            if (!secondList.isEmpty()) {
                removePixels(pixels, secondList);
                changed = true;
            }
        }

        return new BinaryImage(pixels);
    }

    private static DList<PixelPoint> findPixelsToRemove(boolean[][] pixels, int round) {
        DList<PixelPoint> list = new DList<PixelPoint>();
        int height = pixels.length;
        int width = pixels[0].length;

        for (int y = 1; y < height - 1; y++) {
            for (int x = 1; x < width - 1; x++) {
                if (pixels[y][x] && canRemovePixel(pixels, x, y, round)) {
                    list.add(new PixelPoint(x, y));
                }
            }
        }

        return list;
    }

    private static boolean canRemovePixel(boolean[][] pixels, int x, int y, int round) {
        int neighbours = countBlackNeighbours(pixels, x, y);
        if (neighbours < 2 || neighbours > 6) {
            return false;
        }

        if (countBlackGroups(pixels, x, y) != 1) {
            return false;
        }

        boolean top = pixels[y - 1][x];
        boolean right = pixels[y][x + 1];
        boolean bottom = pixels[y + 1][x];
        boolean left = pixels[y][x - 1];

        if (round == 1) {
            return !(top && right && bottom) && !(right && bottom && left);
        }

        return !(top && right && left) && !(top && bottom && left);
    }

    private static int countBlackNeighbours(boolean[][] pixels, int x, int y) {
        int count = 0;

        if (pixels[y - 1][x]) {
            count++;
        }
        if (pixels[y - 1][x + 1]) {
            count++;
        }
        if (pixels[y][x + 1]) {
            count++;
        }
        if (pixels[y + 1][x + 1]) {
            count++;
        }
        if (pixels[y + 1][x]) {
            count++;
        }
        if (pixels[y + 1][x - 1]) {
            count++;
        }
        if (pixels[y][x - 1]) {
            count++;
        }
        if (pixels[y - 1][x - 1]) {
            count++;
        }

        return count;
    }

    private static int countBlackGroups(boolean[][] pixels, int x, int y) {
        int groups = 0;

        boolean top = pixels[y - 1][x];
        boolean topRight = pixels[y - 1][x + 1];
        boolean right = pixels[y][x + 1];
        boolean bottomRight = pixels[y + 1][x + 1];
        boolean bottom = pixels[y + 1][x];
        boolean bottomLeft = pixels[y + 1][x - 1];
        boolean left = pixels[y][x - 1];
        boolean topLeft = pixels[y - 1][x - 1];

        if (!top && topRight) {
            groups++;
        }
        if (!topRight && right) {
            groups++;
        }
        if (!right && bottomRight) {
            groups++;
        }
        if (!bottomRight && bottom) {
            groups++;
        }
        if (!bottom && bottomLeft) {
            groups++;
        }
        if (!bottomLeft && left) {
            groups++;
        }
        if (!left && topLeft) {
            groups++;
        }
        if (!topLeft && top) {
            groups++;
        }

        return groups;
    }

    private static void removePixels(boolean[][] pixels, DList<PixelPoint> list) {
        for (PixelPoint point : list) {
            pixels[point.y][point.x] = false;
        }
    }
}
