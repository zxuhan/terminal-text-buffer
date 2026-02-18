package com.zxuhan;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CellTest {

    private Cell fullySpecified;

    @BeforeEach
    void setUp() {
        fullySpecified = new Cell('A', Color.RED, Color.BLUE, true, false, true);
    }

    // --- blank() ---

    @Test
    void blank_hasAllDefaultProperties() {
        Cell cell = Cell.blank();
        assertAll("blank cell properties",
                () -> assertEquals(' ',  cell.ch,  "character should be space"),
                () -> assertEquals(Color.DEFAULT, cell.fg,  "foreground should be DEFAULT"),
                () -> assertEquals(Color.DEFAULT, cell.bg,  "background should be DEFAULT"),
                () -> assertFalse(cell.bold,                "bold should be false"),
                () -> assertFalse(cell.italic,              "italic should be false"),
                () -> assertFalse(cell.underline,           "underline should be false")
        );
    }

    @Test
    void blank_returnsNewInstanceEachTime() {
        assertNotSame(Cell.blank(), Cell.blank());
    }

    // --- copy() ---

    @Test
    void copy_isDistinctReferenceWithSameFields() {
        Cell copy = fullySpecified.copy();
        assertAll("copied cell",
                () -> assertNotSame(fullySpecified,          copy,           "should be a new instance"),
                () -> assertEquals(fullySpecified.ch,        copy.ch,        "character should match"),
                () -> assertEquals(fullySpecified.fg,        copy.fg,        "foreground should match"),
                () -> assertEquals(fullySpecified.bg,        copy.bg,        "background should match"),
                () -> assertEquals(fullySpecified.bold,      copy.bold,      "bold should match"),
                () -> assertEquals(fullySpecified.italic,    copy.italic,    "italic should match"),
                () -> assertEquals(fullySpecified.underline, copy.underline, "underline should match")
        );
    }

    @Test
    void copy_mutatingOriginalDoesNotAffectCopy() {
        Cell copy = fullySpecified.copy();
        fullySpecified.ch = 'Z';
        fullySpecified.fg = Color.GREEN;
        fullySpecified.bold = false;
        assertAll(
                () -> assertEquals('A', copy.ch),
                () -> assertEquals(Color.RED,    copy.fg),
                () -> assertTrue(copy.bold)
        );
    }

    @Test
    void copy_mutatingCopyDoesNotAffectOriginal() {
        Cell copy = fullySpecified.copy();
        copy.ch = 'Z';
        copy.fg = Color.GREEN;
        assertAll(
                () -> assertEquals('A', fullySpecified.ch),
                () -> assertEquals(Color.RED,    fullySpecified.fg)
        );
    }

    @Test
    void copy_ofBlankCellProducesCorrectBlank() {
        Cell copy = Cell.blank().copy();
        assertAll("blank copy properties",
                () -> assertEquals(' ',  copy.ch),
                () -> assertEquals(Color.DEFAULT, copy.fg),
                () -> assertEquals(Color.DEFAULT, copy.bg),
                () -> assertFalse(copy.bold),
                () -> assertFalse(copy.italic),
                () -> assertFalse(copy.underline)
        );
    }

    // --- Constructor edge cases ---

    @Test
    void constructor_acceptsHighUnicodePlane() {
        int emoji = 0x1F600; // 😀 — above U+FFFF, would be truncated if stored as char
        Cell cell = new Cell(emoji, Color.DEFAULT, Color.DEFAULT, false, false, false);
        assertEquals(emoji, cell.ch);
    }

    @Test
    void constructor_storesAllColors() {
        for (Color c : Color.values()) {
            assertAll("color " + c,
                    () -> assertEquals(c, new Cell(' ', c, Color.DEFAULT, false, false, false).fg),
                    () -> assertEquals(c, new Cell(' ', Color.DEFAULT, c, false, false, false).bg)
            );
        }
    }

    // --- Color enum ---

    @Test
    void color_enumShape() {
        assertAll("Color enum invariants",
                () -> assertEquals(17,      Color.values().length,  "17 values including DEFAULT"),
                () -> assertEquals(Color.DEFAULT,    Color.values()[0],      "DEFAULT must be first"),
                () -> assertNotEquals(Color.DEFAULT, Color.BLACK,            "DEFAULT is not BLACK"),
                () -> assertNotEquals(Color.DEFAULT, Color.WHITE,            "DEFAULT is not WHITE")
        );
    }
}