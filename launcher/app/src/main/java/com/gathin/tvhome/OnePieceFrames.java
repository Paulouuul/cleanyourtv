package com.gathin.tvhome;

import android.animation.ValueAnimator;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.DashPathEffect;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathDashPathEffect;
import android.graphics.PixelFormat;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.view.animation.LinearInterpolator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Random;

/**
 * The One Piece skin's frames, drawn like the top-bar pills (OnePiecePill): anime cels with a manga ink line.
 * <ul>
 * <li>{@link Wood}: an app at rest, a carved wooden frame with grain, a light bevel and gold rivets at the corners.</li>
 * <li>{@link Rope}: focus, a twisted rope of gold (or red, for a carried app) with an ink edge; {@link Rope#play()}
 * pulls it tight (the twists run round), sweeps a shine over the tile and pops two sparkles.</li>
 * <li>{@link Paper}: the Continue watching posters' sheet, parchment with grain, toasted edges and a cel shadow.</li>
 * </ul>
 * Corners follow the tile's own (AppAdapter.cornerRadius at the frame's width) unless a fixed radius is given.
 */
final class OnePieceFrames {

    static final int INK = 0xFF24150A;
    /** Line ink: warm dark brown, slightly see-through, so outlines soften into what they edge. */
    static final int LINE_INK = 0xC83A2414;

    private OnePieceFrames() { }

    private static float cornerFor(Rect b, float density, float fixed) {
        return fixed >= 0f ? fixed : AppAdapter.cornerRadius(b.width(), density);
    }

    private static float mixAlpha(int alpha, float k) {
        return Math.max(0f, Math.min(255f, alpha * k));
    }

    /** A carved wooden frame laid over a tile. */
    static final class Wood extends Drawable {
        private final float d, fixedRadius;
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF r = new RectF();
        private Shader woodShader;
        private int alpha = 255;

        Wood(float density, float radiusPx) {
            d = density;
            fixedRadius = radiusPx;
        }

        @Override
        protected void onBoundsChange(@NonNull Rect b) {
            woodShader = new LinearGradient(b.left, b.top, b.right, b.bottom,
                    new int[]{0xFFE2AA6C, 0xFFA8692F, 0xFF6A3D18}, new float[]{0f, 0.5f, 1f}, Shader.TileMode.CLAMP);
        }

        @Override
        public void draw(@NonNull Canvas c) {
            Rect b = getBounds();
            if (b.isEmpty()) return;
            float radius = cornerFor(b, d, fixedRadius);
            float band = 4.5f * d;
            float half = band / 2f;
            r.set(b.left + half, b.top + half, b.right - half, b.bottom - half);
            float rr = Math.max(0f, radius - half);
            p.setStyle(Paint.Style.STROKE);
            p.setPathEffect(null);
            p.setShader(null);

            // The board: wood lit from the upper left.
            p.setColor(0xFF000000);
            p.setAlpha(alpha);
            p.setShader(woodShader);
            p.setStrokeWidth(band);
            c.drawRoundRect(r, rr, rr, p);
            p.setShader(null);

            // Grain: broken dark and light lines running round the frame.
            p.setStrokeWidth(Math.max(1f, 0.8f * d));
            p.setColor(0x993A1E08);
            p.setAlpha((int) mixAlpha(alpha, 0.6f));
            p.setPathEffect(new DashPathEffect(new float[]{14 * d, 5 * d, 6 * d, 3 * d}, 0));
            inset(c, r, rr, -band * 0.18f);
            p.setColor(0x66FFE4BC);
            p.setAlpha((int) mixAlpha(alpha, 0.4f));
            p.setPathEffect(new DashPathEffect(new float[]{9 * d, 7 * d, 18 * d, 4 * d}, 5 * d));
            inset(c, r, rr, band * 0.2f);
            p.setPathEffect(null);

            // Cel shadow on the lower right half of the board, hatched.
            int save = c.save();
            c.clipRect(b.centerX(), b.centerY(), b.right, b.bottom);
            p.setColor(0x553A1E08);
            p.setAlpha((int) mixAlpha(alpha, 0.33f));
            p.setStrokeWidth(band * 0.5f);
            inset(c, r, rr, -band * 0.25f);
            c.restoreToCount(save);

            // Bevel: a light line on the inner edge, thin soft ink lines on both edges with a faint halo outside.
            p.setStrokeWidth(0.8f * d);
            p.setColor(0xCCFFE6C0);
            p.setAlpha((int) mixAlpha(alpha, 0.7f));
            inset(c, r, rr, half - 1f * d);
            p.setColor(LINE_INK);
            p.setAlpha((int) mixAlpha(alpha, 0.55f));
            p.setStrokeWidth(0.7f * d);
            inset(c, r, rr, half);
            p.setColor(LINE_INK);
            p.setAlpha((int) mixAlpha(alpha, 0.2f));
            p.setStrokeWidth(2f * d);
            inset(c, r, rr, -half + 0.4f * d);
            p.setColor(LINE_INK);
            p.setAlpha((int) mixAlpha(alpha, 0.78f));
            p.setStrokeWidth(0.9f * d);
            inset(c, r, rr, -half + 0.4f * d);

            // Gold rivets at the corners.
            float k = Math.max(rr * 0.29f, band * 0.6f);
            rivet(c, r.left + k, r.top + k, band * 0.42f);
            rivet(c, r.right - k, r.top + k, band * 0.42f);
            rivet(c, r.left + k, r.bottom - k, band * 0.42f);
            rivet(c, r.right - k, r.bottom - k, band * 0.42f);
        }

