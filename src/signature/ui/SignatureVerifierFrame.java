package signature.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Insets;
import java.io.File;
import java.util.concurrent.ExecutionException;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.SwingWorker;
import javax.swing.filechooser.FileNameExtensionFilter;
import signature.algorithms.PathFinder;
import signature.algorithms.PathResult;
import signature.algorithms.SimilarityAnalyzer;
import signature.algorithms.SimilarityResult;
import signature.processing.SignatureData;

/**
 * @author 223017726
 * @author 222008253
 * @author 224029403
 * @author 222209456
 */
public class SignatureVerifierFrame extends JFrame 
{
    private static final long serialVersionUID = 1L;
    private static final Color BACKGROUND = new Color(242, 245, 248);
    private static final Color PANEL = Color.WHITE;
    private static final Color BORDER = new Color(214, 220, 228);
    private static final Color TEXT = new Color(31, 41, 55);
    private static final Color MUTED = new Color(100, 116, 139);
    private static final Color PRIMARY = new Color(20, 111, 170);
    private static final Color PRIMARY_DARK = new Color(13, 87, 135);

    private final SignatureCanvas referenceCanvas;
    private final SignatureCanvas candidateCanvas;
    private final JEditorPane outputPane;
    private final JLabel statusLabel;
    private JButton compareButton;
    private JButton referencePathButton;
    private JButton candidatePathButton;
    private JButton analyzeButton;
    private JButton clearButton;
    private SignatureData reference;
    private SignatureData candidate;
    private File lastDirectory;

    public SignatureVerifierFrame() {
        super("Signature Graph Verification");
        this.referenceCanvas = new SignatureCanvas("Reference");
        this.candidateCanvas = new SignatureCanvas("Candidate");
        this.outputPane = new JEditorPane();
        this.statusLabel = new JLabel("Ready");

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new java.awt.Dimension(980, 640));
        setSize(1240, 780);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        getContentPane().setBackground(BACKGROUND);

        add(buildHeader(), BorderLayout.NORTH);
        add(buildMainContent(), BorderLayout.CENTER);
        add(buildStatusBar(), BorderLayout.SOUTH);

