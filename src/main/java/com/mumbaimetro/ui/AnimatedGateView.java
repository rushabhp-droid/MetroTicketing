package com.mumbaimetro.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;

import static com.mumbaimetro.ui.UITheme.*;

/**
 * 3D Simulated Automated Fare Collection (AFC) Turnstile Gate with Physical Sliding Flaps.
 *
 * All status icons (arrows, checks, crosses) are rendered as pure Graphics2D vector shapes,
 * ensuring clean rendering on all Linux desktop environments without missing glyph boxes.
 */
public class AnimatedGateView extends JPanel {

    public enum GateState {
        IDLE,
        OPENING,
        OPEN,
        CLOSING,
        DENIED_SHAKE
    }

    private GateState state = GateState.IDLE;
    private float flapOpenProgress = 0.0f; // 0.0 = fully closed, 1.0 = fully open
    private float shakeOffset = 0.0f;
    private int shakeTimer = 0;

    private String displayMessage = "READY FOR CARD TAP";
    private String passengerInfo = "Present Smart Card at RFID Sensor";
    private Color ledColor = new Color(59, 130, 246); // Blue idle

    private final Timer gateLoop;
    private Timer autoCloseTimer;

    public AnimatedGateView() {
        setPreferredSize(new Dimension(500, 260));
        setMinimumSize(new Dimension(380, 220));
        setBackground(new Color(15, 23, 42)); // Midnight Slate

        // 60 FPS physics & render loop
        gateLoop = new Timer(16, e -> {
            boolean needRepaint = false;

            if (state == GateState.OPENING) {
                flapOpenProgress += 0.07f;
                if (flapOpenProgress >= 1.0f) {
                    flapOpenProgress = 1.0f;
                    state = GateState.OPEN;
                }
                needRepaint = true;
            } else if (state == GateState.CLOSING) {
                flapOpenProgress -= 0.05f;
                if (flapOpenProgress <= 0.0f) {
                    flapOpenProgress = 0.0f;
                    state = GateState.IDLE;
                    ledColor = new Color(59, 130, 246);
                    displayMessage = "READY FOR CARD TAP";
                    passengerInfo = "Present Smart Card at RFID Sensor";
                }
                needRepaint = true;
            } else if (state == GateState.DENIED_SHAKE) {
                shakeTimer++;
                shakeOffset = (float) Math.sin(shakeTimer * 0.8f) * (6f - (shakeTimer * 0.3f));
                if (shakeTimer > 18) {
                    shakeOffset = 0f;
                    shakeTimer = 0;
                    state = GateState.IDLE;
                }
                needRepaint = true;
            }

            if (needRepaint) repaint();
        });
        gateLoop.start();
    }

    public void triggerOpen(String passengerName, String tripDetails) {
        if (autoCloseTimer != null && autoCloseTimer.isRunning()) {
            autoCloseTimer.stop();
        }

        this.state = GateState.OPENING;
        this.ledColor = new Color(34, 197, 94); // Green
        this.displayMessage = "ACCESS GRANTED — PROCEED";
        this.passengerInfo = passengerName + " | " + tripDetails;
        repaint();

        // Auto close barrier after 2.8 seconds
        autoCloseTimer = new Timer(2800, e -> {
            state = GateState.CLOSING;
            ((Timer) e.getSource()).stop();
        });
        autoCloseTimer.setRepeats(false);
        autoCloseTimer.start();
    }

    public void triggerDenied(String reason) {
        if (autoCloseTimer != null && autoCloseTimer.isRunning()) {
            autoCloseTimer.stop();
        }

        this.state = GateState.DENIED_SHAKE;
        this.shakeTimer = 0;
        this.flapOpenProgress = 0.0f;
        this.ledColor = new Color(239, 68, 68); // Red
        this.displayMessage = "ACCESS DENIED — BARRIER LOCKED";
        this.passengerInfo = reason;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        applyRenderingHints(g2);

        int w = getWidth();
        int h = getHeight();

        // Floor perspective grid
        g2.setColor(new Color(30, 41, 59, 150));
        g2.drawLine(40, h - 20, w - 40, h - 20);
        g2.drawLine(20, h - 5, w - 20, h - 5);

        int gateCenter = w / 2;
        int pillarW = 60;
        int pillarH = 150;
        int pillarY = 60;
        int aisleWidth = 180;

        int leftPillarX = gateCenter - (aisleWidth / 2) - pillarW;
        int rightPillarX = gateCenter + (aisleWidth / 2);

        // Apply shake vibration offset when access is denied
        if (state == GateState.DENIED_SHAKE) {
            g2.translate(shakeOffset, 0);
        }

        // Draw Left & Right Turnstile Pillars
        drawPillar(g2, leftPillarX, pillarY, pillarW, pillarH, true);
        drawPillar(g2, rightPillarX, pillarY, pillarW, pillarH, false);

        // Draw Sliding Glass Barrier Flaps
        drawSlidingFlaps(g2, leftPillarX + pillarW, rightPillarX, pillarY, pillarH);

        // Draw Overhead LED Information Board
        drawOverheadDisplay(g2, gateCenter, 14);

        g2.dispose();
    }