        private void inset(Canvas c, RectF base, float rr, float by) {
            c.drawRoundRect(base.left + by, base.top + by, base.right - by, base.bottom - by,
                    Math.max(0f, rr - by), Math.max(0f, rr - by), p);
        }

        private void rivet(Canvas c, float x, float y, float rad) {
            p.setStyle(Paint.Style.FILL);
            p.setColor(0xFF000000);
            p.setAlpha(alpha);
            p.setShader(new RadialGradient(x - rad * 0.4f, y - rad * 0.4f, rad * 1.6f, 0xFFFFF4B0, 0xFFC88A10,
                    Shader.TileMode.CLAMP));
            c.drawCircle(x, y, rad, p);
            p.setShader(null);
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(0.5f * d);
            p.setColor(LINE_INK);
            p.setAlpha(alpha);
            c.drawCircle(x, y, rad, p);
        }

        @Override public void setAlpha(int a) { alpha = a; invalidateSelf(); }
        @Override public void setColorFilter(@Nullable ColorFilter cf) { p.setColorFilter(cf); invalidateSelf(); }
        @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
    }

    /** A twisted rope round a focused tile, gold or red, with a focus move. */
    static final class Rope extends Drawable {
        private final float d, width;
        private float fixedRadius;
        private final int light, base, dark, twist;
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint fx = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF r = new RectF();
        private final Path path = new Path(), strand = new Path(), gloss = new Path();
        private final ValueAnimator move = ValueAnimator.ofFloat(0f, 1f);
        private Shader ropeShader;
        /** The focus move's progress; 1 at rest. */
        private float t = 1f;
        private int alpha = 255;

        /** Gold rope for focus. */
        static Rope gold(float density, float radiusPx) {
            return new Rope(density, radiusPx, 5f, 0xFFFFF6B0, OnePieceSkin.GOLD, 0xFFD98A0A, 0xFFA8650A);
        }

        /** Red rope, Luffy's vest, for an app being carried. */
        static Rope red(float density, float radiusPx) {
            return new Rope(density, radiusPx, 6f, 0xFFFF8A80, OnePieceSkin.RED, 0xFF8E1414, 0xFF6A0E0E);
        }

