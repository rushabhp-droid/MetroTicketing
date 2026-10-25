# 🎤 Viva Questions & Comprehensive Technical Answers
### Object-Oriented Programming & Enterprise Web Architecture

This document contains a curated and comprehensive repository of academic and technical viva questions and prepared answers for the **Mumbai Metro Smart Card & Fare Ticketing System**. It covers Object-Oriented Programming (OOP) paradigms, Spring Boot Web MVC architecture, multithreading and synchronization, relational database design (SQLite / JDBC), pure-Java QR mathematics, and real-time business rule validation.

---

## 📑 Table of Contents
1. [Core Object-Oriented Programming (OOP)](#1-core-object-oriented-programming-oop)
2. [Spring Boot & Web MVC Architecture](#2-spring-boot--web-mvc-architecture)
3. [Multithreading & Concurrency](#3-multithreading--concurrency)
4. [Relational Database (RDBMS & JDBC)](#4-relational-database-rdbms--jdbc)
5. [Pure-Java QR Code Engine & Mathematics](#5-pure-java-qr-code-engine--mathematics)
6. [Business Logic, Fare Rules & Station Isolation](#6-business-logic-fare-rules--station-isolation)
7. [Exception Handling & Resilience](#7-exception-handling--resilience)

---

## 1. Core Object-Oriented Programming (OOP)

### Q1. How are Abstraction and Inheritance implemented in the Smart Card module?
> **Answer:**
> We defined an abstract base class [`Card.java`](file:///home/rushabhp/Projects/CNN/src/main/java/com/mumbaimetro/model/Card.java) which encapsulates shared properties (`cardId`, `holderName`, `contactNumber`, `balance`, `isBlocked`, `inTransit`, `tapInStation`, `tapInTime`) and concrete logic (such as thread-safe `recharge()`, `deduct()`, `tapIn()`, and `tapOut()`). It also defines two abstract methods:
> 1. `public abstract double calculateDiscount(double baseFare);`
> 2. `public abstract double getMinimumBalanceRequired();`
>
> Concrete subclasses extend `Card`:
> - [`RegularCard.java`](file:///home/rushabhp/Projects/CNN/src/main/java/com/mumbaimetro/model/RegularCard.java): Standard commuter card requiring ₹50.0 minimum balance; provides a 5% loyalty discount on journeys costing $\ge$ ₹30.0.
> - [`StudentCard.java`](file:///home/rushabhp/Projects/CNN/src/main/java/com/mumbaimetro/model/StudentCard.java): Concession card requiring ₹30.0 minimum balance; overrides `calculateDiscount()` to return a flat 20% discount on all fares.
> - [`TouristPassCard.java`](file:///home/rushabhp/Projects/CNN/src/main/java/com/mumbaimetro/model/TouristPassCard.java): Flat-rate unlimited pass requiring ₹0.0 minimum balance; overrides `calculateDiscount()` to return 100% of the fare (free unlimited transit within validity days).

### Q2. How is Polymorphism demonstrated in runtime vs. compile time?
> **Answer:**
> Both types of polymorphism are actively used:
> 1. **Runtime Polymorphism (Dynamic Method Dispatch):**
>    When a passenger taps out at an AFC turnstile, [`MetroService.processTapOut()`](file:///home/rushabhp/Projects/CNN/src/main/java/com/mumbaimetro/service/MetroService.java#L180-L218) invokes:
>    ```java
>    double discount = card.calculateDiscount(baseFare);
>    ```
>    The JVM resolves `calculateDiscount()` dynamically at runtime based on the actual object reference (`StudentCard`, `RegularCard`, or `TouristPassCard`), applying the correct discount formula without `if-else` type inspections.
> 2. **Compile-Time Polymorphism (Method Overloading):**
>    In [`FareCalculator.java`](file:///home/rushabhp/Projects/CNN/src/main/java/com/mumbaimetro/service/FareCalculator.java), the fare calculation logic is overloaded:
>    - `calculateBaseFare(double distanceKm)`: Computes slab-based fare from distance.
>    - `calculateBaseFare(Station src, Station dest)`: Computes distance between stations first, then resolves the base fare.
>    - `calculateTokenFare(Station src, Station dest, int passengers, boolean isReturn, boolean isPeakHour)`: Computes compound fare including round-trip discount, rush-hour surcharges, and passenger multipliers.

### Q3. Why use Interfaces (`Payable`, `PrintableReceipt`) instead of abstract classes alone?
> **Answer:**
> Interfaces define distinct behavioral contracts independent of class hierarchies:
> - [`Payable.java`](file:///home/rushabhp/Projects/CNN/src/main/java/com/mumbaimetro/interfaces/Payable.java): Implemented by classes that support monetary transactions (`recharge()`, `deduct()`, `getBalance()`). If we later add e-wallets, credit cards, or UPI vouchers, they can implement `Payable` without inheriting state from `Card`.
> - [`PrintableReceipt.java`](file:///home/rushabhp/Projects/CNN/src/main/java/com/mumbaimetro/interfaces/PrintableReceipt.java): Implemented by both [`Card.java`](file:///home/rushabhp/Projects/CNN/src/main/java/com/mumbaimetro/model/Card.java) and [`Ticket.java`](file:///home/rushabhp/Projects/CNN/src/main/java/com/mumbaimetro/model/Ticket.java) to standardize ASCII formatted ticket receipts and card summary logs.

### Q4. How is Encapsulation enforced throughout the domain model?
> **Answer:**
> All entity fields (e.g., `stationId`, `balance`, `isBlocked`, `distanceKm`) are declared `private` or `private final`. State transitions are strictly controlled via validated accessor and mutator methods. For example, `balance` cannot be directly modified; it can only change through `recharge(amount)` or `deduct(amount)`, which validate positive inputs and enforce minimum balance invariants.

---

## 2. Spring Boot & Web MVC Architecture

### Q5. Why migrate this project from desktop Java Swing to Spring Boot?
> **Answer:**
> Java Swing is desktop-bound, platform-dependent, and prone to display driver and threading issues (`SwingUtilities.invokeLater`). Migrating to Spring Boot 3.3.5 brings enterprise advantages:
> 1. **Platform Independence:** Accessible from any modern browser (desktop, tablet, mobile) through standard HTTP/HTTPS.
> 2. **Decoupled Architecture:** Separation of concerns between domain services, REST APIs, and presentation layers.
> 3. **Scalability:** Stateless HTTP endpoints allow horizontal scaling behind reverse proxies.
> 4. **Modern UI Capabilities:** Supports rich Single Page Applications (React 18 with SVG interactive maps and CSS animations) alongside server-side rendered Thymeleaf views.
> 5. **Preserved OOP Core:** 100% of the core Java OOP domain model, custom exceptions, multithreading, and JDBC remain identical in the service layer.

### Q6. How does Spring Boot manage dependency injection in this application?
> **Answer:**
> Spring's Inversion of Control (IoC) container manages component lifecycles. In [`MetroConfig.java`](file:///home/rushabhp/Projects/CNN/src/main/java/com/mumbaimetro/config/MetroConfig.java), service beans are declared using `@Bean`:
> ```java
> @Bean
> public MetroService metroService() {
>     return new MetroService();
> }
> ```
> Controllers like [`TicketApiController.java`](file:///home/rushabhp/Projects/CNN/src/main/java/com/mumbaimetro/api/TicketApiController.java) receive `MetroService` via constructor-based dependency injection, promoting loose coupling and testability.

### Q7. How does the frontend communicate with the Spring Boot backend?
> **Answer:**
> The frontend is a decoupled React 18 SPA built with Vite. It interacts with the backend via standard RESTful JSON APIs:
> - `GET /api/stations`: Retrieves all 90 operational stations across 7 lines.
> - `GET /api/stations/lines`: Retrieves available metro corridors.
> - `GET /api/stations/fare-preview`: Fetches real-time distance and fare calculations.
> - `POST /api/tickets/book`: Generates digital tickets and Base64 QR tokens.
> - `POST /api/cards/tap-in` and `POST /api/cards/tap-out`: Simulates AFC turnstile passage.
> - `GET /api/status`: Provides live daemon rush-hour monitoring status.

---

## 3. Multithreading & Concurrency

### Q8. How is Multithreading implemented and managed in Spring Boot?
> **Answer:**
> Multithreading is implemented via [`PeakHourMonitorThread.java`](file:///home/rushabhp/Projects/CNN/src/main/java/com/mumbaimetro/threads/PeakHourMonitorThread.java), which extends `java.lang.Thread`.
> Rather than manually spawning unmanaged threads, it is declared as a Spring `@Bean` with `destroyMethod = "interrupt"` in [`MetroConfig.java`](file:///home/rushabhp/Projects/CNN/src/main/java/com/mumbaimetro/config/MetroConfig.java):
> ```java
> @Bean(destroyMethod = "interrupt")
> public PeakHourMonitorThread peakHourMonitorThread() {
>     PeakHourMonitorThread thread = new PeakHourMonitorThread();
>     thread.setDaemon(true);
>     thread.start();
>     return thread;
> }
> ```
> Marking it as a daemon thread ensures it does not prevent JVM shutdown. When Spring shuts down, it invokes `interrupt()` to gracefully terminate the worker loop.

### Q9. What logic does `PeakHourMonitorThread` execute?
> **Answer:**
> The thread executes an infinite background loop (`while (!isInterrupted())`) with a 15-second polling interval (`Thread.sleep(15000)`). It inspects the current system time using `LocalTime.now()` and checks whether the time falls within Mumbai Metro peak commuter hours:
> - Morning Peak: 08:00 – 11:30
> - Evening Peak: 17:30 – 21:00
>
> When rush hour is active, it flags `isPeakHour = true`, prompting `FareCalculator` to apply the ₹5.00 surge. It also exposes a simulated override toggle for testing.

### Q10. How is Thread Synchronization achieved to prevent race conditions?
> **Answer:**
> Smart card financial transactions can be called concurrently (e.g., automated batch recharge or multiple AFC turnstile taps). In [`Card.java`](file:///home/rushabhp/Projects/CNN/src/main/java/com/mumbaimetro/model/Card.java), critical methods are declared `synchronized`:
> ```java
> public synchronized void recharge(double amount) { ... }
> public synchronized void deduct(double amount) throws InsufficientBalanceException { ... }
> public synchronized void tapIn(Station station) { ... }
> public synchronized void tapOut() { ... }
> ```
> This acquires the intrinsic object monitor lock, guaranteeing mutual exclusion and ensuring balance updates and transit state changes are completely atomic and thread-safe.

---

## 4. Relational Database (RDBMS & JDBC)

### Q11. Why use SQLite and direct JDBC instead of an in-memory Map or heavy database like MySQL?
> **Answer:**
> 1. **Zero Configuration & Portability:** SQLite is an embedded serverless relational database stored as a single file (`mumbai_metro.db`). It runs immediately on any operating system without installing database services.
> 2. **ACID Compliance:** Provides full transactional integrity (`PRAGMA foreign_keys = ON;`).
> 3. **Academic Clarity:** Direct JDBC via [`MetroDatabaseManager.java`](file:///home/rushabhp/Projects/CNN/src/main/java/com/mumbaimetro/data/MetroDatabaseManager.java) explicitly demonstrates JDBC setup (`Connection`, `Statement`, `PreparedStatement`, `ResultSet`, SQL DDL/DML), which is essential for understanding lower-level relational database interactions.

### Q12. Describe the database schema and table constraints.
> **Answer:**
> The database contains 4 normalized tables:
> 1. `stations`: `id` (PK), `name`, `line_code`, `line_name`, `distance_km`, `interchange`, with `UNIQUE(line_code, name)` to permit interchange stations sharing the same name across different lines (e.g., Marol Naka on Line 1 and Line 3).
> 2. `cards`: `card_id` (PK), `holder_name`, `contact_number`, `card_type`, `balance`, `is_blocked`, `in_transit`, `tap_in_station_id` (FK to `stations`), `college_name`, `student_roll`, `validity_days`, `created_at`.
> 3. `tickets`: `ticket_id` (PK), `source_id` (FK), `dest_id` (FK), `distance_km`, `unit_fare`, `passenger_count`, `is_return`, `total_fare`, `ticket_type`, `qr_data`, `booked_at`.
> 4. `transactions`: `txn_id` (PK), `ref_id`, `txn_type`, `amount`, `source_station`, `dest_station`, `balance_after`, `status`, `created_at`.

### Q13. How are SQL Injection vulnerabilities prevented?
> **Answer:**
> All user-submitted parameters in database queries (such as ticket bookings, card recharge, tap-in/tap-out, card lookups) are executed exclusively through parameterized `PreparedStatement` instances with type-safe bindings (`setString()`, `setInt()`, `setDouble()`). Raw SQL execution in the Admin Console is isolated and intended strictly for database administration and review.

---

## 5. Pure-Java QR Code Engine & Mathematics

### Q14. How does the application generate QR codes without third-party libraries like ZXing?
> **Answer:**
> [`QRCodeGenerator.java`](file:///home/rushabhp/Projects/CNN/src/main/java/com/mumbaimetro/qr/QRCodeGenerator.java) is a zero-dependency QR generation engine implemented entirely in core Java:
> 1. **Galois Field $GF(2^{8})$ Math:** Uses polynomial arithmetic with primitive polynomial $x^8 + x^4 + x^3 + x^2 + 1$ (285) using precomputed exponential (`EXP_TABLE`) and logarithm (`LOG_TABLE`) lookup arrays.
> 2. **Reed-Solomon Error Correction:** Computes error-correction parity codewords by polynomial synthetic division of message polynomials against generator polynomials.
> 3. **Matrix Construction:** Allocates a 2D boolean grid and embeds standard QR Version 2/3 patterns: finder patterns (7x7 nested boxes with 1-module separators), timing patterns (alternating black/white lines on row/column 6), dark module, and alignment patterns.
> 4. **Data Masking:** Applies standard QR masking patterns (e.g., `(row + col) % 2 == 0`) to prevent uneven black/white distribution and finder pattern illusions.
> 5. **Base64 Rendering:** Renders the matrix into a pure-Java `BufferedImage`, streams it into a `ByteArrayOutputStream`, and encodes it to a Base64 string (`data:image/png;base64,...`) for instant zero-disk browser display.

### Q15. What data is embedded in the Metro QR token?
> **Answer:**
> The QR payload contains a pipe-delimited security token:
> ```text
> MUMBAI-METRO|TKT-49201|Versova|Ghatkopar|FARE:40.00|PASS:1|SINGLE|TIME:2026-10-25 18:30:00
> ```
> This structure allows AFC optical turnstile scanners to parse, validate line alignment, and check timestamps instantly.

---

## 6. Business Logic, Fare Rules & Station Isolation

### Q16. Why must passengers only select stations from the SAME line?
> **Answer:**
> In rapid transit rail networks, physical lines often operate with separate rolling stock, fare gates, and dedicated ticketing contracts. Permitting passengers to book single-hop QR tokens between unlinked lines (e.g., Line 1 Versova to Line 2A Dahisar East) without designated interchange gate validation causes accounting discrepancies and AFC gate clearance failures.
> Consequently, both the user interface and backend business logic strictly enforce:
> ```java
> if (!src.getLineCode().equals(dest.getLineCode())) {
>     throw new InvalidStationException("Cannot select stations from two different lines! Both stations must be on the same Metro line.");
> }
> ```
> This prevents cross-line station selection across the Interactive Map, QR Ticket Booking, and AFC Turnstile gates.

### Q17. How does the Dynamic Fare Slabs calculation work?
> **Answer:**
> In [`FareCalculator.java`](file:///home/rushabhp/Projects/CNN/src/main/java/com/mumbaimetro/service/FareCalculator.java#L88-L115), distance-based fare slabs are computed as follows:
> - $0 \text{ to } 3\text{ km}$: ₹10.00
> - $3 \text{ to } 6\text{ km}$: ₹20.00
> - $6 \text{ to } 9\text{ km}$: ₹30.00
> - $9 \text{ to } 12\text{ km}$: ₹40.00
> - $12 \text{ to } 15\text{ km}$: ₹50.00
> - $15 \text{ to } 18\text{ km}$: ₹60.00
> - $18 \text{ to } 21\text{ km}$: ₹70.00
> - $21 \text{ to } 24\text{ km}$: ₹80.00
> - $> 24\text{ km}$: ₹90.00 – ₹110.00
>
> **Adjustments:**
> - If `isPeakHour == true`, ₹5.00 surcharge is added to the base unit fare.
> - If `isReturn == true`, a 10% round-trip concession is applied: $\text{Total} = (\text{Unit} \times 2 \times 0.90) \times \text{passengers}$.
> - Student cardholders polymorphically receive a 20% discount on final fare deductions.

---

## 7. Exception Handling & Resilience

### Q18. What custom user-defined exceptions exist in the project, and why?
> **Answer:**
> Standard Java exceptions (`IllegalArgumentException`, `NullPointerException`) do not convey business domain concepts. We created 4 custom checked exceptions inheriting from `java.lang.Exception`:
> 1. [`InvalidStationException.java`](file:///home/rushabhp/Projects/CNN/src/main/java/com/mumbaimetro/exceptions/InvalidStationException.java): Thrown when source and destination are identical, null, or belong to different lines.
> 2. [`InsufficientBalanceException.java`](file:///home/rushabhp/Projects/CNN/src/main/java/com/mumbaimetro/exceptions/InsufficientBalanceException.java): Thrown when card balance is less than journey fare at exit tap-out.
> 3. [`MinimumBalanceException.java`](file:///home/rushabhp/Projects/CNN/src/main/java/com/mumbaimetro/exceptions/MinimumBalanceException.java): Thrown when card balance is below mandatory minimum threshold (₹50 for Regular, ₹30 for Student) at entry tap-in.
> 4. [`CardBlockedException.java`](file:///home/rushabhp/Projects/CNN/src/main/java/com/mumbaimetro/exceptions/CardBlockedException.java): Thrown when an account has been flagged as blocked or suspended.

### Q19. How are exceptions caught and rendered in the Web UI?
> **Answer:**
> - In Spring MVC Controllers ([`TicketController.java`](file:///home/rushabhp/Projects/CNN/src/main/java/com/mumbaimetro/controller/TicketController.java), [`CardController.java`](file:///home/rushabhp/Projects/CNN/src/main/java/com/mumbaimetro/controller/CardController.java)):
>   Service calls are wrapped in `try-catch` blocks. Caught exception messages are passed to redirect attributes:
>   ```java
>   redirectAttributes.addFlashAttribute("error", e.getMessage());
>   ```
>   Thymeleaf displays this in a dismissable error banner: `<div th:if="${error}" class="alert alert-error">`.
> - In REST API Controllers ([`TicketApiController.java`](file:///home/rushabhp/Projects/CNN/src/main/java/com/mumbaimetro/api/TicketApiController.java), [`CardApiController.java`](file:///home/rushabhp/Projects/CNN/src/main/java/com/mumbaimetro/api/CardApiController.java)):
>   Exceptions return `HTTP 400 Bad Request` with structured JSON payloads:
>   ```json
>   { "success": false, "message": "Cannot select stations from two different lines!..." }
>   ```
>   The React SPA catches this and displays toast notifications.

---

### Q20. How is data integrity guaranteed during automated gate passage?
> **Answer:**
> The gate operates as a two-phase state machine:
> 1. **Tap-In:** Verifies card is not blocked, balance $\ge$ minimum balance, and card is not already marked `inTransit`. Sets `inTransit = true` and records `tapInStation`.
> 2. **Tap-Out:** Verifies card is currently `inTransit`, exit station is on the same line as tap-in station, computes fare, and deducts amount. Only upon successful deduction is `inTransit` set to `false`.
> If a passenger attempts double tap-in or double tap-out, an `IllegalStateException` or `InvalidStationException` is thrown, keeping the gate closed.
