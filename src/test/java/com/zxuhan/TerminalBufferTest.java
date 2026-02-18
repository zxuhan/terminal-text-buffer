package com.zxuhan;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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
        TerminalBuffer buf;

        @BeforeEach
        void setUp() {
            buf = new TerminalBuffer(10, 5, 100);
        }

        @Nested
        class GettersTest {

            @Test
            void getCursor_beforeAnyMove_returnsOrigin() {
                assertAll(
                        () -> assertEquals(0, buf.getCursorCol()),
                        () -> assertEquals(0, buf.getCursorRow())
                );
            }

            @Test
            void getCursor_afterSetCursor_reflectsNewPosition() {
                buf.setCursor(7, 3);
                assertAll(
                        () -> assertEquals(7, buf.getCursorCol()),
                        () -> assertEquals(3, buf.getCursorRow())
                );
            }
        }

        @Nested
        class SetCursorNormalTest {

            @ParameterizedTest
            @CsvSource({"0,0", "9,4", "0,4", "9,0", "5,2"})
            void setCursor_validCoordinates_landsExactly(int col, int row) {
                buf.setCursor(col, row);
                assertAll(
                        () -> assertEquals(col, buf.getCursorCol()),
                        () -> assertEquals(row, buf.getCursorRow())
                );
            }
        }

        @Nested
        class SetCursorClampingTest {

            @Test
            void setCursor_negativeCol_clampsToZero() {
                buf.setCursor(-1, 2);
                assertEquals(0, buf.getCursorCol());
            }

            @Test
            void setCursor_colBeyondMax_clampsToWidthMinusOne() {
                buf.setCursor(200, 2);
                assertEquals(9, buf.getCursorCol());
            }

            @Test
            void setCursor_negativeRow_clampsToZero() {
                buf.setCursor(3, -1);
                assertEquals(0, buf.getCursorRow());
            }

            @Test
            void setCursor_rowBeyondMax_clampsToHeightMinusOne() {
                buf.setCursor(3, 200);
                assertEquals(4, buf.getCursorRow());
            }

            @Test
            void setCursor_bothAxesOutOfBounds_clampsBothIndependently() {
                buf.setCursor(-5, 99);
                assertAll(
                        () -> assertEquals(0, buf.getCursorCol()),
                        () -> assertEquals(4, buf.getCursorRow())
                );
            }

            @Test
            void setCursor_colAtWidthMinusOne_accepted() {
                buf.setCursor(9, 0);
                assertEquals(9, buf.getCursorCol());
            }

            @Test
            void setCursor_rowAtHeightMinusOne_accepted() {
                buf.setCursor(0, 4);
                assertEquals(4, buf.getCursorRow());
            }
        }

        @Nested
        class MoveCursorUpTest {

            @Test
            void moveCursorUp_nWithinBounds_decreasesRowByN() {
                buf.setCursor(0, 4);
                buf.moveCursorUp(2);
                assertEquals(2, buf.getCursorRow());
            }

            @Test
            void moveCursorUp_anyN_colUnchanged() {
                buf.setCursor(6, 4);
                buf.moveCursorUp(1);
                assertEquals(6, buf.getCursorCol());
            }

            @Test
            void moveCursorUp_nExceedsCurrentRow_clampsToZero() {
                buf.setCursor(0, 2);
                buf.moveCursorUp(10);
                assertEquals(0, buf.getCursorRow());
            }

            @Test
            void moveCursorUp_nIsZero_rowUnchanged() {
                buf.setCursor(3, 3);
                buf.moveCursorUp(0);
                assertEquals(3, buf.getCursorRow());
            }
        }

        @Nested
        class MoveCursorDownTest {

            @Test
            void moveCursorDown_nWithinBounds_increasesRowByN() {
                buf.setCursor(0, 1);
                buf.moveCursorDown(2);
                assertEquals(3, buf.getCursorRow());
            }

            @Test
            void moveCursorDown_anyN_colUnchanged() {
                buf.setCursor(6, 0);
                buf.moveCursorDown(1);
                assertEquals(6, buf.getCursorCol());
            }

            @Test
            void moveCursorDown_nExceedsRemainingRows_clampsToHeightMinusOne() {
                buf.setCursor(0, 2);
                buf.moveCursorDown(10);
                assertEquals(4, buf.getCursorRow());
            }

            @Test
            void moveCursorDown_nIsZero_rowUnchanged() {
                buf.setCursor(3, 1);
                buf.moveCursorDown(0);
                assertEquals(1, buf.getCursorRow());
            }
        }

        @Nested
        class MoveCursorLeftTest {

            @Test
            void moveCursorLeft_nWithinBounds_decreasesColByN() {
                buf.setCursor(7, 0);
                buf.moveCursorLeft(3);
                assertEquals(4, buf.getCursorCol());
            }

            @Test
            void moveCursorLeft_anyN_rowUnchanged() {
                buf.setCursor(5, 3);
                buf.moveCursorLeft(2);
                assertEquals(3, buf.getCursorRow());
            }

            @Test
            void moveCursorLeft_nExceedsCurrentCol_clampsToZero() {
                buf.setCursor(3, 0);
                buf.moveCursorLeft(10);
                assertEquals(0, buf.getCursorCol());
            }

            @Test
            void moveCursorLeft_nIsZero_colUnchanged() {
                buf.setCursor(5, 0);
                buf.moveCursorLeft(0);
                assertEquals(5, buf.getCursorCol());
            }
        }

        @Nested
        class MoveCursorRightTest {

            @Test
            void moveCursorRight_nWithinBounds_increasesColByN() {
                buf.setCursor(2, 0);
                buf.moveCursorRight(4);
                assertEquals(6, buf.getCursorCol());
            }

            @Test
            void moveCursorRight_anyN_rowUnchanged() {
                buf.setCursor(2, 3);
                buf.moveCursorRight(2);
                assertEquals(3, buf.getCursorRow());
            }

            @Test
            void moveCursorRight_nExceedsRemainingCols_clampsToWidthMinusOne() {
                buf.setCursor(7, 0);
                buf.moveCursorRight(10);
                assertEquals(9, buf.getCursorCol());
            }

            @Test
            void moveCursorRight_nIsZero_colUnchanged() {
                buf.setCursor(4, 0);
                buf.moveCursorRight(0);
                assertEquals(4, buf.getCursorCol());
            }
        }

        @Nested
        class CombinedMovesTest {

            @Test
            void moveCursorUpDown_symmetricMoves_returnsToOriginalRow() {
                buf.setCursor(0, 2);
                buf.moveCursorUp(2);
                buf.moveCursorDown(2);
                assertEquals(2, buf.getCursorRow());
            }

            @Test
            void moveCursorLeftRight_symmetricMoves_returnsToOriginalCol() {
                buf.setCursor(5, 0);
                buf.moveCursorLeft(3);
                buf.moveCursorRight(3);
                assertEquals(5, buf.getCursorCol());
            }

            @Test
            void setCursor_calledTwice_secondCallOverridesBoth() {
                buf.setCursor(3, 1);
                buf.setCursor(7, 4);
                assertAll(
                        () -> assertEquals(7, buf.getCursorCol()),
                        () -> assertEquals(4, buf.getCursorRow())
                );
            }

            @Test
            void moveCursor_repeatedlyClampedAtBoundary_staysAtBoundary() {
                buf.setCursor(0, 0);
                buf.moveCursorLeft(5);
                buf.moveCursorUp(5);
                assertAll(
                        () -> assertEquals(0, buf.getCursorCol()),
                        () -> assertEquals(0, buf.getCursorRow())
                );
            }
        }
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