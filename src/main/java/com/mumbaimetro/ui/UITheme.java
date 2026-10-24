package com.mumbaimetro.ui;

import javax.swing.*;
import javax.swing.border.AbstractBorder;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicButtonUI;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultCaret;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;

/**
 * Custom Swing component styling utilities for a modern, anti-aliased desktop UI.
 * Fixes pixelated fonts and janky appearance on Linux (Wayland/X11).
 *
 * Demonstrates: Custom Painting with Graphics2D, RenderingHints,
 * Anonymous Inner Classes, and UI Decorator Pattern.
 */
public final class UITheme {

    // --- Color Palette ---
    public static final Color NAVY          = new Color(26, 54, 93);
    public static final Color NAVY_LIGHT    = new Color(45, 80, 130);
    public static final Color ACCENT_BLUE   = new Color(49, 130, 206);
    public static final Color ACCENT_GREEN  = new Color(38, 166, 91);
    public static final Color ACCENT_RED    = new Color(220, 53, 69);
    public static final Color ACCENT_TEAL   = new Color(44, 152, 148);
    public static final Color ACCENT_PURPLE = new Color(128, 90, 213);
    public static final Color BG_APP        = new Color(241, 243, 248);
    public static final Color BG_CARD       = Color.WHITE;
    public static final Color BG_INPUT      = new Color(248, 250, 252);
    public static final Color BORDER_LIGHT  = new Color(218, 225, 235);
    public static final Color TEXT_PRIMARY  = new Color(30, 41, 59);
    public static final Color TEXT_MUTED    = new Color(100, 116, 139);
    public static final Color PEAK_RED      = new Color(252, 165, 165);
    public static final Color OFFPEAK_GREEN = new Color(110, 231, 183);

    // --- Font Family Resolution ---
    private static final String SANS_FAMILY;
    private static final String MONO_FAMILY;

    static {
        // Resolve best available sans-serif font for this system
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        String[] available = ge.getAvailableFontFamilyNames();
        java.util.Set<String> fontSet = new java.util.HashSet<>(java.util.Arrays.asList(available));

        if (fontSet.contains("Noto Sans")) {
            SANS_FAMILY = "Noto Sans";
        } else if (fontSet.contains("DejaVu Sans")) {
            SANS_FAMILY = "DejaVu Sans";
        } else if (fontSet.contains("Liberation Sans")) {
            SANS_FAMILY = "Liberation Sans";
        } else if (fontSet.contains("Segoe UI")) {
            SANS_FAMILY = "Segoe UI"; // Windows fallback
        } else {
            SANS_FAMILY = Font.SANS_SERIF;
        }

        if (fontSet.contains("JetBrainsMono NF")) {
            MONO_FAMILY = "JetBrainsMono NF";
        } else if (fontSet.contains("JetBrains Mono")) {
            MONO_FAMILY = "JetBrains Mono";
        } else if (fontSet.contains("DejaVu Sans Mono")) {
            MONO_FAMILY = "DejaVu Sans Mono";
        } else {
            MONO_FAMILY = Font.MONOSPACED;
        }
    }

