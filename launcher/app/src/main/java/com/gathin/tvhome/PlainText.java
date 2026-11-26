package com.gathin.tvhome;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * With the One Piece skin off, text has one colour: plain white. The dimmed whites and light greys (the greeting,
 * "Continue watching", secondary lines) turn full white; a focused row's dark text on its white face stays as it is,
 * and coloured text (an accent, a warning) is left alone.
 */
final class PlainText {

    private static final int[] FOCUSED = {android.R.attr.state_focused};
    private static final int[][] STATES = {FOCUSED, {}};

    private PlainText() { }

    /** Whitens root's text now and again whenever its views are laid out, so rows added later follow. */
    static void keepWhite(View root) {
        whiten(root);
        root.getViewTreeObserver().addOnGlobalLayoutListener(() -> whiten(root));
    }

    static void whiten(View v) {
        if (v instanceof TextView) {
            TextView t = (TextView) v;
            ColorStateList colors = t.getTextColors();
            int rest = colors.getDefaultColor();
            if (rest != Color.WHITE && light(rest)) {
                if (colors.isStateful()) {
                    int focused = colors.getColorForState(FOCUSED, rest);
                    t.setTextColor(new ColorStateList(STATES, new int[]{light(focused) ? Color.WHITE : focused, Color.WHITE}));
                } else {
                    t.setTextColor(Color.WHITE);
                }
            }
        }
        if (v instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) v;
            for (int i = 0; i < g.getChildCount(); i++) whiten(g.getChildAt(i));
        }
    }

    /** White or a light grey, at any opacity. */
    private static boolean light(int c) {
        return Color.red(c) >= 0xA0 && Color.green(c) >= 0xA0 && Color.blue(c) >= 0xA0
                && Math.abs(Color.red(c) - Color.blue(c)) < 0x30;
    }
}
