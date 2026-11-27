package com.gathin.tvhome;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.ImageDecoder;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.graphics.drawable.AnimatedImageDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * The One Piece skin's greeting in Home's header, between the clock and the top-bar pills: a comic panel with Luffy
 * and a speech bubble. {@link #play()} has him say his line in Japanese, smiling, while the bubble types it out in
 * the launcher's language; then he keeps spinning his hat while Home is in front. Needs Android 9 for animated WebP.
 */
@TargetApi(Build.VERSION_CODES.P)
final class LuffyGreeting {

    private static final int PANEL_H_DP = 112, PANEL_W_DP = 199, BUBBLE_W_DP = 236;
    /** How long the bubble takes to type its line, about the length of the spoken line. */
    private static final long TYPE_MS = 3600, SPEECH_MAX_MS = 6500;
    /** The bubble goes as the line ends, with Luffy's switch to the hat spin; the typing ends about a second before
     *  the voice does, so the whole line has been on screen by then. */
    private static final long BUBBLE_HOLD_MS = 0;
    /** Without a key for this long, the hat spin finishes the loop it is in and Luffy rests; the next key resumes it. */
    private static final long IDLE_REST_MS = 2 * 60_000L;

    private final Context ctx;
    private final float density;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final LinearLayout box;
    private final TextView bubble;
    /** The One Piece logo, in the bubble's place whenever the bubble is not up. */
    private final ImageView logo;
    /** Over Luffy for a moment when his animation changes, fading out so the switch is not a cut. */
    private final ImageView fade;
    private final ImageView luffy;
    private final String line;
    private final int pw, ph;
    private MediaPlayer voice;
    private Drawable shown;
    private boolean speaking;
    private int typed;

    LuffyGreeting(Activity home, RelativeLayout header, View leftBlock, View topButtons) {
        ctx = home;
        density = home.getResources().getDisplayMetrics().density;
        line = home.getString(R.string.onepiece_greeting);
        pw = dp(PANEL_W_DP);
        ph = dp(PANEL_H_DP);

        box = new LinearLayout(home);
        box.setOrientation(LinearLayout.HORIZONTAL);
        box.setGravity(Gravity.CENTER_VERTICAL | Gravity.END);
        box.setClipChildren(false);
        box.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);

        bubble = new TextView(home);
        bubble.setTypeface(OnePieceSkin.comic(home));
        bubble.setTextSize(TypedValue.COMPLEX_UNIT_SP, 19);
        bubble.setTextColor(OnePieceSkin.INK);
        bubble.setLetterSpacing(0.02f);
        bubble.setLineSpacing(0, 0.95f);
        bubble.setGravity(Gravity.CENTER);
        bubble.setPadding(dp(14), dp(10), dp(24), dp(10));
        bubble.setBackground(new BubbleDrawable(density));
        bubble.setText(line);
        bubble.setVisibility(View.INVISIBLE);
        // The bubble and the logo share one slot beside Luffy: one shows while the other is hidden.
        FrameLayout slot = new FrameLayout(home);
        slot.setClipChildren(false);
        slot.addView(bubble, new FrameLayout.LayoutParams(dp(BUBBLE_W_DP), FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.CENTER));
        logo = new ImageView(home);
        logo.setImageResource(R.drawable.onepiece_logo);
        logo.setScaleType(ImageView.ScaleType.FIT_CENTER);
        slot.addView(logo, new FrameLayout.LayoutParams(dp(BUBBLE_W_DP), FrameLayout.LayoutParams.MATCH_PARENT, Gravity.CENTER));
        // Above Luffy's panel (which has an elevation for its shadow), so the logo's pop and the bubble's tail are
        // never drawn behind it. The slot has no background, so it casts no shadow of its own.
        slot.setElevation(dp(8));
        logo.setPadding(0, dp(6), dp(10), dp(6));
        box.addView(slot, new LinearLayout.LayoutParams(dp(BUBBLE_W_DP), dp(PANEL_H_DP)));

        FrameLayout panel = new FrameLayout(home);
        GradientDrawable frame = new GradientDrawable();
        frame.setColor(0xFFFFFFFF);
        frame.setCornerRadius(dp(14));
        frame.setStroke(dp(3), 0xFFFFFFFF);   // white: the panel's edge reads on the skin's dark sea
        panel.setBackground(frame);
        panel.setPadding(dp(3), dp(3), dp(3), dp(3));
        panel.setElevation(dp(6));
        luffy = new ImageView(home);
        luffy.setScaleType(ImageView.ScaleType.CENTER_CROP);
        luffy.setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), dp(11));
            }
        });
        luffy.setClipToOutline(true);
        panel.addView(luffy, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        fade = new ImageView(home);
        fade.setScaleType(ImageView.ScaleType.CENTER_CROP);
        fade.setOutlineProvider(luffy.getOutlineProvider());
        fade.setClipToOutline(true);
        fade.setVisibility(View.GONE);
        panel.addView(fade, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        LinearLayout.LayoutParams plp = new LinearLayout.LayoutParams(pw, ph);
        plp.setMarginStart(dp(4));
        box.addView(panel, plp);

        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        lp.addRule(RelativeLayout.START_OF, topButtons.getId());
        lp.addRule(RelativeLayout.ALIGN_BOTTOM, leftBlock.getId());
        lp.setMarginEnd(dp(20));
        header.addView(box, lp);

        // Luffy still, until the greeting plays.
        show(load(R.raw.luffy_idle, 0), false);

        // Too little room beside the clock and the date (a long date, large text): leave the greeting out.
        View clock = home.findViewById(R.id.clock), date = home.findViewById(R.id.date);
        box.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or, ob) -> {
            int right = Math.max(rightEdge(clock), rightEdge(date));
            int[] at = new int[2];
            v.getLocationInWindow(at);
            boolean fits = at[0] >= right + dp(12);
            int want = fits ? View.VISIBLE : View.INVISIBLE;
            if (v.getVisibility() != want) v.post(() -> v.setVisibility(want));
        });
    }

    /** Where a header text's widest line ends, in window coordinates. */
    private static int rightEdge(View v) {
        if (!(v instanceof TextView) || v.getVisibility() != View.VISIBLE) return 0;
        TextView t = (TextView) v;
        int[] at = new int[2];
        t.getLocationInWindow(at);
        float w = 0f;
        if (t.getLayout() != null) {
            for (int i = 0; i < t.getLayout().getLineCount(); i++) w = Math.max(w, t.getLayout().getLineRight(i));
        } else {
            w = t.getWidth();
        }
        return at[0] + t.getPaddingLeft() + Math.round(w);
    }

    /** Luffy says his line: Japanese voice, the bubble typing it out, then his hat spin. */
    void play() {
        stop();
        if (box.getVisibility() != View.VISIBLE) return;
        speaking = true;
        show(load(R.raw.luffy_talk, AnimatedImageDrawable.REPEAT_INFINITE), true);
        typed = 0;
        type();
        logo.animate().cancel();
        logo.animate().alpha(0f).scaleX(0.8f).scaleY(0.8f).setDuration(160)
                .setInterpolator(new android.view.animation.AccelerateInterpolator())
                .withEndAction(() -> logo.setVisibility(View.INVISIBLE)).start();
        bubble.setAlpha(0f);
        bubble.setScaleX(0.85f);
        bubble.setScaleY(0.85f);
        bubble.setPivotX(dp(BUBBLE_W_DP));
        bubble.setVisibility(View.VISIBLE);
        bubble.animate().alpha(1f).scaleX(1f).scaleY(1f).setStartDelay(120).setDuration(220).start();
        handler.postDelayed(typeTick, TYPE_MS / Math.max(1, line.length()));
        try {
            AudioAttributes attrs = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build();
            AudioManager am = ctx.getSystemService(AudioManager.class);
            voice = MediaPlayer.create(ctx, R.raw.luffy_intro, attrs, am != null ? am.generateAudioSessionId() : 0);
            if (voice != null) {
                voice.setOnCompletionListener(mp -> afterSpeech());
                voice.start();
            }
        } catch (RuntimeException ignored) {
            // No sound: the bubble and the animation still play.
        }
        handler.postDelayed(this::afterSpeech, SPEECH_MAX_MS);
    }

    private final Runnable typeTick = new Runnable() {
        @Override
        public void run() {
            if (typed >= line.length()) return;
            typed++;
            type();
            handler.postDelayed(this, TYPE_MS / Math.max(1, line.length()));
        }
    };

    /** The typed part shows; the rest is there but clear, so the bubble keeps its size while it types. */
    private void type() {
        SpannableString s = new SpannableString(line);
        if (typed < line.length()) {
            s.setSpan(new ForegroundColorSpan(0x00000000), typed, line.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        bubble.setText(s);
    }

    private void afterSpeech() {
        if (!speaking) return;
        speaking = false;
        greeted = true;
        handler.removeCallbacksAndMessages(null);
        releaseVoice();
        typed = line.length();
        type();
        show(load(R.raw.luffy_idle, AnimatedImageDrawable.REPEAT_INFINITE), true);
        handler.postDelayed(rest, IDLE_REST_MS);
        handler.postDelayed(() -> bubble.animate().alpha(0f).scaleX(0.9f).scaleY(0.9f).setStartDelay(0).setDuration(300)
                .withEndAction(() -> {
                    bubble.setVisibility(View.INVISIBLE);
                    popLogo();
                }).start(), BUBBLE_HOLD_MS);
    }

    /** Luffy has greeted once: from then on he idles in a loop while Home is in front. */
    private boolean greeted;

    /**
     * Home back in front without a greeting (a panel over it closed, Google Home's for one): the idle loop that
     * {@link #stop()} froze starts again, without the voice.
     */
    void resumeIdle() {
        if (speaking || !greeted) return;
        if (shown instanceof AnimatedImageDrawable && ((AnimatedImageDrawable) shown).isRunning()) return;
        show(load(R.raw.luffy_idle, AnimatedImageDrawable.REPEAT_INFINITE), true);
        handler.removeCallbacks(rest);
        handler.postDelayed(rest, IDLE_REST_MS);
    }

    /**
     * Nobody has pressed a key for {@link #IDLE_REST_MS}: the spin plays to the end of its loop and stops there, so
     * Home stops drawing 15 frames a second for a screen nobody is looking at. The picture stays where the loop ends.
     */
    private final Runnable rest = () -> {
        if (!speaking && shown instanceof AnimatedImageDrawable) ((AnimatedImageDrawable) shown).setRepeatCount(0);
    };

    /** A key on Home: a resting Luffy spins again, and the rest is put off for another {@link #IDLE_REST_MS}. */
    void userActive() {
        if (speaking || !greeted || !(shown instanceof AnimatedImageDrawable)) return;
        AnimatedImageDrawable spin = (AnimatedImageDrawable) shown;
        spin.setRepeatCount(AnimatedImageDrawable.REPEAT_INFINITE);
        if (!spin.isRunning()) spin.start();
        handler.removeCallbacks(rest);
        handler.postDelayed(rest, IDLE_REST_MS);
    }

    /** Home left: the voice and the animation stop, Luffy stands still and the bubble goes. */
    void stop() {
        boolean was = speaking || shown instanceof AnimatedImageDrawable && ((AnimatedImageDrawable) shown).isRunning();
        speaking = false;
        handler.removeCallbacksAndMessages(null);
        releaseVoice();
        bubble.animate().cancel();
        bubble.setVisibility(View.INVISIBLE);
        fade.animate().cancel();
        if (fade.getDrawable() instanceof AnimatedImageDrawable) ((AnimatedImageDrawable) fade.getDrawable()).stop();
        fade.setImageDrawable(null);
        fade.setVisibility(View.GONE);
        logo.animate().cancel();
        logo.setAlpha(1f);
        logo.setScaleX(1f);
        logo.setScaleY(1f);
        logo.setRotation(0f);
        logo.setVisibility(View.VISIBLE);
        if (was) show(load(R.raw.luffy_idle, 0), false);
    }

    void release() {
        stop();
        if (shown instanceof AnimatedImageDrawable) ((AnimatedImageDrawable) shown).stop();
        luffy.setImageDrawable(null);
        shown = null;
    }

    private void releaseVoice() {
        if (voice == null) return;
        try {
            voice.stop();
        } catch (IllegalStateException ignored) {
        }
        voice.release();
        voice = null;
    }

    /** The logo comes back where the bubble was: it pops up from small with a little swing, like a comic stamp. */
    private void popLogo() {
        logo.animate().cancel();
        logo.setVisibility(View.VISIBLE);
        logo.setAlpha(0f);
        logo.setScaleX(0.3f);
        logo.setScaleY(0.3f);
        logo.setRotation(-12f);
        logo.animate().alpha(1f).scaleX(1f).scaleY(1f).rotation(0f).setStartDelay(0).setDuration(550)
                .setInterpolator(new android.view.animation.OvershootInterpolator(2.2f)).withEndAction(null).start();
    }

    private void show(@Nullable Drawable d, boolean start) {
        if (d == null) return;
        Drawable old = shown;
        shown = d;
        luffy.setImageDrawable(d);
        if (old != null && start && luffy.isAttachedToWindow()) {
            // The old animation fades out over the new one, still moving, then stops.
            fade.animate().cancel();
            fade.setImageDrawable(old);
            fade.setAlpha(1f);
            fade.setVisibility(View.VISIBLE);
            fade.animate().alpha(0f).setDuration(350).withEndAction(() -> {
                fade.setVisibility(View.GONE);
                fade.setImageDrawable(null);
                if (old instanceof AnimatedImageDrawable) ((AnimatedImageDrawable) old).stop();
            }).start();
        } else if (old instanceof AnimatedImageDrawable) {
            ((AnimatedImageDrawable) old).stop();
        }
        if (start && d instanceof AnimatedImageDrawable) ((AnimatedImageDrawable) d).start();
    }

    /** Decodes an animated WebP at the panel's size, so its frames take no more memory than they show. */
    @Nullable
    private Drawable load(int res, int repeat) {
        try {
            Drawable d = ImageDecoder.decodeDrawable(ImageDecoder.createSource(ctx.getResources(), res),
                    (decoder, info, source) -> {
                        float s = Math.max(pw / (float) info.getSize().getWidth(), ph / (float) info.getSize().getHeight());
                        if (s < 1f) decoder.setTargetSize(Math.round(info.getSize().getWidth() * s),
                                Math.round(info.getSize().getHeight() * s));
                    });
            if (d instanceof AnimatedImageDrawable) ((AnimatedImageDrawable) d).setRepeatCount(repeat);
            return d;
        } catch (Exception e) {
            return null;
        }
    }

    private int dp(float v) {
        return Math.round(v * density);
    }

    /** A white comic speech bubble with a dark ink line and its tail pointing right, at Luffy. */
    private static final class BubbleDrawable extends Drawable {
        private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG), ink = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path path = new Path();
        private final float d;

        BubbleDrawable(float density) {
            d = density;
            fill.setColor(0xFFFFFFFF);
            ink.setColor(OnePieceSkin.INK);
            ink.setStyle(Paint.Style.STROKE);
            ink.setStrokeWidth(2.5f * density);
            ink.setStrokeJoin(Paint.Join.ROUND);
        }

        @Override
        protected void onBoundsChange(@NonNull android.graphics.Rect b) {
            float tail = 12 * d, inset = 1.5f * d, rad = 18 * d;
            float l = b.left + inset, t = b.top + inset, r = b.right - tail, bot = b.bottom - inset;
            float cy = t + (bot - t) * 0.55f;
            path.reset();
            path.addRoundRect(new RectF(l, t, r, bot), rad, rad, Path.Direction.CW);
            Path tip = new Path();
            tip.moveTo(r - 2 * d, cy - 9 * d);
            tip.quadTo(r + tail * 0.5f, cy - 2 * d, b.right - inset, cy + 6 * d);
            tip.quadTo(r + tail * 0.1f, cy + 4 * d, r - 2 * d, cy + 5 * d);
            tip.close();
            path.op(tip, Path.Op.UNION);
        }

        @Override
        public void draw(@NonNull Canvas c) {
            c.drawPath(path, fill);
            c.drawPath(path, ink);
        }

        @Override public void setAlpha(int alpha) { }
        @Override public void setColorFilter(@Nullable ColorFilter cf) { }
        @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
    }
}
