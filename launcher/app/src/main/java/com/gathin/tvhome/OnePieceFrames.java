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

    /**
     * The focus mark inside a map (the parchment menus, panels and dialogs): a loop of dark ink drawn by hand round the
     * choice, as a route is marked on a treasure map, and a small X at its end. On {@link #play()} the brush draws the
     * loop, then the X pops in. Apart from the gold rope of Home's tiles on purpose.
     */
    static final class InkMark extends Drawable {
        /** Sumi ink, the near-black brown of the icons' lines: neutral on the parchment. */
        private static final int MARK_INK = 0xFF2A1A0E;
        private final float d;
        private final Paint ink = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path loop = new Path(), part = new Path();
        private final android.graphics.PathMeasure measure = new android.graphics.PathMeasure();
        private final RectF r = new RectF();
        private final ValueAnimator move = ValueAnimator.ofFloat(0f, 1f);
        private float k = 1f;
        private float length;

        InkMark(float density) {
            d = density;
            ink.setStyle(Paint.Style.STROKE);
            ink.setStrokeCap(Paint.Cap.ROUND);
            ink.setStrokeJoin(Paint.Join.ROUND);
            move.setDuration(520);
            move.setInterpolator(new android.view.animation.DecelerateInterpolator(1.4f));
            move.addUpdateListener(a -> {
                k = (float) a.getAnimatedValue();
                invalidateSelf();
            });
        }

        /** Draws the mark again from the start. */
        void play() {
            move.cancel();
            k = 0f;
            move.start();
        }

        /** Plays {@code d}'s move when it is an ink mark; anything else is left alone. */
        static void playIfMark(Drawable d) {
            if (d instanceof InkMark) ((InkMark) d).play();
        }

        @Override
        protected void onBoundsChange(@NonNull Rect b) {
            // A loose loop a little inside the bounds, its ends overlapping past the start as a quick brush does.
            float in = 3f * d;
            r.set(b.left + in, b.top + in, b.right - in, b.bottom - in);
            float w = r.width(), h = r.height(), rad = Math.min(h / 2f, 16f * d);
            loop.reset();
            loop.moveTo(r.left + rad * 1.2f, r.top + h * 0.06f);
            loop.lineTo(r.right - rad, r.top);
            loop.quadTo(r.right + 1.5f * d, r.top + h * 0.04f, r.right, r.top + h * 0.5f);
            loop.quadTo(r.right - 0.5f * d, r.bottom, r.right - rad, r.bottom - h * 0.02f);
            loop.lineTo(r.left + rad, r.bottom);
            loop.quadTo(r.left - 1f * d, r.bottom - h * 0.04f, r.left, r.top + h * 0.48f);
            loop.quadTo(r.left + 0.5f * d, r.top + h * 0.02f, r.left + rad * 0.9f + w * 0.04f, r.top - 1.2f * d);
            measure.setPath(loop, false);
            length = measure.getLength();
        }

        @Override
        public void draw(@NonNull Canvas c) {
            Rect b = getBounds();
            if (b.isEmpty()) return;
            // No wash: the mark is drawn over the row's icon and text, so it only lines round them and never tints them.
            float drawn = Math.min(1f, k / 0.8f);
            part.reset();
            measure.getSegment(0f, length * drawn, part, true);
            // A soft halo under the line, then the line, heavier in the middle of the stroke as a brush.
            ink.setColor(MARK_INK);
            ink.setAlpha(0x38);
            ink.setStrokeWidth(4.2f * d);
            c.drawPath(part, ink);
            ink.setAlpha(0xE6);
            ink.setStrokeWidth(2f * d);
            c.drawPath(part, ink);
            // The X at the loop's end, popping in once the loop is done.
            if (k > 0.75f) {
                float p = (k - 0.75f) / 0.25f;
                float pop = 1f + 0.35f * (float) Math.sin(p * Math.PI);
                float x = r.right - 4f * d, y = r.top + 3f * d, s = 3.6f * d * pop * Math.min(1f, p * 1.5f);
                ink.setAlpha(0xF0);
                ink.setStrokeWidth(2.2f * d);
                c.drawLine(x - s, y - s, x + s, y + s, ink);
                c.drawLine(x + s, y - s, x - s, y + s, ink);
            }
        }

        @Override
        public boolean setVisible(boolean visible, boolean restart) {
            // A menu opening focuses its first row before its window shows, and showing hides then shows the mark:
            // the brush picks up where it was rather than leaving the loop undrawn.
            if (!visible) move.cancel();
            else if (k < 1f && !move.isRunning()) {
                move.setCurrentFraction(k);
                move.start();
            }
            return super.setVisible(visible, restart);
        }

        @Override public void setAlpha(int alpha) { }
        @Override public void setColorFilter(@Nullable ColorFilter filter) { }
        @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
    }

    /**
     * The chosen one of a row of choices on a map (Apps per row, Clock...): a gold doubloon stamped in at its right
     * end, landing with a bounce and a turn, then a gleam crossing the plank and a sparkle. It plays when the choice
     * becomes the chosen one (and when the screen opens on it), then rests still: nothing runs while nothing changes.
     */
    static final class ChosenMark extends Drawable {
        private static final int INK = 0xFF2A1A0E, GOLD = 0xFFFFCD00, GOLD_DARK = 0xFFC98A12, SHINE = 0xFFFFF6C8;
        private final float d;
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path path = new Path();
        private final RectF r = new RectF();
        private final ValueAnimator move = ValueAnimator.ofFloat(0f, 1f);
        private boolean chosen;
        private float k = 1f;

        ChosenMark(float density) {
            d = density;
            move.setDuration(1100);
            move.setInterpolator(new LinearInterpolator());
            move.addUpdateListener(a -> {
                k = (float) a.getAnimatedValue();
                invalidateSelf();
            });
        }

        @Override
        public boolean isStateful() {
            return true;
        }

        @Override
        protected boolean onStateChange(@NonNull int[] state) {
            boolean now = false;
            for (int s : state) now |= s == android.R.attr.state_selected;
            if (now == chosen) return false;
            chosen = now;
            if (chosen) {
                k = 0f;
                move.cancel();
                move.start();
            } else {
                move.cancel();
            }
            invalidateSelf();
            return true;
        }

        @Override
        public boolean setVisible(boolean visible, boolean restart) {
            if (!visible) move.cancel();
            else if (chosen && k < 1f && !move.isRunning()) {
                move.setCurrentFraction(k);
                move.start();
            }
            return super.setVisible(visible, restart);
        }

        @Override
        public void draw(@NonNull Canvas c) {
            if (!chosen) return;
            Rect b = getBounds();
            float rad = Math.min(9f * d, b.height() * 0.3f);
            float cx = b.right - 16f * d - rad, cy = b.exactCenterY();
            // The gleam: a light band crossing the plank after the stamp lands.
            if (k > 0.4f && k < 0.95f) {
                float g = (k - 0.4f) / 0.55f;
                int save = c.save();
                float rr = Math.min(b.height() / 2f, 14f * d);
                r.set(b);
                path.reset();
                path.addRoundRect(r, rr, rr, Path.Direction.CW);
                c.clipPath(path);
                float x = b.left - 40f * d + (b.width() + 80f * d) * g;
                p.setStyle(Paint.Style.FILL);
                p.setShader(new LinearGradient(x - 26f * d, 0, x + 26f * d, 0,
                        new int[]{0x00FFFFFF, 0x99FFF6C8, 0x00FFFFFF}, null, Shader.TileMode.CLAMP));
                c.save();
                c.skew(-0.35f, 0f);
                c.drawRect(x - 26f * d + b.exactCenterY() * 0.35f, b.top, x + 26f * d + b.bottom * 0.35f, b.bottom, p);
                c.restore();
                p.setShader(null);
                c.restoreToCount(save);
            }
            // The doubloon: stamped from large and turned, settling with a bounce.
            float land = Math.min(1f, k / 0.4f);
            float scale = 1f + 0.9f * (1f - land) - 0.18f * (float) Math.sin(land * Math.PI) * (land > 0.6f ? 1f : 0f);
            float turn = -50f * (1f - land);
            int alpha = (int) (255 * Math.min(1f, land * 2.5f));
            c.save();
            c.translate(cx, cy);
            c.rotate(turn);
            c.scale(scale, scale);
            p.setStyle(Paint.Style.FILL);
            p.setShader(new RadialGradient(-rad * 0.35f, -rad * 0.35f, rad * 1.5f,
                    new int[]{SHINE, GOLD, GOLD_DARK}, new float[]{0f, 0.45f, 1f}, Shader.TileMode.CLAMP));
            p.setAlpha(alpha);
            c.drawCircle(0, 0, rad, p);
            p.setShader(null);
            p.setStyle(Paint.Style.STROKE);
            p.setColor(INK);
            p.setAlpha(alpha * 0xB0 / 255);
            p.setStrokeWidth(1.3f * d);
            c.drawCircle(0, 0, rad, p);
            p.setStrokeWidth(0.8f * d);
            p.setAlpha(alpha * 0x70 / 255);
            c.drawCircle(0, 0, rad * 0.74f, p);
            // A five-point star struck in the middle.
            path.reset();
            for (int i = 0; i < 10; i++) {
                double a = Math.toRadians(-90 + i * 36);
                float rr = (i % 2 == 0 ? 0.52f : 0.22f) * rad;
                float x = (float) Math.cos(a) * rr, y = (float) Math.sin(a) * rr;
                if (i == 0) path.moveTo(x, y); else path.lineTo(x, y);
            }
            path.close();
            p.setStyle(Paint.Style.FILL);
            p.setColor(INK);
            p.setAlpha(alpha * 0xC8 / 255);
            c.drawPath(path, p);
            c.restore();
            // A sparkle over the coin as the gleam passes.
            if (k > 0.55f && k < 1f) {
                float sp = (float) Math.sin((k - 0.55f) / 0.45f * Math.PI) * 4.5f * d;
                float sx = cx + rad * 0.8f, sy = cy - rad * 0.9f;
                path.reset();
                path.moveTo(sx, sy - sp);
                path.quadTo(sx, sy, sx + sp, sy);
                path.quadTo(sx, sy, sx, sy + sp);
                path.quadTo(sx, sy, sx - sp, sy);
                path.quadTo(sx, sy, sx, sy - sp);
                p.setColor(SHINE);
                p.setAlpha(255);
                c.drawPath(path, p);
            }
        }

        @Override public void setAlpha(int alpha) { }
        @Override public void setColorFilter(@Nullable ColorFilter filter) { }
        @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
    }

    /**
     * A choice on the settings map as a scrap of the map itself, drawn as the skin's icons are: torn, uneven edges
     * and a dog-eared corner showing the paper's back, toasted edges, a hard cel shadow down and to the right, and a
     * brush-ink outline heavier on the shadow side. The chosen choice is gold-leaf paper with a gleam; focus lightens
     * the paper (the map's ink mark circles it, and the view lifts it a little).
     */
    static final class PaperTag extends Drawable {
        private static final int INK = 0xFF2A1A0E;
        private final float d;
        private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint line = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path shape = new Path(), shadow = new Path(), ear = new Path(), earBack = new Path();
        private final RectF box = new RectF();
        private final long seed;
        private boolean focused, chosen;

        PaperTag(float density) {
            d = density;
            line.setStyle(Paint.Style.STROKE);
            line.setStrokeCap(Paint.Cap.ROUND);
            line.setStrokeJoin(Paint.Join.ROUND);
            seed = System.identityHashCode(this);
        }

        @Override public boolean isStateful() { return true; }

        @Override
        protected boolean onStateChange(@NonNull int[] state) {
            boolean f = false, c = false;
            for (int s : state) {
                f |= s == android.R.attr.state_focused;
                c |= s == android.R.attr.state_selected;
            }
            if (f == focused && c == chosen) return false;
            focused = f;
            chosen = c;
            invalidateSelf();
            return true;
        }

        /** The torn outline: jagged top and bottom edges, slightly wavering sides, the top right corner folded in. */
        @Override
        protected void onBoundsChange(@NonNull Rect b) {
            Random rnd = new Random(seed);
            box.set(b.left + 3f * d, b.top + 3f * d, b.right - 6f * d, b.bottom - 7f * d);
            float fold = Math.min(box.height() * 0.42f, 13f * d);
            float step = 9f * d, tear = 1.6f * d;
            shape.reset();
            shape.moveTo(box.left + rnd.nextFloat() * tear, box.top + rnd.nextFloat() * tear);
            for (float x = box.left + step; x < box.right - fold; x += step * (0.6f + rnd.nextFloat() * 0.8f)) {
                shape.lineTo(x, box.top + (rnd.nextFloat() - 0.3f) * tear);
            }
            shape.lineTo(box.right - fold, box.top);                 // the fold's edge
            shape.lineTo(box.right, box.top + fold);
            for (float y = box.top + fold + step; y < box.bottom - step * 0.5f; y += step * (0.7f + rnd.nextFloat())) {
                shape.lineTo(box.right + (rnd.nextFloat() - 0.5f) * tear, y);
            }
            shape.lineTo(box.right - rnd.nextFloat() * tear, box.bottom - rnd.nextFloat() * tear);
            for (float x = box.right - step; x > box.left + step * 0.5f; x -= step * (0.6f + rnd.nextFloat() * 0.8f)) {
                shape.lineTo(x, box.bottom + (rnd.nextFloat() - 0.7f) * tear);
            }
            shape.lineTo(box.left + rnd.nextFloat() * tear, box.bottom - rnd.nextFloat() * tear);
            for (float y = box.bottom - step; y > box.top + step * 0.5f; y -= step * (0.7f + rnd.nextFloat())) {
                shape.lineTo(box.left + (rnd.nextFloat() - 0.5f) * tear, y);
            }
            shape.close();
            shadow.set(shape);
            shadow.offset(3f * d, 4f * d);
            // The folded-down corner: the paper's back, a triangle lying over the corner.
            ear.reset();
            ear.moveTo(box.right - fold, box.top);
            ear.lineTo(box.right, box.top + fold);
            ear.lineTo(box.right - fold * 0.92f, box.top + fold * 0.9f);
            ear.close();
            earBack.reset();
            earBack.moveTo(box.right - fold, box.top);
            earBack.lineTo(box.right, box.top);
            earBack.lineTo(box.right, box.top + fold);
            earBack.close();
        }

        @Override
        public void draw(@NonNull Canvas c) {
            if (getBounds().isEmpty()) return;
            // The hard cel shadow under the scrap.
            fill.setShader(null);
            fill.setColor(0x6A2A1A0E);
            c.drawPath(shadow, fill);
            // Paper: cream, gold leaf when chosen, lighter under focus.
            int top, mid, bottom;
            if (chosen) {
                top = focused ? 0xFFFFF4C4 : 0xFFFFE68C;
                mid = focused ? 0xFFFFDD6A : 0xFFF7C948;
                bottom = 0xFFD9A034;
            } else {
                top = focused ? 0xFFFFFCF0 : 0xFFFBEFD2;
                mid = focused ? 0xFFF8EBCB : 0xFFF0DDB2;
                bottom = focused ? 0xFFEBD3A0 : 0xFFDDBF86;
            }
            fill.setColor(0xFF000000);   // opaque: a paint's alpha also fades its shader
            fill.setShader(new LinearGradient(0, box.top, 0, box.bottom, new int[]{top, mid, bottom},
                    new float[]{0f, 0.55f, 1f}, Shader.TileMode.CLAMP));
            c.drawPath(shape, fill);
            fill.setShader(null);
            int save = c.save();
            c.clipPath(shape);
            // Stains and a toasted edge, inside the scrap only.
            Random rnd = new Random(seed + 3);
            for (int i = 0; i < 3; i++) {
                float x = box.left + box.width() * (0.12f + 0.76f * rnd.nextFloat());
                float y = box.top + box.height() * (0.2f + 0.6f * rnd.nextFloat());
                float sr = box.height() * (0.3f + 0.3f * rnd.nextFloat());
                fill.setShader(new RadialGradient(x, y, sr, chosen ? 0x22A06010 : 0x228A5A1A, 0x008A5A1A,
                        Shader.TileMode.CLAMP));
                c.drawCircle(x, y, sr, fill);
            }
            fill.setShader(null);
            line.setColor(chosen ? 0x66A0601A : 0x5C7A4A1A);
            line.setStrokeWidth(7f * d);
            c.drawPath(shape, line);
            line.setColor(chosen ? 0x337A4010 : 0x2E5A3414);
            line.setStrokeWidth(3f * d);
            c.drawPath(shape, line);
            if (chosen) {
                // Gold leaf catches the light: a soft diagonal gleam.
                fill.setShader(new LinearGradient(box.left, box.top, box.left + box.height() * 2.2f, box.bottom,
                        new int[]{0x00FFFFFF, 0x66FFFFFF, 0x00FFFFFF}, null, Shader.TileMode.CLAMP));
                c.drawRect(box, fill);
                fill.setShader(null);
            }
            // The cel shade on the lower right, as the icons have.
            fill.setColor(chosen ? 0x26A05A10 : 0x1E5A3414);
            c.drawRect(box.left, box.bottom - box.height() * 0.22f, box.right, box.bottom, fill);
            c.restoreToCount(save);
            // The dog-ear: the corner torn off the front, the back of the paper folded over it.
            fill.setColor(0xFF000000);
            fill.setShader(new LinearGradient(box.right, box.top, box.right - 10f * d, box.top + 10f * d,
                    chosen ? 0xFFE0A63A : 0xFFD9BC88, chosen ? 0xFFFFE9A0 : 0xFFF6E7C6, Shader.TileMode.CLAMP));
            c.drawPath(ear, fill);
            fill.setShader(null);
            // The brush-ink outline: a soft halo, the line, and a heavier pass on the shadow side.
            line.setColor(INK);
            line.setAlpha(0x2E);
            line.setStrokeWidth((chosen ? 4.4f : 3.6f) * d);
            c.drawPath(shape, line);
            line.setAlpha(chosen ? 0xF0 : 0xD8);
            line.setStrokeWidth((chosen ? 2.1f : 1.6f) * d);
            c.drawPath(shape, line);
            save = c.save();
            c.translate(0.7f * d, 0.9f * d);
            c.clipRect(box.left + box.width() * 0.5f, box.top + box.height() * 0.5f, box.right + 4f * d,
                    box.bottom + 4f * d);
            line.setAlpha(0x8C);
            line.setStrokeWidth(1.2f * d);
            c.drawPath(shape, line);
            c.restoreToCount(save);
            line.setAlpha(0xC8);
            line.setStrokeWidth(1.2f * d);
            c.drawPath(ear, line);
        }

        @Override public void setAlpha(int alpha) { }
        @Override public void setColorFilter(@Nullable ColorFilter filter) { }
        @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
    }

    /**
     * A treasure map's edge for a parchment sheet, in place of a wooden frame: the paper toasted dark toward its
     * edge, a double ink rule round it (a heavy line and a fine one inside), and a small compass star at each corner.
     */
    static final class MapEdge extends Drawable {
        private static final int INK = 0xFF3A2414;
        private final float d, radius;
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF r = new RectF();
        private final Path star = new Path();

        /** Compact: a card's edge (Continue watching), drawn over its picture, so lighter and closer to the rim. */
        private final boolean compact;

        MapEdge(float density, float radiusPx) {
            this(density, radiusPx, false);
        }

        MapEdge(float density, float radiusPx, boolean compact) {
            d = density;
            radius = radiusPx;
            this.compact = compact;
            p.setStrokeJoin(Paint.Join.ROUND);
            p.setStrokeCap(Paint.Cap.ROUND);
        }

        @Override
        public void draw(@NonNull Canvas c) {
            Rect b = getBounds();
            if (b.isEmpty()) return;
            p.setStyle(Paint.Style.STROKE);
            // Toasted edge: wide soft bands of brown, darker at the very edge.
            float[] widths = compact ? new float[]{7f, 3f} : new float[]{16f, 9f, 4f};
            int[] colors = compact ? new int[]{0x405A2E0C, 0x663A1E08} : new int[]{0x1F6B3A10, 0x2E5A2E0C, 0x4D3A1E08};
            for (int i = 0; i < widths.length; i++) {
                p.setColor(colors[i]);
                p.setStrokeWidth(widths[i] * d);
                r.set(b);
                c.drawRoundRect(r, radius, radius, p);
            }
            // The paper's own edge, inked.
            p.setColor(INK);
            p.setAlpha(0xC0);
            p.setStrokeWidth(1.4f * d);
            r.set(b.left + 0.7f * d, b.top + 0.7f * d, b.right - 0.7f * d, b.bottom - 0.7f * d);
            c.drawRoundRect(r, radius, radius, p);
            // The map's double rule.
            float in1 = (compact ? 3.2f : 5f) * d, in2 = (compact ? 5.4f : 8f) * d;
            float r1 = Math.max(0f, radius - in1), r2 = Math.max(0f, radius - in2);
            p.setAlpha(0xB0);
            p.setStrokeWidth(1.6f * d);
            r.set(b.left + in1, b.top + in1, b.right - in1, b.bottom - in1);
            c.drawRoundRect(r, r1, r1, p);
            p.setAlpha(0x80);
            p.setStrokeWidth(0.8f * d);
            r.set(b.left + in2, b.top + in2, b.right - in2, b.bottom - in2);
            c.drawRoundRect(r, r2, r2, p);
            // A compass star in each corner, on the rule.
            float k = Math.max(in1, radius * 0.3f) + 1f * d, sz = (compact ? 3.6f : 5f) * d;
            float[][] at = {{b.left + k, b.top + k}, {b.right - k, b.top + k}, {b.left + k, b.bottom - k},
                    {b.right - k, b.bottom - k}};
            for (float[] xy : at) compass(c, xy[0], xy[1], sz);
        }

        /** A four-point compass star: gold, inked, with a fine cross through it. */
        private void compass(Canvas c, float x, float y, float s) {
            float in = s * 0.28f;
            star.reset();
            star.moveTo(x, y - s);
            star.lineTo(x + in, y - in);
            star.lineTo(x + s, y);
            star.lineTo(x + in, y + in);
            star.lineTo(x, y + s);
            star.lineTo(x - in, y + in);
            star.lineTo(x - s, y);
            star.lineTo(x - in, y - in);
            star.close();
            p.setStyle(Paint.Style.FILL);
            p.setColor(0xFFE8B84A);
            c.drawPath(star, p);
            p.setStyle(Paint.Style.STROKE);
            p.setColor(INK);
            p.setAlpha(0xD0);
            p.setStrokeWidth(0.9f * d);
            c.drawPath(star, p);
            p.setAlpha(0x90);
            p.setStrokeWidth(0.6f * d);
            c.drawLine(x - s * 0.6f, y - s * 0.6f, x + s * 0.6f, y + s * 0.6f, p);
            c.drawLine(x + s * 0.6f, y - s * 0.6f, x - s * 0.6f, y + s * 0.6f, p);
            p.setStyle(Paint.Style.FILL);
            p.setColor(INK);
            c.drawCircle(x, y, 0.9f * d, p);
        }

        @Override public void setAlpha(int alpha) { }
        @Override public void setColorFilter(@Nullable ColorFilter filter) { }
        @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
    }

    /**
     * The settings map's focus mark, made for the paper scraps: a brush stroke of ink swept under the text from left
     * to right, thick in the middle and tapering at both ends, then a small red X popping in at its end, as a spot
     * marked on a map. {@link #play()} draws it again.
     */
    static final class InkUnderline extends Drawable {
        private static final int INK = 0xFF2A1A0E, RED = 0xFFC0392B;
        private final float d;
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path stroke = new Path();
        private final ValueAnimator move = ValueAnimator.ofFloat(0f, 1f);
        private float k = 1f;
        /** The choice is also the chosen one: its doubloon sits at the right end, so the stroke stops short of it. */
        private boolean chosen;

        InkUnderline(float density) {
            d = density;
            p.setStrokeCap(Paint.Cap.ROUND);
            move.setDuration(480);
            move.setInterpolator(new android.view.animation.DecelerateInterpolator(1.6f));
            move.addUpdateListener(a -> {
                k = (float) a.getAnimatedValue();
                invalidateSelf();
            });
        }

        void play() {
            move.cancel();
            k = 0f;
            move.start();
        }

        static void playIfUnderline(Drawable d) {
            if (d instanceof InkUnderline) ((InkUnderline) d).play();
        }

        @Override
        public boolean setVisible(boolean visible, boolean restart) {
            if (!visible) move.cancel();
            else if (k < 1f && !move.isRunning()) {
                move.setCurrentFraction(k);
                move.start();
            }
            return super.setVisible(visible, restart);
        }

        @Override public boolean isStateful() { return true; }

        @Override
        protected boolean onStateChange(@NonNull int[] state) {
            boolean c = false;
            for (int s : state) c |= s == android.R.attr.state_selected;
            if (c == chosen) return false;
            chosen = c;
            onBoundsChange(getBounds());
            invalidateSelf();
            return true;
        }

        /** Where the stroke ends: clear of the doubloon (ChosenMark) when the choice is the chosen one. */
        private float right(Rect b) {
            return b.right - (chosen ? 72f : 30f) * d;
        }

        @Override
        protected void onBoundsChange(@NonNull Rect b) {
            float left = b.left + 14f * d, right = right(b);
            float y = b.top + b.height() * 0.76f, half = 2.1f * d;
            // A tapered brush mark: the upper edge bows up a little, the lower one down, meeting at points.
            stroke.reset();
            stroke.moveTo(left, y + 0.4f * d);
            stroke.cubicTo(left + (right - left) * 0.3f, y - half * 1.2f, left + (right - left) * 0.7f, y - half,
                    right, y - 0.6f * d);
            stroke.cubicTo(left + (right - left) * 0.72f, y + half * 0.9f, left + (right - left) * 0.3f,
                    y + half * 1.1f, left, y + 0.4f * d);
            stroke.close();
        }

        @Override
        public void draw(@NonNull Canvas c) {
            Rect b = getBounds();
            if (b.isEmpty()) return;
            float left = b.left + 14f * d, right = right(b);
            float drawn = Math.min(1f, k / 0.75f);
            int save = c.save();
            c.clipRect(left - 2f * d, b.top, left + (right - left) * drawn + 2f * d, b.bottom);
            p.setStyle(Paint.Style.FILL);
            p.setColor(INK);
            p.setAlpha(0xE0);
            c.drawPath(stroke, p);
            c.restoreToCount(save);
            if (k > 0.7f) {
                float t = (k - 0.7f) / 0.3f;
                float pop = 1f + 0.4f * (float) Math.sin(t * Math.PI);
                float x = right + 13f * d, y = b.top + b.height() * 0.5f, s = 5f * d * pop * Math.min(1f, t * 1.6f);
                p.setStyle(Paint.Style.STROKE);
                p.setColor(RED);
                p.setStrokeWidth(2.6f * d);
                c.drawLine(x - s, y - s, x + s, y + s, p);
                c.drawLine(x + s, y - s, x - s, y + s, p);
            }
        }

        @Override public void setAlpha(int alpha) { }
        @Override public void setColorFilter(@Nullable ColorFilter filter) { }
        @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
    }

    /**
     * Focus on a Continue watching card (a card of the map, kept clear of its edge): a gold gleam sweeps across it on
     * the diagonal, and a red X, the spot marked on a treasure map, is stamped in its top right corner with a bounce
     * and stays while the card has focus.
     */
    static final class CardMark extends Drawable {
        private static final int RED = 0xFFC0392B, INK = 0xFF2A1A0E;
        private final float d, radius;
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path clip = new Path();
        private final RectF r = new RectF();
        private final ValueAnimator move = ValueAnimator.ofFloat(0f, 1f);
        private float k = 1f;

        CardMark(float density, float radiusPx) {
            d = density;
            radius = radiusPx;
            p.setStrokeCap(Paint.Cap.ROUND);
            move.setDuration(900);
            move.setInterpolator(new LinearInterpolator());
            move.addUpdateListener(a -> {
                k = (float) a.getAnimatedValue();
                invalidateSelf();
            });
        }

        void play() {
            move.cancel();
            k = 0f;
            move.start();
        }

        static void playIfCard(Drawable d) {
            if (d instanceof CardMark) ((CardMark) d).play();
        }

        @Override
        public boolean setVisible(boolean visible, boolean restart) {
            if (!visible) move.cancel();
            else if (k < 1f && !move.isRunning()) {
                move.setCurrentFraction(k);
                move.start();
            }
            return super.setVisible(visible, restart);
        }

        @Override
        public void draw(@NonNull Canvas c) {
            Rect b = getBounds();
            if (b.isEmpty()) return;
            // The gleam, inside the card's rounded shape.
            float g = Math.min(1f, k / 0.65f);
            if (g < 1f) {
                int save = c.save();
                r.set(b);
                clip.reset();
                clip.addRoundRect(r, radius, radius, Path.Direction.CW);
                c.clipPath(clip);
                float w = b.height() * 0.5f, x = b.left - w * 2f + (b.width() + w * 4f) * g;
                p.setStyle(Paint.Style.FILL);
                p.setShader(new LinearGradient(x, b.top, x + w, b.top + w * 0.7f,
                        new int[]{0x00FFE9A0, 0x8CFFE9A0, 0x00FFE9A0}, null, Shader.TileMode.CLAMP));
                c.drawRect(b, p);
                p.setShader(null);
                c.restoreToCount(save);
            }
            // The X stamp, in from large with a bounce, then still.
            float t = Math.max(0f, Math.min(1f, (k - 0.15f) / 0.4f));
            if (t > 0f) {
                float pop = t < 1f ? 1f + 0.9f * (1f - t) - 0.25f * (float) Math.sin(t * Math.PI) : 1f;
                float s = 6.5f * d * pop, x = b.right - 16f * d, y = b.top + 16f * d;
                p.setStyle(Paint.Style.STROKE);
                p.setColor(0xFFFFF4D8);          // a pale edge so it reads on any picture
                p.setAlpha((int) (230 * Math.min(1f, t * 2f)));
                p.setStrokeWidth(5.4f * d);
                c.drawLine(x - s, y - s, x + s, y + s, p);
                c.drawLine(x + s, y - s, x - s, y + s, p);
                p.setColor(INK);
                p.setAlpha((int) (255 * Math.min(1f, t * 2f)));
                p.setStrokeWidth(4f * d);
                c.drawLine(x - s, y - s, x + s, y + s, p);
                c.drawLine(x + s, y - s, x - s, y + s, p);
                p.setColor(RED);
                p.setStrokeWidth(2.6f * d);
                c.drawLine(x - s, y - s, x + s, y + s, p);
                c.drawLine(x + s, y - s, x - s, y + s, p);
            }
        }

        @Override public void setAlpha(int alpha) { }
        @Override public void setColorFilter(@Nullable ColorFilter filter) { }
        @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
    }
}
