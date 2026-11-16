package com.gathin.tvhome;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.SoundEffectConstants;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * The Continue watching row (config.xml continue_watching): a caption and one row of cards between the
 * clock and the dock's title. It owns its own focus and keys, like the library button: the glass and the
 * hero step aside while a card is focused (Host.onRowFocused), Up leaves for the top bar, Down returns to
 * the dock, and Left/Right stop at the row's ends instead of escaping into the grid.
 */
final class ContinueRow extends LinearLayout {

    interface Host {
        void onRowFocused();

        void onRowUp();

        void onRowDown();

        void onRowOpen(ContinueWatching.Item item);

        /** True for a view in the top bar, whose Down is allowed to land here. */
        boolean inTopBar(View view);
    }

    static final int CARD_W_DP = 160, CARD_H_DP = 90;
    /** The most a focused card grows by; less when the room around the cards is narrower (focusScale). */
    private static final float FOCUS_SCALE = 1.05f;

    private final Host host;
    private final float density;
    private final LinearLayout cards;
    private final HorizontalScrollView scroller;
    private final TextView title;
    /** Every size in the row times this (setScale): Home gives the row the room smaller pinned tiles leave. */
    private float scale = 1f;
    /** OnePieceSkin's caption: Pirata One at a larger size (outlineCaption). */
    private boolean pirateCaption;
    private List<ContinueWatching.Item> items;
    private final List<View> cardViews = new ArrayList<>();
    private final java.util.Map<ContinueWatching.Item, ImageView> artViews = new HashMap<>();
    private int lastIndex;
    private boolean wanted;    // the owner of the row wants it on screen (focus is in the top bar, row or dock)
    /** Set by focusCard (Up from the dock) until that focus has arrived. */
    private boolean focusRequested;
    private boolean fits = true;   // false: the screen has no room between the clock and the dock's title

    ContinueRow(Context context, Host host, String caption) {
        super(context);
        this.host = host;
        this.density = context.getResources().getDisplayMetrics().density;
        setOrientation(VERTICAL);
        setVisibility(GONE);
        setClipChildren(false);
        setClipToPadding(false);

        title = new TextView(context);
        title.setText(caption);
        title.setAllCaps(true);
        // The greeting's style (activity_main.xml), so the two caps lines read as one family.
        title.setLetterSpacing(0.22f);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        title.setTextColor(0x8CFFFFFF);
        title.setTypeface(UiFont.get(context, R.string.font_medium));
        LayoutParams tlp = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tlp.setMarginStart(dp(12));
        addView(title, tlp);

        scroller = new HorizontalScrollView(context);
        scroller.setHorizontalScrollBarEnabled(false);
        scroller.setOverScrollMode(OVER_SCROLL_NEVER);
        scroller.setClipChildren(false);
        scroller.setClipToPadding(false);
        scroller.setSmoothScrollingEnabled(true);
        cards = new LinearLayout(context);
        cards.setOrientation(HORIZONTAL);
        cards.setClipChildren(false);
        cards.setClipToPadding(false);
        // Room for the focused card's zoom on every side, so the scroller does not clip it.
        // Bottom only as much as the 1.08 zoom needs, so the next grid row follows the cards closely.
        int pad = dp(12);
        cards.setPadding(pad, pad, pad, Math.round(4 * density));   // unscaled: the same bottom margin at any size
        scroller.addView(cards, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        addView(scroller, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    // Focus only comes here on purpose: Up from the dock, Down from the top bar, or along the row. When the grid
    // rebuilds its tiles the focused one is detached and Android hands focus to the first focusable view, which
    // is a card: that stray focus goes straight back to the dock.
    private final android.view.ViewTreeObserver.OnGlobalFocusChangeListener strayFocus = this::onGlobalFocus;

    private void onGlobalFocus(View oldFocus, View newFocus) {
        if (newFocus == null || !isInRow(newFocus)) return;
        boolean wanted = focusRequested || (oldFocus != null && (isInRow(oldFocus) || host.inTopBar(oldFocus)));
        focusRequested = false;
        if (!wanted) post(host::onRowDown);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        getViewTreeObserver().addOnGlobalFocusChangeListener(strayFocus);
    }

    @Override
    protected void onDetachedFromWindow() {
        getViewTreeObserver().removeOnGlobalFocusChangeListener(strayFocus);
        super.onDetachedFromWindow();
    }

    private boolean isInRow(View v) {
        for (android.view.ViewParent p = v.getParent(); p != null; p = p.getParent()) {
            if (p == this) return true;
        }
        return false;
    }

    boolean hasItems() {
        return !cardViews.isEmpty();
    }

    boolean isShowing() {
        return getVisibility() == VISIBLE && hasItems();
    }

    /** The first card's id, for the top bar's Down; View.NO_ID while the row is not on screen. */
    int firstCardId() {
        return isShowing() ? cardViews.get(0).getId() : View.NO_ID;
    }

    /** OnePieceSkin: the caption in white with a deep-sea outline, readable over a bright wallpaper. */
    void outlineCaption() {
        OnePieceSkin.outline(title, density);
        OnePieceSkin.pirateTitle(title);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22 * scale);
        pirateCaption = true;
    }

    float scale() {
        return scale;
    }

    /** Resizes the cards, their text and the caption; the cards are made again at the new size. */
    void setScale(float s) {
        if (Math.abs(s - scale) < 0.01f) return;
        scale = s;
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, (pirateCaption ? 22 : 12) * scale);
        ((LayoutParams) title.getLayoutParams()).setMarginStart(dp(12));
        int pad = dp(12);
        cards.setPadding(pad, pad, pad, Math.round(4 * density));   // unscaled: the same bottom margin at any size
        if (items != null) setItems(items);
    }

    void setItems(List<ContinueWatching.Item> items) {
        this.items = items;
        boolean hadFocus = hasFocus();
        cards.removeAllViews();
        cardViews.clear();
        artViews.clear();
        for (int i = 0; i < items.size(); i++) {
            View card = card(items.get(i), i);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(CARD_W_DP), dp(CARD_H_DP));
            if (i > 0) lp.setMarginStart(dp(18));
            cards.addView(card, lp);
            cardViews.add(card);
        }
        lastIndex = Math.min(lastIndex, Math.max(0, cardViews.size() - 1));
        applyVisibility();
        if (hadFocus) {
            if (hasItems()) focusCard();
            else host.onRowDown();
        }
    }

