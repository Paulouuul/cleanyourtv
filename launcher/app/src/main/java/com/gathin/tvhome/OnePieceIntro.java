package com.gathin.tvhome;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.os.SystemClock;
import android.view.View;

/**
 * The One Piece skin's opening, played full screen once per power-on when Home first shows (the TV's own TCL and
 * Google TV logos come before it and cannot be changed without root). Deep sea, gold rays turning, manga speed lines
 * as the logo lands with a bounce, a shine across the logo, sparkles and foam waves; then it fades into Home. Any key
 * skips it. About 3.6 s; with reduced motion, a still logo that fades.
 *
 * As the skin's screensaver ({@link #OnePieceIntro(Context, boolean, Runnable, boolean)} with loop) it plays the same
 * opening and then keeps the sea alive: the rays turn, the waves roll, the logo breathes, larger, in the middle (the
 * time is a small line in a corner), and every few seconds the shine and the sparkles come back. The logo drifts slowly (and reports
 * it through {@link Drift}, for the time to follow) so nothing burns in, at about 25 frames a second after the opening.
 */
final class OnePieceIntro extends View {

    private static final String PREFS = "onepiece_intro", BOOT_KEY = "boot_at";
    private static final long DURATION = 3600, REDUCED = 1400;

    /** True once per power-on: the boot time moved since the opening last played. */
    static boolean dueAfterBoot(Context ctx) {
        long bootAt = System.currentTimeMillis() - SystemClock.elapsedRealtime();
        SharedPreferences p = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        long last = p.getLong(BOOT_KEY, 0L);
        // The clock can be set after boot (network time): a minute's slack keeps one boot one boot.
        boolean due = Math.abs(bootAt - last) >= 60_000L;
        android.util.Log.i("OnePieceIntro", "boot at " + bootAt + ", last played for " + last + ": " + (due ? "play" : "skip"));
        if (due) p.edit().putLong(BOOT_KEY, bootAt).apply();
        return due;
    }

    private final float d;
    private final boolean reduced;
    private final Runnable onEnd;
    private final Bitmap logo;
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final Paint fx = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final RectF r = new RectF();
    private final long total;
    private long startedAt = -1L;
    private long fadeStartedAt = -1L;
    /** Elapsed time, in ms. */
    private float ms;
    private boolean ending;
    private final boolean loop;
    private Drift drift;

    /** Where the screensaver's logo has drifted to, for the clock under it to follow. */
    interface Drift {
        void moved(float dx, float dy);
    }

    OnePieceIntro(Context ctx, boolean reducedMotion, Runnable onEnd) {
        this(ctx, reducedMotion, onEnd, false);
    }

    OnePieceIntro(Context ctx, boolean reducedMotion, Runnable onEnd, boolean loop) {
        super(ctx);
        this.loop = loop;
        d = ctx.getResources().getDisplayMetrics().density;
        reduced = reducedMotion;
        this.onEnd = onEnd;
        logo = BitmapFactory.decodeResource(ctx.getResources(), R.drawable.onepiece_logo);
        setLayerType(LAYER_TYPE_HARDWARE, null);
        total = reduced ? REDUCED : DURATION;
    }

    /**
     * Holds on the first frame (the dark sea, covering Home) while the TV's boot animation is still on screen: Home
     * starts some seconds before the Google TV animation finishes its last loop, and an opening started then played
     * unseen under it. Starts once init.svc.bootanim reads "stopped" (or cannot be read), at most 15 s after.
     */
    void start() {
        if (loop) {
            startedAt = SystemClock.uptimeMillis();
            postInvalidateOnAnimation();
            return;
        }
        long since = SystemClock.uptimeMillis();
        Runnable check = new Runnable() {
            @Override public void run() {
                if (ending) return;
                String anim = systemProperty("init.svc.bootanim");
                boolean waiting = "running".equals(anim) && SystemClock.uptimeMillis() - since < 15_000L;
                if (waiting) {
                    postDelayed(this, 200);
                    return;
                }
                android.util.Log.i("OnePieceIntro", "opening started (boot animation: " + anim + ", waited "
                        + (SystemClock.uptimeMillis() - since) + " ms)");
                startedAt = SystemClock.uptimeMillis();
                postInvalidateOnAnimation();
            }
        };
        check.run();
    }

