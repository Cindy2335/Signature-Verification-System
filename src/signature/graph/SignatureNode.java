package signature.graph;

public class SignatureNode
{
    public final int id;
    public final int x;
    public final int y;
    public final String kind;
    public final int pixelDegree;

    public SignatureNode(int id, int x, int y, String kind, int pixelDegree)
    {
        this.id = id;
        this.x = x;
        this.y = y;
        this.kind = kind;
        this.pixelDegree = pixelDegree;
    }

    public PixelPoint point()
    {
        return new PixelPoint(x, y);
    }

}