    /** Shown only while focus is up here: the grid scrolls through this space once Down leaves the dock. */
    void setWanted(boolean wanted) {
        if (this.wanted == wanted) return;
        this.wanted = wanted;
        applyVisibility();
    }

    void setFits(boolean fits) {
        if (this.fits == fits) return;
        this.fits = fits;
        applyVisibility();
    }

    private void applyVisibility() {
        int v = wanted && fits && hasItems() ? VISIBLE : GONE;
        if (getVisibility() == v) return;
        animate().cancel();
        if (v == VISIBLE) {
            setAlpha(0f);
            setVisibility(VISIBLE);
            animate().alpha(1f).setDuration(200).start();
        } else {
            setVisibility(GONE);
        }
    }

    /** A poster that arrived after the row was drawn takes the banner's place. */
    void showPoster(ContinueWatching.Item item) {
        ImageView art = artViews.get(item);
        if (art == null || item.poster == null) return;
        art.setImageBitmap(item.poster);
        art.setAlpha(0f);
        art.animate().alpha(1f).setDuration(250).start();
    }

    void focusCard() {
        if (!hasItems()) return;
        focusRequested = true;
        if (!cardViews.get(Math.min(lastIndex, cardViews.size() - 1)).requestFocus()) focusRequested = false;
    }

