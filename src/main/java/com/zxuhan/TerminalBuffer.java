package com.zxuhan;

import java.util.ArrayList;

public class TerminalBuffer {

    // Buffer configuration
    int width;
    int height;
    Line[] screen;
    ArrayList<Line> scrollback;
    int maxScrollback;

    // Cursor position
    int cursorCol;
    int cursorRow;

    // Current cell attributes
    Color currentFg;
    Color currentBg;
    boolean currentBold;
    boolean currentItalic;
    boolean currentUnderline;

    public TerminalBuffer(int width, int height, int maxScrollback) {
        this.width = width;
        this.height = height;
        this.maxScrollback = maxScrollback;

        screen = new Line[height];
        for (int i = 0; i < height; i++) {
            screen[i] = new Line(width);
        }

        scrollback = new ArrayList<>();

        cursorCol = 0;
        cursorRow = 0;

        currentFg = Color.DEFAULT;
        currentBg = Color.DEFAULT;
        currentBold = false;
        currentItalic = false;
        currentUnderline = false;
    }

    // --- Cursor ---

    public int getCursorCol() {
        return cursorCol;
    }

    public int getCursorRow() {
        return cursorRow;
    }

    /** Clamps both axes to valid ranges: col to [0, width-1], row to [0, height-1]. */
    public void setCursor(int col, int row) {
        cursorCol = Math.max(0, Math.min(col, width - 1));
        cursorRow = Math.max(0, Math.min(row, height - 1));
    }

    public void moveCursorUp(int n) {
        setCursor(cursorCol, cursorRow - n);
    }

    public void moveCursorDown(int n) {
        setCursor(cursorCol, cursorRow + n);
    }

    public void moveCursorLeft(int n) {
        setCursor(cursorCol - n, cursorRow);
    }

    public void moveCursorRight(int n) {
        setCursor(cursorCol + n, cursorRow);
    }

    // --- Attributes ---

    public void setForeground(Color fg) {
        currentFg = fg;
    }

    public void setBackground(Color bg) {
        currentBg = bg;
    }

    public void setBold(boolean bold) {
        currentBold = bold;
    }

    public void setItalic(boolean italic) {
        currentItalic = italic;
    }

    public void setUnderline(boolean underline) {
        currentUnderline = underline;
    }

    public void resetAttributes() {
        currentFg = Color.DEFAULT;
        currentBg = Color.DEFAULT;
        currentBold = false;
        currentItalic = false;
        currentUnderline = false;
    }

    // --- Editing operations ---

    /**
     * Overwrites cells at the cursor moving right; cursor row never changes.
     *
     * Cursor snaps left if it starts on a CONTINUATION. Stop condition: before each character except
     * the first, if cursor is already at width-1, return immediately — no partial writes at the edge.
     *
     * Wide chars need two columns: writes WIDE at cursorCol, CONTINUATION at cursorCol+1.
     * If only one column remains (cursorCol == width-1), the wide char is skipped entirely.
     * Orphan cleanup on overwrite: narrow-over-WIDE blanks col+1; wide-over-adjacent-WIDE blanks col+2.
     * Cursor advances by 2 for wide, 1 for narrow, clamped to width-1.
     */
    public void writeText(String text) {
        if (text.isEmpty()) {
            return;
        }
        snapCursorOffContinuation();
        int[] codePoints = text.codePoints().toArray();
        for (int i = 0; i < codePoints.length; i++) {

            int cp = codePoints[i];
            if (UnicodeUtils.isWide(cp)) {
                if (cursorCol == width - 1) return;
                // If the cell at cursorCol+1 is WIDE, blank its orphaned CONTINUATION at cursorCol+2
                if (screen[cursorRow].getCell(cursorCol + 1).type == CellType.WIDE && cursorCol + 2 < width) {
                    screen[cursorRow].setCell(cursorCol + 2, Cell.blank());
                }
                Cell wide = new Cell(cp, currentFg, currentBg, currentBold, currentItalic, currentUnderline);
                wide.type = CellType.WIDE;
                screen[cursorRow].cells[cursorCol] = wide;
                screen[cursorRow].cells[cursorCol + 1] = Cell.continuation();

                cursorCol = Math.min(cursorCol + 2, width - 1);
            } else {
                // If overwriting a WIDE cell, blank its orphaned CONTINUATION
                if (screen[cursorRow].getCell(cursorCol).type == CellType.WIDE && cursorCol + 1 < width) {
                    screen[cursorRow].setCell(cursorCol + 1, Cell.blank());
                }
                Cell normal = new Cell(cp, currentFg, currentBg, currentBold, currentItalic, currentUnderline);
                screen[cursorRow].cells[cursorCol] = normal;

                if (cursorCol == width - 1) {
                    return;
                }
                cursorCol = Math.min(cursorCol + 1, width - 1);
            }


        }
    }

