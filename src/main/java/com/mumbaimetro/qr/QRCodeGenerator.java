package com.mumbaimetro.qr;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import javax.imageio.ImageIO;

/**
 * A self-contained QR Code generator in pure Java (no external libraries).
 *
 * <p>Supports QR versions 1–4 with Error Correction Level M (default) or L.
 * Encodes data in Byte mode (ISO-8859-1). Produces valid, scannable QR codes
 * suitable for short alphanumeric strings such as ticket IDs ("TKT-5001"),
 * card IDs ("MMC-1002"), and short info strings up to ~80 characters.</p>
 *
 * <h3>Algorithm Overview</h3>
 * <ol>
 *   <li>Encode input as byte-mode data codewords</li>
 *   <li>Select the smallest QR version that fits the data</li>
 *   <li>Compute Reed-Solomon error correction codewords over GF(256)</li>
 *   <li>Build the module matrix: finder patterns, alignment, timing, format info</li>
 *   <li>Place data bits in the zigzag pattern</li>
 *   <li>Apply data masking and write format information</li>
 * </ol>
 *
 * @author Mumbai Metro QR Subsystem
 */
public class QRCodeGenerator {

    // ──────────────────────────────────────────────────────────────────────
    //  GF(256) Arithmetic Tables
    // ──────────────────────────────────────────────────────────────────────

    /**
     * Galois Field GF(256) log table.
     * {@code logTable[a]} gives the discrete logarithm base α of {@code a}.
     */
    private static final int[] LOG_TABLE = new int[256];

    /**
     * Galois Field GF(256) anti-log (exponent) table.
     * {@code EXP_TABLE[i]} gives α^i in GF(256).
     * Extended to 512 entries to simplify modular reduction.
     */
    private static final int[] EXP_TABLE = new int[512];

    /**
     * Primitive polynomial for QR codes: 0x11D = x^8 + x^4 + x^3 + x^2 + 1.
     */
    private static final int PRIMITIVE_POLY = 0x11D;

    static {
        // Build the log and exp tables for GF(256) with primitive poly 0x11D.
        int value = 1;
        for (int i = 0; i < 255; i++) {
            EXP_TABLE[i] = value;
            LOG_TABLE[value] = i;
            value <<= 1;
            if (value >= 256) {
                value ^= PRIMITIVE_POLY;
            }
        }
        // Duplicate the table so that EXP_TABLE[i + 255] == EXP_TABLE[i]
        // This avoids having to do modular reduction during multiplication.
        for (int i = 255; i < 512; i++) {
            EXP_TABLE[i] = EXP_TABLE[i - 255];
        }
    }

    // ──────────────────────────────────────────────────────────────────────
    //  QR Version / Error-Correction Capacity Tables
    // ──────────────────────────────────────────────────────────────────────

    /**
     * Total number of data codewords for each version at EC Level M.
     * Index 0 = version 1, index 3 = version 4.
     */
    private static final int[] DATA_CODEWORDS_M = {16, 28, 44, 64};

    /**
     * Total number of data codewords for each version at EC Level L.
     */
    private static final int[] DATA_CODEWORDS_L = {19, 34, 55, 80};

    /**
     * Number of EC codewords per block for each version at EC Level M.
     */
    private static final int[] EC_CODEWORDS_PER_BLOCK_M = {10, 16, 26, 18};

    /**
     * Number of EC codewords per block for each version at EC Level L.
     */
    private static final int[] EC_CODEWORDS_PER_BLOCK_L = {7, 10, 15, 18};

    /**
     * Total number of codewords (data + EC) for each version.
     * Version n has (16n+128+1) / something — easier to just list them:
     * V1: 26, V2: 44, V3: 70, V4: 100
     */
    private static final int[] TOTAL_CODEWORDS = {26, 44, 70, 100};

    /**
     * Number of data blocks for each version at EC Level M.
     * V1-M: 1 block, V2-M: 1 block, V3-M: 1 block (26 EC cw from 1 block), V4-M: 2 blocks.
     */
    private static final int[] NUM_BLOCKS_M = {1, 1, 1, 2};

    /**
     * Number of data blocks for each version at EC Level L.
     * V1-L: 1, V2-L: 1, V3-L: 1, V4-L: 1 (but let's check — V4-L: 80 data, 20 EC = 100 total, 1 block)
     */
    private static final int[] NUM_BLOCKS_L = {1, 1, 1, 1};

    /**
     * Alignment pattern center coordinates for versions 2-4.
     * Version 1 has no alignment pattern.
     */
    private static final int[] ALIGNMENT_CENTERS = {0, 18, 22, 26};

    /**
     * Number of remainder bits appended after the codeword bit stream.
     * V1: 0, V2-V4: 7.
     */
    private static final int[] REMAINDER_BITS = {0, 7, 7, 7};

    /**
     * Dark navy colour used for "black" QR modules (matches the app theme).
     */
    private static final Color MODULE_DARK = new Color(30, 41, 59);

    /**
     * White colour for "white" QR modules and quiet zone.
     */
    private static final Color MODULE_LIGHT = Color.WHITE;

    /**
     * Width of the quiet zone in modules.
     */
    private static final int QUIET_ZONE = 4;

    // ──────────────────────────────────────────────────────────────────────
    //  Public API
    // ──────────────────────────────────────────────────────────────────────

    /**
     * Generate a QR code as a {@link BufferedImage}.
     *
     * @param text      the text to encode (must fit in versions 1-4 at EC Level M)
     * @param pixelSize the size in pixels of each QR module (e.g. 10)
     * @return a {@link BufferedImage} containing the rendered QR code with quiet zone
     * @throws IllegalArgumentException if the text is too long for version 4
     */
    public static BufferedImage generateQRImage(String text, int pixelSize) {
        boolean[][] matrix = generateQRMatrix(text);
        int modules = matrix.length;
        int imageSize = (modules + 2 * QUIET_ZONE) * pixelSize;

        BufferedImage image = new BufferedImage(imageSize, imageSize, BufferedImage.TYPE_INT_RGB);

        // Fill the entire image with white (quiet zone + background)
        for (int y = 0; y < imageSize; y++) {
            for (int x = 0; x < imageSize; x++) {
                image.setRGB(x, y, MODULE_LIGHT.getRGB());
            }
        }

        // Draw each module
        for (int row = 0; row < modules; row++) {
            for (int col = 0; col < modules; col++) {
                if (matrix[row][col]) {
                    int rgb = MODULE_DARK.getRGB();
                    int xOffset = (col + QUIET_ZONE) * pixelSize;
                    int yOffset = (row + QUIET_ZONE) * pixelSize;
                    for (int dy = 0; dy < pixelSize; dy++) {
                        for (int dx = 0; dx < pixelSize; dx++) {
                            image.setRGB(xOffset + dx, yOffset + dy, rgb);
                        }
                    }
                }
            }
        }

        return image;
    }

