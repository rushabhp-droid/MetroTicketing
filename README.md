# Mumbai Metro One (MMOPL) - Smart Card & Fare Ticketing System
### Academic Mini-Project | SE Semester 3 - OOPM
#### Modernized Spring Boot 3.3.5 Web Application + SQLite Relational Database (RDBMS)

---

## 📌 Project Overview
The **Mumbai Metro Smart Card & Fare Ticketing System** is an enterprise-grade full-stack web application built using **Java 17**, **Spring Boot 3.3.5**, a modern **React 18 Single Page Application (SPA)** with Vite and interactive animations, and an embedded **SQLite Relational Database (JDBC)**. It simulates the end-to-end Automated Fare Collection (AFC), passenger smart card management, dynamic distance-based fare calculation, and digital QR ticketing infrastructure across all **7 operational Mumbai & Navi Mumbai Metro corridors (90 Stations)**.

The project features a decoupled architecture: a high-performance **Spring Boot REST API** backend paired with a reactive **React 18** frontend featuring an interactive visual SVG metro line map, 3D animated AFC turnstile gates, live fare calculations, holographic card tilt effects, and instant Base64 pure-Java QR token reveals. The core Object-Oriented Programming (OOP) foundation—including polymorphic smart card hierarchies, user-defined business exceptions, thread-safe financial operations, multithreaded rush-hour monitoring, and zero-dependency Galois Field QR generation—remains 100% intact and thoroughly tested.

---

## 🎯 OOPM Syllabus Mapping

