package signature.graph;

public class PixelPoint {

    public final int x;
    public final int y;

    public PixelPoint(int x, int y)
    {
        this.x = x;
        this.y = y; 
    }

    public double distanceTo(PixelPoint other)
    {
        int dx = x - other.x;
        int dy = y - other.y;
        return Math.sqrt((dx*dx) + (dy*dy));
    }

    
}