        private Rope(float density, float radiusPx, float widthDp, int light, int base, int dark, int twist) {
            d = density;
            fixedRadius = radiusPx;
            width = widthDp * density;
            this.light = light;
            this.base = base;
            this.dark = dark;
            this.twist = twist;
            move.setDuration(1100);
            move.setInterpolator(new LinearInterpolator());   // eased per part in draw
            move.addUpdateListener(a -> {
                t = (float) a.getAnimatedValue();
                invalidateSelf();
            });
            // One twist: a slanted lens across the rope, stamped along it.
            float h = width / 2f, a = width * 0.32f, th = Math.max(0.8f, 0.75f * d);
            strand.moveTo(-a - th, -h);
            strand.lineTo(-a + th, -h);
            strand.lineTo(a + th, h);
            strand.lineTo(a - th, h);
            strand.close();
            float g = Math.max(0.5f, 0.35f * d);
            gloss.moveTo(-a - g, -h * 0.85f);
            gloss.lineTo(-a + g, -h * 0.85f);
            gloss.lineTo(-a * 0.1f + g, 0f);
            gloss.lineTo(-a * 0.1f - g, 0f);
            gloss.close();
        }

        /** A new corner radius (the Home highlight follows the tile's size). */
        void setRadius(float px) {
            fixedRadius = px;
            invalidateSelf();
        }

        /** The focus move: the twists run round as the rope pulls tight, a shine crosses, two sparkles pop. */
        void play() {
            move.cancel();
            t = 0f;
            move.start();
        }

        /** Plays {@code d}'s move when it is a rope; anything else is left alone. */
        static void playIfRope(Drawable d) {
            if (d instanceof Rope) ((Rope) d).play();
        }

        @Override
        public boolean setVisible(boolean visible, boolean restart) {
            if (!visible) move.cancel();
            return super.setVisible(visible, restart);
        }

        @Override
        protected void onBoundsChange(@NonNull Rect b) {
            ropeShader = new LinearGradient(b.left, b.top, b.right, b.bottom, new int[]{light, base, dark},
                    new float[]{0f, 0.45f, 1f}, Shader.TileMode.CLAMP);
        }

        @Override
        public void draw(@NonNull Canvas c) {
            Rect b = getBounds();
            if (b.isEmpty()) return;
            float radius = cornerFor(b, d, fixedRadius);
            float half = width / 2f + 0.8f * d;
            r.set(b.left + half, b.top + half, b.right - half, b.bottom - half);
            float rr = Math.max(0f, radius - half);
            path.reset();
            path.addRoundRect(r, rr, rr, Path.Direction.CW);

            // Shine across the tile under the rope while the move plays.
            if (t > 0.08f && t < 0.8f) shine(c, b, radius);

            p.setStyle(Paint.Style.STROKE);
            p.setPathEffect(null);
            // A soft ink edge on both sides: a faint wide halo, then a thin line just outside the rope.
            p.setShader(null);
            p.setColor(LINE_INK);
            p.setAlpha((int) mixAlpha(alpha, 0.22f));
            p.setStrokeWidth(width + 2.4f * d);
            c.drawPath(path, p);
            p.setColor(LINE_INK);
            p.setAlpha((int) mixAlpha(alpha, 0.75f));
            p.setStrokeWidth(width + 1f * d);
            c.drawPath(path, p);
            // The rope's body in a lit gradient.
            p.setColor(0xFF000000);
            p.setAlpha(alpha);
            p.setShader(ropeShader);
            p.setStrokeWidth(width);
            c.drawPath(path, p);
            p.setShader(null);
            // Twists and their gloss, running round while the move plays.
            float advance = width * 0.95f;
            float pull = 1f - (1f - t) * (1f - t) * (1f - t);   // fast, then easing to a stop
            float run = (1f - pull) * advance * 8f;
            p.setStyle(Paint.Style.FILL);
            p.setColor(twist);
            p.setAlpha(alpha);
            p.setPathEffect(new PathDashPathEffect(strand, advance, run, PathDashPathEffect.Style.MORPH));
            c.drawPath(path, p);
            p.setColor(0xFFFFFFFF);
            p.setAlpha((int) mixAlpha(alpha, 0.6f));
            p.setPathEffect(new PathDashPathEffect(gloss, advance, run + advance * 0.5f, PathDashPathEffect.Style.MORPH));
            c.drawPath(path, p);
            p.setPathEffect(null);
            p.setStyle(Paint.Style.STROKE);

            // Sparkles at two corners.
            if (t < 1f) {
                float s = (float) Math.sin(Math.min(1f, t / 0.6f) * Math.PI);
                float k = rr * 0.29f + width * 1.6f;   // inside the rope, clear of the tile's rounded clip
                star(c, r.right - k, r.top + k, 9f * d * s);
                float s2 = (float) Math.sin(Math.max(0f, Math.min(1f, (t - 0.25f) / 0.6f)) * Math.PI);
                star(c, r.left + k, r.bottom - k, 7f * d * s2);
            }
        }

