package signature.algorithms;

import java.util.Arrays;
import signature.dataStructures.intDoubleMinHeap;
import signature.dataStructures.DList;
import signature.graph.GraphConnection;
import signature.graph.PixelPoint;
import signature.graph.SignatureEdge;
import signature.graph.SignatureGraph;
import signature.graph.SignatureNode;

public class PathFinder {
    private PathFinder() {
    }

    public static PathResult dijkstra(SignatureGraph graph, int start, int end) {
        return shortestPath(graph, start, end, false, true);
    }

    static PathResult dijkstraWithoutPenLifts(SignatureGraph graph, int start, int end) {
        return shortestPath(graph, start, end, false, false);
    }

    public static PathResult astar(SignatureGraph graph, int start, int end) {
        return shortestPath(graph, start, end, true, true);
    }

    public static PathResult longestShortestPath(SignatureGraph graph) {
        int[] candidates = pathCandidateIds(graph);
        PathResult best = PathResult.notFound();
        for (int i = 0; i < candidates.length; i++) {
            for (int j = i + 1; j < candidates.length; j++) {
                PathResult result = dijkstra(graph, candidates[i], candidates[j]);
                if (result.found && (!best.found || result.distance > best.distance)) {
                    best = result;
                }
            }
        }

        return best;
    }

    static int[] pathCandidateIds(SignatureGraph graph) {
        int[] endpoints = graph.endpointIds();
        return endpoints.length >= 2 ? endpoints : allNodeIds(graph);
    }

    private static PathResult shortestPath(
            SignatureGraph graph,
            int start,
            int end,
            boolean useAstar,
            boolean includePenLift) {
        int nodeCount = graph.nodeCount();
        if (start < 0 || start >= nodeCount || end < 0 || end >= nodeCount) {
            return PathResult.notFound();
        }

        if (start == end) {
            DList<PixelPoint> pixels = new DList<PixelPoint>();
            pixels.add(graph.node(start).point());
            return new PathResult(true, new int[] {start}, 0.0, pixels);
        }

        double[] distance = new double[nodeCount];
        int[] previous = new int[nodeCount];
        boolean[] visited = new boolean[nodeCount];
        Arrays.fill(distance, Double.POSITIVE_INFINITY);
        Arrays.fill(previous, -1);

        distance[start] = 0.0;
        intDoubleMinHeap heap = new intDoubleMinHeap();
        heap.push(start, 0.0);

        while (!heap.isEmpty()) {
            intDoubleMinHeap.HeapEntry entry = heap.pop();
            int current = entry.node;
            if (visited[current]) {
                continue;
            }
            visited[current] = true;

            if (current == end) {
                break;
            }

            for (GraphConnection connection : graph.connections(current)) {
                if (!includePenLift && connection.edge.penLift) {
                    continue;
                }
                int next = connection.targetNodeId;
                double tentative = distance[current] + connection.distance;
                if (tentative >= distance[next]) {
                    continue;
                }

                previous[next] = current;
                distance[next] = tentative;
                double priority = useAstar
                        ? tentative + heuristic(graph.node(next), graph.node(end))
                        : tentative;
                heap.push(next, priority);
            }
        }

        if (Double.isInfinite(distance[end])) {
            return PathResult.notFound();
        }

        int[] path = reconstructPath(previous, start, end);
        return new PathResult(true, path, distance[end], expandPixels(graph, path));
    }

    private static DList<PixelPoint> expandPixels(SignatureGraph graph, int[] nodeIds) {
        DList<PixelPoint> pixels = new DList<PixelPoint>();
        if (nodeIds.length == 0) {
            return pixels;
        }

        for (int i = 0; i < nodeIds.length - 1; i++) {
            int left = nodeIds[i];
            int right = nodeIds[i + 1];
            SignatureEdge edge = graph.edgeBetween(left, right);
            if (edge == null) {
                continue;
            }

            appendEdgePixels(pixels, edge, edge.start != left);
        }

        if (pixels.isEmpty()) {
            pixels.add(graph.node(nodeIds[0]).point());
        }
        return pixels;
    }

    private static void appendEdgePixels(
            DList<PixelPoint> target,
            SignatureEdge edge,
            boolean reverse) {
        int end = reverse ? -1 : edge.pixelCount();
        int step = reverse ? -1 : 1;
        for (int i = reverse ? edge.pixelCount() - 1 : 0; i != end; i += step) {
            addIfNew(target, edge.pixelAt(i));
        }
    }

    private static void addIfNew(DList<PixelPoint> target, PixelPoint point) {
        if (!target.isEmpty()) {
            PixelPoint last = target.get(target.size() - 1);
            if (last.x == point.x && last.y == point.y) {
                return;
            }
        }
        target.add(point);
    }

    private static int[] reconstructPath(int[] previous, int start, int end) {
        int[] reversed = new int[previous.length];
        int count = 0;
        for (int cursor = end; cursor != -1; cursor = previous[cursor]) {
            reversed[count] = cursor;
            count++;
            if (cursor == start) {
                break;
            }
        }

        int[] path = new int[count];
        for (int i = 0; i < count; i++) {
            path[i] = reversed[count - 1 - i];
        }
        return path;
    }

    private static int[] allNodeIds(SignatureGraph graph) {
        int[] ids = new int[graph.nodeCount()];
        for (int i = 0; i < ids.length; i++) {
            ids[i] = i;
        }
        return ids;
    }

    private static double heuristic(SignatureNode a, SignatureNode b) {
        return Math.hypot(a.x - b.x, a.y - b.y);
    }
}
