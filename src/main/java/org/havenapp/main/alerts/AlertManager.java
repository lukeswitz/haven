package org.havenapp.main.alerts;

import android.content.Context;
import android.util.Log;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AlertManager {
    private static final String TAG = "AlertManager";
    private List<AlertChannel> channels = new ArrayList<>();
    private ExecutorService executor = Executors.newCachedThreadPool();
    private Context context;

    public AlertManager(Context context) {
        this.context = context;
        initializeChannels();
    }

    private void initializeChannels() {
        channels.add(new SMSAlertChannel(context));
        channels.add(new SignalAlertChannel(context));
        channels.add(new MatrixAlertChannel(context));
        channels.add(new BriarAlertChannel(context));
        channels.add(new SessionAlertChannel(context));
    }

    public void sendAlert(String message, String mediaPath, int eventType) {
        for (AlertChannel channel : channels) {
            if (channel.isEnabled() && channel.isAvailable()) {
                executor.execute(() -> {
                    try {
                        channel.sendAlert(message, mediaPath, eventType);
                        Log.d(TAG, "Alert sent via " + channel.getChannelName());
                    } catch (Exception e) {
                        Log.e(TAG, "Failed to send alert via " + channel.getChannelName(), e);
                    }
                });
            }
        }
    }

    public void addChannel(AlertChannel channel) {
        channels.add(channel);
    }

    public void removeChannel(AlertChannel channel) {
        channels.remove(channel);
    }

    public List<AlertChannel> getChannels() {
        return new ArrayList<>(channels);
    }

}