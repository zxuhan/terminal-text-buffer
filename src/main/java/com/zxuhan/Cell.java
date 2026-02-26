package com.zxuhan;

/**
 * A single mutable cell in the terminal grid.
 * Use {@link #blank()} for empty cells; call {@link #copy()} before storing elsewhere.
 */
public class Cell {

    /** Unicode code point — int to support code points above U+FFFF. */
    int ch;
    Color fg;
    Color bg;
    boolean bold;
    boolean italic;
    boolean underline;
    CellType type;

    public Cell(int ch, Color fg, Color bg, boolean bold, boolean italic, boolean underline) {
        this.ch = ch;
        this.fg = fg;
        this.bg = bg;
        this.bold = bold;
        this.italic = italic;
        this.underline = underline;
        this.type = CellType.NORMAL;
    }

    /** Returns a new blank cell: space character, all attributes default, type NORMAL. */
    public static Cell blank() {
        return new Cell(' ', Color.DEFAULT, Color.DEFAULT, false, false, false);
    }

    /** Returns a blank cell with type CONTINUATION — placeholder for the right half of a wide character. */
    public static Cell continuation() {
        Cell c = Cell.blank();
        c.type = CellType.CONTINUATION;
        return c;
    }

    /** Returns a deep copy; prevents aliasing when storing into scrollback. */
    public Cell copy() {
        Cell c = new Cell(ch, fg, bg, bold, italic, underline);
        c.type = type;
        return c;
    }
}