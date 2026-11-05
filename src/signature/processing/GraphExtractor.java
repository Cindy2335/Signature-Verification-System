package signature.processing;

import signature.dataStructures.DList;
import signature.graph.PixelPoint;
import signature.graph.SignatureGraph;
import signature.image.BinaryImage;

public class GraphExtractor {
    private static final int[] DX = {-1, 0, 1, -1, 1, -1, 0, 1};
    private static final int[] DY = {-1, -1, -1, 0, 0, 1, 1, 1};

    private GraphExtractor() {
    }

    public static SignatureGraph extract(BinaryImage image) {
        SignatureGraph graph = new SignatureGraph(image.width(), image.height());
        if (image.width() == 0 || image.height() == 0 || image.inkCount() == 0) {
            return graph;
        }

        int width = image.width();
        int height = image.height();
        int[][] degree = computePixelDegrees(image);
        boolean[][] key = new boolean[height][width];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (image.isInk(x, y) && degree[y][x] != 2) {
                    key[y][x] = true;
                }
            }
        }

        addCycleAnchors(image, key);

        int[][] nodeAt = new int[height][width];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                nodeAt[y][x] = -1;
            }
        }
        createNodes(graph, key, degree, nodeAt);
        createEdges(graph, image, nodeAt);
        addPenLiftBridges(graph);

        return graph;
    }

    private static int[][] computePixelDegrees(BinaryImage image) {
        int[][] degree = new int[image.height()][image.width()];
        for (int y = 0; y < image.height(); y++) {
            for (int x = 0; x < image.width(); x++) {
                if (!image.isInk(x, y)) {
                    continue;
                }
                degree[y][x] = inkNeighbors(image, x, y).size();
            }
        }
        return degree;
    }

    private static void addCycleAnchors(BinaryImage image, boolean[][] key) {
        int width = image.width();
        int height = image.height();
        boolean[][] seen = new boolean[height][width];
        int[] queueX = new int[Math.max(1, width * height)];
        int[] queueY = new int[Math.max(1, width * height)];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (!image.isInk(x, y) || seen[y][x]) {
                    continue;
                }

                int head = 0;
                int tail = 0;
                queueX[tail] = x;
                queueY[tail] = y;
                tail++;
                seen[y][x] = true;

                boolean hasKey = false;
                int anchorX = x;
                int anchorY = y;

                while (head < tail) {
                    int currentX = queueX[head];
                    int currentY = queueY[head];
                    head++;

                    if (key[currentY][currentX]) {
                        hasKey = true;
                    }
                    if (currentY < anchorY || (currentY == anchorY && currentX < anchorX)) {
                        anchorX = currentX;
                        anchorY = currentY;
                    }

                    for (int i = 0; i < DX.length; i++) {
                        int nx = currentX + DX[i];
                        int ny = currentY + DY[i];
                        if (!inside(width, height, nx, ny)
                                || !image.isInk(nx, ny)
                                || seen[ny][nx]) {
                            continue;
                        }
                        seen[ny][nx] = true;
                        queueX[tail] = nx;
                        queueY[tail] = ny;
                        tail++;
                    }
                }

                if (!hasKey) {
                    key[anchorY][anchorX] = true;
                }
            }
        }
    }

    private static void createNodes(
            SignatureGraph graph,
            boolean[][] key,
            int[][] degree,
            int[][] nodeAt) {
        int height = key.length;
        int width = height == 0 ? 0 : key[0].length;
        boolean[][] seenJunction = new boolean[height][width];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (!key[y][x] || degree[y][x] < 3 || seenJunction[y][x]) {
                    continue;
                }
                DList<PixelPoint> cluster = collectJunctionCluster(key, degree, seenJunction, x, y);
                PixelPoint representative = representative(cluster);
                int maxDegree = maxDegree(cluster, degree);
                int nodeId = graph.addNode(representative.x, representative.y, "junction", maxDegree);
                for (PixelPoint point : cluster) {
                    nodeAt[point.y][point.x] = nodeId;
                }
            }
        }

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (!key[y][x] || nodeAt[y][x] != -1) {
                    continue;
                }
                int pixelDegree = degree[y][x];
                String kind = pixelDegree == 1 ? "endpoint"
                        : pixelDegree == 2 ? "cycle_anchor"
                        : "connector";
                int nodeId = graph.addNode(x, y, kind, pixelDegree);
                nodeAt[y][x] = nodeId;
            }
        }
    }

    private static DList<PixelPoint> collectJunctionCluster(
            boolean[][] key,
            int[][] degree,
            boolean[][] seen,
            int startX,
            int startY) {
        int width = key[0].length;
        int height = key.length;
        int[] queueX = new int[width * height];
        int[] queueY = new int[width * height];
        int head = 0;
        int tail = 0;
        DList<PixelPoint> cluster = new DList<PixelPoint>();

        queueX[tail] = startX;
        queueY[tail] = startY;
        tail++;
        seen[startY][startX] = true;

        while (head < tail) {
            int x = queueX[head];
            int y = queueY[head];
            head++;
            cluster.add(new PixelPoint(x, y));

            for (int i = 0; i < DX.length; i++) {
                int nx = x + DX[i];
                int ny = y + DY[i];
                if (!inside(width, height, nx, ny)
                        || seen[ny][nx]
                        || !key[ny][nx]
                        || degree[ny][nx] < 3) {
                    continue;
                }
                seen[ny][nx] = true;
                queueX[tail] = nx;
                queueY[tail] = ny;
                tail++;
            }
        }

        return cluster;
    }

    private static void createEdges(SignatureGraph graph, BinaryImage image, int[][] nodeAt) {
        int width = image.width();
        int height = image.height();
        boolean[][][] visited = new boolean[height][width][DX.length];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int startNode = nodeAt[y][x];
                if (startNode < 0) {
                    continue;
                }

                for (int direction = 0; direction < DX.length; direction++) {
                    int nx = x + DX[direction];
                    int ny = y + DY[direction];
                    if (!inside(width, height, nx, ny) || !image.isInk(nx, ny)) {
                        continue;
                    }
                    if (nodeAt[ny][nx] == startNode) {
                        continue;
                    }
                    if (visited[y][x][direction]) {
                        continue;
                    }

                    DList<PixelPoint> path = traceEdge(image, nodeAt, x, y, nx, ny);
                    if (path.size() < 2) {
                        continue;
                    }

                    markVisited(path, visited);
                    PixelPoint end = path.get(path.size() - 1);
                    int endNode = nodeAt[end.y][end.x];
                    if (endNode < 0) {
                        continue;
                    }

                    graph.addEdge(startNode, endNode, path, pathLength(path));
                }
            }
        }
    }

    private static void addPenLiftBridges(SignatureGraph graph) {
        int nodeCount = graph.nodeCount();
        if (nodeCount < 2 || graph.connectedComponents() <= 1) {
            return;
        }

        Component[] components = initialComponents(graph);
        int componentCount = componentCount(components);

        while (componentCount > 1) {
            Bridge best = closestBridge(graph, components, componentCount);
            if (best == null) {
                return;
            }

            DList<PixelPoint> pixels = straightLine(
                    graph.node(best.startNode).point(),
                    graph.node(best.endNode).point());
            graph.addEdge(best.startNode, best.endNode, pixels, best.distance, true);
            mergeComponents(components[best.leftComponent], components[best.rightComponent]);
            components[best.rightComponent] = components[componentCount - 1];
            components[componentCount - 1] = null;
            componentCount--;
        }
    }

    private static Component[] initialComponents(SignatureGraph graph) {
        Component[] components = new Component[graph.nodeCount()];
        boolean[] seen = new boolean[graph.nodeCount()];
        int[] stack = new int[graph.nodeCount()];
        int componentCount = 0;

        for (int nodeId = 0; nodeId < graph.nodeCount(); nodeId++) {
            if (seen[nodeId]) {
                continue;
            }

            Component component = new Component();
            int top = 0;
            stack[top++] = nodeId;
            seen[nodeId] = true;

            while (top > 0) {
                int current = stack[--top];
                component.nodeIds.add(Integer.valueOf(current));

                for (signature.graph.GraphConnection connection : graph.connections(current)) {
                    if (!seen[connection.targetNodeId]) {
                        seen[connection.targetNodeId] = true;
                        stack[top++] = connection.targetNodeId;
                    }
                }
            }

            components[componentCount] = component;
            componentCount++;
        }

        return components;
    }

    private static int componentCount(Component[] components) {
        int count = 0;
        while (count < components.length && components[count] != null) {
            count++;
        }
        return count;
    }

    private static Bridge closestBridge(
            SignatureGraph graph,
            Component[] components,
            int componentCount) {
        Bridge best = null;
        for (int i = 0; i < componentCount; i++) {
            for (int j = i + 1; j < componentCount; j++) {
                Bridge candidate = closestBridgeBetween(graph, components[i], components[j], i, j, true);
                if (candidate == null) {
                    candidate = closestBridgeBetween(graph, components[i], components[j], i, j, false);
                }
                if (candidate != null && (best == null || candidate.distance < best.distance)) {
                    best = candidate;
                }
            }
        }
        return best;
    }

    private static Bridge closestBridgeBetween(
            SignatureGraph graph,
            Component left,
            Component right,
            int leftIndex,
            int rightIndex,
            boolean endpointsOnly) {
        Bridge best = null;

        for (Integer leftNodeId : left.nodeIds) {
            if (endpointsOnly && !"endpoint".equals(graph.node(leftNodeId.intValue()).kind)) {
                continue;
            }

            for (Integer rightNodeId : right.nodeIds) {
                if (endpointsOnly && !"endpoint".equals(graph.node(rightNodeId.intValue()).kind)) {
                    continue;
                }

                double distance = graph.node(leftNodeId.intValue())
                        .point()
                        .distanceTo(graph.node(rightNodeId.intValue()).point());
                if (best == null || distance < best.distance) {
                    best = new Bridge(
                            leftIndex,
                            rightIndex,
                            leftNodeId.intValue(),
                            rightNodeId.intValue(),
                            distance);
                }
            }
        }

        return best;
    }

    private static void mergeComponents(Component target, Component source) {
        for (Integer nodeId : source.nodeIds) {
            target.nodeIds.add(nodeId);
        }
    }

    private static DList<PixelPoint> straightLine(PixelPoint start, PixelPoint end) {
        DList<PixelPoint> pixels = new DList<PixelPoint>();
        int x = start.x;
        int y = start.y;
        int dx = Math.abs(end.x - start.x);
        int dy = Math.abs(end.y - start.y);
        int sx = start.x < end.x ? 1 : -1;
        int sy = start.y < end.y ? 1 : -1;
        int error = dx - dy;

        while (true) {
            pixels.add(new PixelPoint(x, y));
            if (x == end.x && y == end.y) {
                break;
            }

            int twiceError = error * 2;
            if (twiceError > -dy) {
                error -= dy;
                x += sx;
            }
            if (twiceError < dx) {
                error += dx;
                y += sy;
            }
        }

        return pixels;
    }

    private static DList<PixelPoint> traceEdge(
            BinaryImage image,
            int[][] nodeAt,
            int startX,
            int startY,
            int nextX,
            int nextY) {
        DList<PixelPoint> path = new DList<PixelPoint>();
        path.add(new PixelPoint(startX, startY));
        path.add(new PixelPoint(nextX, nextY));

        int previousX = startX;
        int previousY = startY;
        int currentX = nextX;
        int currentY = nextY;
        int maxSteps = Math.max(1, image.width() * image.height() + 1);

        while (nodeAt[currentY][currentX] < 0 && path.size() <= maxSteps) {
            DList<PixelPoint> candidates = inkNeighbors(image, currentX, currentY);
            PixelPoint best = null;
            int bestPenalty = Integer.MAX_VALUE;

            for (PixelPoint candidate : candidates) {
                if (candidate.x == previousX && candidate.y == previousY) {
                    continue;
                }
                int penalty = turnPenalty(previousX, previousY, currentX, currentY, candidate.x, candidate.y);
                if (best == null || penalty < bestPenalty) {
                    best = candidate;
                    bestPenalty = penalty;
                }
            }

            if (best == null) {
                break;
            }

            previousX = currentX;
            previousY = currentY;
            currentX = best.x;
            currentY = best.y;
            path.add(new PixelPoint(currentX, currentY));
        }

        return path;
    }

    private static void markVisited(DList<PixelPoint> path, boolean[][][] visited) {
        for (int i = 0; i < path.size() - 1; i++) {
            PixelPoint a = path.get(i);
            PixelPoint b = path.get(i + 1);
            int direction = directionIndex(b.x - a.x, b.y - a.y);
            int reverse = directionIndex(a.x - b.x, a.y - b.y);
            if (direction >= 0) {
                visited[a.y][a.x][direction] = true;
            }
            if (reverse >= 0) {
                visited[b.y][b.x][reverse] = true;
            }
        }
    }

    private static double pathLength(DList<PixelPoint> path) {
        double total = 0.0;
        for (int i = 0; i < path.size() - 1; i++) {
            PixelPoint a = path.get(i);
            PixelPoint b = path.get(i + 1);
            int dx = Math.abs(a.x - b.x);
            int dy = Math.abs(a.y - b.y);
            total += dx == 1 && dy == 1 ? Math.sqrt(2.0) : 1.0;
        }
        return total;
    }

    private static DList<PixelPoint> inkNeighbors(BinaryImage image, int x, int y) {
        DList<PixelPoint> neighbors = new DList<PixelPoint>();
        for (int i = 0; i < DX.length; i++) {
            int nx = x + DX[i];
            int ny = y + DY[i];
            if (inside(image.width(), image.height(), nx, ny) && image.isInk(nx, ny)) {
                neighbors.add(new PixelPoint(nx, ny));
            }
        }
        return neighbors;
    }

    private static PixelPoint representative(DList<PixelPoint> cluster) {
        if (cluster.size() == 1) {
            return cluster.get(0);
        }

        double centerX = 0.0;
        double centerY = 0.0;
        for (PixelPoint point : cluster) {
            centerX += point.x;
            centerY += point.y;
        }
        centerX /= cluster.size();
        centerY /= cluster.size();

        PixelPoint best = cluster.get(0);
        double bestScore = Double.MAX_VALUE;
        for (PixelPoint point : cluster) {
            double dx = point.x - centerX;
            double dy = point.y - centerY;
            double score = (dx * dx) + (dy * dy);
            if (score < bestScore) {
                best = point;
                bestScore = score;
            }
        }
        return best;
    }

    private static int maxDegree(DList<PixelPoint> cluster, int[][] degree) {
        int max = 0;
        for (PixelPoint point : cluster) {
            max = Math.max(max, degree[point.y][point.x]);
        }
        return max;
    }

    private static int turnPenalty(
            int previousX,
            int previousY,
            int currentX,
            int currentY,
            int candidateX,
            int candidateY) {
        int inX = currentX - previousX;
        int inY = currentY - previousY;
        int outX = candidateX - currentX;
        int outY = candidateY - currentY;
        return Math.abs(inX - outX) + Math.abs(inY - outY);
    }

    private static int directionIndex(int dx, int dy) {
        for (int i = 0; i < DX.length; i++) {
            if (DX[i] == dx && DY[i] == dy) {
                return i;
            }
        }
        return -1;
    }

    private static class Component {
        final DList<Integer> nodeIds = new DList<Integer>();
    }

    private static class Bridge {
        final int leftComponent;
        final int rightComponent;
        final int startNode;
        final int endNode;
        final double distance;

        Bridge(
                int leftComponent,
                int rightComponent,
                int startNode,
                int endNode,
                double distance) {
            this.leftComponent = leftComponent;
            this.rightComponent = rightComponent;
            this.startNode = startNode;
            this.endNode = endNode;
            this.distance = distance;
        }
    }

    private static boolean inside(int width, int height, int x, int y) {
        return x >= 0 && y >= 0 && x < width && y < height;
    }
}
