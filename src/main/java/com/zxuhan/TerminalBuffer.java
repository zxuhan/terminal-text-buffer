package com.zxuhan;

import java.util.ArrayList;

public class TerminalBuffer {

    // Buffer configuration
    int width;
    int height;
    Line[] screen;
    ArrayList<Line> scrollback;
    int maxScrollback;

    // Cursor position
    int cursorCol;
    int cursorRow;

    // Current cell attributes
    Color currentFg;
    Color currentBg;
    boolean currentBold;
    boolean currentItalic;
    boolean currentUnderline;

    TerminalBuffer(int width, int height, int maxScrollback) {
        this.width = width;
        this.height = height;
        this.maxScrollback = maxScrollback;

        screen = new Line[height];
        for (int i = 0; i < height; i++) {
            screen[i] = new Line(width);
        }

        scrollback = new ArrayList<>();

        cursorCol = 0;
        cursorRow = 0;

        currentFg = Color.DEFAULT;
        currentBg = Color.DEFAULT;
        currentBold = false;
        currentItalic = false;
        currentUnderline = false;
    }
}