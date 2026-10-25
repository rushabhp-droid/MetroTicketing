# 🎫 QR Ticketing & Dynamic Fare Engine

The ticketing system simulates modern digital paperless metro transit tokens. It integrates real-time distance-based fare calculation, round-trip concessions, automated rush-hour surcharges, and a pure-Java mathematical QR code generator.

---

## 💰 Dynamic Fare Slabs Matrix

Fare computation is managed by [`FareCalculator.java`](file:///home/rushabhp/Projects/CNN/src/main/java/com/mumbaimetro/service/FareCalculator.java) based on official metro distance brackets:

| Distance Slab | Standard Single Fare | Peak Rush (+₹5.00) | Notes |
| :--- | :--- | :--- | :--- |
| **$0 \text{ to } 3\text{ km}$** | ₹10.00 | ₹15.00 | Minimum commuter base fare |
| **$3 \text{ to } 6\text{ km}$** | ₹20.00 | ₹25.00 | Short hop trip |
| **$6 \text{ to } 9\text{ km}$** | ₹30.00 | ₹35.00 | Medium commuter journey |
| **$9 \text{ to } 12\text{ km}$** | ₹40.00 | ₹45.00 | Full corridor transit (Line 1 Versova–Ghatkopar) |
| **$12 \text{ to } 15\text{ km}$** | ₹50.00 | ₹55.00 | Extended transit |
| **$15 \text{ to } 18\text{ km}$** | ₹60.00 | ₹65.00 | Lines 2A & 7 end-to-end |
| **$18 \text{ to } 21\text{ km}$** | ₹70.00 | ₹75.00 | Long cross-suburb journey |
| **$21 \text{ to } 24\text{ km}$** | ₹80.00 | ₹85.00 | Extended corridor transit |
| **$> 24\text{ km}$** | ₹90.00 – ₹110.00 | ₹95.00 – ₹115.00 | Maximum Line 3 corridor stretch |

### Mathematical Formula for Compound Fare

The compound token fare is evaluated using compile-time overloaded method:
```java
public double calculateTokenFare(Station src, Station dest, int passengers, boolean isReturn, boolean isPeakHour)
```
1. **Base Fare Calculation:**
   $$\text{BaseUnit} = \text{calculateBaseFare}(\text{distanceKm})$$
2. **Peak-Hour Adjustment:**
   $$\text{AdjustedUnit} = \text{BaseUnit} + (\text{isPeakHour} ? 5.0 : 0.0)$$
3. **Trip Type & Return Discount:**
   $$\text{TripMultiplier} = \text{isReturn} ? (2 \times 0.90) : 1.0$$
4. **Total Fare:**
   $$\text{TotalFare} = \text{AdjustedUnit} \times \text{TripMultiplier} \times \text{passengers}$$

---

## 🧮 Pure-Java Reed-Solomon QR Code Generator

A key technical highlight is that QR code generation is achieved **without external third-party libraries** (e.g. ZXing). [`QRCodeGenerator.java`](file:///home/rushabhp/Projects/CNN/src/main/java/com/mumbaimetro/qr/QRCodeGenerator.java) implements standard ISO/IEC 18004 algorithmic steps from first mathematical principles.

### 1. Finite Galois Field $GF(2^8)$ Arithmetic
The generator uses Galois Field arithmetic modulo primitive polynomial:
$$P(x) = x^8 + x^4 + x^3 + x^2 + 1 \quad (285 \text{ decimal})$$
Precomputed lookup tables enable $O(1)$ multiplication:
```java
private static final int[] EXP_TABLE = new int[512];
private static final int[] LOG_TABLE = new int[256];
```
Multiplication of two numbers $a, b \in GF(2^8)$:
$$a \otimes b = \text{EXP\_TABLE}[\text{LOG\_TABLE}[a] + \text{LOG\_TABLE}[b]]$$

### 2. Error-Correction Codeword Polynomials
Generator polynomials $g(x)$ are created by synthetic convolution:
$$g(x) = \prod_{i=0}^{k-1} (x - \alpha^i)$$
The message data polynomial is multiplied by $x^k$ and divided by $g(x)$. The remainder coefficients constitute the Reed-Solomon error correction bytes, allowing the QR token to remain scannable even if partially damaged or smudged.

### 3. QR Matrix Assembly
A 2D binary grid (`boolean[][]`) is constructed with standard spatial modules:
- **Finder Patterns:** Three 7x7 concentric square boxes located at the top-left, top-right, and bottom-left corners.
- **Separators:** 1-module white borders surrounding finder patterns.
- **Timing Patterns:** Alternating dark/light rows and columns connecting finder patterns.
- **Data & Parity Codewords:** Interleaved data bits written into 2-module vertical zigzag columns.
- **Data Masking:** Pattern $(i + j) \pmod 2 == 0$ is evaluated across all active data modules.

### 4. Zero-Disk In-Memory Base64 Streaming
Rather than writing temporary PNG files to disk, the engine streams image bytes directly to RAM:
```java
BufferedImage image = generateQRImage(qrPayload, pixelScale);
ByteArrayOutputStream baos = new ByteArrayOutputStream();
ImageIO.write(image, "PNG", baos);
return Base64.getEncoder().encodeToString(baos.toByteArray());
```
The browser receives a self-contained data URI:
```html
<img src="data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAA..." alt="Metro Token" />
```

---

## 🧾 ASCII Ticket Receipt Generation

Both [`Card.java`](file:///home/rushabhp/Projects/CNN/src/main/java/com/mumbaimetro/model/Card.java) and [`Ticket.java`](file:///home/rushabhp/Projects/CNN/src/main/java/com/mumbaimetro/model/Ticket.java) implement the [`PrintableReceipt`](file:///home/rushabhp/Projects/CNN/src/main/java/com/mumbaimetro/interfaces/PrintableReceipt.java) contract:

```text
=====================================================
            MUMBAI METRO ONE (MMOPL)                 
          DIGITAL QR COMMUTER RECEIPT                
=====================================================
 Ticket ID     : TKT-894102
 Type          : QR Single Journey Token
 Source        : Versova (Line 1 (Blue))
 Destination   : Ghatkopar (Line 1 (Blue))
 Distance      : 11.4 km
 Passengers    : 2
 Total Paid    : ₹80.00
 Status        : VALID / ACTIVE
 Issue Time    : 25-Oct-2026 18:45
 QR Security   : MUMBAI-METRO|TKT-894102|Versova|Ghatkopar
=====================================================
 Scan this QR token at entry & exit turnstile gates.
```
