package signature.graph;

import signature.dataStructures.DList;

public class SignatureEdge {

    public final int id;
    public final int start;
    public final int end;
    public final double length;
    public final boolean penLift;
    private final DList<PixelPoint> pixels;




    public SignatureEdge(int id, int start, int end, double length, DList<PixelPoint> pixels)
    {
        this(id, start, end, length, pixels, false);
    }

    public SignatureEdge(
            int id,
            int start,
            int end,
            double length,
            DList<PixelPoint> pixels,
            boolean penLift)
    {
        this.id = id;
        this.start = start;
        this.end = end;
        this.length = length;
        this.pixels = pixels;
        this.penLift = penLift;
    }

    public int pixelCount()
    {
        return pixels.size();

    }

    public PixelPoint pixelAt(int index)
    {
        return pixels.get(index);
    }


    public DList<PixelPoint> pixels()
    {
        return pixels;
    }

    public int other(int nodeId)
    {
        
        if(nodeId == start)
        {
            return end;
        }
        if(nodeId == end)
        {
            return start;
        }
        
        throw new IllegalArgumentException("Node id " + nodeId + " is not part of this edge.");

    }

    
}