    private View card(ContinueWatching.Item item, int index) {
        Context ctx = getContext();
        FrameLayout card = new FrameLayout(ctx);
        card.setId(View.generateViewId());
        card.setFocusable(true);
        card.setClickable(true);
        card.setContentDescription(appLabel(item.pkg) + ": " + item.title);
        // One Piece skin (pirateCaption): a "wanted poster", the picture on a sheet of parchment over a caption.
        boolean poster = pirateCaption;
        if (poster) {
            // Aged parchment with grain, toasted edges and a hatched cel shadow (OnePieceFrames.Paper).
            card.setBackground(new OnePieceFrames.Paper(density, dp(12)));
        } else {
            GradientDrawable bg = new GradientDrawable();
            bg.setColor(0xFF1E2433);
            bg.setCornerRadius(dp(12));
            card.setBackground(bg);
        }
        card.setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), dp(12));
            }
        });
        card.setClipToOutline(true);

        ImageView art = new ImageView(ctx);
        art.setScaleType(ImageView.ScaleType.CENTER_CROP);
        if (item.poster != null) {
            art.setImageBitmap(item.poster);
        } else {
            // No local poster: the app's own banner, dimmed, under the title. A resource of the app itself,
            // so still no network.
            Drawable banner = banner(item.pkg);
            if (banner != null) {
                art.setImageDrawable(banner);
                art.setAlpha(0.35f);
            }
        }
        artViews.put(item, art);
        FrameLayout.LayoutParams alp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        if (poster) {
            alp.setMargins(dp(6), dp(6), dp(6), dp(item.subtitle.isEmpty() ? 34 : 48));
            art.setOutlineProvider(new ViewOutlineProvider() {
                @Override
                public void getOutline(View view, Outline outline) {
                    outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), dp(7));
                }
            });
            art.setClipToOutline(true);
        }
        card.addView(art, alp);

        if (!poster) {
            View scrim = new View(ctx);
            // Flat: a solid band under the text instead of a gradient (TCL 32S5K build).
            scrim.setBackgroundColor(0xCC111216);
            card.addView(scrim, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(46), Gravity.BOTTOM));
        }

        LinearLayout text = new LinearLayout(ctx);
        text.setOrientation(VERTICAL);
        text.setPadding(dp(10), 0, dp(10), dp(item.progress >= 0 ? 10 : 8));
        TextView name = label(item.title, 14, poster ? OnePieceSkin.PAPER_INK : 0xFFFFFFFF, R.string.font_medium);
        if (poster) {
            // The poster's title in the pirate face, inked, with a soft light edge like a printed bill.
            name.setTypeface(getResources().getFont(R.font.pirata_one));
            name.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18 * scale);
            name.setLetterSpacing(0.02f);
            name.setShadowLayer(1.5f * density, 0f, 1f * density, 0x99FFF6DC);
        }
        text.addView(name);
        if (!item.subtitle.isEmpty()) {
            TextView sub = label(item.subtitle, 11, poster ? 0xE05A3A1E : 0xB3FFFFFF, R.string.font_regular);
            if (poster) sub.setShadowLayer(1f * density, 0f, 0.8f * density, 0x80FFF6DC);
            text.addView(sub);
        }
        card.addView(text, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM));

        // Which app the row came from, as Google TV shows it: the app's icon in the top corner.
        Drawable icon = icon(item.pkg);
        if (icon != null) {
            ImageView badge = new ImageView(ctx);
            badge.setImageDrawable(icon);
            badge.setScaleType(ImageView.ScaleType.CENTER_CROP);
            badge.setOutlineProvider(new ViewOutlineProvider() {
                @Override
                public void getOutline(View view, Outline outline) {
                    outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), dp(6));
                }
            });
            badge.setClipToOutline(true);
            FrameLayout.LayoutParams ilp = new FrameLayout.LayoutParams(dp(26), dp(26), Gravity.TOP | Gravity.START);
            ilp.setMargins(dp(poster ? 12 : 8), dp(poster ? 12 : 8), 0, 0);
            card.addView(badge, ilp);
        }

        if (item.progress >= 0) {
            View track = new View(ctx);
            track.setBackgroundColor(poster ? 0x405A3A1E : 0x40FFFFFF);
            card.addView(track, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(3), Gravity.BOTTOM));
            View bar = new View(ctx);
            if (poster) {
                // A burning fuse: red to orange with a bright tip.
                bar.setBackground(new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                        new int[]{0xFF8E1414, 0xFFD62828, 0xFFFF9A2E}));
            } else {
                bar.setBackgroundColor(Color.WHITE);
            }
            FrameLayout.LayoutParams blp = new FrameLayout.LayoutParams(0, dp(3), Gravity.BOTTOM | Gravity.START);
            card.addView(bar, blp);
            card.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or, ob) -> {
                int w = Math.round((r - l) * item.progress);
                if (blp.width != w) {
                    blp.width = w;
                    bar.post(bar::requestLayout);
                }
            });
        }

        // The ring is drawn over the card, so it shows on light posters too.
        GradientDrawable ring = new GradientDrawable();
        ring.setCornerRadius(dp(12));
        ring.setStroke(dp(2), Color.WHITE);
        ring.setColor(Color.TRANSPARENT);
        Drawable focusRing = ring, restRing = null;
        if (poster) {
            // The poster's carved wooden frame at rest, the twisted gold rope (with its focus move) under focus.
            restRing = OnePieceSkin.woodFrame(density, dp(12));
            focusRing = OnePieceSkin.rope(density, dp(12));
            card.setForeground(restRing);
        }
        final Drawable onFocus = focusRing, atRest = restRing;
        card.setOnFocusChangeListener((v, focused) -> {
            v.animate().cancel();
            float zoom = focused ? focusScale(v) : 1f;
            v.animate().scaleX(zoom).scaleY(zoom).setDuration(150).start();
            card.setForeground(focused ? onFocus : atRest);
            if (focused) OnePieceFrames.Rope.playIfRope(onFocus);
            // A stray focus also lands here; strayFocus has already queued the move back to the dock, which puts
            // the glass and the title back.
            if (focused) {
                // The scroller only brings the card's own edges on screen, so the last card sat against the screen's
                // right edge and its zoom was cut off: scroll on until the zoom's room is on screen too.
                post(() -> keepZoomOnScreen(v));
                lastIndex = index;
                host.onRowFocused();
            }
        });
        card.setOnClickListener(v -> host.onRowOpen(item));
        card.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() != KeyEvent.ACTION_DOWN) {
                return keyCode == KeyEvent.KEYCODE_DPAD_UP || keyCode == KeyEvent.KEYCODE_DPAD_DOWN;
            }
            switch (keyCode) {
                case KeyEvent.KEYCODE_DPAD_UP:
                    v.playSoundEffect(SoundEffectConstants.NAVIGATION_UP);
                    host.onRowUp();
                    return true;
                case KeyEvent.KEYCODE_DPAD_DOWN:
                    v.playSoundEffect(SoundEffectConstants.NAVIGATION_DOWN);
                    host.onRowDown();
                    return true;
                // Move to the neighbouring card ourselves: left to Android's focus search, Right from a middle card
                // sometimes landed on a top-bar pill above the row (TCL 32S5K, G13). The ends stay put.
                case KeyEvent.KEYCODE_DPAD_LEFT:
                    if (index > 0) cardViews.get(index - 1).requestFocus();
                    return true;
                case KeyEvent.KEYCODE_DPAD_RIGHT:
                    if (index < cardViews.size() - 1) cardViews.get(index + 1).requestFocus();
                    return true;
                default:
                    return false;
            }
        });
        return card;
    }

    /** The room kept around the cards for the zoom: the cards' side padding. */
    private int zoomRoom() {
        return dp(12);
    }

    /** At most FOCUS_SCALE, and never more than the card can grow inside zoomRoom on each side. */
    private float focusScale(View card) {
        int w = card.getWidth();
        if (w <= 0) return 1f;
        return Math.min(FOCUS_SCALE, 1f + 2f * zoomRoom() / w);
    }

    private void keepZoomOnScreen(View card) {
        if (!card.isFocused()) return;
        int room = zoomRoom(), x = scroller.getScrollX(), viewW = scroller.getWidth();
        int left = card.getLeft() + cards.getLeft() - room, right = card.getRight() + cards.getLeft() + room;
        if (right > x + viewW) scroller.smoothScrollTo(right - viewW, 0);
        else if (left < x) scroller.smoothScrollTo(Math.max(0, left), 0);
    }

    private TextView label(String s, int sp, int color, int font) {
        TextView t = new TextView(getContext());
        t.setText(s);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp * scale);
        t.setTextColor(color);
        t.setTypeface(UiFont.get(getContext(), font));
        t.setSingleLine(true);
        t.setEllipsize(TextUtils.TruncateAt.END);
        t.setIncludeFontPadding(false);
        t.setPadding(0, dp(2), 0, 0);
        return t;
    }

    private Drawable banner(String pkg) {
        PackageManager pm = getContext().getPackageManager();
        try {
            Drawable d = pm.getApplicationBanner(pkg);
            return d != null ? d : pm.getApplicationIcon(pkg);
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }

    private Drawable icon(String pkg) {
        try {
            return getContext().getPackageManager().getApplicationIcon(pkg);
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }

    private String appLabel(String pkg) {
        PackageManager pm = getContext().getPackageManager();
        try {
            return String.valueOf(pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)));
        } catch (PackageManager.NameNotFoundException e) {
            return pkg;
        }
    }

    private int dp(int v) {
        return Math.round(v * density * scale);
    }
}
