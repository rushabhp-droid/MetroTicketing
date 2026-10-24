package com.mumbaimetro.data;

import com.mumbaimetro.model.*;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Full Relational Database Management System using SQLite and JDBC.
 * Manages tables, constraints, foreign keys, prepared statements, and transactional integrity.
 *
 * Supports all 4 operational Mumbai Metro Lines:
 * - Line 1 (Blue Line: Versova - Ghatkopar)
 * - Line 2A (Yellow Line: Dahisar East - Andheri West)
 * - Line 7 (Red Line: Dahisar East - Gundavali)
 * - Line 3 (Aqua Line: Aarey JVLR - Cuffe Parade)
 */
public class MetroDatabaseManager {

    private static final String DB_FILE = "mumbai_metro.db";
    private static final String DB_URL = "jdbc:sqlite:" + DB_FILE;
    private static boolean driverLoaded = false;

    static {
        try {
            Class.forName("org.sqlite.JDBC");
            driverLoaded = true;
        } catch (ClassNotFoundException e) {
            System.err.println("[JDBC Error] SQLite driver not found: " + e.getMessage());
        }
    }

    public static Connection getConnection() throws SQLException {
        Connection conn = DriverManager.getConnection(DB_URL);
        try (Statement s = conn.createStatement()) {
            s.execute("PRAGMA foreign_keys = ON;");
        }
        return conn;
    }

    public static boolean isDriverAvailable() {
        return driverLoaded;
    }

    public static String getDatabasePath() {
        return new java.io.File(DB_FILE).getAbsolutePath();
    }

