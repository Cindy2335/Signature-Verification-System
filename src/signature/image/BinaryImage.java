package signature.image;

public class BinaryImage {
    private final int width;
    private final int height;
    private final boolean[][] ink;

    public BinaryImage(int width, int height) {
        this.width = width;
        this.height = height;
        this.ink = new boolean[height][width];
    }

    public BinaryImage(boolean[][] source) {
        this.height = source.length;
        this.width = height == 0 ? 0 : source[0].length;
        this.ink = new boolean[height][width];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                ink[y][x] = source[y][x];
            }
        }
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public boolean isInk(int x, int y) {
        return ink[y][x];
    }

    public void setInk(int x, int y, boolean value) {
        ink[y][x] = value;
    }

    public boolean[][] copyPixels() {
        boolean[][] copy = new boolean[height][width];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                copy[y][x] = ink[y][x];
            }
        }

        return copy;
    }

    public int inkCount() {
        int count = 0;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (ink[y][x]) {
                    count++;
                }
            }
        }

        return count;
    }
}
