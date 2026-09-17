package ro.noadsx15.hybrid;

import android.Manifest;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.VpnService;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.accessibility.AccessibilityManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.net.InetAddress;
import java.util.List;

public final class MainActivity extends Activity {
    private static final String PREFS = "settings";
    private static final String MASTER_ENABLED = "master_enabled";
    private static final String SETUP_PENDING = "setup_pending";

    private BlocklistStore rules;
    private TextView badge;
    private TextView details;
    private Button startButton;
    private Button stopButton;
    private Button updateButton;

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            render(intent.getStringExtra("message"));
        }
    };

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        rules = new BlocklistStore(this);
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);
        }
        setContentView(buildUi());
        render(null);
    }

    @Override protected void onStart() {
        super.onStart();
        IntentFilter filter = new IntentFilter(DnsVpnService.STATUS);
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(receiver, filter, RECEIVER_NOT_EXPORTED);
        else registerReceiver(receiver, filter);
    }

    @Override protected void onResume() {
        super.onResume();
        boolean pending = getSharedPreferences(PREFS, MODE_PRIVATE).getBoolean(SETUP_PENDING, false);
        if (pending && isAccessibilityReady()) {
            getSharedPreferences(PREFS, MODE_PRIVATE).edit().putBoolean(SETUP_PENDING, false).apply();
            requestVpnPermission();
        }
        render(null);
    }

    @Override protected void onStop() {
        try { unregisterReceiver(receiver); } catch (Throwable ignored) {}
        super.onStop();
    }

    private ScrollView buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(22), dp(38), dp(22), dp(28));
        root.setBackgroundColor(Color.rgb(248, 250, 252));

        add(root, text("NoAds X15 Hybrid", 31, Color.rgb(15,23,42), Gravity.CENTER), 58);
        add(root, text("4.1 • control centralizat", 15, Color.rgb(71,85,105), Gravity.CENTER), 42);

        badge = text("OPRIT", 18, Color.WHITE, Gravity.CENTER);
        details = text("", 14, Color.rgb(71,85,105), Gravity.CENTER);
        add(root, badge, 54);
        add(root, details, 100);

        startButton = button("PORNEȘTE TOT", Color.rgb(22, 163, 74), view -> startEverything());
        stopButton = button("OPREȘTE TOT", Color.rgb(220, 38, 38), view -> stopEverything());
        updateButton = button("Actualizează listele", Color.rgb(15,118,110), view -> updateLists());
        add(root, startButton, 62);
        add(root, stopButton, 62);
        add(root, updateButton, 54);
        add(root, button("Testează DNS", Color.rgb(124,58,237), view -> testDns()), 54);
        add(root, button("Setări Accesibilitate", Color.rgb(71,85,105), view -> openAccessibility()), 54);
        add(root, button("Setări VPN", Color.rgb(71,85,105), view -> startActivity(new Intent(Settings.ACTION_VPN_SETTINGS))), 54);

        TextView note = text(
            "La prima pornire, Android cere separat activarea serviciului de Accesibilitate și acordul VPN. " +
            "După configurarea inițială, PORNEȘTE TOT activează ambele funcții din aplicație. OPREȘTE TOT " +
            "dezactivează filtrarea și face serviciul de Accesibilitate inactiv, chiar dacă rămâne autorizat în setările Android.",
            13, Color.rgb(100,116,139), Gravity.START
        );
        note.setPadding(0, dp(18), 0, 0);
        root.addView(note);

        ScrollView scroll = new ScrollView(this);
        scroll.addView(root);
        return scroll;
    }

    private void startEverything() {
        getSharedPreferences(PREFS, MODE_PRIVATE).edit()
            .putBoolean(MASTER_ENABLED, true)
            .putBoolean("ui_helper", true)
            .apply();

        if (!isAccessibilityReady()) {
            getSharedPreferences(PREFS, MODE_PRIVATE).edit().putBoolean(SETUP_PENDING, true).apply();
            details.setText("Prima configurare: activează «NoAds X15 — Skip/Close Ads», apoi revino în aplicație.");
            openAccessibility();
            return;
        }
        requestVpnPermission();
    }

    private void requestVpnPermission() {
        Intent consent = VpnService.prepare(this);
        if (consent != null) startActivityForResult(consent, 10);
        else startVpn();
    }

    private void startVpn() {
        Intent intent = new Intent(this, DnsVpnService.class).setAction(DnsVpnService.START);
        if (Build.VERSION.SDK_INT >= 26) startForegroundService(intent);
        else startService(intent);
        render("Pornire filtrare și Auto Skip/Close…");
    }

    private void stopEverything() {
        getSharedPreferences(PREFS, MODE_PRIVATE).edit()
            .putBoolean(MASTER_ENABLED, false)
            .putBoolean("ui_helper", false)
            .putBoolean(SETUP_PENDING, false)
            .apply();
        startService(new Intent(this, DnsVpnService.class).setAction(DnsVpnService.STOP));
        stopService(new Intent(this, DnsVpnService.class));
        render("Totul este oprit. Serviciul autorizat de Accesibilitate rămâne inactiv.");
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 10) {
            if (resultCode == RESULT_OK) startVpn();
            else {
                getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                    .putBoolean(MASTER_ENABLED, false).putBoolean("ui_helper", false).apply();
                render("Permisiunea VPN nu a fost acordată.");
            }
        }
    }

    private boolean isAccessibilityReady() {
        AccessibilityManager manager = (AccessibilityManager) getSystemService(ACCESSIBILITY_SERVICE);
        List<AccessibilityServiceInfo> enabled = manager.getEnabledAccessibilityServiceList(
            AccessibilityServiceInfo.FEEDBACK_ALL_MASK
        );
        String expected = getPackageName() + "/" + AdUiAccessibilityService.class.getName();
        String shortExpected = getPackageName() + "/.AdUiAccessibilityService";
        for (AccessibilityServiceInfo info : enabled) {
            if (info.getResolveInfo() == null || info.getResolveInfo().serviceInfo == null) continue;
            String id = info.getResolveInfo().serviceInfo.packageName + "/" + info.getResolveInfo().serviceInfo.name;
            if (id.equals(expected) || id.equals(shortExpected) ||
                (info.getId() != null && (info.getId().equals(expected) || info.getId().equals(shortExpected)))) {
                return true;
            }
        }
        return false;
    }

    private void openAccessibility() {
        startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
    }

    private void updateLists() {
        if (DnsVpnService.running) {
            toast("Oprește protecția înainte de actualizare");
            return;
        }
        updateButton.setEnabled(false);
        details.setText("Pregătesc actualizarea…");
        new Thread(() -> {
            try {
                int count = rules.update(message -> runOnUiThread(() -> details.setText(message)));
                runOnUiThread(() -> { updateButton.setEnabled(true); render("Liste actualizate: " + count + " domenii"); });
            } catch (Exception error) {
                runOnUiThread(() -> { updateButton.setEnabled(true); render("Actualizare eșuată: " + error.getMessage()); });
            }
        }).start();
    }

    private void testDns() {
        details.setText("Testez…");
        new Thread(() -> {
            String result;
            try { result = "DNS OK: " + InetAddress.getByName("example.com").getHostAddress(); }
            catch (Throwable error) { result = "DNS eșuat: " + error.getClass().getSimpleName(); }
            String finalResult = result;
            runOnUiThread(() -> details.setText(finalResult));
        }).start();
    }

    private void render(String message) {
        boolean master = getSharedPreferences(PREFS, MODE_PRIVATE).getBoolean(MASTER_ENABLED, false);
        boolean accessibility = isAccessibilityReady();
        boolean vpn = DnsVpnService.running;
        boolean fullyActive = master && accessibility && vpn;

        badge.setText(fullyActive ? "TOTUL ACTIV" : (master ? "CONFIGURARE INCOMPLETĂ" : "TOTUL OPRIT"));
        badge.setBackground(rounded(
            fullyActive ? Color.rgb(22,163,74) : (master ? Color.rgb(217,119,6) : Color.rgb(100,116,139)), 22
        ));

        details.setText(message != null ? message :
            "VPN: " + (vpn ? "activ" : "oprit") +
            " • Auto Skip/Close: " + (master && accessibility ? "activ" : "oprit") +
            "\n" + rules.size() + " domenii • Blocate " + DnsVpnService.blockedCount.get());
        startButton.setEnabled(!fullyActive);
        stopButton.setEnabled(master || vpn);
    }

    private TextView text(String value, int size, int color, int gravity) {
        TextView view = new TextView(this); view.setText(value); view.setTextSize(size); view.setTextColor(color); view.setGravity(gravity); return view;
    }
    private Button button(String value, int color, android.view.View.OnClickListener listener) {
        Button button = new Button(this); button.setText(value); button.setAllCaps(false); button.setTextColor(Color.WHITE); button.setBackground(rounded(color, 13)); button.setOnClickListener(listener); return button;
    }
    private GradientDrawable rounded(int color, int radius) { GradientDrawable d=new GradientDrawable(); d.setColor(color); d.setCornerRadius(dp(radius)); return d; }
    private void add(LinearLayout parent, android.view.View child, int height) { LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(height)); p.setMargins(0,dp(5),0,dp(5)); parent.addView(child,p); }
    private int dp(int value) { return (int)(value * getResources().getDisplayMetrics().density); }
    private void toast(String value) { Toast.makeText(this,value,Toast.LENGTH_SHORT).show(); }
}
