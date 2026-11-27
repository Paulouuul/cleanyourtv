package com.gathin.tvhome;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Toast;

/** Lets the user change HOME through the system UI, without shell or privileged permissions. The picker is translucent
 *  on Android 14 (permissioncontroller), so this stays behind it as a black backdrop: over Home, its text sat on the
 *  tiles. */
public class HomeSettingsActivity extends Activity {
    @Override
    protected void attachBaseContext(android.content.Context base) {
        super.attachBaseContext(LauncherLocale.wrap(base));   // locale
    }

    private boolean left;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        boolean onePiece = OnePieceSkin.on(Config.load(this));
        if (onePiece) setContentView(backdrop());
        if (state != null) { left = true; return; }
        try {
            startActivity(new Intent(Settings.ACTION_HOME_SETTINGS));
            // The skin's backdrop already carries the help on its parchment; the plain one shows it as a note.
            if (!onePiece) Toast.makeText(this, R.string.home_settings_help, Toast.LENGTH_LONG).show();
        } catch (ActivityNotFoundException | SecurityException unavailable) {
            Toast.makeText(this, R.string.not_available, Toast.LENGTH_LONG).show();
            finish();
        }
    }

    /**
     * One Piece skin: the deep sea behind the system's picker, and at the foot of the screen a parchment sheet with
     * the screen's name in the pirate lettering and the help under it.
     */
    private android.view.View backdrop() {
        float d = getResources().getDisplayMetrics().density;
        android.widget.FrameLayout sea = new android.widget.FrameLayout(this);
        sea.setBackground(OnePieceSkin.ocean(this));
        android.widget.LinearLayout sheet = new android.widget.LinearLayout(this);
        sheet.setOrientation(android.widget.LinearLayout.VERTICAL);
        sheet.setBackground(OnePieceSkin.sheet(d, 22 * d));
        int padH = Math.round(36 * d), padV = Math.round(20 * d);
        sheet.setPadding(padH, padV, padH, padV);
        android.widget.TextView title = new android.widget.TextView(this);
        title.setText(R.string.home_settings);
        title.setTypeface(getResources().getFont(R.font.pirata_one));
        title.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 30);
        title.setTextColor(OnePieceSkin.PAPER_INK);
        sheet.addView(title);
        android.widget.TextView help = new android.widget.TextView(this);
        help.setText(R.string.home_settings_help);
        help.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 15);
        help.setTextColor(0xFF6B4A2A);
        help.setPadding(0, Math.round(6 * d), 0, 0);
        sheet.addView(help);
        android.widget.FrameLayout.LayoutParams at = new android.widget.FrameLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
                android.view.Gravity.BOTTOM);
        int m = Math.round(56 * d);
        at.setMargins(m, 0, m, Math.round(28 * d));
        sea.addView(sheet, at);
        return sea;
    }

    @Override
    protected void onPause() {
        super.onPause();
        left = true;
    }

    /** Back from the picker: this screen was only the backdrop. */
    @Override
    protected void onResume() {
        super.onResume();
        if (left) finish();
    }
}
