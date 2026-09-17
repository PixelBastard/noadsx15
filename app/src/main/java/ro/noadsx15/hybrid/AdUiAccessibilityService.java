package ro.noadsx15.hybrid;

import android.accessibilityservice.AccessibilityService;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public final class AdUiAccessibilityService extends AccessibilityService {
    private static final Set<String> EXACT = new HashSet<>(Arrays.asList(
        "skip ad", "skip ads", "sari peste reclamă", "sari peste reclame",
        "omite anunțul", "omite anunțurile", "close ad", "close advertisement",
        "dismiss ad", "închide reclama", "închide anunțul"
    ));
    private static final String[] IDS = {
        "skip_ad_button", "skip_button", "ad_skip_button", "close_ad", "dismiss_ad", "ad_close_button"
    };
    private long lastAction;

    @Override public void onAccessibilityEvent(AccessibilityEvent event) {
        SharedPreferences preferences = getSharedPreferences("settings", MODE_PRIVATE);
        boolean enabled = preferences.getBoolean("master_enabled", false) &&
            preferences.getBoolean("ui_helper", false);
        if (!enabled || SystemClock.elapsedRealtime() - lastAction < 700) return;

        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return;
        AccessibilityNodeInfo target = find(root);
        if (target != null && click(target)) lastAction = SystemClock.elapsedRealtime();
    }

    private AccessibilityNodeInfo find(AccessibilityNodeInfo node) {
        CharSequence text = node.getText();
        CharSequence description = node.getContentDescription();
        String label = ((text == null ? "" : text.toString()) + " " +
            (description == null ? "" : description.toString())).trim().toLowerCase(Locale.ROOT);
        String id = node.getViewIdResourceName();
        if (EXACT.contains(label) || containsKnownId(id)) return node;
        for (int i=0; i<node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child != null) {
                AccessibilityNodeInfo result = find(child);
                if (result != null) return result;
            }
        }
        return null;
    }

    private boolean containsKnownId(String id) {
        if (id == null) return false;
        String normalized = id.toLowerCase(Locale.ROOT);
        for (String known : IDS) if (normalized.contains(known)) return true;
        return false;
    }

    private boolean click(AccessibilityNodeInfo node) {
        AccessibilityNodeInfo current = node;
        for (int depth=0; depth<4 && current!=null; depth++) {
            if (current.isClickable() && current.performAction(AccessibilityNodeInfo.ACTION_CLICK)) return true;
            current = current.getParent();
        }
        return false;
    }

    @Override public void onInterrupt() {}
}