| Syllabus Requirement | Implementation in Code | Exact Source File Reference |
| :--- | :--- | :--- |
| **Abstraction & Inheritance** | Abstract base class `Card` inherited by concrete subtypes `RegularCard`, `StudentCard`, and `TouristPassCard`. | [`model/Card.java`](file:///home/rushabhp/Projects/CNN/src/main/java/com/mumbaimetro/model/Card.java), [`model/RegularCard.java`](file:///home/rushabhp/Projects/CNN/src/main/java/com/mumbaimetro/model/RegularCard.java), [`model/StudentCard.java`](file:///home/rushabhp/Projects/CNN/src/main/java/com/mumbaimetro/model/StudentCard.java), [`model/TouristPassCard.java`](file:///home/rushabhp/Projects/CNN/src/main/java/com/mumbaimetro/model/TouristPassCard.java) |
| **Polymorphism (Runtime)** | Overridden method `calculateDiscount(double baseFare)` dynamically calculates 20% concession for students vs 5% loyalty discount for commuters. | [`model/StudentCard.java`](file:///home/rushabhp/Projects/CNN/src/main/java/com/mumbaimetro/model/StudentCard.java), [`model/RegularCard.java`](file:///home/rushabhp/Projects/CNN/src/main/java/com/mumbaimetro/model/RegularCard.java) |
| **Polymorphism (Compile-time)** | Method overloading in `FareCalculator`: `calculateBaseFare(double)`, `calculateBaseFare(Station, Station)`, `calculateTokenFare(...)`. | [`service/FareCalculator.java`](file:///home/rushabhp/Projects/CNN/src/main/java/com/mumbaimetro/service/FareCalculator.java) |
| **Interfaces** | `Payable` (contract for financial recharge/deduction) and `PrintableReceipt` (contract for receipt generation). | [`interfaces/Payable.java`](file:///home/rushabhp/Projects/CNN/src/main/java/com/mumbaimetro/interfaces/Payable.java), [`interfaces/PrintableReceipt.java`](file:///home/rushabhp/Projects/CNN/src/main/java/com/mumbaimetro/interfaces/PrintableReceipt.java) |
| **User-Defined Exceptions** | 4 Custom Exception classes handling edge cases: `InsufficientBalanceException`, `MinimumBalanceException`, `InvalidStationException`, `CardBlockedException`. | [`exceptions/*.java`](file:///home/rushabhp/Projects/CNN/src/main/java/com/mumbaimetro/exceptions) |
| **Multithreading** | Background daemon worker `PeakHourMonitorThread` extending `Thread` that monitors rush hours and dynamically adjusts fares in real time. Managed as a lifecycle bean via `@Configuration`. | [`threads/PeakHourMonitorThread.java`](file:///home/rushabhp/Projects/CNN/src/main/java/com/mumbaimetro/threads/PeakHourMonitorThread.java), [`config/MetroConfig.java`](file:///home/rushabhp/Projects/CNN/src/main/java/com/mumbaimetro/config/MetroConfig.java) |
| **Thread Synchronization** | `synchronized` methods on `recharge()`, `deduct()`, `tapIn()`, and `tapOut()` to prevent race conditions during concurrent smart card transactions. | [`model/Card.java`](file:///home/rushabhp/Projects/CNN/src/main/java/com/mumbaimetro/model/Card.java) |
| **Collections Framework** | `ArrayList` for stations/transactions, `HashMap` for $O(1)$ smart card lookups, `LinkedHashMap` for line ordering. | [`data/DataStore.java`](file:///home/rushabhp/Projects/CNN/src/main/java/com/mumbaimetro/data/DataStore.java) |
| **Relational Database (RDBMS & JDBC)** | Embedded SQLite (`mumbai_metro.db`) with Foreign Keys, 4 normalized tables (`stations`, `cards`, `tickets`, `transactions`), and live web SQL query console. | [`data/MetroDatabaseManager.java`](file:///home/rushabhp/Projects/CNN/src/main/java/com/mumbaimetro/data/MetroDatabaseManager.java), [`controller/AdminController.java`](file:///home/rushabhp/Projects/CNN/src/main/java/com/mumbaimetro/controller/AdminController.java) |
| **Pure-Java QR Engine** | Zero-dependency Galois Field $GF(2^{8})$ arithmetic, Reed-Solomon polynomial division, 2D matrix masking, and Base64 data URI generation for direct browser rendering. | [`qr/QRCodeGenerator.java`](file:///home/rushabhp/Projects/CNN/src/main/java/com/mumbaimetro/qr/QRCodeGenerator.java) |
| **Spring Boot Web MVC** | Spring MVC Controllers (`@Controller`), Thymeleaf HTML5 templates, Flash messaging (`RedirectAttributes`), and component scanning. | [`MetroApplication.java`](file:///home/rushabhp/Projects/CNN/src/main/java/com/mumbaimetro/MetroApplication.java), [`controller/*.java`](file:///home/rushabhp/Projects/CNN/src/main/java/com/mumbaimetro/controller) |

---

## 🏛️ System Architecture & Project Structure

```text
/home/rushabhp/Projects/CNN
├── build.sh                                # Linux/macOS build & run automation script
├── build.bat                               # Windows build & run batch script
├── pom.xml                                 # Maven project descriptor (Spring Boot 3.3.5)
├── README.md                               # Comprehensive documentation & syllabus mapping
├── README_SPRING_BOOT.md                   # Quick-start web migration guide
├── lib/                                    # Offline driver JARs (sqlite-jdbc, slf4j)
└── src
    └── main
        ├── java/com/mumbaimetro
        │   ├── MetroApplication.java       # Spring Boot main entry point & DB startup init
        │   ├── config
        │   │   └── MetroConfig.java        # Spring IoC bean definitions (Service, Monitor Thread)
        │   ├── controller
        │   │   ├── HomeController.java     # Dashboard, analytics, and traffic summary routes
        │   │   ├── TicketController.java   # Single/Return journey QR ticket booking routes
        │   │   ├── CardController.java     # Smart card issue, recharge, tap-in/out gate routes
        │   │   └── AdminController.java    # SQLite RDBMS console & raw SQL query execution
        │   ├── data
        │   │   ├── DataStore.java          # In-memory repository cache & stats accumulator
        │   │   └── MetroDatabaseManager.java# SQLite JDBC manager, DDL schema, and raw queries
        │   ├── exceptions
        │   │   ├── CardBlockedException.java
        │   │   ├── InsufficientBalanceException.java
        │   │   ├── InvalidStationException.java
        │   │   └── MinimumBalanceException.java
        │   ├── interfaces
        │   │   ├── Payable.java            # Recharge / deduction financial contract
        │   │   └── PrintableReceipt.java   # ASCII / text receipt generation contract
        │   ├── model
        │   │   ├── Card.java               # Abstract base smart card entity
        │   │   ├── RegularCard.java        # Commuter card with loyalty discount
        │   │   ├── StudentCard.java        # Student pass with 20% concession
        │   │   ├── TouristPassCard.java    # Unlimited travel pass
        │   │   ├── Station.java            # Metro station with line code and distance
        │   │   ├── Ticket.java             # Digital QR ticket model
        │   │   └── Transaction.java        # Financial & journey audit trail entity
        │   ├── qr
        │   │   └── QRCodeGenerator.java    # Pure-Java Reed-Solomon GF(256) QR Code encoder
        │   ├── service
        │   │   ├── FareCalculator.java     # Compile-time overloaded distance & fare calculator
        │   │   └── MetroService.java       # Business service coordinating AFC validation
        │   ├── test
        │   │   └── MetroSystemTest.java    # Self-contained academic unit test suite (9 checks)
        │   └── threads
        │       └── PeakHourMonitorThread.java # Background rush-hour monitoring daemon
        └── resources
            ├── application.properties      # Spring Boot server port (8080) & Thymeleaf config
            ├── static
            │   └── css
            │       └── style.css           # Modern custom stylesheet (cards, tables, badges)
            └── templates
                ├── layout.html             # Base layout template with navigation & alerts
                ├── dashboard.html          # Operational overview, stats cards, line table
                ├── tickets.html            # QR token booking form, receipt & Base64 QR view
                ├── cards.html              # Smart card issuance, recharge & AFC gate taps
                └── admin.html              # SQLite database manager & live SQL console
```

---

## 🚇 Operational Mumbai & Navi Mumbai Metro Network (90 Stations)

The application models all **7 operational Metro corridors** matching the official network map with active interchanges:

1. **Line 1 (Blue Line) - 11.4 km (12 Stations):**
   * Versova ↔ D.N. Nagar (*Interchange Line 2A*) ↔ Azad Nagar ↔ Andheri (*Interchange Western Railway Local*) ↔ Western Express Highway (*Interchange Line 7 Gundavali*) ↔ Chakala (J.B. Nagar) ↔ Airport Road ↔ Marol Naka (*Interchange Line 3*) ↔ Saki Naka ↔ Asalpha ↔ Jagruti Nagar ↔ Ghatkopar (*Interchange Central Railway Local*)
2. **Line 2A (Yellow Line) - 18.6 km (17 Stations):**
   * Dahisar East (*Interchange Line 7 & Line 9*) ↔ Anand Nagar ↔ Kandarpada ↔ Mandapeshwar - IC Colony ↔ Eksar ↔ Borivali West (*Western Railway Local*) ↔ Shimpoli ↔ Kandivali West ↔ Dahanukarwadi ↔ Valnai - Meeth Chowky ↔ Malad West ↔ Lower Malad ↔ Bangur Nagar ↔ Goregaon West ↔ Oshiwara ↔ Lower Oshiwara ↔ Andheri West / D.N. Nagar (*Interchange Line 1*)
3. **Line 7 (Red Line) - 16.5 km (14 Stations):**
   * Dahisar East (*Interchange Line 2A & Line 9*) ↔ Ovaripada ↔ Rashtriya Udyan (*National Park*) ↔ Devipada ↔ Magathane ↔ Poisar ↔ Akurli (*Kandivali East*) ↔ Kurar ↔ Dindoshi (*Malad East*) ↔ Aarey (*Interchange Line 3 JVLR*) ↔ Goregaon East ↔ Jogeshwari East ↔ Mogra ↔ Gundavali (*Interchange Line 1 WEH*)
4. **Line 9 (Red Extension) - 6.2 km (4 Stations):**
   * Dahisar East (*Interchange Line 2A & Line 7*) ↔ Pandurangwadi ↔ Miragaon ↔ Kashigaon (*Mira Road Extension*)
5. **Line 3 (Aqua Line Underground) - 34.5 km (27 Stations):**
   * Aarey JVLR (*Interchange Line 7*) ↔ SEEPZ ↔ MIDC Andheri ↔ Marol Naka (*Interchange Line 1*) ↔ CSMI Airport - T2 (*International*) ↔ Sahar Road ↔ CSMI Airport - T1 (*Domestic*) ↔ Santacruz Metro (*Western Railway*) ↔ Bandra Colony ↔ BKC (*Bullet Train*) ↔ Dharavi ↔ Shitala Devi Mandir ↔ Dadar Metro (*Central & Western Railway*) ↔ Siddhivinayak (*Prabhadevi*) ↔ Worli ↔ Acharya Atre Chowk ↔ Science Centre ↔ Mahalaxmi Metro (*Western Railway / Monorail*) ↔ Mumbai Central - JSS Metro (*Western Railway & Outstation*) ↔ Grant Road (*Western Railway*) ↔ Charni Road - Girgaon (*Western Railway*) ↔ Marine Lines - Kalbadevi (*Western Railway*) ↔ CSMT (*Central Railway & Outstation*) ↔ Hutatma Chowk (*Flora Fountain*) ↔ Churchgate (*Western Railway*) ↔ Vidhan Bhavan ↔ Cuffe Parade (*South Mumbai Terminal*)
6. **Line 2B (Yellow Extension) - 5.5 km (5 Stations):**
   * Mandale (*Car Depot*) ↔ Mankhurd (*Harbour Line Local*) ↔ Deonar ↔ C Shivaji Maharaj Chowk ↔ Diamond Garden (*Chembur Local Interchange*)
7. **Navi Mumbai Line 1 (NMM-1) - 11.1 km (11 Stations):**
   * Belapur CBD (*Harbour Line Local*) ↔ RBI Colony ↔ Belpada ↔ Utsav Chowk ↔ Kendriya Vihar ↔ Kharghar Village ↔ Central Park ↔ Pethpada ↔ Amandoot ↔ Pethali Taloja ↔ Pendhar (*Taloja MIDC Terminal*)

---

## 💰 Dynamic Fare Calculation Slabs

| Distance Range | Standard Single Fare | Peak Rush (+₹5.00) | Notes |
| :--- | :--- | :--- | :--- |
| **0 to 3 km** | ₹10.00 | ₹15.00 | Minimum fare base |
| **3 to 6 km** | ₹20.00 | ₹25.00 | Short commuter hop |
| **6 to 9 km** | ₹30.00 | ₹35.00 | Medium distance |
| **9 to 12 km** | ₹40.00 | ₹45.00 | Line 1 full stretch |
| **12 to 15 km** | ₹50.00 | ₹55.00 | Extended distance |
| **15 to 18 km** | ₹60.00 | ₹65.00 | Lines 2A & 7 end-to-end |
| **18 to 21 km** | ₹70.00 | ₹75.00 | Cross-line journeys |
| **21 to 24 km** | ₹80.00 | ₹85.00 | Long corridor trips |
| **> 24 km** | ₹90.00 - ₹110.00 | ₹95.00 - ₹115.00 | Line 3 full stretch (33.5 km) |

* **Student Concession:** Flat 20% discount applied polymorphically to all travel fares.
* **Regular Card Loyalty Discount:** 5% discount on fares $\ge$ ₹30.00.
* **Return Journey Tokens:** Automatic 10% round-trip concession.
* **Peak Hour Rush Surcharge:** +₹5.00 surcharge automatically monitored by `PeakHourMonitorThread`.

---

## 🚀 How to Build and Run the Application

### Method 1: Using the Automated Build Script (Recommended)

```bash
# 1. Compile and package the Spring Boot executable Fat JAR:
./build.sh build

# 2. Launch the Spring Boot Web Server (port 8080):
./build.sh run

# 3. Execute the automated academic unit test suite:
./build.sh test

# 4. Clean build artifacts and temporary files:
./build.sh clean
```

> **For Windows (College Lab PCs):** Run `build.bat`, `build.bat run`, or `build.bat test`.

### Method 2: Using Maven Directly

```bash
# 1. Package the Spring Boot Fat JAR:
mvn clean package -DskipTests

# 2. Run the executable JAR:
java -jar target/mumbai-metro-ticketing-1.0.0.jar

# 3. Or launch via Spring Boot Maven plugin:
mvn spring-boot:run
```

### Accessing the Web Application

Once launched, open your web browser and navigate to:
👉 **`http://localhost:8080`**

* **Dashboard (`/dashboard`):** Real-time system statistics, network stations, active line badges, recent transaction audit trail.
* **QR Tickets (`/tickets`):** Interactive booking form with origin/destination dropdowns, passenger count, return trip concession, and live Base64 QR code rendering.
* **Smart Cards (`/cards`):** Card issuance form, balance recharge, AFC turnstile gate tap-in and tap-out simulation, card blocking/unblocking.
* **Admin & RDBMS (`/admin`):** Embedded SQLite database viewer, table schemas, and live SQL query console with preset queries.

---

## 🧪 Automated Unit Verification Test Suite

Run the academic test suite at any time:
```bash
./build.sh test
```

**Verification Results:**
```text
=========================================================
       MUMBAI METRO SYSTEM - AUTOMATED TEST SUITE        
=========================================================
 [PASS] Fare Slabs Calculation (0-3km: ₹10, 3-6km: ₹20, 6-9km: ₹30, 9-12km: ₹40)
 [PASS] Custom Exception: InvalidStationException thrown on same origin & destination
 [PASS] Custom Exception: InvalidStationException thrown when selecting stations from different lines
 [PASS] Polymorphic Concession: StudentCard gives 20% discount (₹8 on ₹40)
 [PASS] Polymorphic Concession: RegularCard loyalty discount on high fares
 [PASS] Thread-Safe Recharge: Balance increased to ₹250.0
 [PASS] Custom Exception: MinimumBalanceException correctly triggered when balance < ₹50
 [PASS] Custom Exception: InsufficientBalanceException caught on deficit deduction
 [PASS] QR Code Engine: Generates valid scannable BufferedImage & matrix without external libs
 [PASS] Relational Database: SQLite connected, schema created & 90 stations across 7 lines verified via SQL
---------------------------------------------------------
 Test Results: 10 / 10 PASSED (100% Success Rate)
=========================================================
```

---

## 📚 Comprehensive Feature Documentation

Extensive documentation for each architectural component and system feature is organized in the [`docs/`](file:///home/rushabhp/Projects/CNN/docs) directory:

- 🏛️ [**System Architecture & Web MVC Design**](docs/architecture.md): Layered architecture, REST APIs, Inversion of Control, and sequence diagrams.
- 🚇 [**Metro Network & Same-Line Station Routing**](docs/metro_network.md): 7 corridors, 90 stations, distance tracking, and **same-line station selection enforcement**.
- 🎫 [**QR Ticketing & Dynamic Fare Engine**](docs/ticketing_and_fares.md): Distance fare slabs, peak rush surcharges, and **pure-Java Reed-Solomon $GF(2^8)$ QR code generator**.
- 💳 [**Smart Card System & Polymorphism**](docs/smart_cards.md): Polymorphic card hierarchy (`RegularCard`, `StudentCard`, `TouristPassCard`), `Payable` interface, and thread safety.
- 🚪 [**AFC Turnstile Gate Simulator**](docs/afc_turnstile.md): Motorized barrier flap physics, RFID sensor simulation, two-phase tap-in/tap-out state machine, and line clearance.
- ⏱️ [**Multithreading & Background Daemons**](docs/multithreading.md): `PeakHourMonitorThread` daemon, Spring bean lifecycle integration, and concurrency control.
- 🗄️ [**Relational Database & SQL Console**](docs/database_rdbms.md): SQLite schema DDL, foreign key relationships, JDBC prepared statements, and live SQL execution console.
- 📊 [**Analytics Dashboard & Traffic Insights**](docs/analytics_dashboard.md): Gross revenue tracking, station footfall counters, passenger volume metrics, and transaction audit trails.

---

## 🎤 Viva Questions & Comprehensive Answers Guide

The complete collection of viva questions, conceptual deep dives, and prepared model answers has been moved to its own dedicated guide in the documentation directory:

👉 **Read the Full Viva Guide:** [**`docs/viva_questions.md`**](docs/viva_questions.md)

### Quick Overview of Covered Viva Topics:
1. **Core OOP:** Abstraction, inheritance, runtime dynamic method dispatch, compile-time method overloading, interface segregation, and encapsulation invariants.
2. **Spring Boot MVC:** Web migration advantages, inversion of control (IoC), dependency injection, REST API design, and Thymeleaf flash attributes.
3. **Multithreading:** `PeakHourMonitorThread` background worker daemon, Spring bean lifecycle (`destroyMethod = "interrupt"`), memory visibility (`volatile`), and thread synchronization (`synchronized`).
4. **Relational Database (RDBMS):** SQLite embedded architecture, foreign key integrity, ACID transactions, and parameterized SQL injection prevention.
5. **Pure-Java QR Math:** Galois Field $GF(2^8)$ arithmetic, Reed-Solomon polynomial division, matrix masking, and zero-disk Base64 data streaming.
6. **Station Isolation:** Real-world transit justification and technical enforcement of the same-line station selection constraint.
7. **Custom Exceptions:** Hierarchy and handling of `InvalidStationException`, `InsufficientBalanceException`, `MinimumBalanceException`, and `CardBlockedException`.
