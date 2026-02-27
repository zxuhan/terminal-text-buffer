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
            void moveCursorUp_colIsNotAffected() {
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
            void moveCursorDown_colIsNotAffected() {
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
            void moveCursorLeft_rowIsNotAffected() {
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
            void moveCursorRight_rowIsNotAffected() {
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
        TerminalBuffer buf;

        @BeforeEach
        void setUp() {
            buf = new TerminalBuffer(10, 5, 100);
        }

        @Nested
        class SetForegroundTest {

            @Test
            void setForeground_specificColor_storesThatColor() {
                buf.setForeground(Color.RED);
                assertEquals(Color.RED, buf.currentFg);
            }

            @Test
            void setForeground_explicitDefault_restoresDefault() {
                buf.setForeground(Color.GREEN);
                buf.setForeground(Color.DEFAULT);
                assertEquals(Color.DEFAULT, buf.currentFg);
            }

            @Test
            void setForeground_doesNotAffectBackground() {
                buf.setForeground(Color.BLUE);
                assertEquals(Color.DEFAULT, buf.currentBg);
            }
        }

        @Nested
        class SetBackgroundTest {

            @Test
            void setBackground_specificColor_storesThatColor() {
                buf.setBackground(Color.CYAN);
                assertEquals(Color.CYAN, buf.currentBg);
            }

            @Test
            void setBackground_explicitDefault_restoresDefault() {
                buf.setBackground(Color.YELLOW);
                buf.setBackground(Color.DEFAULT);
                assertEquals(Color.DEFAULT, buf.currentBg);
            }

            @Test
            void setBackground_doesNotAffectForeground() {
                buf.setBackground(Color.MAGENTA);
                assertEquals(Color.DEFAULT, buf.currentFg);
            }
        }

        @Nested
        class SetBoldTest {

            @Test
            void setBold_true_setsTrue() {
                buf.setBold(true);
                assertTrue(buf.currentBold);
            }

            @Test
            void setBold_false_setsFalse() {
                buf.setBold(true);
                buf.setBold(false);
                assertFalse(buf.currentBold);
            }

            @Test
            void setBold_doesNotAffectOtherFlags() {
                buf.setBold(true);
                assertAll(
                        () -> assertFalse(buf.currentItalic),
                        () -> assertFalse(buf.currentUnderline)
                );
            }
        }

        @Nested
        class SetItalicTest {

            @Test
            void setItalic_true_setsTrue() {
                buf.setItalic(true);
                assertTrue(buf.currentItalic);
            }

            @Test
            void setItalic_false_setsFalse() {
                buf.setItalic(true);
                buf.setItalic(false);
                assertFalse(buf.currentItalic);
            }

            @Test
            void setItalic_doesNotAffectOtherFlags() {
                buf.setItalic(true);
                assertAll(
                        () -> assertFalse(buf.currentBold),
                        () -> assertFalse(buf.currentUnderline)
                );
            }
        }

        @Nested
        class SetUnderlineTest {

            @Test
            void setUnderline_true_setsTrue() {
                buf.setUnderline(true);
                assertTrue(buf.currentUnderline);
            }

            @Test
            void setUnderline_false_setsFalse() {
                buf.setUnderline(true);
                buf.setUnderline(false);
                assertFalse(buf.currentUnderline);
            }

            @Test
            void setUnderline_doesNotAffectOtherFlags() {
                buf.setUnderline(true);
                assertAll(
                        () -> assertFalse(buf.currentBold),
                        () -> assertFalse(buf.currentItalic)
                );
            }
        }

        @Nested
        class ResetAttributesTest {

            @Test
            void resetAttributes_afterAllChanged_allReturnToDefaults() {
                buf.setForeground(Color.BRIGHT_RED);
                buf.setBackground(Color.BRIGHT_BLUE);
                buf.setBold(true);
                buf.setItalic(true);
                buf.setUnderline(true);

                buf.resetAttributes();

                assertAll(
                        () -> assertEquals(Color.DEFAULT, buf.currentFg),
                        () -> assertEquals(Color.DEFAULT, buf.currentBg),
                        () -> assertFalse(buf.currentBold),
                        () -> assertFalse(buf.currentItalic),
                        () -> assertFalse(buf.currentUnderline)
                );
            }

            @Test
            void resetAttributes_doesNotMoveCursor() {
                buf.setCursor(5, 3);
                buf.resetAttributes();
                assertAll(
                        () -> assertEquals(5, buf.getCursorCol()),
                        () -> assertEquals(3, buf.getCursorRow())
                );
            }
        }
    }

    // -------------------------------------------------------------------------

    @Nested
    class EditingTest {

        // width=5, height=3 unless a test needs something specific
        TerminalBuffer buf;

        @BeforeEach
        void setUp() {
            buf = new TerminalBuffer(5, 3, 10);
        }

        @Nested
        class WriteTextTest {

            @Test
            void writeText_emptyString_cursorUnchanged() {
                buf.setCursor(2, 1);
                buf.writeText("");
                assertAll(
                        () -> assertEquals(2, buf.getCursorCol()),
                        () -> assertEquals(1, buf.getCursorRow())
                );
            }

            @Test
            void writeText_singleChar_writesAtCursorAndAdvancesOne() {
                buf.setCursor(1, 0);
                buf.writeText("X");
                assertAll(
                        () -> assertEquals('X', buf.getScreenChar(1, 0)),
                        () -> assertEquals(2, buf.getCursorCol())
                );
            }

            @Test
            void writeText_multipleChars_writesSequentiallyAndCursorAdvances() {
                buf.setCursor(1, 0);
                buf.writeText("AB");
                assertAll(
                        () -> assertEquals('A', buf.getScreenChar(1, 0)),
                        () -> assertEquals('B', buf.getScreenChar(2, 0)),
                        () -> assertEquals(3, buf.getCursorCol())
                );
            }

            @Test
            void writeText_fromMidRow_doesNotAffectPrecedingCells() {
                buf.setCursor(2, 0);
                buf.writeText("XY");
                assertAll(
                        () -> assertEquals(' ', buf.getScreenChar(0, 0)),
                        () -> assertEquals(' ', buf.getScreenChar(1, 0))
                );
            }

            @Test
            void writeText_cursorAtRightEdge_writesFirstCharOnly() {
                buf.setCursor(4, 0);
                buf.writeText("AB");
                assertAll(
                        () -> assertEquals('A', buf.getScreenChar(4, 0)),
                        () -> assertEquals(4, buf.getCursorCol())
                );
            }

            @Test
            void writeText_truncatesAtRightEdge_neverWraps() {
                buf.setCursor(3, 0);
                buf.writeText("ABCD");
                assertAll(
                        () -> assertEquals('A', buf.getScreenChar(3, 0)),
                        () -> assertEquals('B', buf.getScreenChar(4, 0)),
                        () -> assertEquals(4, buf.getCursorCol()),
                        () -> assertEquals(0, buf.getCursorRow())
                );
            }

            @Test
            void writeText_cursorRowUnchanged_afterWrite() {
                buf.setCursor(0, 1);
                buf.writeText("ABCDE");
                assertEquals(1, buf.getCursorRow());
            }

            @Test
            void writeText_appliesPenAttributes_toWrittenCells() {
                buf.setForeground(Color.RED);
                buf.setBackground(Color.BLUE);
                buf.setBold(true);
                buf.setCursor(0, 0);
                buf.writeText("A");

                CellAttributes attrs = buf.getScreenAttributes(0, 0);
                assertAll(
                        () -> assertEquals(Color.RED, attrs.fg()),
                        () -> assertEquals(Color.BLUE, attrs.bg()),
                        () -> assertTrue(attrs.bold())
                );
            }

            @Test
            void writeText_highUnicodeCodePoint_storedAsInt() {
                buf.setCursor(0, 0);
                buf.writeText("\uD83D\uDE00");  // U+1F600 GRINNING FACE (surrogate pair)
                assertEquals(0x1F600, buf.getScreenChar(0, 0));
            }
        }

        @Nested
        class InsertTextTest {

            @Test
            void insertText_emptyString_noChange() {
                buf.setCursor(0, 0);
                buf.insertText("");
                assertEquals("     ", buf.getScreenLine(0));
                assertEquals(0, buf.getCursorCol());
            }

            @Test
            void insertText_intoAllBlankRow_writesAtCursor() {
                buf.setCursor(1, 0);
                buf.insertText("AB");
                assertAll(
                        () -> assertEquals('A', buf.getScreenChar(1, 0)),
                        () -> assertEquals('B', buf.getScreenChar(2, 0))
                );
            }

            @Test
            void insertText_shiftsExistingContentRight() {
                // Pre-fill col 1 with 'Z'
                buf.screen[0].getCell(1).ch = 'Z';
                buf.setCursor(1, 0);
                buf.insertText("A");
                assertAll(
                        () -> assertEquals('A', buf.getScreenChar(1, 0)),
                        () -> assertEquals('Z', buf.getScreenChar(2, 0))
                );
            }

            @Test
            void insertText_truncatesTextToAvailableSlots() {
                // "XXX  " — only 2 trailing blanks, so 5-char input is truncated to 2
                buf = new TerminalBuffer(5, 1, 10);
                for (int c = 0; c < 3; c++) buf.screen[0].getCell(c).ch = 'X';
                buf.setCursor(0, 0);
                buf.insertText("ABCDE");
                assertAll(
                        () -> assertEquals('A', buf.getScreenChar(0, 0)),
                        () -> assertEquals('B', buf.getScreenChar(1, 0)),
                        () -> assertEquals('X', buf.getScreenChar(2, 0))
                );
            }

            @Test
            void insertText_screenFull_noOp() {
                buf = new TerminalBuffer(3, 1, 10);
                buf.screen[0].getCell(0).ch = 'A';
                buf.screen[0].getCell(1).ch = 'B';
                buf.screen[0].getCell(2).ch = 'C';
                buf.setCursor(0, 0);
                buf.insertText("X");
                assertEquals("ABC", buf.getScreenLine(0));
                assertEquals(0, buf.getCursorCol());
            }

            @Test
            void insertText_cursorAdvancesByInsertCount() {
                buf.setCursor(0, 0);
                buf.insertText("AB");
                assertEquals(2, buf.getCursorCol());
            }

            @Test
            void insertText_cursorCanAdvanceToNextRow() {
                buf = new TerminalBuffer(3, 2, 10);
                // row 0: "X  ", row 1: "   " → flat: X at 0, blanks 1-5
                buf.screen[0].getCell(0).ch = 'X';
                buf.setCursor(1, 0);
                buf.insertText("ABCDE");
                assertAll(
                        () -> assertEquals('A', buf.getScreenChar(1, 0)),
                        () -> assertEquals('B', buf.getScreenChar(2, 0)),
                        () -> assertEquals('C', buf.getScreenChar(0, 1)),
                        () -> assertEquals('D', buf.getScreenChar(1, 1)),
                        () -> assertEquals('E', buf.getScreenChar(2, 1)),
                        () -> assertEquals(1, buf.getCursorRow())
                );
            }

            @Test
            void insertText_appliesPenAttributesToInsertedCells() {
                buf.setForeground(Color.GREEN);
                buf.setBold(true);
                buf.setCursor(0, 0);
                buf.insertText("A");

                CellAttributes attrs = buf.getScreenAttributes(0, 0);
                assertAll(
                        () -> assertEquals(Color.GREEN, attrs.fg()),
                        () -> assertTrue(attrs.bold())
                );
            }

            @Test
            void insertText_existingContentRetainsOriginalAttributes() {
                Cell existing = buf.screen[0].getCell(0);
                existing.ch = 'Z';
                existing.fg = Color.YELLOW;
                buf.setForeground(Color.RED);
                buf.setCursor(0, 0);
                buf.insertText("A");

                // 'Z' was shifted to col 1
                CellAttributes shifted = buf.getScreenAttributes(1, 0);
                assertEquals(Color.YELLOW, shifted.fg());
            }

            @Test
            void insertText_lastNonBlankFollowedByBlanks_correctSlotCount() {
                buf = new TerminalBuffer(5, 1, 10);
                buf.screen[0].getCell(0).ch = 'A';
                buf.screen[0].getCell(1).ch = 'B';
                // cols 2,3,4 blank → 3 available slots
                buf.setCursor(2, 0);
                buf.insertText("XYZ");  // exactly fits
                assertAll(
                        () -> assertEquals('X', buf.getScreenChar(2, 0)),
                        () -> assertEquals('Y', buf.getScreenChar(3, 0)),
                        () -> assertEquals('Z', buf.getScreenChar(4, 0))
                );
            }

            @Test
            void insertText_nonBlankAtEnd_zeroAvailableSlots() {
                buf = new TerminalBuffer(5, 1, 10);
                buf.screen[0].getCell(0).ch = 'A';
                buf.screen[0].getCell(1).ch = 'B';
                // cols 2,3 blank (middle), col 4 = 'C' (non-blank at end)
                buf.screen[0].getCell(4).ch = 'C';
                buf.setCursor(2, 0);
                buf.insertText("X");
                assertAll(
                        () -> assertEquals('A', buf.getScreenChar(0, 0)),
                        () -> assertEquals('B', buf.getScreenChar(1, 0)),
                        () -> assertEquals(' ', buf.getScreenChar(2, 0)),
                        () -> assertEquals(' ', buf.getScreenChar(3, 0)),
                        () -> assertEquals('C', buf.getScreenChar(4, 0)),
                        () -> assertEquals(2, buf.getCursorCol())
                );
            }
        }

        @Nested
        class FillLineTest {

            @Test
            void fillLine_nullArg_fillsRowWithBlankCells() {
                buf.screen[1].getCell(0).ch = 'X';
                buf.setCursor(0, 1);
                buf.fillLine(null);
                assertEquals("     ", buf.getScreenLine(1));
            }

            @Test
            void fillLine_nonNullArg_fillsRowWithGivenCodePoint() {
                buf.setCursor(0, 0);
                buf.fillLine((int) '-');
                assertEquals("-----", buf.getScreenLine(0));
            }

            @Test
            void fillLine_appliesPenAttributesWhenNonNull() {
                buf.setForeground(Color.CYAN);
                buf.setItalic(true);
                buf.setCursor(0, 0);
                buf.fillLine((int) '*');

                CellAttributes attrs = buf.getScreenAttributes(2, 0);
                assertAll(
                        () -> assertEquals(Color.CYAN, attrs.fg()),
                        () -> assertTrue(attrs.italic())
                );
            }

            @Test
            void fillLine_nullArg_allAttributesAreDefault() {
                buf.screen[0].getCell(0).fg = Color.RED;
                buf.setCursor(0, 0);
                buf.fillLine(null);

                CellAttributes attrs = buf.getScreenAttributes(0, 0);
                assertAll(
                        () -> assertEquals(Color.DEFAULT, attrs.fg()),
                        () -> assertEquals(Color.DEFAULT, attrs.bg()),
                        () -> assertFalse(attrs.bold()),
                        () -> assertFalse(attrs.italic()),
                        () -> assertFalse(attrs.underline())
                );
            }

            @Test
            void fillLine_doesNotMoveCursor() {
                buf.setCursor(3, 1);
                buf.fillLine((int) 'A');
                assertAll(
                        () -> assertEquals(3, buf.getCursorCol()),
                        () -> assertEquals(1, buf.getCursorRow())
                );
            }

            @Test
            void fillLine_overwritesExistingContent() {
                buf.screen[0].getCell(2).ch = 'Q';
                buf.setCursor(0, 0);
                buf.fillLine((int) 'Z');
                assertEquals("ZZZZZ", buf.getScreenLine(0));
            }

            @Test
            void fillLine_onlyAffectsCursorRow_otherRowsUnchanged() {
                buf.setCursor(0, 1);
                buf.fillLine((int) 'X');
                assertAll(
                        () -> assertEquals("     ", buf.getScreenLine(0)),
                        () -> assertEquals("     ", buf.getScreenLine(2))
                );
            }
        }
    }

    // -------------------------------------------------------------------------

    @Nested
    class ScreenOperationsTest {

        @Nested
        class InsertEmptyLineAtBottomTest {

            @Test
            void insertEmptyLineAtBottom_nonFullScrollback_pushesOldestScreenRowToScrollback() {
                TerminalBuffer buf = new TerminalBuffer(5, 3, 10);
                buf.setCursor(0, 0);
                buf.writeText("ABC  ");

                buf.insertEmptyLineAtBottom();

                assertEquals(1, buf.scrollback.size());
                assertEquals("ABC  ", buf.scrollback.get(0).toString());
            }

            @Test
            void insertEmptyLineAtBottom_nonFullScrollback_shiftsScreenLinesUp() {
                TerminalBuffer buf = new TerminalBuffer(5, 3, 10);
                buf.setCursor(0, 0);
                buf.writeText("AAA  ");
                buf.setCursor(0, 1);
                buf.writeText("BBB  ");
                buf.setCursor(0, 2);
                buf.writeText("CCC  ");

                buf.insertEmptyLineAtBottom();

                assertEquals("BBB  ", buf.getScreenLine(0));
                assertEquals("CCC  ", buf.getScreenLine(1));
            }

            @Test
            void insertEmptyLineAtBottom_anyCall_appendsBlankLineAtBottom() {
                TerminalBuffer buf = new TerminalBuffer(5, 3, 10);
                buf.setCursor(0, 0);
                buf.writeText("AAA  ");

                buf.insertEmptyLineAtBottom();

                assertEquals("     ", buf.getScreenLine(2));
            }

            @Test
            void insertEmptyLineAtBottom_anyCall_cursorPositionUnchanged() {
                TerminalBuffer buf = new TerminalBuffer(5, 3, 10);
                buf.setCursor(2, 1);

                buf.insertEmptyLineAtBottom();

                assertEquals(2, buf.getCursorCol());
                assertEquals(1, buf.getCursorRow());
            }

            @Test
            void insertEmptyLineAtBottom_scrollbackAtMaxScrollback_evictsOldestLine() {
                TerminalBuffer buf = new TerminalBuffer(5, 2, 2);
                buf.setCursor(0, 0);
                buf.writeText("L1   ");
                buf.insertEmptyLineAtBottom(); // scrollback: [L1]
                buf.setCursor(0, 0);
                buf.writeText("L2   ");
                buf.insertEmptyLineAtBottom(); // scrollback: [L1, L2]
                buf.setCursor(0, 0);
                buf.writeText("L3   ");
                buf.insertEmptyLineAtBottom(); // scrollback should be [L2, L3]

                assertEquals(2, buf.scrollback.size());
                assertEquals("L2   ", buf.scrollback.get(0).toString());
                assertEquals("L3   ", buf.scrollback.get(1).toString());
            }

            @Test
            void insertEmptyLineAtBottom_scrollbackSnapshot_isolatedFromSubsequentScreenEdits() {
                TerminalBuffer buf = new TerminalBuffer(5, 3, 10);
                buf.setCursor(0, 0);
                buf.writeText("ORIG ");

                buf.insertEmptyLineAtBottom();

                // Mutate what is now screen[0] (was screen[1] before shift)
                buf.screen[0].getCell(0).ch = 'X';

                assertEquals("ORIG ", buf.scrollback.get(0).toString());
            }

            @Test
            void insertEmptyLineAtBottom_multipleInserts_scrollbackOrderIsOldestFirst() {
                TerminalBuffer buf = new TerminalBuffer(5, 2, 10);
                buf.setCursor(0, 0);
                buf.writeText("FIRST");
                buf.insertEmptyLineAtBottom();
                buf.setCursor(0, 0);
                buf.writeText("SECND");
                buf.insertEmptyLineAtBottom();

                assertEquals("FIRST", buf.scrollback.get(0).toString());
                assertEquals("SECND", buf.scrollback.get(1).toString());
            }
        }

        @Nested
        class ClearScreenTest {

            @Test
            void clearScreen_withContent_allScreenRowsBecomeBlanks() {
                TerminalBuffer buf = new TerminalBuffer(4, 3, 10);
                buf.setCursor(0, 0);
                buf.writeText("ABCD");
                buf.setCursor(0, 1);
                buf.writeText("EFGH");
                buf.setCursor(0, 2);
                buf.writeText("IJKL");

                buf.clearScreen();

                for (int r = 0; r < buf.height; r++) {
                    assertEquals("    ", buf.getScreenLine(r));
                }
            }

            @Test
            void clearScreen_withCursorNotAtOrigin_resetsCursorToOrigin() {
                TerminalBuffer buf = new TerminalBuffer(5, 3, 10);
                buf.setCursor(3, 2);

                buf.clearScreen();

                assertEquals(0, buf.getCursorCol());
                assertEquals(0, buf.getCursorRow());
            }

            @Test
            void clearScreen_withScrollback_scrollbackUntouched() {
                TerminalBuffer buf = new TerminalBuffer(5, 2, 10);
                buf.setCursor(0, 0);
                buf.writeText("HIST ");
                buf.insertEmptyLineAtBottom(); // populate scrollback

                buf.clearScreen();

                assertEquals(1, buf.scrollback.size());
                assertEquals("HIST ", buf.scrollback.get(0).toString());
            }
        }

        @Nested
        class ClearScreenAndScrollbackTest {

            @Test
            void clearScreenAndScrollback_withContent_allScreenRowsBecomeBlanks() {
                TerminalBuffer buf = new TerminalBuffer(4, 2, 10);
                buf.setCursor(0, 0);
                buf.writeText("ABCD");
                buf.setCursor(0, 1);
                buf.writeText("EFGH");

                buf.clearScreenAndScrollback();

                for (int r = 0; r < buf.height; r++) {
                    assertEquals("    ", buf.getScreenLine(r));
                }
            }

            @Test
            void clearScreenAndScrollback_withScrollback_scrollbackBecomesEmpty() {
                TerminalBuffer buf = new TerminalBuffer(5, 2, 10);
                buf.setCursor(0, 0);
                buf.writeText("LINE1");
                buf.insertEmptyLineAtBottom();
                buf.setCursor(0, 0);
                buf.writeText("LINE2");
                buf.insertEmptyLineAtBottom();

                buf.clearScreenAndScrollback();

                assertEquals(0, buf.scrollback.size());
            }

            @Test
            void clearScreenAndScrollback_withCursorNotAtOrigin_resetsCursorToOrigin() {
                TerminalBuffer buf = new TerminalBuffer(5, 3, 10);
                buf.setCursor(4, 2);

                buf.clearScreenAndScrollback();

                assertEquals(0, buf.getCursorCol());
                assertEquals(0, buf.getCursorRow());
            }

            @Test
            void clearScreenAndScrollback_emptyScrollback_remainsEmpty() {
                TerminalBuffer buf = new TerminalBuffer(5, 2, 10);

                buf.clearScreenAndScrollback();

                assertEquals(0, buf.scrollback.size());
            }
        }
    }

    // -------------------------------------------------------------------------

    @Nested
    class ContentAccessTest {

        private TerminalBuffer buf;

        @BeforeEach
        void setUp() {
            buf = new TerminalBuffer(5, 3, 3);
        }

        private void scrollLines(int n) {
            for (int i = 0; i < n; i++) {
                buf.setCursor(0, 0);
                buf.writeText("L" + i + "   ");
                buf.insertEmptyLineAtBottom();
            }
        }

        @Nested
        class GetScreenChar {

            @Test
            void getScreenChar_freshBuffer_returnsSpace() {
                assertEquals(' ', buf.getScreenChar(0, 0));
            }

            @Test
            void getScreenChar_afterWrite_returnsWrittenCodePoint() {
                buf.setCursor(0, 1);
                buf.writeText("  X  ");
                assertEquals('X', buf.getScreenChar(2, 1));
            }

            @Test
            void getScreenChar_unwrittenCellAdjacentToWrite_remainsSpace() {
                buf.setCursor(0, 1);
                buf.writeText("  X  ");
                assertEquals(' ', buf.getScreenChar(3, 1));
            }

            @Test
            void getScreenChar_afterScroll_reflectsShiftedContent() {
                buf.setCursor(0, 0);
                buf.writeText("TOP  ");
                buf.insertEmptyLineAtBottom();
                // screen[0] is now what was screen[1] — blank
                assertEquals(' ', buf.getScreenChar(0, 0));
            }
        }

        @Nested
        class GetScreenAttributes {

            @Test
            void getScreenAttributes_freshBuffer_returnsAllDefaults() {
                CellAttributes attrs = buf.getScreenAttributes(0, 0);
                assertAll(
                        () -> assertEquals(Color.DEFAULT, attrs.fg()),
                        () -> assertEquals(Color.DEFAULT, attrs.bg()),
                        () -> assertFalse(attrs.bold()),
                        () -> assertFalse(attrs.italic()),
                        () -> assertFalse(attrs.underline())
                );
            }

            @Test
            void getScreenAttributes_afterDirectCellWrite_reflectsStoredAttributes() {
                Cell cell = buf.screen[1].getCell(1);
                cell.fg = Color.RED;
                cell.bg = Color.BLUE;
                cell.bold = true;
                cell.italic = true;
                cell.underline = true;
                cell.ch = 'A';

                CellAttributes attrs = buf.getScreenAttributes(1, 1);
                assertAll(
                        () -> assertEquals(Color.RED, attrs.fg()),
                        () -> assertEquals(Color.BLUE, attrs.bg()),
                        () -> assertTrue(attrs.bold()),
                        () -> assertTrue(attrs.italic()),
                        () -> assertTrue(attrs.underline())
                );
            }
        }

        @Nested
        class GetScreenLine {

            @Test
            void getScreenLine_freshBuffer_returnsAllSpaces() {
                assertEquals("     ", buf.getScreenLine(0));
            }

            @Test
            void getScreenLine_afterPartialWrite_paddedWithSpaces() {
                buf.setCursor(0, 1);
                buf.writeText("Hi");
                assertEquals("Hi   ", buf.getScreenLine(1));
            }

            @Test
            void getScreenLine_lastRow_returnsCorrectContent() {
                buf.setCursor(0, 2);
                buf.writeText("End  ");
                assertEquals("End  ", buf.getScreenLine(2));
            }
        }

        @Nested
        class GetScreenContent {

            @Test
            void getScreenContent_freshBuffer_allBlankLinesWithTrailingNewlines() {
                assertEquals("     \n     \n     \n", buf.getScreenContent());
            }

            @Test
            void getScreenContent_afterWrites_includesAllRowsInOrder() {
                buf.setCursor(0, 0);
                buf.writeText("AAA  ");
                buf.setCursor(0, 1);
                buf.writeText("BBB  ");
                buf.setCursor(0, 2);
                buf.writeText("CCC  ");
                assertEquals("AAA  \nBBB  \nCCC  \n", buf.getScreenContent());
            }

            @Test
            void getScreenContent_lastLineAlsoHasTrailingNewline() {
                String content = buf.getScreenContent();
                assertTrue(content.endsWith("\n"));
                // Exactly one newline per row, no extras
                assertEquals(buf.height, content.chars().filter(c -> c == '\n').count());
            }
        }

        @Nested
        class GetScrollbackChar {

            @Test
            void getScrollbackChar_singleScroll_row0IsScrolledLine() {
                buf.setCursor(0, 0);
                buf.writeText("HELLO");
                buf.insertEmptyLineAtBottom();

                assertEquals('H', buf.getScrollbackChar(0, 0));
                assertEquals('E', buf.getScrollbackChar(1, 0));
            }

            @Test
            void getScrollbackChar_multipleScrolls_oldestAtRow0NewestAtLastIndex() {
                scrollLines(3);
                assertEquals('0', buf.getScrollbackChar(1, 0));  // oldest: "L0   "
                assertEquals('2', buf.getScrollbackChar(1, 2));  // newest: "L2   "
            }

            @Test
            void getScrollbackChar_scrolledBlankLine_returnsSpace() {
                buf.insertEmptyLineAtBottom();
                assertEquals(' ', buf.getScrollbackChar(0, 0));
            }

            @Test
            void getScrollbackChar_mutatingScreenAfterScroll_doesNotCorruptScrollback() {
                buf.setCursor(0, 0);
                buf.writeText("ORIG ");
                buf.insertEmptyLineAtBottom();
                // Overwrite screen row 0 (now the line that was screen[1])
                buf.setCursor(0, 0);
                buf.writeText("MUTD ");

                assertEquals('O', buf.getScrollbackChar(0, 0));
            }
        }

        @Nested
        class GetScrollbackAttributes {

            @Test
            void getScrollbackAttributes_scrolledCellWithAttributes_preservedInScrollback() {
                Cell cell = buf.screen[0].getCell(0);
                cell.fg = Color.MAGENTA;
                cell.bold = true;
                buf.insertEmptyLineAtBottom();

                CellAttributes attrs = buf.getScrollbackAttributes(0, 0);
                assertAll(
                        () -> assertEquals(Color.MAGENTA, attrs.fg()),
                        () -> assertTrue(attrs.bold())
                );
            }

            @Test
            void getScrollbackAttributes_mutatingCellAfterScroll_doesNotCorruptScrollback() {
                buf.screen[0].getCell(0).fg = Color.YELLOW;
                buf.insertEmptyLineAtBottom();

                // Mutate the cell that is now on screen
                buf.screen[0].getCell(0).fg = Color.CYAN;

                assertEquals(Color.YELLOW, buf.getScrollbackAttributes(0, 0).fg());
            }
        }

        @Nested
        class GetScrollbackLine {

            @Test
            void getScrollbackLine_singleScroll_returnsScrolledContent() {
                buf.setCursor(0, 0);
                buf.writeText("ROW0 ");
                buf.insertEmptyLineAtBottom();

                assertEquals("ROW0 ", buf.getScrollbackLine(0));
            }

            @Test
            void getScrollbackLine_multipleScrolls_indexMatchesScrollOrder() {
                buf.setCursor(0, 0);
                buf.writeText("FIRST");
                buf.insertEmptyLineAtBottom();
                buf.setCursor(0, 0);
                buf.writeText("SECND");
                buf.insertEmptyLineAtBottom();

                assertEquals("FIRST", buf.getScrollbackLine(0));
                assertEquals("SECND", buf.getScrollbackLine(1));
            }
        }

        @Nested
        class OutOfBoundsAccessTest {

            // --- getScreenChar ---

            @Test
            void getScreenChar_negativeCol_returnsSpace() {
                assertEquals(' ', buf.getScreenChar(-1, 0));
            }

            @Test
            void getScreenChar_colBeyondWidth_returnsSpace() {
                assertEquals(' ', buf.getScreenChar(100, 0));
            }

            @Test
            void getScreenChar_negativeRow_returnsSpace() {
                assertEquals(' ', buf.getScreenChar(0, -1));
            }

            @Test
            void getScreenChar_rowBeyondHeight_returnsSpace() {
                assertEquals(' ', buf.getScreenChar(0, 100));
            }

            // --- getScreenAttributes ---

            @Test
            void getScreenAttributes_negativeCol_returnsDefaultAttributes() {
                CellAttributes attrs = buf.getScreenAttributes(-1, 0);
                assertAll(
                        () -> assertEquals(Color.DEFAULT, attrs.fg()),
                        () -> assertEquals(Color.DEFAULT, attrs.bg()),
                        () -> assertFalse(attrs.bold()),
                        () -> assertFalse(attrs.italic()),
                        () -> assertFalse(attrs.underline())
                );
            }

            @Test
            void getScreenAttributes_colBeyondWidth_returnsDefaultAttributes() {
                CellAttributes attrs = buf.getScreenAttributes(100, 0);
                assertAll(
                        () -> assertEquals(Color.DEFAULT, attrs.fg()),
                        () -> assertEquals(Color.DEFAULT, attrs.bg()),
                        () -> assertFalse(attrs.bold()),
                        () -> assertFalse(attrs.italic()),
                        () -> assertFalse(attrs.underline())
                );
            }

            @Test
            void getScreenAttributes_negativeRow_returnsDefaultAttributes() {
                CellAttributes attrs = buf.getScreenAttributes(0, -1);
                assertAll(
                        () -> assertEquals(Color.DEFAULT, attrs.fg()),
                        () -> assertEquals(Color.DEFAULT, attrs.bg()),
                        () -> assertFalse(attrs.bold()),
                        () -> assertFalse(attrs.italic()),
                        () -> assertFalse(attrs.underline())
                );
            }

            @Test
            void getScreenAttributes_rowBeyondHeight_returnsDefaultAttributes() {
                CellAttributes attrs = buf.getScreenAttributes(0, 100);
                assertAll(
                        () -> assertEquals(Color.DEFAULT, attrs.fg()),
                        () -> assertEquals(Color.DEFAULT, attrs.bg()),
                        () -> assertFalse(attrs.bold()),
                        () -> assertFalse(attrs.italic()),
                        () -> assertFalse(attrs.underline())
                );
            }

            // --- getScreenLine ---

            @Test
            void getScreenLine_negativeRow_returnsAllSpaces() {
                assertEquals("     ", buf.getScreenLine(-1));
            }

            @Test
            void getScreenLine_rowBeyondHeight_returnsAllSpaces() {
                assertEquals("     ", buf.getScreenLine(100));
            }

            // --- getScrollbackChar ---

            @Test
            void getScrollbackChar_negativeCol_returnsSpace() {
                buf.insertEmptyLineAtBottom();
                assertEquals(' ', buf.getScrollbackChar(-1, 0));
            }

            @Test
            void getScrollbackChar_colBeyondWidth_returnsSpace() {
                buf.insertEmptyLineAtBottom();
                assertEquals(' ', buf.getScrollbackChar(100, 0));
            }

            @Test
            void getScrollbackChar_negativeRow_returnsSpace() {
                assertEquals(' ', buf.getScrollbackChar(0, -1));
            }

            @Test
            void getScrollbackChar_rowBeyondSize_returnsSpace() {
                assertEquals(' ', buf.getScrollbackChar(0, 100));
            }

            @Test
            void getScrollbackChar_emptyScrollback_returnsSpace() {
                assertEquals(' ', buf.getScrollbackChar(0, 0));
            }

            // --- getScrollbackAttributes ---

            @Test
            void getScrollbackAttributes_negativeCol_returnsDefaultAttributes() {
                buf.insertEmptyLineAtBottom();
                CellAttributes attrs = buf.getScrollbackAttributes(-1, 0);
                assertAll(
                        () -> assertEquals(Color.DEFAULT, attrs.fg()),
                        () -> assertEquals(Color.DEFAULT, attrs.bg()),
                        () -> assertFalse(attrs.bold()),
                        () -> assertFalse(attrs.italic()),
                        () -> assertFalse(attrs.underline())
                );
            }

            @Test
            void getScrollbackAttributes_colBeyondWidth_returnsDefaultAttributes() {
                buf.insertEmptyLineAtBottom();
                CellAttributes attrs = buf.getScrollbackAttributes(100, 0);
                assertAll(
                        () -> assertEquals(Color.DEFAULT, attrs.fg()),
                        () -> assertEquals(Color.DEFAULT, attrs.bg()),
                        () -> assertFalse(attrs.bold()),
                        () -> assertFalse(attrs.italic()),
                        () -> assertFalse(attrs.underline())
                );
            }

            @Test
            void getScrollbackAttributes_negativeRow_returnsDefaultAttributes() {
                CellAttributes attrs = buf.getScrollbackAttributes(0, -1);
                assertAll(
                        () -> assertEquals(Color.DEFAULT, attrs.fg()),
                        () -> assertEquals(Color.DEFAULT, attrs.bg()),
                        () -> assertFalse(attrs.bold()),
                        () -> assertFalse(attrs.italic()),
                        () -> assertFalse(attrs.underline())
                );
            }

            @Test
            void getScrollbackAttributes_rowBeyondSize_returnsDefaultAttributes() {
                CellAttributes attrs = buf.getScrollbackAttributes(0, 100);
                assertAll(
                        () -> assertEquals(Color.DEFAULT, attrs.fg()),
                        () -> assertEquals(Color.DEFAULT, attrs.bg()),
                        () -> assertFalse(attrs.bold()),
                        () -> assertFalse(attrs.italic()),
                        () -> assertFalse(attrs.underline())
                );
            }

            @Test
            void getScrollbackAttributes_emptyScrollback_returnsDefaultAttributes() {
                CellAttributes attrs = buf.getScrollbackAttributes(0, 0);
                assertAll(
                        () -> assertEquals(Color.DEFAULT, attrs.fg()),
                        () -> assertEquals(Color.DEFAULT, attrs.bg()),
                        () -> assertFalse(attrs.bold()),
                        () -> assertFalse(attrs.italic()),
                        () -> assertFalse(attrs.underline())
                );
            }

            // --- getScrollbackLine ---

            @Test
            void getScrollbackLine_negativeRow_returnsAllSpaces() {
                assertEquals("     ", buf.getScrollbackLine(-1));
            }

            @Test
            void getScrollbackLine_rowBeyondSize_returnsAllSpaces() {
                assertEquals("     ", buf.getScrollbackLine(100));
            }

            @Test
            void getScrollbackLine_emptyScrollback_returnsAllSpaces() {
                assertEquals("     ", buf.getScrollbackLine(0));
            }
        }

        @Nested
        class GetFullContent {

            @Test
            void getFullContent_noScrollback_equalsBlankScreen() {
                assertEquals("     \n     \n     \n", buf.getFullContent());
            }

            @Test
            void getFullContent_noScrollback_withScreenWrites_matchesExactOutput() {
                buf.setCursor(0, 0);
                buf.writeText("AAA  ");
                buf.setCursor(0, 1);
                buf.writeText("BBB  ");
                buf.setCursor(0, 2);
                buf.writeText("CCC  ");
                assertEquals("AAA  \nBBB  \nCCC  \n", buf.getFullContent());
            }

            @Test
            void getFullContent_withScrollback_scrollbackFirstThenScreen() {
                buf.setCursor(0, 0);
                buf.writeText("SB   ");
                buf.insertEmptyLineAtBottom();
                buf.setCursor(0, 0);
                buf.writeText("SC   ");
                // scrollback: ["SB   "], screen: ["SC   ", "     ", "     "]
                assertEquals("SB   \nSC   \n     \n     \n", buf.getFullContent());
            }

            @Test
            void getFullContent_multipleScrolls_oldestFirstNewestScreenLast() {
                buf.setCursor(0, 0);
                buf.writeText("OLD  ");
                buf.insertEmptyLineAtBottom();
                buf.setCursor(0, 0);
                buf.writeText("MID  ");
                buf.insertEmptyLineAtBottom();
                buf.setCursor(0, 0);
                buf.writeText("SCR  ");
                // scrollback: ["OLD  ", "MID  "], screen: ["SCR  ", "     ", "     "]
                assertEquals("OLD  \nMID  \nSCR  \n     \n     \n", buf.getFullContent());
            }

            @Test
            void getFullContent_everyLineHasTrailingNewline() {
                scrollLines(2);
                String full = buf.getFullContent();
                int expectedNewlines = buf.scrollback.size() + buf.height;
                assertEquals(expectedNewlines, full.chars().filter(c -> c == '\n').count());
                assertTrue(full.endsWith("\n"));
            }
        }
    }

    // -------------------------------------------------------------------------

    @Nested
    class WideCharTest {

        @Nested
        class CursorSnapTest {

            @Test
            void writeText_cursorOnContinuation_snapsLeftToWidePartner() {
                TerminalBuffer buf = new TerminalBuffer(4, 1, 10);
                // WIDE(中)@0, CONT@1 — cursor placed on the CONTINUATION
                Cell wide = new Cell(0x4E2D, Color.DEFAULT, Color.DEFAULT, false, false, false);
                wide.type = CellType.WIDE;
                buf.screen[0].cells[0] = wide;
                buf.screen[0].cells[1] = Cell.continuation();
                buf.setCursor(1, 0);
                buf.writeText("A");
                // snapped to col 0 before write; 'A' overwrites WIDE and orphaned CONT is blanked
                assertAll(
                        () -> assertEquals('A', buf.screen[0].getCell(0).ch),
                        () -> assertEquals(CellType.NORMAL, buf.screen[0].getCell(0).type),
                        () -> assertEquals(CellType.NORMAL, buf.screen[0].getCell(1).type)
                );
            }

            @Test
            void insertText_cursorOnContinuation_snapsLeftToWidePartner() {
                TerminalBuffer buf = new TerminalBuffer(6, 1, 10);
                // WIDE(中)@0, CONT@1, blanks@2-5 — cursor on CONT
                Cell wide = new Cell(0x4E2D, Color.DEFAULT, Color.DEFAULT, false, false, false);
                wide.type = CellType.WIDE;
                buf.screen[0].cells[0] = wide;
                buf.screen[0].cells[1] = Cell.continuation();
                buf.setCursor(1, 0);
                buf.insertText("A");
                // snapped to col 0; 'A' inserted there, wide pair shifts right
                assertEquals('A', buf.screen[0].getCell(0).ch);
                assertEquals(CellType.WIDE, buf.screen[0].getCell(1).type);
            }

            @Test
            void writeText_continuationAtCol0_doesNotMoveToNegativeCol() {
                // CONTINUATION at col 0 is a corrupted state; snap must clamp at 0, not go to -1
                TerminalBuffer buf = new TerminalBuffer(4, 1, 10);
                buf.screen[0].cells[0] = Cell.continuation();
                buf.setCursor(0, 0);
                assertDoesNotThrow(() -> buf.writeText("A"));
                assertEquals('A', buf.screen[0].getCell(0).ch);
            }
        }

        @Nested
        class WriteTextWideCharTest {

            @Test
            void writeText_wideChar_writesCellPairAndAdvancesCursorByTwo() {
                TerminalBuffer buf = new TerminalBuffer(4, 1, 10);
                buf.writeText("\u4E2D"); // 中
                assertAll(
                        () -> assertEquals(CellType.WIDE, buf.screen[0].getCell(0).type),
                        () -> assertEquals(0x4E2D, buf.screen[0].getCell(0).ch),
                        () -> assertEquals(CellType.CONTINUATION, buf.screen[0].getCell(1).type),
                        () -> assertEquals(2, buf.getCursorCol())
                );
            }

            @Test
            void writeText_wideChar_noRoomForPairAtLastColumn_nothingWritten() {
                TerminalBuffer buf = new TerminalBuffer(4, 1, 10);
                buf.setCursor(3, 0);
                buf.writeText("\u4E2D");
                assertAll(
                        () -> assertEquals(CellType.NORMAL, buf.screen[0].getCell(3).type),
                        () -> assertEquals(' ', buf.screen[0].getCell(3).ch),
                        () -> assertEquals(3, buf.getCursorCol())
                );
            }

            @Test
            void writeText_wideChar_atSecondToLastColumn_writesPairCursorClampsToEnd() {
                TerminalBuffer buf = new TerminalBuffer(4, 1, 10);
                buf.setCursor(2, 0);
                buf.writeText("\u4E2D");
                assertAll(
                        () -> assertEquals(CellType.WIDE, buf.screen[0].getCell(2).type),
                        () -> assertEquals(CellType.CONTINUATION, buf.screen[0].getCell(3).type),
                        () -> assertEquals(3, buf.getCursorCol())
                );
            }

            @Test
            void writeText_narrowOverWide_blanksOrphanedContinuation() {
                TerminalBuffer buf = new TerminalBuffer(4, 1, 10);
                buf.writeText("\u4E2D"); // WIDE@0, CONT@1
                buf.setCursor(0, 0);
                buf.writeText("A"); // narrow overwrites WIDE; CONT at col 1 must be cleared
                assertAll(
                        () -> assertEquals('A', buf.screen[0].getCell(0).ch),
                        () -> assertEquals(CellType.NORMAL, buf.screen[0].getCell(0).type),
                        () -> assertEquals(' ', buf.screen[0].getCell(1).ch),
                        () -> assertEquals(CellType.NORMAL, buf.screen[0].getCell(1).type)
                );
            }

            @Test
            void writeText_wideChar_adjacentCellIsWide_blanksOrphanedContinuation() {
                // Writing a wide char at col 0 when col 1 already holds a WIDE:
                // the new CONTINUATION will overwrite col 1, leaving col 2 (old CONT) orphaned → must be blanked.
                TerminalBuffer buf = new TerminalBuffer(6, 1, 10);
                // Manually place: blank@0, WIDE(中)@1, CONT@2, blanks@3-5
                Cell wide = new Cell(0x4E2D, Color.DEFAULT, Color.DEFAULT, false, false, false);
                wide.type = CellType.WIDE;
                buf.screen[0].cells[1] = wide;
                buf.screen[0].cells[2] = Cell.continuation();
                buf.setCursor(0, 0);
                buf.writeText("\u5927"); // 大 — writes WIDE@0, CONT@1; col 1 was WIDE so col 2 gets blanked
                assertAll(
                        () -> assertEquals(CellType.WIDE, buf.screen[0].getCell(0).type),
                        () -> assertEquals(CellType.CONTINUATION, buf.screen[0].getCell(1).type),
                        () -> assertEquals(CellType.NORMAL, buf.screen[0].getCell(2).type),
                        () -> assertEquals(' ', buf.screen[0].getCell(2).ch)
                );
            }

            @Test
            void writeText_mixedNarrowAndWide_eachCharAdvancesByItsOwnWidth() {
                TerminalBuffer buf = new TerminalBuffer(6, 1, 10);
                buf.writeText("A\u4E2DB"); // narrow(1) + wide(2) + narrow(1) = cursor at 4
                assertAll(
                        () -> assertEquals('A', buf.screen[0].getCell(0).ch),
                        () -> assertEquals(CellType.WIDE, buf.screen[0].getCell(1).type),
                        () -> assertEquals(CellType.CONTINUATION, buf.screen[0].getCell(2).type),
                        () -> assertEquals('B', buf.screen[0].getCell(3).ch),
                        () -> assertEquals(4, buf.getCursorCol())
                );
            }

            @Test
            void writeText_twoWideChars_fillEntireWidth4Line() {
                TerminalBuffer buf = new TerminalBuffer(4, 1, 10);
                buf.writeText("\u4E2D\u5927"); // 中大 — two pairs exactly fill width=4
                assertAll(
                        () -> assertEquals(CellType.WIDE, buf.screen[0].getCell(0).type),
                        () -> assertEquals(CellType.CONTINUATION, buf.screen[0].getCell(1).type),
                        () -> assertEquals(CellType.WIDE, buf.screen[0].getCell(2).type),
                        () -> assertEquals(CellType.CONTINUATION, buf.screen[0].getCell(3).type),
                        () -> assertEquals(3, buf.getCursorCol())
                );
            }

            @Test
            void writeText_wideChar_appliesCurrentPenAttributes() {
                TerminalBuffer buf = new TerminalBuffer(4, 1, 10);
                buf.setForeground(Color.RED);
                buf.setBackground(Color.BLUE);
                buf.setBold(true);
                buf.writeText("\u4E2D");
                Cell wide = buf.screen[0].getCell(0);
                assertAll(
                        () -> assertEquals(Color.RED,  wide.fg),
                        () -> assertEquals(Color.BLUE, wide.bg),
                        () -> assertTrue(wide.bold),
                        () -> assertEquals(CellType.WIDE, wide.type)
                );
            }
        }

        @Nested
        class InsertTextWideCharTest {

            @Test
            void insertText_wideChar_occupiesTwoSlotsAndAdvancesCursorByTwo() {
                TerminalBuffer buf = new TerminalBuffer(4, 1, 10);
                buf.insertText("\u4E2D");
                assertAll(
                        () -> assertEquals(CellType.WIDE, buf.screen[0].getCell(0).type),
                        () -> assertEquals(CellType.CONTINUATION, buf.screen[0].getCell(1).type),
                        () -> assertEquals(2, buf.getCursorCol())
                );
            }

            @Test
            void insertText_wideChar_onlyOneSlotAvailable_noOp() {
                // cursor at col 3 of width=4 leaves exactly 1 trailing blank — not enough for a wide pair
                TerminalBuffer buf = new TerminalBuffer(4, 1, 10);
                buf.setCursor(3, 0);
                buf.insertText("\u4E2D");
                assertAll(
                        () -> assertEquals(CellType.NORMAL, buf.screen[0].getCell(3).type),
                        () -> assertEquals(' ', buf.screen[0].getCell(3).ch)
                );
            }

            @Test
            void insertText_mixedNarrowAndWide_writesInInputOrder() {
                TerminalBuffer buf = new TerminalBuffer(6, 1, 10);
                buf.insertText("A\u4E2D"); // 'A'(1 slot) + 中(2 slots) = 3 slots consumed
                assertAll(
                        () -> assertEquals('A', buf.screen[0].getCell(0).ch),
                        () -> assertEquals(CellType.WIDE, buf.screen[0].getCell(1).type),
                        () -> assertEquals(CellType.CONTINUATION, buf.screen[0].getCell(2).type),
                        () -> assertEquals(3, buf.getCursorCol())
                );
            }

            @Test
            void insertText_wideChar_shiftsExistingContentRightByTwoSlots() {
                TerminalBuffer buf = new TerminalBuffer(6, 1, 10);
                buf.writeText("A");
                buf.setCursor(0, 0);
                buf.insertText("\u4E2D"); // 中 inserted at col 0; 'A' shifts to col 2
                assertAll(
                        () -> assertEquals(CellType.WIDE, buf.screen[0].getCell(0).type),
                        () -> assertEquals(CellType.CONTINUATION, buf.screen[0].getCell(1).type),
                        () -> assertEquals('A', buf.screen[0].getCell(2).ch),
                        () -> assertEquals(CellType.NORMAL, buf.screen[0].getCell(2).type)
                );
            }

            @Test
            void insertText_crossRowGuard_preventsWideSplitAtRowBoundary() {
                // width=3, height=2: WIDE(中)@(0,0), CONT@(0,1), blanks everywhere else.
                // Inserting "AB" (insertCount=2) would shift WIDE from flat 0 to flat 2 = (0, col 2 = width-1).
                // Its CONTINUATION would land on (1,0) — a cross-row split. Guard must reduce insertCount to 1.
                TerminalBuffer buf = new TerminalBuffer(3, 2, 10);
                Cell wide = new Cell(0x4E2D, Color.DEFAULT, Color.DEFAULT, false, false, false);
                wide.type = CellType.WIDE;
                buf.screen[0].cells[0] = wide;
                buf.screen[0].cells[1] = Cell.continuation();
                // cursor at (0,0); only "A" can be inserted (guard shrinks budget from 2 to 1)
                buf.insertText("AB");
                assertAll(
                        () -> assertEquals('A', buf.screen[0].getCell(0).ch),
                        () -> assertEquals(CellType.WIDE,  buf.screen[0].getCell(1).type), // shifted by 1, stays in row 0
                        () -> assertEquals(CellType.CONTINUATION, buf.screen[0].getCell(2).type), // still in row 0
                        () -> assertEquals(CellType.NORMAL, buf.screen[1].getCell(0).type)  // no split to row 1
                );
            }
        }

        @Nested
        class FillLineWideCharTest {

            @Test
            void fillLine_wideChar_evenWidth_fillsPairs() {
                TerminalBuffer buf = new TerminalBuffer(4, 1, 10);
                buf.fillLine(0x4E2D); // 中 — wide
                assertAll(
                        () -> assertEquals(CellType.WIDE, buf.screen[0].getCell(0).type),
                        () -> assertEquals(0x4E2D, buf.screen[0].getCell(0).ch),
                        () -> assertEquals(CellType.CONTINUATION, buf.screen[0].getCell(1).type),
                        () -> assertEquals(CellType.WIDE, buf.screen[0].getCell(2).type),
                        () -> assertEquals(0x4E2D, buf.screen[0].getCell(2).ch),
                        () -> assertEquals(CellType.CONTINUATION, buf.screen[0].getCell(3).type)
                );
            }

            @Test
            void fillLine_wideChar_oddWidth_lastColumnIsNormalSpace() {
                // width=5: two complete pairs at cols 0-3; col 4 cannot form a pair so gets a normal space
                TerminalBuffer buf = new TerminalBuffer(5, 1, 10);
                buf.fillLine(0x4E2D);
                assertAll(
                        () -> assertEquals(CellType.WIDE, buf.screen[0].getCell(0).type),
                        () -> assertEquals(CellType.CONTINUATION, buf.screen[0].getCell(1).type),
                        () -> assertEquals(CellType.WIDE, buf.screen[0].getCell(2).type),
                        () -> assertEquals(CellType.CONTINUATION, buf.screen[0].getCell(3).type),
                        () -> assertEquals(CellType.NORMAL, buf.screen[0].getCell(4).type),
                        () -> assertEquals(' ', buf.screen[0].getCell(4).ch)
                );
            }

            @Test
            void fillLine_wideChar_appliesCurrentPenAttributes() {
                TerminalBuffer buf = new TerminalBuffer(4, 1, 10);
                buf.setForeground(Color.RED);
                buf.setBold(true);
                buf.fillLine(0x4E2D);
                Cell wide = buf.screen[0].getCell(0);
                assertAll(
                        () -> assertEquals(Color.RED, wide.fg),
                        () -> assertTrue(wide.bold),
                        () -> assertEquals(CellType.WIDE, wide.type)
                );
            }

            @Test
            void fillLine_wideChar_doesNotMoveCursor() {
                TerminalBuffer buf = new TerminalBuffer(4, 2, 10);
                buf.setCursor(1, 1);
                buf.fillLine(0x4E2D);
                assertAll(
                        () -> assertEquals(1, buf.getCursorCol()),
                        () -> assertEquals(1, buf.getCursorRow())
                );
            }
        }

        @Nested
        class ContentAccessWideCharTest {

            @Test
            void getScreenChar_wideCell_returnsCodePoint() {
                TerminalBuffer buf = new TerminalBuffer(4, 1, 10);
                buf.writeText("\u4E2D");
                assertEquals(0x4E2D, buf.getScreenChar(0, 0));
            }

            @Test
            void getScreenChar_continuationCell_returnsWidePartnerCodePoint() {
                TerminalBuffer buf = new TerminalBuffer(4, 1, 10);
                buf.writeText("\u4E2D"); // WIDE@0, CONT@1
                assertEquals(0x4E2D, buf.getScreenChar(1, 0));
            }

            @Test
            void getScreenAttributes_continuationCell_returnsWidePartnerAttributes() {
                TerminalBuffer buf = new TerminalBuffer(4, 1, 10);
                buf.setForeground(Color.GREEN);
                buf.setBold(true);
                buf.writeText("\u4E2D"); // WIDE@0, CONT@1
                CellAttributes attrs = buf.getScreenAttributes(1, 0); // query CONT col
                assertAll(
                        () -> assertEquals(Color.GREEN, attrs.fg()),
                        () -> assertTrue(attrs.bold())
                );
            }

            @Test
            void getScrollbackChar_continuationCell_returnsWidePartnerCodePoint() {
                TerminalBuffer buf = new TerminalBuffer(4, 2, 10);
                buf.writeText("\u4E2D"); // WIDE@0, CONT@1
                buf.insertEmptyLineAtBottom();
                assertEquals(0x4E2D, buf.getScrollbackChar(1, 0));
            }

            @Test
            void getScrollbackAttributes_continuationCell_returnsWidePartnerAttributes() {
                TerminalBuffer buf = new TerminalBuffer(4, 2, 10);
                buf.setForeground(Color.BLUE);
                buf.writeText("\u4E2D"); // WIDE@0, CONT@1
                buf.insertEmptyLineAtBottom();
                CellAttributes attrs = buf.getScrollbackAttributes(1, 0); // query CONT col
                assertEquals(Color.BLUE, attrs.fg());
            }

            @Test
            void getScreenLine_wideChar_continuationCellNotInOutput() {
                TerminalBuffer buf = new TerminalBuffer(4, 1, 10);
                buf.writeText("\u4E2D"); // WIDE@0, CONT@1, blanks@2-3
                String line = buf.getScreenLine(0);
                // CONTINUATION skipped: "中" + 2 spaces = 3 chars, not 4
                assertEquals("中  ", line);
            }

            @Test
            void getScrollbackLine_wideChar_continuationCellNotInOutput() {
                TerminalBuffer buf = new TerminalBuffer(4, 2, 10);
                buf.writeText("\u4E2D"); // WIDE@0, CONT@1, blanks@2-3
                buf.insertEmptyLineAtBottom();
                assertEquals("中  ", buf.getScrollbackLine(0));
            }

            @Test
            void getScreenContent_wideChars_outputLengthReflectsVisibleChars() {
                TerminalBuffer buf = new TerminalBuffer(4, 2, 10);
                buf.writeText("\u4E2D\u5927"); // 中大 — fills row 0 (4 cols, 2 wide chars)
                // Each wide char produces 1 output char; 2 trailing spaces in blank row 1
                assertEquals("中大\n    \n", buf.getScreenContent());
            }

            @Test
            void getFullContent_wideCharsInScrollbackAndScreen_correctOrder() {
                TerminalBuffer buf = new TerminalBuffer(4, 2, 10);
                buf.writeText("\u4E2D\u5927"); // 中大 fills row 0; cursor ends at col 3
                buf.insertEmptyLineAtBottom(); // 中大 → scrollback[0]; cursor stays at (0,3)
                buf.setCursor(0, 0);
                buf.writeText("\u5927\u4E2D"); // 大中 fills new row 0
                buf.insertEmptyLineAtBottom(); // 大中 → scrollback[1]
                assertEquals("中大\n大中\n    \n    \n", buf.getFullContent());
            }
        }
    }

    // -------------------------------------------------------------------------

    @Nested
    class ResizeTest {

        @Nested
        class HeightIncreaseTest {

            @Test
            void resize_heightIncrease_addsBlankLinesAtBottom() {
                TerminalBuffer buf = new TerminalBuffer(5, 2, 10);
                buf.resize(5, 4);
                assertEquals(4, buf.screen.length);
                assertEquals("     ", buf.getScreenLine(2));
                assertEquals("     ", buf.getScreenLine(3));
            }

            @Test
            void resize_heightIncrease_preservesExistingContent() {
                TerminalBuffer buf = new TerminalBuffer(5, 2, 10);
                buf.writeText("Hello");
                buf.resize(5, 4);
                assertEquals("Hello", buf.getScreenLine(0));
                assertEquals("     ", buf.getScreenLine(1));
                assertEquals("     ", buf.getScreenLine(2));
                assertEquals("     ", buf.getScreenLine(3));

            }

            @Test
            void resize_heightIncrease_cursorUnchanged() {
                TerminalBuffer buf = new TerminalBuffer(5, 2, 10);
                buf.setCursor(2, 1);
                buf.resize(5, 4);
                assertAll(
                        () -> assertEquals(2, buf.getCursorCol()),
                        () -> assertEquals(1, buf.getCursorRow())
                );
            }
        }

        @Nested
        class HeightDecreaseTest {

            @Test
            void resize_heightDecrease_pushesTopRowsToScrollback() {
                TerminalBuffer buf = new TerminalBuffer(5, 4, 10);
                buf.writeText("Row0");
                buf.setCursor(0, 1);
                buf.writeText("Row1");
                buf.resize(5, 2);
                assertAll(
                        () -> assertEquals(2, buf.scrollback.size()),
                        () -> assertEquals("Row0 ", buf.getScrollbackLine(0)),
                        () -> assertEquals("Row1 ", buf.getScrollbackLine(1))
                );
            }

            @Test
            void resize_heightDecrease_evictsScrollbackWhenOverMax() {
                // maxScrollback=2; shrinking by 3 adds 3 lines, evicting oldest
                TerminalBuffer buf = new TerminalBuffer(5, 3, 2);
                buf.writeText("Row0");
                buf.setCursor(0, 1);
                buf.writeText("Row1");
                buf.setCursor(0, 2);
                buf.writeText("Row2");
                buf.resize(5, 1);
                assertAll(
                        () -> assertEquals(2, buf.scrollback.size()),
                        () -> assertEquals("Row0 ", buf.getScrollbackLine(0)),
                        () -> assertEquals("Row1 ", buf.getScrollbackLine(1))
                );
            }

            @Test
            void resize_heightDecrease_cursorRowClampsToNewHeight() {
                TerminalBuffer buf = new TerminalBuffer(5, 4, 10);
                buf.setCursor(0, 3);
                buf.resize(5, 2);
                // cursor was at row 3, delta=2, new row = 3-2=1, within [0,1]
                assertEquals(1, buf.getCursorRow());
            }

            @Test
            void resize_heightDecrease_cursorOnPushedRowLandsAtZero() {
                TerminalBuffer buf = new TerminalBuffer(5, 4, 10);
                buf.setCursor(2, 0);
                buf.resize(5, 2);
                // cursor was at row 0, delta=2, new row = 0-2=-2, clamped to 0
                assertEquals(0, buf.getCursorRow());
            }
        }

        @Nested
        class WidthIncreaseTest {

            @Test
            void resize_widthIncrease_paddedBlanksOnRight() {
                TerminalBuffer buf = new TerminalBuffer(3, 2, 10);
                buf.writeText("ABC");
                buf.resize(5, 2);
                assertEquals("ABC  ", buf.getScreenLine(0));
            }

            @Test
            void resize_widthIncrease_scrollbackAlsoResized() {
                TerminalBuffer buf = new TerminalBuffer(3, 2, 10);
                buf.writeText("ABC");
                buf.insertEmptyLineAtBottom();
                buf.resize(5, 2);
                assertAll(
                        () -> assertEquals(1, buf.scrollback.size()),
                        () -> assertEquals("ABC  ", buf.getScrollbackLine(0))
                );
            }

            @Test
            void resize_widthIncrease_cursorUnchanged() {
                TerminalBuffer buf = new TerminalBuffer(3, 2, 10);
                buf.setCursor(2, 1);
                buf.resize(6, 2);
                assertAll(
                        () -> assertEquals(2, buf.getCursorCol()),
                        () -> assertEquals(1, buf.getCursorRow())
                );
            }
        }

        @Nested
        class WidthDecreaseTest {

            @Test
            void resize_widthDecrease_truncatesContent() {
                TerminalBuffer buf = new TerminalBuffer(6, 2, 10);
                buf.writeText("ABCDEF");
                buf.resize(3, 2);
                assertEquals("ABC", buf.getScreenLine(0));
            }

            @Test
            void resize_widthDecrease_scrollbackAlsoTruncated() {
                TerminalBuffer buf = new TerminalBuffer(6, 2, 10);
                buf.writeText("ABCDEF");
                buf.insertEmptyLineAtBottom();
                buf.resize(3, 2);
                assertAll(
                        () -> assertEquals(1, buf.scrollback.size()),
                        () -> assertEquals("ABC", buf.getScrollbackLine(0))
                );
            }

            @Test
            void resize_widthDecrease_cursorColClamped() {
                TerminalBuffer buf = new TerminalBuffer(6, 2, 10);
                buf.setCursor(5, 0);
                buf.resize(3, 2);
                assertEquals(2, buf.getCursorCol());
            }

            @Test
            void resize_widthDecrease_wideCharAtBoundaryBlanked() {
                // width=4: write 中(WIDE@0,CONT@1) 大(WIDE@2,CONT@3); shrink to 3 → col 2 is WIDE, blank it
                TerminalBuffer buf = new TerminalBuffer(4, 1, 10);
                buf.writeText("\u4E2D\u5927"); // 中大
                buf.resize(3, 1);
                assertAll(
                        () -> assertEquals(CellType.NORMAL, buf.screen[0].getCell(2).type),
                        () -> assertEquals(' ', buf.screen[0].getCell(2).ch)
                );
            }

            @Test
            void resize_widthDecrease_continuationAtBoundary_pairIntact() {
                // width=6: WIDE@0,CONT@1,WIDE@2,CONT@3,blank@4,blank@5; shrink to 4
                // → col 3 is CONTINUATION, col 2 is its WIDE partner — both within bounds, pair is valid
                TerminalBuffer buf = new TerminalBuffer(6, 1, 10);
                buf.writeText("\u4E2D\u5927"); // 中大
                buf.resize(4, 1);
                assertAll(
                        () -> assertEquals(CellType.WIDE, buf.screen[0].getCell(2).type),
                        () -> assertEquals(0x5927, buf.screen[0].getCell(2).ch),
                        () -> assertEquals(CellType.CONTINUATION, buf.screen[0].getCell(3).type)
                );
            }
        }

        @Nested
        class NoOpTest {

            @Test
            void resize_sameDimensions_noChange() {
                TerminalBuffer buf = new TerminalBuffer(5, 3, 10);
                buf.writeText("Hello");
                buf.setCursor(3, 2);
                Line[] screenBefore = buf.screen;
                buf.resize(5, 3);
                assertAll(
                        () -> assertSame(screenBefore, buf.screen),
                        () -> assertEquals("Hello", buf.getScreenLine(0)),
                        () -> assertEquals(3, buf.getCursorCol()),
                        () -> assertEquals(2, buf.getCursorRow())
                );
            }
        }

        @Nested
        class CombinedTest {

            @Test
            void resize_heightAndWidthBothChange_correctResult() {
                TerminalBuffer buf = new TerminalBuffer(6, 4, 10);
                buf.writeText("ABCDEF");
                buf.setCursor(0, 1);
                buf.writeText("GHIJKL");
                buf.setCursor(0, 2);
                buf.writeText("MNOPQR");
                buf.setCursor(5, 3);
                // shrink: height 4→2 (rows 0,1 pushed to scrollback), width 6→4 (truncate to 4)
                buf.resize(4, 2);
                assertAll(
                        () -> assertEquals(2, buf.scrollback.size()),
                        () -> assertEquals("ABCD", buf.getScrollbackLine(0)),
                        () -> assertEquals("GHIJ", buf.getScrollbackLine(1)),
                        () -> assertEquals("MNOP", buf.getScreenLine(0)),
                        () -> assertEquals("    ", buf.getScreenLine(1)),
                        // cursor was at (5,3); delta=2 → row=1; col clamped from 5→3
                        () -> assertEquals(3, buf.getCursorCol()),
                        () -> assertEquals(1, buf.getCursorRow())
                );
            }
        }
    }
}
