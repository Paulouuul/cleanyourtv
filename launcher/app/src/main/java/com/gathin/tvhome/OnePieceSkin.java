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
                // Each pill its own drawable: the focus move plays on the one that took focus.
                if (pill.getBackground() != null) pill.setBackground(new OnePiecePill(d));
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
    private static int stretch, denDen, gomuGomu, haki, don, alert;
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
        // A sound asked for before it finished loading (the quick panel opened in a fresh process) plays once loaded.
        sounds.setOnLoadCompleteListener((pool, id, status) -> {
            if (status != 0) return;
            loaded.add(id);
            if (id == pending) {
                pending = 0;
                pool.play(id, pendingVolume, pendingVolume, 1, 0, 1f);
            }
        });
        stretch = sounds.load(app, R.raw.click_stretch, 1);
        denDen = sounds.load(app, R.raw.click_denden, 1);
        gomuGomu = sounds.load(app, R.raw.click_gomugomu, 1);
        haki = sounds.load(app, R.raw.click_haki, 1);
        don = sounds.load(app, R.raw.click_don, 1);
        alert = sounds.load(app, R.raw.click_alert, 1);
        focusSounds[OnePieceIcon.LOG_POSE] = sounds.load(app, R.raw.focus_logpose, 1);
        focusSounds[OnePieceIcon.DEN_DEN] = sounds.load(app, R.raw.focus_denden, 1);
        focusSounds[OnePieceIcon.CHEST] = sounds.load(app, R.raw.focus_chest, 1);
        focusSounds[OnePieceIcon.WHEEL] = sounds.load(app, R.raw.focus_wheel, 1);
    }

    private static final java.util.Set<Integer> loaded = new java.util.HashSet<>();
    private static int pending;
    private static float pendingVolume = 0.6f;

    private static void play(int id) {
        play(id, 0.6f);
    }

    private static void play(int id, float volume) {
        if (sounds == null || id == 0) return;
        if (!loaded.contains(id)) {
            pending = id;
            pendingVolume = volume;
            return;
        }
        sounds.play(id, volume, volume, 1, 0, 1f);
    }

    /** A top-bar icon takes focus: its own short sound. */
    static void focus(int kind) {
        if (kind >= 0 && kind < focusSounds.length) play(focusSounds[kind]);
    }

    /** An app or a Continue watching card opens: Luffy's rubber stretch. */
    static void clickOpen() { play(stretch); }

    /** The network pill (the Den Den Mushi): its ring. */
    static void clickNetwork() { play(denDen); }

    /** The inputs pill (the Log Pose): the anime's Observation Haki sound, sensing what is around. */
    static void clickInputs() { play(haki); }

    /** The settings pill (the ship's wheel): the anime's "DON!". */
    static void clickWheel() { play(don); }

    /** The quick panel the remote's Settings key opens: the anime's alert, at about half volume (it opens often). */
    static void clickPanel() { play(alert, 0.32f); }

    /** The Apps screen opens: Luffy calling "Gomu Gomu no!". */
    static void clickApps() { play(gomuGomu); }

    // ---- frames and panels ----

    /** Wood brown of the tiles' frames and the parchment's edge. */
    static final int WOOD = 0xFF6B3F1D;
    /** Parchment: the panels and the Continue watching cards' "wanted poster" paper. */
    static final int PAPER = 0xFFF3E0B0, PAPER_INK = 0xFF3B2412;

    /** A carved wooden frame over a tile (OnePieceFrames.Wood), its corners following the tile's own. */
    static android.graphics.drawable.Drawable woodFrame(float density) {
        return new OnePieceFrames.Wood(density, -1f);
    }

    /** The wooden frame for a tile whose corners have a fixed radius (the Apps screen's). */
    static android.graphics.drawable.Drawable woodFrame(float density, float radiusPx) {
        return new OnePieceFrames.Wood(density, radiusPx);
    }

    /** The twisted gold rope round the focused tile (OnePieceFrames.Rope); it plays its move each time it is shown. */
    static android.graphics.drawable.Drawable rope(float density, float radiusPx) {
        return OnePieceFrames.Rope.gold(density, radiusPx);
    }

    /** Luffy's vest red: the app being carried on the Apps screen. */
    static final int RED = 0xFFD62828;

    /** The carried app's ring on the Apps screen: a thicker red rope, apart from the gold rope of plain focus. */
    static android.graphics.drawable.Drawable carried(float density, float radiusPx) {
        return OnePieceFrames.Rope.red(density, radiusPx);
    }

    /** Aged parchment in a carved wooden frame (OnePieceFrames): the menus' and notes' sheet. */
    static android.graphics.drawable.Drawable sheet(float density, float radiusPx) {
        return new android.graphics.drawable.LayerDrawable(new android.graphics.drawable.Drawable[]{
                new OnePieceFrames.Paper(density, radiusPx), new OnePieceFrames.Wood(density, radiusPx)});
    }

    /** A short note on a parchment slip at the bottom of the screen, in place of the system's grey toast. */
    @SuppressWarnings("deprecation")   // Toast.setView: still shown while the app is in front, as it is here
    static void toast(android.content.Context ctx, int text) {
        float d = ctx.getResources().getDisplayMetrics().density;
        TextView note = new TextView(ctx);
        note.setText(text);
        note.setTextColor(PAPER_INK);
        note.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 18);
        note.setTypeface(ctx.getResources().getFont(R.font.pirata_one));
        int padH = Math.round(22 * d), padV = Math.round(10 * d);
        note.setPadding(padH, padV, padH, padV);
        note.setBackground(sheet(d, 16 * d));
        android.widget.Toast t = new android.widget.Toast(ctx.getApplicationContext());
        t.setDuration(android.widget.Toast.LENGTH_SHORT);
        t.setView(note);
        t.show();
    }

    /**
     * A menu panel as a sheet of parchment: paper with a wooden edge, brown text and icons, and a gold row under
     * focus. Every focusable row in it is restyled; call it once the rows are in.
     */
    static void parchment(View panel) {
        float d = panel.getResources().getDisplayMetrics().density;
        panel.setBackground(sheet(d, 18 * d));
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
