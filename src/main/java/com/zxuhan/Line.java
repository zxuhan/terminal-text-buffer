package com.zxuhan;

/**
 * A single row in the terminal grid.
 * Each Line owns exactly {@code width} {@link Cell} objects.
 */
public class Line {

    final int width;
    final Cell[] cells;

    public Line(int width) {
        this.width = width;
        this.cells = new Cell[width];
        for (int i = 0; i < width; i++) {
            cells[i] = Cell.blank();
        }
    }

    /** Returns the live cell reference at {@code col}. */
    public Cell getCell(int col) {
        return cells[col];
    }

    /** Writes a defensive copy of {@code cell} at {@code col}; no-op if out of bounds. */
    public void setCell(int col, Cell cell) {
        if (col < 0 || col >= width) return;
        cells[col] = cell.copy();
    }

    /** Returns a deep copy: new Line with new Cell objects at every column. */
    public Line copy() {
        Line copy = new Line(width);
        for (int i = 0; i < width; i++) {
            copy.cells[i] = cells[i].copy();
        }
        return copy;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < width; i++) {
            sb.appendCodePoint(cells[i].ch);
        }
        return sb.toString();
    }
}