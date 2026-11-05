package signature.algorithms;

import java.util.Arrays;
import signature.graph.SignatureEdge;
import signature.graph.SignatureGraph;
import signature.graph.SignatureNode;
import signature.image.BinaryImage;

public class SimilarityAnalyzer {
    public static final double DEFAULT_THRESHOLD = 0.60;
    private static final int SHAPE_GRID_SIZE = 64;

    private SimilarityAnalyzer() {
    }

    public static SimilarityResult compare(SignatureGraph reference, SignatureGraph candidate) {
        return compare(reference, candidate, DEFAULT_THRESHOLD);
    }

    public static SimilarityResult compare(
            SignatureGraph reference,
            BinaryImage referenceImage,
            SignatureGraph candidate,
            BinaryImage candidateImage) {
        return compare(reference, referenceImage, candidate, candidateImage, DEFAULT_THRESHOLD);
    }

    public static SimilarityResult compare(
            SignatureGraph reference,
            SignatureGraph candidate,
            double threshold) {
        if (reference.nodeCount() == 0
                || candidate.nodeCount() == 0
                || reference.edgeCount() == 0
                || candidate.edgeCount() == 0) {
            return new SimilarityResult(
                    0.0,
                    false,
                    0.0,
                    0.0,
                    0.0,
                    0.0,
                    0.0);
        }

        double topology = topologyScore(reference, candidate);
        double degreeHistogram = cosine(degreeHistogram(reference), degreeHistogram(candidate));
        double edgeLengths = distributionScore(
                normalizedEdgeLengths(reference),
                normalizedEdgeLengths(candidate));
        double pathProfile = distributionScore(
                endpointPathProfile(reference),
                endpointPathProfile(candidate));
        double geometry = geometryScore(reference, candidate);

        double score = (0.25 * topology)
                + (0.15 * degreeHistogram)
                + (0.20 * edgeLengths)
                + (0.20 * pathProfile)
                + (0.20 * geometry);
        score = clamp(score);

        return new SimilarityResult(
                score,
                score >= threshold,
                topology,
                degreeHistogram,
                edgeLengths,
                pathProfile,
                geometry);
    }

    public static SimilarityResult compare(
            SignatureGraph reference,
            BinaryImage referenceImage,
            SignatureGraph candidate,
            BinaryImage candidateImage,
            double threshold) {
        SimilarityResult graphResult = compare(reference, candidate, threshold);
        if (referenceImage == null || candidateImage == null || graphResult.score == 0.0) {
            return graphResult;
        }

        double shape = rasterShapeScore(referenceImage, candidateImage);
        double geometry = (0.25 * graphResult.geometry) + (0.75 * shape);
        double score = (0.15 * graphResult.topology)
                + (0.05 * graphResult.degreeHistogram)
                + (0.15 * graphResult.edgeLengths)
                + (0.15 * graphResult.pathProfile)
                + (0.50 * geometry);

        if (geometry < 0.55) {
            score = Math.min(score, 0.40);
        } else if (geometry < 0.70) {
            score = Math.min(score, 0.55);
        }
        score = confidenceCalibration(score);

        return new SimilarityResult(
                clamp(score),
                score >= threshold,
                graphResult.topology,
                graphResult.degreeHistogram,
                graphResult.edgeLengths,
                graphResult.pathProfile,
                geometry);
    }

    private static double topologyScore(SignatureGraph a, SignatureGraph b) {
        double total = countScore(a.nodeCount(), b.nodeCount())
                + countScore(a.structuralEdgeCount(), b.structuralEdgeCount())
                + countScore(a.endpointIds(false).length, b.endpointIds(false).length)
                + countScore(a.junctionIds(false).length, b.junctionIds(false).length)
                + countScore(a.structuralConnectedComponents(), b.structuralConnectedComponents());
        return total / 5.0;
    }

    private static double[] degreeHistogram(SignatureGraph graph) {
        double[] histogram = new double[7];
        for (SignatureNode node : graph.nodes()) {
            int degree = Math.min(6, graph.graphDegree(node.id, false));
            histogram[degree] += 1.0;
        }
        return histogram;
    }

