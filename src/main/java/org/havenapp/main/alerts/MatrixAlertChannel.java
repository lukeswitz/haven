package org.havenapp.main.alerts;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import org.havenapp.main.PreferenceManager;

public class MatrixAlertChannel implements AlertChannel {
    private static final String TAG = "MatrixAlertChannel";
    private Context context;
    private PreferenceManager prefs;

    public MatrixAlertChannel(Context context) {
        this.context = context;
        this.prefs = new PreferenceManager(context);
        initializeMatrix();
    }

    private void initializeMatrix() {
        try {
            // Matrix SDK API has changed - this is a placeholder implementation
            Log.w(TAG, "Matrix integration needs to be updated for current SDK version");
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize Matrix", e);
        }
    }

    @Override
    public boolean isEnabled() {
        return prefs.getMatrixEnabled() &&
                !TextUtils.isEmpty(prefs.getMatrixUsername()) &&
                !TextUtils.isEmpty(prefs.getMatrixRoomId());
    }

    @Override
    public boolean isAvailable() {
        // Temporarily disable Matrix until properly implemented
        return false;
    }

    @Override
    public void sendAlert(String message, String mediaPath, int eventType) throws Exception {
        throw new Exception("Matrix integration is currently disabled - SDK needs to be updated");
    }

    @Override
    public String getChannelName() {
        return "Matrix";
    }

    @Override
    public void configure(String... params) {
        if (params.length > 0) {
            prefs.setMatrixUsername(params[0]);
        }
        if (params.length > 1) {
            prefs.setMatrixHomeserver(params[1]);
        }
        if (params.length > 2) {
            prefs.setMatrixRoomId(params[2]);
        }
    }

    @Override
    public boolean requiresConfiguration() {
        return TextUtils.isEmpty(prefs.getMatrixUsername()) ||
                TextUtils.isEmpty(prefs.getMatrixHomeserver()) ||
                TextUtils.isEmpty(prefs.getMatrixRoomId());
    }
}