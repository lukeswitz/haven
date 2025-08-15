package org.havenapp.main.alerts;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
import org.havenapp.main.PreferenceManager;

public class SessionAlertChannel implements AlertChannel {
    private static final String TAG = "SessionAlertChannel";
    private static final String SESSION_PACKAGE = "network.loki.messenger";
    private Context context;
    private PreferenceManager prefs;

    public SessionAlertChannel(Context context) {
        this.context = context;
        this.prefs = new PreferenceManager(context);
    }

    @Override
    public boolean isEnabled() {
        return prefs.getSessionEnabled() &&
                !TextUtils.isEmpty(prefs.getSessionId()) &&
                isSessionInstalled();
    }

    @Override
    public boolean isAvailable() {
        return isSessionInstalled();
    }

    private boolean isSessionInstalled() {
        try {
            context.getPackageManager().getPackageInfo(SESSION_PACKAGE, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    @Override
    public void sendAlert(String message, String mediaPath, int eventType) throws Exception {
        if (!isSessionInstalled()) {
            throw new Exception("Session is not installed");
        }

        // Create intent to send via Session
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, message);
        intent.setPackage(SESSION_PACKAGE);

        // Add recipient Session ID if configured
        String sessionId = prefs.getSessionId();
        if (!TextUtils.isEmpty(sessionId)) {
            intent.putExtra("recipient", sessionId);
        }

        // Add media if available
        if (!TextUtils.isEmpty(mediaPath)) {
            Uri mediaUri = Uri.parse("file://" + mediaPath);
            intent.putExtra(Intent.EXTRA_STREAM, mediaUri);
            intent.setType("*/*");
        }

        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);

        Log.d(TAG, "Session alert intent sent");
    }

    @Override
    public String getChannelName() {
        return "Session";
    }

    @Override
    public void configure(String... params) {
        if (params.length > 0) {
            prefs.setSessionId(params[0]);
        }
        if (params.length > 1) {
            prefs.setSessionEnabled(Boolean.parseBoolean(params[1]));
        }
    }

    @Override
    public boolean requiresConfiguration() {
        return !isSessionInstalled() || TextUtils.isEmpty(prefs.getSessionId());
    }
}