    /**
     * Inserts characters at the cursor, shifting existing content rightward across the whole screen.
     * Screen is treated as a flat 1D array: (row, col) → row * width + col.
     * Cursor snaps left if it starts on a CONTINUATION.
     *
     * Phase 1 — available slots: scan backward from the last cell counting consecutive blank NORMAL cells.
     *   CONTINUATION cells are not blank; they are part of an existing wide pair, not free space.
     * Phase 2 — truncate text: walk codepoints accumulating cost (wide=2, narrow=1) until slots run out.
     *   insertCount = total slot we can insert.
     * Phase 3 — cross-row guard: a WIDE cell in the shift range that would land at col width-1 after the
     *   shift would leave its CONTINUATION on the next row, splitting the pair. Decrement insertCount by 1
     *   and repeat until no such violation exists.
     * Phase 4 — shift: copy cells from (total-1-insertCount) down to cursorFlat, each moving right by
     *   insertCount. Reverse order prevents overwriting a source cell before it is copied.
     * Phase 5 — write: fill the freed slots with the truncated text. If a wide char has only 1 slot left,
     *   write a normal space instead. Cursor advances by insertCount flat positions, clamped to last cell.
     */
    public void insertText(String text) {
        if (text.isEmpty()) {
            return;
        }
        snapCursorOffContinuation();
        int[] codePoints = text.codePoints().toArray();
        int total = height * width;
        int cursorFlat = cursorRow * width + cursorCol;

        // Phase 1: count trailing blank NORMAL cells
        int availableSlots = 0;
        for (int i = total - 1; i >= cursorFlat; i--) {
            if (isBlank(screen[i / width].getCell(i % width))) {
                availableSlots++;
            } else {
                break;
            }
        }

        // Phase 2: slot budget — largest prefix of text that fits (wide=2, narrow=1)
        int insertCount = 0;
        for (int cp : codePoints) {
            int cost = UnicodeUtils.isWide(cp) ? 2 : 1;
            if (insertCount + cost > availableSlots) {
                break;
            }

            insertCount += cost;
        }
        if (insertCount == 0) {
            return;
        }

        // Phase 3: cross-row guard — re-scan from cursorFlat after every decrement, since
        // reducing insertCount changes which flat index each existing WIDE cell lands on
        boolean foundViolation = true;
        while (insertCount > 0 && foundViolation) {
            foundViolation = false;
            for (int i = cursorFlat; i < cursorFlat + insertCount; i++) {
                if (screen[i / width].getCell(i % width).type == CellType.WIDE
                        && (i + insertCount) % width == width - 1) {
                    insertCount--;
                    foundViolation = true;
                    break;
                }
            }
        }
        if (insertCount == 0) {
            return;
        }

        // Phase 4: shift existing content rightward by insertCount positions
        for (int i = total - 1 - insertCount; i >= cursorFlat; i--) {
            Cell src = screen[i / width].getCell(i % width);
            screen[(i + insertCount) / width].setCell((i + insertCount) % width, src);
        }

        // Phase 5: write characters into freed slots; insertCount is the slot budget
        int flat = cursorFlat;
        for (int cp : codePoints) {
            if (insertCount <= 0) {
                break;
            }
            if (UnicodeUtils.isWide(cp)) {
                if (insertCount < 2) {
                    screen[flat / width].cells[flat % width] = Cell.blank();
                    flat++;
                    break;
                }
                Cell wide = new Cell(cp, currentFg, currentBg, currentBold, currentItalic, currentUnderline);
                wide.type = CellType.WIDE;
                screen[flat / width].cells[flat % width] = wide;
                screen[(flat + 1) / width].cells[(flat + 1) % width] = Cell.continuation();
                flat += 2;
                insertCount -= 2;
            } else {
                screen[flat / width].cells[flat % width] =
                        new Cell(cp, currentFg, currentBg, currentBold, currentItalic, currentUnderline);
                flat++;
                insertCount--;
            }
        }

        int newFlat = Math.min(flat, total - 1);
        cursorRow = newFlat / width;
        cursorCol = newFlat % width;
    }

