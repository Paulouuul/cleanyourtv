package com.gathin.tvhome;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

/**
 * The panel the remote's Settings button opens when {@code settings_key_panel} is on (HomeKeyService):
 * a few glass rows under the Settings pill, over Home or whatever app is playing. Translucent and in a
 * task of its own, so the app behind keeps running and Back returns to it. Pressing Settings again
 * closes the panel.
 */
public final class QuickSettingsActivity extends Activity {
    /** Extra: Home was in front when the Settings key was pressed, so the panel lines up under its Settings pill. */
    static final String EXTRA_OVER_HOME = "com.gathin.tvhome.extra.OVER_HOME";

    @Override
    protected void attachBaseContext(android.content.Context base) {
        super.attachBaseContext(LauncherLocale.wrap(base));   // locale
    }


    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_quick_settings);
        if (getResources().getBoolean(R.bool.rtl_menu_bar)) {
            View panel = findViewById(R.id.panel);
            android.widget.FrameLayout.LayoutParams position =
                    (android.widget.FrameLayout.LayoutParams) panel.getLayoutParams();
            position.gravity = android.view.Gravity.TOP | android.view.Gravity.LEFT;
            int inset = position.getMarginEnd();
            position.setMarginEnd(0);
            position.setMargins(inset, position.topMargin, 0, position.bottomMargin);
            panel.setLayoutParams(position);
        }
        final boolean overHome = getIntent().hasExtra(EXTRA_OVER_HOME)
                ? getIntent().getBooleanExtra(EXTRA_OVER_HOME, false) : MainActivity.panelOverHome();
        if (!overHome) {
            // Over any other app there is no Settings pill to line up under: the panel sits in the corner, a small
            // gap from the top and side edges of the screen.
            View panel = findViewById(R.id.panel);
            android.widget.FrameLayout.LayoutParams position =
                    (android.widget.FrameLayout.LayoutParams) panel.getLayoutParams();
            int gap = Math.round(16 * getResources().getDisplayMetrics().density);
            position.setMargins(gap, gap, gap, position.bottomMargin);
            position.setMarginStart(gap);
            position.setMarginEnd(gap);
            panel.setLayoutParams(position);
        }
        findViewById(R.id.scrim).setOnClickListener(v -> finish());
        LinearLayout rows = findViewById(R.id.rows);
        addRow(rows, R.drawable.ic_settings, R.string.tv_settings, 0, SystemSettings.intent(this, Settings.ACTION_SETTINGS));
        addRow(rows, R.drawable.ic_picture, R.string.quick_picture, 0, SystemSettings.displayIntent(this));
        addRow(rows, R.drawable.ic_sound, R.string.quick_sound, 0, SystemSettings.soundIntent(this));
        addRow(rows, R.drawable.ic_wifi, R.string.quick_network, 0, SystemSettings.intent(this, Settings.ACTION_WIFI_SETTINGS));
        // Apps and Launcher settings left this panel: it is for the TV's own settings over the app in front, and
        // Launcher settings lives in the Home screen's Settings menu (settings_pill_menu).
        if (OnePieceSkin.on(Config.load(this))) {
            // The One Piece skin: a parchment panel, opened with the anime's alert sound, quietly.
            OnePieceSkin.parchment(findViewById(R.id.panel));
            OnePieceSkin.loadSounds(this);
            OnePieceSkin.clickPanel();
        }
        rows.getChildAt(0).requestFocus();
    }

    /** Settings pressed again while the panel is up. */
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        finish();
    }

    /** Anything else taking the screen (Home, an app, a screensaver) closes the panel. */
    @Override
    @SuppressWarnings("deprecation")
    protected void onPause() {
        super.onPause();
        finish();
        overridePendingTransition(0, 0);
    }

    private void addRow(LinearLayout rows, int icon, int title, int status, Intent target) {
        View row = getLayoutInflater().inflate(R.layout.item_input, rows, false);
        ((ImageView) row.findViewById(R.id.icon)).setImageResource(icon);
        ((TextView) row.findViewById(R.id.title)).setText(title);
        row.findViewById(R.id.dot).setVisibility(View.GONE);
        TextView hint = row.findViewById(R.id.status);
        if (status != 0) hint.setText(status);
        else hint.setVisibility(View.GONE);
        row.setOnClickListener(v -> {
            try {
                startActivity(target.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            } catch (ActivityNotFoundException | SecurityException e) {
                Toast.makeText(this, R.string.not_available, Toast.LENGTH_SHORT).show();
            }
            finish();
        });
        rows.addView(row);
    }
}
