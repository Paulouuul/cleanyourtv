package com.gathin.tvhome;

import android.animation.ValueAnimator;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.ComposeShader;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.text.Layout;
import android.text.TextPaint;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * The One Piece skin's lettering for text that sits on the sea rather than on a map: Home's clock, the screensaver's
 * clock and the section titles. Luckiest Guy (Apache 2.0), a chunky cartoon face, filled white to cream with a faint
 * screentone, edged with a brush-ink outline over a hard shadow in the frames' wood brown: light and warm against the
 * deep blue sea, as an anime title card. {@link #play} sweeps a gleam across the
 * letters and sets a sparkle off; on the clock it also plays each time the minute changes. Nothing runs between those moves.
 * (Pirata One, the map lettering, stays on the parchment.)
 */
final class OnePieceText {

    /** White into a warm cream: the clocks. */
    static final int CLOCK = 0;
    /** White into a paler cream: section titles. */
    static final int TITLE = 1;

    /** The outline's ink and the hard shadow, in the wood brown of the skin's frames. */
    private static final int INK = 0xFF1A0F08, SHADOW = 0xFF8A5626;

    private OnePieceText() { }

    /** Puts the lettering on {@code t}; returns the gleam, to play it again. */
    static Gleam apply(TextView t, int style) {
        float d = t.getResources().getDisplayMetrics().density;
        t.setTypeface(t.getResources().getFont(R.font.luckiest_guy));
        t.setAllCaps(false);
        t.setShadowLayer(0f, 0f, 0f, 0);
        t.setTextColor(0xFFFFFFFF);
        // Room for the outline and the shadow, which reach past the letters.
        int pad = Math.round(t.getTextSize() * 0.12f + 2f * d);
        // Luckiest Guy's capitals and the outline reach above the line's ascent: room on top too.
        t.setPaddingRelative(t.getPaddingStart() + pad / 2, t.getPaddingTop() + pad,
                t.getPaddingEnd() + pad, t.getPaddingBottom() + pad);
        Shader tone = screentone(d, style);
        int[] colors = style == CLOCK
                ? new int[]{0xFFFFFFFF, 0xFFFFF6DA, 0xFFFFDD8A}
                : new int[]{0xFFFFFFFF, 0xFFFFFAEA, 0xFFFFE7B0};
        Runnable fill = () -> {
            Layout layout = t.getLayout();
            if (layout == null || layout.getLineCount() == 0) return;
            float top = layout.getLineTop(0), bottom = layout.getLineBottom(layout.getLineCount() - 1);
            Shader gradient = new LinearGradient(0, top + (bottom - top) * 0.12f, 0, bottom - (bottom - top) * 0.1f,
                    colors, new float[]{0f, 0.5f, 1f}, Shader.TileMode.CLAMP);
            t.getPaint().setShader(new ComposeShader(gradient, tone, PorterDuff.Mode.SRC_OVER));
            t.invalidate();
        };
        t.addOnLayoutChangeListener((v, l, tp, r, b, ol, ot, or, ob) -> fill.run());
        t.post(fill);
        t.setBackground(new Behind(t, d));
        Gleam gleam = new Gleam(t, d);
        t.setForeground(gleam);
        return gleam;
    }

    /** Plays the gleam of every lettered text under {@code root}: when a screen comes into view. */
    static void playIn(View root) {
        if (root instanceof TextView && root.getForeground() instanceof Gleam) ((Gleam) root.getForeground()).play();
        if (root instanceof android.view.ViewGroup) {
            android.view.ViewGroup g = (android.view.ViewGroup) root;
            for (int i = 0; i < g.getChildCount(); i++) playIn(g.getChildAt(i));
        }
    }

    /** The clock's minute change: the gleam crosses the new time (the clock itself keeps its size). */
    static void popOnChange(TextView clock, Gleam gleam, boolean reducedMotion) {
        clock.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void afterTextChanged(android.text.Editable s) {
                if (clock.isAttachedToWindow() && !reducedMotion) gleam.play();
            }
        });
    }

    /** Manga screentone: a tile of small dots in a warm brown, faint, laid over the fill. */
    private static Shader screentone(float d, int style) {
        int cell = Math.max(4, Math.round(4.5f * d));
        Bitmap tile = Bitmap.createBitmap(cell, cell, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(tile);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(0x2A6B4A2A);
        c.drawCircle(cell / 2f, cell / 2f, Math.max(0.8f, 0.85f * d), p);
        return new BitmapShader(tile, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT);
    }

    /** Draws {@code t}'s text with {@code paint} set up by {@code setup}, putting the paint back afterwards. */
    private static void drawText(Canvas c, TextView t, float dx, float dy, java.util.function.Consumer<TextPaint> setup) {
        Layout layout = t.getLayout();
        if (layout == null) return;
        TextPaint paint = t.getPaint();
        Shader shader = paint.getShader();
        Paint.Style style = paint.getStyle();
        float width = paint.getStrokeWidth();
        int color = paint.getColor();
        Paint.Join join = paint.getStrokeJoin();
        int save = c.save();
        c.translate(t.getTotalPaddingLeft() - t.getScrollX() + dx, t.getTotalPaddingTop() - t.getScrollY() + dy);
        setup.accept(paint);
        layout.draw(c);
        c.restoreToCount(save);
        paint.setShader(shader);
        paint.setStyle(style);
        paint.setStrokeWidth(width);
        paint.setColor(color);
        paint.setStrokeJoin(join);
        paint.setXfermode(null);
    }

    /** Behind the letters: a hard wood-brown shadow down and right, inked, then the brush-ink outline. */
    private static final class Behind extends Drawable {
        private final TextView t;
        private final float d;

        Behind(TextView t, float density) {
            this.t = t;
            d = density;
        }

        @Override
        public void draw(@NonNull Canvas c) {
            float size = t.getTextSize();
            float line = Math.max(2f * d, size * 0.085f);
            float ox = size * 0.045f + d, oy = size * 0.06f + d;
            drawText(c, t, ox, oy, p -> {
                p.setShader(null);
                p.setColor(INK);
                p.setStyle(Paint.Style.FILL_AND_STROKE);
                p.setStrokeJoin(Paint.Join.ROUND);
                p.setStrokeWidth(line * 2f + 1.6f * d);
            });
            drawText(c, t, ox, oy, p -> {
                p.setShader(null);
                p.setColor(SHADOW);
                p.setStyle(Paint.Style.FILL_AND_STROKE);
                p.setStrokeJoin(Paint.Join.ROUND);
                p.setStrokeWidth(line * 2f - 1.2f * d);
            });
            // A soft halo first, then the ink line: a brush edge rather than a hard vector one.
            drawText(c, t, 0f, 0f, p -> {
                p.setShader(null);
                p.setColor(0x6624150A);
                p.setStyle(Paint.Style.STROKE);
                p.setStrokeJoin(Paint.Join.ROUND);
                p.setStrokeWidth(line * 2f + 2f * d);
            });
            drawText(c, t, 0f, 0f, p -> {
                p.setShader(null);
                p.setColor(INK);
                p.setStyle(Paint.Style.STROKE);
                p.setStrokeJoin(Paint.Join.ROUND);
                p.setStrokeWidth(line * 2f);
            });
        }

        @Override public void setAlpha(int alpha) { }
        @Override public void setColorFilter(@Nullable ColorFilter filter) { }
        @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
    }

    /** Over the letters: a gleam crossing them only, and a sparkle at the end of the word. */
    static final class Gleam extends Drawable {
        private final TextView t;
        private final float d;
        private final Paint band = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint star = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path path = new Path();
        private final ValueAnimator move = ValueAnimator.ofFloat(0f, 1f);
        private float k = -1f;

        Gleam(TextView t, float density) {
            this.t = t;
            d = density;
            move.setDuration(1300);
            move.setInterpolator(new LinearInterpolator());
            move.addUpdateListener(a -> {
                k = (float) a.getAnimatedValue();
                invalidateSelf();
            });
            move.addListener(new android.animation.AnimatorListenerAdapter() {
                @Override public void onAnimationEnd(android.animation.Animator a) {
                    k = -1f;
                    invalidateSelf();
                }
            });
        }

        /** Sweeps the gleam across the letters once. */
        void play() {
            if (!t.isAttachedToWindow()) return;
            move.cancel();
            move.start();
        }

        @Override
        public boolean setVisible(boolean visible, boolean restart) {
            if (!visible) move.cancel();
            return super.setVisible(visible, restart);
        }

        @Override
        public void draw(@NonNull Canvas c) {
            if (k < 0f) return;
            Layout layout = t.getLayout();
            if (layout == null || layout.getLineCount() == 0) return;
            float textW = 0f;
            for (int i = 0; i < layout.getLineCount(); i++) textW = Math.max(textW, layout.getLineRight(i));
            float left = t.getTotalPaddingLeft(), top = t.getTotalPaddingTop();
            float h = layout.getHeight();
            float g = Math.min(1f, k / 0.75f);
            if (g < 1f) {
                // The band on the letters only: the glyphs go into a layer first, then the band is kept only where
                // they are (SRC_IN), so it lights the letters' shapes and never a box round them.
                int layer = c.saveLayer(0, 0, t.getWidth(), t.getHeight(), null);
                drawText(c, t, 0f, 0f, p -> {
                    p.setShader(null);
                    p.setColor(0xFFFFFFFF);
                    p.setStyle(Paint.Style.FILL);
                });
                float w = h * 0.55f, x = left - w * 2f + (textW + w * 4f) * g;
                band.setShader(new LinearGradient(x, top, x + w, top + h * 0.35f,
                        new int[]{0x00FFC83A, 0xF2FFC83A, 0x00FFC83A}, null, Shader.TileMode.CLAMP));   // gold on white
                band.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
                c.drawRect(0, 0, t.getWidth(), t.getHeight(), band);
                band.setShader(null);
                band.setXfermode(null);
                c.restoreToCount(layer);
            }
            // A sparkle at the word's top right as the gleam leaves it.
            if (k > 0.5f) {
                float s = (float) Math.sin((k - 0.5f) / 0.5f * Math.PI) * Math.min(14f * d, h * 0.3f);
                if (s > 0.5f) {
                    float x = left + textW - s * 0.2f, y = top + h * 0.18f;
                    float in = s * 0.12f;
                    path.reset();
                    path.moveTo(x, y - s);
                    path.quadTo(x + in, y - in, x + s, y);
                    path.quadTo(x + in, y + in, x, y + s);
                    path.quadTo(x - in, y + in, x - s, y);
                    path.quadTo(x - in, y - in, x, y - s);
                    star.setShader(new RadialGradient(x, y, s, 0xFFFFFFFF, 0xFFFFE27A, Shader.TileMode.CLAMP));
                    c.drawPath(path, star);
                    star.setShader(null);
                }
            }
        }

        @Override public void setAlpha(int alpha) { }
        @Override public void setColorFilter(@Nullable ColorFilter filter) { }
        @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
    }
}
