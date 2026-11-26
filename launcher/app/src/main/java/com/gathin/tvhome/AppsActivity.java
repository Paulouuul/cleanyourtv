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
import android.view.KeyEvent;
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
 * (APPS_COLUMNS_KEY, 4 by default). OK opens the app, Back returns to Home. Holding OK picks an app up: the arrows
 * carry it through the grid, OK or Back puts it down, and the order is kept (APPS_ORDER_KEY); apps it does not
 * know yet follow alphabetically.
 */
public final class AppsActivity extends Activity {

    /** Launcher settings key for the apps per row on this screen. */
    static final String APPS_COLUMNS_KEY = "apps_screen_columns";
    static final int DEFAULT_COLUMNS = 4;
    /** The owner's order on this screen: component keys, one per line. */
    static final String APPS_ORDER_KEY = "apps_screen_order";
    /** Start extra: choose an app for a pinned place instead of opening one. The result carries EXTRA_COMPONENT. */
    static final String EXTRA_PICK = "com.gathin.tvhome.extra.PICK";
    /** Result extra: the chosen app's component, flattened short (the Home tile key). */
    static final String EXTRA_COMPONENT = "com.gathin.tvhome.extra.COMPONENT";
    private boolean pick;
    /** The theme's One Piece skin: solid background, wooden tiles, gold rope focus, click sounds. */
    private boolean onePiece;
    private android.widget.PopupWindow menu;
    private static final long HOLD_OK_MS = 550;
    private static final int MOVE_ACCENT = 0xFF6EC1FF;

    private RecyclerView grid;
    private Tiles tiles;
    private int columns;
    private SharedPreferences choices;
    /** Position of the app being carried, or -1. */
    private int moving = -1;
    private boolean okDown;
    private boolean holdFired;
    private final Runnable holdTimer = () -> {
        if (!okDown) return;
        holdFired = true;
        showMenu();
    };
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
        choices = getSharedPreferences(Config.SETTINGS_NAME, MODE_PRIVATE);
        pick = getIntent().getBooleanExtra(EXTRA_PICK, false);
        columns = Math.max(3, Math.min(8, choices.getInt(APPS_COLUMNS_KEY, DEFAULT_COLUMNS)));

        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        Config skinCfg = Config.load(this);
        onePiece = OnePieceSkin.on(skinCfg);
        OnePieceSkin.backdrop(page, skinCfg);   // the same as Home's
        page.setClipChildren(false);   // the focused tile's zoom reaches past the grid's edge
        int side = dp(56);
        int inset = dp(14);   // room inside the grid for the focused tile's zoom at its edges
        page.setPadding(side - inset, dp(36), side - inset, 0);

