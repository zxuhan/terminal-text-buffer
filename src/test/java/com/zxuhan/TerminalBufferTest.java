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

    }

    // -------------------------------------------------------------------------

    @Nested
    class ScreenOperationsTest {

        // Helper: write a distinguishable character string into row 0 col 0
        private void writeToRow(TerminalBuffer buf, int row, String text) {
            buf.setCursor(0, row);
            for (int i = 0; i < text.length() && i < buf.width; i++) {
                buf.screen[row].getCell(i).ch = text.codePointAt(i);
            }
        }

        private String rowContent(TerminalBuffer buf, int row) {
            return buf.screen[row].toString();
        }

        @Nested
        class InsertEmptyLineAtBottomTest {

            @Test
            void insertEmptyLineAtBottom_nonFullScrollback_pushesOldestScreenRowToScrollback() {
                TerminalBuffer buf = new TerminalBuffer(5, 3, 10);
                writeToRow(buf, 0, "ABC  ");

                buf.insertEmptyLineAtBottom();

                assertEquals(1, buf.scrollback.size());
                assertEquals("ABC  ", buf.scrollback.get(0).toString());
            }

            @Test
            void insertEmptyLineAtBottom_nonFullScrollback_shiftsScreenLinesUp() {
                TerminalBuffer buf = new TerminalBuffer(5, 3, 10);
                writeToRow(buf, 0, "AAA  ");
                writeToRow(buf, 1, "BBB  ");
                writeToRow(buf, 2, "CCC  ");

                buf.insertEmptyLineAtBottom();

                assertEquals("BBB  ", rowContent(buf, 0));
                assertEquals("CCC  ", rowContent(buf, 1));
            }

            @Test
            void insertEmptyLineAtBottom_anyCall_appendsBlankLineAtBottom() {
                TerminalBuffer buf = new TerminalBuffer(5, 3, 10);
                writeToRow(buf, 0, "AAA  ");

                buf.insertEmptyLineAtBottom();

                assertEquals("     ", rowContent(buf, 2));
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
                writeToRow(buf, 0, "L1   ");
                buf.insertEmptyLineAtBottom(); // scrollback: [L1]
                writeToRow(buf, 0, "L2   ");
                buf.insertEmptyLineAtBottom(); // scrollback: [L1, L2]
                writeToRow(buf, 0, "L3   ");
                buf.insertEmptyLineAtBottom(); // scrollback should be [L2, L3]

                assertEquals(2, buf.scrollback.size());
                assertEquals("L2   ", buf.scrollback.get(0).toString());
                assertEquals("L3   ", buf.scrollback.get(1).toString());
            }

            @Test
            void insertEmptyLineAtBottom_scrollbackSnapshot_isolatedFromSubsequentScreenEdits() {
                TerminalBuffer buf = new TerminalBuffer(5, 3, 10);
                writeToRow(buf, 0, "ORIG ");

                buf.insertEmptyLineAtBottom();

                // Mutate what is now screen[0] (was screen[1] before shift)
                buf.screen[0].getCell(0).ch = 'X';

                assertEquals("ORIG ", buf.scrollback.get(0).toString());
            }

            @Test
            void insertEmptyLineAtBottom_multipleInserts_scrollbackOrderIsOldestFirst() {
                TerminalBuffer buf = new TerminalBuffer(5, 2, 10);
                writeToRow(buf, 0, "FIRST");
                buf.insertEmptyLineAtBottom();
                writeToRow(buf, 0, "SECND");
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
                writeToRow(buf, 0, "ABCD");
                writeToRow(buf, 1, "EFGH");
                writeToRow(buf, 2, "IJKL");

                buf.clearScreen();

                for (int r = 0; r < buf.height; r++) {
                    assertEquals("    ", rowContent(buf, r));
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
                writeToRow(buf, 0, "HIST ");
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
                writeToRow(buf, 0, "ABCD");
                writeToRow(buf, 1, "EFGH");

                buf.clearScreenAndScrollback();

                for (int r = 0; r < buf.height; r++) {
                    assertEquals("    ", rowContent(buf, r));
                }
            }

            @Test
            void clearScreenAndScrollback_withScrollback_scrollbackBecomesEmpty() {
                TerminalBuffer buf = new TerminalBuffer(5, 2, 10);
                writeToRow(buf, 0, "LINE1");
                buf.insertEmptyLineAtBottom();
                writeToRow(buf, 0, "LINE2");
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

        // Helper: write a distinguishable character string at cursor row
        private void writeToRow(TerminalBuffer buf, int row, String text) {
            buf.setCursor(0, row);
            for (int i = 0; i < text.length() && i < buf.width; i++) {
                buf.screen[row].getCell(i).ch = text.codePointAt(i);
            }
        }

        private String rowContent(TerminalBuffer buf, int row) {
            return buf.screen[row].toString();
        }


        private void scrollLines(int n) {
            for (int i = 0; i < n; i++) {
                writeToRow(buf, 0, "L" + i + "   ");
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
                writeToRow(buf, 1, "  X  ");
                assertEquals('X', buf.getScreenChar(2, 1));
            }

            @Test
            void getScreenChar_unwrittenCellAdjacentToWrite_remainsSpace() {
                writeToRow(buf, 1, "  X  ");
                assertEquals(' ', buf.getScreenChar(3, 1));
            }

            @Test
            void getScreenChar_afterScroll_reflectsShiftedContent() {
                writeToRow(buf, 0, "TOP  ");
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
                writeToRow(buf, 1, "Hi");
                assertEquals("Hi   ", buf.getScreenLine(1));
            }

            @Test
            void getScreenLine_lastRow_returnsCorrectContent() {
                writeToRow(buf, 2, "End  ");
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
                writeToRow(buf, 0, "AAA  ");
                writeToRow(buf, 1, "BBB  ");
                writeToRow(buf, 2, "CCC  ");
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
                writeToRow(buf, 0, "HELLO");
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
                writeToRow(buf, 0, "ORIG ");
                buf.insertEmptyLineAtBottom();
                // Overwrite screen row 0 (now the line that was screen[1])
                writeToRow(buf, 0, "MUTD ");

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
                writeToRow(buf, 0, "ROW0 ");
                buf.insertEmptyLineAtBottom();

                assertEquals("ROW0 ", buf.getScrollbackLine(0));
            }

            @Test
            void getScrollbackLine_multipleScrolls_indexMatchesScrollOrder() {
                writeToRow(buf, 0, "FIRST");
                buf.insertEmptyLineAtBottom();
                writeToRow(buf, 0, "SECND");
                buf.insertEmptyLineAtBottom();

                assertEquals("FIRST", buf.getScrollbackLine(0));
                assertEquals("SECND", buf.getScrollbackLine(1));
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
                writeToRow(buf, 0, "AAA  ");
                writeToRow(buf, 1, "BBB  ");
                writeToRow(buf, 2, "CCC  ");
                assertEquals("AAA  \nBBB  \nCCC  \n", buf.getFullContent());
            }

            @Test
            void getFullContent_withScrollback_scrollbackFirstThenScreen() {
                writeToRow(buf, 0, "SB   ");
                buf.insertEmptyLineAtBottom();
                writeToRow(buf, 0, "SC   ");
                // scrollback: ["SB   "], screen: ["SC   ", "     ", "     "]
                assertEquals("SB   \nSC   \n     \n     \n", buf.getFullContent());
            }

            @Test
            void getFullContent_multipleScrolls_oldestFirstNewestScreenLast() {
                writeToRow(buf, 0, "OLD  ");
                buf.insertEmptyLineAtBottom();
                writeToRow(buf, 0, "MID  ");
                buf.insertEmptyLineAtBottom();
                writeToRow(buf, 0, "SCR  ");
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
}