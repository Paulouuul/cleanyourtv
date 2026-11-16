package com.gathin.tvhome;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * A One Piece look for Home, on when a theme's config.json sets {@code "skin": "one_piece"}: the header's text gets a
 * deep-sea outline so it reads over a bright sky wallpaper, and the top-bar pills turn sea blue with a sun-gold
 * focus. Off by default; without the key nothing here runs.
 */
final class OnePieceSkin {
    static final String NAME = "one_piece";

    /** Deep sea blue: the outline and shadow under white text on a bright wallpaper. */
    static final int INK = 0xFF0B2A4A;
    /** Sun gold: focus and highlights. */
    static final int GOLD = 0xFFFFCD00;

    private OnePieceSkin() { }

    static boolean on(Config cfg) {
        return cfg != null && NAME.equals(cfg.skin);
    }

    /** Header text and top-bar pills; called once Home's views are set up. */
    static void applyHome(Activity home) {
        float d = home.getResources().getDisplayMetrics().density;
        int[] texts = {R.id.greeting, R.id.clock, R.id.date, R.id.shamsiDate, R.id.lunarDate};
        for (int id : texts) {
            View v = home.findViewById(id);
            if (v instanceof TextView) outline((TextView) v, d);
        }
        // Every TextView in the clock block (AM/PM and the like) as well as the named ones.
        View block = home.findViewById(R.id.leftBlock);
        if (block instanceof ViewGroup) outlineAll((ViewGroup) block, d);
        // Bangers, a hand-lettered comic face, for the clock, the date and the greeting.
        android.graphics.Typeface comic = comic(home);
        for (int id : new int[]{R.id.greeting, R.id.clock, R.id.date}) {
            View v = home.findViewById(id);
            if (v instanceof TextView) ((TextView) v).setTypeface(comic);
        }
        // Bangers is narrow: the clock (52sp, -0.04 spacing for the thin default face) gets more size and air.
        View clock = home.findViewById(R.id.clock);
        if (clock instanceof TextView) {
            TextView c = (TextView) clock;
            c.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 64);
            c.setLetterSpacing(0.03f);
            // Bangers leans right: the last digit's top and the shadow overhang the text's width and were clipped.
            c.setPaddingRelative(c.getPaddingStart(), c.getPaddingTop(), c.getPaddingEnd() + Math.round(10 * d),
                    c.getPaddingBottom());
        }
        View bar = home.findViewById(R.id.topButtons);
        if (bar instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) bar;
            for (int i = 0; i < g.getChildCount(); i++) {
                View pill = g.getChildAt(i);
                if (pill.getBackground() != null) pill.setBackgroundResource(R.drawable.pill_bg_onepiece);
            }
        }
    }

    /** Bangers (OFL): clock, date, greeting and the speech bubble. */
    static android.graphics.Typeface comic(android.content.Context ctx) {
        return ctx.getResources().getFont(R.font.bangers);
    }

    /** Pirata One (OFL): section titles, in title case (its capitals are hard to read in a row). */
    static void pirateTitle(TextView t) {
        t.setTypeface(t.getResources().getFont(R.font.pirata_one));
        t.setAllCaps(false);
        t.setLetterSpacing(0.02f);
    }

    /** White text with a soft deep-sea shadow under it, like a cartoon's ink line, readable over sky and sea. */
    static void outline(TextView t, float density) {
        t.setTextColor(0xFFFFFFFF);
        t.setShadowLayer(4f * density, 0f, 1.5f * density, 0xE60B2A4A);
    }

    // ---- sounds ----

    private static android.media.SoundPool sounds;
    private static int stretch, denDen, shishishi;
    /** The top-bar icons' focus sounds, by OnePieceIcon kind: chime, ring, coins, wooden ratchet. */
    private static final int[] focusSounds = new int[4];

    /** Loads the click sounds once for the process; every screen of the skin calls it, only the first loads. */
    static void loadSounds(android.content.Context ctx) {
        if (sounds != null) return;
        sounds = new android.media.SoundPool.Builder().setMaxStreams(2).setAudioAttributes(
                new android.media.AudioAttributes.Builder()
                        .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                        .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION).build()).build();
        android.content.Context app = ctx.getApplicationContext();
        stretch = sounds.load(app, R.raw.click_stretch, 1);
        denDen = sounds.load(app, R.raw.click_denden, 1);
        shishishi = sounds.load(app, R.raw.click_shishishi, 1);
        focusSounds[OnePieceIcon.LOG_POSE] = sounds.load(app, R.raw.focus_logpose, 1);
        focusSounds[OnePieceIcon.DEN_DEN] = sounds.load(app, R.raw.focus_denden, 1);
        focusSounds[OnePieceIcon.CHEST] = sounds.load(app, R.raw.focus_chest, 1);
        focusSounds[OnePieceIcon.WHEEL] = sounds.load(app, R.raw.focus_wheel, 1);
    }

    private static void play(int id) {
        if (sounds != null && id != 0) sounds.play(id, 0.6f, 0.6f, 1, 0, 1f);
    }

    /** A top-bar icon takes focus: its own short sound. */
    static void focus(int kind) {
        if (kind >= 0 && kind < focusSounds.length) play(focusSounds[kind]);
    }

    /** An app or a Continue watching card opens: Luffy's rubber stretch. */
    static void clickOpen() { play(stretch); }

    /** Settings and the network: the Den Den Mushi's ring. */
    static void clickSettings() { play(denDen); }

    /** The Apps screen opens: Luffy's laugh. */
    static void clickApps() { play(shishishi); }

    // ---- frames and panels ----

    /** Wood brown of the tiles' frames and the parchment's edge. */
    static final int WOOD = 0xFF6B3F1D;
    /** Parchment: the panels and the Continue watching cards' "wanted poster" paper. */
    static final int PAPER = 0xFFF3E0B0, PAPER_DARK = 0xFFE4C788, PAPER_INK = 0xFF3B2412;

    /**
     * A wooden frame drawn over a tile: a dark outer board and a light inner bevel, its corners following the tile's
     * own (AppAdapter.cornerRadius at the tile's width) whatever size the tile is.
     */
    static android.graphics.drawable.Drawable woodFrame(float density) {
        return new Frame(density, false, -1f);
    }

    /** The wooden frame for a tile whose corners have a fixed radius (the Apps screen's). */
    static android.graphics.drawable.Drawable woodFrame(float density, float radiusPx) {
        return new Frame(density, false, radiusPx);
    }

    /** A gold rope round the focused tile: gold with darker twists; its corners at a fixed radius. */
    static android.graphics.drawable.Drawable rope(float density, float radiusPx) {
        return new Frame(density, true, radiusPx);
    }

    private static final class Frame extends android.graphics.drawable.Drawable {
        private final android.graphics.Paint a = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
        private final android.graphics.Paint b = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
        private final android.graphics.RectF r = new android.graphics.RectF();
        private final float d;
        private final boolean rope;
        /** The corners' radius, or below 0 for AppAdapter.cornerRadius at the frame's width. */
        private final float fixedRadius;

        Frame(float density, boolean rope, float radiusPx) {
            d = density;
            this.rope = rope;
            fixedRadius = radiusPx;
            a.setStyle(android.graphics.Paint.Style.STROKE);
            b.setStyle(android.graphics.Paint.Style.STROKE);
            if (rope) {
                a.setColor(GOLD);
                a.setStrokeWidth(4.5f * d);
                b.setColor(0xFFC8871A);
                b.setStrokeWidth(4.5f * d);
                b.setPathEffect(new android.graphics.DashPathEffect(new float[]{3.5f * d, 4f * d}, 0));
            } else {
                a.setColor(WOOD);
                a.setStrokeWidth(4f * d);
                b.setColor(0xFFC08A50);
                b.setStrokeWidth(1.2f * d);
            }
        }

        @Override
        public void draw(@androidx.annotation.NonNull android.graphics.Canvas c) {
            android.graphics.Rect bounds = getBounds();
            float radius = fixedRadius >= 0f ? fixedRadius : AppAdapter.cornerRadius(bounds.width(), d);
            float half = a.getStrokeWidth() / 2f;
            r.set(bounds.left + half, bounds.top + half, bounds.right - half, bounds.bottom - half);
            float rr = Math.max(0f, radius - half);
            c.drawRoundRect(r, rr, rr, a);
            if (!rope) {
                float in = a.getStrokeWidth() + b.getStrokeWidth() / 2f;
                r.set(bounds.left + in, bounds.top + in, bounds.right - in, bounds.bottom - in);
                rr = Math.max(0f, radius - in);
            }
            c.drawRoundRect(r, rr, rr, b);
        }

        @Override public void setAlpha(int alpha) { }
        @Override public void setColorFilter(android.graphics.ColorFilter cf) { }
        @Override public int getOpacity() { return android.graphics.PixelFormat.TRANSLUCENT; }
    }

    /**
     * A menu panel as a sheet of parchment: paper with a wooden edge, brown text and icons, and a gold row under
     * focus. Every focusable row in it is restyled; call it once the rows are in.
     */
    static void parchment(View panel) {
        float d = panel.getResources().getDisplayMetrics().density;
        android.graphics.drawable.GradientDrawable paper = new android.graphics.drawable.GradientDrawable(
                android.graphics.drawable.GradientDrawable.Orientation.TOP_BOTTOM, new int[]{PAPER, PAPER_DARK});
        paper.setCornerRadius(18 * d);
        paper.setStroke(Math.round(3 * d), WOOD);
        panel.setBackground(paper);
        android.content.res.ColorStateList ink = new android.content.res.ColorStateList(
                new int[][]{{android.R.attr.state_focused}, {}}, new int[]{INK, PAPER_INK});
        if (panel instanceof ViewGroup) parchmentRows((ViewGroup) panel, ink, d);
    }

    private static void parchmentRows(ViewGroup g, android.content.res.ColorStateList ink, float d) {
        for (int i = 0; i < g.getChildCount(); i++) {
            View c = g.getChildAt(i);
            if (c.isFocusable()) {
                android.graphics.drawable.StateListDrawable bg = new android.graphics.drawable.StateListDrawable();
                android.graphics.drawable.GradientDrawable on = new android.graphics.drawable.GradientDrawable();
                on.setColor(GOLD);
                on.setCornerRadius(13 * d);
                on.setStroke(Math.round(2 * d), INK);
                bg.addState(new int[]{android.R.attr.state_focused}, on);
                bg.addState(new int[]{}, new android.graphics.drawable.ColorDrawable(0));
                c.setBackground(bg);
            }
            if (c instanceof TextView) {
                ((TextView) c).setTextColor(ink);
                ((TextView) c).setCompoundDrawableTintList(ink);
            } else if (c instanceof android.widget.ImageView) {
                ((android.widget.ImageView) c).setImageTintList(ink);
            } else if (c instanceof ViewGroup) {
                parchmentRows((ViewGroup) c, ink, d);
            }
        }
    }

    private static void outlineAll(ViewGroup g, float density) {
        for (int i = 0; i < g.getChildCount(); i++) {
            View c = g.getChildAt(i);
            if (c instanceof TextView) outline((TextView) c, density);
            else if (c instanceof ViewGroup) outlineAll((ViewGroup) c, density);
        }
    }
}