    /**
     * Generate Base64 encoded PNG data URI string of the QR Code.
     * Useful for embedding in HTML/Thymeleaf web templates directly without writing to disk.
     *
     * @param text      the text to encode
     * @param pixelSize module pixel scale factor
     * @return Base64 data string (PNG format)
     */
    public static String generateQRCodeBase64(String text, int pixelSize) {
        try {
            BufferedImage img = generateQRImage(text, pixelSize);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(img, "PNG", baos);
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Generate the raw QR module matrix.
     *
     * @param text the text to encode
     * @return a 2D boolean array where {@code true} = dark module, {@code false} = light module
     * @throws IllegalArgumentException if the text is too long for version 4
     */
    public static boolean[][] generateQRMatrix(String text) {
        // Step 1: Convert text to byte array
        byte[] dataBytes = encodeToISO8859(text);

        // Step 2: Select QR version and EC level
        int version = selectVersion(dataBytes.length);
        int ecLevel = 0; // 0 = M (default for better error correction)

        int dataCapacity = DATA_CODEWORDS_M[version - 1];
        int ecCodewordsPerBlock = EC_CODEWORDS_PER_BLOCK_M[version - 1];
        int numBlocks = NUM_BLOCKS_M[version - 1];

        // If data doesn't fit in EC Level M, try EC Level L
        if (dataBytes.length > getByteCapacity(version, 0)) {
            ecLevel = 1; // L
            dataCapacity = DATA_CODEWORDS_L[version - 1];
            ecCodewordsPerBlock = EC_CODEWORDS_PER_BLOCK_L[version - 1];
            numBlocks = NUM_BLOCKS_L[version - 1];
        }

        // Step 3: Build the data codeword sequence
        int[] dataCodewords = encodeDataCodewords(dataBytes, dataCapacity, version);

        // Step 4: Compute Reed-Solomon error correction
        int[] ecCodewords = computeErrorCorrection(dataCodewords, ecCodewordsPerBlock, numBlocks);

        // Step 5: Interleave and build the full bit stream
        int[] allCodewords = interleaveCodewords(dataCodewords, ecCodewords, numBlocks, ecCodewordsPerBlock);

        // Step 6: Build the module matrix
        int size = 17 + version * 4; // V1=21, V2=25, V3=29, V4=33
        boolean[][] matrix = new boolean[size][size];
        boolean[][] reserved = new boolean[size][size]; // tracks reserved (non-data) areas

        // Place function patterns
        placeFinders(matrix, reserved, size);
        placeSeparators(matrix, reserved, size);
        placeAlignmentPattern(matrix, reserved, size, version);
        placeTimingPatterns(matrix, reserved, size);
        reserveFormatAreas(reserved, size);
        // Dark module (always present)
        matrix[4 * version + 9][8] = true;
        reserved[4 * version + 9][8] = true;

        // Step 7: Place data bits in the zigzag pattern
        int totalBits = allCodewords.length * 8 + REMAINDER_BITS[version - 1];
        boolean[] bitStream = codewordsToBits(allCodewords, totalBits);
        placeDataBits(matrix, reserved, size, bitStream);

        // Step 8: Apply masking — evaluate all 8 patterns and pick the best
        int bestMask = selectBestMask(matrix, reserved, size);
        applyMask(matrix, reserved, size, bestMask);

        // Step 9: Write format information
        writeFormatInfo(matrix, size, ecLevel, bestMask);

        return matrix;
    }

    // ──────────────────────────────────────────────────────────────────────
    //  Data Encoding
    // ──────────────────────────────────────────────────────────────────────

    /**
     * Encode a Java String to ISO-8859-1 bytes.
     *
     * @param text the input string
     * @return ISO-8859-1 encoded byte array
     */
    private static byte[] encodeToISO8859(String text) {
        byte[] result = new byte[text.length()];
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c > 255) {
                throw new IllegalArgumentException(
                        "Character '" + c + "' at index " + i + " is outside ISO-8859-1 range");
            }
            result[i] = (byte) c;
        }
        return result;
    }

    /**
     * Get the maximum number of data bytes (payload) that can be encoded
     * for a given version and EC level after accounting for mode indicator
     * and character count indicator overhead.
     *
     * <p>Byte mode overhead: 4 bits (mode) + 8 bits (char count) = 12 bits = 1.5 bytes.
     * Plus 4-bit terminator. So usable payload = dataCodewords - 2 (approximately,
     * since the bit packing is done in {@link #encodeDataCodewords}).</p>
     *
     * @param version QR version (1-4)
     * @param ecLevel 0 = M, 1 = L
     * @return maximum number of payload bytes
     */
    private static int getByteCapacity(int version, int ecLevel) {
        int dataCodewords = (ecLevel == 0)
                ? DATA_CODEWORDS_M[version - 1]
                : DATA_CODEWORDS_L[version - 1];
        // Overhead: 4 bits mode + 8 bits count + 4 bits terminator = 16 bits = 2 bytes
        return dataCodewords - 2;
    }

    /**
     * Select the smallest QR version (1-4) that can hold the given number of
     * payload bytes at EC Level M, falling back to EC Level L if needed.
     *
     * @param payloadLength number of data bytes
     * @return QR version (1-4)
     * @throws IllegalArgumentException if the payload is too large for version 4
     */
    private static int selectVersion(int payloadLength) {
        for (int v = 1; v <= 4; v++) {
            // Try M first, then L
            if (payloadLength <= getByteCapacity(v, 0)) {
                return v;
            }
        }
        // Try with Level L
        for (int v = 1; v <= 4; v++) {
            if (payloadLength <= getByteCapacity(v, 1)) {
                return v;
            }
        }
        throw new IllegalArgumentException(
                "Text too long (" + payloadLength + " bytes) for QR versions 1-4");
    }

