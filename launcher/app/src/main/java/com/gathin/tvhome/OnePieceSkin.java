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
        // Luckiest Guy, a chunky cartoon face, for the clock, the date and the greeting.
        android.graphics.Typeface comic = comic(home);
        for (int id : new int[]{R.id.greeting, R.id.clock, R.id.date}) {
            View v = home.findViewById(id);
            if (v instanceof TextView) ((TextView) v).setTypeface(comic);
        }
        // The clock: a little larger than the default face's 52sp, with some air between the chunky digits.
        View clock = home.findViewById(R.id.clock);
        if (clock instanceof TextView) {
            TextView c = (TextView) clock;
            c.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 56);
            c.setLetterSpacing(0.03f);
            // The outline and shadow overhang the text's width: room at the end so they are not clipped.
            c.setPaddingRelative(c.getPaddingStart(), c.getPaddingTop(), c.getPaddingEnd() + Math.round(10 * d),
                    c.getPaddingBottom());
            // The anime title-card lettering: gold with screentone, brush-ink outline and a hard shadow; it pops and
            // gleams each time the minute changes.
            OnePieceText.popOnChange(c, OnePieceText.apply(c, OnePieceText.CLOCK), false);
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

    /** Luckiest Guy (Apache 2.0), the lettering off the maps: clock, date, greeting and the speech bubble. */
    static android.graphics.Typeface comic(android.content.Context ctx) {
        return ctx.getResources().getFont(R.font.luckiest_guy);
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
    /** Text under the map's focus mark: near-black ink, as the mark. */
    static final int MAP_INK = 0xFF1E120A;

    /** The carried app's ring on the Apps screen: a thicker red rope, apart from the gold rope of plain focus. */
    static android.graphics.drawable.Drawable carried(float density, float radiusPx) {
        return OnePieceFrames.Rope.red(density, radiusPx);
    }

    /** Aged parchment with a treasure map's edge (OnePieceFrames.MapEdge): the menus', panels' and notes' sheet. */
    static android.graphics.drawable.Drawable sheet(float density, float radiusPx) {
        return new android.graphics.drawable.LayerDrawable(new android.graphics.drawable.Drawable[]{
                new OnePieceFrames.Paper(density, radiusPx), new OnePieceFrames.MapEdge(density, radiusPx)});
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
                new int[][]{{android.R.attr.state_focused}, {}}, new int[]{MAP_INK, PAPER_INK});
        if (panel instanceof ViewGroup) parchmentRows((ViewGroup) panel, ink, d);
    }

    /**
     * Launcher settings in the skin: the deep sea behind, the list on a parchment sheet in a wooden frame, the
     * headings in the pirate lettering, and every choice a plank of paper that turns sun gold under focus and keeps a
     * gold edge while it is the chosen one.
     */
    static void styleSettings(Activity screen) {
        float d = screen.getResources().getDisplayMetrics().density;
        View scroll = ((ViewGroup) screen.findViewById(android.R.id.content)).getChildAt(0);
        if (!(scroll instanceof ViewGroup) || ((ViewGroup) scroll).getChildCount() == 0) return;
        scroll.setBackgroundResource(R.drawable.onepiece_ocean);
        View list = ((ViewGroup) scroll).getChildAt(0);
        list.setBackground(sheet(d, 26 * d));
        ViewGroup.LayoutParams lp = list.getLayoutParams();
        if (lp instanceof ViewGroup.MarginLayoutParams) {
            int m = Math.round(28 * d);
            ((ViewGroup.MarginLayoutParams) lp).setMargins(Math.round(56 * d), m, Math.round(56 * d), m);
            list.setLayoutParams(lp);
        }
        list.setPaddingRelative(Math.round(44 * d), Math.round(30 * d), Math.round(44 * d), Math.round(34 * d));
        android.graphics.Typeface pirate = screen.getResources().getFont(R.font.pirata_one);
        android.content.res.ColorStateList ink = new android.content.res.ColorStateList(
                new int[][]{{android.R.attr.state_focused}, {}}, new int[]{MAP_INK, PAPER_INK});
        styleSettingsViews((ViewGroup) list, pirate, ink, d);
    }

    private static void styleSettingsViews(ViewGroup g, android.graphics.Typeface pirate,
                                           android.content.res.ColorStateList ink, float d) {
        for (int i = 0; i < g.getChildCount(); i++) {
            View c = g.getChildAt(i);
            if (c instanceof ViewGroup) {
                styleSettingsViews((ViewGroup) c, pirate, ink, d);
                continue;
            }
            if (!(c instanceof TextView)) continue;
            TextView t = (TextView) c;
            if (t.isFocusable()) {
                t.setTextColor(ink);
                t.setBackground(new android.graphics.drawable.LayerDrawable(new android.graphics.drawable.Drawable[]{
                        settingsChoice(d), new OnePieceFrames.ChosenMark(d)}));
                underlineOnFocus(t, d);
                liftOnFocus(t);
                continue;
            }
            float sp = t.getTextSize() / t.getResources().getDisplayMetrics().scaledDensity;
            if (sp >= 19f) {
                // Headings: the pirate lettering, a little larger as it is narrow.
                t.setTypeface(pirate);
                t.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, sp >= 30f ? 40f : 24f);
                t.setTextColor(PAPER_INK);
            } else {
                t.setTextColor(0xFF6B4A2A);   // hints: faded ink
            }
        }
    }

    /**
     * A shown dialog on its parchment sheet: the title in the pirate lettering, the message and list items in brown
     * ink, and the buttons as paper planks that turn sun gold under focus. Colours are set here, not left to the
     * dialog theme, which some firmware paints white on the light sheet.
     */
    static void styleDialog(android.app.AlertDialog dialog) {
        android.view.Window w = dialog.getWindow();
        if (w == null) return;
        float d = dialog.getContext().getResources().getDisplayMetrics().density;
        View root = w.getDecorView();
        TextView title = root.findViewById(dialog.getContext().getResources()
                .getIdentifier("alertTitle", "id", "android"));
        if (title != null) {
            title.setTypeface(dialog.getContext().getResources().getFont(R.font.pirata_one));
            title.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 26);
            title.setTextColor(PAPER_INK);
        }
        TextView message = root.findViewById(android.R.id.message);
        if (message != null) message.setTextColor(PAPER_INK);
        android.content.res.ColorStateList ink = new android.content.res.ColorStateList(
                new int[][]{{android.R.attr.state_focused}, {}}, new int[]{MAP_INK, PAPER_INK});
        for (int which : new int[]{android.app.AlertDialog.BUTTON_POSITIVE, android.app.AlertDialog.BUTTON_NEGATIVE,
                android.app.AlertDialog.BUTTON_NEUTRAL}) {
            android.widget.Button b = dialog.getButton(which);
            if (b == null) continue;
            b.setTextColor(ink);
            b.setBackground(settingsChoice(d));
            underlineOnFocus(b, d);
            liftOnFocus(b);
            int pad = Math.round(16 * d);
            b.setPadding(pad, b.getPaddingTop(), pad, b.getPaddingBottom());
            if (b.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
                ((ViewGroup.MarginLayoutParams) b.getLayoutParams()).setMarginStart(Math.round(10 * d));
            }
        }
        android.widget.ListView list = dialog.getListView();
        if (list != null) {
            list.setSelector(new OnePieceFrames.InkMark(d));
            list.setDrawSelectorOnTop(true);
            list.setOnHierarchyChangeListener(new ViewGroup.OnHierarchyChangeListener() {
                @Override public void onChildViewAdded(View parent, View child) {
                    if (child instanceof TextView) ((TextView) child).setTextColor(PAPER_INK);
                }
                @Override public void onChildViewRemoved(View parent, View child) { }
            });
            for (int i = 0; i < list.getChildCount(); i++) {
                if (list.getChildAt(i) instanceof TextView) ((TextView) list.getChildAt(i)).setTextColor(PAPER_INK);
            }
        }
    }

    /**
     * A choice on a map (a parchment menu, panel, dialog or the settings sheet): under focus a loop of dark ink is
     * drawn round it with an X at its end (OnePieceFrames.InkMark), each time focus arrives. Home's tiles keep the
     * gold rope; the maps have this mark of their own.
     */
    static void markOnFocus(View v, float d) {
        android.graphics.drawable.Drawable mark = new OnePieceFrames.InkMark(d);
        View.OnFocusChangeListener before = v.getOnFocusChangeListener();
        v.setOnFocusChangeListener((view, has) -> {
            if (before != null) before.onFocusChange(view, has);
            view.setForeground(has ? mark : null);
            if (has) OnePieceFrames.InkMark.playIfMark(mark);
        });
        if (v.isFocused()) {
            v.setForeground(mark);
            OnePieceFrames.InkMark.playIfMark(mark);
        }
    }

    /**
     * The settings map's focus: a brush stroke of ink swept under the choice with a red X at its end
     * (OnePieceFrames.InkUnderline), in place of the menus' circling ink loop.
     */
    static void underlineOnFocus(View v, float d) {
        android.graphics.drawable.Drawable mark = new OnePieceFrames.InkUnderline(d);
        View.OnFocusChangeListener before = v.getOnFocusChangeListener();
        v.setOnFocusChangeListener((view, has) -> {
            if (before != null) before.onFocusChange(view, has);
            view.setForeground(has ? mark : null);
            if (has) OnePieceFrames.InkUnderline.playIfUnderline(mark);
        });
        if (v.isFocused()) {
            v.setForeground(mark);
            OnePieceFrames.InkUnderline.playIfUnderline(mark);
        }
    }

    /**
     * The scrap under focus lifts a little off the map (no zoom) and flutters, as paper caught by a breeze; it
     * settles back flat when focus leaves.
     */
    private static void liftOnFocus(View v) {
        View.OnFocusChangeListener before = v.getOnFocusChangeListener();
        v.setOnFocusChangeListener((view, has) -> {
            if (before != null) before.onFocusChange(view, has);
            float d = view.getResources().getDisplayMetrics().density;
            // No zoom: a wide choice would grow past the sheet's edge. A lift, and a flutter smaller on wide ones.
            view.animate().translationY(has ? -2f * d : 0f)
                    .setDuration(180).setInterpolator(new android.view.animation.OvershootInterpolator(2f)).start();
            if (has) {
                float amp = 1.8f * Math.min(1f, 260f * d / Math.max(1, view.getWidth()));
                android.animation.ObjectAnimator flutter = android.animation.ObjectAnimator.ofFloat(view, View.ROTATION,
                        0f, -amp, amp * 0.7f, -amp * 0.4f, amp * 0.15f, 0f);
                flutter.setDuration(520);
                flutter.start();
            } else {
                view.setRotation(0f);
            }
        });
    }

    /** A settings choice on the parchment: paper at rest, gold with an ink edge under focus, gold-edged when chosen. */
    private static android.graphics.drawable.Drawable settingsChoice(float d) {
        return new OnePieceFrames.PaperTag(d);
    }

    /** Sets a menu row's plain icon, remembered so the One Piece skin can swap in its own drawn one (parchment). */
    static void rowIcon(android.widget.ImageView view, int res) {
        view.setImageResource(res);
        view.setTag(R.id.icon, res);
    }

    /** The row's drawn icon plays its move when the row takes focus. */
    private static void nudgeOnFocus(View row, OnePieceIcon icon) {
        View.OnFocusChangeListener before = row.getOnFocusChangeListener();
        row.setOnFocusChangeListener((v, has) -> {
            if (before != null) before.onFocusChange(v, has);
            if (has) icon.nudge();
        });
    }

    private static void parchmentRows(ViewGroup g, android.content.res.ColorStateList ink, float d) {
        for (int i = 0; i < g.getChildCount(); i++) {
            View c = g.getChildAt(i);
            if (c.isFocusable()) {
                c.setBackground(null);
                markOnFocus(c, d);
            }
            if (c instanceof TextView) {
                ((TextView) c).setTextColor(ink);
                ((TextView) c).setCompoundDrawableTintList(ink);
            } else if (c instanceof android.widget.ImageView) {
                android.widget.ImageView image = (android.widget.ImageView) c;
                Object res = image.getTag(R.id.icon);
                int kind = res instanceof Integer ? OnePieceIcon.forIcon((Integer) res) : -1;
                if (kind < 0) {
                    image.setImageTintList(ink);
                    continue;
                }
                // The skin's own drawn icon, in its colours, a little larger than the plain one.
                OnePieceIcon icon = new OnePieceIcon(d, kind);
                image.setImageTintList(null);
                image.setImageDrawable(icon);
                ViewGroup.LayoutParams size = image.getLayoutParams();
                size.width = size.height = Math.round(26 * d);
                image.setLayoutParams(size);
                for (View p = (View) image.getParent(); p != null && p != g.getRootView(); p = (View) p.getParent()) {
                    if (p.isFocusable()) {
                        nudgeOnFocus(p, icon);
                        break;
                    }
                    if (!(p.getParent() instanceof View)) break;
                }
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
