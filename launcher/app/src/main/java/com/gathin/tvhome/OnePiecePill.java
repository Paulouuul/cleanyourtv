package com.gathin.tvhome;

import android.animation.ValueAnimator;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.view.animation.DecelerateInterpolator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Random;

/**
 * The One Piece skin's top-bar pill, drawn like an anime cel with a manga ink line: a wooden rim with grain in a
 * gradient, a parchment face with paper grain and toasted edges, a cel shadow hatched in ink on the lower right, a
 * glossy highlight on the upper left and a brush outline that is heavier on the shadow side. Focused it turns to
 * sun gold, and manga focus lines open and turn behind the icon while a shine sweeps across and a star twinkles.
 * The still layers are drawn once per size and state into a bitmap; only the focus move is drawn per frame.
 */
final class OnePiecePill extends Drawable {

    private static final int REST = 0, FOCUSED = 1, ACTIVATED = 2;
    private static final int INK = 0xFF24150A;

    private final float d;
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final Paint fx = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF outer = new RectF(), inner = new RectF();
    private final Path path = new Path(), path2 = new Path();
    private final Bitmap[] cache = new Bitmap[3];
    private final ValueAnimator focusMove = ValueAnimator.ofFloat(0f, 1f);
    private int state = REST;
    /** The focus move's progress; 1 once played (the lines stay, still). */
    private float t = 1f;
    private int alpha = 255;
    private boolean reduced;

    OnePiecePill(float density) {
        d = density;
        focusMove.setDuration(950);
        focusMove.setInterpolator(new DecelerateInterpolator(1.4f));
        focusMove.addUpdateListener(a -> {
            t = (float) a.getAnimatedValue();
            invalidateSelf();
        });
    }

    void setReducedMotion(boolean reducedMotion) {
        reduced = reducedMotion;
    }

    @Override
    public boolean isStateful() {
        return true;
    }

    @Override
    protected boolean onStateChange(@NonNull int[] states) {
        int next = REST;
        for (int s : states) {
            if (s == android.R.attr.state_focused) { next = FOCUSED; break; }
            if (s == android.R.attr.state_activated) next = ACTIVATED;
        }
        if (next == state) return false;
        state = next;
        focusMove.cancel();
        if (state == FOCUSED && !reduced) {
            t = 0f;
            focusMove.start();
        } else {
            t = 1f;
        }
        invalidateSelf();
        return true;
    }

    @Override
    protected void onBoundsChange(@NonNull Rect bounds) {
        for (int i = 0; i < cache.length; i++) {
            if (cache[i] != null) cache[i].recycle();
            cache[i] = null;
        }
    }

    @Override
    public void draw(@NonNull Canvas c) {
        Rect b = getBounds();
        if (b.width() <= 0 || b.height() <= 0) return;
        if (cache[state] == null) cache[state] = render(b.width(), b.height(), state);
        p.setAlpha(alpha);
        c.drawBitmap(cache[state], b.left, b.top, p);
        if (state == FOCUSED) {
            int save = c.save();
            c.translate(b.left, b.top);
            focusLines(c, b.width(), b.height());
            c.restoreToCount(save);
        }
    }

    // ---- the still layers ----

    private void shapes(int w, int h) {
        float pad = 2f * d;
        outer.set(pad, pad, w - pad, h - pad - 1.2f * d);   // room for the cel drop shadow under it
        float rim = Math.max(2.5f * d, Math.min(outer.height(), outer.width()) * 0.075f);
        inner.set(outer.left + rim, outer.top + rim, outer.right - rim, outer.bottom - rim);
    }

    private static float radius(RectF r) {
        return Math.min(r.height(), r.width()) / 2f;
    }

    private Bitmap render(int w, int h, int which) {
        Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bmp);
        shapes(w, h);
        Paint q = new Paint(Paint.ANTI_ALIAS_FLAG);
        float ro = radius(outer), ri = radius(inner);

        // Soft drop shadow, blurred (this is a software canvas, so the blur filter works here).
        q.setColor(0x80000814);
        q.setMaskFilter(new android.graphics.BlurMaskFilter(1.6f * d, android.graphics.BlurMaskFilter.Blur.NORMAL));
        c.drawRoundRect(outer.left + 0.4f * d, outer.top + 1.2f * d, outer.right + 0.4f * d, outer.bottom + 1.2f * d,
                ro, ro, q);
        q.setMaskFilter(null);

