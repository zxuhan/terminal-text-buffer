package com.zxuhan;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

class TerminalBufferTest {

    @Nested
    class ConstructorTest {

        TerminalBuffer buf;

        @BeforeEach
        void setUp() {
            buf = new TerminalBuffer(10, 5, 100);
        }

        @Test
        void screen_hasExactlyHeightLines() {
            assertEquals(5, buf.screen.length);
        }

        @Test
        void screen_allSlotsNonNull() {
            for (Line line : buf.screen) {
                assertNotNull(line);
            }
        }

        @Test
        void screen_eachLineHasCorrectWidth() {
            for (Line line : buf.screen) {
                assertEquals(10, line.width);
            }
        }

        @ParameterizedTest
        @CsvSource({"0,0", "0,4", "9,0", "9,4", "4,2"})
        void screen_allCellsAreBlank(int col, int row) {
            Cell cell = buf.screen[row].getCell(col);
            assertAll(
                    () -> assertEquals(' ', cell.ch),
                    () -> assertEquals(Color.DEFAULT, cell.fg),
                    () -> assertEquals(Color.DEFAULT, cell.bg),
                    () -> assertFalse(cell.bold),
                    () -> assertFalse(cell.italic),
                    () -> assertFalse(cell.underline)
            );
        }

        @Test
        void scrollback_isEmpty() {
            assertTrue(buf.scrollback.isEmpty());
        }

        @Test
        void maxScrollback_isStored() {
            assertEquals(100, buf.maxScrollback);
        }

        @Test
        void cursor_startsAtOrigin() {
            assertAll(
                    () -> assertEquals(0, buf.cursorCol),
                    () -> assertEquals(0, buf.cursorRow)
            );
        }

        @Test
        void penColors_areDefault() {
            assertAll(
                    () -> assertEquals(Color.DEFAULT, buf.currentFg),
                    () -> assertEquals(Color.DEFAULT, buf.currentBg)
            );
        }

        @Test
        void penColors_areNotBlackOrWhite() {
            assertAll(
                    () -> assertNotEquals(Color.BLACK, buf.currentFg),
                    () -> assertNotEquals(Color.WHITE, buf.currentFg),
                    () -> assertNotEquals(Color.BLACK, buf.currentBg),
                    () -> assertNotEquals(Color.WHITE, buf.currentBg)
            );
        }

        @Test
        void penAttributes_allFalse() {
            assertAll(
                    () -> assertFalse(buf.currentBold),
                    () -> assertFalse(buf.currentItalic),
                    () -> assertFalse(buf.currentUnderline)
            );
        }
    }

    // -------------------------------------------------------------------------

    @Nested
    class CursorTest {

    }

    // -------------------------------------------------------------------------

    @Nested
    class AttributeTest {

    }

    // -------------------------------------------------------------------------

    @Nested
    class EditingTest {

    }

    // -------------------------------------------------------------------------

    @Nested
    class ScreenOperationsTest {

    }

    // -------------------------------------------------------------------------

    @Nested
    class ContentAccessTest {

    }
}