        writeWelcomeText();
        updateControls();
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout(0, 12));
        header.setBackground(PANEL);
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER),
                BorderFactory.createEmptyBorder(16, 18, 14, 18)));

        JPanel titles = new JPanel(new GridLayout(2, 1, 0, 2));
        titles.setOpaque(false);

        JLabel title = new JLabel("Signature Graph Verification");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 22.0f));
        title.setForeground(TEXT);

        JLabel subtitle = new JLabel("Signature Matching Report");
        subtitle.setFont(subtitle.getFont().deriveFont(13.0f));
        subtitle.setForeground(MUTED);

        titles.add(title);
        titles.add(subtitle);
        header.add(titles, BorderLayout.NORTH);
        header.add(buildToolbar(), BorderLayout.SOUTH);
        return header;
    }

    private JPanel buildToolbar() {
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        toolbar.setOpaque(false);

        JButton loadReference = actionButton("Load Reference", false);
        JButton loadCandidate = actionButton("Load Candidate", false);
        compareButton = actionButton("Compare", true);
        referencePathButton = actionButton("Reference Path", false);
        candidatePathButton = actionButton("Candidate Path", false);
        analyzeButton = actionButton("Analyze", false);
        clearButton = actionButton("Clear", false);

        loadReference.addActionListener(event -> loadSignature(true));
        loadCandidate.addActionListener(event -> loadSignature(false));
        compareButton.addActionListener(event -> compareSignatures());
        referencePathButton.addActionListener(event -> findPath(true));
        candidatePathButton.addActionListener(event -> findPath(false));
        analyzeButton.addActionListener(event -> analyzeGraphs());
        clearButton.addActionListener(event -> clear());

        toolbar.add(loadReference);
        toolbar.add(loadCandidate);
        toolbar.add(compareButton);
        toolbar.add(referencePathButton);
        toolbar.add(candidatePathButton);
        toolbar.add(analyzeButton);
        toolbar.add(clearButton);
        return toolbar;
    }

    private JButton actionButton(String text, boolean primary) {
        JButton button = new JButton(text);
        button.setFocusPainted(false);
        button.setOpaque(true);
        button.setContentAreaFilled(true);
        button.setBorderPainted(true);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setMargin(new Insets(8, 12, 8, 12));
        button.setFont(button.getFont().deriveFont(Font.BOLD, 12.0f));
        if (primary) {
            button.setForeground(Color.WHITE);
            button.setBackground(PRIMARY);
            button.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(PRIMARY_DARK),
                    BorderFactory.createEmptyBorder(6, 10, 6, 10)));
        } else {
            button.setForeground(TEXT);
            button.setBackground(new Color(249, 250, 251));
            button.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(BORDER),
                    BorderFactory.createEmptyBorder(6, 10, 6, 10)));
        }
        return button;
    }

    private JSplitPane buildMainContent() {
        JPanel canvases = new JPanel(new GridLayout(1, 2, 10, 10));
        canvases.setOpaque(false);
        canvases.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 10));
        canvases.add(referenceCanvas);
        canvases.add(candidateCanvas);

        outputPane.setEditable(false);
        outputPane.setContentType("text/html");
        outputPane.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
        outputPane.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        outputPane.setForeground(TEXT);
        outputPane.setBackground(PANEL);
        outputPane.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

        JScrollPane outputScroll = new JScrollPane(outputPane);
        outputScroll.setBorder(BorderFactory.createLineBorder(BORDER));

        JPanel resultsPanel = new JPanel(new BorderLayout(0, 8));
        resultsPanel.setOpaque(false);
        resultsPanel.setBorder(BorderFactory.createEmptyBorder(14, 0, 14, 14));

        JLabel resultsTitle = new JLabel("Signature Match Report");
        resultsTitle.setFont(resultsTitle.getFont().deriveFont(Font.BOLD, 15.0f));
        resultsTitle.setForeground(TEXT);
        resultsPanel.add(resultsTitle, BorderLayout.NORTH);
        resultsPanel.add(outputScroll, BorderLayout.CENTER);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, canvases, resultsPanel);
        splitPane.setBorder(BorderFactory.createEmptyBorder());
        splitPane.setOpaque(false);
        splitPane.setResizeWeight(0.72);
        return splitPane;
     }

    private JPanel buildStatusBar() {
        JPanel statusBar = new JPanel(new BorderLayout());
        statusBar.setBackground(PANEL);
        statusBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER),
                BorderFactory.createEmptyBorder(8, 14, 8, 14)));
        statusLabel.setForeground(MUTED);
        statusLabel.setFont(statusLabel.getFont().deriveFont(12.0f));
        statusBar.add(statusLabel, BorderLayout.WEST);
        return statusBar;
    }

    private void loadSignature(boolean isReference) {
        JFileChooser chooser = new JFileChooser(lastDirectory);
        chooser.setFileFilter(new FileNameExtensionFilter(
                "Signature images (*.png, *.jpg, *.jpeg, *.bmp)",
                "png", "jpg", "jpeg", "bmp"));

        int choice = chooser.showOpenDialog(this);
        if (choice != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File file = chooser.getSelectedFile();
        lastDirectory = file.getParentFile();
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        setStatus("Loading " + (isReference ? "reference" : "candidate") + ": " + file.getName());

        new SwingWorker<SignatureData, Void>() {
            @Override
            protected SignatureData doInBackground() throws Exception {
                return SignatureData.load(file);
            }

            @Override
            protected void done() {
                try {
                    SignatureData data = get();
                    if (isReference) {
                        reference = data;
                        referenceCanvas.setData(data);
                    } else {
                        candidate = data;
                        candidateCanvas.setData(data);
                    }

                    writeLoadedText(isReference ? "Reference" : "Candidate", data);
                    setStatus((isReference ? "Reference" : "Candidate")
                            + " loaded: " + data.displayName());
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    setStatus("Loading interrupted");
                } catch (ExecutionException ex) {
                    showError("Could not load image", ex.getCause());
                    setStatus("Could not load " + file.getName());
                } finally {
                    setCursor(Cursor.getDefaultCursor());
                    updateControls();
                }
            }
        }.execute();
    }

    private void compareSignatures() {
        if (reference == null || candidate == null) {
            JOptionPane.showMessageDialog(
                    this,
                    "Load both a reference and candidate signature first.",
                    "Missing signatures",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        referenceCanvas.clearPath();
        candidateCanvas.clearPath();
        SimilarityResult result = SimilarityAnalyzer.compare(
                reference.graph,
                reference.image,
                candidate.graph,
                candidate.image);
        Verdict verdict = verdictFor(result.score);

        writeComparisonText(result);
        setStatus("Comparison complete: " + verdict.title
                + " with score " + formatPercent(result.score));
    }

    private void findPath(boolean useReference) {
        SignatureData data = useReference ? reference : candidate;
        SignatureCanvas canvas = useReference ? referenceCanvas : candidateCanvas;

        if (data == null) {
            JOptionPane.showMessageDialog(
                    this,
                    "Load the " + (useReference ? "reference" : "candidate") + " signature first.",
                    "Missing signature",
					
                    JOptionPane.INFORMATION_MESSAGE);
            return;
         }

        PathResult result = PathFinder.longestShortestPath(data.graph);
        canvas.setHighlightedPath(result);

        if (!result.found) {
            setResultsHtml(
                    "<h2>No Path Found</h2>"
                            + "<p>The graph does not contain enough connected points to trace a path.</p>");
            setStatus("No path found for " + data.displayName());
            return;
        }

        setResultsHtml(
                "<p class='eyebrow'>" + (useReference ? "Reference" : "Candidate") + " Signature</p>"
                        + "<h2>Longest Path</h2>"
                        + "<div class='metric'>"
                        + "<span>Distance</span><strong>" + String.format("%.2f", result.distance) + "</strong>"
                        + "</div>"
                        + "<div class='metric'>"
                        + "<span>Nodes in path</span><strong>" + result.nodeIds.length + "</strong>"
                        + "</div>"
                        + "<p class='muted'>Image: " + escapeHtml(data.displayName()) + "</p>");
        setStatus((useReference ? "Reference" : "Candidate")
                + " path highlighted: " + String.format("%.3f", result.distance));
    }

    private void analyzeGraphs() {
        if (reference == null && candidate == null) {
            setResultsHtml("<h2>No Signature Loaded</h2><p>Load at least one signature to view its graph details.</p>");
            setStatus("No signature loaded");
            return;
        }
        StringBuilder builder = new StringBuilder();
        builder.append("<h2>Graph Summary</h2>");
        if (reference != null) {
            appendGraphAnalysis(builder, "Reference", reference);
        }
        if (candidate != null) {
            appendGraphAnalysis(builder, "Candidate", candidate);
        }
        setResultsHtml(builder.toString());
        setStatus("Graph analysis updated");
    }

    private void appendGraphAnalysis(StringBuilder builder, String label, SignatureData data) {
        builder.append("<table class='stats'>")
                .append("<tr><th colspan='2'>").append(label).append("</th></tr>")
                .append("<tr><td>Image</td><td>").append(escapeHtml(data.displayName())).append("</td></tr>")
                .append("<tr><td>Nodes</td><td>").append(data.graph.nodeCount()).append("</td></tr>")
                .append("<tr><td>Edge length</td><td>").append(format(data.graph.totalEdgeLength())).append("</td></tr>")
                .append("</table>");
    }

    private void clear() {
        reference = null;
        candidate = null;
        referenceCanvas.setData(null);
        candidateCanvas.setData(null);
        writeWelcomeText();
        updateControls();
        setStatus("Cleared");
    }

    private void writeWelcomeText() {
        setResultsHtml(
                "<h2>Ready</h2>"
                        + "<p>Load a reference signature and a candidate signature, then press Compare.</p>"
                        + "<table class='stats'>"
                        + "<tr><td>Reference</td><td>Not loaded</td></tr>"
                        + "<tr><td>Candidate</td><td>Not loaded</td></tr>"
                        + "</table>");
    }

    private void writeLoadedText(String label, SignatureData data) {
        setResultsHtml(
                "<p class='eyebrow'>" + label + "</p>"
                        + "<h2>Signature Loaded</h2>"
                        + "<p>" + escapeHtml(data.displayName()) + "</p>"
                        + "<table class='stats'>"
                        + "<tr><td>Nodes</td><td>" + data.graph.nodeCount() + "</td></tr>"
                        + "<tr><td>Edge length</td><td>" + format(data.graph.totalEdgeLength()) + "</td></tr>"
                        + "</table>");
    }

    private void writeComparisonText(SimilarityResult result) {
        Verdict verdict = verdictFor(result.score);
        String score = formatPercent(result.score);

        StringBuilder builder = new StringBuilder();
        builder.append("<p class='eyebrow'>Similarity Result</p>")
                .append("<div class='score'>").append(score).append("</div>")
                .append("<h2>").append(verdict.title).append("</h2>")
                .append("<p>").append(verdict.message).append("</p>")
                .append("<table class='stats'>")
                .append("<tr><th></th><th>Reference</th><th>Candidate</th></tr>")
                .append("<tr><td>Image</td><td>")
                .append(escapeHtml(reference.displayName()))
                .append("</td><td>")
                .append(escapeHtml(candidate.displayName()))
                .append("</td></tr>")
                .append("<tr><td>Nodes</td><td>")
                .append(reference.graph.nodeCount())
                .append("</td><td>")
                .append(candidate.graph.nodeCount())
                .append("</td></tr>")
                .append("<tr><td>Edge length</td><td>")
                .append(format(reference.graph.totalEdgeLength()))
                .append("</td><td>")
                .append(format(candidate.graph.totalEdgeLength()))
                .append("</td></tr>")
                .append("</table>");

        setResultsHtml(builder.toString());
    }

    private void setResultsHtml(String body) {
        outputPane.setText(
                "<html><head><style>"
                        + "body{font-family:Segoe UI,Arial,sans-serif;color:#1f2937;background:#ffffff;margin:0;padding:18px;}"
                        + "h2{font-size:21px;margin:6px 0 8px 0;color:#111827;}"
                        + "p{font-size:13px;line-height:1.45;margin:6px 0 14px 0;}"
                        + ".eyebrow{font-size:11px;text-transform:uppercase;letter-spacing:1px;color:#64748b;font-weight:bold;margin:0 0 6px 0;}"
                        + ".muted{color:#64748b;}"
                        + ".score{font-size:44px;font-weight:bold;color:#0f6fa8;margin:2px 0 6px 0;}"
                        + ".metric{border:1px solid #d6dce4;padding:10px;margin:8px 0;background:#f8fafc;}"
                        + ".metric span{color:#64748b;}"
                        + ".metric strong{display:block;font-size:22px;color:#111827;margin-top:4px;}"
                        + "table.stats{width:100%;border-collapse:collapse;margin-top:12px;font-size:12px;}"
                        + ".stats th{background:#f1f5f9;text-align:left;color:#334155;padding:8px;border:1px solid #d6dce4;}"
                        + ".stats td{padding:8px;border:1px solid #d6dce4;vertical-align:top;}"
                        + ".stats td:first-child{color:#64748b;font-weight:bold;}"
                        + "</style></head><body>" + body + "</body></html>");
        outputPane.setCaretPosition(0);
    }

    private Verdict verdictFor(double score) {
        if (score >= SimilarityAnalyzer.DEFAULT_THRESHOLD) {
            return new Verdict(
                    "Very Similar",
                    "The signatures are very similar and are likely to belong to the same person.");
        }
        if (score >= 0.45) {
            return new Verdict(
                    "Kind Of Similar",
                    "The signatures share several graph features, but the match is not very strong.");
        }
        if (score >= 0.30) {
            return new Verdict(
                    "Low Similarity",
                    "The signatures share only a few graph features.");
        }
        return new Verdict(
                "Very Low Similarity",
                "The signatures do not show enough similarity to be considered a match.");
    }

    private void updateControls() {
        boolean hasReference = reference != null;
        boolean hasCandidate = candidate != null;
        if (compareButton != null) {
            compareButton.setEnabled(hasReference && hasCandidate);
        }
        if (referencePathButton != null) {
            referencePathButton.setEnabled(hasReference);
        }
        if (candidatePathButton != null) {
            candidatePathButton.setEnabled(hasCandidate);
        }
        if (analyzeButton != null) {
            analyzeButton.setEnabled(hasReference || hasCandidate);
        }
        if (clearButton != null) {
            clearButton.setEnabled(hasReference || hasCandidate);
        }
    }

    private void setStatus(String message) {
        statusLabel.setText(message);
    }

    private String format(double value) {
        return String.format("%.2f", value);
    }

    private String formatPercent(double value) {
        return String.format("%.2f%%", value * 100.0);
    }

    private String escapeHtml(String text) {
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private void showError(String title, Throwable ex) {
        JOptionPane.showMessageDialog(
                this,
            ex.getMessage() == null ? ex.toString() : ex.getMessage(),
                title,
                JOptionPane.ERROR_MESSAGE);
    }

    private static class Verdict {
        final String title;
        final String message;

        Verdict(String title, String message) {
            this.title = title;
            this.message = message;
        }
    }
}
