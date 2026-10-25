# 📚 Mumbai Metro System Documentation

Welcome to the comprehensive documentation suite for the **Mumbai Metro Smart Card & Fare Ticketing System**. This documentation covers system design, architectural layers, object-oriented domain models, relational database schemas, algorithm specifications, and viva examination preparation.

---

## 📑 Documentation Index

| Document | Title | Key Topics Covered |
| :--- | :--- | :--- |
| [**System Architecture**](architecture.md) | Web MVC & Layered Design | Spring Boot 3.3.5, React 18 SPA, Decoupled REST APIs, Inversion of Control, Layered architecture, Sequence diagrams. |
| [**Metro Network & Routing**](metro_network.md) | Network Corridors & Line Isolation | 7 Operational Corridors, 90 Stations, Station distances, **Same-line selection constraint**, Station entity model. |
| [**QR Ticketing & Fares**](ticketing_and_fares.md) | Dynamic Fare Engine & QR Generator | Distance fare slabs (0-3km to >24km), Peak rush surcharges, Return trip concessions, **Pure-Java Reed-Solomon $GF(2^8)$ QR engine**, In-memory Base64 streaming. |
| [**Smart Card System**](smart_cards.md) | Polymorphic Card Hierarchy | `Card` abstract class, `RegularCard`, `StudentCard` (20% discount), `TouristPassCard`, `Payable` interface, Thread-safe recharge/deduction, Card blocking. |
| [**AFC Turnstile Simulator**](afc_turnstile.md) | Automated Fare Collection Gates | Physics barrier flap animations, RFID sensor simulation, Two-phase Tap-In and Tap-Out state machine, Minimum balance enforcement, Line verification. |
| [**Multithreading & Daemons**](multithreading.md) | Background Monitoring Threads | `PeakHourMonitorThread`, Spring `@Bean` lifecycle with `destroyMethod="interrupt"`, Daemon thread configuration, Concurrency safety. |
| [**Relational Database (RDBMS)**](database_rdbms.md) | SQLite Schema & SQL Administration | `mumbai_metro.db`, 4 normalized tables (`stations`, `cards`, `tickets`, `transactions`), Foreign keys, JDBC `PreparedStatement`, Live SQL console. |
| [**Analytics Dashboard**](analytics_dashboard.md) | Operational Metrics & Traffic Insights | Gross revenue tracking, Station footfall counters, Passenger transit volume, Immutable financial audit trail. |
| [**Viva Questions & Answers**](viva_questions.md) | Academic & Technical Viva Guide | 20+ Detailed questions and prepared answers covering OOP, Spring Boot, Concurrency, SQLite, QR mathematics, and Exception handling. |

---

## 🚀 Quick Navigation

- **For Viva Preparation:** Start directly with [Viva Questions & Answers](viva_questions.md).
- **For Architecture & Code Structure:** Read [System Architecture](architecture.md) and [Smart Card Polymorphism](smart_cards.md).
- **For Network Routing & Business Rules:** Review [Metro Network & Line Isolation](metro_network.md).
- **For Algorithms & Math:** Read [QR Ticketing & Reed-Solomon Encoding](ticketing_and_fares.md).