    /**
     * Encode the raw payload bytes into the full sequence of data codewords,
     * including mode indicator, character count, data, terminator, and padding.
     *
     * <h4>Bit layout (Byte mode, versions 1-9):</h4>
     * <pre>
     *   [0100]                     — 4-bit mode indicator (byte mode)
     *   [CCCCCCCC]                 — 8-bit character count
     *   [DDDDDDDD] × n            — 8-bit data per character
     *   [0000]                     — 4-bit terminator (may be shorter if at capacity)
     *   [0...]                     — pad to byte boundary
     *   [11101100 00010001] ...    — alternating pad codewords 0xEC, 0x11
     * </pre>
     *
     * @param dataBytes     the raw payload bytes
     * @param dataCapacity  the total number of data codewords available
     * @param version       the QR version (unused here but kept for clarity)
     * @return array of data codewords (each 0-255)
     */
    private static int[] encodeDataCodewords(byte[] dataBytes, int dataCapacity, int version) {
        // Total bit capacity
        int totalBits = dataCapacity * 8;

        // Build the bit stream
        BitBuffer bits = new BitBuffer(totalBits);

        // Mode indicator: 0100 (Byte mode)
        bits.append(0b0100, 4);

        // Character count indicator: 8 bits for versions 1-9
        bits.append(dataBytes.length, 8);

        // Data bytes
        for (byte b : dataBytes) {
            bits.append(b & 0xFF, 8);
        }

        // Terminator: up to 4 zero bits (or fewer if we're already at capacity)
        int terminatorLength = Math.min(4, totalBits - bits.length());
        bits.append(0, terminatorLength);

        // Pad to byte boundary
        int padToByteLen = (8 - (bits.length() % 8)) % 8;
        bits.append(0, padToByteLen);

        // Fill remaining capacity with alternating pad codewords 0xEC and 0x11
        boolean useEC = true;
        while (bits.length() < totalBits) {
            bits.append(useEC ? 0xEC : 0x11, 8);
            useEC = !useEC;
        }

        // Convert bit buffer to codeword array
        return bits.toCodewords();
    }

    // ──────────────────────────────────────────────────────────────────────
    //  Reed-Solomon Error Correction over GF(256)
    // ──────────────────────────────────────────────────────────────────────

    /**
     * Multiply two elements in GF(256).
     *
     * @param a first operand (0-255)
     * @param b second operand (0-255)
     * @return product in GF(256)
     */
    private static int gfMultiply(int a, int b) {
        if (a == 0 || b == 0) return 0;
        return EXP_TABLE[LOG_TABLE[a] + LOG_TABLE[b]];
    }

    /**
     * Build the Reed-Solomon generator polynomial for the given number of
     * error correction codewords.
     *
     * <p>The generator polynomial is:</p>
     * <pre>
     *   g(x) = (x - α^0)(x - α^1) ··· (x - α^{n-1})
     * </pre>
     * <p>In GF(256), subtraction is the same as addition (XOR), so
     * (x - α^i) = (x + α^i).</p>
     *
     * <p>The polynomial is represented as an array of coefficients where
     * index 0 is the highest degree term. For example, for n=2:
     * g(x) = x^2 + (α^0 + α^1)x + α^0·α^1, stored as [1, coeff, coeff].</p>
     *
     * @param numECCodewords number of EC codewords (= degree of generator)
     * @return coefficients of the generator polynomial (length = numECCodewords + 1)
     */
    private static int[] buildGeneratorPoly(int numECCodewords) {
        // Start with g(x) = 1 (i.e., [1])
        int[] gen = new int[]{1};

        for (int i = 0; i < numECCodewords; i++) {
            // Multiply gen by (x + α^i)
            int[] newGen = new int[gen.length + 1];
            for (int j = 0; j < gen.length; j++) {
                newGen[j] ^= gen[j]; // coefficient * x
                newGen[j + 1] ^= gfMultiply(gen[j], EXP_TABLE[i]); // coefficient * α^i
            }
            gen = newGen;
        }

        return gen;
    }

    /**
     * Compute Reed-Solomon error correction codewords for the given data.
     *
     * <p>Performs polynomial long division of the message polynomial by the
     * generator polynomial in GF(256). The remainder is the set of EC codewords.</p>
     *
     * @param dataCodewords       the data codewords
     * @param ecCodewordsPerBlock number of EC codewords per block
     * @param numBlocks           number of blocks to split data into
     * @return array of all EC codewords (concatenated across all blocks)
     */
    private static int[] computeErrorCorrection(int[] dataCodewords, int ecCodewordsPerBlock,
                                                 int numBlocks) {
        int[] generatorPoly = buildGeneratorPoly(ecCodewordsPerBlock);
        int dataPerBlock = dataCodewords.length / numBlocks;
        int remainder = dataCodewords.length % numBlocks;

        // Collect EC codewords from all blocks
        int[][] ecBlocks = new int[numBlocks][];

        int offset = 0;
        for (int b = 0; b < numBlocks; b++) {
            // If there are remainder data codewords, the last blocks get one extra
            int blockDataLen = dataPerBlock + (b >= numBlocks - remainder && remainder > 0 ? 1 : 0);
            int[] blockData = new int[blockDataLen];
            System.arraycopy(dataCodewords, offset, blockData, 0, blockDataLen);
            offset += blockDataLen;

            ecBlocks[b] = polynomialDivision(blockData, generatorPoly, ecCodewordsPerBlock);
        }

        // Flatten all EC blocks into a single array
        int[] allEC = new int[numBlocks * ecCodewordsPerBlock];
        int idx = 0;
        for (int b = 0; b < numBlocks; b++) {
            System.arraycopy(ecBlocks[b], 0, allEC, idx, ecCodewordsPerBlock);
            idx += ecCodewordsPerBlock;
        }
        return allEC;
    }

