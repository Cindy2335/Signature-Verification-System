package signature.algorithms;

import signature.dataStructures.DList;
import signature.graph.PixelPoint;

public class PathResult {
    public final boolean found;
    public final int[] nodeIds;
    public final double distance;
    public final DList<PixelPoint> pixels;

    public PathResult(boolean found, int[] nodeIds, double distance, DList<PixelPoint> pixels) {
        this.found = found;
        this.nodeIds = nodeIds;
        this.distance = distance;
        this.pixels = pixels;
    }

    public static PathResult notFound() {
        return new PathResult(false, new int[0], Double.POSITIVE_INFINITY, new DList<PixelPoint>());
    }
}
