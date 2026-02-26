package com.zxuhan;

/** Determines terminal display width of Unicode code points based on East Asian Width ranges. */
class UnicodeUtils {

    private UnicodeUtils() {}

    /** Returns {@code true} if {@code cp} occupies two terminal columns (CJK, emoji, fullwidth). */
    static boolean isWide(int cp) {
        return (cp >= 0x1100  && cp <= 0x115F)
            || (cp >= 0x2E80  && cp <= 0x303E)
            || (cp >= 0x3041  && cp <= 0x33FF)
            || (cp >= 0x3400  && cp <= 0x4DBF)
            || (cp >= 0x4E00  && cp <= 0x9FFF)
            || (cp >= 0xA000  && cp <= 0xA4CF)
            || (cp >= 0xAC00  && cp <= 0xD7AF)
            || (cp >= 0xF900  && cp <= 0xFAFF)
            || (cp >= 0xFE10  && cp <= 0xFE6F)
            || (cp >= 0xFF01  && cp <= 0xFF60)
            || (cp >= 0xFFE0  && cp <= 0xFFE6)
            || (cp >= 0x1B000 && cp <= 0x1B77F)
            || (cp >= 0x1F300 && cp <= 0x1F9FF)
            || (cp >= 0x20000 && cp <= 0x2A6DF)
            || (cp >= 0x2A700 && cp <= 0x2CEAF)
            || (cp >= 0x2CEB0 && cp <= 0x2EBEF)
            || (cp >= 0x30000 && cp <= 0x3134F);
    }
}
