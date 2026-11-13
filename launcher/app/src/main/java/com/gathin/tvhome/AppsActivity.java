package com.gathin.tvhome;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * apps_screen: every installed TV app in a grid of its own, opened from the Apps button in Home's top bar, so Home
 * keeps only the pinned apps and Continue watching. The number of apps per row is a Launcher settings choice
 * (APPS_COLUMNS_KEY, 4 by default). Alphabetical; OK opens the app, Back returns to Home.
 */
public final class AppsActivity extends Activity {

    /** Launcher settings key for the apps per row on this screen. */
    static final String APPS_COLUMNS_KEY = "apps_screen_columns";
    static final int DEFAULT_COLUMNS = 4;
    private static final float FOCUS_SCALE = 1.08f;

    private float density;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LauncherLocale.wrap(base));
    }

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        density = getResources().getDisplayMetrics().density;
        SharedPreferences choices = getSharedPreferences(Config.SETTINGS_NAME, MODE_PRIVATE);
        int columns = Math.max(3, Math.min(8, choices.getInt(APPS_COLUMNS_KEY, DEFAULT_COLUMNS)));

        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setBackgroundColor(Color.BLACK);
        page.setClipChildren(false);   // the focused tile's zoom reaches past the grid's edge
        int side = dp(56);
        int inset = dp(14);   // room inside the grid for the focused tile's zoom at its edges
        page.setPadding(side - inset, dp(36), side - inset, 0);

        TextView title = new TextView(this);
        title.setText(R.string.apps_screen);
        title.setTextColor(Color.WHITE);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 34);
        title.setTypeface(UiFont.get(this, R.string.font_light));
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tlp.setMarginStart(inset);
        page.addView(title, tlp);

        RecyclerView grid = new RecyclerView(this);
        grid.setClipToPadding(false);
        grid.setClipChildren(false);
        grid.setPadding(inset, dp(16), inset, dp(40));
        grid.setLayoutManager(new GridLayoutManager(this, columns));
        int width = getResources().getDisplayMetrics().widthPixels - 2 * side;
        int gap = dp(columns >= 6 ? 14 : 22);
        int tileW = (width - gap * (columns - 1)) / columns;
        grid.setAdapter(new Tiles(loadApps(), tileW, gap));
        LinearLayout.LayoutParams glp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        page.addView(grid, glp);
        setContentView(page);
        grid.post(() -> {
            View first = grid.getLayoutManager() != null ? grid.getLayoutManager().findViewByPosition(0) : null;
            if (first != null) first.requestFocus();
        });
    }

    private static final class App {
        final String label;
        final ComponentName component;
        final Drawable banner;
        final Drawable icon;

        App(String label, ComponentName component, Drawable banner, Drawable icon) {
            this.label = label;
            this.component = component;
            this.banner = banner;
            this.icon = icon;
        }
    }

    /** The TV apps Home would show, minus hidden_apps and this launcher; one per package. */
    private List<App> loadApps() {
        PackageManager pm = getPackageManager();
        Set<String> hidden = new HashSet<>(Config.load(this).hiddenApps);
        List<App> apps = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (ResolveInfo ri : pm.queryIntentActivities(
                new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER), 0)) {
            if (ri.activityInfo == null || !ri.activityInfo.exported) continue;
            String pkg = ri.activityInfo.packageName;
            if (pkg.equals(getPackageName()) || hidden.contains(pkg) || !seen.add(pkg)) continue;
            String label = Cards.cleanLabel(String.valueOf(ri.loadLabel(pm)), pkg);
            Drawable banner = ri.activityInfo.loadBanner(pm);
            if (banner != null && banner.getIntrinsicWidth() <= banner.getIntrinsicHeight()) banner = null;   // square: use the icon
            apps.add(new App(label, new ComponentName(pkg, ri.activityInfo.name), banner,
                    banner == null ? ri.loadIcon(pm) : null));
        }
        Collections.sort(apps, (a, b) -> a.label.compareToIgnoreCase(b.label));
        return apps;
    }

    private void open(App app) {
        Intent i = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
                .setComponent(app.component).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            startActivity(i);
            LastOpened.record(this, app.component.getPackageName());
        } catch (RuntimeException e) {
            android.widget.Toast.makeText(this, R.string.not_available, android.widget.Toast.LENGTH_SHORT).show();
        }
    }

    private int dp(float v) {
        return Math.round(v * density);
    }

    private final class Tiles extends RecyclerView.Adapter<Tiles.Holder> {
        private final List<App> apps;
        private final int tileW, tileH, gap;

        Tiles(List<App> apps, int tileW, int gap) {
            this.apps = apps;
            this.tileW = tileW;
            this.tileH = tileW * 9 / 16;
            this.gap = gap;
        }

        final class Holder extends RecyclerView.ViewHolder {
            final FrameLayout card;
            final ImageView art;
            final TextView name;

            Holder(LinearLayout item, FrameLayout card, ImageView art, TextView name) {
                super(item);
                this.card = card;
                this.art = art;
                this.name = name;
            }
        }

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            Context ctx = parent.getContext();
            LinearLayout item = new LinearLayout(ctx);
            item.setOrientation(LinearLayout.VERTICAL);
            item.setGravity(Gravity.CENTER_HORIZONTAL);
            item.setClipChildren(false);
            RecyclerView.LayoutParams ilp = new RecyclerView.LayoutParams(tileW, ViewGroup.LayoutParams.WRAP_CONTENT);
            ilp.bottomMargin = gap / 2;
            item.setLayoutParams(ilp);

            FrameLayout card = new FrameLayout(ctx);
            card.setFocusable(true);
            card.setClickable(true);
            GradientDrawable bg = new GradientDrawable();
            bg.setColor(0xFF1E2433);
            bg.setCornerRadius(dp(14));
            card.setBackground(bg);
            card.setClipToOutline(true);
            ImageView art = new ImageView(ctx);
            art.setScaleType(ImageView.ScaleType.CENTER_CROP);
            card.addView(art, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            item.addView(card, new LinearLayout.LayoutParams(tileW, tileH));

            TextView name = new TextView(ctx);
            name.setTextColor(Color.WHITE);
            name.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
            name.setTypeface(UiFont.get(ctx, R.string.font_medium));
            name.setSingleLine(true);
            name.setEllipsize(TextUtils.TruncateAt.END);
            name.setGravity(Gravity.CENTER);
            name.setVisibility(View.INVISIBLE);
            LinearLayout.LayoutParams nlp = new LinearLayout.LayoutParams(tileW, ViewGroup.LayoutParams.WRAP_CONTENT);
            nlp.topMargin = dp(8);
            item.addView(name, nlp);

            GradientDrawable ring = new GradientDrawable();
            ring.setColor(Color.TRANSPARENT);
            ring.setCornerRadius(dp(14));
            ring.setStroke(dp(3), Color.WHITE);
            card.setOnFocusChangeListener((v, focused) -> {
                v.animate().cancel();
                v.animate().scaleX(focused ? FOCUS_SCALE : 1f).scaleY(focused ? FOCUS_SCALE : 1f).setDuration(150).start();
                card.setForeground(focused ? ring : null);
                name.setVisibility(focused ? View.VISIBLE : View.INVISIBLE);
            });
            return new Holder(item, card, art, name);
        }

        @Override
        public void onBindViewHolder(@NonNull Holder h, int position) {
            App app = apps.get(position);
            h.name.setText(app.label);
            h.card.setContentDescription(app.label);
            if (app.banner != null) {
                h.art.setScaleType(ImageView.ScaleType.CENTER_CROP);
                h.art.setPadding(0, 0, 0, 0);
                h.art.setImageDrawable(app.banner);
            } else {
                int pad = tileH / 5;
                h.art.setScaleType(ImageView.ScaleType.FIT_CENTER);
                h.art.setPadding(pad, pad, pad, pad);
                h.art.setImageDrawable(app.icon);
            }
            h.card.setOnClickListener(v -> open(app));
        }

        @Override
        public int getItemCount() {
            return apps.size();
        }
    }
}
