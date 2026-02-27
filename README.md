# Terminal Text Buffer

A terminal text buffer — the core data structure that terminal emulators use to store and display text. The buffer maintains a grid of character cells, a cursor, and a scrollback history.


## Updated bonus part solution notes

### Wide Characters

`CellType` enum (`NORMAL`, `WIDE`, `CONTINUATION`) is added to `Cell` — the cell grid stays uniform and no existing indexing logic changes. A CONTINUATION cell is a blank cell with a distinguished type; it carries no independent content and is skipped by `Line.toString()`.

**Cursor snap:** any write operation that begins with the cursor on a CONTINUATION moves it left to its WIDE partner first, since WIDE part is defined as the only logical part.

**Orphan cleanup on overwrite:** writing a narrow char over a WIDE cell blanks the CONTINUATION at `col+1`. Writing a wide char checks `col+1` — if it is itself a WIDE, its CONTINUATION at `col+2` is blanked, avoiding corrupt-pair states.

**Wide char at right edge:** if `cursorCol == width-1` when a wide char is to be written, the char is skipped entirely. Half a wide char cannot be rendered.

**Cross-row guard in `insertText`:** a WIDE cell shifted, so its WIDE might land at `col width-1`, its CONTINUATION might land at `col 0` - one pair spanning on two rows. `insertCount` is decremented until no such split exists before the shift runs.

### Resize

`resize(newWidth, newHeight)` is a single method — height and width adjustments share the line-rebuild step and a single cursor fixup at the end.

**Height decrease:** top rows are pushed into scrollback using the same deep-copy-then-evict rule as `insertEmptyLineAtBottom`. The cursor row shifts up by the same delta and is clamped.

**Height increase:** blank lines are appended at the bottom. No content or cursor changes.

**Width change (both directions):** every screen and scrollback line is rebuilt at the new width. Scrollback is rebuilt too — keeping all lines at a uniform width means content-access methods are consistent.

**Width decrease boundary fix:** after copying `[0, newWidth-1]`, only one corrupt state is possible — a WIDE cell at `newWidth-1` whose CONTINUATION was truncated. That cell is blanked.

**Cursor fixup:** `setCursor(cursorCol, cursorRow)` clamps both axes to the new dimensions, then `snapCursorOffContinuation()` handles the case where a width shrink left the cursor on a CONTINUATION cell.


## Structure

```
src/
├── main/java/com/zxuhan/
│   ├── Cell.java              # Single mutable grid cell (code point + colors + style flags + CellType)
│   ├── CellType.java          # Enum: NORMAL, WIDE, CONTINUATION — wide character cell classification
│   ├── Line.java              # Row of cells with deep-copy support
│   ├── Color.java             # 17-value enum: DEFAULT + 16 standard terminal colors
│   ├── CellAttributes.java    # Immutable record for returning cell style metadata
│   ├── UnicodeUtils.java      # Static wide-character detection (isWide)
│   └── TerminalBuffer.java    # Main buffer: screen, scrollback, cursor, editing, resize
└── test/java/com/zxuhan/
    ├── CellTest.java          # Unit tests for Cell: blank(), copy(), constructor edge cases
    ├── LineTest.java          # Unit tests for Line: getCell/setCell, copy(), toString()
    ├── UnicodeUtilsTest.java  # Unit tests for UnicodeUtils.isWide()
    └── TerminalBufferTest.java # Integration tests for cursor, attributes, editing, content access, wide chars, resize
```

The buffer has two logical parts:
- **Screen** — a fixed `height × width` grid, the editable visible area
- **Scrollback** — lines that have scrolled off the top, read-only, bounded by `maxScrollback`

## Build & Test

Requires JDK 17+ and uses the Gradle wrapper.

```bash
# Run all tests
./gradlew test

# Build
./gradlew build
```
