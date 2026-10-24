package com.mumbaimetro.ui;

import com.mumbaimetro.model.Station;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.ArrayList;
import java.util.List;

import static com.mumbaimetro.ui.UITheme.*;

/**
 * Interactive 60 FPS Mumbai Metro Route Visualizer with Smooth Train Animation.
 * Supports multi-line switching (Line 1 Blue, Line 2A Yellow, Line 7 Red, Line 3 Aqua).
 *
 * All icons and indicators are rendered via Graphics2D vector primitives,
 * preventing any missing emoji font / square tofu boxes on Linux.
 */
public class MetroTrackCanvas extends JPanel {

    public interface StationSelectionListener {
        void onStationSelected(Station station, boolean isOrigin);
    }

    private final List<Station> stations = new ArrayList<>();
    private final List<Point2D.Float> stationPoints = new ArrayList<>();
    private StationSelectionListener selectionListener;

    private Station selectedOrigin;
    private Station selectedDestination;
    private int hoveredStationIndex = -1;

    // Line Colors
    private Color trackPrimaryColor = new Color(37, 99, 235); // Blue by default
    private Color trackGlowColor = new Color(14, 165, 233, 40);
    private String currentLineName = "Line 1 (Blue)";

    // Train animation state
    private final Timer animationTimer;
    private boolean isTrainAnimating = false;
    private float trainProgress = 0f;
    private int trainFromIndex = 0;
    private int trainToIndex = 1;
    private float glowPulse = 0f;
    private boolean glowAscending = true;

