package com.mumbaimetro.ui;

import com.mumbaimetro.data.DataStore;
import com.mumbaimetro.data.MetroDatabaseManager;
import com.mumbaimetro.exceptions.*;
import com.mumbaimetro.model.*;
import com.mumbaimetro.qr.QRCodeGenerator;
import com.mumbaimetro.service.MetroService;
import com.mumbaimetro.threads.PeakHourMonitorThread;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;
import java.util.Map;

import static com.mumbaimetro.ui.UITheme.*;

/**
 * Immersive, Animated Desktop Application for Mumbai Metro Ticketing System.
 *
 * Major Features:
 * 1. 60 FPS Interactive Mumbai Metro Route Visualizer with animated train (MetroTrackCanvas)
 * 2. Multi-Line Selector (Line 1 Blue, Line 2A Yellow, Line 7 Red, Line 3 Aqua) with 62 stations
 * 3. 3D Physical Turnstile Gate Simulator with motorized sliding flaps (AnimatedGateView)
 * 4. Relational Database Backend powered by SQLite with Foreign Keys & ACID integrity
 * 5. Interactive SQL Console Tab allowing dynamic SQL query execution & table inspection
 * 6. High-definition QR Code generator and PNG/TXT ticket exporter
 * 7. Pure vector & text rendering (zero missing font emoji tofu boxes)
 */
public class MetroAppGUI extends JFrame implements PeakHourMonitorThread.PeakHourListener, MetroTrackCanvas.StationSelectionListener {

    private final MetroService service;
    private final PeakHourMonitorThread peakHourThread;

    // Line selection
    private JComboBox<String> lineSelectorCombo;
    private String currentLineCode = "LINE_1";

    // Header
    private JLabel statusBannerLabel;
    private JLabel dbStatusLabel;

    // Tab 1: Map & Token Booking
    private MetroTrackCanvas trackCanvas;
    private JComboBox<Station> sourceCombo;
    private JComboBox<Station> destCombo;
    private JSpinner passengerSpinner;
    private JCheckBox returnTripCheck;
    private JLabel fareEstimateLabel;
    private JTextArea ticketReceiptArea;
    private JLabel qrImageLabel;
    private Ticket lastBookedTicket;

    // Tab 2: Gate Simulator
    private AnimatedGateView animatedGateView;
    private JTextField gateCardIdField;
    private JComboBox<Station> gateStationCombo;
    private JLabel cardInfoDisplayLabel;
    private JComboBox<String> quickCardSelector;

    // Tab 3: Card Management
    private JTextField newHolderNameField;
    private JTextField newPhoneField;
    private JComboBox<String> newCardTypeCombo;
    private JTextField newInitialBalanceField;
    private JTextField newCollegeField;
    private JTextField newRollNoField;
    private JPanel studentExtraPanel;
    private JTextField rechargeCardIdField;
    private JTextField rechargeAmountField;
    private JTextArea cardDetailsReceiptArea;

    // Tab 4: Audit
    private DefaultTableModel transactionTableModel;
    private JLabel totalRevenueLabel;
    private JLabel totalTripsLabel;
    private JTextArea stationTrafficArea;

    // Tab 5: Relational Database & SQL Console
    private JTextArea sqlQueryArea;
    private DefaultTableModel sqlResultTableModel;
    private JLabel sqlExecutionStatusLabel;

    public MetroAppGUI() {
        super("Mumbai Metro One — Smart Card & Fare Ticketing System (Multi-Line Edition)");
        this.service = new MetroService();

        initComponents();

        this.peakHourThread = new PeakHourMonitorThread(this);
        this.peakHourThread.start();

        updateQuickCardSelector();
        refreshTransactionTable();
        calculateEstimatedTokenFare();
    }