    // --- Prebuilt Font Instances ---
    public static final Font FONT_TITLE       = new Font(SANS_FAMILY, Font.BOLD, 20);
    public static final Font FONT_SUBTITLE    = new Font(SANS_FAMILY, Font.PLAIN, 13);
    public static final Font FONT_HEADING     = new Font(SANS_FAMILY, Font.BOLD, 14);
    public static final Font FONT_BODY        = new Font(SANS_FAMILY, Font.PLAIN, 13);
    public static final Font FONT_BODY_BOLD   = new Font(SANS_FAMILY, Font.BOLD, 13);
    public static final Font FONT_SMALL       = new Font(SANS_FAMILY, Font.PLAIN, 12);
    public static final Font FONT_SMALL_BOLD  = new Font(SANS_FAMILY, Font.BOLD, 12);
    public static final Font FONT_LARGE_BOLD  = new Font(SANS_FAMILY, Font.BOLD, 18);
    public static final Font FONT_MONO        = new Font(MONO_FAMILY, Font.PLAIN, 13);
    public static final Font FONT_MONO_SMALL  = new Font(MONO_FAMILY, Font.PLAIN, 12);
    public static final Font FONT_TAB         = new Font(SANS_FAMILY, Font.BOLD, 13);
    public static final Font FONT_TABLE_HEADER= new Font(SANS_FAMILY, Font.BOLD, 12);
    public static final Font FONT_TABLE_CELL  = new Font(SANS_FAMILY, Font.PLAIN, 12);
    public static final Font FONT_BADGE       = new Font(SANS_FAMILY, Font.BOLD, 22);
    public static final Font FONT_BADGE_MSG   = new Font(SANS_FAMILY, Font.PLAIN, 13);
    public static final Font FONT_BUTTON      = new Font(SANS_FAMILY, Font.BOLD, 13);
    public static final Font FONT_BUTTON_SM   = new Font(SANS_FAMILY, Font.PLAIN, 12);

    private UITheme() {} // Utility class

    /**
     * Applies global anti-aliasing and rendering settings.
     * MUST be called before any Swing component is created.
     */
    public static void applyGlobalAntiAliasing() {
        // Java2D text anti-aliasing
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");
        // Disable hardware pipelines known to cause caret drift & font glyph overlap on Linux X11/Wayland
        System.setProperty("sun.java2d.opengl", "false");
        System.setProperty("sun.java2d.xrender", "true");
        System.setProperty("sun.java2d.uiScale.enabled", "true");

        // Global Swing defaults
        UIManager.put("TextArea.font", FONT_MONO);
        UIManager.put("TextField.font", FONT_BODY);
        UIManager.put("Label.font", FONT_BODY);
        UIManager.put("Button.font", FONT_BUTTON_SM);
        UIManager.put("ComboBox.font", FONT_BODY);
        UIManager.put("TabbedPane.font", FONT_TAB);
        UIManager.put("Table.font", FONT_TABLE_CELL);
        UIManager.put("TableHeader.font", FONT_TABLE_HEADER);
        UIManager.put("ToolTip.font", FONT_SMALL);
        UIManager.put("CheckBox.font", FONT_BODY);
        UIManager.put("Spinner.font", FONT_BODY);
        UIManager.put("OptionPane.messageFont", FONT_BODY);
        UIManager.put("OptionPane.buttonFont", FONT_BODY_BOLD);

        // Make focus rings less obtrusive
        UIManager.put("Button.focus", new Color(0, 0, 0, 0));
        UIManager.put("TabbedPane.focus", new Color(0, 0, 0, 0));
    }

    /**
     * Paints anti-aliased rendering hints into any Graphics context.
     * Uses FRACTIONALMETRICS_OFF to keep integer-aligned font bounds,
     * preventing caret drift and overlap bugs.
     */
    public static void applyRenderingHints(Graphics g) {
        if (g instanceof Graphics2D g2) {
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
            g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        }
    }

    /**
     * Dedicated rendering hints for Swing text components (JTextField, JTextArea, JTextPane).
     * Strictly forces FRACTIONALMETRICS_OFF and STROKE_NORMALIZE so that Swing's internal
     * text layout engine (FieldView / PlainView) and DefaultCaret integer modelToView coordinates
     * match rendered glyph locations pixel-for-pixel, preventing the cursor from overlapping text.
     */
    public static void applyTextRenderingHints(Graphics g) {
        if (g instanceof Graphics2D g2) {
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
            g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        }
    }

    // ---- Custom Component Factories ----

    /**
     * Creates a modern styled button with rounded corners and custom colors.
     */
    public static JButton createStyledButton(String text, Color bg, Color fg) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                applyRenderingHints(g2);