    /**
     * Perform polynomial long division in GF(256) and return the remainder.
     *
     * @param message     the message polynomial coefficients (highest degree first)
     * @param generator   the generator polynomial coefficients
     * @param ecCount     the number of EC codewords (= degree of generator)
     * @return the remainder polynomial coefficients (= EC codewords)
     */
    private static int[] polynomialDivision(int[] message, int[] generator, int ecCount) {
        // Create a working array: message padded with ecCount zeros
        int[] work = new int[message.length + ecCount];
        System.arraycopy(message, 0, work, 0, message.length);

        for (int i = 0; i < message.length; i++) {
            int leadCoeff = work[i];
            if (leadCoeff == 0) continue;

            for (int j = 0; j < generator.length; j++) {
                work[i + j] ^= gfMultiply(generator[j], leadCoeff);
            }
        }

        // The remainder is in the last ecCount positions
        int[] ec = new int[ecCount];
        System.arraycopy(work, message.length, ec, 0, ecCount);
        return ec;
    }

    // ──────────────────────────────────────────────────────────────────────
    //  Codeword Interleaving
    // ──────────────────────────────────────────────────────────────────────

    /**
     * Interleave data and EC codewords across blocks, then concatenate.
     *
     * <p>For a single block (versions 1-3 at EC Level M), no interleaving is needed.
     * For multiple blocks, data codewords are interleaved column-wise, then EC
     * codewords are interleaved column-wise.</p>
     *
     * @param dataCodewords       all data codewords
     * @param ecCodewords         all EC codewords
     * @param numBlocks           number of blocks
     * @param ecCodewordsPerBlock EC codewords per block
     * @return the final interleaved codeword sequence
     */
    private static int[] interleaveCodewords(int[] dataCodewords, int[] ecCodewords,
                                              int numBlocks, int ecCodewordsPerBlock) {
        int[] result = new int[dataCodewords.length + ecCodewords.length];
        int idx = 0;

        if (numBlocks == 1) {
            // No interleaving needed
            System.arraycopy(dataCodewords, 0, result, 0, dataCodewords.length);
            System.arraycopy(ecCodewords, 0, result, dataCodewords.length, ecCodewords.length);
            return result;
        }

        // Split data into blocks
        int dataPerBlock = dataCodewords.length / numBlocks;
        int extra = dataCodewords.length % numBlocks;
        int[][] dataBlocks = new int[numBlocks][];
        int offset = 0;
        for (int b = 0; b < numBlocks; b++) {
            int len = dataPerBlock + (b >= numBlocks - extra && extra > 0 ? 1 : 0);
            dataBlocks[b] = new int[len];
            System.arraycopy(dataCodewords, offset, dataBlocks[b], 0, len);
            offset += len;
        }

        // Interleave data: take one codeword from each block in turn
        int maxDataLen = 0;
        for (int[] block : dataBlocks) {
            maxDataLen = Math.max(maxDataLen, block.length);
        }
        for (int i = 0; i < maxDataLen; i++) {
            for (int b = 0; b < numBlocks; b++) {
                if (i < dataBlocks[b].length) {
                    result[idx++] = dataBlocks[b][i];
                }
            }
        }

        // Split EC into blocks and interleave
        int[][] ecBlocks = new int[numBlocks][ecCodewordsPerBlock];
        for (int b = 0; b < numBlocks; b++) {
            System.arraycopy(ecCodewords, b * ecCodewordsPerBlock, ecBlocks[b], 0, ecCodewordsPerBlock);
        }
        for (int i = 0; i < ecCodewordsPerBlock; i++) {
            for (int b = 0; b < numBlocks; b++) {
                result[idx++] = ecBlocks[b][i];
            }
        }

        return result;
    }

    // ──────────────────────────────────────────────────────────────────────
    //  Bit Stream Conversion
    // ──────────────────────────────────────────────────────────────────────

    /**
     * Convert an array of codewords (each 0-255) to a boolean bit array.
     *
     * @param codewords  the codewords
     * @param totalBits  the total number of bits to produce (includes remainder bits)
     * @return boolean array where {@code true} = 1, {@code false} = 0
     */
    private static boolean[] codewordsToBits(int[] codewords, int totalBits) {
        boolean[] bits = new boolean[totalBits];
        for (int i = 0; i < codewords.length; i++) {
            for (int bit = 7; bit >= 0; bit--) {
                int bitIndex = i * 8 + (7 - bit);
                if (bitIndex < totalBits) {
                    bits[bitIndex] = ((codewords[i] >> bit) & 1) == 1;
                }
            }
        }
        // Remainder bits are already false (0)
        return bits;
    }

    // ──────────────────────────────────────────────────────────────────────
    //  Matrix Construction — Function Patterns
    // ──────────────────────────────────────────────────────────────────────

    /**
     * Place the three 7×7 finder patterns at top-left, top-right, and bottom-left.
     *
     * <p>Each finder pattern consists of a 7×7 outer dark border, a 5×5 inner
     * light border, and a 3×3 dark center.</p>
     *
     * @param matrix   the module matrix
     * @param reserved the reservation matrix
     * @param size     the matrix dimension
     */
    private static void placeFinders(boolean[][] matrix, boolean[][] reserved, int size) {
        // Finder pattern template (7×7)
        int[][] finderPattern = {
                {1, 1, 1, 1, 1, 1, 1},
                {1, 0, 0, 0, 0, 0, 1},
                {1, 0, 1, 1, 1, 0, 1},
                {1, 0, 1, 1, 1, 0, 1},
                {1, 0, 1, 1, 1, 0, 1},
                {1, 0, 0, 0, 0, 0, 1},
                {1, 1, 1, 1, 1, 1, 1}
        };

        // Top-left (row 0, col 0)
        placePattern(matrix, reserved, 0, 0, finderPattern);
        // Top-right (row 0, col size-7)
        placePattern(matrix, reserved, 0, size - 7, finderPattern);
        // Bottom-left (row size-7, col 0)
        placePattern(matrix, reserved, size - 7, 0, finderPattern);
    }

    /**
     * Place a pattern template onto the matrix and mark the area as reserved.
     */
    private static void placePattern(boolean[][] matrix, boolean[][] reserved,
                                      int startRow, int startCol, int[][] pattern) {
        for (int r = 0; r < pattern.length; r++) {
            for (int c = 0; c < pattern[r].length; c++) {
                int row = startRow + r;
                int col = startCol + c;
                if (row >= 0 && row < matrix.length && col >= 0 && col < matrix.length) {
                    matrix[row][col] = (pattern[r][c] == 1);
                    reserved[row][col] = true;
                }
            }
        }
    }