    /** A system property, or "" when it cannot be read. */
    @android.annotation.SuppressLint("PrivateApi")
    private static String systemProperty(String key) {
        try {
            Object v = Class.forName("android.os.SystemProperties").getMethod("get", String.class).invoke(null, key);
            return v == null ? "" : v.toString();
        } catch (ReflectiveOperationException | RuntimeException e) {
            return "";
        }
    }

    /** Advances the clock each frame; ends after {@link #total}, or after the short fade a key started. */
    void setDrift(Drift drift) {
        this.drift = drift;
    }

    private boolean tick() {
        if (startedAt < 0L) return false;
        long now = SystemClock.uptimeMillis();
        if (loop) {
            ms = now - startedAt;
            // After the opening the sea moves slowly: 25 frames a second is plenty and spares a 1 GB TV.
            if (ms > 3200f) postInvalidateDelayed(40);
            else postInvalidateOnAnimation();
            return true;
        }
        ms = Math.min(total, now - startedAt);
        if (fadeStartedAt >= 0L) {
            float k = clamp((now - fadeStartedAt) / 220f);
            setAlpha(1f - k);
            if (k >= 1f) { finish(); return false; }
        } else if (ms >= total) {
            finish();
            return false;
        }
        postInvalidateOnAnimation();
        return true;
    }

    boolean playing() {
        return !ending && getParent() != null;
    }

    /** A key was pressed: fade out now. */
    void skip() {
        if (ending || fadeStartedAt >= 0L) return;
        fadeStartedAt = SystemClock.uptimeMillis();
        postInvalidateOnAnimation();
    }

    private void finish() {
        if (ending) return;
        ending = true;
        android.util.Log.i("OnePieceIntro", "opening ended at " + Math.round(ms) + " ms");
        post(() -> {
            if (getParent() instanceof android.view.ViewGroup) ((android.view.ViewGroup) getParent()).removeView(this);
            if (logo != null) logo.recycle();
            if (onEnd != null) onEnd.run();
        });
    }

