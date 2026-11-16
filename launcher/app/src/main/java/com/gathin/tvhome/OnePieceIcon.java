package com.gathin.tvhome;

import android.animation.ValueAnimator;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * The One Piece skin's top-bar icons, flat cartoon shapes with a dark ink line, drawn in a 24 x 24 unit box:
 * a Log Pose for Inputs (its needle swings and settles), a Den Den Mushi for the network (it blinks, and sleeps
 * with its eyes shut while offline), a treasure chest for Apps (its lid pops open) and a ship's wheel for Settings
 * (it turns an eighth). {@link #nudge()} plays the icon's move, on focus.
 */
final class OnePieceIcon extends Drawable {

    static final int LOG_POSE = 0, DEN_DEN = 1, CHEST = 2, WHEEL = 3;

    private static final float BOX = 24f;
    private static final int INK = 0xFF2A1A0E, WOOD = 0xFFB8732F, WOOD_LIGHT = 0xFFD08C45, WOOD_DARK = 0xFF7A4A1F,
            GOLD = 0xFFFFCD00, GOLD_DARK = 0xFFE0A100, GLASS = 0xFFC4F0FF, SKIN = 0xFFF6CFA4, SHELL = 0xFFE69A3A,
            RED = 0xFFE53935, WHITE = 0xFFFFFFFF, PHONE = 0xFF3A3F47;

    private final int kind;
    private final float density;
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ink = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final RectF r = new RectF();
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
        ink.setColor(INK);
        ink.setStrokeJoin(Paint.Join.ROUND);
        ink.setStrokeCap(Paint.Cap.ROUND);
        animator.setDuration(kind == WHEEL ? 700 : kind == DEN_DEN ? 320 : kind == CHEST ? 1100 : 1400);
        animator.setInterpolator(kind == WHEEL ? new DecelerateInterpolator(1.6f) : new LinearInterpolator());
        animator.addUpdateListener(a -> {
            t = (float) a.getAnimatedValue();
            invalidateSelf();
        });
    }

    /** The icon's move: the needle swings, the snail blinks, the lid pops, the wheel turns. */
    void nudge() {
        if (reduced) return;
        if (kind == DEN_DEN && !awake) return;
        // A move still playing (focus left and came back quickly) carries on instead of starting over.
        if (animator.isRunning()) return;
        if (kind == WHEEL) {
            float now = animator.isRunning() ? turnFrom + (turned - turnFrom) * t : turned;
            turnFrom = now;
            turned = (float) (Math.floor(now / 45f + 0.01f) + 1) * 45f;
        }
        animator.cancel();
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

    // ---- shapes ----

    private void logPose(Canvas c) {
        // Strap, wooden base with a gold ring, then the glass globe with its needle.
        r.set(1.5f, 16.2f, 22.5f, 21.2f);
        roundRect(c, r, 2f, WOOD_DARK, 1f);
        fill.setColor(WOOD_LIGHT);
        for (float x : new float[]{4f, 20f}) c.drawCircle(x, 18.7f, 0.7f, fill);
        r.set(7f, 13.2f, 17f, 17.6f);
        roundRect(c, r, 1f, WOOD, 1f);
        r.set(7f, 13.2f, 17f, 14.6f);
        roundRect(c, r, 0.6f, GOLD, 0.7f);
        fill.setColor(GLASS);
        c.drawCircle(12f, 8f, 5.9f, fill);
        ink.setStrokeWidth(0.6f);
        c.drawLine(12f, 13.2f, 12f, 9f, ink);
        // Needle: swings out and settles back on its island.
        float swing = 0f;
        if (animator.isRunning()) {
            swing = (float) (38.0 * Math.exp(-3.5 * t) * Math.sin(t * 5.5 * Math.PI));
        }
        c.save();
        c.rotate(25f + swing, 12f, 8f);
        path.reset();
        path.moveTo(12f, 3.6f);
        path.lineTo(13.1f, 8f);
        path.lineTo(10.9f, 8f);
        path.close();
        shape(c, path, RED, 0.5f);
        path.reset();
        path.moveTo(12f, 12.4f);
        path.lineTo(13.1f, 8f);
        path.lineTo(10.9f, 8f);
        path.close();
        shape(c, path, WHITE, 0.5f);
        c.restore();
        fill.setColor(GOLD);
        c.drawCircle(12f, 8f, 0.7f, fill);
        // Glint and rim.
        fill.setColor(0xCCFFFFFF);
        r.set(8.2f, 4.2f, 10.6f, 5.6f);
        c.save();
        c.rotate(-35f, 9.4f, 4.9f);
        c.drawOval(r, fill);
        c.restore();
        ink.setStrokeWidth(1f);
        c.drawCircle(12f, 8f, 5.9f, ink);
    }

    private void denDen(Canvas c) {
        // Neck and head, the foot, then the shell with the receiver on top.
        r.set(2.6f, 8.8f, 8.6f, 19.5f);
        roundRect(c, r, 3f, SKIN, 1f);
        r.set(2f, 16.6f, 22f, 21.2f);
        roundRect(c, r, 2.3f, SKIN, 1f);
        fill.setColor(SKIN);
        c.drawRect(3.4f, 15f, 7.8f, 18f, fill);
        fill.setColor(SHELL);
        c.drawCircle(14.6f, 12.4f, 6.1f, fill);
        ink.setStrokeWidth(1f);
        c.drawCircle(14.6f, 12.4f, 6.1f, ink);
        ink.setStrokeWidth(0.7f);
        path.reset();
        path.addArc(new RectF(10.4f, 8.2f, 18.8f, 16.6f), 160f, 260f);
        path.addArc(new RectF(12.4f, 10.2f, 16.8f, 14.6f), 200f, 240f);
        c.drawPath(path, ink);
        r.set(10.6f, 4.3f, 18.6f, 6.4f);
        roundRect(c, r, 1f, PHONE, 0.8f);
        ink.setStrokeWidth(0.6f);
        c.drawLine(12f, 6.4f, 12.6f, 7f, ink);
        c.drawLine(17.2f, 6.4f, 16.6f, 7f, ink);
        // Eye stalks and eyes.
        ink.setStrokeWidth(1.6f);
        c.drawLine(4.6f, 9.6f, 3.9f, 5.8f, ink);
        c.drawLine(7f, 9.6f, 7.7f, 5.8f, ink);
        ink.setStrokeWidth(0.8f);
        float lid = !awake ? 1f : animator.isRunning() ? (t < 0.5f ? t * 2f : (1f - t) * 2f) : 0f;
        eye(c, 3.8f, 4.6f, lid);
        eye(c, 7.9f, 4.6f, lid);
        // Mouth: a small smile, or a sleepy line.
        ink.setStrokeWidth(0.7f);
        path.reset();
        if (awake) {
            path.moveTo(3.6f, 14f);
            path.quadTo(5.2f, 15.4f, 6.8f, 14f);
        } else {
            path.moveTo(3.8f, 14.5f);
            path.lineTo(6.6f, 14.5f);
        }
        c.drawPath(path, ink);
    }

    /** An eye at (x, y): white with a pupil, closing as {@code lid} goes to 1. */
    private void eye(Canvas c, float x, float y, float lid) {
        float rad = 1.7f;
        if (lid >= 0.95f) {
            fill.setColor(SKIN);
            c.drawCircle(x, y, rad, fill);
            ink.setStrokeWidth(0.8f);
            c.drawCircle(x, y, rad, ink);
            path.reset();
            path.moveTo(x - 1.1f, y);
            path.quadTo(x, y + 0.9f, x + 1.1f, y);
            c.drawPath(path, ink);
            return;
        }
        fill.setColor(WHITE);
        c.drawCircle(x, y, rad, fill);
        fill.setColor(INK);
        c.drawCircle(x - 0.3f, y + 0.2f, 0.8f, fill);
        if (lid > 0f) {
            fill.setColor(SKIN);
            c.save();
            c.clipRect(x - rad, y - rad, x + rad, y - rad + 2 * rad * lid);
            c.drawCircle(x, y, rad, fill);
            c.restore();
        }
        ink.setStrokeWidth(0.8f);
        c.drawCircle(x, y, rad, ink);
    }

    private void chest(Canvas c) {
        // Open: up quickly, a moment's look at the gold, then shut.
        float open = !animator.isRunning() ? 0f : t < 0.2f ? t / 0.2f : t < 0.65f ? 1f : Math.max(0f, 1f - (t - 0.65f) / 0.35f);
        if (open > 0f) {
            // Coins heaped inside and a gleam over them.
            fill.setColor(GOLD);
            r.set(4.6f, 8.6f, 19.4f, 13.4f);
            c.drawOval(r, fill);
            ink.setStrokeWidth(0.7f);
            c.drawOval(r, ink);
            fill.setColor(0xFFFFF3B0);
            c.drawCircle(9f, 10.4f, 1f, fill);
            c.drawCircle(14.6f, 10f, 0.8f, fill);
        }
        // Body and its bands and lock.
        r.set(3.5f, 11.5f, 20.5f, 20.6f);
        roundRect(c, r, 1.2f, WOOD, 1f);
        ink.setStrokeWidth(0.6f);
        c.drawLine(4f, 16f, 20f, 16f, ink);
        for (float x : new float[]{6.4f, 15.6f}) {
            r.set(x, 11.5f, x + 2f, 20.6f);
            roundRect(c, r, 0.3f, GOLD, 0.7f);
        }
        // Lid, hinged at the back left, tipped open.
        c.save();
        c.rotate(-32f * open, 3.5f, 11.5f);
        c.translate(0f, -1.2f * open);
        path.reset();
        path.moveTo(3.5f, 11.5f);
        path.lineTo(3.5f, 8.6f);
        path.cubicTo(3.5f, 4.4f, 20.5f, 4.4f, 20.5f, 8.6f);
        path.lineTo(20.5f, 11.5f);
        path.close();
        fill.setColor(WOOD_LIGHT);
        c.drawPath(path, fill);
        c.save();
        c.clipPath(path);
        fill.setColor(GOLD);
        c.drawRect(6.4f, 3f, 8.4f, 11.5f, fill);
        c.drawRect(15.6f, 3f, 17.6f, 11.5f, fill);
        ink.setStrokeWidth(0.7f);
        for (float x : new float[]{6.4f, 8.4f, 15.6f, 17.6f}) c.drawLine(x, 3f, x, 11.5f, ink);
        c.restore();
        ink.setStrokeWidth(1f);
        c.drawPath(path, ink);
        c.restore();
        r.set(10.4f, 10f, 13.6f, 14.4f);
        roundRect(c, r, 0.6f, GOLD, 0.8f);
        fill.setColor(INK);
        c.drawCircle(12f, 11.8f, 0.55f, fill);
        c.drawRect(11.75f, 11.8f, 12.25f, 13.2f, fill);
        if (open > 0.6f) {
            // A twinkle over the gold.
            ink.setColor(0xFFFFF3B0);
            ink.setStrokeWidth(0.6f);
            c.drawLine(19.5f, 2.2f, 19.5f, 5.2f, ink);
            c.drawLine(18f, 3.7f, 21f, 3.7f, ink);
            ink.setColor(INK);
        }
    }

    private void wheel(Canvas c) {
        float angle = animator.isRunning() ? turnFrom + (turned - turnFrom) * t : turned;
        c.save();
        c.rotate(angle, 12f, 12f);
        // Spokes run through the rim out to the handles: ink under, wood over.
        for (int i = 0; i < 8; i++) {
            double a = Math.toRadians(i * 45.0);
            float x = 12f + (float) (Math.cos(a) * 10f), y = 12f + (float) (Math.sin(a) * 10f);
            ink.setStrokeWidth(3f);
            c.drawLine(12f, 12f, x, y, ink);
        }
        for (int i = 0; i < 8; i++) {
            double a = Math.toRadians(i * 45.0);
            float x = 12f + (float) (Math.cos(a) * 10f), y = 12f + (float) (Math.sin(a) * 10f);
            fill.setStyle(Paint.Style.STROKE);
            fill.setStrokeCap(Paint.Cap.ROUND);
            fill.setStrokeWidth(1.6f);
            fill.setColor(WOOD_LIGHT);
            c.drawLine(12f, 12f, x, y, fill);
            fill.setStyle(Paint.Style.FILL);
            fill.setColor(WOOD);
            c.drawCircle(x, y, 1.45f, fill);
            ink.setStrokeWidth(0.8f);
            c.drawCircle(x, y, 1.45f, ink);
        }
        // Rim.
        ink.setStrokeWidth(3.6f);
        c.drawCircle(12f, 12f, 6.6f, ink);
        fill.setStyle(Paint.Style.STROKE);
        fill.setStrokeWidth(2.2f);
        fill.setColor(WOOD);
        c.drawCircle(12f, 12f, 6.6f, fill);
        fill.setStyle(Paint.Style.FILL);
        // Hub.
        fill.setColor(GOLD);
        c.drawCircle(12f, 12f, 2.5f, fill);
        ink.setStrokeWidth(0.9f);
        c.drawCircle(12f, 12f, 2.5f, ink);
        fill.setColor(GOLD_DARK);
        c.drawCircle(12f, 12f, 0.9f, fill);
        c.restore();
    }

    private void roundRect(Canvas c, RectF rect, float radius, int color, float line) {
        fill.setColor(color);
        c.drawRoundRect(rect, radius, radius, fill);
        ink.setStrokeWidth(line);
        c.drawRoundRect(rect, radius, radius, ink);
    }

    private void shape(Canvas c, Path p, int color, float line) {
        fill.setColor(color);
        c.drawPath(p, fill);
        ink.setStrokeWidth(line);
        c.drawPath(p, ink);
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
        invalidateSelf();
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }
}
