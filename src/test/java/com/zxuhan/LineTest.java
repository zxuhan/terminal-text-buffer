package com.zxuhan;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LineTest {

    Line line;

    @BeforeEach
    void setUp() {
        line = new Line(5);
    }

    // --- Constructor ---

    @Test
    void allCellsInitializedToBlank() {
        for (int col = 0; col < 5; col++) {
            Cell cell = line.cells[col];
            final int c = col;
            assertAll("col " + c,
                    () -> assertNotNull(cell,                  "cell at col " + c + " should not be null"),
                    () -> assertEquals(' ',  cell.ch, "ch should be space"),
                    () -> assertEquals(Color.DEFAULT, cell.fg, "fg should be DEFAULT"),
                    () -> assertEquals(Color.DEFAULT, cell.bg, "bg should be DEFAULT"),
                    () -> assertFalse(cell.bold,               "bold should be false"),
                    () -> assertFalse(cell.italic,             "italic should be false"),
                    () -> assertFalse(cell.underline,          "underline should be false")
            );
        }
    }

    @Test
    void widthStoredAndCellsArrayLengthMatch() {
        assertEquals(line.width, line.cells.length, "cells.length should equal stored width");
    }

    // --- getCell ---

    @Test
    void getCell_returnsLiveReferenceMutationsVisibleInCellsArray() {
        line.getCell(0).ch = 'X';
        assertEquals('X', line.cells[0].ch, "mutation through getCell reference should be visible in cells array");
    }

    // --- setCell ---

    @Test
    void setCell_storesCopyNotOriginalReference() {
        Cell original = new Cell('A', Color.RED, Color.DEFAULT, false, false, false);
        line.setCell(1, original);
        assertNotSame(original, line.getCell(1), "setCell should store a copy, not the original reference");
    }

    @Test
    void setCell_mutatingOriginalAfterSetDoesNotAffectStoredCell() {
        Cell original = new Cell('A', Color.RED, Color.DEFAULT, false, false, false);
        line.setCell(1, original);
        original.ch = 'Z';
        assertEquals('A', line.getCell(1).ch, "stored cell should not reflect mutations to the original");
    }

    @Test
    void setCell_mutatingStoredCellAfterSetDoesNotAffectOriginal() {
        Cell original = new Cell('A', Color.RED, Color.DEFAULT, false, false, false);
        line.setCell(1, original);
        line.getCell(1).ch = 'Z';
        assertEquals('A', original.ch, "original cell should not reflect mutations to the stored cell");
    }

    @Test
    void setCell_secondSetOverwritesFirst() {
        line.setCell(1, new Cell('A', Color.RED,   Color.DEFAULT, false, false, false));
        line.setCell(1, new Cell('B', Color.GREEN, Color.DEFAULT, false, false, false));
        assertEquals('B', line.getCell(1).ch, "second setCell should overwrite first");
    }

    @Test
    void setCell_colAtNegativeOrBeyondWidthIsSilentNoOp() {
        assertAll(
                () -> assertDoesNotThrow(
                        () -> line.setCell(-1, new Cell('X', Color.RED, Color.DEFAULT, false, false, false)),
                        "setCell with negative col should not throw"),
                () -> assertDoesNotThrow(
                        () -> line.setCell(5,  new Cell('X', Color.RED, Color.DEFAULT, false, false, false)),
                        "setCell at col == width should not throw"),
                () -> assertDoesNotThrow(
                        () -> line.setCell(99, new Cell('X', Color.RED, Color.DEFAULT, false, false, false)),
                        "setCell at col >> width should not throw")
        );
    }

    // --- copy ---

    @Test
    void copy_isDistinctLineAndCellsArrayInstance() {
        Line copy = line.copy();
        assertAll(
                () -> assertNotSame(line,       copy,       "copy() should return a new Line instance"),
                () -> assertNotSame(line.cells, copy.cells, "copy should have a new cells array")
        );
    }

    @Test
    void copy_hasSameWidthAndCellValues() {
        line.setCell(0, new Cell('A', Color.RED, Color.BLUE, true, false, false));
        Line copy = line.copy();
        assertAll(
                () -> assertEquals(line.width, copy.width,               "copy should have same width"),
                () -> assertEquals('A', copy.getCell(0).ch, "copy cell ch should match original"),
                () -> assertEquals(Color.RED,  copy.getCell(0).fg,   "copy cell fg should match original"),
                () -> assertEquals(Color.BLUE, copy.getCell(0).bg,   "copy cell bg should match original"),
                () -> assertTrue(copy.getCell(0).bold,               "copy cell bold should match original")
        );
    }

    @Test
    void copy_mutatingOriginalCellDoesNotAffectCopy() {
        line.setCell(0, new Cell('A', Color.RED, Color.DEFAULT, false, false, false));
        Line copy = line.copy();
        line.getCell(0).ch = 'Z';
        assertEquals('A', copy.getCell(0).ch, "mutating original cell after copy should not affect copy");
    }

    @Test
    void copy_mutatingCopyCellDoesNotAffectOriginal() {
        line.setCell(0, new Cell('A', Color.RED, Color.DEFAULT, false, false, false));
        Line copy = line.copy();
        copy.getCell(0).ch = 'Z';
        assertEquals('A', line.getCell(0).ch, "mutating copy cell should not affect original");
    }

    // --- toString ---

    @Test
    void toString_blankLineIsAllSpaces() {
        assertEquals("     ", line.toString(), "blank line should render as five spaces");
    }

    @Test
    void toString_charactersAppearInColumnOrder() {
        String word = "Hello";
        for (int i = 0; i < word.length(); i++) {
            line.setCell(i, new Cell(word.charAt(i), Color.DEFAULT, Color.DEFAULT, false, false, false));
        }
        assertEquals("Hello", line.toString(), "characters should appear in column order");
    }

    @Test
    void toString_attributesAreNotIncludedInOutput() {
        line.setCell(0, new Cell('A', Color.RED, Color.BLUE, true, true, true));
        assertEquals("A    ", line.toString(), "attributes should not appear in toString output");
    }

    @Test
    void toString_highUnicodeCodePointAboveFFFFRenderedCorrectly() {
        int emoji = 0x1F600;
        line.setCell(0, new Cell(emoji, Color.DEFAULT, Color.DEFAULT, false, false, false));
        String expected = new String(Character.toChars(emoji)) + "    ";
        assertEquals(expected, line.toString(), "code points above U+FFFF should render correctly via appendCodePoint");
    }
}