                int arc = 10;
                if (getModel().isPressed()) {
                    g2.setColor(bg.darker());
                } else if (getModel().isRollover()) {
                    g2.setColor(bg.brighter());
                } else {
                    g2.setColor(bg);
                }
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), arc, arc));
                g2.dispose();

                super.paintComponent(g);
            }
        };
        btn.setFont(FONT_BUTTON);
        btn.setForeground(fg);
        btn.setBackground(bg);
        btn.setContentAreaFilled(false);
        btn.setOpaque(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(new EmptyBorder(8, 18, 8, 18));
        return btn;
    }

    /**
     * Creates a secondary outlined button.
     */
    public static JButton createOutlinedButton(String text, Color accentColor) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                applyRenderingHints(g2);

                int arc = 10;
                if (getModel().isRollover()) {
                    g2.setColor(new Color(accentColor.getRed(), accentColor.getGreen(), accentColor.getBlue(), 25));
                    g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), arc, arc));
                }
                g2.setColor(accentColor);
                g2.setStroke(new BasicStroke(1.5f));
                g2.draw(new RoundRectangle2D.Float(1, 1, getWidth() - 2, getHeight() - 2, arc, arc));
                g2.dispose();

                super.paintComponent(g);
            }
        };
        btn.setFont(FONT_BUTTON_SM);
        btn.setForeground(accentColor);
        btn.setContentAreaFilled(false);
        btn.setOpaque(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(new EmptyBorder(7, 16, 7, 16));
        return btn;
    }

    /**
     * Creates a styled text field with rounded border, interactive focus state,
     * smooth background, and precision caret that eliminates text overlapping.
     */
    public static JTextField createStyledTextField(String defaultText, int columns) {
        JTextField field = new JTextField(defaultText, columns) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                applyRenderingHints(g2);
                g2.setColor(getBackground());
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                g2.dispose();

                Graphics2D gText = (Graphics2D) g.create();
                applyTextRenderingHints(gText);
                super.paintComponent(gText);
                gText.dispose();
            }
        };
        field.setOpaque(false);
        field.setFont(FONT_BODY);
        field.setBackground(BG_INPUT);
        field.setForeground(TEXT_PRIMARY);
        field.setCaretColor(TEXT_PRIMARY);
        field.setCaret(new MetroCaret());

        RoundedBorder normalBorder = new RoundedBorder(BORDER_LIGHT, 8, 1.2f);
        RoundedBorder focusedBorder = new RoundedBorder(ACCENT_BLUE, 8, 1.6f);
        Border innerPadding = new EmptyBorder(6, 12, 6, 12);

        field.setBorder(BorderFactory.createCompoundBorder(normalBorder, innerPadding));
        field.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                field.setBorder(BorderFactory.createCompoundBorder(focusedBorder, innerPadding));
                field.repaint();
            }
            @Override
            public void focusLost(FocusEvent e) {
                field.setBorder(BorderFactory.createCompoundBorder(normalBorder, innerPadding));
                field.repaint();
            }
        });
        field.addCaretListener(e -> field.repaint());
        return field;
    }

    /**
     * Creates a styled monospaced text area for receipts with precision caret.
     */
    public static JTextArea createReceiptArea() {
        JTextArea area = new JTextArea() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D gText = (Graphics2D) g.create();
                applyTextRenderingHints(gText);
                super.paintComponent(gText);
                gText.dispose();
            }
        };
        area.setFont(FONT_MONO);
        area.setEditable(false);
        area.setBackground(BG_INPUT);
        area.setForeground(TEXT_PRIMARY);
        area.setCaretColor(TEXT_PRIMARY);
        area.setCaret(new MetroCaret());
        area.setMargin(new Insets(12, 14, 12, 14));
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        return area;
    }

    /**
     * Creates a card-style panel with rounded corners and subtle shadow.
     */
    public static JPanel createCardPanel() {
        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                applyRenderingHints(g2);
                g2.setColor(getBackground());
                g2.fill(new RoundRectangle2D.Float(0, 2, getWidth(), getHeight() - 2, 14, 14));
                g2.dispose();
            }

            @Override
            protected void paintBorder(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                applyRenderingHints(g2);
                g2.setColor(BORDER_LIGHT);
                g2.setStroke(new BasicStroke(1f));
                g2.draw(new RoundRectangle2D.Float(0, 2, getWidth() - 1, getHeight() - 3, 14, 14));
                g2.dispose();
            }
        };
        panel.setBackground(BG_CARD);
        panel.setOpaque(false);
        return panel;
    }

    /**
     * Creates a section title label.
     */
    public static JLabel createSectionTitle(String text) {
        JLabel label = new JLabel(text) {
            @Override
            protected void paintComponent(Graphics g) {
                applyRenderingHints(g);
                super.paintComponent(g);
            }
        };
        label.setFont(FONT_HEADING);
        label.setForeground(TEXT_PRIMARY);
        label.setBorder(new EmptyBorder(0, 0, 6, 0));
        return label;
    }

    // ---- Precision Caret for Swing Text Components ----
    /**
     * Precision Caret for Swing text components.
     * Uses integer-aligned coordinate bounds and custom damage repainting to prevent
     * caret drift, font overlap, and ghost trails on Linux/X11/Wayland.
     */
    public static class MetroCaret extends DefaultCaret {
        public MetroCaret() {
            setBlinkRate(500);
        }

        @Override
        public void paint(Graphics g) {
            if (!isVisible()) return;
            JTextComponent comp = getComponent();
            if (comp == null) return;

            Rectangle r = null;
            try {
                Rectangle2D r2d = comp.modelToView2D(getDot());
                if (r2d != null) {
                    r = r2d.getBounds();
                }
            } catch (BadLocationException ignored) {}

            if (r == null) {
                try {
                    @SuppressWarnings("deprecation")
                    Rectangle oldR = comp.modelToView(getDot());
                    r = oldR;
                } catch (BadLocationException ignored) {}
            }

            if (r != null) {
                Graphics2D g2 = (Graphics2D) g.create();
                applyTextRenderingHints(g2);
                Color caretColor = comp.getCaretColor() != null ? comp.getCaretColor() : TEXT_PRIMARY;
                g2.setColor(caretColor);
                int caretH = Math.max(r.height - 2, 14);
                // Draw clean 2-pixel wide cursor snapped to integer coordinates
                g2.fillRect(r.x, r.y + 1, 2, caretH);
                g2.dispose();
            }
        }

        @Override
        protected synchronized void damage(Rectangle r) {
            if (r == null) return;
            // Expand repaint damage box to clean anti-aliasing edges and avoid ghost remnants
            x = r.x - 3;
            y = r.y;
            width = 10;
            height = r.height;
            repaint();
        }
    }

    // ---- Rounded Border implementation ----
    public static class RoundedBorder extends AbstractBorder {
        private final Color color;
        private final int radius;
        private final float strokeWidth;

        public RoundedBorder(Color color, int radius) {
            this(color, radius, 1.2f);
        }

        public RoundedBorder(Color color, int radius, float strokeWidth) {
            this.color = color;
            this.radius = radius;
            this.strokeWidth = strokeWidth;
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            Graphics2D g2 = (Graphics2D) g.create();
            applyRenderingHints(g2);
            g2.setColor(color);
            g2.setStroke(new BasicStroke(strokeWidth));
            g2.drawRoundRect(x, y, width - 1, height - 1, radius, radius);
            g2.dispose();
        }

        @Override
        public Insets getBorderInsets(Component c) {
            return new Insets(4, 8, 4, 8);
        }

        @Override
        public Insets getBorderInsets(Component c, Insets insets) {
            insets.set(4, 8, 4, 8);
            return insets;
        }
    }

    /**
     * Anti-aliased JPanel base class for all app panels.
     */
    public static class AAPanel extends JPanel {
        public AAPanel() { super(); }
        public AAPanel(LayoutManager lm) { super(lm); }

        @Override
        protected void paintComponent(Graphics g) {
            applyRenderingHints(g);
            super.paintComponent(g);
        }
    }
}
