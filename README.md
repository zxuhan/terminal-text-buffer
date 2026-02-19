# Terminal Text Buffer

A terminal text buffer — the core data structure that terminal emulators use to store and display text. The buffer maintains a grid of character cells, a cursor, and a scrollback history.

## Structure

```
src/main/java/com/zxuhan/
├── Cell.java              # Single mutable grid cell (code point + colors + style flags)
├── Line.java              # Row of cells with deep-copy support
├── Color.java             # 17-value enum: DEFAULT + 16 standard terminal colors
├── CellAttributes.java    # Immutable record for returning cell style metadata
└── TerminalBuffer.java    # Main buffer: screen, scrollback, cursor, editing operations
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

Test report: `build/reports/tests/test/index.html`
