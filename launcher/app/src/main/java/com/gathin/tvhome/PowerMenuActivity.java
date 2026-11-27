package com.gathin.tvhome;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * The launcher's own power menu, opened from the settings menu, in place of Android's (the phone layout, which a
 * D-pad cannot use): "Power off the TV?" with Yes (rest, as a tap on the remote's power key, carried out by the Home key
 * helper) and No, which starts focused. Back closes it too. In the One Piece skin it is a parchment sheet with a
 * lantern and an anchor. No Restart: on the TCL 32S5K, Android's own Restart switches the TV off instead of restarting it.
 * A held power key is not replaced: Android shows its own confirmation before an accessibility service hears of
 * it, so it would always flash first. On the TCL 32S5K a held key shuts the TV off with no dialog instead
 * (settings put global power_button_long_press 3), and a tap rests it (screen off).
 */
public class PowerMenuActivity extends Activity {

    private float density;
    private boolean onePiece;
    private boolean reducedMotion;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LauncherLocale.wrap(base));
    }

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        if (!HomeKeyService.canOpenPowerMenu()) {
            finish();
            return;
        }
        Config cfg = Config.load(this);
        onePiece = OnePieceSkin.on(cfg);
        reducedMotion = cfg.reducedMotion;
        density = getResources().getDisplayMetrics().density;
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        getWindow().setDimAmount(0.6f);

        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setGravity(Gravity.CENTER_HORIZONTAL);
        int pad = (int) (22 * density);
        panel.setPadding(pad, (int) (18 * density), pad, pad);
        panel.setBackgroundResource(R.drawable.glass_panel);

        TextView title = new TextView(this);
        title.setText(R.string.power_confirm);
        title.setGravity(Gravity.CENTER);
        if (onePiece) {
            title.setTypeface(getResources().getFont(R.font.pirata_one));
            title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 26);
            title.setTextColor(OnePieceSkin.PAPER_INK);
        } else {
            title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 19);
            title.setTextColor(Color.WHITE);
        }
        panel.addView(title);

        LinearLayout buttons = new LinearLayout(this);
        buttons.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams under = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        under.topMargin = (int) (14 * density);
        panel.addView(buttons, under);
        // Room for the focus mark: its loop and its cross reach past the button, and the button grows on focus.
        int room = (int) (12 * density);
        buttons.setPadding(room, room, room, room);
        buttons.setClipChildren(false);
        buttons.setClipToPadding(false);
        panel.setClipChildren(false);
        panel.setClipToPadding(false);
        addButton(buttons, R.string.power_yes, R.drawable.ic_power, OnePieceIcon.LANTERN, HomeKeyService::powerOff);
        // No: the ship stays at anchor. It takes the focus, so a stray OK does not switch the TV off.
        View stay = addButton(buttons, R.string.power_no, R.drawable.ic_close, OnePieceIcon.ANCHOR, () -> { });

        if (onePiece) {
            panel.setBackground(OnePieceSkin.sheet(density, 22 * density));
            OnePieceSkin.clickPanel();
        }
        FrameLayout screen = new FrameLayout(this);
        screen.addView(panel, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER));
        setContentView(screen);
        panel.setAlpha(0f);
        panel.setScaleX(0.9f);
        panel.setScaleY(0.9f);
        panel.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(reducedMotion ? Motion.REDUCED_MS : 240)
                .setInterpolator(new android.view.animation.OvershootInterpolator(1.4f)).start();
        panel.post(stay::requestFocus);
    }

    @Override
    protected void onPause() {
        super.onPause();
        finish();   // never left waiting behind an app
    }

    private View addButton(LinearLayout buttons, int label, int plainIcon, int skinIcon, Runnable action) {
        LinearLayout button = new LinearLayout(this);
        button.setOrientation(LinearLayout.VERTICAL);
        button.setGravity(Gravity.CENTER);
        button.setFocusable(true);
        button.setClickable(true);
        int pad = (int) (12 * density);
        button.setPadding(pad, pad, pad, pad);
        ImageView icon = new ImageView(this);
        icon.setDuplicateParentStateEnabled(true);
        int iconSize = (int) ((onePiece ? 72 : 48) * density);
        button.addView(icon, new LinearLayout.LayoutParams(iconSize, iconSize));
        TextView text = new TextView(this);
        text.setDuplicateParentStateEnabled(true);
        text.setText(label);
        text.setGravity(Gravity.CENTER);
        text.setSingleLine(true);
        LinearLayout.LayoutParams gap = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        gap.topMargin = (int) (8 * density);
        button.addView(text, gap);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams((int) (156 * density), (int) (150 * density));
        int m = (int) (14 * density);
        lp.setMargins(m, 0, m, 0);
        buttons.addView(button, lp);

        android.content.res.ColorStateList ink;
        if (onePiece) {
            OnePieceIcon drawn = new OnePieceIcon(density, skinIcon);
            drawn.setReducedMotion(reducedMotion);
            icon.setImageDrawable(drawn);
            text.setTypeface(getResources().getFont(R.font.pirata_one));
            text.setTextSize(TypedValue.COMPLEX_UNIT_SP, 21);
            ink = new android.content.res.ColorStateList(new int[][]{{android.R.attr.state_focused}, {}},
                    new int[]{OnePieceSkin.MAP_INK, OnePieceSkin.PAPER_INK});
            float radius = 16 * density;
            // On the map: a paper plank, and the map's own focus mark (an ink loop), not the tiles' rope.
            android.graphics.drawable.GradientDrawable face = new android.graphics.drawable.GradientDrawable(
                    android.graphics.drawable.GradientDrawable.Orientation.TL_BR, new int[]{0x55FFF6DC, 0x22B07A3A});
            face.setCornerRadius(radius);
            button.setBackground(face);
            OnePieceSkin.markOnFocus(button, density);
            View.OnFocusChangeListener marked = button.getOnFocusChangeListener();
            button.setOnFocusChangeListener((v, has) -> {
                marked.onFocusChange(v, has);
                if (has) drawn.nudge();
                v.animate().scaleX(has ? 1.05f : 1f).scaleY(has ? 1.05f : 1f)
                        .setDuration(reducedMotion ? 0 : 160).start();
            });
        } else {
            icon.setImageResource(plainIcon);
            text.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            ink = new android.content.res.ColorStateList(new int[][]{{android.R.attr.state_focused}, {}},
                    new int[]{0xFF111111, Color.WHITE});
            icon.setImageTintList(ink);
            button.setBackgroundResource(R.drawable.input_row_bg);
        }
        text.setTextColor(ink);
        button.setOnClickListener(v -> {
            finish();
            action.run();
        });
        return button;
    }
}