    private static double[] normalizedEdgeLengths(SignatureGraph graph) {
        double[] lengths = new double[graph.structuralEdgeCount()];
        double scale = graphScale(graph);
        int index = 0;
        for (SignatureEdge edge : graph.edges()) {
            if (!edge.penLift) {
                lengths[index++] = edge.length / scale;
            }
        }
        sortDescending(lengths);
        return lengths;
    }

    private static double[] endpointPathProfile(SignatureGraph graph) {
        int[] endpoints = graph.endpointIds(false);
        if (endpoints.length < 2) {
            endpoints = PathFinder.pathCandidateIds(graph);
        }
        if (endpoints.length < 2) {
            return new double[0];
        }

        double[] profile = new double[(endpoints.length * (endpoints.length - 1)) / 2];
        int count = 0;
        double scale = graphScale(graph);
        for (int i = 0; i < endpoints.length; i++) {
            for (int j = i + 1; j < endpoints.length; j++) {
                PathResult path = PathFinder.dijkstraWithoutPenLifts(graph, endpoints[i], endpoints[j]);
                if (path.found) {
                    profile[count] = path.distance / scale;
                    count++;
                }
            }
        }

        double[] compact = Arrays.copyOf(profile, count);
        sortDescending(compact);
        return compact;
    }

    private static double geometryScore(SignatureGraph a, SignatureGraph b) {
        if (a.nodeCount() == 0 && b.nodeCount() == 0) {
            return 1.0;
        }

        if (a.nodeCount() == 0 || b.nodeCount() == 0) {
            return 0.0;
        }

        double[][] pointsA = normalizedNodePoints(a);
        double[][] pointsB = normalizedNodePoints(b);
        double distance = (averageNearestDistance(pointsA, pointsB)
                + averageNearestDistance(pointsB, pointsA)) / 2.0;
        return clamp(1.0 - (distance / Math.sqrt(2.0)));
    }

    private static double rasterShapeScore(BinaryImage a, BinaryImage b) {
        boolean[][] left = rasterize(a, SHAPE_GRID_SIZE);
        boolean[][] right = rasterize(b, SHAPE_GRID_SIZE);
        double leftToRight = averageDistance(left, right);
        double rightToLeft = averageDistance(right, left);
        double chamfer = Math.exp(-4.0 * ((leftToRight + rightToLeft) / 2.0));
        double projections = projectionScore(left, right);
        return clamp((0.70 * chamfer) + (0.30 * projections));
    }

    private static boolean[][] rasterize(BinaryImage image, int size) {
        boolean[][] result = new boolean[size][size];
        double scale = Math.min(
                (size - 1.0) / Math.max(1, image.width()),
                (size - 1.0) / Math.max(1, image.height()));
        int offsetX = (int) Math.round((size - (image.width() * scale)) / 2.0);
        int offsetY = (int) Math.round((size - (image.height() * scale)) / 2.0);
        for (int y = 0; y < image.height(); y++) {
            for (int x = 0; x < image.width(); x++) {
                if (!image.isInk(x, y)) {
                    continue;
                }
                int rasterX = Math.min(size - 1, offsetX + (int) Math.round(x * scale));
                int rasterY = Math.min(size - 1, offsetY + (int) Math.round(y * scale));
                result[rasterY][rasterX] = true;
            }
        }
        return result;
    }