    @Override
    protected void onDraw(Canvas c) {
        if (ending) return;
        int w = getWidth(), h = getHeight();
        if (startedAt < 0L) {
            c.drawColor(0xFF04121F);   // holding: the opening's first frame, until the boot animation is gone
            return;
        }
        if (!tick()) return;
        float dx = 0f, dy = 0f;
        if (loop) {
            dx = (float) Math.sin(ms / 53000.0 * Math.PI * 2) * 28f * d;
            dy = (float) Math.sin(ms / 71000.0 * Math.PI * 2) * 12f * d;
            if (drift != null) drift.moved(dx, dy);
        }
        float cx = w / 2f + dx, cy = h * (loop ? 0.47f : 0.46f) + dy;
        float size = 1f;
        // The whole opening fades into Home over its last 600 ms; the screensaver never fades.
        float out = loop ? 1f : clamp((total - ms) / (reduced ? 500f : 600f));

        // Deep sea, lit from where the logo lands.
        p.setShader(new RadialGradient(cx, cy, Math.max(w, h) * 0.75f, new int[]{0xFF1E5A8C, 0xFF0B2A4A, 0xFF04121F},
                new float[]{0f, 0.55f, 1f}, Shader.TileMode.CLAMP));
        p.setAlpha(Math.round(255 * out));
        c.drawRect(0, 0, w, h, p);
        p.setShader(null);

        if (reduced) {
            drawLogo(c, cx, cy, size, 0f, out);
            if (loop) waves(c, w, h, 1f);
            return;
        }

        // Gold sun rays, turning slowly, opening in the first 0.8 s.
        float rays = clamp(ms / 800f) * out;
        if (rays > 0f) {
            float turn = ms * 0.012f;
            float reach = (float) Math.hypot(w, h);
            fx.setStyle(Paint.Style.FILL);
            fx.setShader(new RadialGradient(cx, cy, reach * 0.55f, 0x66FFD54F, 0x00FFD54F, Shader.TileMode.CLAMP));
            fx.setAlpha(Math.round(255 * rays));
            int n = 18;
            for (int i = 0; i < n; i++) {
                double a0 = Math.toRadians(turn + i * 360.0 / n), half = Math.toRadians(5.5);
                path.reset();
                path.moveTo(cx, cy);
                path.lineTo(cx + (float) Math.cos(a0 - half) * reach, cy + (float) Math.sin(a0 - half) * reach);
                path.lineTo(cx + (float) Math.cos(a0 + half) * reach, cy + (float) Math.sin(a0 + half) * reach);
                path.close();
                c.drawPath(path, fx);
            }
            fx.setShader(null);
        }

        // Manga speed lines rushing in as the logo lands (0.35 to 1.2 s).
        float lines = window(ms, 350f, 1200f);
        if (lines > 0f) {
            fx.setStyle(Paint.Style.STROKE);
            fx.setStrokeCap(Paint.Cap.ROUND);
            java.util.Random rnd = new java.util.Random(5);
            float reach = (float) Math.hypot(w, h) / 2f;
            for (int i = 0; i < 64; i++) {
                double a = rnd.nextDouble() * Math.PI * 2;
                float in = reach * (0.42f + rnd.nextFloat() * 0.25f), len = reach * (0.15f + rnd.nextFloat() * 0.3f);
                float slide = (1f - lines) * reach * 0.2f;
                fx.setColor(0xFFFFFFFF);
                fx.setAlpha(Math.round(130 * (float) Math.sin(lines * Math.PI) * out));
                fx.setStrokeWidth((1f + rnd.nextFloat() * 2f) * d);
                float x0 = cx + (float) Math.cos(a) * (in + slide), y0 = cy + (float) Math.sin(a) * (in + slide);
                c.drawLine(x0, y0, x0 + (float) Math.cos(a) * len, y0 + (float) Math.sin(a) * len, fx);
            }
        }

        // The logo: drops in from small with a bounce (0.3 to 1.1 s), then breathes a little.
        float land = clamp((ms - 300f) / 800f);
        if (land > 0f) {
            float scale = size * easeOutBack(land) * (1f + 0.012f * (float) Math.sin(ms / 420f));
            float tilt = (1f - land) * -8f;
            float shine = window(ms, 1150f, 1950f) > 0f ? clamp((ms - 1150f) / 800f) : -1f;
            if (loop && ms > 1950f) {
                // The screensaver: the shine crosses the logo again every 9 s.
                float cycle = (ms - 1150f) % 9000f;
                shine = cycle < 800f ? cycle / 800f : -1f;
            }
            drawLogo(c, cx, cy, scale, tilt, Math.min(clamp(land * 2f), out), shine);
        }

        // Sparkles round the logo (1.2 to 3 s; the screensaver brings them back every 7 s).
        float st = loop && ms > 3000f ? 1200f + (ms - 3000f) % 7000f : ms;
        sparkle(c, cx - w * 0.21f * size, cy - h * 0.13f * size, window(st, 1200f, 2000f) * out, 14f);
        sparkle(c, cx + w * 0.22f * size, cy - h * 0.09f * size, window(st, 1450f, 2300f) * out, 18f);
        sparkle(c, cx + w * 0.17f * size, cy + h * 0.15f * size, window(st, 1800f, 2700f) * out, 12f);
        sparkle(c, cx - w * 0.16f * size, cy + h * 0.13f * size, window(st, 2100f, 3000f) * out, 15f);

        // Foam waves rolling along the bottom.
        waves(c, w, h, clamp(ms / 600f) * out);
    }

    private void drawLogo(Canvas c, float cx, float cy, float scale, float tilt, float alpha) {
        drawLogo(c, cx, cy, scale, tilt, alpha, -1f);
    }

