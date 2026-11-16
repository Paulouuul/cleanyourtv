package com.gathin.tvhome;

import android.animation.ValueAnimator;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.view.animation.LinearInterpolator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * The One Piece skin's top-bar icons, drawn in a 24 x 24 unit box as anime cels with a manga ink line: every shape
 * is lit from the upper left in a gradient, carries a hard cel shadow on the lower right, and is outlined with a brush
 * line that is heavier on the shadow side. No line is thinner than about a pixel: the icons are some 32px on a 720p
 * panel, where finer hatching only shimmers. A Log Pose for Inputs (its needle swings and a shine
 * crosses the glass), a Den Den Mushi for the network (it blinks, then rings: it shakes, its receiver hops and sound
 * rings spread; asleep with "z"s while offline), a treasure chest for Apps (the lid pops open, coins jump out and
 * sparkle, the chest squashes as it shuts) and a ship's wheel for Settings (it spins an eighth with speed lines and
 * rocks back). {@link #nudge()} plays the icon's move, on focus.
 */
final class OnePieceIcon extends Drawable {

    static final int LOG_POSE = 0, DEN_DEN = 1, CHEST = 2, WHEEL = 3;

    private static final float BOX = 24f;
    private static final int INK = 0xFF24150A, WOOD = 0xFFB8732F, WOOD_LIGHT = 0xFFD9974F, WOOD_DARK = 0xFF7A4A1F,
            GOLD = 0xFFFFCD00, GOLD_DARK = 0xFFE0A100, SKIN = 0xFFF6CFA4, SHELL = 0xFFE69A3A, RED = 0xFFE53935,
            WHITE = 0xFFFFFFFF, PHONE = 0xFF3A3F47, LEAF = 0xFF5BB04A, SHINE = 0xFFFFF6C8;
    /** Line ink: warm dark brown, slightly see-through, so a line softens into the colour it edges. */
    private static final int LINE_INK = 0xD93A2414;
    /** Detail lines, in box units (24 to the icon): thin, as an anime cel's inner lines. */
    private static final float LINE = 0.5f;

    private final int kind;
    private final float density;
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ink = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fx = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path(), shifted = new Path(), heavy = new Path(), grainPath = new Path();
    private final RectF r = new RectF(), rb = new RectF();
    private final ValueAnimator animator = ValueAnimator.ofFloat(0f, 1f);

    /** The move's progress, 0 to 1. */
    private float t;
    /** The wheel's turn so far, in degrees. */
    private float turned, turnFrom;
    private boolean awake = true, reduced;
    private int alpha = 255;

    OnePieceIcon(float density, int kind) {
        this.density = density;
        this.kind = kind;
        ink.setStyle(Paint.Style.STROKE);
        ink.setColor(LINE_INK);
        ink.setStrokeJoin(Paint.Join.ROUND);
        ink.setStrokeCap(Paint.Cap.ROUND);
        fx.setStrokeCap(Paint.Cap.ROUND);
        animator.setDuration(kind == WHEEL ? 950 : kind == DEN_DEN ? 1150 : kind == CHEST ? 1350 : 1500);
        animator.setInterpolator(new LinearInterpolator());
        animator.addUpdateListener(a -> {
            t = (float) a.getAnimatedValue();
            invalidateSelf();
        });
    }

    /** The icon's move: the needle swings, the snail rings, the chest spills its gold, the wheel spins. */
    void nudge() {
        if (reduced) return;
        if (kind == DEN_DEN && !awake) return;
        // A move still playing (focus left and came back quickly) carries on instead of starting over.
        if (animator.isRunning()) return;
        if (kind == WHEEL) {
            turnFrom = turned;
            turned = (float) (Math.floor(turned / 45f + 0.01f) + 1) * 45f;
        }
        t = 0f;
        animator.start();
    }

    /** Den Den Mushi: awake while online, asleep (eyes shut) while offline. */
    void setAwake(boolean awake) {
        if (this.awake == awake) return;
        this.awake = awake;
        invalidateSelf();
    }

    void setReducedMotion(boolean reducedMotion) {
        reduced = reducedMotion;
        if (reduced && animator.isRunning()) animator.end();
    }

    private boolean moving() {
        return animator.isRunning();
    }

    @Override
    public void draw(@NonNull Canvas c) {
        int save = c.save();
        c.translate(getBounds().left, getBounds().top);
        c.scale(getBounds().width() / BOX, getBounds().height() / BOX);
        if (alpha < 255) c.saveLayerAlpha(0, 0, BOX, BOX, alpha);
        switch (kind) {
            case LOG_POSE: logPose(c); break;
            case DEN_DEN: denDen(c); break;
            case CHEST: chest(c); break;
            default: wheel(c); break;
        }
        c.restoreToCount(save);
    }

    // ---- Log Pose ----

    private void logPose(Canvas c) {
        // Strap with gold rivets, the wooden base with a gold band, then the glass globe.
        r.set(1.5f, 16.2f, 22.5f, 21.2f);
        roundRect(c, r, 2f, WOOD_DARK, 1f);
        grain(c, 2.5f, 18.1f, 21.5f, 19.6f, 2);
        for (float x : new float[]{4f, 20f}) circle(c, x, 18.7f, 0.85f, GOLD, 0.4f);
        r.set(7f, 13.8f, 17f, 17.6f);
        roundRect(c, r, 1f, WOOD, 1f);
        r.set(7f, 13.8f, 17f, 15.1f);
        roundRect(c, r, 0.6f, GOLD, 0.7f);

        // Glass: a clear, light sphere, brighter on the lit side.
        fill.setColor(0xFF000000);   // opaque: a paint's alpha also fades its shader
        fill.setShader(new RadialGradient(10.4f, 5.6f, 8f, new int[]{0xFFFFFFFF, 0xFFD6F3FF, 0xFF8CCBE8},
                new float[]{0f, 0.55f, 1f}, Shader.TileMode.CLAMP));
        c.drawCircle(12f, 7.6f, 6.4f, fill);
        fill.setShader(null);
        // The compass card: four marks and a faint ring, so the needle reads as a compass.
        fx.setStyle(Paint.Style.STROKE);
        fx.setColor(0x995A7A90);
        fx.setStrokeWidth(0.4f);
        c.drawCircle(12f, 7.6f, 4.7f, fx);
        fx.setStrokeWidth(0.55f);
        fx.setColor(0xCC4A6A80);
        for (int i = 0; i < 4; i++) {
            double a = Math.toRadians(i * 90.0);
            c.drawLine(12f + (float) Math.cos(a) * 4.7f, 7.6f + (float) Math.sin(a) * 4.7f,
                    12f + (float) Math.cos(a) * 5.7f, 7.6f + (float) Math.sin(a) * 5.7f, fx);
        }
        // A small island at the bottom of the glass, where the needle points.
        int save = c.save();
        path.reset();
        path.addCircle(12f, 7.6f, 6.4f, Path.Direction.CW);
        c.clipPath(path);
        path.reset();
        path.moveTo(13.6f, 13.2f);
        path.quadTo(15.6f, 10.6f, 17.8f, 13.2f);
        path.close();
        fill.setColor(LEAF);
        c.drawPath(path, fill);
        c.restoreToCount(save);

        // Needle: swings out and settles back on its island.
        float swing = 0f;
        if (moving()) swing = (float) (40.0 * Math.exp(-3.2 * t) * Math.sin(t * 5.5 * Math.PI));
        c.save();
        c.rotate(25f + swing, 12f, 7.6f);
        path.reset();
        path.moveTo(12f, 2.6f);
        path.lineTo(13.3f, 7.6f);
        path.lineTo(10.7f, 7.6f);
        path.close();
        shape(c, path, RED, 0.6f);
        path.reset();
        path.moveTo(12f, 12.6f);
        path.lineTo(13.3f, 7.6f);
        path.lineTo(10.7f, 7.6f);
        path.close();
        shape(c, path, 0xFFF2F2F2, 0.6f);
        c.restore();
        circle(c, 12f, 7.6f, 0.9f, GOLD, 0.6f);

        // A shine crossing the glass as it moves, then the fixed glints and the rim.
        if (moving() && t < 0.6f) {
            float k = t / 0.6f;
            save = c.save();
            path.reset();
            path.addCircle(12f, 7.6f, 6.4f, Path.Direction.CW);
            c.clipPath(path);
            fx.setStyle(Paint.Style.FILL);
            fx.setColor(WHITE);
            fx.setAlpha((int) (200 * Math.sin(k * Math.PI)));
            float x = 3f + 18f * k;
            path.reset();
            path.moveTo(x, 1f);
            path.lineTo(x + 2.2f, 1f);
            path.lineTo(x - 3.8f, 15f);
            path.lineTo(x - 6f, 15f);
            path.close();
            c.drawPath(path, fx);
            c.restoreToCount(save);
        }
        fill.setColor(0xCCFFFFFF);
        r.set(7.9f, 3.2f, 10.6f, 4.8f);
        c.save();
        c.rotate(-35f, 9.25f, 4f);
        c.drawOval(r, fill);
        c.restore();
        path.reset();
        path.addCircle(12f, 7.6f, 6.4f, Path.Direction.CW);
        brush(c, path, 1f);
        if (moving() && t > 0.55f) star(c, 19.6f, 3.4f, 2.2f * (float) Math.sin((t - 0.55f) / 0.45f * Math.PI));
    }

    // ---- Den Den Mushi ----

    private void denDen(Canvas c) {
        // Blink first, then ring: a shake that fades, the receiver hopping and rings spreading.
        float blink = 0f, shake = 0f, hop = 0f, ringK = -1f;
        if (moving()) {
            if (t < 0.25f) {
                float k = t / 0.25f;
                blink = k < 0.5f ? k * 2f : (1f - k) * 2f;
            } else {
                float k = (t - 0.25f) / 0.75f;
                float fade = 1f - k;
                shake = (float) Math.sin(k * 26 * Math.PI) * 5f * fade;
                hop = (float) Math.abs(Math.sin(k * 13 * Math.PI)) * 1.3f * fade;
                ringK = k;
            }
        }
        if (ringK >= 0f) soundRings(c, ringK);
        c.save();
        c.rotate(shake, 12f, 20f);
        // Neck and head, the foot, then the shell.
        r.set(2.6f, 8.8f, 8.6f, 19.5f);
        roundRect(c, r, 3f, SKIN, 1f);
        r.set(2f, 16.6f, 22f, 21.2f);
        roundRect(c, r, 2.3f, SKIN, 1f);
        fill.setColor(SKIN);
        c.drawRect(3.5f, 15.2f, 7.7f, 17.6f, fill);
        // Foot's soft ridges.
        ink.setStrokeWidth(LINE);
        for (float x : new float[]{9.5f, 13f, 16.5f}) c.drawLine(x, 19.4f, x + 1.2f, 19.4f, ink);
        circle(c, 14.6f, 12.4f, 6.1f, SHELL, 1f);
        // Spiral in two weights, as a brush lays it.
        ink.setStrokeWidth(LINE);
        path.reset();
        path.addArc(new RectF(10.4f, 8.2f, 18.8f, 16.6f), 160f, 260f);
        c.drawPath(path, ink);
        ink.setStrokeWidth(LINE);
        path.reset();
        path.addArc(new RectF(12.4f, 10.2f, 16.8f, 14.6f), 200f, 240f);
        c.drawPath(path, ink);
        glint(c, 11.4f, 9.4f, 1.6f);

        // The receiver on top, hopping when it rings.
        c.save();
        c.translate(0f, -hop);
        r.set(10.6f, 4.3f, 18.6f, 6.4f);
        roundRect(c, r, 1f, PHONE, 0.8f);
        fill.setColor(0x88FFFFFF);
        c.drawRect(11.6f, 4.75f, 15f, 5.1f, fill);
        c.restore();
        ink.setStrokeWidth(LINE);
        c.drawLine(12f, 6.4f, 12.6f, 7.1f, ink);
        c.drawLine(17.2f, 6.4f, 16.6f, 7.1f, ink);

        // Eye stalks, eyes with an anime catch-light, cheeks.
        ink.setStrokeWidth(1.60f);
        c.drawLine(4.6f, 9.6f, 3.9f, 5.8f, ink);
        c.drawLine(7f, 9.6f, 7.7f, 5.8f, ink);
        fx.setStyle(Paint.Style.STROKE);
        fx.setStrokeWidth(0.55f);
        fx.setColor(SKIN);
        fx.setAlpha(255);
        c.drawLine(4.6f, 9.4f, 3.95f, 6.1f, fx);
        c.drawLine(7f, 9.4f, 7.65f, 6.1f, fx);
        float lid = !awake ? 1f : blink;
        eye(c, 3.8f, 4.6f, lid);
        eye(c, 7.9f, 4.6f, lid);
        fill.setColor(0x66FF7A7A);
        r.set(3f, 12.2f, 4.6f, 13.1f);
        c.drawOval(r, fill);
        r.set(6.3f, 12.2f, 7.9f, 13.1f);
        c.drawOval(r, fill);
        // Mouth: a smile, a ringing "o", or a sleepy line.
        ink.setStrokeWidth(LINE);
        path.reset();
        if (!awake) {
            path.moveTo(3.8f, 14.5f);
            path.lineTo(6.6f, 14.5f);
            c.drawPath(path, ink);
        } else if (ringK >= 0f) {
            r.set(4.4f, 13.4f, 6f, 15.2f);
            fill.setColor(0xFF8A2A1A);
            c.drawOval(r, fill);
            ink.setStrokeWidth(LINE);
            c.drawOval(r, ink);
        } else {
            path.moveTo(3.6f, 14f);
            path.quadTo(5.2f, 15.4f, 6.8f, 14f);
            c.drawPath(path, ink);
        }
        c.restore();
        if (!awake) sleepyZ(c);
    }

    /** The ringing: three arcs spreading up and right from the receiver, fading out. */
    private void soundRings(Canvas c, float k) {
        fx.setStyle(Paint.Style.STROKE);
        fx.setColor(INK);
        for (int i = 0; i < 3; i++) {
            float p = (k * 2.2f - i * 0.35f) % 1f;
            if (p < 0f) continue;
            float rad = 2.2f + p * 4.4f;
            fx.setStrokeWidth(1f - 0.25f * p);
            fx.setAlpha((int) (255 * (1f - p) * Math.min(1f, (1f - k) * 3f)));
            r.set(16.6f - rad, 5.4f - rad, 16.6f + rad, 5.4f + rad);
            c.drawArc(r, -75f, 55f, false, fx);
        }
    }

    private void sleepyZ(Canvas c) {
        ink.setStrokeWidth(LINE);
        float[][] zs = {{9.4f, 3.6f, 1.4f}, {11.6f, 1.2f, 1f}};
        for (float[] z : zs) {
            float x = z[0], y = z[1], s = z[2];
            path.reset();
            path.moveTo(x, y);
            path.lineTo(x + s, y);
            path.lineTo(x, y + s);
            path.lineTo(x + s, y + s);
            c.drawPath(path, ink);
        }
    }

    /** An eye at (x, y): white with a pupil and a catch-light, closing as {@code lid} goes to 1. */
    private void eye(Canvas c, float x, float y, float lid) {
        float rad = 1.7f;
        if (lid >= 0.95f) {
            circle(c, x, y, rad, SKIN, 0.8f);
            ink.setStrokeWidth(LINE);
            path.reset();
            path.moveTo(x - 1.1f, y + 0.1f);
            path.quadTo(x, y + 1f, x + 1.1f, y + 0.1f);
            c.drawPath(path, ink);
            return;
        }
        fill.setColor(0xFF000000);   // opaque: a paint's alpha also fades its shader
        fill.setShader(new RadialGradient(x - 0.6f, y - 0.6f, rad * 1.6f, WHITE, 0xFFD9E4F0, Shader.TileMode.CLAMP));
        c.drawCircle(x, y, rad, fill);
        fill.setShader(null);
        fill.setColor(INK);
        c.drawCircle(x - 0.25f, y + 0.25f, 0.85f, fill);
        fill.setColor(WHITE);
        c.drawCircle(x - 0.55f, y - 0.1f, 0.32f, fill);
        if (lid > 0f) {
            fill.setColor(SKIN);
            c.save();
            c.clipRect(x - rad, y - rad, x + rad, y - rad + 2 * rad * lid);
            c.drawCircle(x, y, rad, fill);
            c.restore();
            ink.setStrokeWidth(LINE);
            float ly = y - rad + 2 * rad * lid;
            c.drawLine(x - rad * 0.9f, ly, x + rad * 0.9f, ly, ink);
        }
        path.reset();
        path.addCircle(x, y, rad, Path.Direction.CW);
        brush(c, path, 0.75f);
    }

    // ---- Treasure chest ----

    private void chest(Canvas c) {
        // The lid pops past open and settles, holds while the coins fly, then slams with a squash.
        float open = 0f, squash = 0f;
        if (moving()) {
            if (t < 0.14f) open = easeOutBack(t / 0.14f);
            else if (t < 0.66f) open = 1f;
            else if (t < 0.8f) open = 1f - (t - 0.66f) / 0.14f;
            if (t >= 0.8f) squash = (float) Math.sin((t - 0.8f) / 0.2f * Math.PI) * (1f - (t - 0.8f) / 0.2f * 0.4f);
        }
        c.save();
        c.scale(1f + 0.06f * squash, 1f - 0.08f * squash, 12f, 20.6f);
        if (open > 0f) {
            // The dark inside and the heap of gold.
            fill.setColor(0xFF3A2010);
            r.set(4.2f, 9.6f, 19.8f, 13.6f);
            c.drawOval(r, fill);
            fill.setColor(0xFF000000);   // opaque: a paint's alpha also fades its shader
            fill.setShader(new LinearGradient(0, 8.6f, 0, 13.4f, 0xFFFFF0A0, GOLD_DARK, Shader.TileMode.CLAMP));
            r.set(4.8f, 8.8f, 19.2f, 13.4f);
            c.drawOval(r, fill);
            fill.setShader(null);
            ink.setStrokeWidth(LINE);
            c.drawOval(r, ink);
            fill.setColor(SHINE);
            c.drawCircle(9f, 10.4f, 0.9f, fill);
            c.drawCircle(14.6f, 10f, 0.7f, fill);
            // A glow rising out of it.
            fx.setStyle(Paint.Style.FILL);
            fx.setShader(new RadialGradient(12f, 10f, 9f, 0x88FFF2A0, 0x00FFF2A0, Shader.TileMode.CLAMP));
            fx.setAlpha((int) (255 * Math.min(1f, open)));
            c.drawCircle(12f, 10f, 9f, fx);
            fx.setShader(null);
        }
        // Body: wood with grain, gold bands with rivets.
        r.set(3.5f, 11.5f, 20.5f, 20.6f);
        roundRect(c, r, 1.2f, WOOD, 1f);
        grain(c, 4.2f, 12.6f, 19.8f, 20f, 3);
        ink.setStrokeWidth(LINE);
        c.drawLine(4f, 16f, 20f, 16f, ink);
        for (float x : new float[]{6.4f, 15.6f}) {
            r.set(x, 11.5f, x + 2f, 20.6f);
            roundRect(c, r, 0.3f, GOLD, 0.7f);
            fill.setColor(INK);
            c.drawCircle(x + 1f, 13f, 0.28f, fill);
            c.drawCircle(x + 1f, 19.1f, 0.28f, fill);
        }
        // Lid, hinged at the back left.
        c.save();
        c.rotate(-34f * open, 3.5f, 11.5f);
        c.translate(0f, -1.2f * Math.min(1f, open));
        path.reset();
        path.moveTo(3.5f, 11.5f);
        path.lineTo(3.5f, 8.6f);
        path.cubicTo(3.5f, 4.4f, 20.5f, 4.4f, 20.5f, 8.6f);
        path.lineTo(20.5f, 11.5f);
        path.close();
        cel(c, path, WOOD_LIGHT);
        int save = c.save();
        c.clipPath(path);
        grain(c, 4f, 6.5f, 20f, 11f, 2);
        for (float x : new float[]{6.4f, 15.6f}) {
            fill.setColor(0xFF000000);   // opaque: a paint's alpha also fades its shader
            fill.setShader(new LinearGradient(x, 0, x + 2f, 0, 0xFFFFF0A0, GOLD_DARK, Shader.TileMode.CLAMP));
            c.drawRect(x, 3f, x + 2f, 11.5f, fill);
            fill.setShader(null);
        }
        ink.setStrokeWidth(LINE);
        for (float x : new float[]{6.4f, 8.4f, 15.6f, 17.6f}) c.drawLine(x, 3f, x, 11.5f, ink);
        c.restoreToCount(save);
        brush(c, path, 1f);
        c.restore();
        // Lock with a keyhole.
        r.set(10.4f, 10f, 13.6f, 14.4f);
        roundRect(c, r, 0.6f, GOLD, 0.8f);
        fill.setColor(INK);
        c.drawCircle(12f, 11.8f, 0.55f, fill);
        c.drawRect(11.75f, 11.8f, 12.25f, 13.2f, fill);
        c.restore();

        if (moving()) coins(c);
    }

    /** Three coins jump out of the open chest, flipping, and drop back in; sparkles twinkle over them. */
    private void coins(Canvas c) {
        float k = (t - 0.12f) / 0.56f;
        if (k > 0f && k < 1f) {
            float[] vx = {-5.6f, 0.6f, 5.8f};
            float[] high = {7.5f, 9.5f, 7f};
            for (int i = 0; i < 3; i++) {
                float ki = Math.min(1f, k * (1f + i * 0.12f));
                if (ki >= 1f) continue;
                float x = 12f + vx[i] * ki;
                float y = 10.5f - high[i] * 4f * ki * (1f - ki);
                float flip = Math.abs((float) Math.cos(ki * Math.PI * (2.5f + i)));
                coin(c, x, y, 1.5f, Math.max(0.18f, flip));
            }
        }
        if (t > 0.2f && t < 0.75f) {
            float s = (t - 0.2f) / 0.55f;
            star(c, 4.4f, 4.6f, 1.9f * (float) Math.sin(s * Math.PI));
            star(c, 19.8f, 3f, 2.3f * (float) Math.sin(Math.min(1f, s * 1.3f) * Math.PI));
            float s3 = Math.max(0f, s - 0.3f) / 0.7f;
            if (s3 > 0f) star(c, 12.6f, 1.6f, 1.5f * (float) Math.sin(s3 * Math.PI));
        }
    }

    private void coin(Canvas c, float x, float y, float rad, float width) {
        r.set(x - rad * width, y - rad, x + rad * width, y + rad);
        fill.setColor(0xFF000000);   // opaque: a paint's alpha also fades its shader
        fill.setShader(new LinearGradient(r.left, r.top, r.right, r.bottom, 0xFFFFF4B0, GOLD_DARK, Shader.TileMode.CLAMP));
        c.drawOval(r, fill);
        fill.setShader(null);
        ink.setStrokeWidth(LINE);
        c.drawOval(r, ink);
        if (width > 0.5f) {
            r.inset(rad * width * 0.35f, rad * 0.35f);
            ink.setStrokeWidth(LINE);
            c.drawOval(r, ink);
        }
    }

    // ---- Ship's wheel ----

    private void wheel(Canvas c) {
        float angle = turned;
        float speed = 0f;
        if (moving()) {
            // Spins past the eighth and rocks back to it.
            float e = easeOutBack(t);
            angle = turnFrom + (turned - turnFrom) * e;
            speed = Math.max(0f, 1f - t * 1.7f);
        }
        if (speed > 0.02f) speedLines(c, angle, speed);
        // Spokes out to the handles, placed by hand so the light stays on the upper left as it turns.
        for (int i = 0; i < 8; i++) {
            double a = Math.toRadians(angle + i * 45.0);
            float x = 12f + (float) (Math.cos(a) * 10f), y = 12f + (float) (Math.sin(a) * 10f);
            ink.setStrokeWidth(2.4f);
            c.drawLine(12f, 12f, x, y, ink);
        }
        fx.setStyle(Paint.Style.STROKE);
        fx.setStrokeWidth(1.60f);
        for (int i = 0; i < 8; i++) {
            double a = Math.toRadians(angle + i * 45.0);
            float x = 12f + (float) (Math.cos(a) * 10f), y = 12f + (float) (Math.sin(a) * 10f);
            fx.setColor(WOOD_LIGHT);
            fx.setAlpha(255);
            c.drawLine(12f, 12f, x, y, fx);
            circle(c, x, y, 1.45f, WOOD, 0.8f);
        }
        // Rim: ink, then wood lit from the top left, a grain line round it.
        ink.setStrokeWidth(2.9f);
        c.drawCircle(12f, 12f, 6.6f, ink);
        fx.setStrokeWidth(2.20f);
        fx.setShader(new LinearGradient(6f, 6f, 18f, 18f, new int[]{0xFFE6A866, WOOD, WOOD_DARK}, null,
                Shader.TileMode.CLAMP));
        fx.setAlpha(255);
        c.drawCircle(12f, 12f, 6.6f, fx);
        fx.setShader(null);
        fx.setStrokeWidth(0.55f);
        fx.setColor(0x995A3214);
        r.set(5.9f, 5.9f, 18.1f, 18.1f);
        c.drawArc(r, angle + 20f, 110f, false, fx);
        c.drawArc(r, angle + 200f, 90f, false, fx);
        fx.setColor(0xAAFFFFFF);
        fx.setStrokeWidth(0.55f);
        r.set(5.6f, 5.6f, 18.4f, 18.4f);
        c.drawArc(r, 200f, 50f, false, fx);
        // Hub.
        circle(c, 12f, 12f, 2.5f, GOLD, 0.9f);
        fill.setColor(GOLD_DARK);
        c.drawCircle(12f, 12f, 0.9f, fill);
        fill.setColor(SHINE);
        c.drawCircle(11.2f, 11.1f, 0.5f, fill);
    }

    /** Curved manga speed lines trailing the handles while the wheel spins. */
    private void speedLines(Canvas c, float angle, float speed) {
        fx.setStyle(Paint.Style.STROKE);
        fx.setColor(INK);
        r.set(0.4f, 0.4f, 23.6f, 23.6f);
        for (int i = 0; i < 8; i += 2) {
            float a = angle + i * 45f;
            fx.setAlpha((int) (200 * speed));
            fx.setStrokeWidth(0.55f);
            c.drawArc(r, a - 38f * speed - 8f, 30f * speed, false, fx);
        }
        r.set(2.2f, 2.2f, 21.8f, 21.8f);
        for (int i = 1; i < 8; i += 2) {
            fx.setAlpha((int) (140 * speed));
            fx.setStrokeWidth(0.55f);
            c.drawArc(r, angle + i * 45f - 30f * speed - 10f, 22f * speed, false, fx);
        }
    }

    // ---- drawing helpers ----

    private void roundRect(Canvas c, RectF rect, float radius, int color, float line) {
        path.reset();
        path.addRoundRect(rect, radius, radius, Path.Direction.CW);
        cel(c, path, color);
        brush(c, path, line);
    }

    private void circle(Canvas c, float x, float y, float rad, int color, float line) {
        path.reset();
        path.addCircle(x, y, rad, Path.Direction.CW);
        cel(c, path, color);
        brush(c, path, line);
    }

    private void shape(Canvas c, Path p, int color, float line) {
        cel(c, p, color);
        brush(c, p, line);
    }

    /**
     * An anime cel: the colour in a gradient lit from the upper left, then a hard shadow on the lower right (the
     * shape minus itself moved up-left) in a darker tone.
     */
    private void cel(Canvas c, Path p, int base) {
        p.computeBounds(rb, true);
        fill.setColor(0xFF000000);   // opaque: a paint's alpha also fades its shader
        fill.setShader(new LinearGradient(rb.left, rb.top, rb.right, rb.bottom,
                new int[]{mix(base, WHITE, 0.4f), base, base}, new float[]{0f, 0.5f, 1f}, Shader.TileMode.CLAMP));
        c.drawPath(p, fill);
        fill.setShader(null);
        float off = Math.max(0.45f, Math.min(1.1f, Math.min(rb.width(), rb.height()) * 0.16f));
        int save = c.save();
        c.clipPath(p);
        shifted.set(p);
        shifted.offset(-off, -off * 1.2f);
        c.clipOutPath(shifted);
        fill.setColor(mix(base, 0xFF1A0A00, 0.32f));
        c.drawRect(rb, fill);
        c.restoreToCount(save);
    }

    /** A soft brush outline: a faint halo and a thin line. */
    private void brush(Canvas c, Path p, float width) {
        // A soft outline: a faint wide halo, like a blur, under one thin line slightly toward the shadow side. The
        // line stays thin so it edges the colour instead of covering it.
        float main = Math.max(0.42f, width * 0.55f);
        ink.setStrokeWidth(main + 1.1f);
        ink.setAlpha(0x2E);
        c.drawPath(p, ink);
        heavy.set(p);
        heavy.offset(0.08f, 0.1f);
        ink.setStrokeWidth(main);
        ink.setColor(LINE_INK);
        c.drawPath(heavy, ink);
    }

    /** Fine wood grain: wavy dark and light lines across a box. */
    private void grain(Canvas c, float l, float tp, float rt, float bt, int lines) {
        fx.setStyle(Paint.Style.STROKE);
        fx.setStrokeWidth(0.55f);
        for (int i = 0; i < lines; i++) {
            float y = tp + (bt - tp) * (i + 0.5f) / lines;
            grainPath.reset();
            grainPath.moveTo(l, y);
            float w = (rt - l) / 4f;
            for (int s = 0; s < 4; s++) {
                float x0 = l + s * w;
                grainPath.quadTo(x0 + w / 2f, y + ((s + i) % 2 == 0 ? 0.45f : -0.45f), x0 + w, y);
            }
            fx.setColor(i % 2 == 0 ? 0x993A1E08 : 0x55FFE6C0);
            c.drawPath(grainPath, fx);
        }
    }

    /** A short curved white highlight. */
    private void glint(Canvas c, float x, float y, float len) {
        fx.setStyle(Paint.Style.STROKE);
        fx.setStrokeWidth(0.55f);
        fx.setColor(0xDDFFFFFF);
        r.set(x, y, x + len * 2f, y + len * 2f);
        c.drawArc(r, 190f, 70f, false, fx);
    }

    /** A four-point anime sparkle with an ink edge. */
    private void star(Canvas c, float x, float y, float rad) {
        if (rad <= 0.05f) return;
        float k = rad * 0.08f;
        path.reset();
        path.moveTo(x, y - rad);
        path.quadTo(x + k, y - k, x + rad, y);
        path.quadTo(x + k, y + k, x, y + rad);
        path.quadTo(x - k, y + k, x - rad, y);
        path.quadTo(x - k, y - k, x, y - rad);
        path.close();
        fill.setColor(SHINE);
        c.drawPath(path, fill);
        ink.setStrokeWidth(LINE);
        c.drawPath(path, ink);
    }

    private static float easeOutBack(float x) {
        float s = 1.9f;
        float u = x - 1f;
        return 1f + u * u * ((s + 1f) * u + s);
    }

    private static int mix(int a, int b, float k) {
        int ar = (a >> 16) & 0xFF, ag = (a >> 8) & 0xFF, ab = a & 0xFF;
        int br = (b >> 16) & 0xFF, bg = (b >> 8) & 0xFF, bb = b & 0xFF;
        return 0xFF000000 | ((int) (ar + (br - ar) * k) << 16) | ((int) (ag + (bg - ag) * k) << 8)
                | (int) (ab + (bb - ab) * k);
    }

    @Override
    public int getIntrinsicWidth() {
        return Math.round(BOX * density);
    }

    @Override
    public int getIntrinsicHeight() {
        return Math.round(BOX * density);
    }

    @Override
    public void setAlpha(int alpha) {
        if (this.alpha == alpha) return;
        this.alpha = alpha;
        invalidateSelf();
    }

    @Override
    public void setColorFilter(@Nullable ColorFilter filter) {
        fill.setColorFilter(filter);
        ink.setColorFilter(filter);
        fx.setColorFilter(filter);
        invalidateSelf();
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }
}
