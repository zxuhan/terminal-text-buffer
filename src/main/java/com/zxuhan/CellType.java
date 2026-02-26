package com.zxuhan;

/**
 * Describes how a {@link Cell} occupies terminal columns.
 * Wide characters (CJK, emoji, fullwidth) span two columns: the left column holds WIDE,
 * the right holds CONTINUATION as a placeholder that is skipped during rendering.
 */
enum CellType { NORMAL, WIDE, CONTINUATION }
