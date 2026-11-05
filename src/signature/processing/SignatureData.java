package signature.processing;

import java.io.File;
import java.io.IOException;
import signature.graph.SignatureGraph;
import signature.image.BinaryImage;
import signature.image.ImageLoader;

public class SignatureData {
    public final File file;
    public final BinaryImage image;
    public final SignatureGraph graph;

    private SignatureData(File file, BinaryImage image, SignatureGraph graph) {
        this.file = file;
        this.image = image;
        this.graph = graph;
    }

    public static SignatureData load(File file) throws IOException {
        BinaryImage image = ImageLoader.loadSignature(file);
        SignatureGraph graph = GraphExtractor.extract(image);
        return new SignatureData(file, image, graph);
    }

    public String displayName() {
        return file == null ? "No file" : file.getName();
    }
}