    public static void initializeDatabase() {
        String createStationsSql = """
            CREATE TABLE IF NOT EXISTS stations (
                id INTEGER PRIMARY KEY,
                name TEXT NOT NULL,
                line_code TEXT NOT NULL,
                line_name TEXT NOT NULL,
                distance_km REAL NOT NULL,
                interchange TEXT DEFAULT 'None',
                UNIQUE(line_code, name)
            );
        """;

        String createCardsSql = """
            CREATE TABLE IF NOT EXISTS cards (
                card_id TEXT PRIMARY KEY,
                holder_name TEXT NOT NULL,
                contact_number TEXT,
                card_type TEXT NOT NULL,
                balance REAL NOT NULL DEFAULT 0.0,
                is_blocked INTEGER NOT NULL DEFAULT 0,
                in_transit INTEGER NOT NULL DEFAULT 0,
                tap_in_station_id INTEGER,
                tap_in_time TEXT,
                college_name TEXT,
                student_roll TEXT,
                validity_days INTEGER DEFAULT 0,
                created_at TEXT NOT NULL,
                FOREIGN KEY (tap_in_station_id) REFERENCES stations(id)
            );
        """;

        String createTicketsSql = """
            CREATE TABLE IF NOT EXISTS tickets (
                ticket_id TEXT PRIMARY KEY,
                source_id INTEGER NOT NULL,
                dest_id INTEGER NOT NULL,
                distance_km REAL NOT NULL,
                unit_fare REAL NOT NULL,
                passenger_count INTEGER NOT NULL DEFAULT 1,
                is_return INTEGER NOT NULL DEFAULT 0,
                total_fare REAL NOT NULL,
                ticket_type TEXT NOT NULL,
                qr_data TEXT,
                booked_at TEXT NOT NULL,
                FOREIGN KEY (source_id) REFERENCES stations(id),
                FOREIGN KEY (dest_id) REFERENCES stations(id)
            );
        """;

        String createTransactionsSql = """
            CREATE TABLE IF NOT EXISTS transactions (
                txn_id TEXT PRIMARY KEY,
                ref_id TEXT NOT NULL,
                txn_type TEXT NOT NULL,
                amount REAL NOT NULL,
                source_station TEXT,
                dest_station TEXT,
                balance_after REAL NOT NULL DEFAULT 0.0,
                status TEXT NOT NULL,
                created_at TEXT NOT NULL
            );
        """;

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.execute(createStationsSql);
            stmt.execute(createCardsSql);
            stmt.execute(createTicketsSql);
            stmt.execute(createTransactionsSql);

            // Populate all 4 operational Mumbai Metro lines if not already fully seeded
            seedStationsIfEmpty(conn);

            // Populate sample cards
            seedCardsIfEmpty(conn);

            System.out.println("[RDBMS] Relational Database ready: " + DB_FILE);

        } catch (SQLException e) {
            System.err.println("[RDBMS Error] Database init error: " + e.getMessage());
        }
    }

    private static void seedStationsIfEmpty(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM stations")) {

            // If stations table has less than 60 stations, re-seed all 4 lines
            if (rs.next() && rs.getInt(1) < 60) {
                stmt.execute("DELETE FROM stations;");
                String sql = "INSERT OR REPLACE INTO stations (id, name, line_code, line_name, distance_km, interchange) VALUES (?, ?, ?, ?, ?, ?)";

                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    Object[][] allStations = {
                        // ── LINE 1: BLUE LINE (Versova - Ghatkopar, 11.4 km, 12 Stations) ──
                        {1, "Versova", "LINE_1", "Line 1 (Blue)", 0.0, "None"},
                        {2, "D.N. Nagar", "LINE_1", "Line 1 (Blue)", 1.1, "Line 2A (Yellow)"},
                        {3, "Azad Nagar", "LINE_1", "Line 1 (Blue)", 2.2, "None"},
                        {4, "Andheri", "LINE_1", "Line 1 (Blue)", 3.6, "Western Railway Local"},
                        {5, "Western Express Highway", "LINE_1", "Line 1 (Blue)", 4.8, "Line 7 (Red - Gundavali)"},
                        {6, "Chakala (J.B. Nagar)", "LINE_1", "Line 1 (Blue)", 6.1, "None"},
                        {7, "Airport Road", "LINE_1", "Line 1 (Blue)", 7.2, "None"},
                        {8, "Marol Naka", "LINE_1", "Line 1 (Blue)", 8.3, "Line 3 (Aqua Underground)"},
                        {9, "Saki Naka", "LINE_1", "Line 1 (Blue)", 9.4, "None"},
                        {10, "Asalpha", "LINE_1", "Line 1 (Blue)", 10.2, "None"},
                        {11, "Jagruti Nagar", "LINE_1", "Line 1 (Blue)", 10.8, "None"},
                        {12, "Ghatkopar", "LINE_1", "Line 1 (Blue)", 11.4, "Central Railway Local"},

                        // ── LINE 2A: YELLOW LINE (Dahisar East - Andheri West, 18.6 km, 17 Stations) ──
                        {101, "Dahisar (East)", "LINE_2A", "Line 2A (Yellow)", 0.0, "Line 7 (Red)"},
                        {102, "Anand Nagar", "LINE_2A", "Line 2A (Yellow)", 1.4, "None"},
                        {103, "Kandarpada", "LINE_2A", "Line 2A (Yellow)", 2.7, "None"},
                        {104, "Mandapeshwar", "LINE_2A", "Line 2A (Yellow)", 4.0, "None"},
                        {105, "Eksar Road", "LINE_2A", "Line 2A (Yellow)", 5.2, "None"},
                        {106, "Borivali (West)", "LINE_2A", "Line 2A (Yellow)", 6.5, "None"},
                        {107, "Pahadi Eksar", "LINE_2A", "Line 2A (Yellow)", 7.8, "None"},
                        {108, "Kandivali (West)", "LINE_2A", "Line 2A (Yellow)", 9.1, "None"},
                        {109, "Dahanukar Wadi", "LINE_2A", "Line 2A (Yellow)", 10.4, "None"},
                        {110, "Valnai Meeth Chowky", "LINE_2A", "Line 2A (Yellow)", 11.7, "None"},
                        {111, "Malad (West)", "LINE_2A", "Line 2A (Yellow)", 13.0, "None"},
                        {112, "Lower Malad", "LINE_2A", "Line 2A (Yellow)", 14.2, "None"},
                        {113, "Bangur Nagar", "LINE_2A", "Line 2A (Yellow)", 15.4, "None"},
                        {114, "Goregaon (West)", "LINE_2A", "Line 2A (Yellow)", 16.6, "None"},
                        {115, "Oshiwara", "LINE_2A", "Line 2A (Yellow)", 17.4, "None"},
                        {116, "Lower Oshiwara", "LINE_2A", "Line 2A (Yellow)", 18.0, "None"},
                        {117, "Andheri (West) / D.N. Nagar", "LINE_2A", "Line 2A (Yellow)", 18.6, "Line 1 (Blue)"},

                        // ── LINE 7: RED LINE (Dahisar East - Gundavali, 16.5 km, 14 Stations) ──
                        {201, "Dahisar (East)", "LINE_7", "Line 7 (Red)", 0.0, "Line 2A (Yellow)"},
                        {202, "Ovaripada", "LINE_7", "Line 7 (Red)", 1.3, "None"},
                        {203, "National Park", "LINE_7", "Line 7 (Red)", 2.6, "None"},
                        {204, "Devipada", "LINE_7", "Line 7 (Red)", 3.8, "None"},
                        {205, "Magathane", "LINE_7", "Line 7 (Red)", 5.1, "None"},
                        {206, "Poisar", "LINE_7", "Line 7 (Red)", 6.4, "None"},
                        {207, "Akurli / Kandivali (E)", "LINE_7", "Line 7 (Red)", 7.6, "None"},
                        {208, "Kurar", "LINE_7", "Line 7 (Red)", 8.9, "None"},
                        {209, "Dindoshi", "LINE_7", "Line 7 (Red)", 10.2, "None"},
                        {210, "Aarey Colony", "LINE_7", "Line 7 (Red)", 11.5, "None"},
                        {211, "Goregaon (East)", "LINE_7", "Line 7 (Red)", 12.8, "None"},
                        {212, "JVLR", "LINE_7", "Line 7 (Red)", 14.0, "Line 3 (Aqua)"},
                        {213, "Shankarwadi", "LINE_7", "Line 7 (Red)", 15.2, "None"},
                        {214, "Gundavali / Andheri (East)", "LINE_7", "Line 7 (Red)", 16.5, "Line 1 (Blue - WEH)"},

                        // ── LINE 3: AQUA LINE (Aarey JVLR - Cuffe Parade, 33.5 km, 19 Stations) ──
                        {301, "Aarey (JVLR)", "LINE_3", "Line 3 (Aqua)", 0.0, "Line 7 (Red)"},
                        {302, "SEEPZ", "LINE_3", "Line 3 (Aqua)", 2.1, "None"},
                        {303, "MIDC - Andheri", "LINE_3", "Line 3 (Aqua)", 3.8, "None"},
                        {304, "Marol Naka", "LINE_3", "Line 3 (Aqua)", 5.4, "Line 1 (Blue)"},
                        {305, "CSMIA Airport (T2)", "LINE_3", "Line 3 (Aqua)", 7.2, "International Terminal 2"},
                        {306, "Sahar Road", "LINE_3", "Line 3 (Aqua)", 8.9, "None"},
                        {307, "CSMIA Airport (T1)", "LINE_3", "Line 3 (Aqua)", 10.6, "Domestic Terminal 1"},
                        {308, "Santacruz", "LINE_3", "Line 3 (Aqua)", 12.4, "Western Railway"},
                        {309, "Bandra Colony", "LINE_3", "Line 3 (Aqua)", 14.5, "None"},
                        {310, "BKC (Bandra Kurla Complex)", "LINE_3", "Line 3 (Aqua)", 16.8, "Bullet Train Interchange"},
                        {311, "Dharavi", "LINE_3", "Line 3 (Aqua)", 18.5, "None"},
                        {312, "Dadar Metro", "LINE_3", "Line 3 (Aqua)", 21.2, "Central & Western Local"},
                        {313, "Siddhivinayak", "LINE_3", "Line 3 (Aqua)", 22.9, "None"},
                        {314, "Worli", "LINE_3", "Line 3 (Aqua)", 24.8, "None"},
                        {315, "Mahalaxmi", "LINE_3", "Line 3 (Aqua)", 27.1, "Western Railway Local"},
                        {316, "Mumbai Central Metro", "LINE_3", "Line 3 (Aqua)", 28.6, "National Rail Outstation"},
                        {317, "CSMT Metro", "LINE_3", "Line 3 (Aqua)", 31.2, "Central Railway Outstation"},
                        {318, "Churchgate Metro", "LINE_3", "Line 3 (Aqua)", 32.4, "Western Railway Local"},
                        {319, "Cuffe Parade", "LINE_3", "Line 3 (Aqua)", 33.5, "South Mumbai Terminal"}
                    };

                    for (Object[] row : allStations) {
                        ps.setInt(1, (Integer) row[0]);
                        ps.setString(2, (String) row[1]);
                        ps.setString(3, (String) row[2]);
                        ps.setString(4, (String) row[3]);
                        ps.setDouble(5, (Double) row[4]);
                        ps.setString(6, (String) row[5]);
                        ps.executeUpdate();
                    }
                }
                System.out.println("[RDBMS] Seeded 62 stations across all 4 operational Mumbai Metro lines into 'stations' table.");
            }
        }
    }

    private static void seedCardsIfEmpty(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM cards")) {
            if (rs.next() && rs.getInt(1) == 0) {
                saveCardDirect(new RegularCard("MMC-1001", "Rahul Sharma", "9820011223", 250.0), conn);
                saveCardDirect(new StudentCard("MMC-1002", "Priya Deshmukh", "9819922334", 120.0, "VJTI Mumbai", "22CE1042"), conn);
                saveCardDirect(new TouristPassCard("MMC-1003", "David Smith", "9892233445", 500.0, 3), conn);
                saveCardDirect(new RegularCard("MMC-1004", "Neha Kulkarni", "9769012345", 15.0), conn);
                System.out.println("[RDBMS] Seeded 4 default smart cards into 'cards' table.");
            }
        }
    }

    // =========================================================================
    //  Card CRUD Operations
    // =========================================================================

    public static void saveCard(Card card) {
        try (Connection conn = getConnection()) {
            saveCardDirect(card, conn);
        } catch (SQLException e) {
            System.err.println("[RDBMS Error] Failed to save card: " + e.getMessage());
        }
    }

    private static void saveCardDirect(Card card, Connection conn) throws SQLException {
        String sql = """
            INSERT INTO cards (
                card_id, holder_name, contact_number, card_type, balance, is_blocked,
                in_transit, tap_in_station_id, tap_in_time, college_name, student_roll,
                validity_days, created_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT(card_id) DO UPDATE SET
                holder_name = excluded.holder_name,
                contact_number = excluded.contact_number,
                balance = excluded.balance,
                is_blocked = excluded.is_blocked,
                in_transit = excluded.in_transit,
                tap_in_station_id = excluded.tap_in_station_id,
                tap_in_time = excluded.tap_in_time,
                college_name = excluded.college_name,
                student_roll = excluded.student_roll,
                validity_days = excluded.validity_days;
        """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, card.getCardId());
            ps.setString(2, card.getHolderName());
            ps.setString(3, card.getContactNumber());
            ps.setString(4, card instanceof StudentCard ? "STUDENT" : (card instanceof TouristPassCard ? "TOURIST" : "REGULAR"));
            ps.setDouble(5, card.getBalance());
            ps.setInt(6, card.isBlocked() ? 1 : 0);
            ps.setInt(7, card.isInTransit() ? 1 : 0);

            if (card.isInTransit() && card.getTapInStation() != null) {
                ps.setInt(8, card.getTapInStation().getStationId());
                ps.setString(9, card.getTapInTime() != null ? card.getTapInTime().toString() : LocalDateTime.now().toString());
            } else {
                ps.setNull(8, Types.INTEGER);
                ps.setNull(9, Types.VARCHAR);
            }

            if (card instanceof StudentCard sc) {
                ps.setString(10, sc.getCollegeName());
                ps.setString(11, sc.getStudentRollNo());
                ps.setInt(12, 0);
            } else if (card instanceof TouristPassCard tc) {
                ps.setNull(10, Types.VARCHAR);
                ps.setNull(11, Types.VARCHAR);
                ps.setInt(12, tc.getValidityDays());
            } else {
                ps.setNull(10, Types.VARCHAR);
                ps.setNull(11, Types.VARCHAR);
                ps.setInt(12, 0);
            }

            ps.setString(13, card.getIssueDate() != null ? card.getIssueDate() : LocalDateTime.now().toString());
            ps.executeUpdate();
        }
    }

    public static Card getCard(String cardId) {
        String sql = """
            SELECT c.*, s.name as station_name, s.distance_km as station_dist,
                   s.line_code as station_line_code, s.line_name as station_line_name,
                   s.interchange as station_interchange
            FROM cards c
            LEFT JOIN stations s ON c.tap_in_station_id = s.id
            WHERE c.card_id = ?;
        """;

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, cardId.trim().toUpperCase());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToCard(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("[RDBMS Error] Failed to get card: " + e.getMessage());
        }
        return null;
    }

    public static List<Card> getAllCards() {
        List<Card> list = new ArrayList<>();
        String sql = """
            SELECT c.*, s.name as station_name, s.distance_km as station_dist,
                   s.line_code as station_line_code, s.line_name as station_line_name,
                   s.interchange as station_interchange
            FROM cards c
            LEFT JOIN stations s ON c.tap_in_station_id = s.id
            ORDER BY c.card_id ASC;
        """;

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                list.add(mapResultSetToCard(rs));
            }
        } catch (SQLException e) {
            System.err.println("[RDBMS Error] Failed to list cards: " + e.getMessage());
        }
        return list;
    }

    private static Card mapResultSetToCard(ResultSet rs) throws SQLException {
        String id = rs.getString("card_id");
        String name = rs.getString("holder_name");
        String contact = rs.getString("contact_number");
        String type = rs.getString("card_type");
        double balance = rs.getDouble("balance");
        boolean blocked = rs.getInt("is_blocked") == 1;
        boolean inTransit = rs.getInt("in_transit") == 1;
        int tapStationId = rs.getInt("tap_in_station_id");
        String college = rs.getString("college_name");
        String roll = rs.getString("student_roll");
        int validity = rs.getInt("validity_days");

        Card card;
        if ("STUDENT".equalsIgnoreCase(type)) {
            card = new StudentCard(id, name, contact, balance, college, roll);
        } else if ("TOURIST".equalsIgnoreCase(type)) {
            card = new TouristPassCard(id, name, contact, balance, validity);
        } else {
            card = new RegularCard(id, name, contact, balance);
        }

        card.setBlocked(blocked);

        if (inTransit && tapStationId > 0) {
            String sName = rs.getString("station_name");
            double dist = rs.getDouble("station_dist");
            String lineCode = rs.getString("station_line_code");
            String lineName = rs.getString("station_line_name");
            String interchange = rs.getString("station_interchange");
            Station station = new Station(tapStationId, sName, dist, lineCode, lineName, interchange);
            card.tapIn(station);
        }

        return card;
    }

    // =========================================================================
    //  Stations Operations (Multi-Line)
    // =========================================================================

    public static List<Station> getAllStations() {
        List<Station> stations = new ArrayList<>();
        String sql = "SELECT * FROM stations ORDER BY line_code ASC, distance_km ASC;";
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                stations.add(mapResultSetToStation(rs));
            }
        } catch (SQLException e) {
            System.err.println("[RDBMS Error] Failed to fetch stations: " + e.getMessage());
        }
        return stations;
    }

    public static List<Station> getStationsByLine(String lineCode) {
        List<Station> stations = new ArrayList<>();
        String sql = "SELECT * FROM stations WHERE line_code = ? ORDER BY distance_km ASC;";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, lineCode);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    stations.add(mapResultSetToStation(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[RDBMS Error] Failed to fetch stations by line: " + e.getMessage());
        }
        return stations;
    }

    private static Station mapResultSetToStation(ResultSet rs) throws SQLException {
        return new Station(
            rs.getInt("id"),
            rs.getString("name"),
            rs.getDouble("distance_km"),
            rs.getString("line_code"),
            rs.getString("line_name"),
            rs.getString("interchange")
        );
    }

    public static Map<String, String> getAvailableLines() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("LINE_1", "Line 1 (Blue): Versova <-> Ghatkopar");
        map.put("LINE_2A", "Line 2A (Yellow): Dahisar East <-> Andheri West");
        map.put("LINE_7", "Line 7 (Red): Dahisar East <-> Gundavali");
        map.put("LINE_3", "Line 3 (Aqua): Aarey JVLR <-> Cuffe Parade");
        return map;
    }

    // =========================================================================
    //  Transactions & Tickets CRUD
    // =========================================================================

    public static void logTransaction(Transaction txn) {
        String sql = """
            INSERT INTO transactions (
                txn_id, ref_id, txn_type, amount, source_station, dest_station, balance_after, status, created_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?);
        """;

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, txn.getTxnId());
            ps.setString(2, txn.getReferenceId());
            ps.setString(3, txn.getType().name());
            ps.setDouble(4, txn.getAmount());
            ps.setString(5, txn.getSourceStation());
            ps.setString(6, txn.getDestStation());
            ps.setDouble(7, txn.getBalanceAfter());
            ps.setString(8, txn.getStatus());
            ps.setString(9, txn.getTimestamp());
            ps.executeUpdate();

        } catch (SQLException e) {
            System.err.println("[RDBMS Error] Failed to log transaction: " + e.getMessage());
        }
    }

    public static List<Transaction> getAllTransactions() {
        List<Transaction> list = new ArrayList<>();
        String sql = "SELECT * FROM transactions ORDER BY rowid DESC;";

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Transaction.Type type;
                try {
                    type = Transaction.Type.valueOf(rs.getString("txn_type"));
                } catch (Exception ex) {
                    type = Transaction.Type.TOKEN_PURCHASE;
                }
                list.add(new Transaction(
                    rs.getString("txn_id"),
                    rs.getString("ref_id"),
                    type,
                    rs.getDouble("amount"),
                    rs.getString("source_station"),
                    rs.getString("dest_station"),
                    rs.getDouble("balance_after"),
                    rs.getString("status")
                ));
            }
        } catch (SQLException e) {
            System.err.println("[RDBMS Error] Failed to fetch transactions: " + e.getMessage());
        }
        return list;
    }

    public static void saveTicket(Ticket tkt) {
        String sql = """
            INSERT INTO tickets (
                ticket_id, source_id, dest_id, distance_km, unit_fare,
                passenger_count, is_return, total_fare, ticket_type, qr_data, booked_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);
        """;

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, tkt.getTicketId());
            ps.setInt(2, tkt.getSource().getStationId());
            ps.setInt(3, tkt.getDestination().getStationId());
            ps.setDouble(4, tkt.getDistanceKm());
            ps.setDouble(5, tkt.getUnitFare());
            ps.setInt(6, tkt.getPassengerCount());
            ps.setInt(7, tkt.isReturnTrip() ? 1 : 0);
            ps.setDouble(8, tkt.getTotalAmount());
            ps.setString(9, tkt.getTicketType());
            ps.setString(10, tkt.getTicketId());
            ps.setString(11, tkt.getBookingTime());
            ps.executeUpdate();

        } catch (SQLException e) {
            System.err.println("[RDBMS Error] Failed to save ticket: " + e.getMessage());
        }
    }

    // =========================================================================
    //  Interactive SQL Console Query Executor (for UI)
    // =========================================================================

    public static class RawQueryResult {
        public final String[] columnNames;
        public final Object[][] rows;
        public final int rowCount;
        public final long executionTimeMs;
        public final String message;

        public RawQueryResult(String[] columnNames, Object[][] rows, int rowCount, long executionTimeMs, String message) {
            this.columnNames = columnNames;
            this.rows = rows;
            this.rowCount = rowCount;
            this.executionTimeMs = executionTimeMs;
            this.message = message;
        }
    }

    public static RawQueryResult executeRawQuery(String sql) {
        if (sql == null || sql.trim().isEmpty()) {
            return new RawQueryResult(new String[]{"Notice"}, new Object[][]{{"Empty SQL query"}}, 0, 0, "No query provided.");
        }

        long start = System.currentTimeMillis();
        String trimmed = sql.trim();
        boolean isSelect = trimmed.toUpperCase().startsWith("SELECT") || trimmed.toUpperCase().startsWith("PRAGMA");

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            if (isSelect) {
                try (ResultSet rs = stmt.executeQuery(trimmed)) {
                    ResultSetMetaData md = rs.getMetaData();
                    int colCount = md.getColumnCount();
                    String[] cols = new String[colCount];
                    for (int i = 1; i <= colCount; i++) {
                        cols[i - 1] = md.getColumnLabel(i);
                    }

                    List<Object[]> rowList = new ArrayList<>();
                    while (rs.next()) {
                        Object[] row = new Object[colCount];
                        for (int i = 1; i <= colCount; i++) {
                            row[i - 1] = rs.getObject(i);
                        }
                        rowList.add(row);
                    }

                    long duration = System.currentTimeMillis() - start;
                    Object[][] data = rowList.toArray(new Object[0][]);
                    return new RawQueryResult(cols, data, data.length, duration,
                            String.format("Query executed successfully. Fetched %d row(s) in %d ms.", data.length, duration));
                }
            } else {
                int affected = stmt.executeUpdate(trimmed);
                long duration = System.currentTimeMillis() - start;
                String[] cols = {"Statement Type", "Rows Affected", "Execution Time"};
                Object[][] data = {{"DML / DDL Command", affected, duration + " ms"}};
                return new RawQueryResult(cols, data, affected, duration,
                        String.format("Command executed successfully. %d row(s) affected in %d ms.", affected, duration));
            }

        } catch (SQLException e) {
            long duration = System.currentTimeMillis() - start;
            String[] cols = {"SQL Error", "Error Code"};
            Object[][] data = {{e.getMessage(), "SQL State: " + e.getSQLState()}};
            return new RawQueryResult(cols, data, 0, duration, "SQL Error: " + e.getMessage());
        }
    }

    public static Map<String, Integer> getStationTrafficSummary() {
        Map<String, Integer> map = new LinkedHashMap<>();
        String sql = """
            SELECT s.name, COUNT(t.txn_id) as traffic
            FROM stations s
            LEFT JOIN transactions t ON (t.source_station = s.name OR t.dest_station = s.name)
            GROUP BY s.id, s.name
            ORDER BY s.distance_km ASC;
        """;

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                map.put(rs.getString("name"), rs.getInt("traffic"));
            }
        } catch (SQLException e) {
            System.err.println("[RDBMS Error] Failed to aggregate station traffic: " + e.getMessage());
        }
        return map;
    }

    public static double getTotalRevenue() {
        String sql = "SELECT COALESCE(SUM(amount), 0.0) FROM transactions WHERE status = 'SUCCESS';";
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                return rs.getDouble(1);
            }
        } catch (SQLException e) {
            System.err.println("[RDBMS Error] Failed to get revenue: " + e.getMessage());
        }
        return 0.0;
    }
}
