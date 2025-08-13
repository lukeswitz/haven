package org.havenapp.main.ui;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TextView;

import org.havenapp.main.PreferenceManager;
import org.havenapp.main.R;
import org.havenapp.main.sensors.media.MicSamplerTask;
import org.havenapp.main.sensors.media.MicrophoneTaskFactory;

import java.util.ArrayList;
import java.util.LinkedList;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import me.angrybyte.numberpicker.view.ActualNumberPicker;

public class MicrophoneConfigureActivity extends AppCompatActivity implements MicSamplerTask.MicListener {

    private MicSamplerTask microphone;
    private TextView mTextLevel;
    private ActualNumberPicker mNumberTrigger;
    private PreferenceManager mPrefManager;
    private SimpleWaveformExtended mWaveform;
    private LinkedList<Integer> mWaveAmpList;
    private static final int MAX_SLIDER_VALUE = 120;

    private double maxAmp = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_microphone_configure);
        mPrefManager = new PreferenceManager(this.getApplicationContext());

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        setTitle("");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mTextLevel = findViewById(R.id.text_display_level);
        mNumberTrigger = findViewById(R.id.number_trigger_level);
        mWaveform = findViewById(R.id.simplewaveform);
        mWaveform.setMaxVal(100);

        mNumberTrigger.setMinValue(0);
        mNumberTrigger.setMaxValue(MAX_SLIDER_VALUE);

        if (!mPrefManager.getMicrophoneSensitivity().equals(PreferenceManager.MEDIUM))
            mNumberTrigger.setValue(Integer.parseInt(mPrefManager.getMicrophoneSensitivity()));
        else
            mNumberTrigger.setValue(60);

        mNumberTrigger.setListener((oldValue, newValue) -> {
            mWaveform.setThreshold(newValue);
            mPrefManager.setMicrophoneSensitivity(newValue + "");
        });

        initWave();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, 999);
        } else {
            startMic();
        }

    }

    private void initWave() {
        mWaveAmpList = new LinkedList<>();

        // Configure WaveformSeekBar properties
        mWaveform.setWaveWidth(5f);
        mWaveform.setWaveGap(2f);
        mWaveform.setWaveMinHeight(5f);
        mWaveform.setWaveCornerRadius(2f);
        mWaveform.setWaveBackgroundColor(Color.WHITE);
        mWaveform.setWaveProgressColor(ContextCompat.getColor(this, R.color.colorAccent));

        // Initialize with empty sample data
        updateWaveformData();
    }

    private void updateWaveformData() {
        // Convert LinkedList to int array for WaveformSeekBar
        int[] sampleArray = new int[Math.max(mWaveAmpList.size(), 1)];
        for (int i = 0; i < mWaveAmpList.size(); i++) {
            sampleArray[i] = mWaveAmpList.get(i);
        }

        // If empty, add a single zero value
        if (sampleArray.length == 0 || mWaveAmpList.isEmpty()) {
            sampleArray = new int[]{0};
        }

        mWaveform.setSampleFrom(sampleArray);
    }

    private void startMic() {
        String permission = Manifest.permission.RECORD_AUDIO;
        int requestCode = 999;
        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {

                //This is called if user has denied the permission before
                //In this case I am just asking the permission again
                ActivityCompat.requestPermissions(this, new String[]{permission}, requestCode);

            } else {

                ActivityCompat.requestPermissions(this, new String[]{permission}, requestCode);
            }
        } else {

            try {
                microphone = MicrophoneTaskFactory.makeSampler(this);
                microphone.setMicListener(this);
                microphone.execute();
            } catch (MicrophoneTaskFactory.RecordLimitExceeded e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case 999:
                startMic();
                break;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (microphone != null)
            microphone.cancel(true);
    }

    @Override
    public void onSignalReceived(short[] signal) {
        /*
         * We do and average of the 512 samples
         */
        int total = 0;
        int count = 0;
        for (short peak : signal) {
            //Log.i("MicrophoneFragment", "Sampled values are: "+peak);
            if (peak != 0) {
                total += Math.abs(peak);
                count++;
            }
        }
        //  Log.i("MicrophoneFragment", "Total value: " + total);
        int average = 0;
        if (count > 0) average = total / count;
        /*
         * We compute a value in decibels
         */
        double averageDB = 0.0;
        if (average != 0) {
            averageDB = 20 * Math.log10(Math.abs(average));
        }

        if (averageDB > maxAmp) {
            maxAmp = averageDB + 5d; //add 5db buffer
            mNumberTrigger.setValue((int) maxAmp);
            mNumberTrigger.invalidate();
        }

        int perc = (int) ((averageDB / 120d) * 100d) - 10;
        mWaveAmpList.addFirst(perc);

        // Limit the size of the waveform data
        int maxSamples = Math.max(50, getResources().getDisplayMetrics().widthPixels / 10);
        if (mWaveAmpList.size() > maxSamples) {
            mWaveAmpList.removeLast();
        }

        updateWaveformData();
        mTextLevel.setText(getString(R.string.current_noise_base).concat(" ").concat(Integer.toString((int) averageDB)).concat("db"));
    }

    @Override
    public void onMicError() {
        // Handle microphone error
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
        }
        return true;
    }

    /**
     * When user closes the activity
     */
    @Override
    public void onBackPressed() {
        finish();
    }
}