    private boolean isBlank(Cell cell) {
        return cell.type == CellType.NORMAL
                && cell.ch == ' '
                && cell.fg == Color.DEFAULT
                && cell.bg == Color.DEFAULT
                && !cell.bold
                && !cell.italic
                && !cell.underline;
    }

    private void snapCursorOffContinuation() {
        if (screen[cursorRow].getCell(cursorCol).type == CellType.CONTINUATION) {
            cursorCol = Math.max(0, cursorCol - 1);
        }
    }

    /**
     * Fills every cell in the cursor row.
     * {@code ch == null} fills with blank NORMAL cells.
     * Narrow ch fills each cell with a NORMAL cell carrying that code point and current pen attributes.
     * Wide ch fills in WIDE+CONTINUATION pairs; if width is odd the last column gets a NORMAL space.
     * Cursor does not move.
     */
    public void fillLine(Integer ch) {
        if (ch == null) {
            for (int col = 0; col < width; col++) {
                screen[cursorRow].cells[col] = Cell.blank();
            }
        } else if (UnicodeUtils.isWide(ch)) {
            for (int col = 0; col + 1 < width; col += 2) {
                Cell wide = new Cell(ch, currentFg, currentBg, currentBold, currentItalic, currentUnderline);
                wide.type = CellType.WIDE;
                screen[cursorRow].cells[col] = wide;
                screen[cursorRow].cells[col + 1] = Cell.continuation();
            }
            if (width % 2 != 0) {
                screen[cursorRow].cells[width - 1] = Cell.blank();
            }
        } else {
            for (int col = 0; col < width; col++) {
                screen[cursorRow].cells[col] =
                        new Cell(ch, currentFg, currentBg, currentBold, currentItalic, currentUnderline);
            }
        }
    }

    // --- Screen-level operations ---

    /**
     * Pushes screen[0] into scrollback (evicting oldest if over limit),
     * shifts all screen lines up by one, and appends a fresh blank line at the bottom.
     * Cursor position is unchanged.
     */
    public void insertEmptyLineAtBottom() {
        scrollback.add(screen[0].copy());
        if (scrollback.size() > maxScrollback) {
            scrollback.remove(0);
        }

        // Shift lines up — reference copy, not deep copy
        System.arraycopy(screen, 1, screen, 0, height - 1);

        screen[height - 1] = new Line(width);
    }

    /** Replaces every screen line with a fresh blank line and resets the cursor to (0, 0). */
    public void clearScreen() {
        for (int i = 0; i < height; i++) {
            screen[i] = new Line(width);
        }
        setCursor(0, 0);
    }

    /** Same as {@link #clearScreen()}, but also discards all scrollback history. */
    public void clearScreenAndScrollback() {
        clearScreen();
        scrollback.clear();
    }

    // --- Content access: screen ---

