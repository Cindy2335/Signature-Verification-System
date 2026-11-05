package signature.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import signature.algorithms.PathResult;
import signature.graph.PixelPoint;
import signature.graph.SignatureEdge;
import signature.graph.SignatureNode;
import signature.image.BinaryImage;
import signature.processing.SignatureData;

public class SignatureCanvas extends javax.swing.JPanel 
{
    private static final long serialVersionUID = 1L;
    private static final Color BACKGROUND = new Color(242, 245, 248);
    private static final Color PANEL = Color.WHITE;
    private static final Color BORDER = new Color(214, 220, 228);
    private static final Color TEXT = new Color(31, 41, 55);
    private static final Color MUTED = new Color(100, 116, 139);
    private static final Color GRID = new Color(233, 238, 244);
    private static final Color INK = new Color(22, 27, 34);
    private static final Color EDGE = new Color(20, 111, 170, 150);
    private static final Color PATH = new Color(208, 63, 63, 230);
    private static final int MAX_VISIBLE_NODE_MARKERS = 60;

    private SignatureData data;
    private PathResult highlightedPath;
    private final String title;

    public SignatureCanvas(String title) {
        this.title = title;
        setBackground(BACKGROUND);
        setPreferredSize(new Dimension(520, 360));
    }

    public void setData(SignatureData data) {
        this.data = data;
        this.highlightedPath = null;
        repaint();
     }

    public void setHighlightedPath(PathResult highlightedPath) {
        this.highlightedPath = highlightedPath;
        repaint();
    }

    public void clearPath() {
        this.highlightedPath = null;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        drawPanel(g);

        drawHeader(g);
        if (data == null) {
            drawEmptyMessage(g);
            g.dispose();
            return;
        }

        Viewport viewport = viewport(data.image);
        drawImage(g, data.image, viewport);
        drawGraph(g, viewport);
        drawPath(g, viewport);
        drawNodes(g, viewport);
        drawFooter(g);

        g.dispose();
    }