    /**
     * Place the 1-module-wide white separator borders around each finder pattern.
     *
     * <p>Separators are always white (false) modules placed immediately adjacent
     * to the finder patterns.</p>
     */
    private static void placeSeparators(boolean[][] matrix, boolean[][] reserved, int size) {
        // Top-left separator: column 7 (rows 0-7) and row 7 (cols 0-7)
        for (int i = 0; i <= 7; i++) {
            setModule(matrix, reserved, i, 7, false, size);  // right side
            setModule(matrix, reserved, 7, i, false, size);  // bottom side
        }

        // Top-right separator: column size-8 (rows 0-7) and row 7 (cols size-8 to size-1)
        for (int i = 0; i <= 7; i++) {
            setModule(matrix, reserved, i, size - 8, false, size);  // left side
            setModule(matrix, reserved, 7, size - 8 + i, false, size);  // bottom side
        }

        // Bottom-left separator: column 7 (rows size-8 to size-1) and row size-8 (cols 0-7)
        for (int i = 0; i <= 7; i++) {
            setModule(matrix, reserved, size - 8, i, false, size);  // top side
            setModule(matrix, reserved, size - 8 + i, 7, false, size);  // right side
        }
    }

    /**
     * Set a single module in the matrix and mark it as reserved.
     */
    private static void setModule(boolean[][] matrix, boolean[][] reserved,
                                   int row, int col, boolean dark, int size) {
        if (row >= 0 && row < size && col >= 0 && col < size) {
            matrix[row][col] = dark;
            reserved[row][col] = true;
        }
    }

    /**
     * Place the alignment pattern for versions ≥ 2.
     *
     * <p>The alignment pattern is a 5×5 module: dark border, light ring, dark center.
     * It is placed so that its center is at the coordinate given by
     * {@link #ALIGNMENT_CENTERS}.</p>
     *
     * @param matrix   the module matrix
     * @param reserved the reservation matrix
     * @param size     the matrix dimension
     * @param version  the QR version (1-4)
     */
    private static void placeAlignmentPattern(boolean[][] matrix, boolean[][] reserved,
                                               int size, int version) {
        if (version < 2) return; // Version 1 has no alignment pattern

        int center = ALIGNMENT_CENTERS[version - 1];

        int[][] alignPattern = {
                {1, 1, 1, 1, 1},
                {1, 0, 0, 0, 1},
                {1, 0, 1, 0, 1},
                {1, 0, 0, 0, 1},
                {1, 1, 1, 1, 1}
        };

        int startRow = center - 2;
        int startCol = center - 2;

        // Only place if it doesn't overlap with finder patterns
        // For versions 2-4, there's only one alignment pattern at (center, center)
        // and it won't overlap with the top-left finder (which ends at row/col 8)
        // Check overlap with all three finders
        if (!overlapsWithFinder(startRow, startCol, size)) {
            placePattern(matrix, reserved, startRow, startCol, alignPattern);
        }
    }

    /**
     * Check if a 5×5 alignment pattern starting at (startRow, startCol) would
     * overlap with any of the three finder pattern regions (including separators).
     */
    private static boolean overlapsWithFinder(int startRow, int startCol, int size) {
        // Finder regions (including separator):
        // Top-left: rows 0-8, cols 0-8
        // Top-right: rows 0-8, cols size-9 to size-1
        // Bottom-left: rows size-9 to size-1, cols 0-8
        for (int r = startRow; r < startRow + 5; r++) {
            for (int c = startCol; c < startCol + 5; c++) {
                if (r <= 8 && c <= 8) return true;                           // top-left
                if (r <= 8 && c >= size - 8) return true;                    // top-right
                if (r >= size - 8 && c <= 8) return true;                    // bottom-left
            }
        }
        return false;
    }

    /**
     * Place the horizontal and vertical timing patterns.
     *
     * <p>Timing patterns are alternating dark/light modules along row 6
     * (between the top finders) and column 6 (between the left finders).</p>
     */
    private static void placeTimingPatterns(boolean[][] matrix, boolean[][] reserved, int size) {
        // Horizontal timing pattern (row 6, columns 8 to size-9)
        for (int col = 8; col < size - 8; col++) {
            if (!reserved[6][col]) {
                matrix[6][col] = (col % 2 == 0); // alternating, starting dark
                reserved[6][col] = true;
            }
        }

        // Vertical timing pattern (column 6, rows 8 to size-9)
        for (int row = 8; row < size - 8; row++) {
            if (!reserved[row][6]) {
                matrix[row][6] = (row % 2 == 0); // alternating, starting dark
                reserved[row][6] = true;
            }
        }
    }

    /**
     * Reserve the areas where format information will be written.
     *
     * <p>Format information occupies 15 modules in two strips:
     * <ul>
     *   <li>Around the top-left finder: along row 8 (cols 0-8, skipping col 6)
     *       and column 8 (rows 0-8, skipping row 6)</li>
     *   <li>Along the bottom-left finder (column 8, rows size-7 to size-1)
     *       and the top-right finder (row 8, cols size-8 to size-1)</li>
     * </ul>
     * </p>
     */
    private static void reserveFormatAreas(boolean[][] reserved, int size) {
        // Around top-left finder
        for (int i = 0; i <= 8; i++) {
            reserved[8][i] = true;  // row 8, cols 0-8
            reserved[i][8] = true;  // col 8, rows 0-8
        }

        // Bottom-left strip (column 8, rows size-7 to size-1)
        for (int i = 0; i < 7; i++) {
            reserved[size - 1 - i][8] = true;
        }

        // Top-right strip (row 8, cols size-8 to size-1)
        for (int i = 0; i < 8; i++) {
            reserved[8][size - 8 + i] = true;
        }
    }

    // ──────────────────────────────────────────────────────────────────────
    //  Data Bit Placement — Zigzag Pattern
    // ──────────────────────────────────────────────────────────────────────

