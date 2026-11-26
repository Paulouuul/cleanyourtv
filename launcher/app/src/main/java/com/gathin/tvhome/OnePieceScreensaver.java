package com.gathin.tvhome;

/**
 * The One Piece skin's screensaver scene, for ClockDream and ClockActivity alike: the opening (OnePieceIntro) on a
 * loop with its logo large in the middle, drifting slowly, and the time and date on one small, still line in the top
 * left corner, clear of the logo and its shade.
 */
final class OnePieceScreensaver {
    final android.view.View root;
    private final OnePieceIntro intro;
    private final ClockRefresh refresh;

    OnePieceScreensaver(android.content.Context ctx, Config cfg) {
        float d = ctx.getResources().getDisplayMetrics().density;
        android.widget.FrameLayout frame = new android.widget.FrameLayout(ctx);
        frame.setBackgroundColor(0xFF04121F);
        intro = new OnePieceIntro(ctx, cfg.reducedMotion, null, true);
        frame.addView(intro, new android.widget.FrameLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.MATCH_PARENT));

        // The time and the date on one small line in the top left corner, so the logo can fill the screen.
        android.widget.LinearLayout box = new android.widget.LinearLayout(ctx);
        box.setOrientation(android.widget.LinearLayout.HORIZONTAL);
        box.setBaselineAligned(true);
        android.widget.TextClock time = new android.widget.TextClock(ctx);
        time.setFormat12Hour("h:mm a");
        time.setFormat24Hour("HH:mm");
        android.widget.TextView dot = new android.widget.TextView(ctx);
        dot.setText(" \u00B7 ");
        android.widget.TextClock date = new android.widget.TextClock(ctx);
        date.setFormat12Hour(ctx.getString(R.string.date_format));
        date.setFormat24Hour(ctx.getString(R.string.date_format));
        for (android.widget.TextView t : new android.widget.TextView[]{time, dot, date}) {
            t.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 18);
            t.setIncludeFontPadding(false);   // no room above or below the line
            t.setPadding(0, 0, 0, 0);
            t.setLetterSpacing(0.04f);
            OnePieceSkin.outline(t, d);
            t.setTypeface(OnePieceSkin.comic(ctx));
            box.addView(t);
        }
        android.widget.FrameLayout.LayoutParams at = new android.widget.FrameLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
                android.view.Gravity.TOP | android.view.Gravity.START);
        at.setMargins(Math.round(32 * d), Math.round(18 * d), 0, 0);
        frame.addView(box, at);
        refresh = new ClockRefresh(box);
        root = frame;
    }

    void start() {
        intro.start();
        refresh.start();
    }

    void stop() {
        refresh.stop();
    }
}