    /** The logo at most a little over its own pixel size (it stays crisp), with an optional shine at {@code shine}. */
    private void drawLogo(Canvas c, float cx, float cy, float scale, float tilt, float alpha, float shine) {
        if (logo == null || alpha <= 0f) return;
        // The screensaver's logo fills more of the screen (the time sits small in a corner there).
        float lw = loop ? Math.min(getWidth() * 0.7f, logo.getWidth() * 1.7f)
                : Math.min(getWidth() * 0.48f, logo.getWidth() * 1.15f);
        float lh = lw * logo.getHeight() / logo.getWidth();
        int save = c.save();
        c.rotate(tilt, cx, cy);
        c.scale(scale, scale, cx, cy);
        r.set(cx - lw / 2f, cy - lh / 2f, cx + lw / 2f, cy + lh / 2f);
        // A soft dark glow behind it so it reads over the rays.
        fx.setStyle(Paint.Style.FILL);
        fx.setShader(new RadialGradient(cx, cy, lw * (loop ? 0.55f : 0.62f), 0x99041220, 0x00041220, Shader.TileMode.CLAMP));
        fx.setAlpha(Math.round(255 * alpha));
        // The screensaver's large logo keeps its shade lower, clear of the time in the top corner.
        float shade = loop ? 0.72f : 0.9f;
        c.drawOval(cx - lw * 0.62f, cy - lh * shade, cx + lw * 0.62f, cy + lh * shade, fx);
        fx.setShader(null);
        int layer = c.saveLayer(r.left - 4, r.top - 4, r.right + 4, r.bottom + 4, null);
        p.setAlpha(Math.round(255 * alpha));
        c.drawBitmap(logo, null, r, p);
        if (shine >= 0f && shine <= 1f) {
            // A white band sweeping over the logo only (drawn on top of its own pixels).
            float band = lw * 0.18f, x = r.left - band * 2 + (r.width() + band * 4) * shine;
            fx.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP));
            fx.setShader(new LinearGradient(x, r.top, x + band, r.top + band * 0.6f,
                    new int[]{0x00FFFFFF, 0xCCFFFFFF, 0x00FFFFFF}, null, Shader.TileMode.CLAMP));
            fx.setAlpha(Math.round(255 * alpha * (float) Math.sin(shine * Math.PI)));
            c.drawRect(r, fx);
            fx.setShader(null);
            fx.setXfermode(null);
        }
        c.restoreToCount(layer);
        c.restoreToCount(save);
    }

    private void sparkle(Canvas c, float x, float y, float k, float sizeDp) {
        if (k <= 0f) return;
        float rad = sizeDp * d * (float) Math.sin(k * Math.PI);
        if (rad < 1f) return;
        float in = rad * 0.12f;
        path.reset();
        path.moveTo(x, y - rad);
        path.quadTo(x + in, y - in, x + rad, y);
        path.quadTo(x + in, y + in, x, y + rad);
        path.quadTo(x - in, y + in, x - rad, y);
        path.quadTo(x - in, y - in, x, y - rad);
        path.close();
        fx.setStyle(Paint.Style.FILL);
        fx.setShader(new RadialGradient(x, y, rad, 0xFFFFFFFF, 0xFFFFE27A, Shader.TileMode.CLAMP));
        fx.setAlpha(255);
        c.save();
        c.rotate(k * 40f, x, y);
        c.drawPath(path, fx);
        c.restore();
        fx.setShader(null);
    }

    private void waves(Canvas c, int w, int h, float a) {
        if (a <= 0f) return;
        for (int layer = 0; layer < 2; layer++) {
            float base = h * (layer == 0 ? 0.86f : 0.92f);
            float amp = (layer == 0 ? 10f : 7f) * d, len = w / (layer == 0 ? 3.2f : 4.5f);
            float shift = ms * (layer == 0 ? 0.08f : -0.12f) * d;
            path.reset();
            path.moveTo(0, h);
            for (float x = 0; x <= w + 8; x += 8) {
                path.lineTo(x, base + (float) Math.sin((x + shift) / len * Math.PI * 2) * amp);
            }
            path.lineTo(w, h);
            path.close();
            fx.setStyle(Paint.Style.FILL);
            fx.setShader(new LinearGradient(0, base - amp, 0, h, layer == 0 ? 0xFF1C6FA8 : 0xFF0E4F80,
                    0xFF06243F, Shader.TileMode.CLAMP));
            fx.setAlpha(Math.round(255 * a));
            c.drawPath(path, fx);
            fx.setShader(null);
            // Foam along the crest.
            fx.setStyle(Paint.Style.STROKE);
            fx.setStrokeWidth(2.2f * d);
            fx.setColor(0xFFFFFFFF);
            fx.setAlpha(Math.round((layer == 0 ? 200 : 140) * a));
            path.reset();
            for (float x = 0; x <= w + 8; x += 8) {
                float y = base + (float) Math.sin((x + shift) / len * Math.PI * 2) * amp;
                if (x == 0) path.moveTo(x, y); else path.lineTo(x, y);
            }
            c.drawPath(path, fx);
        }
    }

    private static float clamp(float v) {
        return Math.max(0f, Math.min(1f, v));
    }

    /** 0 outside [from, to], rising 0 to 1 across it. */
    private static float window(float t, float from, float to) {
        if (t < from || t > to) return 0f;
        return (t - from) / (to - from);
    }

    private static float easeOutBack(float x) {
        float s = 1.7f, u = x - 1f;
        return 1f + u * u * ((s + 1f) * u + s);
    }


}