    private void drawPanel(Graphics2D g) {
        int arc = 8;
        g.setColor(PANEL);
        g.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);
        g.setColor(BORDER);
        g.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);
        g.setColor(GRID);
        g.drawLine(1, 46, getWidth() - 2, 46);
    }

    private void drawHeader(Graphics2D g) {
        g.setColor(TEXT);
        g.setFont(getFont().deriveFont(Font.BOLD, 15.0f));
        g.drawString(title, 16, 29);

        if (data != null) {
            g.setFont(getFont().deriveFont(12.0f));
            g.setColor(MUTED);
            String fileName = shorten(data.displayName(), Math.max(120, getWidth() - 210), g.getFontMetrics());
            g.drawString(fileName, 16, 44);
        }

        drawBadge(g);
    }

    private void drawBadge(Graphics2D g) {
        String badge = "Empty";
        Color fill = new Color(241, 245, 249);
        Color text = MUTED;
        if (data != null && highlightedPath != null && highlightedPath.found) {
            badge = "Path";
            fill = new Color(255, 238, 235);
            text = PATH;
        } else if (data != null) {
            badge = "Graph";
            fill = new Color(232, 244, 252);
            text = new Color(14, 104, 161);
        }

        g.setFont(getFont().deriveFont(Font.BOLD, 11.0f));
        FontMetrics metrics = g.getFontMetrics();
        int width = metrics.stringWidth(badge) + 20;
        int height = 22;
        int x = Math.max(16, getWidth() - width - 16);
        int y = 13;
        g.setColor(fill);
        g.fillRoundRect(x, y, width, height, 8, 8);
        g.setColor(text);
        g.drawString(badge, x + 10, y + 15);
    }

    private void drawEmptyMessage(Graphics2D g) {
        int emptyX = 28;
        int emptyY = 74;
        int width = Math.max(1, getWidth() - 56);
        int height = Math.max(1, getHeight() - 112);

        g.setColor(new Color(248, 250, 252));
        g.fillRoundRect(emptyX, emptyY, width, height, 8, 8);
        g.setColor(BORDER);
        g.setStroke(new BasicStroke(
                1.2f,
                BasicStroke.CAP_BUTT,
                BasicStroke.JOIN_ROUND,
                1.0f,
                new float[] {5.0f, 5.0f},
                0.0f));
        g.drawRoundRect(emptyX, emptyY, width, height, 8, 8);

        String message = "No signature loaded";
        g.setFont(getFont().deriveFont(Font.BOLD, 15.0f));
        FontMetrics metrics = g.getFontMetrics();
        int x = (getWidth() - metrics.stringWidth(message)) / 2;
        int y = getHeight() / 2;
        g.setColor(TEXT);
        g.drawString(message, Math.max(16, x), y);
    }

    private Viewport viewport(BinaryImage image) {
        int padding = 30;
        int top = 66;
        int bottom = 44;
        int availableWidth = Math.max(1, getWidth() - (padding * 2));
        int availableHeight = Math.max(1, getHeight() - top - bottom);

        double scale = Math.min(
                availableWidth / (double) Math.max(1, image.width()),
                availableHeight / (double) Math.max(1, image.height()));
        scale = Math.max(0.15, Math.min(8.0, scale));

        int drawWidth = (int) Math.round(image.width() * scale);
        int drawHeight = (int) Math.round(image.height() * scale);

        int x = (getWidth() - drawWidth) / 2;
        int y = top + ((availableHeight - drawHeight) / 2);
        return new Viewport(x, y, scale);
    }

    private void drawImage(Graphics2D g, BinaryImage image, Viewport viewport) {
        int imageWidth = (int) Math.round(image.width() * viewport.scale);
        int imageHeight = (int) Math.round(image.height() * viewport.scale);

        g.setColor(new Color(222, 228, 236, 120));
        g.fillRoundRect(viewport.x + 3, viewport.y + 4, imageWidth, imageHeight, 8, 8);
        g.setColor(Color.WHITE);
        g.fillRect(viewport.x, viewport.y, imageWidth, imageHeight);

        g.setColor(INK);
        int pixelSize = Math.max(1, (int) Math.ceil(viewport.scale));

        for (int y = 0; y < image.height(); y++) {
            for (int x = 0; x < image.width(); x++) {
                if (image.isInk(x, y)) {
                    g.fillRect(
                            viewport.toScreenX(x),
                            viewport.toScreenY(y),
                            pixelSize,
                            pixelSize);
                }
            }
        }

        g.setColor(BORDER);
        g.drawRect(viewport.x, viewport.y, imageWidth, imageHeight);
    }

    private void drawGraph(Graphics2D g, Viewport viewport) {
        g.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(EDGE);
        for (SignatureEdge edge : data.graph.edges()) {
            if (edge.penLift) {
                continue;
            }
            drawEdgePixels(g, edge, viewport);
        }
    }

    private void drawPath(Graphics2D g, Viewport viewport) {
        if (highlightedPath == null || !highlightedPath.found || highlightedPath.pixels.size() < 2) {
            return;
        }

        g.setStroke(new BasicStroke(3.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(PATH);
        PixelPoint previous = highlightedPath.pixels.get(0);
        for (int i = 1; i < highlightedPath.pixels.size(); i++) {
            PixelPoint current = highlightedPath.pixels.get(i);
            g.drawLine(
                    viewport.toScreenX(previous.x),
                    viewport.toScreenY(previous.y),
                    viewport.toScreenX(current.x),
                    viewport.toScreenY(current.y));
            previous = current;
        }
		
    }

    private void drawNodes(Graphics2D g, Viewport viewport) {
        if (data.graph.nodeCount() > MAX_VISIBLE_NODE_MARKERS) {
            return;
        }
        for (SignatureNode node : data.graph.nodes()) {
            if (!"endpoint".equals(node.kind) && !"junction".equals(node.kind)) {
                continue;
            }
            Color color = nodeColor(node.kind);
            int radius = "endpoint".equals(node.kind) ? 4 : 3;
            int x = viewport.toScreenX(node.x);
            int y = viewport.toScreenY(node.y);
            g.setColor(color);
            g.fillOval(x - radius, y - radius, radius * 2, radius * 2);
            g.setColor(Color.WHITE);
            g.drawOval(x - radius, y - radius, radius * 2, radius * 2);
        }
    }

    private void drawEdgePixels(Graphics2D g, SignatureEdge edge, Viewport viewport) {
        if (edge.pixelCount() < 2) {
            return;
        }
        PixelPoint previous = edge.pixelAt(0);
        for (int i = 1; i < edge.pixelCount(); i++) {
            PixelPoint current = edge.pixelAt(i);
            g.drawLine(
                    viewport.toScreenX(previous.x),
                    viewport.toScreenY(previous.y),
                    viewport.toScreenX(current.x),
                    viewport.toScreenY(current.y));
            previous = current;
        }
    }

    private Color nodeColor(String kind) {
        if ("endpoint".equals(kind)) {
            return new Color(23, 150, 90, 185);
        }
        if ("junction".equals(kind)) {
            return new Color(30, 100, 210, 175);
        }
        if ("cycle_anchor".equals(kind)) {
            return new Color(180, 130, 20);
        }
        return new Color(110, 110, 110);
    }

    private void drawFooter(Graphics2D g) {
        String summary = "Nodes " + data.graph.nodeCount()
                + "   Edge length " + String.format("%.2f", data.graph.totalEdgeLength());
        g.setFont(getFont().deriveFont(12.0f));
        g.setColor(MUTED);
        g.drawString(summary, 16, Math.max(62, getHeight() - 16));
    }

    private String shorten(String text, int maxWidth, FontMetrics metrics) {
        if (metrics.stringWidth(text) <= maxWidth) {
            return text;
        }
        String suffix = "...";
        int suffixWidth = metrics.stringWidth(suffix);
        int keep = text.length();
        while (keep > 0 && metrics.stringWidth(text.substring(0, keep)) + suffixWidth > maxWidth) {
            keep--;
        }
        return keep <= 0 ? suffix : text.substring(0, keep) + suffix;
    }

    private static class Viewport {
        final int x;
        final int y;
        final double scale;

        Viewport(int x, int y, double scale) {
            this.x = x;
            this.y = y;
            this.scale = scale;
        }

        int toScreenX(int imageX) {
            return x + (int) Math.round(imageX * scale);
        }

        int toScreenY(int imageY) {
            return y + (int) Math.round(imageY * scale);
        }
    }
}