        // The rim: wood with grain, or polished gold under focus.
        int[] rimColors = which == FOCUSED ? new int[]{0xFFFFF7B0, 0xFFFFC21A, 0xFFD9780A}
                : new int[]{0xFFD9A066, 0xFF9A5E2A, 0xFF5A3214};
        q.setColor(0xFF000000);   // opaque: a paint's alpha also fades its shader
        q.setShader(new LinearGradient(0, outer.top, 0, outer.bottom, rimColors, new float[]{0f, 0.45f, 1f},
                Shader.TileMode.CLAMP));
        c.drawRoundRect(outer, ro, ro, q);
        q.setShader(null);
        if (which != FOCUSED) woodGrain(c, q, ro);

        // The face: parchment, wood (activated) or gold, lit from the upper left.
        int[] face = which == FOCUSED ? new int[]{0xFFFFFDE8, 0xFFFFE066, 0xFFFFA90A}
                : which == ACTIVATED ? new int[]{0xFFC68A52, 0xFF8A5527, 0xFF5C3517}
                : new int[]{0xFFFFF6DC, 0xFFF2DCA4, 0xFFC99A55};
        float gx = inner.left + inner.width() * 0.32f, gy = inner.top + inner.height() * 0.22f;
        float gr = Math.max(inner.width(), inner.height()) * 0.95f;
        q.setColor(0xFF000000);   // opaque: a paint's alpha also fades its shader
        q.setShader(new RadialGradient(gx, gy, gr, face, new float[]{0f, 0.55f, 1f}, Shader.TileMode.CLAMP));
        c.drawRoundRect(inner, ri, ri, q);
        q.setShader(null);

        path.reset();
        path.addRoundRect(inner, ri, ri, Path.Direction.CW);
        int save = c.save();
        c.clipPath(path);
        if (which == REST) paperGrain(c, q);
        // Cel shadow on the lower right: the face minus itself moved up-left, hatched in ink like a manga panel.
        path2.reset();
        path2.addRoundRect(inner, ri, ri, Path.Direction.CW);
        path2.offset(-2.2f * d, -2.6f * d);
        c.clipOutPath(path2);
        q.setColor(which == FOCUSED ? 0x55E06000 : 0x40542E0E);
        c.drawRect(inner, q);
        hatch(c, q, which == FOCUSED ? 0x4DB34700 : 0x403B2412);
        c.restoreToCount(save);

        // Gloss: a bright arc and a dot on the upper left, as anime draws a shine.
        q.setStyle(Paint.Style.STROKE);
        q.setStrokeCap(Paint.Cap.ROUND);
        q.setStrokeWidth(Math.max(1.5f, 1.6f * d));
        q.setColor(0xD8FFFFFF);
        RectF shine = new RectF(inner);
        shine.inset(2.2f * d, 2.2f * d);
        float rs = radius(shine);
        path.reset();
        if (shine.width() - shine.height() > 1f) {
            // A wide pill: along the top edge from the left cap.
            path.addArc(new RectF(shine.left, shine.top, shine.left + 2 * rs, shine.bottom), 195f, 60f);
            path.rLineTo(Math.min(shine.width() * 0.25f, 14 * d), 0f);
        } else {
            path.addArc(shine, 200f, 55f);
        }
        c.drawPath(path, q);
        q.setStyle(Paint.Style.FILL);
        float dot = 1.1f * d;
        c.drawCircle(shine.left + rs * 0.42f + dot * 4.2f, shine.top + rs * 0.42f + dot * 0.4f, dot, q);