        TextView title = new TextView(this);
        title.setText(pick ? R.string.pick_app_title : R.string.apps_screen);
        title.setTextColor(Color.WHITE);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 34);
        title.setTypeface(UiFont.get(this, R.string.font_light));
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tlp.setMarginStart(inset);
        page.addView(title, tlp);
        if (onePiece) {
            OnePieceSkin.loadSounds(this);
            title.setLetterSpacing(0.03f);
            OnePieceText.apply(title, OnePieceText.TITLE);
            title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 44);
        }

        grid = new RecyclerView(this);
        grid.setClipToPadding(false);
        grid.setClipChildren(false);
        grid.setPadding(inset, dp(16), inset, dp(40));
        grid.setLayoutManager(new GridLayoutManager(this, columns));
        int width = getResources().getDisplayMetrics().widthPixels - 2 * side;
        int gap = dp(columns >= 6 ? 14 : 22);
        int tileW = (width - gap * (columns - 1)) / columns;
        tiles = new Tiles(loadApps(), tileW, gap);
        grid.setAdapter(tiles);
        grid.setItemAnimator(null);   // a carried tile jumps cell to cell; no fade between them
        LinearLayout.LayoutParams glp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        page.addView(grid, glp);
        setContentView(page);
        if (!onePiece) PlainText.keepWhite(page);   // one text colour, white, with the skin off
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
        List<String> saved = new ArrayList<>();
        for (String line : choices.getString(APPS_ORDER_KEY, "").split("\n")) if (!line.isEmpty()) saved.add(line);
        Collections.sort(apps, (a, b) -> {
            int ia = saved.indexOf(a.component.flattenToShortString()), ib = saved.indexOf(b.component.flattenToShortString());
            if (ia >= 0 || ib >= 0) {
                if (ia < 0) return 1;
                if (ib < 0) return -1;
                return Integer.compare(ia, ib);
            }
            return a.label.compareToIgnoreCase(b.label);
        });
        return apps;
    }

    // ---- moving an app: hold OK, arrows, OK or Back -------------------------------------------------

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        int code = event.getKeyCode();
        boolean down = event.getAction() == KeyEvent.ACTION_DOWN;
        boolean ok = code == KeyEvent.KEYCODE_DPAD_CENTER || code == KeyEvent.KEYCODE_ENTER
                || code == KeyEvent.KEYCODE_NUMPAD_ENTER;
        if (moving >= 0) {
            if (ok || code == KeyEvent.KEYCODE_BACK || code == KeyEvent.KEYCODE_ESCAPE) {
                if (!down) putDown();
                return true;
            }
            int step = 0;
            if (code == KeyEvent.KEYCODE_DPAD_LEFT) step = -1;
            else if (code == KeyEvent.KEYCODE_DPAD_RIGHT) step = 1;
            else if (code == KeyEvent.KEYCODE_DPAD_UP) step = -columns;
            else if (code == KeyEvent.KEYCODE_DPAD_DOWN) step = columns;
            if (step != 0) {
                if (down) carry(step);
                return true;
            }
            return true;   // nothing else while carrying
        }
        // The remote's Menu key picks the focused app up too.
        if (code == KeyEvent.KEYCODE_MENU) {
            if (!down && focusedPosition() >= 0) showMenu();
            return true;
        }
        if (ok) {
            int pos = focusedPosition();
            if (pos < 0) return super.dispatchKeyEvent(event);
            // The launcher times the hold itself: some remotes never deliver a long press to the view.
            if (down) {
                if (event.getRepeatCount() == 0) {
                    okDown = true;
                    holdFired = false;
                    grid.postDelayed(holdTimer, HOLD_OK_MS);
                } else if (!holdFired && (event.isLongPress() || event.getRepeatCount() > 1)) {
                    // A remote that repeats while held: the repeats say it is a hold before the timer does.
                    grid.removeCallbacks(holdTimer);
                    holdFired = true;
                    showMenu();
                }
            } else {
                okDown = false;
                grid.removeCallbacks(holdTimer);
                if (!holdFired) {
                    if (pick) choose(tiles.apps.get(pos));
                    else open(tiles.apps.get(pos));
                }
                holdFired = false;
            }
            return true;
        }
        return super.dispatchKeyEvent(event);
    }

    private int focusedPosition() {
        View focus = getCurrentFocus();
        if (focus == null) return -1;
        View item = grid.findContainingItemView(focus);
        return item != null ? grid.getChildAdapterPosition(item) : -1;
    }

    /** Pick mode: hand the chosen app back to Home. */
    private void choose(App app) {
        setResult(RESULT_OK, new Intent().putExtra(EXTRA_COMPONENT, app.component.flattenToShortString()));
        finish();
    }

    /** Hold OK or Menu on an app: Move and App info, as on Home's pinned row. None in pick mode. */
    private void showMenu() {
        if (pick || (menu != null && menu.isShowing())) return;
        int pos = focusedPosition();
        if (pos < 0) return;
        App app = tiles.apps.get(pos);
        View anchor = getCurrentFocus();
        View panel = getLayoutInflater().inflate(R.layout.popup_inputs, null, false);
        LinearLayout rows = panel.findViewById(R.id.rows);
        menu = new android.widget.PopupWindow(panel, dp(TileMenu.WIDTH_DP), ViewGroup.LayoutParams.WRAP_CONTENT, true);
        menu.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
        menu.setOutsideTouchable(true);
        menu.setElevation(0f);
        addMenuRow(rows, R.string.tile_move, () -> pickUpAt(pos));
        if (removable(app.component.getPackageName())) addMenuRow(rows, R.string.apps_uninstall, () -> {
            // The system's own confirmation asks; nothing is removed without it.
            Intent delete = new Intent(Intent.ACTION_DELETE, android.net.Uri.fromParts("package", app.component.getPackageName(), null));
            try {
                startActivity(delete.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            } catch (RuntimeException e) {
                android.widget.Toast.makeText(this, R.string.not_available, android.widget.Toast.LENGTH_SHORT).show();
            }
        });
        Intent info = SystemSettings.appInfo(this, app.component.getPackageName());
        if (info != null) addMenuRow(rows, R.string.library_app_info, () -> {
            try {
                startActivity(info.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            } catch (RuntimeException ignored) {
            }
        });
        if (onePiece) OnePieceSkin.parchment(panel);
        showBesideAnchor(menu, panel, anchor);
        rows.getChildAt(0).requestFocus();
    }

    /**
     * Below the tile, or above it when the screen's bottom edge would cut the menu off (the last row of apps), and
     * kept inside the left and right edges. A WRAP_CONTENT popup does not know its height when showAsDropDown places
     * it, so it never flipped up by itself: it is measured here first.
     */
    private void showBesideAnchor(android.widget.PopupWindow popup, View panel, View anchor) {
        int width = popup.getWidth();
        panel.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        int height = panel.getMeasuredHeight();
        int[] place = TileMenu.place(anchor, getWindow().getDecorView(), width, height, FOCUS_SCALE,
                getResources().getDisplayMetrics().density);
        int x = place[0], y = place[1];
        popup.showAtLocation(anchor, Gravity.TOP | Gravity.START, x, y);
    }

    /** An app the owner installed: not part of the firmware, so Uninstall removes it rather than failing. */
    private boolean removable(String pkg) {
        try {
            android.content.pm.ApplicationInfo ai = getPackageManager().getApplicationInfo(pkg, 0);
            return (ai.flags & android.content.pm.ApplicationInfo.FLAG_SYSTEM) == 0;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    /** Back from an uninstall: drop the apps that are gone. */
    @Override
    protected void onResume() {
        super.onResume();
        if (onePiece) {
            View screen = findViewById(android.R.id.content);
            screen.postDelayed(() -> OnePieceText.playIn(screen), 350);
        }
        if (tiles == null) return;
        PackageManager pm = getPackageManager();
        for (int i = tiles.apps.size() - 1; i >= 0; i--) {
            try {
                pm.getApplicationInfo(tiles.apps.get(i).component.getPackageName(), 0);
            } catch (PackageManager.NameNotFoundException gone) {
                tiles.apps.remove(i);
                tiles.notifyItemRemoved(i);
            }
        }
    }

    private void addMenuRow(LinearLayout rows, int title, Runnable action) {
        View row = getLayoutInflater().inflate(R.layout.item_input, rows, false);
        row.findViewById(R.id.icon).setVisibility(View.GONE);
        row.findViewById(R.id.dot).setVisibility(View.GONE);
        row.findViewById(R.id.status).setVisibility(View.GONE);
        ((TextView) row.findViewById(R.id.title)).setText(title);
        // The OK-up of the hold that opened the menu must not activate its first row: only a press made here counts.
        final boolean[] pressed = {false};
        row.setOnKeyListener((v, code, event) -> {
            if (code != KeyEvent.KEYCODE_DPAD_CENTER && code != KeyEvent.KEYCODE_ENTER) return false;
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                if (event.getRepeatCount() == 0) pressed[0] = true;
                return true;
            }
            if (pressed[0]) {
                pressed[0] = false;
                menu.dismiss();
                action.run();
            }
            return true;
        });
        rows.addView(row);
    }

    private void pickUpAt(int pos) {
        if (pos < 0 || pos >= tiles.apps.size()) return;
        moving = pos;
        tiles.notifyItemChanged(pos);
        focusAt(pos);
        if (onePiece) OnePieceSkin.toast(this, R.string.apps_move_hint);
        else android.widget.Toast.makeText(this, R.string.apps_move_hint, android.widget.Toast.LENGTH_SHORT).show();
    }

    private void carry(int step) {
        int to = moving + step;
        if (to < 0 || to >= tiles.apps.size()) return;
        // Left and Right stay in the row; Up and Down keep the column.
        if (Math.abs(step) == 1 && to / columns != moving / columns) return;
        App app = tiles.apps.remove(moving);
        tiles.apps.add(to, app);
        tiles.notifyItemMoved(moving, to);
        moving = to;
        grid.scrollToPosition(to);
        focusAt(to);
    }

    private void putDown() {
        int pos = moving;
        moving = -1;
        tiles.notifyItemChanged(pos);
        focusAt(pos);
        StringBuilder order = new StringBuilder();
        for (App a : tiles.apps) order.append(a.component.flattenToShortString()).append('\n');
        choices.edit().putString(APPS_ORDER_KEY, order.toString()).apply();
    }

    private void focusAt(int pos) {
        grid.post(() -> {
            RecyclerView.ViewHolder vh = grid.findViewHolderForAdapterPosition(pos);
            if (vh instanceof Tiles.Holder) ((Tiles.Holder) vh).card.requestFocus();
        });
    }

    private void open(App app) {
        Intent i = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
                .setComponent(app.component).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            startActivity(i);
            if (onePiece) OnePieceSkin.clickOpen();
            LastOpened.record(this, app.component.getPackageName());
        } catch (RuntimeException e) {
            android.widget.Toast.makeText(this, R.string.not_available, android.widget.Toast.LENGTH_SHORT).show();
        }
    }

    private int dp(float v) {
        return Math.round(v * density);
    }

    private final class Tiles extends RecyclerView.Adapter<Tiles.Holder> {
        final List<App> apps;
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

            Drawable ring;
            if (onePiece) {
                float d = getResources().getDisplayMetrics().density;
                ring = OnePieceSkin.rope(d, dp(14));
                card.setTag(R.id.banner, OnePieceSkin.woodFrame(d, dp(14)));
            } else {
                GradientDrawable plain = new GradientDrawable();
                plain.setColor(Color.TRANSPARENT);
                plain.setCornerRadius(dp(14));
                plain.setStroke(dp(3), Color.WHITE);
                ring = plain;
            }
            Drawable carried;
            if (onePiece) {
                carried = OnePieceSkin.carried(getResources().getDisplayMetrics().density, dp(14));
            } else {
                GradientDrawable plain = new GradientDrawable();
                plain.setColor(Color.TRANSPARENT);
                plain.setCornerRadius(dp(14));
                plain.setStroke(dp(4), MOVE_ACCENT);
                carried = plain;
            }
            card.setTag(R.id.card, carried);
            card.setTag(ring);
            Holder holder = new Holder(item, card, art, name);
            card.setOnFocusChangeListener((v, focused) -> {
                v.animate().cancel();
                v.animate().scaleX(focused ? FOCUS_SCALE : 1f).scaleY(focused ? FOCUS_SCALE : 1f).setDuration(150).start();
                card.setForeground(foregroundFor(card, focused, holder.getBindingAdapterPosition()));
                if (focused && onePiece) OnePieceFrames.Rope.playIfRope(card.getForeground());
                name.setVisibility(focused ? View.VISIBLE : View.INVISIBLE);
            });
            return holder;
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
            h.card.setForeground(foregroundFor(h.card, h.card.isFocused(), position));
        }

        /** White ring on the focused tile; the accent ring on the carried one, focused or not. */
        private Drawable foregroundFor(View card, boolean focused, int position) {
            if (moving >= 0 && position == moving) return (Drawable) card.getTag(R.id.card);
            return focused ? (Drawable) card.getTag() : (Drawable) card.getTag(R.id.banner);
        }

        @Override
        public int getItemCount() {
            return apps.size();
        }
    }
}
