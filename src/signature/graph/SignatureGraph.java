package signature.graph;

import signature.dataStructures.DList;

public class SignatureGraph {
    private final DList<SignatureNode> nodes;
    private final DList<SignatureEdge> edges;
    private DList<GraphConnection>[] adjacency;
    private final int width;
    private final int height;

    @SuppressWarnings("unchecked")
    public SignatureGraph(int width, int height) {
        this.width = width;
        this.height = height;
        this.nodes = new DList<SignatureNode>();
        this.edges = new DList<SignatureEdge>();
        this.adjacency = (DList<GraphConnection>[]) new DList[16];
    }

    public int addNode(int x, int y, String kind, int pixelDegree) {
        int id = nodes.size();
        nodes.add(new SignatureNode(id, x, y, kind, pixelDegree));
        ensureAdjacencyCapacity(id + 1);
        adjacency[id] = new DList<GraphConnection>();
        return id;
    }

    public int addEdge(int start, int end, DList<PixelPoint> pixels, double length) {
        return addEdge(start, end, pixels, length, false);
    }

    public int addEdge(int start, int end, DList<PixelPoint> pixels, double length, boolean penLift) {
        int id = edges.size();
        SignatureEdge edge = new SignatureEdge(id, start, end, length, pixels, penLift);
        edges.add(edge);
        adjacency[start].add(new GraphConnection(end, length, edge));
        adjacency[end].add(new GraphConnection(start, length, edge));
        return id;
    }

    public int nodeCount() {
        return nodes.size();
    }

    public int edgeCount() {
        return edges.size();
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public SignatureNode node(int id) {
        return nodes.get(id);
    }

    public SignatureEdge edge(int id) {
        return edges.get(id);
    }

    public DList<SignatureNode> nodes() {
        return nodes;
    }

    public DList<SignatureEdge> edges() {
        return edges;
    }

    public DList<GraphConnection> connections(int nodeId) {
        return adjacency[nodeId];
    }

    public int graphDegree(int nodeId) {
        return adjacency[nodeId].size();
    }

    public int graphDegree(int nodeId, boolean includePenLift) {
        if (includePenLift) {
            return graphDegree(nodeId);
        }

        int degree = 0;
        for (GraphConnection connection : adjacency[nodeId]) {
            if (!connection.edge.penLift) {
                degree++;
            }
        }
        return degree;
    }

    public int[] endpointIds() {
        DList<Integer> ids = new DList<Integer>();
        for (SignatureNode node : nodes) {
            if ("endpoint".equals(node.kind) || graphDegree(node.id) == 1) {
                ids.add(node.id);
            }
        }
        return toIntArray(ids);
    }

    public int[] junctionIds() {
        return junctionIds(true);
    }

    public int[] junctionIds(boolean includePenLift) {
        DList<Integer> ids = new DList<Integer>();
        for (SignatureNode node : nodes) {
            if ("junction".equals(node.kind)
                    || node.pixelDegree >= 3
                    || graphDegree(node.id, includePenLift) >= 3) {
                ids.add(node.id);
            }
        }
        return toIntArray(ids);
    }

    public int[] endpointIds(boolean includePenLift) {
        DList<Integer> ids = new DList<Integer>();
        for (SignatureNode node : nodes) {
            if ("endpoint".equals(node.kind) || graphDegree(node.id, includePenLift) == 1) {
                ids.add(node.id);
            }
        }
        return toIntArray(ids);
    }

    public int structuralEdgeCount() {
        int count = 0;
        for (SignatureEdge edge : edges) {
            if (!edge.penLift) {
                count++;
            }
        }
        return count;
    }

    public int structuralConnectedComponents() {
        if (nodes.size() == 0) {
            return 0;
        }

        boolean[] seen = new boolean[nodes.size()];
        int components = 0;
        int[] stack = new int[nodes.size()];

        for (SignatureNode node : nodes) {
            if (seen[node.id]) {
                continue;
            }
            components++;
            int top = 0;
            stack[top++] = node.id;
            seen[node.id] = true;

            while (top > 0) {
                int current = stack[--top];
                for (GraphConnection connection : adjacency[current]) {
                    if (connection.edge.penLift || seen[connection.targetNodeId]) {
                        continue;
                    }
                    seen[connection.targetNodeId] = true;
                    stack[top++] = connection.targetNodeId;
                }
            }
        }
        return components;
    }

    public SignatureEdge edgeBetween(int start, int end) {
        SignatureEdge best = null;
        for (GraphConnection connection : adjacency[start]) {
            if (connection.targetNodeId == end) {
                if (best == null || connection.edge.length < best.length) {
                    best = connection.edge;
                }
            }
        }
        return best;
    }

    public double totalEdgeLength() {
        double total = 0.0;
        for (SignatureEdge edge : edges) {
            total += edge.length;
        }
        return total;
    }

    public int connectedComponents() {
        if (nodes.size() == 0) {
            return 0;
        }

        boolean[] seen = new boolean[nodes.size()];
        int components = 0;
        int[] stack = new int[nodes.size()];

        for (SignatureNode node : nodes) {
            if (seen[node.id]) {
                continue;
            }
            components++;
            int top = 0;
            stack[top++] = node.id;
            seen[node.id] = true;

            while (top > 0) {
                int current = stack[--top];
                for (GraphConnection connection : adjacency[current]) {
                    if (!seen[connection.targetNodeId]) {
                        seen[connection.targetNodeId] = true;
                        stack[top++] = connection.targetNodeId;
                    }
                }
            }
        }

        return components;
    }

    public Bounds bounds() {
        if (nodes.size() == 0) {
            return new Bounds(0, 0, 0, 0);
        }

        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        for (SignatureNode node : nodes) {
            minX = Math.min(minX, node.x);
            minY = Math.min(minY, node.y);
            maxX = Math.max(maxX, node.x);
            maxY = Math.max(maxY, node.y);
        }

        return new Bounds(minX, minY, maxX, maxY);
    }

    public String summaryText() {
        return "nodes=" + nodeCount()
                + ", edges=" + edgeCount()
                + ", endpoints=" + endpointIds().length
                + ", junctions=" + junctionIds().length
                + ", components=" + connectedComponents()
                + ", total length=" + String.format("%.2f", totalEdgeLength());
    }

    private int[] toIntArray(DList<Integer> ids) {
        int[] result = new int[ids.size()];
        for (int i = 0; i < ids.size(); i++) {
            result[i] = ids.get(i).intValue();
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private void ensureAdjacencyCapacity(int required) {
        if (required <= adjacency.length) {
            return;
        }

        int nextSize = adjacency.length * 2;
        while (nextSize < required) {
            nextSize *= 2;
        }

        DList<GraphConnection>[] next =
                (DList<GraphConnection>[]) new DList[nextSize];
        for (int i = 0; i < adjacency.length; i++) {
            next[i] = adjacency[i];
        }
        adjacency = next;
    }

    public static class Bounds {
        public final int minX;
        public final int minY;
        public final int maxX;
        public final int maxY;

        public Bounds(int minX, int minY, int maxX, int maxY) {
            this.minX = minX;
            this.minY = minY;
            this.maxX = maxX;
            this.maxY = maxY;
        }

        public double diagonal() {
            int dx = maxX - minX;
            int dy = maxY - minY;
            return Math.sqrt((dx * dx) + (dy * dy));
        }
    }
}