        // Ink: a faint line between rim and face.
        q.setStyle(Paint.Style.STROKE);
        q.setStrokeWidth(0.7f * d);
        q.setColor(which == FOCUSED ? 0x888A3B00 : 0x772A170A);
        c.drawRoundRect(inner, ri, ri, q);
        if (which == FOCUSED) {
            q.setColor(0xFFFFFFFF);
            q.setStrokeWidth(1.4f * d);
            RectF white = new RectF(outer);
            white.inset(1.6f * d, 1.6f * d);
            float rw = radius(white);
            c.drawRoundRect(white, rw, rw, q);
        }
        // Outline: a thin warm-brown line, softened by a faint blur round it, instead of a heavy black ring.
        q.setColor(0x553A2414);
        q.setStrokeWidth(1.8f * d);
        q.setMaskFilter(new android.graphics.BlurMaskFilter(0.8f * d, android.graphics.BlurMaskFilter.Blur.NORMAL));
        c.drawRoundRect(outer, ro, ro, q);
        q.setMaskFilter(null);
        q.setColor(0xD93A2414);
        q.setStrokeWidth(0.9f * d);
        c.drawRoundRect(outer, ro, ro, q);
        return bmp;
    }

    /** Dark grain lines that follow the rim round, clipped to the rim. */
    private void woodGrain(Canvas c, Paint q, float ro) {
        int save = c.save();
        path.reset();
        path.addRoundRect(outer, ro, ro, Path.Direction.CW);
        c.clipPath(path);
        path2.reset();
        path2.addRoundRect(inner, radius(inner), radius(inner), Path.Direction.CW);
        c.clipOutPath(path2);
        q.setStyle(Paint.Style.STROKE);
        q.setStrokeWidth(Math.max(1f, 0.8f * d));
        Random rnd = new Random(11);
        for (int i = 0; i < 4; i++) {
            float k = 0.25f + 0.17f * i + rnd.nextFloat() * 0.05f;
            RectF g = new RectF(outer);
            float in = (outer.height() - inner.height()) / 2f * k;
            g.inset(in, in);
            float rg = radius(g);
            q.setColor(i % 2 == 0 ? 0x803A1E08 : 0x55FFE0B0);
            float start = rnd.nextFloat() * 360f;
            path.reset();
            path.addArc(new RectF(g.left, g.top, g.left + 2 * rg, g.bottom), start, 70f + rnd.nextFloat() * 60f);
            c.drawPath(path, q);
            path.reset();
            path.addArc(new RectF(g.right - 2 * rg, g.top, g.right, g.bottom), start + 150f, 60f + rnd.nextFloat() * 50f);
            c.drawPath(path, q);
        }
        q.setStyle(Paint.Style.FILL);
        c.restoreToCount(save);
    }

    /** Paper grain: fixed speckles and short fibres, the same on every pill of a size. */
    private void paperGrain(Canvas c, Paint q) {
        // Soft stains, a few pixels across: a paper look that stays smooth at this size (fine speckles shimmered).
        Random rnd = new Random(7);
        for (int i = 0; i < 4; i++) {
            float x = inner.left + (0.2f + rnd.nextFloat() * 0.6f) * inner.width();
            float y = inner.top + (0.2f + rnd.nextFloat() * 0.6f) * inner.height();
            float rad = (5f + rnd.nextFloat() * 6f) * d;
            q.setColor(0xFF000000);
            q.setShader(new RadialGradient(x, y, rad, i % 2 == 0 ? 0x1E8A5A2A : 0x2AFFFFFF, 0x00FFFFFF,
                    Shader.TileMode.CLAMP));
            c.drawCircle(x, y, rad, q);
            q.setShader(null);
        }
        // Toasted edge: a darker ring fading inwards.
        float ri = radius(inner);
        q.setColor(0xFF000000);   // opaque: a paint's alpha also fades its shader
        q.setShader(new RadialGradient(inner.centerX(), inner.centerY(), Math.max(inner.width(), inner.height()) * 0.62f,
                new int[]{0x00000000, 0x00000000, 0x3A7A4A18}, new float[]{0f, 0.7f, 1f}, Shader.TileMode.CLAMP));
        c.drawRoundRect(inner, ri, ri, q);
        q.setShader(null);
    }

    /** Diagonal manga hatching over whatever the clip leaves. */
    private void hatch(Canvas c, Paint q, int color) {
        q.setStyle(Paint.Style.STROKE);
        q.setStrokeWidth(Math.max(1f, 0.8f * d));
        q.setColor(color);
        float step = Math.max(3.5f, 3.2f * d), span = inner.width() + inner.height();
        for (float s = 0; s < span; s += step) {
            c.drawLine(inner.left + s, inner.bottom, inner.left + s - inner.height(), inner.top, q);
        }
        q.setStyle(Paint.Style.FILL);
    }

    // ---- the focus move ----

    private void focusLines(Canvas c, int w, int h) {
        shapes(w, h);
        float ri = radius(inner);
        path.reset();
        path.addRoundRect(inner, ri, ri, Path.Direction.CW);
        int save = c.save();
        c.clipPath(path);
        float cx = inner.centerX(), cy = inner.centerY();
        float reach = (float) Math.hypot(inner.width(), inner.height()) / 2f;
        // Manga focus lines: thin wedges from the edge in towards the icon, opening and turning as focus lands.
        float open = Math.min(1f, t * 1.6f);
        float turn = 18f * t;
        fx.setStyle(Paint.Style.FILL);
        fx.setColor(0xFFFFF4B0);
        fx.setAlpha((int) (alpha * (0.35f + 0.4f * (1f - t)) * open));
        int rays = 16;
        for (int i = 0; i < rays; i++) {
            double a0 = Math.toRadians(turn + i * 360.0 / rays);
            double half = Math.toRadians(4.5 + (i % 3) * 1.5);
            float in = reach * (0.92f - 0.42f * open) * (i % 2 == 0 ? 1f : 1.12f);
            path2.reset();
            path2.moveTo(cx + (float) Math.cos(a0) * in, cy + (float) Math.sin(a0) * in);
            path2.lineTo(cx + (float) Math.cos(a0 - half) * reach * 1.1f, cy + (float) Math.sin(a0 - half) * reach * 1.1f);
            path2.lineTo(cx + (float) Math.cos(a0 + half) * reach * 1.1f, cy + (float) Math.sin(a0 + half) * reach * 1.1f);
            path2.close();
            c.drawPath(path2, fx);
        }
        // A shine sweeping across, upper left to lower right.
        if (t > 0.05f && t < 0.85f) {
            float k = (t - 0.05f) / 0.8f;
            float x = inner.left - inner.height() + (inner.width() + 2 * inner.height()) * k;
            fx.setColor(0xFFFFFFFF);
            fx.setAlpha((int) (alpha * 0.55f * (float) Math.sin(k * Math.PI)));
            path2.reset();
            float band = Math.max(4f * d, inner.height() * 0.22f);
            path2.moveTo(x, inner.top);
            path2.lineTo(x + band, inner.top);
            path2.lineTo(x + band - inner.height(), inner.bottom);
            path2.lineTo(x - inner.height(), inner.bottom);
            path2.close();
            c.drawPath(path2, fx);
        }
        c.restoreToCount(save);
        // A four-point star twinkling on the upper right of the rim.
        float tw = t < 1f ? (float) Math.sin(Math.min(1f, t / 0.7f) * Math.PI) : 0f;
        if (tw > 0.02f) {
            float sx = outer.right - radius(outer) * 0.42f, sy = outer.top + radius(outer) * 0.36f;
            star(c, sx, sy, 4.2f * d * tw, alpha);
        }
    }

    private void star(Canvas c, float x, float y, float r, int a) {
        path2.reset();
        float k = r * 0.24f;
        path2.moveTo(x, y - r);
        path2.quadTo(x + k * 0.3f, y - k * 0.3f, x + r, y);
        path2.quadTo(x + k * 0.3f, y + k * 0.3f, x, y + r);
        path2.quadTo(x - k * 0.3f, y + k * 0.3f, x - r, y);
        path2.quadTo(x - k * 0.3f, y - k * 0.3f, x, y - r);
        path2.close();
        fx.setStyle(Paint.Style.FILL);
        fx.setColor(0xFFFFFFFF);
        fx.setAlpha(a);
        c.drawPath(path2, fx);
        fx.setStyle(Paint.Style.STROKE);
        fx.setStrokeWidth(0.8f * d);
        fx.setColor(INK);
        fx.setAlpha(a);
        c.drawPath(path2, fx);
        fx.setStyle(Paint.Style.FILL);
    }

    @Override
    public void setAlpha(int alpha) {
        if (this.alpha == alpha) return;
        this.alpha = alpha;
        invalidateSelf();
    }

    @Override
    public void setColorFilter(@Nullable ColorFilter filter) {
        p.setColorFilter(filter);
        fx.setColorFilter(filter);
        invalidateSelf();
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }
}
