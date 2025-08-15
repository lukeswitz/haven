package org.havenapp.main.alerts;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
import org.havenapp.main.PreferenceManager;

public class BriarAlertChannel implements AlertChannel {
    private static final String TAG = "BriarAlertChannel";
    private static final String BRIAR_PACKAGE = "org.briarproject.briar.android";
    private Context context;
    private PreferenceManager prefs;

    public BriarAlertChannel(Context context) {
        this.context = context;
        this.prefs = new PreferenceManager(context);
    }

    @Override
    public boolean isEnabled() {
        return prefs.getBriarEnabled() && isBriarInstalled();
    }

    @Override
    public boolean isAvailable() {
        return isBriarInstalled();
    }

    private boolean isBriarInstalled() {
        try {
            context.getPackageManager().getPackageInfo(BRIAR_PACKAGE, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    @Override
    public void sendAlert(String message, String mediaPath, int eventType) throws Exception {
        if (!isBriarInstalled()) {
            throw new Exception("Briar is not installed");
        }

        // Create intent to send via Briar
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, message);
        intent.setPackage(BRIAR_PACKAGE);

        // Add media if available
        if (!TextUtils.isEmpty(mediaPath)) {
            Uri mediaUri = Uri.parse("file://" + mediaPath);
            intent.putExtra(Intent.EXTRA_STREAM, mediaUri);
            intent.setType("*/*");
        }

        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);

        Log.d(TAG, "Briar alert intent sent");
    }

    @Override
    public String getChannelName() {
        return "Briar";
    }

    @Override
    public void configure(String... params) {
        if (params.length > 0) {
            prefs.setBriarEnabled(Boolean.parseBoolean(params[0]));
        }
    }

    @Override
    public boolean requiresConfiguration() {
        return !isBriarInstalled();
    }
}