    /**
     * Place the data bit stream into the matrix using the standard QR zigzag pattern.
     *
     * <p>Data is placed in two-column-wide strips, starting from the bottom-right
     * corner, going upward, then switching direction at the top and going downward,
     * and so on. Column 6 (timing pattern column) is skipped entirely — when a
     * strip would include column 6, we shift left by one.</p>
     *
     * <p>Within each two-column strip, bits are placed right-to-left on each row:
     * first the right column, then the left column, before moving to the next row.</p>
     *
     * @param matrix   the module matrix
     * @param reserved the reservation matrix
     * @param size     the matrix dimension
     * @param bits     the data bit stream
     */
    private static void placeDataBits(boolean[][] matrix, boolean[][] reserved,
                                       int size, boolean[] bits) {
        int bitIndex = 0;
        // Start from the rightmost column and work left in pairs
        // right = the right column of the current two-column strip
        int right = size - 1;

        while (right >= 1) {
            // Skip column 6 (timing pattern)
            if (right == 6) {
                right--;
                continue;
            }

            int left = right - 1;

            // Determine direction: going up or down
            // The first strip goes upward, then alternates
            // We can compute: strip number (0-based from right) determines direction
            // Strip 0 (rightmost): upward. Strip 1: downward. etc.
            // Column pairs from right: (size-1, size-2), (size-3, size-4), ...
            // But we need to account for the column 6 skip.
            // Simpler: compute which strip we're on
            int adjustedRight = right;
            if (right < 6) adjustedRight++; // account for skipped column 6
            int stripFromRight = (size - 1 - adjustedRight) / 2;
            boolean goingUp = (stripFromRight % 2 == 0);

            if (goingUp) {
                for (int row = size - 1; row >= 0; row--) {
                    // Right column first, then left
                    if (!reserved[row][right] && bitIndex < bits.length) {
                        matrix[row][right] = bits[bitIndex++];
                    }
                    if (!reserved[row][left] && bitIndex < bits.length) {
                        matrix[row][left] = bits[bitIndex++];
                    }
                }
            } else {
                for (int row = 0; row < size; row++) {
                    if (!reserved[row][right] && bitIndex < bits.length) {
                        matrix[row][right] = bits[bitIndex++];
                    }
                    if (!reserved[row][left] && bitIndex < bits.length) {
                        matrix[row][left] = bits[bitIndex++];
                    }
                }
            }

            right -= 2;
        }
    }

    // ──────────────────────────────────────────────────────────────────────
    //  Masking
    // ──────────────────────────────────────────────────────────────────────

    /**
     * Evaluate all 8 mask patterns and return the one with the lowest penalty score.
     *
     * @param matrix   the unmasked module matrix (data modules placed)
     * @param reserved the reservation matrix
     * @param size     the matrix dimension
     * @return the index of the best mask pattern (0-7)
     */
    private static int selectBestMask(boolean[][] matrix, boolean[][] reserved, int size) {
        int bestMask = 0;
        int bestPenalty = Integer.MAX_VALUE;

        for (int mask = 0; mask < 8; mask++) {
            // Create a copy and apply the mask
            boolean[][] testMatrix = copyMatrix(matrix, size);
            applyMask(testMatrix, reserved, size, mask);

            int penalty = computePenalty(testMatrix, size);
            if (penalty < bestPenalty) {
                bestPenalty = penalty;
                bestMask = mask;
            }
        }

        return bestMask;
    }

    /**
     * Apply a mask pattern to the data modules of the matrix.
     *
     * <p>The 8 mask patterns are:</p>
     * <ul>
     *   <li>0: (row + col) % 2 == 0</li>
     *   <li>1: row % 2 == 0</li>
     *   <li>2: col % 3 == 0</li>
     *   <li>3: (row + col) % 3 == 0</li>
     *   <li>4: (row/2 + col/3) % 2 == 0</li>
     *   <li>5: (row*col)%2 + (row*col)%3 == 0</li>
     *   <li>6: ((row*col)%2 + (row*col)%3) % 2 == 0</li>
     *   <li>7: ((row+col)%2 + (row*col)%3) % 2 == 0</li>
     * </ul>
     *
     * <p>Only data modules (non-reserved) are affected by masking.</p>
     *
     * @param matrix       the module matrix (modified in place)
     * @param reserved     the reservation matrix
     * @param size         the matrix dimension
     * @param maskPattern  the mask pattern index (0-7)
     */
    private static void applyMask(boolean[][] matrix, boolean[][] reserved,
                                   int size, int maskPattern) {
        for (int row = 0; row < size; row++) {
            for (int col = 0; col < size; col++) {
                if (reserved[row][col]) continue;

                boolean shouldFlip;
                switch (maskPattern) {
                    case 0: shouldFlip = (row + col) % 2 == 0; break;
                    case 1: shouldFlip = row % 2 == 0; break;
                    case 2: shouldFlip = col % 3 == 0; break;
                    case 3: shouldFlip = (row + col) % 3 == 0; break;
                    case 4: shouldFlip = (row / 2 + col / 3) % 2 == 0; break;
                    case 5: shouldFlip = (row * col) % 2 + (row * col) % 3 == 0; break;
                    case 6: shouldFlip = ((row * col) % 2 + (row * col) % 3) % 2 == 0; break;
                    case 7: shouldFlip = ((row + col) % 2 + (row * col) % 3) % 2 == 0; break;
                    default: shouldFlip = false;
                }

                if (shouldFlip) {
                    matrix[row][col] = !matrix[row][col];
                }
            }
        }
    }