    /** Returns the code point at {@code (col, row)}; row in [0, height-1]. Returns space if out of bounds.
     *  If the cell is a CONTINUATION, returns the code point of its WIDE partner at {@code col-1}. */
    public int getScreenChar(int col, int row) {
        if (row < 0 || row >= height) {
            return ' ';
        }
        Cell cell = screen[row].getCell(col);
        if (cell.type == CellType.CONTINUATION && col > 0) {
            cell = screen[row].getCell(col - 1);
        }
        return cell.ch;
    }

    /** Returns the cell attributes at {@code (col, row)}; row in [0, height-1]. Returns default attributes if out of bounds.
     *  If the cell is a CONTINUATION, returns the attributes of its WIDE partner at {@code col-1}. */
    public CellAttributes getScreenAttributes(int col, int row) {
        if (row < 0 || row >= height) {
            return new CellAttributes(Color.DEFAULT, Color.DEFAULT, false, false, false);
        }
        Cell cell = screen[row].getCell(col);
        if (cell.type == CellType.CONTINUATION && col > 0) {
            cell = screen[row].getCell(col - 1);
        }
        return new CellAttributes(cell.fg, cell.bg, cell.bold, cell.italic, cell.underline);
    }

    /** Returns the string content of screen row {@code row}; row in [0, height-1]. Returns all-spaces if out of bounds. */
    public String getScreenLine(int row) {
        if (row < 0 || row >= height) {
            return " ".repeat(width);
        }
        return screen[row].toString();
    }

    /** Returns all screen lines from row 0 to row height-1, each terminated with {@code \n}. */
    public String getScreenContent() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < height; i++) {
            sb.append(screen[i].toString()).append('\n');
        }
        return sb.toString();
    }

    // --- Content access: scrollback ---

    /** Returns the code point at {@code (col, row)}; row in [0, scrollback.size()-1], oldest first. Returns space if out of bounds.
     *  If the cell is a CONTINUATION, returns the code point of its WIDE partner at {@code col-1}. */
    public int getScrollbackChar(int col, int row) {
        if (row < 0 || row >= scrollback.size()) {
            return ' ';
        }
        Cell cell = scrollback.get(row).getCell(col);
        if (cell.type == CellType.CONTINUATION && col > 0) {
            cell = scrollback.get(row).getCell(col - 1);
        }
        return cell.ch;
    }

    /** Returns the cell attributes at {@code (col, row)}; row in [0, scrollback.size()-1], oldest first. Returns default attributes if out of bounds.
     *  If the cell is a CONTINUATION, returns the attributes of its WIDE partner at {@code col-1}. */
    public CellAttributes getScrollbackAttributes(int col, int row) {
        if (row < 0 || row >= scrollback.size()) {
            return new CellAttributes(Color.DEFAULT, Color.DEFAULT, false, false, false);
        }
        Cell cell = scrollback.get(row).getCell(col);
        if (cell.type == CellType.CONTINUATION && col > 0) {
            cell = scrollback.get(row).getCell(col - 1);
        }
        return new CellAttributes(cell.fg, cell.bg, cell.bold, cell.italic, cell.underline);
    }

    /** Returns the string content of scrollback row {@code row}; row in [0, scrollback.size()-1], oldest first. Returns all-spaces if out of bounds. */
    public String getScrollbackLine(int row) {
        if (row < 0 || row >= scrollback.size()) {
            return " ".repeat(width);
        }
        return scrollback.get(row).toString();
    }

    /**
     * Returns the full terminal history: scrollback lines (oldest first) followed by screen lines,
     * each terminated with {@code \n}.
     */
    public String getFullContent() {
        StringBuilder sb = new StringBuilder();
        for (Line line : scrollback) {
            sb.append(line.toString()).append('\n');
        }
        for (int i = 0; i < height; i++) {
            sb.append(screen[i].toString()).append('\n');
        }
        return sb.toString();
    }
}