    private void initComponents() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1220, 820);
        setMinimumSize(new Dimension(1020, 700));
        setLocationRelativeTo(null);

        AAPanel root = new AAPanel(new BorderLayout(0, 0));
        root.setBackground(BG_APP);

        root.add(createHeaderPanel(), BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane(JTabbedPane.TOP);
        tabs.setFont(FONT_TAB);
        tabs.setBorder(new EmptyBorder(6, 12, 12, 12));
        tabs.setOpaque(false);

        // Clean typography without square emoji glyphs
        tabs.addTab("  Route Map & Booking  ", createRouteAndBookingTab());
        tabs.addTab("  Turnstile Gate Simulator  ", createGateSimulatorTab());
        tabs.addTab("  Card Management  ", createCardManagementTab());
        tabs.addTab("  Audit & Traffic  ", createAuditTab());
        tabs.addTab("  Relational Database (SQL)  ", createSqlConsoleTab());

        root.add(tabs, BorderLayout.CENTER);
        setContentPane(root);
    }

    // =========================================================================
    //  Header Panel
    // =========================================================================
    private JPanel createHeaderPanel() {
        AAPanel header = new AAPanel(new BorderLayout(14, 0));
        header.setBackground(NAVY);
        header.setBorder(new EmptyBorder(12, 20, 12, 20));

        AAPanel titleBox = new AAPanel(new GridLayout(2, 1, 0, 2));
        titleBox.setOpaque(false);

        JLabel title = new JLabel("MUMBAI METRO TRANSIT SYSTEM (MMOPL / MMMOCL)") {
            @Override protected void paintComponent(Graphics g) { applyRenderingHints(g); super.paintComponent(g); }
        };
        title.setFont(FONT_TITLE);
        title.setForeground(Color.WHITE);

        JLabel subtitle = new JLabel("Operational Network: Blue Line 1  |  Yellow Line 2A  |  Red Line 7  |  Aqua Line 3 (62 Stations)") {
            @Override protected void paintComponent(Graphics g) { applyRenderingHints(g); super.paintComponent(g); }
        };
        subtitle.setFont(FONT_SUBTITLE);
        subtitle.setForeground(new Color(180, 200, 225));

        titleBox.add(title);
        titleBox.add(subtitle);
        header.add(titleBox, BorderLayout.WEST);

        AAPanel statusBox = new AAPanel(new FlowLayout(FlowLayout.RIGHT, 14, 4));
        statusBox.setOpaque(false);

        dbStatusLabel = new JLabel("[DB: SQLite Connected]") {
            @Override protected void paintComponent(Graphics g) { applyRenderingHints(g); super.paintComponent(g); }
        };
        dbStatusLabel.setFont(FONT_SMALL_BOLD);
        dbStatusLabel.setForeground(OFFPEAK_GREEN);

        statusBannerLabel = new JLabel("[ONLINE] Initializing Rush-Hour Thread...") {
            @Override protected void paintComponent(Graphics g) { applyRenderingHints(g); super.paintComponent(g); }
        };
        statusBannerLabel.setFont(FONT_SMALL_BOLD);
        statusBannerLabel.setForeground(OFFPEAK_GREEN);

        JButton toggleBtn = createStyledButton("Toggle Peak / Off-Peak", NAVY_LIGHT, Color.WHITE);
        toggleBtn.setFont(FONT_BUTTON_SM);
        toggleBtn.addActionListener(e -> {
            peakHourThread.togglePeakHourManually();
            calculateEstimatedTokenFare();
        });

        statusBox.add(dbStatusLabel);
        statusBox.add(statusBannerLabel);
        statusBox.add(toggleBtn);
        header.add(statusBox, BorderLayout.EAST);

        return header;
    }

    // =========================================================================
    //  Tab 1: Immersive Route Map & Token Booking
    // =========================================================================
    private JPanel createRouteAndBookingTab() {
        AAPanel panel = new AAPanel(new BorderLayout(12, 12));
        panel.setBorder(new EmptyBorder(12, 12, 12, 12));
        panel.setBackground(BG_APP);

        // Top Control: Line Selector Bar
        JPanel lineBar = createCardPanel();
        lineBar.setLayout(new BorderLayout(10, 0));
        lineBar.setBorder(new EmptyBorder(8, 14, 8, 14));

        AAPanel leftLineGroup = new AAPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        leftLineGroup.setOpaque(false);

        JLabel lineLbl = new JLabel("Select Metro Line:") {
            @Override protected void paintComponent(Graphics g) { applyRenderingHints(g); super.paintComponent(g); }
        };
        lineLbl.setFont(FONT_BODY_BOLD);
        lineLbl.setForeground(NAVY);

        String[] lineChoices = {
            "Line 1 (Blue Line): Versova <-> Ghatkopar (12 Stations)",
            "Line 2A (Yellow Line): Dahisar East <-> Andheri West (17 Stations)",
            "Line 7 (Red Line): Dahisar East <-> Gundavali (14 Stations)",
            "Line 3 (Aqua Line): Aarey JVLR <-> Cuffe Parade (19 Stations)"
        };
        lineSelectorCombo = new JComboBox<>(lineChoices);
        lineSelectorCombo.setFont(FONT_BODY);
        lineSelectorCombo.addActionListener(e -> onLineSelectionChanged());

        leftLineGroup.add(lineLbl);
        leftLineGroup.add(lineSelectorCombo);
        lineBar.add(leftLineGroup, BorderLayout.WEST);

        JLabel networkInfoLbl = new JLabel("Integrated Multi-Line Relational Database Network") {
            @Override protected void paintComponent(Graphics g) { applyRenderingHints(g); super.paintComponent(g); }
        };
        networkInfoLbl.setFont(FONT_SMALL);
        networkInfoLbl.setForeground(TEXT_MUTED);
        lineBar.add(networkInfoLbl, BorderLayout.EAST);

        AAPanel topArea = new AAPanel(new BorderLayout(0, 8));
        topArea.setOpaque(false);
        topArea.add(lineBar, BorderLayout.NORTH);

        // 60 FPS Interactive Metro Route Visualizer
        trackCanvas = new MetroTrackCanvas(service.getStationsByLine(currentLineCode));
        trackCanvas.setSelectionListener(this);
        trackCanvas.setBorder(new RoundedBorder(BORDER_LIGHT, 12));
        topArea.add(trackCanvas, BorderLayout.CENTER);

        panel.add(topArea, BorderLayout.NORTH);

        // Bottom Container: Booking Form (Left) & Digital QR Ticket (Right)
        AAPanel bottomSplit = new AAPanel(new BorderLayout(14, 0));
        bottomSplit.setOpaque(false);

        // Left: Journey Booking Form
        JPanel formCard = createCardPanel();
        formCard.setLayout(new GridBagLayout());
        formCard.setBorder(new EmptyBorder(16, 18, 16, 18));
        formCard.setPreferredSize(new Dimension(420, 0));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 8, 6, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        List<Station> lineStations = service.getStationsByLine(currentLineCode);
        Station[] stArr = lineStations.toArray(new Station[0]);
        sourceCombo = new JComboBox<>(stArr);
        destCombo = new JComboBox<>(stArr);
        if (stArr.length > 3) destCombo.setSelectedIndex(3);
        sourceCombo.setFont(FONT_BODY);
        destCombo.setFont(FONT_BODY);

        sourceCombo.addActionListener(e -> {
            calculateEstimatedTokenFare();
            trackCanvas.setSelectedStations((Station) sourceCombo.getSelectedItem(), (Station) destCombo.getSelectedItem());
        });
        destCombo.addActionListener(e -> {
            calculateEstimatedTokenFare();
            trackCanvas.setSelectedStations((Station) sourceCombo.getSelectedItem(), (Station) destCombo.getSelectedItem());
        });

        passengerSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 10, 1));
        passengerSpinner.setFont(FONT_BODY);
        if (passengerSpinner.getEditor() instanceof JSpinner.DefaultEditor de) {
            JTextField tf = de.getTextField();
            tf.setBackground(BG_INPUT);
            tf.setForeground(TEXT_PRIMARY);
            tf.setCaretColor(TEXT_PRIMARY);
            tf.setCaret(new MetroCaret());
            tf.setFont(FONT_BODY_BOLD);
            tf.setBorder(new EmptyBorder(4, 8, 4, 8));
            tf.addCaretListener(ev -> tf.repaint());
        }
        passengerSpinner.addChangeListener(e -> calculateEstimatedTokenFare());

        returnTripCheck = new JCheckBox("Return Journey (10% Round-Trip Discount)");
        returnTripCheck.setFont(FONT_BODY);
        returnTripCheck.setOpaque(false);
        returnTripCheck.addActionListener(e -> calculateEstimatedTokenFare());

        fareEstimateLabel = new JLabel("Distance: — | Fare: —") {
            @Override protected void paintComponent(Graphics g) { applyRenderingHints(g); super.paintComponent(g); }
        };
        fareEstimateLabel.setFont(FONT_BODY_BOLD);
        fareEstimateLabel.setForeground(ACCENT_BLUE);

        JButton bookBtn = createStyledButton("Generate QR Ticket", ACCENT_TEAL, Color.WHITE);
        bookBtn.addActionListener(e -> bookTokenAction());

        JButton animateTrainBtn = createOutlinedButton("Dispatch Train", ACCENT_BLUE);
        animateTrainBtn.addActionListener(e -> {
            Station src = (Station) sourceCombo.getSelectedItem();
            Station dst = (Station) destCombo.getSelectedItem();
            trackCanvas.dispatchTrain(src, dst);
        });

        JButton saveBtn = createOutlinedButton("Save Receipt & QR", TEXT_MUTED);
        saveBtn.addActionListener(e -> saveReceiptToFileAction());

        int r = 0;
        addFormRow(formCard, gbc, r++, "Single Journey Booking", null, true);
        addFormRow(formCard, gbc, r++, "Origin Station", sourceCombo, false);
        addFormRow(formCard, gbc, r++, "Destination Station", destCombo, false);
        addFormRow(formCard, gbc, r++, "Passengers", passengerSpinner, false);

        gbc.gridx = 0; gbc.gridy = r++; gbc.gridwidth = 2;
        formCard.add(returnTripCheck, gbc);
        gbc.gridwidth = 1;

        gbc.gridx = 0; gbc.gridy = r++; gbc.gridwidth = 2;
        formCard.add(fareEstimateLabel, gbc);
        gbc.gridwidth = 1;

        gbc.gridx = 0; gbc.gridy = r; gbc.gridwidth = 2;
        AAPanel btnRow = new AAPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        btnRow.setOpaque(false);
        btnRow.add(bookBtn);
        btnRow.add(animateTrainBtn);
        btnRow.add(saveBtn);
        formCard.add(btnRow, gbc);

        bottomSplit.add(formCard, BorderLayout.WEST);

        // Right: QR Image & Text Receipt
        AAPanel rightPanel = new AAPanel(new BorderLayout(12, 0));
        rightPanel.setOpaque(false);

        qrImageLabel = new JLabel("QR Code will render here", SwingConstants.CENTER) {
            @Override protected void paintComponent(Graphics g) { applyRenderingHints(g); super.paintComponent(g); }
        };
        qrImageLabel.setPreferredSize(new Dimension(210, 210));
        qrImageLabel.setOpaque(true);
        qrImageLabel.setBackground(BG_CARD);
        qrImageLabel.setBorder(new RoundedBorder(BORDER_LIGHT, 10));

        AAPanel qrCard = new AAPanel(new BorderLayout());
        qrCard.setOpaque(false);
        qrCard.add(qrImageLabel, BorderLayout.CENTER);
        rightPanel.add(qrCard, BorderLayout.WEST);

        ticketReceiptArea = createReceiptArea();
        ticketReceiptArea.setText("""
              ====================================================
                      MUMBAI METRO ONE — DIGITAL TRAVEL TICKET
              ====================================================
               Select a metro line above and choose stations.
               Click 'Generate QR Ticket' to issue ticket.
              ====================================================
            """);

        JScrollPane scroll = new JScrollPane(ticketReceiptArea);
        scroll.setBorder(new RoundedBorder(BORDER_LIGHT, 10));
        rightPanel.add(scroll, BorderLayout.CENTER);

        bottomSplit.add(rightPanel, BorderLayout.CENTER);
        panel.add(bottomSplit, BorderLayout.CENTER);

        return panel;
    }

    private void onLineSelectionChanged() {
        int idx = lineSelectorCombo.getSelectedIndex();
        switch (idx) {
            case 1 -> currentLineCode = "LINE_2A";
            case 2 -> currentLineCode = "LINE_7";
            case 3 -> currentLineCode = "LINE_3";
            default -> currentLineCode = "LINE_1";
        }

        List<Station> stations = service.getStationsByLine(currentLineCode);
        trackCanvas.setStations(stations, currentLineCode);

        // Update dropdowns
        sourceCombo.removeAllItems();
        destCombo.removeAllItems();
        for (Station s : stations) {
            sourceCombo.addItem(s);
            destCombo.addItem(s);
        }
        if (stations.size() > 3) {
            destCombo.setSelectedIndex(3);
        }

        // Update gate station combo as well
        if (gateStationCombo != null) {
            gateStationCombo.removeAllItems();
            for (Station s : stations) {
                gateStationCombo.addItem(s);
            }
        }

        calculateEstimatedTokenFare();
    }

    @Override
    public void onStationSelected(Station station, boolean isOrigin) {
        if (isOrigin) {
            sourceCombo.setSelectedItem(station);
        } else {
            destCombo.setSelectedItem(station);
        }
        calculateEstimatedTokenFare();
    }

    private void calculateEstimatedTokenFare() {
        if (sourceCombo == null || destCombo == null) return;
        Station src = (Station) sourceCombo.getSelectedItem();
        Station dest = (Station) destCombo.getSelectedItem();
        if (src == null || dest == null) return;

        try {
            int pax = (Integer) passengerSpinner.getValue();
            boolean isReturn = returnTripCheck.isSelected();
            boolean isPeak = peakHourThread.isPeakHour();
            double dist = service.getFareCalculator().calculateDistance(src, dest);
            double fare = service.getFareCalculator().calculateTokenFare(src, dest, pax, isReturn, isPeak);
            fareEstimateLabel.setText(String.format("Distance: %.1f km  |  Total: ₹%.2f %s",
                    dist, fare, isPeak ? "(Peak Rush +₹5)" : ""));
            fareEstimateLabel.setForeground(ACCENT_BLUE);
        } catch (InvalidStationException ex) {
            fareEstimateLabel.setText("Notice: " + ex.getMessage());
            fareEstimateLabel.setForeground(ACCENT_RED);
        }
    }

    private void bookTokenAction() {
        Station src = (Station) sourceCombo.getSelectedItem();
        Station dest = (Station) destCombo.getSelectedItem();
        int pax = (Integer) passengerSpinner.getValue();
        boolean isReturn = returnTripCheck.isSelected();
        boolean isPeak = peakHourThread.isPeakHour();

        try {
            lastBookedTicket = service.bookSingleJourneyToken(src, dest, pax, isReturn, isPeak);
            ticketReceiptArea.setText(lastBookedTicket.generateReceipt());
            ticketReceiptArea.setCaretPosition(0);

            trackCanvas.dispatchTrain(src, dest);

            // Generate real pure-Java QR Code
            String qrData = String.format("MUMBAI-METRO|%s|%s|%s|PAX:%d|%.2f",
                    lastBookedTicket.getTicketId(),
                    src.getName(), dest.getName(), pax, lastBookedTicket.getTotalAmount());
            try {
                BufferedImage qrImg = QRCodeGenerator.generateQRImage(qrData, 6);
                qrImageLabel.setIcon(new ImageIcon(qrImg));
                qrImageLabel.setText("");
            } catch (Exception qrEx) {
                qrImageLabel.setIcon(null);
                qrImageLabel.setText("QR Error: " + qrEx.getMessage());
            }

            refreshTransactionTable();
            JOptionPane.showMessageDialog(this,
                    "Ticket Booked & Saved to SQLite DB!\nID: " + lastBookedTicket.getTicketId() + "\nTotal: ₹" + lastBookedTicket.getTotalAmount(),
                    "Ticket Issued", JOptionPane.INFORMATION_MESSAGE);

        } catch (InvalidStationException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Station Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void saveReceiptToFileAction() {
        if (lastBookedTicket == null) {
            JOptionPane.showMessageDialog(this, "Book a ticket first to export receipt.", "No Ticket", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String fn = "Ticket_" + lastBookedTicket.getTicketId() + ".txt";
        String qrFn = "QR_" + lastBookedTicket.getTicketId() + ".png";
        try {
            DataStore.saveReceiptToFile(fn, lastBookedTicket.generateReceipt());

            String qrData = String.format("MUMBAI-METRO|%s|%s|%s|PAX:%d|%.2f",
                    lastBookedTicket.getTicketId(),
                    lastBookedTicket.getSource().getName(),
                    lastBookedTicket.getDestination().getName(),
                    lastBookedTicket.getPassengerCount(),
                    lastBookedTicket.getTotalAmount());

            BufferedImage qrImg = QRCodeGenerator.generateQRImage(qrData, 8);
            javax.imageio.ImageIO.write(qrImg, "PNG", new File(qrFn));

            JOptionPane.showMessageDialog(this,
                    "Saved to local directory:\n1. Text Receipt: " + new File(fn).getAbsolutePath() +
                            "\n2. High-Res QR Code: " + new File(qrFn).getAbsolutePath(),
                    "Files Exported", JOptionPane.INFORMATION_MESSAGE);

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Export failed: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // =========================================================================
    //  Tab 2: Physical Simulated Turnstile Gate
    // =========================================================================
    private JPanel createGateSimulatorTab() {
        AAPanel panel = new AAPanel(new BorderLayout(14, 14));
        panel.setBorder(new EmptyBorder(14, 14, 14, 14));
        panel.setBackground(BG_APP);

        animatedGateView = new AnimatedGateView();
        animatedGateView.setBorder(new RoundedBorder(BORDER_LIGHT, 12));
        panel.add(animatedGateView, BorderLayout.NORTH);

        AAPanel bottomSplit = new AAPanel(new GridLayout(1, 2, 14, 14));
        bottomSplit.setOpaque(false);

        JPanel ctrlCard = createCardPanel();
        ctrlCard.setLayout(new GridBagLayout());
        ctrlCard.setBorder(new EmptyBorder(16, 18, 16, 18));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 8, 6, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        quickCardSelector = new JComboBox<>();
        quickCardSelector.setFont(FONT_BODY);
        quickCardSelector.addActionListener(e -> {
            String sel = (String) quickCardSelector.getSelectedItem();
            if (sel != null && sel.contains("[")) {
                String id = sel.substring(sel.indexOf('[') + 1, sel.indexOf(']'));
                gateCardIdField.setText(id);
                updateCardDisplay(id);
            }
        });

        gateCardIdField = createStyledTextField("MMC-1001", 12);
        JButton checkCardBtn = createOutlinedButton("Lookup", ACCENT_BLUE);
        checkCardBtn.addActionListener(e -> updateCardDisplay(gateCardIdField.getText().trim()));

        List<Station> stations = service.getStations();
        gateStationCombo = new JComboBox<>(stations.toArray(new Station[0]));
        gateStationCombo.setFont(FONT_BODY);

        JButton tapInBtn = createStyledButton("TAP IN  (Entry Gate)", ACCENT_GREEN, Color.WHITE);
        tapInBtn.addActionListener(e -> processTapInAction());

        JButton tapOutBtn = createStyledButton("TAP OUT  (Exit Gate)", ACCENT_RED, Color.WHITE);
        tapOutBtn.addActionListener(e -> processTapOutAction());

        int r = 0;
        addFormRow(ctrlCard, gbc, r++, "Turnstile Gate Controls", null, true);
        addFormRow(ctrlCard, gbc, r++, "Quick Pick Sample Card", quickCardSelector, false);

        gbc.gridy = r++;
        gbc.gridx = 0; gbc.weightx = 0;
        JLabel idLbl = new JLabel("Card ID") { @Override protected void paintComponent(Graphics g) { applyRenderingHints(g); super.paintComponent(g); }};
        idLbl.setFont(FONT_BODY); idLbl.setForeground(TEXT_MUTED);
        ctrlCard.add(idLbl, gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        AAPanel idRow = new AAPanel(new BorderLayout(6, 0)); idRow.setOpaque(false);
        idRow.add(gateCardIdField, BorderLayout.CENTER);
        idRow.add(checkCardBtn, BorderLayout.EAST);
        ctrlCard.add(idRow, gbc);
        gbc.weightx = 0;

        addFormRow(ctrlCard, gbc, r++, "Turnstile Station", gateStationCombo, false);

        gbc.gridx = 0; gbc.gridy = r; gbc.gridwidth = 2;
        AAPanel gateBtns = new AAPanel(new GridLayout(1, 2, 10, 0));
        gateBtns.setOpaque(false);
        gateBtns.add(tapInBtn);
        gateBtns.add(tapOutBtn);
        ctrlCard.add(gateBtns, gbc);
        gbc.gridwidth = 1;

        bottomSplit.add(ctrlCard);

        JPanel infoCard = createCardPanel();
        infoCard.setLayout(new BorderLayout());
        infoCard.setBorder(new EmptyBorder(16, 18, 16, 18));

        cardInfoDisplayLabel = new JLabel("<html><body style='font-family:sans-serif; color:#64748B;'>Select or enter a card to inspect live relational database record.</body></html>") {
            @Override protected void paintComponent(Graphics g) { applyRenderingHints(g); super.paintComponent(g); }
        };
        infoCard.add(cardInfoDisplayLabel, BorderLayout.CENTER);

        bottomSplit.add(infoCard);
        panel.add(bottomSplit, BorderLayout.CENTER);

        return panel;
    }

    private void updateCardDisplay(String cardId) {
        Card card = service.getCard(cardId);
        if (card == null) {
            cardInfoDisplayLabel.setText("<html><body style='padding:16px; color:#DC2626;'><b>Card not found in database: " + cardId + "</b></body></html>");
            return;
        }

        String transit = card.isInTransit()
                ? "<span style='color:#16A34A;'><b>IN TRANSIT</b> (Tapped in at " + card.getTapInStation().getName() + ")</span>"
                : "<span style='color:#94A3B8;'>NOT IN TRANSIT (Ready for Entry)</span>";

        String extra = "";
        if (card instanceof StudentCard sc) {
            extra = "<br><b>College:</b> " + sc.getCollegeName() + " &nbsp;|&nbsp; <b>Roll No:</b> " + sc.getStudentRollNo();
        }

        cardInfoDisplayLabel.setText(String.format("""
            <html><body style='font-family:sans-serif; font-size:12px; line-height:1.7;'>
                <div style='font-size:16px; font-weight:bold; color:#1A365D; margin-bottom:8px;'>%s</div>
                <b>Card ID:</b> %s &nbsp;&nbsp;|&nbsp;&nbsp; <b>Holder Name:</b> %s<br>
                <b>Category:</b> %s %s<br>
                <b>Account Balance:</b> <span style='font-size:18px; color:#2563EB;'><b>₹%.2f</b></span><br>
                <b>Card Status:</b> %s &nbsp;&nbsp;|&nbsp;&nbsp; <b>State:</b> %s<br>
                <b>Min. Required Balance:</b> ₹%.2f<br>
                <small style='color:#64748B;'>Managed in SQLite table: <code>cards</code></small>
            </body></html>
            """,
                card.getCardType(), card.getCardId(), card.getHolderName(),
                card.getCardType(), extra, card.getBalance(),
                card.isBlocked() ? "<span style='color:#DC2626;'>BLOCKED</span>" : "<span style='color:#16A34A;'>ACTIVE</span>",
                transit, card.getMinimumBalanceRequired()));
    }

    private void processTapInAction() {
        String cardId = gateCardIdField.getText().trim();
        Station station = (Station) gateStationCombo.getSelectedItem();

        try {
            service.processTapIn(cardId, station);
            Card c = service.getCard(cardId);
            animatedGateView.triggerOpen(c.getHolderName(), "Entered at " + station.getName());
            updateCardDisplay(cardId);
            refreshTransactionTable();
        } catch (CardBlockedException ex) {
            animatedGateView.triggerDenied(ex.getMessage());
        } catch (MinimumBalanceException ex) {
            animatedGateView.triggerDenied(String.format("Balance ₹%.2f < Min ₹%.2f", ex.getCurrentBalance(), ex.getMinRequired()));
        } catch (Exception ex) {
            animatedGateView.triggerDenied(ex.getMessage());
        }
    }

    private void processTapOutAction() {
        String cardId = gateCardIdField.getText().trim();
        Station station = (Station) gateStationCombo.getSelectedItem();
        boolean isPeak = peakHourThread.isPeakHour();

        try {
            MetroService.JourneyResult r = service.processTapOut(cardId, station, isPeak);
            String disc = r.discount > 0 ? String.format(" (Saved ₹%.2f)", r.discount) : "";
            animatedGateView.triggerOpen(r.card.getHolderName(),
                    String.format("%s -> %s | Fare: ₹%.2f%s | Bal: ₹%.2f",
                            r.origin.getName(), r.destination.getName(), r.finalFare, disc, r.remainingBalance));
            updateCardDisplay(cardId);
            refreshTransactionTable();
        } catch (InsufficientBalanceException ex) {
            animatedGateView.triggerDenied(String.format("Deficit ₹%.2f (Need ₹%.2f)", ex.getDeficit(), ex.getRequiredAmount()));
        } catch (Exception ex) {
            animatedGateView.triggerDenied(ex.getMessage());
        }
    }

    // =========================================================================
    //  Tab 3: Card Management Tab
    // =========================================================================
    private JPanel createCardManagementTab() {
        AAPanel panel = new AAPanel(new GridLayout(1, 2, 16, 16));
        panel.setBorder(new EmptyBorder(16, 16, 16, 16));
        panel.setBackground(BG_APP);

        JPanel issueCard = createCardPanel();
        issueCard.setLayout(new GridBagLayout());
        issueCard.setBorder(new EmptyBorder(18, 20, 18, 20));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 8, 6, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        newHolderNameField = createStyledTextField("", 15);
        newPhoneField = createStyledTextField("", 15);
        newCardTypeCombo = new JComboBox<>(new String[]{"Regular Card", "Student Concession (20% OFF)", "Tourist Pass (3-Day)"});
        newCardTypeCombo.setFont(FONT_BODY);
        newInitialBalanceField = createStyledTextField("200.0", 15);
        newCollegeField = createStyledTextField("Mumbai University", 15);
        newRollNoField = createStyledTextField("MU-2024-001", 15);

        studentExtraPanel = new AAPanel(new GridLayout(2, 2, 6, 6));
        studentExtraPanel.setOpaque(false);
        JLabel cLbl = new JLabel("College") { @Override protected void paintComponent(Graphics g) { applyRenderingHints(g); super.paintComponent(g); }};
        cLbl.setFont(FONT_BODY); cLbl.setForeground(TEXT_MUTED);
        JLabel rLbl = new JLabel("Roll No") { @Override protected void paintComponent(Graphics g) { applyRenderingHints(g); super.paintComponent(g); }};
        rLbl.setFont(FONT_BODY); rLbl.setForeground(TEXT_MUTED);
        studentExtraPanel.add(cLbl); studentExtraPanel.add(newCollegeField);
        studentExtraPanel.add(rLbl); studentExtraPanel.add(newRollNoField);
        studentExtraPanel.setVisible(false);

        newCardTypeCombo.addActionListener(e -> {
            studentExtraPanel.setVisible(newCardTypeCombo.getSelectedIndex() == 1);
            issueCard.revalidate(); issueCard.repaint();
        });

        JButton issueBtn = createStyledButton("Issue & Save to DB", ACCENT_BLUE, Color.WHITE);
        issueBtn.addActionListener(e -> issueCardAction());

        int r = 0;
        addFormRow(issueCard, gbc, r++, "Issue New Smart Card (RDBMS)", null, true);
        addFormRow(issueCard, gbc, r++, "Full Name", newHolderNameField, false);
        addFormRow(issueCard, gbc, r++, "Phone", newPhoneField, false);
        addFormRow(issueCard, gbc, r++, "Card Category", newCardTypeCombo, false);
        addFormRow(issueCard, gbc, r++, "Initial Balance (₹)", newInitialBalanceField, false);
        gbc.gridx = 0; gbc.gridy = r++; gbc.gridwidth = 2;
        issueCard.add(studentExtraPanel, gbc);
        gbc.gridwidth = 1;
        gbc.gridx = 0; gbc.gridy = r; gbc.gridwidth = 2;
        issueCard.add(issueBtn, gbc);

        panel.add(issueCard);

        JPanel actionCard = createCardPanel();
        actionCard.setLayout(new BorderLayout(0, 12));
        actionCard.setBorder(new EmptyBorder(18, 20, 18, 20));

        AAPanel rechForm = new AAPanel(new GridBagLayout());
        rechForm.setOpaque(false);
        GridBagConstraints gbc2 = new GridBagConstraints();
        gbc2.insets = new Insets(6, 8, 6, 8);
        gbc2.fill = GridBagConstraints.HORIZONTAL;

        rechargeCardIdField = createStyledTextField("MMC-1001", 12);
        rechargeAmountField = createStyledTextField("100.0", 12);

        JButton rechBtn = createStyledButton("Top-Up Balance", ACCENT_GREEN, Color.WHITE);
        rechBtn.addActionListener(e -> rechargeCardAction());
        JButton blockBtn = createOutlinedButton("Toggle Block/Unblock", ACCENT_RED);
        blockBtn.addActionListener(e -> toggleBlockAction());

        int r2 = 0;
        addFormRow(rechForm, gbc2, r2++, "Recharge & Account Security", null, true);
        addFormRow(rechForm, gbc2, r2++, "Card ID", rechargeCardIdField, false);
        addFormRow(rechForm, gbc2, r2++, "Top-Up Amount (₹)", rechargeAmountField, false);
        gbc2.gridx = 0; gbc2.gridy = r2; gbc2.gridwidth = 2;
        AAPanel rBtns = new AAPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        rBtns.setOpaque(false);
        rBtns.add(rechBtn); rBtns.add(blockBtn);
        rechForm.add(rBtns, gbc2);

        actionCard.add(rechForm, BorderLayout.NORTH);

        cardDetailsReceiptArea = createReceiptArea();
        cardDetailsReceiptArea.setText("Card statement / receipt will appear here.");
        JScrollPane cardScroll = new JScrollPane(cardDetailsReceiptArea);
        cardScroll.setBorder(new RoundedBorder(BORDER_LIGHT, 10));
        actionCard.add(cardScroll, BorderLayout.CENTER);

        panel.add(actionCard);
        return panel;
    }

    private void issueCardAction() {
        String name = newHolderNameField.getText().trim();
        String phone = newPhoneField.getText().trim();
        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter customer name.", "Missing Name", JOptionPane.WARNING_MESSAGE);
            return;
        }
        double bal = 100.0;
        try { bal = Double.parseDouble(newInitialBalanceField.getText().trim()); } catch (NumberFormatException ignored) {}

        int idx = newCardTypeCombo.getSelectedIndex();
        String type = idx == 1 ? "STUDENT" : (idx == 2 ? "TOURIST" : "REGULAR");

        Card card = service.issueCard(name, phone, type, bal, newCollegeField.getText().trim(), newRollNoField.getText().trim(), 3);
        updateQuickCardSelector();
        refreshTransactionTable();
        cardDetailsReceiptArea.setText(card.generateReceipt());

        JOptionPane.showMessageDialog(this,
                "Card saved to SQLite Relational DB!\nCard ID: " + card.getCardId() + "\nHolder: " + card.getHolderName(),
                "Card Issued", JOptionPane.INFORMATION_MESSAGE);

        newHolderNameField.setText(""); newPhoneField.setText("");
    }

    private void rechargeCardAction() {
        String cardId = rechargeCardIdField.getText().trim();
        double amt;
        try {
            amt = Double.parseDouble(rechargeAmountField.getText().trim());
            if (amt <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Enter a valid positive recharge amount.", "Invalid Amount", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            double newBal = service.rechargeCard(cardId, amt);
            Card card = service.getCard(cardId);
            if (card != null) cardDetailsReceiptArea.setText(card.generateReceipt());
            updateQuickCardSelector();
            refreshTransactionTable();
            JOptionPane.showMessageDialog(this,
                    "Recharge committed to SQLite database!\nUpdated Balance: ₹" + newBal,
                    "Recharge Completed", JOptionPane.INFORMATION_MESSAGE);
        } catch (CardBlockedException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Card Blocked", JOptionPane.ERROR_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void toggleBlockAction() {
        String cardId = rechargeCardIdField.getText().trim();
        boolean blocked = service.toggleBlockStatus(cardId);
        Card card = service.getCard(cardId);
        if (card != null) {
            cardDetailsReceiptArea.setText(card.generateReceipt());
            JOptionPane.showMessageDialog(this,
                    "Card [" + cardId + "] is now " + (blocked ? "BLOCKED" : "ACTIVE"),
                    "Security Status Updated", JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(this, "Card not found in database.", "Error", JOptionPane.ERROR_MESSAGE);
        }
        updateQuickCardSelector();
    }

    // =========================================================================
    //  Tab 4: Audit & Traffic Tab
    // =========================================================================
    private JPanel createAuditTab() {
        AAPanel panel = new AAPanel(new BorderLayout(12, 12));
        panel.setBorder(new EmptyBorder(16, 16, 16, 16));
        panel.setBackground(BG_APP);

        AAPanel stats = new AAPanel(new GridLayout(1, 3, 12, 0));
        stats.setOpaque(false);
        totalRevenueLabel = createStatCard("Total Revenue (SQL SUM)", "₹0.00", ACCENT_BLUE);
        totalTripsLabel = createStatCard("Operations (SQL COUNT)", "0", ACCENT_GREEN);
        JLabel sysStatus = createStatCard("Database Engine", "SQLite 3 (ACID)", ACCENT_PURPLE);
        stats.add(totalRevenueLabel); stats.add(totalTripsLabel); stats.add(sysStatus);
        panel.add(stats, BorderLayout.NORTH);

        String[] cols = {"Txn ID", "Ref ID", "Type", "Amount (₹)", "Origin", "Destination", "Balance After", "Status", "Timestamp"};
        transactionTableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(transactionTableModel);
        table.setFillsViewportHeight(true);
        table.setRowHeight(28);
        table.setFont(FONT_TABLE_CELL);
        table.setGridColor(new Color(235, 240, 248));
        table.setSelectionBackground(new Color(219, 234, 254));
        table.setShowHorizontalLines(true);
        table.setShowVerticalLines(false);

        JTableHeader th = table.getTableHeader();
        th.setFont(FONT_TABLE_HEADER);
        th.setBackground(new Color(241, 245, 249));
        th.setForeground(TEXT_PRIMARY);

        DefaultTableCellRenderer centerR = new DefaultTableCellRenderer();
        centerR.setHorizontalAlignment(SwingConstants.CENTER);
        for (int i = 0; i < cols.length; i++) table.getColumnModel().getColumn(i).setCellRenderer(centerR);

        JScrollPane tableScroll = new JScrollPane(table);
        tableScroll.setBorder(new RoundedBorder(BORDER_LIGHT, 10));
        panel.add(tableScroll, BorderLayout.CENTER);

        stationTrafficArea = createReceiptArea();
        stationTrafficArea.setRows(4);
        JScrollPane trafficScroll = new JScrollPane(stationTrafficArea);
        trafficScroll.setBorder(new RoundedBorder(BORDER_LIGHT, 10));

        AAPanel bottomWrap = new AAPanel(new BorderLayout(0, 4));
        bottomWrap.setOpaque(false);
        JLabel trafficTitle = createSectionTitle("Station Traffic Aggregation (SQL GROUP BY)");
        bottomWrap.add(trafficTitle, BorderLayout.NORTH);
        bottomWrap.add(trafficScroll, BorderLayout.CENTER);
        panel.add(bottomWrap, BorderLayout.SOUTH);

        return panel;
    }

    private JLabel createStatCard(String title, String value, Color accent) {
        JLabel lbl = new JLabel(
            String.format("<html><body style='padding:10px; font-family:sans-serif;'>" +
                "<div style='font-size:11px; color:#64748B;'>%s</div>" +
                "<div style='font-size:18px; font-weight:bold; color:#1E293B;'>%s</div></body></html>", title, value),
            SwingConstants.LEFT) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                applyRenderingHints(g2);
                g2.setColor(BG_CARD);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 12, 12));
                g2.setColor(accent);
                g2.fill(new RoundRectangle2D.Float(0, 0, 5, getHeight(), 4, 4));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        lbl.setOpaque(false);
        lbl.setBorder(new EmptyBorder(0, 10, 0, 0));
        return lbl;
    }

    // =========================================================================
    //  Tab 5: Relational Database & Interactive SQL Console
    // =========================================================================
    private JPanel createSqlConsoleTab() {
        AAPanel panel = new AAPanel(new BorderLayout(14, 14));
        panel.setBorder(new EmptyBorder(16, 16, 16, 16));
        panel.setBackground(BG_APP);

        JPanel topCard = createCardPanel();
        topCard.setLayout(new BorderLayout(0, 10));
        topCard.setBorder(new EmptyBorder(14, 16, 14, 16));

        AAPanel titleRow = new AAPanel(new BorderLayout());
        titleRow.setOpaque(false);
        titleRow.add(createSectionTitle("Relational Database Explorer & SQL Console"), BorderLayout.WEST);

        JLabel dbPathLbl = new JLabel("Database: " + MetroDatabaseManager.getDatabasePath());
        dbPathLbl.setFont(FONT_SMALL);
        dbPathLbl.setForeground(TEXT_MUTED);
        titleRow.add(dbPathLbl, BorderLayout.EAST);
        topCard.add(titleRow, BorderLayout.NORTH);

        AAPanel presetPanel = new AAPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        presetPanel.setOpaque(false);

        JButton btnQ1 = createOutlinedButton("SELECT * FROM cards", ACCENT_BLUE);
        btnQ1.addActionListener(e -> setAndRunSql("SELECT * FROM cards;"));

        JButton btnQ2 = createOutlinedButton("SELECT * FROM stations", ACCENT_BLUE);
        btnQ2.addActionListener(e -> setAndRunSql("SELECT * FROM stations;"));

        JButton btnQ3 = createOutlinedButton("SELECT * FROM transactions", ACCENT_BLUE);
        btnQ3.addActionListener(e -> setAndRunSql("SELECT * FROM transactions ORDER BY rowid DESC LIMIT 25;"));

        JButton btnQ4 = createOutlinedButton("Card Aggregates (GROUP BY)", ACCENT_TEAL);
        btnQ4.addActionListener(e -> setAndRunSql("SELECT card_type, COUNT(*) as count, ROUND(SUM(balance), 2) as total_balance, ROUND(AVG(balance), 2) as avg_balance FROM cards GROUP BY card_type;"));

        JButton btnQ5 = createOutlinedButton("Traffic Join (JOIN + GROUP BY)", ACCENT_PURPLE);
        btnQ5.addActionListener(e -> setAndRunSql("SELECT s.name, s.line_name, COUNT(t.txn_id) as total_taps FROM stations s LEFT JOIN transactions t ON (s.name = t.source_station OR s.name = t.dest_station) GROUP BY s.id, s.name ORDER BY total_taps DESC;"));

        presetPanel.add(btnQ1);
        presetPanel.add(btnQ2);
        presetPanel.add(btnQ3);
        presetPanel.add(btnQ4);
        presetPanel.add(btnQ5);
        topCard.add(presetPanel, BorderLayout.CENTER);

        sqlQueryArea = new JTextArea("SELECT * FROM stations WHERE line_code = 'LINE_1';", 3, 60) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D gText = (Graphics2D) g.create();
                applyTextRenderingHints(gText);
                super.paintComponent(gText);
                gText.dispose();
            }
        };
        sqlQueryArea.setFont(FONT_MONO);
        sqlQueryArea.setBackground(BG_INPUT);
        sqlQueryArea.setForeground(TEXT_PRIMARY);
        sqlQueryArea.setCaretColor(TEXT_PRIMARY);
        sqlQueryArea.setCaret(new MetroCaret());
        sqlQueryArea.setMargin(new Insets(8, 12, 8, 12));
        sqlQueryArea.addCaretListener(ev -> sqlQueryArea.repaint());
        JScrollPane sqlScroll = new JScrollPane(sqlQueryArea);
        sqlScroll.setBorder(new RoundedBorder(BORDER_LIGHT, 8));

        JButton executeSqlBtn = createStyledButton("Execute SQL Query", ACCENT_GREEN, Color.WHITE);
        executeSqlBtn.addActionListener(e -> executeCurrentSql());

        AAPanel execRow = new AAPanel(new BorderLayout(8, 0));
        execRow.setOpaque(false);
        execRow.add(sqlScroll, BorderLayout.CENTER);
        execRow.add(executeSqlBtn, BorderLayout.EAST);
        topCard.add(execRow, BorderLayout.SOUTH);

        panel.add(topCard, BorderLayout.NORTH);

        sqlResultTableModel = new DefaultTableModel(new String[]{"Notice"}, 0);
        JTable sqlTable = new JTable(sqlResultTableModel);
        sqlTable.setFillsViewportHeight(true);
        sqlTable.setRowHeight(26);
        sqlTable.setFont(FONT_TABLE_CELL);

        JTableHeader sqlTh = sqlTable.getTableHeader();
        sqlTh.setFont(FONT_TABLE_HEADER);
        sqlTh.setBackground(new Color(241, 245, 249));

        JScrollPane resScroll = new JScrollPane(sqlTable);
        resScroll.setBorder(new RoundedBorder(BORDER_LIGHT, 10));
        panel.add(resScroll, BorderLayout.CENTER);

        sqlExecutionStatusLabel = new JLabel("Click any preset query above or enter standard SQL and click Execute.");
        sqlExecutionStatusLabel.setFont(FONT_SMALL);
        sqlExecutionStatusLabel.setForeground(TEXT_MUTED);
        panel.add(sqlExecutionStatusLabel, BorderLayout.SOUTH);

        SwingUtilities.invokeLater(() -> executeCurrentSql());

        return panel;
    }

    private void setAndRunSql(String sql) {
        sqlQueryArea.setText(sql);
        executeCurrentSql();
    }

    private void executeCurrentSql() {
        String sql = sqlQueryArea.getText().trim();
        MetroDatabaseManager.RawQueryResult res = MetroDatabaseManager.executeRawQuery(sql);

        sqlResultTableModel.setDataVector(res.rows, res.columnNames);
        sqlExecutionStatusLabel.setText(res.message);
        sqlExecutionStatusLabel.setForeground(res.rowCount >= 0 ? new Color(22, 101, 52) : ACCENT_RED);
    }

    // =========================================================================
    //  Data Refresh Logic
    // =========================================================================
    private void refreshTransactionTable() {
        if (transactionTableModel == null) return;
        transactionTableModel.setRowCount(0);
        List<Transaction> txns = service.getAllTransactions();
        for (int i = txns.size() - 1; i >= 0; i--) {
            Transaction t = txns.get(i);
            transactionTableModel.addRow(new Object[]{
                    t.getTxnId(), t.getReferenceId(), t.getType(),
                    String.format("%.2f", t.getAmount()), t.getSourceStation(), t.getDestStation(),
                    String.format("%.2f", t.getBalanceAfter()), t.getStatus(), t.getTimestamp()
            });
        }

        double rev = DataStore.getInstance().getTotalRevenue();
        totalRevenueLabel.setText(String.format("<html><body style='padding:10px; font-family:sans-serif;'>" +
            "<div style='font-size:11px; color:#64748B;'>Total Revenue (SQL SUM)</div>" +
            "<div style='font-size:18px; font-weight:bold; color:#1E293B;'>₹%.2f</div></body></html>", rev));

        totalTripsLabel.setText(String.format("<html><body style='padding:10px; font-family:sans-serif;'>" +
            "<div style='font-size:11px; color:#64748B;'>Operations (SQL COUNT)</div>" +
            "<div style='font-size:18px; font-weight:bold; color:#1E293B;'>%d</div></body></html>", txns.size()));

        StringBuilder sb = new StringBuilder();
        DataStore.getInstance().getStationTraffic().forEach((st, cnt) ->
                sb.append(String.format("  %-24s : %d taps\n", st, cnt)));
        stationTrafficArea.setText(sb.toString());
    }

    private void updateQuickCardSelector() {
        if (quickCardSelector == null) return;
        quickCardSelector.removeAllItems();
        for (Card c : service.getAllCards()) {
            quickCardSelector.addItem(String.format("[%s] %s (₹%.2f) — %s",
                    c.getCardId(), c.getHolderName(), c.getBalance(), c.getCardType()));
        }
    }

    private void addFormRow(JPanel form, GridBagConstraints gbc, int row, String label, JComponent field, boolean isHeader) {
        gbc.gridy = row;
        if (isHeader) {
            gbc.gridx = 0; gbc.gridwidth = 2;
            form.add(createSectionTitle(label), gbc);
            gbc.gridwidth = 1;
        } else {
            JLabel lbl = new JLabel(label) {
                @Override protected void paintComponent(Graphics g) { applyRenderingHints(g); super.paintComponent(g); }
            };
            lbl.setFont(FONT_BODY);
            lbl.setForeground(TEXT_MUTED);
            gbc.gridx = 0; gbc.weightx = 0;
            form.add(lbl, gbc);
            gbc.gridx = 1; gbc.weightx = 1.0;
            form.add(field, gbc);
            gbc.weightx = 0;
        }
    }

    @Override
    public void onPeakHourStatusChanged(boolean isPeakHour, String reason) {
        SwingUtilities.invokeLater(() -> {
            statusBannerLabel.setText((isPeakHour ? "[PEAK] " : "[OFF-PEAK] ") + reason);
            statusBannerLabel.setForeground(isPeakHour ? PEAK_RED : OFFPEAK_GREEN);
            calculateEstimatedTokenFare();
        });
    }
}