        private void shine(Canvas c, Rect b, float radius) {
            float k = (t - 0.08f) / 0.72f;
            int save = c.save();
            path.reset();
            path.addRoundRect(b.left, b.top, b.right, b.bottom, radius, radius, Path.Direction.CW);
            c.clipPath(path);
            // A soft white band, tilted like a glint on glass, crossing from left to right.
            float diag = (float) Math.hypot(b.width(), b.height());
            float band = Math.max(22f * d, b.width() * 0.16f);
            c.rotate(24f, b.exactCenterX(), b.exactCenterY());
            float x = b.exactCenterX() - diag / 2f - band + (diag + 2 * band) * k;
            fx.setStyle(Paint.Style.FILL);
            fx.setColor(0xFF000000);
            fx.setShader(new LinearGradient(x, 0, x + band, 0, new int[]{0x00FFFFFF, 0x99FFFFFF, 0x00FFFFFF}, null,
                    Shader.TileMode.CLAMP));
            fx.setAlpha((int) mixAlpha(alpha, (float) Math.sin(k * Math.PI)));
            c.drawRect(x, b.exactCenterY() - diag, x + band, b.exactCenterY() + diag, fx);
            fx.setShader(null);
            c.restoreToCount(save);
            // The rope's own path is rebuilt by draw after this.
            float half = width / 2f + 0.8f * d;
            float rr = Math.max(0f, radius - half);
            path.reset();
            path.addRoundRect(r, rr, rr, Path.Direction.CW);
        }

        private void star(Canvas c, float x, float y, float rad) {
            if (rad < 0.5f) return;
            float k = rad * 0.1f;
            fx.setShader(null);
            Path s = new Path();
            s.moveTo(x, y - rad);
            s.quadTo(x + k, y - k, x + rad, y);
            s.quadTo(x + k, y + k, x, y + rad);
            s.quadTo(x - k, y + k, x - rad, y);
            s.quadTo(x - k, y - k, x, y - rad);
            s.close();
            fx.setStyle(Paint.Style.FILL);
            fx.setColor(0xFFFFF8D8);
            fx.setAlpha(alpha);
            c.drawPath(s, fx);
            fx.setStyle(Paint.Style.STROKE);
            fx.setStrokeWidth(0.6f * d);
            fx.setColor(LINE_INK);
            fx.setAlpha(alpha);
            c.drawPath(s, fx);
        }

        @Override public void setAlpha(int a) { alpha = a; invalidateSelf(); }
        @Override public void setColorFilter(@Nullable ColorFilter cf) {
            p.setColorFilter(cf);
            fx.setColorFilter(cf);
            invalidateSelf();
        }
        @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
    }

    /** A parchment sheet: the Continue watching poster's paper, drawn once per size. */
    static final class Paper extends Drawable {
        private final float d, radius;
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        private Bitmap cache;
        private int alpha = 255;

        Paper(float density, float radiusPx) {
            d = density;
            radius = radiusPx;
        }

        @Override
        protected void onBoundsChange(@NonNull Rect b) {
            if (cache != null) cache.recycle();
            cache = null;
        }

        @Override
        public void draw(@NonNull Canvas c) {
            Rect b = getBounds();
            if (b.isEmpty()) return;
            if (cache == null) cache = render(b.width(), b.height());
            p.setAlpha(alpha);
            c.drawBitmap(cache, b.left, b.top, p);
        }

