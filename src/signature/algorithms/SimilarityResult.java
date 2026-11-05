package signature.algorithms;

public class SimilarityResult {
    public final double score;
    public final boolean accepted;
    public final double topology;
    public final double degreeHistogram;
    public final double edgeLengths;
    public final double pathProfile;
    public final double geometry;

    public SimilarityResult(
            double score,
            boolean accepted,
            double topology,
            double degreeHistogram,
            double edgeLengths,
            double pathProfile,
            double geometry) {
        this.score = score;
        this.accepted = accepted;
        this.topology = topology;
        this.degreeHistogram = degreeHistogram;
        this.edgeLengths = edgeLengths;
        this.pathProfile = pathProfile;
        this.geometry = geometry;
    }
}
