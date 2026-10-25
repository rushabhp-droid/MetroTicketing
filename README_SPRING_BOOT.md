# Mumbai Metro Ticketing System - Spring Boot + React SPA Migration Guide

## 🚀 Quick Start

### 1. Build & Run with Automated Script (Builds React SPA + Fat JAR)
```bash
# Automatically builds React SPA with Vite, packages Fat JAR, and runs:
./build.sh build
./build.sh run
```

### 2. Or Run Directly with Maven
```bash
mvn clean package -DskipTests
java -jar target/mumbai-metro-ticketing-1.0.0.jar
# or
mvn spring-boot:run
```

Once running, navigate to:
👉 **`http://localhost:8080`** (Loads the modern animated React 18 SPA)  
👉 **`http://localhost:8080/classic`** (Loads the classic Thymeleaf Server-Rendered UI)

---

## 🌐 Full REST API & Web Routes Reference

| API Endpoint / Route | Method | Controller | Description |
| :--- | :---: | :--- | :--- |
| **`GET /`**, **`GET /react`** | `GET` | `Static / React` | Futuristic React 18 Single Page Application |
| **`GET /classic`**, **`GET /dashboard`** | `GET` | `HomeController` | Classic Thymeleaf Server-rendered dashboard |
| **`GET /api/stations`** | `GET` | `StationApiController` | Returns all 90 stations across 7 lines |
| **`GET /api/stations/lines`** | `GET` | `StationApiController` | Returns available line mappings |
| **`GET /api/stations/fare-preview`** | `GET` | `StationApiController` | Real-time live fare calculation with concessions |
| **`POST /api/tickets/book`** | `POST` | `TicketApiController` | Books Single/Return token, returns pure-Java QR Base64 |
| **`GET /api/cards`** | `GET` | `CardApiController` | Returns all polymorphic smart cards & balances |
| **`POST /api/cards/issue`** | `POST` | `CardApiController` | Issues Regular, Student (20% OFF), or Tourist card |
| **`POST /api/cards/recharge`** | `POST` | `CardApiController` | Tops up card balance and logs financial transaction |
| **`POST /api/cards/tap-in`** | `POST` | `CardApiController` | Validates AFC Turnstile gate entry tap |
| **`POST /api/cards/tap-out`** | `POST` | `CardApiController` | Computes distance fare, deducts balance, opens exit gate |
| **`POST /api/cards/toggle-block`** | `POST` | `CardApiController` | Toggles card security lock/unlock flag |
| **`GET /api/admin/stats`** | `GET` | `AdminApiController` | Returns live network revenue, traffic, card metrics |
| **`POST /api/admin/query`** | `POST` | `AdminApiController` | Executes raw SQL query on SQLite `mumbai_metro.db` |
| **`GET /api/status`** | `GET` | `StatusApiController` | Live PeakHourMonitorThread daemon rush-hour surcharge status |

---

## 🛠️ Key Technical Highlights

1. **React 18 Single Page Application:** Built with Vite and Lucide React icons, featuring an interactive visual SVG metro map, 3D animated AFC turnstiles, and laser scanner QR reveals.
2. **Zero External QR Dependencies:** The pure-Java `QRCodeGenerator` encodes data using Galois Field GF(256) Reed-Solomon math and streams it directly to browser clients as a Base64 data URI string (`data:image/png;base64,...`).
3. **Spring Managed Multithreading:** `PeakHourMonitorThread` runs as a background worker daemon bean defined in `MetroConfig.java`, monitoring peak hours and applying fare surcharges without interfering with HTTP request handling.
4. **SQLite RDBMS:** Direct JDBC integration with `mumbai_metro.db` supporting all 90 stations across 7 lines, normalized tables, foreign keys, transactions, and live SQL execution.
5. **Academic OOP Alignment:** 100% of the Mumbai University SE Sem 3 OOPM syllabus requirements (Inheritance, Polymorphism, Interfaces, Custom Exceptions, Multithreading, Synchronization, Collections, and JDBC) are preserved in the domain layer.