    private static double averageDistance(boolean[][] source, boolean[][] target) {
        int size = source.length;
        int targetCount = 0;
        for (boolean[] row : target) {
            for (boolean pixel : row) {
                if (pixel) {
                    targetCount++;
                }
            }
        }
        if (targetCount == 0) {
            return 1.0;
        }

        double total = 0.0;
        int sourceCount = 0;
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                if (!source[y][x]) {
                    continue;
                }
                sourceCount++;
                int nearest = size * size;
                for (int targetY = 0; targetY < size; targetY++) {
                    for (int targetX = 0; targetX < size; targetX++) {
                        if (!target[targetY][targetX]) {
                            continue;
                        }
                        int dx = x - targetX;
                        int dy = y - targetY;
                        nearest = Math.min(nearest, (dx * dx) + (dy * dy));
                    }
                }
                total += Math.sqrt(nearest) / Math.sqrt(2.0 * size * size);
            }
        }
        return sourceCount == 0 ? 1.0 : total / sourceCount;
    }

    private static double projectionScore(boolean[][] left, boolean[][] right) {
        int size = left.length;
        double difference = 0.0;
        double baseline = 0.0;
        for (int index = 0; index < size; index++) {
            int leftRow = 0;
            int rightRow = 0;
            int leftColumn = 0;
            int rightColumn = 0;
            for (int offset = 0; offset < size; offset++) {
                if (left[index][offset]) {
                    leftRow++;
                }
                if (right[index][offset]) {
                    rightRow++;
                }
                if (left[offset][index]) {
                    leftColumn++;
                }
                if (right[offset][index]) {
                    rightColumn++;
                }
            }
            difference += Math.abs(leftRow - rightRow) + Math.abs(leftColumn - rightColumn);
            baseline += Math.max(leftRow, rightRow) + Math.max(leftColumn, rightColumn);
        }
        return baseline == 0.0 ? 0.0 : clamp(1.0 - (difference / baseline));
    }

    private static double[][] normalizedNodePoints(SignatureGraph graph) {
        SignatureGraph.Bounds bounds = graph.bounds();
        double width = Math.max(1.0, bounds.maxX - bounds.minX);
        double height = Math.max(1.0, bounds.maxY - bounds.minY);
        double[][] points = new double[graph.nodeCount()][2];
        for (int i = 0; i < graph.nodeCount(); i++) {
            SignatureNode node = graph.node(i);
            points[i][0] = (node.x - bounds.minX) / width;
            points[i][1] = (node.y - bounds.minY) / height;
        }
        return points;
    }

    private static double averageNearestDistance(double[][] source, double[][] target) {
        double total = 0.0;
        for (double[] point : source) {
            double best = Double.POSITIVE_INFINITY;
            for (double[] other : target) {
                best = Math.min(best, Math.hypot(point[0] - other[0], point[1] - other[1]));
            }
            total += best;
        }
        return total / source.length;
    }

    private static double distributionScore(double[] a, double[] b) {
        if (a.length == 0 && b.length == 0) {
            return 1.0;
        }
        if (a.length == 0 || b.length == 0) {
            return 0.0;
        }

        int length = Math.max(a.length, b.length);
        double difference = 0.0;
        double baseline = 0.0;
        for (int i = 0; i < length; i++) {
            double left = i < a.length ? a[i] : 0.0;
            double right = i < b.length ? b[i] : 0.0;
            difference += Math.abs(left - right);
            baseline += Math.max(left, right);
        }
        return baseline == 0.0 ? 1.0 : clamp(1.0 - (difference / baseline));
    }

    private static double countScore(int a, int b) {
        return a == 0 && b == 0
                ? 1.0
                : clamp(1.0 - (Math.abs(a - b) / (double) Math.max(1, Math.max(a, b))));
    }

    private static double cosine(double[] a, double[] b) {
        double numerator = 0.0;
        double aNorm = 0.0;
        double bNorm = 0.0;
        for (int i = 0; i < a.length; i++) {
            numerator += a[i] * b[i];
            aNorm += a[i] * a[i];
            bNorm += b[i] * b[i];
        }
        if (aNorm == 0.0 || bNorm == 0.0) {
            return aNorm == bNorm ? 1.0 : 0.0;
        }
        return clamp(numerator / (Math.sqrt(aNorm) * Math.sqrt(bNorm)));
    }

    private static double graphScale(SignatureGraph graph) {
        return Math.max(1.0, graph.bounds().diagonal());
    }

    private static void sortDescending(double[] values) {
        Arrays.sort(values);
        for (int i = 0, j = values.length - 1; i < j; i++, j--) {
            double tmp = values[i];
            values[i] = values[j];
            values[j] = tmp;
        }
    }

    private static double clamp(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }

    private static double confidenceCalibration(double score) {
        return clamp((score - 0.35) / 0.65);
    }
}
