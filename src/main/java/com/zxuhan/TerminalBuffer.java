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
     * Overwrites cells starting at the cursor position, moving right.
     * Stops before writing if the cursor is already at {@code width-1} and this is not the first character.
     * Cursor row never changes; cursor col is clamped to {@code width-1}.
     */
    public void writeText(String text) {
        if (text.isEmpty()) {
            return;
        }
        int[] codePoints = text.codePoints().toArray();
        for (int i = 0; i < codePoints.length; i++) {
            screen[cursorRow].setCell(cursorCol,
                    new Cell(codePoints[i], currentFg, currentBg, currentBold, currentItalic, currentUnderline));

            if (cursorCol == width - 1) {
                return;
            }
            cursorCol = Math.min(cursorCol + 1, width - 1);
        }
    }

    /**
     * Inserts characters at the cursor, shifting existing content rightward.
     * Only as many characters are inserted as there are trailing blank cells to absorb the shift.
     * Cursor advances by the number of inserted characters (clamped to last cell).
     */
    public void insertText(String text) {
        if (text.isEmpty()) {
            return;
        }
        int[] codePoints = text.codePoints().toArray();
        int total = height * width;
        // Treat the entire screen as a flat array: flat index = row * width + col
        int cursorFlat = cursorRow * width + cursorCol;

        int availableSlots = 0;
        for (int i = total - 1; i >= cursorFlat; i--) {
            Cell cell = screen[i / width].getCell(i % width);
            if (isBlank(cell)) {
                availableSlots++;
            } else {
                break;
            }
        }

        int insertCount = Math.min(codePoints.length, availableSlots);
        if (insertCount == 0) return;

        // Reverse order: avoids overwriting source cells before they are copied
        for (int i = total - 1 - insertCount; i >= cursorFlat; i--) {
            Cell src = screen[i / width].getCell(i % width);
            screen[(i + insertCount) / width].setCell((i + insertCount) % width, src);
        }

        for (int i = 0; i < insertCount; i++) {
            int flat = cursorFlat + i;
            screen[flat / width].setCell(flat % width,
                    new Cell(codePoints[i], currentFg, currentBg, currentBold, currentItalic, currentUnderline));
        }

        int newFlat = Math.min(cursorFlat + insertCount, total - 1);
        cursorRow = newFlat / width;
        cursorCol = newFlat % width;
    }

    private boolean isBlank(Cell cell) {
        return cell.ch == ' '
                && cell.fg == Color.DEFAULT
                && cell.bg == Color.DEFAULT
                && !cell.bold
                && !cell.italic
                && !cell.underline;
    }

    /**
     * Fills every cell in the cursor row.
     * {@code ch == null} fills with blank cells; otherwise fills with the given code point
     * and current pen attributes. Cursor does not move.
     */
    public void fillLine(Integer ch) {
        for (int col = 0; col < width; col++) {
            if (ch == null) {
                screen[cursorRow].setCell(col, Cell.blank());
            } else {
                screen[cursorRow].setCell(col,
                        new Cell(ch, currentFg, currentBg, currentBold, currentItalic, currentUnderline));
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

    /** Returns the code point at {@code (col, row)}; row in [0, height-1]. Returns space if out of bounds. */
    public int getScreenChar(int col, int row) {
        if (row < 0 || row >= height) {
            return ' ';
        }
        return screen[row].getCell(col).ch;
    }

    /** Returns the cell attributes at {@code (col, row)}; row in [0, height-1]. Returns default attributes if out of bounds. */
    public CellAttributes getScreenAttributes(int col, int row) {
        if (row < 0 || row >= height) {
            return new CellAttributes(Color.DEFAULT, Color.DEFAULT, false, false, false);
        }
        Cell cell = screen[row].getCell(col);
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

    /** Returns the code point at {@code (col, row)}; row in [0, scrollback.size()-1], oldest first. Returns space if out of bounds. */
    public int getScrollbackChar(int col, int row) {
        if (row < 0 || row >= scrollback.size()) {
            return ' ';
        }
        return scrollback.get(row).getCell(col).ch;
    }

    /** Returns the cell attributes at {@code (col, row)}; row in [0, scrollback.size()-1], oldest first. Returns default attributes if out of bounds. */
    public CellAttributes getScrollbackAttributes(int col, int row) {
        if (row < 0 || row >= scrollback.size()) {
            return new CellAttributes(Color.DEFAULT, Color.DEFAULT, false, false, false);
        }
        Cell cell = scrollback.get(row).getCell(col);
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