    public MetroTrackCanvas(List<Station> initialStations) {
        setStations(initialStations, "LINE_1");
        setPreferredSize(new Dimension(860, 240));
        setMinimumSize(new Dimension(600, 200));
        setBackground(new Color(15, 23, 42)); // Deep Midnight

        // 60 FPS animation loop
        animationTimer = new Timer(16, e -> {
            if (glowAscending) {
                glowPulse += 0.03f;
                if (glowPulse >= 1.0f) glowAscending = false;
            } else {
                glowPulse -= 0.03f;
                if (glowPulse <= 0.0f) glowAscending = true;
            }

            if (isTrainAnimating) {
                trainProgress += 0.012f;
                if (trainProgress >= 1.0f) {
                    trainProgress = 1.0f;
                    isTrainAnimating = false;
                }
            }
            repaint();
        });
        animationTimer.start();

        // Mouse click listener
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int clicked = getStationAtPoint(e.getPoint());
                if (clicked >= 0 && clicked < stations.size()) {
                    Station s = stations.get(clicked);
                    if (e.getButton() == MouseEvent.BUTTON3 || (e.getButton() == MouseEvent.BUTTON1 && e.isShiftDown())) {
                        selectedDestination = s;
                        if (selectionListener != null) selectionListener.onStationSelected(s, false);
                    } else {
                        selectedOrigin = s;
                        if (selectionListener != null) selectionListener.onStationSelected(s, true);
                    }
                    dispatchTrain(selectedOrigin, selectedDestination);
                }
            }
        });

        // Mouse hover listener
        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int oldHover = hoveredStationIndex;
                hoveredStationIndex = getStationAtPoint(e.getPoint());
                if (hoveredStationIndex != oldHover) {
                    setCursor(Cursor.getPredefinedCursor(hoveredStationIndex >= 0 ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
                    repaint();
                }
            }
        });
    }

    public void setStations(List<Station> newStations, String lineCode) {
        this.stations.clear();
        if (newStations != null) {
            this.stations.addAll(newStations);
        }

        // Apply line-specific color branding
        if ("LINE_2A".equalsIgnoreCase(lineCode)) {
            trackPrimaryColor = new Color(234, 179, 8); // Vibrant Yellow
            trackGlowColor = new Color(234, 179, 8, 45);
            currentLineName = "Line 2A (Yellow Line: Dahisar E <-> Andheri W)";
        } else if ("LINE_7".equalsIgnoreCase(lineCode)) {
            trackPrimaryColor = new Color(220, 38, 38); // Crimson Red
            trackGlowColor = new Color(220, 38, 38, 45);
            currentLineName = "Line 7 (Red Line: Dahisar E <-> Gundavali)";
        } else if ("LINE_3".equalsIgnoreCase(lineCode)) {
            trackPrimaryColor = new Color(13, 148, 136); // Aqua Teal
            trackGlowColor = new Color(13, 148, 136, 45);
            currentLineName = "Line 3 (Aqua Underground: Aarey <-> Cuffe Parade)";
        } else {
            trackPrimaryColor = new Color(37, 99, 235); // Classic Blue
            trackGlowColor = new Color(14, 165, 233, 45);
            currentLineName = "Line 1 (Blue Line: Versova <-> Ghatkopar)";
        }

        if (stations.size() >= 2) {
            selectedOrigin = stations.get(0);
            selectedDestination = stations.get(Math.min(3, stations.size() - 1));
        } else {
            selectedOrigin = null;
            selectedDestination = null;
        }

        this.isTrainAnimating = false;
        this.trainProgress = 0f;
        repaint();
    }

    public void setSelectionListener(StationSelectionListener listener) {
        this.selectionListener = listener;
    }

    public void setSelectedStations(Station origin, Station destination) {
        this.selectedOrigin = origin;
        this.selectedDestination = destination;
        repaint();
    }

    public void dispatchTrain(Station from, Station to) {
        if (from == null || to == null) return;
        this.trainFromIndex = stations.indexOf(from);
        this.trainToIndex = stations.indexOf(to);
        if (trainFromIndex < 0) trainFromIndex = 0;
        if (trainToIndex < 0) trainToIndex = Math.max(0, stations.size() - 1);
        this.trainProgress = 0f;
        this.isTrainAnimating = true;
    }

    private int getStationAtPoint(Point p) {
        for (int i = 0; i < stationPoints.size(); i++) {
            Point2D.Float pt = stationPoints.get(i);
            if (pt.distance(p.x, p.y) <= 18) {
                return i;
            }
        }
        return -1;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        applyRenderingHints(g2);

        int w = getWidth();
        int h = getHeight();

        // Background subtle grid
        g2.setColor(new Color(30, 41, 59, 120));
        for (int x = 0; x < w; x += 40) g2.drawLine(x, 0, x, h);
        for (int y = 0; y < h; y += 40) g2.drawLine(0, y, w, y);

        computeStationLayout(w, h);
        drawTrack(g2);
        drawStations(g2);
        drawTrain(g2);
        drawHeaderOverlay(g2, w);

        g2.dispose();
    }

    private void computeStationLayout(int w, int h) {
        stationPoints.clear();
        int count = stations.size();
        if (count == 0) return;

        int marginX = 50;
        float usableW = w - (marginX * 2);
        float baseY = h * 0.58f;

        for (int i = 0; i < count; i++) {
            float x = (count == 1) ? (w / 2f) : (marginX + (i * (usableW / (count - 1))));
            float wave = (float) Math.sin((i / (float) Math.max(1, count - 1)) * Math.PI) * 22f;
            float y = baseY - wave;
            stationPoints.add(new Point2D.Float(x, y));
        }
    }

    private void drawTrack(Graphics2D g2) {
        if (stationPoints.size() < 2) return;

        Path2D.Float trackPath = new Path2D.Float();
        trackPath.moveTo(stationPoints.get(0).x, stationPoints.get(0).y);
        for (int i = 1; i < stationPoints.size(); i++) {
            Point2D.Float pt = stationPoints.get(i);
            Point2D.Float prev = stationPoints.get(i - 1);
            float cx = (prev.x + pt.x) / 2;
            trackPath.quadTo(cx, (prev.y + pt.y) / 2, pt.x, pt.y);
        }

        // Glow Layer
        g2.setColor(trackGlowColor);
        g2.setStroke(new BasicStroke(16f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.draw(trackPath);

        // Primary Rail
        g2.setColor(trackPrimaryColor);
        g2.setStroke(new BasicStroke(7f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.draw(trackPath);

        // Core Highlight Rail
        g2.setColor(Color.WHITE);
        g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.draw(trackPath);
    }

    private void drawStations(Graphics2D g2) {
        int count = stations.size();
        for (int i = 0; i < stationPoints.size(); i++) {
            Point2D.Float pt = stationPoints.get(i);
            Station st = stations.get(i);
            boolean isOrigin = (selectedOrigin != null && selectedOrigin.equals(st));
            boolean isDest = (selectedDestination != null && selectedDestination.equals(st));
            boolean isHover = (i == hoveredStationIndex);

            // Selection Glow Rings
            if (isOrigin || isDest) {
                float ringSize = 22 + (glowPulse * 8);
                Color ringColor = isOrigin ? new Color(34, 197, 94, 130) : new Color(59, 130, 246, 130);
                g2.setColor(ringColor);
                g2.fill(new Ellipse2D.Float(pt.x - ringSize/2, pt.y - ringSize/2, ringSize, ringSize));
            }

            // Node Disc
            int nodeRadius = (isOrigin || isDest || isHover) ? 9 : (count > 15 ? 5 : 7);
            Color nodeBg = isOrigin ? new Color(34, 197, 94) : (isDest ? new Color(59, 130, 246) : Color.WHITE);
            g2.setColor(nodeBg);
            g2.fill(new Ellipse2D.Float(pt.x - nodeRadius, pt.y - nodeRadius, nodeRadius * 2, nodeRadius * 2));

            g2.setColor(new Color(15, 23, 42));
            g2.setStroke(new BasicStroke(2f));
            g2.draw(new Ellipse2D.Float(pt.x - nodeRadius, pt.y - nodeRadius, nodeRadius * 2, nodeRadius * 2));

            // Staggered Station Name Labels
            int fontSize = (count > 15) ? 9 : 10;
            g2.setFont(new Font("Segoe UI", Font.BOLD, isHover || isOrigin || isDest ? (fontSize + 1) : fontSize));

            Color labelColor = isOrigin ? new Color(134, 239, 172) : (isDest ? new Color(147, 197, 253) : new Color(226, 232, 240));
            g2.setColor(labelColor);

            FontMetrics fm = g2.getFontMetrics();
            int strW = fm.stringWidth(st.getName());

            // Multi-tier alternating labels to prevent overcrowding on lines with 17-19 stations
            float labelY;
            if (i % 3 == 0) {
                labelY = pt.y - 14;
            } else if (i % 3 == 1) {
                labelY = pt.y + 24;
            } else {
                labelY = pt.y + 40;
            }

            g2.drawString(st.getName(), pt.x - (strW / 2f), labelY);

            // Distance Tag
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 8));
            g2.setColor(new Color(148, 163, 184));
            String distStr = String.format("%.1f km", st.getDistanceKm());
            int distW = g2.getFontMetrics().stringWidth(distStr);
            float distY = (i % 3 == 0) ? (labelY - 11) : (labelY + 11);
            g2.drawString(distStr, pt.x - (distW / 2f), distY);

            // Interchange Indicator Dot
            if (!st.getInterchange().equals("None")) {
                g2.setColor(new Color(245, 158, 11)); // Amber
                g2.fill(new Ellipse2D.Float(pt.x - 3, pt.y - 3, 6, 6));
            }
        }
    }

    private void drawTrain(Graphics2D g2) {
        if (stationPoints.isEmpty()) return;

        float t = isTrainAnimating ? smoothEase(trainProgress) : 0f;
        Point2D.Float pFrom = stationPoints.get(Math.min(trainFromIndex, stationPoints.size() - 1));
        Point2D.Float pTo = stationPoints.get(Math.min(trainToIndex, stationPoints.size() - 1));

        float trainX = pFrom.x + (pTo.x - pFrom.x) * t;
        float trainY = pFrom.y + (pTo.y - pFrom.y) * t;

        float trainW = 42;
        float trainH = 14;

        // Train glow
        g2.setColor(new Color(trackPrimaryColor.getRed(), trackPrimaryColor.getGreen(), trackPrimaryColor.getBlue(), 90));
        g2.fill(new RoundRectangle2D.Float(trainX - trainW/2 - 4, trainY - trainH/2 - 4, trainW + 8, trainH + 8, 8, 8));

        // Train body
        GradientPaint trainGrad = new GradientPaint(
            trainX - trainW/2, trainY - trainH/2, new Color(248, 250, 252),
            trainX + trainW/2, trainY + trainH/2, new Color(148, 163, 184)
        );
        g2.setPaint(trainGrad);
        g2.fill(new RoundRectangle2D.Float(trainX - trainW/2, trainY - trainH/2, trainW, trainH, 6, 6));

        // Line color stripe
        g2.setColor(trackPrimaryColor);
        g2.fill(new Rectangle2D.Float(trainX - trainW/2, trainY + 1, trainW, 3));

        // Windows
        g2.setColor(new Color(15, 23, 42));
        g2.fill(new RoundRectangle2D.Float(trainX - 16, trainY - 5, 8, 5, 2, 2));
        g2.fill(new RoundRectangle2D.Float(trainX - 5, trainY - 5, 8, 5, 2, 2));
        g2.fill(new RoundRectangle2D.Float(trainX + 6, trainY - 5, 8, 5, 2, 2));

        // Headlight beam
        boolean movingRight = (trainToIndex >= trainFromIndex);
        float lightX = movingRight ? (trainX + trainW/2) : (trainX - trainW/2);
        Polygon beam = new Polygon();
        beam.addPoint((int) lightX, (int) trainY);
        beam.addPoint((int) (lightX + (movingRight ? 35 : -35)), (int) (trainY - 14));
        beam.addPoint((int) (lightX + (movingRight ? 35 : -35)), (int) (trainY + 14));

        g2.setPaint(new GradientPaint(lightX, trainY, new Color(254, 240, 138, 160),
                lightX + (movingRight ? 35 : -35), trainY, new Color(254, 240, 138, 0)));
        g2.fill(beam);
    }

    private void drawHeaderOverlay(Graphics2D g2, int w) {
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Segoe UI", Font.BOLD, 12));
        g2.drawString(currentLineName.toUpperCase(), 16, 20);

        g2.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        g2.setColor(new Color(148, 163, 184));
        g2.drawString("Click: [Left-Click = Origin]  |  [Right-Click = Destination]", 16, 36);

        // Vector Drawn Legend (No square emojis!)
        int legendX = w - 230;
        g2.setColor(new Color(34, 197, 94)); // Green
        g2.fillOval(legendX, 12, 8, 8);
        g2.setColor(Color.WHITE);
        g2.drawString("Origin", legendX + 12, 20);

        g2.setColor(new Color(59, 130, 246)); // Blue
        g2.fillOval(legendX + 65, 12, 8, 8);
        g2.setColor(Color.WHITE);
        g2.drawString("Destination", legendX + 77, 20);

        g2.setColor(new Color(245, 158, 11)); // Amber
        g2.fillOval(legendX + 150, 12, 8, 8);
        g2.setColor(Color.WHITE);
        g2.drawString("Interchange", legendX + 162, 20);
    }

    private float smoothEase(float x) {
        return x < 0.5f ? 4f * x * x * x : 1f - (float) Math.pow(-2f * x + 2f, 3f) / 2f;
    }
}
