package com.gathin.tvhome;

import android.view.View;

/**
 * Where an app's hold menu (Move, Replace, App info...) opens, shared by Home's tiles and the Apps screen so both
 * menus have the same width and the same air round the app: below the tile, clear of what the focused tile grows by
 * plus a gap, or above it when the screen's bottom edge would cut the menu off, and kept inside the screen's edges.
 */
final class TileMenu {
    /** The menu's width, in dp. */
    static final int WIDTH_DP = 220;
    /** The space between the focused (grown) tile and the menu, in dp. */
    static final int GAP_DP = 10;
    /** The least space kept between the menu and the screen's edges, in dp. */
    static final int EDGE_DP = 16;

    private TileMenu() { }

    /**
     * The menu's top-left corner in the window, for a menu {@code width} by {@code height} px beside {@code anchor},
     * a tile scaled by {@code focusScale} round its centre. [2] is 1 when the menu opens above the tile.
     */
    static int[] place(View anchor, View screen, int width, int height, float focusScale, float density) {
        int[] at = new int[2];
        anchor.getLocationInWindow(at);
        int grow = Math.round(anchor.getHeight() * (focusScale - 1f) / 2f);
        int gap = Math.round(GAP_DP * density) + grow, edge = Math.round(EDGE_DP * density);
        int y = at[1] + anchor.getHeight() + gap;
        int above = 0;
        if (y + height > screen.getHeight() - edge) {
            y = at[1] - gap - height;   // no room below: above the tile
            above = 1;
        }
        y = Math.max(edge, Math.min(y, screen.getHeight() - edge - height));
        int growX = Math.round(anchor.getWidth() * (focusScale - 1f) / 2f);
        int x = Math.max(edge, Math.min(at[0] - growX, screen.getWidth() - edge - width));
        return new int[]{x, y, above};
    }
}