        private Bitmap render(int w, int h) {
            Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(bmp);
            Paint q = new Paint(Paint.ANTI_ALIAS_FLAG);
            RectF all = new RectF(0, 0, w, h);
            Path clip = new Path();
            clip.addRoundRect(all, radius, radius, Path.Direction.CW);
            c.clipPath(clip);
            // Paper lit from the upper left, darker and warmer to the lower right.
            q.setColor(0xFF000000);
            q.setShader(new LinearGradient(0, 0, w, h, new int[]{0xFFFFF5D8, 0xFFF1D9A0, 0xFFD8B270},
                    new float[]{0f, 0.55f, 1f}, Shader.TileMode.CLAMP));
            c.drawRect(all, q);
            q.setShader(null);
            // Stains: soft blotches a few dp across, darker and lighter (fine speckles shimmered on a 720p panel).
            Random rnd = new Random(23);
            int n = Math.max(4, Math.round(w * h / (900f * d * d)));
            for (int i = 0; i < n; i++) {
                float x = rnd.nextFloat() * w, y = rnd.nextFloat() * h;
                float rad = (6f + rnd.nextFloat() * 10f) * d;
                q.setColor(0xFF000000);
                q.setShader(new RadialGradient(x, y, rad, rnd.nextInt(3) == 0 ? 0x30FFFFFF : 0x1C8A5A2A, 0x00FFFFFF,
                        Shader.TileMode.CLAMP));
                c.drawCircle(x, y, rad, q);
            }
            q.setShader(null);
            q.setStyle(Paint.Style.STROKE);
            // A couple of faint creases, as on a folded poster.
            q.setStrokeWidth(Math.max(1f, 0.8f * d));
            q.setColor(0x2A5A3A1E);
            c.drawLine(w * 0.5f, 0, w * 0.47f, h, q);
            q.setColor(0x33FFFFFF);
            c.drawLine(w * 0.5f + 1.2f * d, 0, w * 0.47f + 1.2f * d, h, q);
            q.setStyle(Paint.Style.FILL);
            // Toasted edges: dark brown fading in from every side.
            float edge = Math.min(w, h) * 0.16f;
            q.setColor(0xFF000000);
            q.setShader(new LinearGradient(0, 0, edge, 0, 0x667A4A18, 0x007A4A18, Shader.TileMode.CLAMP));
            c.drawRect(0, 0, edge, h, q);
            q.setShader(new LinearGradient(w, 0, w - edge, 0, 0x777A4A18, 0x007A4A18, Shader.TileMode.CLAMP));
            c.drawRect(w - edge, 0, w, h, q);
            q.setShader(new LinearGradient(0, 0, 0, edge, 0x557A4A18, 0x007A4A18, Shader.TileMode.CLAMP));
            c.drawRect(0, 0, w, edge, q);
            q.setShader(new LinearGradient(0, h, 0, h - edge, 0x777A4A18, 0x007A4A18, Shader.TileMode.CLAMP));
            c.drawRect(0, h - edge, w, h, q);
            q.setShader(null);
            // Cel shadow along the lower right, hatched like a manga panel.
            Path shade = new Path();
            shade.addRoundRect(all, radius, radius, Path.Direction.CW);
            Path lit = new Path();
            lit.addRoundRect(all, radius, radius, Path.Direction.CW);
            lit.offset(-5f * d, -6f * d);
            c.save();
            c.clipPath(shade);
            c.clipOutPath(lit);
            q.setColor(0x30542E0E);
            c.drawRect(all, q);
            q.setStyle(Paint.Style.STROKE);
            q.setStrokeWidth(Math.max(1f, 0.8f * d));
            q.setColor(0x303B2412);
            for (float s = 0; s < w + h; s += Math.max(4f, 4f * d)) c.drawLine(s, h, s - h, 0, q);
            c.restore();
            return bmp;
        }

        @Override public void setAlpha(int a) { alpha = a; invalidateSelf(); }
        @Override public void setColorFilter(@Nullable ColorFilter cf) { p.setColorFilter(cf); invalidateSelf(); }
        @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
    }
}
