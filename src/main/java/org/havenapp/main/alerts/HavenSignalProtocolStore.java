package org.havenapp.main.alerts;

import android.content.Context;
import android.content.SharedPreferences;
import org.whispersystems.libsignal.IdentityKey;
import org.whispersystems.libsignal.IdentityKeyPair;
import org.whispersystems.libsignal.InvalidKeyIdException;
import org.whispersystems.libsignal.SignalProtocolAddress;
import org.whispersystems.libsignal.state.PreKeyRecord;
import org.whispersystems.libsignal.state.SessionRecord;
import org.whispersystems.libsignal.state.SignalProtocolStore;
import org.whispersystems.libsignal.state.SignedPreKeyRecord;

import java.util.List;

public class HavenSignalProtocolStore implements SignalProtocolStore {
    private static final String PREFS_NAME = "haven_signal_store";
    private SharedPreferences prefs;
    private Context context;

    public HavenSignalProtocolStore(Context context) {
        this.context = context;
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    @Override
    public void storeSession(SignalProtocolAddress address, SessionRecord record) {
        String key = "session_" + address.toString();
        // Store in SharedPreferences as a simple implementation
        // TODO serialize the SessionRecord
        prefs.edit().putString(key, "session_data").apply();
    }

    @Override
    public IdentityKeyPair getIdentityKeyPair() {
        return null;
    }

    @Override
    public int getLocalRegistrationId() {
        return prefs.getInt("registration_id", 0);
    }

    @Override
    public boolean saveIdentity(SignalProtocolAddress address, IdentityKey identityKey) {
        return false;
    }

    @Override
    public boolean isTrustedIdentity(SignalProtocolAddress address, IdentityKey identityKey, Direction direction) {
        // Implementation for trust verification
        return true; // Placeholder
    }

    public IdentityKey getIdentity(SignalProtocolAddress address) {
        // Implementation for loading identity
        return null; // Placeholder
    }

    @Override
    public PreKeyRecord loadPreKey(int preKeyId) throws InvalidKeyIdException {
        // Implementation for loading prekey
        return null; // Placeholder
    }

    @Override
    public void storePreKey(int preKeyId, PreKeyRecord record) {
        // Store prekey in SharedPreferences
        prefs.edit().putString("prekey_" + preKeyId, "prekey_data").apply();
    }

    @Override
    public boolean containsPreKey(int preKeyId) {
        return prefs.contains("prekey_" + preKeyId);
    }

    @Override
    public void removePreKey(int preKeyId) {
        prefs.edit().remove("prekey_" + preKeyId).apply();
    }

    @Override
    public SessionRecord loadSession(SignalProtocolAddress address) {
        // Implementation for loading session
        return new SessionRecord();
    }

    @Override
    public List<Integer> getSubDeviceSessions(String name) {
        // Implementation for getting sub-device sessions
        return null; // Placeholder
    }

    @Override
    public boolean containsSession(SignalProtocolAddress address) {
        return prefs.contains("session_" + address.toString());
    }

    @Override
    public void deleteSession(SignalProtocolAddress address) {
        prefs.edit().remove("session_" + address.toString()).apply();
    }

    @Override
    public void deleteAllSessions(String name) {
        // Implementation for deleting all sessions for a name
    }

    @Override
    public SignedPreKeyRecord loadSignedPreKey(int signedPreKeyId) throws InvalidKeyIdException {
        // Implementation for loading signed prekey
        return null; // Placeholder
    }

    @Override
    public List<SignedPreKeyRecord> loadSignedPreKeys() {
        // Implementation for loading all signed prekeys
        return null; // Placeholder
    }

    @Override
    public void storeSignedPreKey(int signedPreKeyId, SignedPreKeyRecord record) {
        // Store signed prekey in SharedPreferences
        prefs.edit().putString("signed_prekey_" + signedPreKeyId, "signed_prekey_data").apply();
    }

    @Override
    public boolean containsSignedPreKey(int signedPreKeyId) {
        return prefs.contains("signed_prekey_" + signedPreKeyId);
    }

    @Override
    public void removeSignedPreKey(int signedPreKeyId) {
        prefs.edit().remove("signed_prekey_" + signedPreKeyId).apply();
    }
}