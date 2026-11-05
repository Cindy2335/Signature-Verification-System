package signature.image;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

public class ImageLoader {
    private static final int CROP_GAP = 4;
    private static final int MIN_COMPONENT_PIXELS = 18;
    private static final int MIN_COMPONENT_SPAN = 8;
    private static final int[] DX = {-1, 0, 1, -1, 1, -1, 0, 1};
    private static final int[] DY = {-1, -1, -1, 0, 0, 1, 1, 1};

    private ImageLoader() {
    }

    public static BinaryImage loadSignature(File file) throws IOException {
        BufferedImage image = ImageIO.read(file);
        if (image == null) {
            throw new IOException("Could not read image: " + file.getName());
        }

        BinaryImage blackWhite = makeBlackAndWhite(image);
        BinaryImage cleaned = removeSmallComponents(blackWhite);
        BinaryImage cropped = cropImage(cleaned);
        return Skeletonizer.skeletonize(cropped);
    }

    private static BinaryImage makeBlackAndWhite(BufferedImage image) {
        BinaryImage result = new BinaryImage(image.getWidth(), image.getHeight());
        int[] histogram = new int[256];
        int[] greys = new int[image.getWidth() * image.getHeight()];

        int index = 0;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                Color colour = new Color(image.getRGB(x, y));
                int grey = (colour.getRed() + colour.getGreen() + colour.getBlue()) / 3;
                greys[index++] = grey;
                histogram[grey]++;
            }
        }

        int threshold = otsuThreshold(histogram, greys.length);
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                if (greys[(y * image.getWidth()) + x] <= threshold) {
                    result.setInk(x, y, true);
                }
            }
        }

        return result;
    }

    private static int otsuThreshold(int[] histogram, int pixelCount) {
        long totalIntensity = 0;
        for (int intensity = 0; intensity < histogram.length; intensity++) {
            totalIntensity += (long) intensity * histogram[intensity];
        }

        long backgroundIntensity = 0;
        int backgroundCount = 0;
        double bestVariance = -1.0;
        int threshold = 127;

        for (int candidate = 0; candidate < histogram.length; candidate++) {
            backgroundCount += histogram[candidate];
            if (backgroundCount == 0) {
                continue;
            }

            int foregroundCount = pixelCount - backgroundCount;
            if (foregroundCount == 0) {
                break;
            }

            backgroundIntensity += (long) candidate * histogram[candidate];
            double backgroundMean = backgroundIntensity / (double) backgroundCount;
            double foregroundMean =
                    (totalIntensity - backgroundIntensity) / (double) foregroundCount;
            double difference = backgroundMean - foregroundMean;
            double variance = (double) backgroundCount * foregroundCount
                    * difference * difference;

            if (variance > bestVariance) {
                bestVariance = variance;
                threshold = candidate;
            }
        }

        return threshold;
    }

    private static BinaryImage removeSmallComponents(BinaryImage image) {
        int width = image.width();
        int height = image.height();
        if (width == 0 || height == 0 || image.inkCount() == 0) {
            return image;
        }

        boolean[][] kept = new boolean[height][width];
        boolean[][] seen = new boolean[height][width];
        int[] queueX = new int[Math.max(1, width * height)];
        int[] queueY = new int[Math.max(1, width * height)];
        int minimumPixels = Math.max(MIN_COMPONENT_PIXELS, (width * height) / 60000);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (!image.isInk(x, y) || seen[y][x]) {
                    continue;
                }

                int head = 0;
                int tail = 0;
                int minX = x;
                int minY = y;
                int maxX = x;
                int maxY = y;

                queueX[tail] = x;
                queueY[tail] = y;
                tail++;
                seen[y][x] = true;

                while (head < tail) {
                    int currentX = queueX[head];
                    int currentY = queueY[head];
                    head++;

                    minX = Math.min(minX, currentX);
                    minY = Math.min(minY, currentY);
                    maxX = Math.max(maxX, currentX);
                    maxY = Math.max(maxY, currentY);

                    for (int i = 0; i < DX.length; i++) {
                        int nextX = currentX + DX[i];
                        int nextY = currentY + DY[i];
                        if (!inside(width, height, nextX, nextY)
                                || seen[nextY][nextX]
                                || !image.isInk(nextX, nextY)) {
                            continue;
                        }
                        seen[nextY][nextX] = true;
                        queueX[tail] = nextX;
                        queueY[tail] = nextY;
                        tail++;
                    }
                }

                int componentWidth = maxX - minX + 1;
                int componentHeight = maxY - minY + 1;
                int componentSpan = Math.max(componentWidth, componentHeight);
                boolean longStroke = tail >= minimumPixels && componentSpan >= MIN_COMPONENT_SPAN;
                boolean denseStroke = tail >= (minimumPixels * 3) && componentSpan >= 4;

                if (longStroke || denseStroke) {
                    for (int i = 0; i < tail; i++) {
                        kept[queueY[i]][queueX[i]] = true;
                    }
                }
            }
        }

        return new BinaryImage(kept);
    }

    private static BinaryImage cropImage(BinaryImage image) {
        int left = image.width();
        int top = image.height();
        int right = -1;
        int bottom = -1;

        for (int y = 0; y < image.height(); y++) {
            for (int x = 0; x < image.width(); x++) {
                if (image.isInk(x, y)) {
                    if (x < left) {
                        left = x;
                    }
                    if (x > right) {
                        right = x;
                    }
                    if (y < top) {
                        top = y;
                    }
                    if (y > bottom) {
                        bottom = y;
                    }
                }
            }
        }

        if (right == -1) {
            return image;
        }

        left = Math.max(0, left - CROP_GAP);
        top = Math.max(0, top - CROP_GAP);
        right = Math.min(image.width() - 1, right + CROP_GAP);
        bottom = Math.min(image.height() - 1, bottom + CROP_GAP);

        BinaryImage cropped = new BinaryImage(right - left + 1, bottom - top + 1);
        for (int y = top; y <= bottom; y++) {
            for (int x = left; x <= right; x++) {
                cropped.setInk(x - left, y - top, image.isInk(x, y));
            }
        }

        return cropped;
    }

    private static boolean inside(int width, int height, int x, int y) {
        return x >= 0 && y >= 0 && x < width && y < height;
    }
}