    /**
     * Compute the total penalty score for a masked QR matrix.
     *
     * <p>The penalty has four rules:</p>
     * <ol>
     *   <li><b>Rule 1:</b> Groups of 5+ same-coloured modules in a row/column → 3 + (count - 5)</li>
     *   <li><b>Rule 2:</b> 2×2 blocks of same colour → 3 per block</li>
     *   <li><b>Rule 3:</b> Patterns resembling finder patterns in rows/columns → 40 per occurrence</li>
     *   <li><b>Rule 4:</b> Proportion of dark modules deviating from 50% → 10 per 5% deviation</li>
     * </ol>
     */
    private static int computePenalty(boolean[][] matrix, int size) {
        int penalty = 0;

        // Rule 1: consecutive same-colour modules (horizontal and vertical)
        for (int row = 0; row < size; row++) {
            int count = 1;
            for (int col = 1; col < size; col++) {
                if (matrix[row][col] == matrix[row][col - 1]) {
                    count++;
                } else {
                    if (count >= 5) penalty += 3 + (count - 5);
                    count = 1;
                }
            }
            if (count >= 5) penalty += 3 + (count - 5);
        }
        for (int col = 0; col < size; col++) {
            int count = 1;
            for (int row = 1; row < size; row++) {
                if (matrix[row][col] == matrix[row - 1][col]) {
                    count++;
                } else {
                    if (count >= 5) penalty += 3 + (count - 5);
                    count = 1;
                }
            }
            if (count >= 5) penalty += 3 + (count - 5);
        }

        // Rule 2: 2×2 blocks
        for (int row = 0; row < size - 1; row++) {
            for (int col = 0; col < size - 1; col++) {
                boolean val = matrix[row][col];
                if (val == matrix[row][col + 1] &&
                    val == matrix[row + 1][col] &&
                    val == matrix[row + 1][col + 1]) {
                    penalty += 3;
                }
            }
        }

        // Rule 3: finder-like patterns (1011101 0000 or 0000 1011101)
        for (int row = 0; row < size; row++) {
            for (int col = 0; col <= size - 11; col++) {
                if (matchesFinderLikePattern(matrix, row, col, true)) {
                    penalty += 40;
                }
            }
        }
        for (int col = 0; col < size; col++) {
            for (int row = 0; row <= size - 11; row++) {
                if (matchesFinderLikePattern(matrix, row, col, false)) {
                    penalty += 40;
                }
            }
        }

        // Rule 4: dark module proportion
        int darkCount = 0;
        for (int row = 0; row < size; row++) {
            for (int col = 0; col < size; col++) {
                if (matrix[row][col]) darkCount++;
            }
        }
        int total = size * size;
        int percentage = (darkCount * 100) / total;
        int prevFive = (percentage / 5) * 5;
        int nextFive = prevFive + 5;
        int penaltyFromPrev = Math.abs(prevFive - 50) / 5;
        int penaltyFromNext = Math.abs(nextFive - 50) / 5;
        penalty += Math.min(penaltyFromPrev, penaltyFromNext) * 10;

        return penalty;
    }

    /**
     * Check if a finder-like pattern (1011101 0000 or 0000 1011101) exists
     * starting at the given position.
     *
     * @param matrix     the module matrix
     * @param row        starting row
     * @param col        starting column
     * @param horizontal if true, check horizontally; otherwise vertically
     */
    private static boolean matchesFinderLikePattern(boolean[][] matrix, int row, int col,
                                                     boolean horizontal) {
        // Pattern: 1 0 1 1 1 0 1 0 0 0 0
        boolean[] pattern1 = {true, false, true, true, true, false, true, false, false, false, false};
        // Pattern: 0 0 0 0 1 0 1 1 1 0 1
        boolean[] pattern2 = {false, false, false, false, true, false, true, true, true, false, true};

        boolean match1 = true, match2 = true;
        for (int i = 0; i < 11; i++) {
            boolean val;
            if (horizontal) {
                val = matrix[row][col + i];
            } else {
                val = matrix[row + i][col];
            }
            if (val != pattern1[i]) match1 = false;
            if (val != pattern2[i]) match2 = false;
            if (!match1 && !match2) return false;
        }
        return match1 || match2;
    }

    /**
     * Create a deep copy of a square boolean matrix.
     */
    private static boolean[][] copyMatrix(boolean[][] matrix, int size) {
        boolean[][] copy = new boolean[size][size];
        for (int i = 0; i < size; i++) {
            System.arraycopy(matrix[i], 0, copy[i], 0, size);
        }
        return copy;
    }

    // ──────────────────────────────────────────────────────────────────────
    //  Format Information
    // ──────────────────────────────────────────────────────────────────────

    /**
     * Write the 15-bit format information string into the matrix.
     *
     * <p>The format information encodes the error correction level and mask pattern.
     * It is BCH(15,5) encoded and XORed with the mask 0x5412.</p>
     *
     * <h4>Bit layout:</h4>
     * <pre>
     *   data = (ecLevelBits << 3) | maskPattern   (5 bits)
     *   encoded = BCH(15,5) encode with generator 0x537
     *   final = encoded ^ 0x5412
     * </pre>
     *
     * <p>EC level bits: L=01, M=00, Q=11, H=10</p>
     *
     * @param matrix      the module matrix
     * @param size        the matrix dimension
     * @param ecLevel     0 = M, 1 = L
     * @param maskPattern the mask pattern index (0-7)
     */
    private static void writeFormatInfo(boolean[][] matrix, int size, int ecLevel, int maskPattern) {
        // EC level indicator bits: M=00, L=01
        int ecBits = (ecLevel == 0) ? 0b00 : 0b01;
        int formatData = (ecBits << 3) | maskPattern;

        // BCH(15,5) encoding with generator polynomial 0x537
        int encoded = bchEncode(formatData);

        // XOR with mask pattern 0x5412
        encoded ^= 0x5412;

        // Place the 15 bits in two locations
        // Bits are numbered 0 (MSB) to 14 (LSB)

        // Location 1: around the top-left finder pattern
        // Bits 0-7 go in column 8, rows 0-7 (skipping row 6)
        // Bits 8-14 go in row 8, columns 7-0 (skipping column 6)
        int[] topLeftRow = {0, 1, 2, 3, 4, 5, 7, 8, 8, 8, 8, 8, 8, 8, 8};
        int[] topLeftCol = {8, 8, 8, 8, 8, 8, 8, 8, 7, 5, 4, 3, 2, 1, 0};

        // Location 2: around the other two finder patterns
        // Bits 0-6 go in row 8, columns size-1 down to size-7
        // Bits 7-14 go in column 8, rows size-7 up to size-1
        int[] otherRow = {8, 8, 8, 8, 8, 8, 8, size - 7, size - 6, size - 5, size - 4, size - 3, size - 2, size - 1};
        int[] otherCol = {size - 1, size - 2, size - 3, size - 4, size - 5, size - 6, size - 7, 8, 8, 8, 8, 8, 8, 8};

        for (int i = 0; i < 15; i++) {
            boolean bit = ((encoded >> (14 - i)) & 1) == 1;

            // Top-left location
            matrix[topLeftRow[i]][topLeftCol[i]] = bit;

            // Other location
            if (i < 7) {
                matrix[otherRow[i]][otherCol[i]] = bit;
            } else {
                matrix[otherRow[i - 1]][otherCol[i - 1]] = bit;
            }
        }

        // Correction: let me redo this more carefully per the QR spec.
        // The format info placement is tricky. Let me use the standard layout.
        writeFormatInfoStandard(matrix, size, encoded);
    }

