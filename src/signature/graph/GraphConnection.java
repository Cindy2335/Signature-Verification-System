package signature.graph;

public class GraphConnection
{
    public final int targetNodeId;
    public final double distance;
    public final SignatureEdge edge;

    public GraphConnection(int targetNodeId, double distance, SignatureEdge edge)
    {
        this.targetNodeId = targetNodeId;
        this.distance = distance;
        this.edge = edge;
    }
}
