package org.havenapp.main.alerts;

public interface AlertChannel {
    boolean isEnabled();
    boolean isAvailable();
    void sendAlert(String message, String mediaPath, int eventType) throws Exception;
    String getChannelName();
    void configure(String... params);
    boolean requiresConfiguration();
}