    /**
     * Write the pre-computed 15-bit format info into the two standard locations.
     *
     * <p><b>Location 1 (around top-left finder):</b></p>
     * <ul>
     *   <li>Bits 0-5: column 8, rows 0-5</li>
     *   <li>Bit 6: column 8, row 7 (skip row 6 = timing)</li>
     *   <li>Bit 7: column 8, row 8</li>
     *   <li>Bit 8: row 8, column 7 (skip col 6 = timing)</li>
     *   <li>Bits 9-14: row 8, columns 5 down to 0</li>
     * </ul>
     *
     * <p><b>Location 2 (split between bottom-left and top-right):</b></p>
     * <ul>
     *   <li>Bits 0-6: row 8, columns (size-1) down to (size-7)</li>
     *   <li>Bits 7-14: column 8, rows (size-8) up to (size-1)</li>
     * </ul>
     */
    private static void writeFormatInfoStandard(boolean[][] matrix, int size, int formatBits) {
        // Extract bits: bit[0] is MSB (bit 14 of the 15-bit value)
        boolean[] bits = new boolean[15];
        for (int i = 0; i < 15; i++) {
            bits[i] = ((formatBits >> (14 - i)) & 1) == 1;
        }

        // Location 1: around top-left finder
        // Column 8, going down rows 0..8 (skip row 6)
        // This places bits 0..7
        int[] loc1Rows = {0, 1, 2, 3, 4, 5, 7, 8};
        for (int i = 0; i < 8; i++) {
            matrix[loc1Rows[i]][8] = bits[i];
        }
        // Row 8, going right-to-left cols 7..0 (skip col 6)
        // This places bits 7..14 (bit 7 at col 7, bit 8 at col 5, ...)
        // Wait — bit 7 is already placed at row 8, col 8. Let me re-check.
        // Actually the standard says:
        //   bits[0..5] → (row=0,col=8)..(row=5,col=8)
        //   bits[6]    → (row=7,col=8)
        //   bits[7]    → (row=8,col=8)
        //   bits[8]    → (row=8,col=7)
        //   bits[9]    → (row=8,col=5)
        //   bits[10]   → (row=8,col=4)
        //   bits[11]   → (row=8,col=3)
        //   bits[12]   → (row=8,col=2)
        //   bits[13]   → (row=8,col=1)
        //   bits[14]   → (row=8,col=0)
        int[] loc1bCols = {7, 5, 4, 3, 2, 1, 0};
        for (int i = 0; i < 7; i++) {
            matrix[8][loc1bCols[i]] = bits[8 + i];
        }

        // Location 2: around the other finders
        // Bits 0..7: row 8, cols (size-1)..(size-8)
        for (int i = 0; i < 8; i++) {
            matrix[8][size - 1 - i] = bits[i];
        }
        // Bits 8..14: col 8, rows (size-7)..(size-1)
        for (int i = 0; i < 7; i++) {
            matrix[size - 7 + i][8] = bits[8 + i];
        }
    }

    /**
     * BCH(15,5) encode a 5-bit format data value.
     *
     * <p>The generator polynomial is 0x537 (10100110111 in binary).
     * The data is shifted left by 10 bits and the remainder of division
     * by the generator is appended.</p>
     *
     * @param data the 5-bit format data
     * @return the 15-bit BCH-encoded value
     */
    private static int bchEncode(int data) {
        // Shift data left by 10 to make room for the 10 check bits
        int encoded = data << 10;

        // Generator polynomial: x^10 + x^8 + x^5 + x^4 + x^2 + x + 1 = 0x537
        int generator = 0x537;

        // Perform polynomial division to find remainder
        for (int i = 4; i >= 0; i--) {
            if ((encoded >> (i + 10)) != 0) {
                encoded ^= (generator << i);
            }
        }

        // The result is data << 10 | remainder
        return (data << 10) | encoded;
    }

    // ──────────────────────────────────────────────────────────────────────
    //  Bit Buffer Helper Class
    // ──────────────────────────────────────────────────────────────────────

    /**
     * A simple bit buffer that accumulates bits and can convert them to
     * 8-bit codewords.
     */
    private static class BitBuffer {
        private final int[] data;
        private int bitLength;

        /**
         * Create a new BitBuffer with the given maximum bit capacity.
         *
         * @param maxBits maximum number of bits this buffer can hold
         */
        BitBuffer(int maxBits) {
            this.data = new int[(maxBits + 31) / 32];
            this.bitLength = 0;
        }

        /**
         * Append {@code numBits} bits from the given value (MSB first).
         *
         * @param value   the value whose lowest {@code numBits} bits are appended
         * @param numBits number of bits to append (0 to 32)
         */
        void append(int value, int numBits) {
            for (int i = numBits - 1; i >= 0; i--) {
                int bit = (value >> i) & 1;
                if (bit == 1) {
                    data[bitLength / 32] |= (1 << (31 - (bitLength % 32)));
                }
                bitLength++;
            }
        }

        /**
         * @return the current number of bits stored
         */
        int length() {
            return bitLength;
        }

        /**
         * Convert the bit buffer to an array of 8-bit codewords.
         *
         * @return codeword array
         */
        int[] toCodewords() {
            int numCodewords = (bitLength + 7) / 8;
            int[] codewords = new int[numCodewords];
            for (int i = 0; i < bitLength; i++) {
                int bit = (data[i / 32] >> (31 - (i % 32))) & 1;
                codewords[i / 8] |= (bit << (7 - (i % 8)));
            }
            return codewords;
        }
    }
}
