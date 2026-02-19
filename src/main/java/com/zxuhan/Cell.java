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

    public Cell(int ch, Color fg, Color bg, boolean bold, boolean italic, boolean underline) {
        this.ch = ch;
        this.fg = fg;
        this.bg = bg;
        this.bold = bold;
        this.italic = italic;
        this.underline = underline;
    }

    /** Returns a new blank cell: space character, all attributes default. */
    public static Cell blank() {
        return new Cell(' ', Color.DEFAULT, Color.DEFAULT, false, false, false);
    }

    /** Returns a deep copy; prevents aliasing when storing into scrollback. */
    public Cell copy() {
        return new Cell(ch, fg, bg, bold, italic, underline);
    }
}