    private void drawPillar(Graphics2D g2, int x, int y, int w, int h, boolean isLeft) {
        GradientPaint steel = new GradientPaint(x, y, new Color(203, 213, 225),
                x + w, y, new Color(100, 116, 139));
        g2.setPaint(steel);
        g2.fill(new RoundRectangle2D.Float(x, y, w, h, 14, 14));

        g2.setColor(new Color(51, 65, 85));
        g2.setStroke(new BasicStroke(2f));
        g2.draw(new RoundRectangle2D.Float(x, y, w, h, 14, 14));

        // RFID Tap Sensor Pad (Left pillar)
        if (isLeft) {
            int padX = x + 10;
            int padY = y + 16;
            g2.setColor(new Color(15, 23, 42));
            g2.fill(new RoundRectangle2D.Float(padX, padY, w - 20, 24, 6, 6));

            g2.setColor(ledColor);
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawArc(padX + 8, padY + 6, 24, 12, 45, 90);
            g2.drawArc(padX + 4, padY + 2, 32, 20, 45, 90);
        }

        // Vertical LED edge strip
        int stripX = isLeft ? (x + w - 8) : (x + 2);
        g2.setColor(new Color(ledColor.getRed(), ledColor.getGreen(), ledColor.getBlue(), 180));
        g2.fill(new RoundRectangle2D.Float(stripX, y + 20, 6, h - 40, 4, 4));
    }

    private void drawSlidingFlaps(Graphics2D g2, int leftEdge, int rightEdge, int pillarY, int pillarH) {
        int aisleWidth = rightEdge - leftEdge;
        int flapMaxW = (aisleWidth / 2) - 4;
        int flapH = 80;
        int flapY = pillarY + 35;

        int currentFlapW = (int) (flapMaxW * (1.0f - flapOpenProgress));

        // Left Glass Flap
        if (currentFlapW > 2) {
            GradientPaint leftFlapGrad = new GradientPaint(
                leftEdge, flapY, new Color(ledColor.getRed(), ledColor.getGreen(), ledColor.getBlue(), 160),
                leftEdge + currentFlapW, flapY, new Color(255, 255, 255, 220)
            );
            g2.setPaint(leftFlapGrad);
            g2.fill(new RoundRectangle2D.Float(leftEdge, flapY, currentFlapW, flapH, 8, 8));

            g2.setColor(ledColor);
            g2.setStroke(new BasicStroke(2f));
            g2.draw(new RoundRectangle2D.Float(leftEdge, flapY, currentFlapW, flapH, 8, 8));
        }

        // Right Glass Flap
        if (currentFlapW > 2) {
            int rightFlapX = rightEdge - currentFlapW;
            GradientPaint rightFlapGrad = new GradientPaint(
                rightFlapX, flapY, new Color(255, 255, 255, 220),
                rightEdge, flapY, new Color(ledColor.getRed(), ledColor.getGreen(), ledColor.getBlue(), 160)
            );
            g2.setPaint(rightFlapGrad);
            g2.fill(new RoundRectangle2D.Float(rightFlapX, flapY, currentFlapW, flapH, 8, 8));

            g2.setColor(ledColor);
            g2.setStroke(new BasicStroke(2f));
            g2.draw(new RoundRectangle2D.Float(rightFlapX, flapY, currentFlapW, flapH, 8, 8));
        }

        // Center Vector Clearance Indicator (Drawn with pure polygons — NO square font glyphs!)
        if (flapOpenProgress > 0.6f) {
            int cx = (leftEdge + rightEdge) / 2;
            int cy = flapY + 45;

            // Draw clean downward arrow polygon
            Polygon arrow = new Polygon();
            arrow.addPoint(cx - 6, cy - 14);
            arrow.addPoint(cx + 6, cy - 14);
            arrow.addPoint(cx + 6, cy);
            arrow.addPoint(cx + 14, cy);
            arrow.addPoint(cx, cy + 16);
            arrow.addPoint(cx - 14, cy);
            arrow.addPoint(cx - 6, cy);

            g2.setColor(new Color(34, 197, 94, 230));
            g2.fillPolygon(arrow);
        }
    }

    private void drawOverheadDisplay(Graphics2D g2, int centerX, int y) {
        int boxW = 340;
        int boxH = 46;
        int boxX = centerX - (boxW / 2);

        // Dark digital casing
        g2.setColor(new Color(15, 23, 42));
        g2.fill(new RoundRectangle2D.Float(boxX, y, boxW, boxH, 10, 10));
        g2.setColor(new Color(51, 65, 85));
        g2.setStroke(new BasicStroke(1.5f));
        g2.draw(new RoundRectangle2D.Float(boxX, y, boxW, boxH, 10, 10));

        // Digital LED Text
        g2.setColor(ledColor);
        g2.setFont(new Font("Segoe UI", Font.BOLD, 13));
        FontMetrics fm = g2.getFontMetrics();
        int msgW = fm.stringWidth(displayMessage);
        g2.drawString(displayMessage, centerX - (msgW / 2), y + 18);

        // Subtitle Info
        g2.setColor(new Color(203, 213, 225));
        g2.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        FontMetrics fm2 = g2.getFontMetrics();
        int infoW = fm2.stringWidth(passengerInfo);
        g2.drawString(passengerInfo, centerX - (infoW / 2), y + 36);
    }
}
