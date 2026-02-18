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

    TerminalBuffer(int width, int height, int maxScrollback) {
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

    int getCursorCol() {
        return cursorCol;
    }

    int getCursorRow() {
        return cursorRow;
    }

    /** Clamps both axes to valid ranges: col to [0, width-1], row to [0, height-1]. */
    void setCursor(int col, int row) {
        cursorCol = Math.max(0, Math.min(col, width - 1));
        cursorRow = Math.max(0, Math.min(row, height - 1));
    }

    void moveCursorUp(int n) {
        setCursor(cursorCol, cursorRow - n);
    }

    void moveCursorDown(int n) {
        setCursor(cursorCol, cursorRow + n);
    }

    void moveCursorLeft(int n) {
        setCursor(cursorCol - n, cursorRow);
    }

    void moveCursorRight(int n) {
        setCursor(cursorCol + n, cursorRow);
    }

    void setForeground(Color fg) {
        currentFg = fg;
    }

    void setBackground(Color bg) {
        currentBg = bg;
    }

    void setBold(boolean bold) {
        currentBold = bold;
    }

    void setItalic(boolean italic) {
        currentItalic = italic;
    }

    void setUnderline(boolean underline) {
        currentUnderline = underline;
    }

    void resetAttributes() {
        currentFg = Color.DEFAULT;
        currentBg = Color.DEFAULT;
        currentBold = false;
        currentItalic = false;
        currentUnderline = false;
    }

    // --- Screen-level operations ---

    /**
     * Pushes screen[0] into scrollback (evicting oldest if over limit),
     * shifts all screen lines up by one, and appends a fresh blank line at the bottom.
     * Cursor position is unchanged.
     */
    void insertEmptyLineAtBottom() {
        scrollback.add(screen[0].copy());
        if (scrollback.size() > maxScrollback) {
            scrollback.remove(0);
        }

        // Shift lines up — reference copy, not deep copy
        System.arraycopy(screen, 1, screen, 0, height - 1);

        screen[height - 1] = new Line(width);
    }

    /** Replaces every screen line with a fresh blank line and resets the cursor to (0, 0). */
    void clearScreen() {
        for (int i = 0; i < height; i++) {
            screen[i] = new Line(width);
        }
        setCursor(0, 0);
    }

    /** Same as clearScreen(), but also discards all scrollback history. */
    void clearScreenAndScrollback() {
        clearScreen();
        scrollback.clear();
    }

    // --- Content access: screen ---

    // Row is in [0, height-1].
    public int getScreenChar(int col, int row) {
        return screen[row].getCell(col).ch;
    }

    public CellAttributes getScreenAttributes(int col, int row) {
        Cell cell = screen[row].getCell(col);
        return new CellAttributes(cell.fg, cell.bg, cell.bold, cell.italic, cell.underline);
    }

    public String getScreenLine(int row) {
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

    // Row is in [0, scrollback.size()-1]; row 0 is the oldest line.
    public int getScrollbackChar(int col, int row) {
        return scrollback.get(row).getCell(col).ch;
    }

    public CellAttributes getScrollbackAttributes(int col, int row) {
        Cell cell = scrollback.get(row).getCell(col);
        return new CellAttributes(cell.fg, cell.bg, cell.bold, cell.italic, cell.underline);
    }

    public String getScrollbackLine(int row) {
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