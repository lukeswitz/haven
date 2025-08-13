package org.havenapp.main.ui;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.TextView;

import org.havenapp.main.PreferenceManager;
import org.havenapp.main.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.masoudss.lib.SeekBarOnProgressChanged;
import me.angrybyte.numberpicker.view.ActualNumberPicker;

public class AccelConfigureActivity extends AppCompatActivity implements SensorEventListener {

    private TextView mTextLevel;
    private ActualNumberPicker mNumberTrigger;
    private PreferenceManager mPrefManager;
    private SimpleWaveformExtended mWaveform;
    private List<Integer> mWaveAmpList;

    private static final int MAX_SLIDER_VALUE = 100;
    private static final int MAX_SAMPLES = 50; // Limit the number of samples

    private double maxAmp = 0;

    /**
     * Last update of the accelerometer
     */
    private long lastUpdate = -1;

    /**
     * Current accelerometer values
     */
    private float accel_values[];

    /**
     * Last accelerometer values
     */
    private float last_accel_values[];

    private float mAccelCurrent = SensorManager.GRAVITY_EARTH;
    private float mAccelLast = SensorManager.GRAVITY_EARTH;
    private float mAccel = 0.00f;

    /**
     * Text showing accelerometer values
     */
    private int maxAlertPeriod = 30;
    private int remainingAlertPeriod = 0;
    private boolean alert = false;
    private final static int CHECK_INTERVAL = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_accel_configure);
        mPrefManager = new PreferenceManager(this.getApplicationContext());

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        setTitle("");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Initialize UI first
        mTextLevel = findViewById(R.id.text_display_level);
        mNumberTrigger = findViewById(R.id.number_trigger_level);
        mWaveform = findViewById(R.id.simplewaveform);
        mWaveform.setMaxVal(MAX_SLIDER_VALUE);

        mNumberTrigger.setMinValue(0);
        mNumberTrigger.setMaxValue(MAX_SLIDER_VALUE);

        if (!mPrefManager.getAccelerometerSensitivity().equals(PreferenceManager.HIGH))
            mNumberTrigger.setValue(Integer.parseInt(mPrefManager.getAccelerometerSensitivity()));
        else
            mNumberTrigger.setValue(50);

        mNumberTrigger.setListener((oldValue, newValue) -> {
            mWaveform.setThreshold(newValue);
            mPrefManager.setAccelerometerSensitivity(newValue + "");
        });

        // Only request motion sensor permission for Android 12+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.HIGH_SAMPLING_RATE_SENSORS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.HIGH_SAMPLING_RATE_SENSORS}, 999);
            } else {
                initWave();
                startAccel();
            }
        } else {
            initWave();
            startAccel();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 999) {
            // Start accelerometer regardless of permission result
            initWave();
            startAccel();
        }
    }


    private void initWave() {
        mWaveAmpList = new ArrayList<>();

        // Initialize with some default data to prevent crashes
        for (int i = 0; i < 10; i++) {
            mWaveAmpList.add(0);
        }

        // Configure the waveform seekbar with the new API
        mWaveform.setWaveWidth(15f);
        mWaveform.setWaveGap(2f);
        mWaveform.setWaveMinHeight(5f);
        mWaveform.setWaveCornerRadius(2f);
        mWaveform.setWaveBackgroundColor(ContextCompat.getColor(this, R.color.colorPrimaryDark));
        mWaveform.setWaveProgressColor(ContextCompat.getColor(this, R.color.colorAccent));
        mWaveform.setMaxProgress(100f);
        mWaveform.setProgress(0f);

        // Set initial sample data
        updateWaveformSample();

        // Set progress change listener
        mWaveform.setOnProgressChanged(new SeekBarOnProgressChanged() {
            @Override
            public void onProgressChanged(com.masoudss.lib.WaveformSeekBar waveformSeekBar, float progress, boolean fromUser) {
                // Handle progress changes if needed
            }
        });
    }

    private void updateWaveformSample() {
        // Convert List to int array for the WaveformSeekBar
        int[] samples = new int[mWaveAmpList.size()];
        for (int i = 0; i < mWaveAmpList.size(); i++) {
            samples[i] = mWaveAmpList.get(i);
        }

        // Set the sample data
        mWaveform.setSample(samples);
    }

    private void startAccel() {
        try {
            SensorManager sensorMgr = (SensorManager) getSystemService(AppCompatActivity.SENSOR_SERVICE);
            Sensor sensor = sensorMgr.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

            if (sensor == null) {
                Log.i("AccelerometerFragment", "Warning: no accelerometer");
            } else {
                sensorMgr.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void onSensorChanged(SensorEvent event) {
        long curTime = System.currentTimeMillis();
        // only allow one update every 100ms.
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            if ((curTime - lastUpdate) > CHECK_INTERVAL) {
                long diffTime = (curTime - lastUpdate);
                lastUpdate = curTime;

                accel_values = event.values.clone();

                if (alert && remainingAlertPeriod > 0) {
                    remainingAlertPeriod = remainingAlertPeriod - 1;
                } else {
                    alert = false;
                }

                if (last_accel_values != null) {
                    mAccelLast = mAccelCurrent;
                    mAccelCurrent = (float) Math.sqrt(accel_values[0] * accel_values[0] + accel_values[1] * accel_values[1]
                            + accel_values[2] * accel_values[2]);
                    float delta = mAccelCurrent - mAccelLast;
                    mAccel = (mAccel * 0.9f + delta);

                    double averageDB = 0.0;
                    if (mAccel != 0) {
                        averageDB = 20 * Math.log10(Math.abs(mAccel));
                    }

                    if (averageDB > maxAmp) {
                        maxAmp = averageDB + 5d; // add 5db buffer
                    }

                    // Add new sample at the beginning
                    mWaveAmpList.add(0, (int) Math.abs(mAccel * 10)); // Scale for visibility

                    // Keep only the latest samples
                    if (mWaveAmpList.size() > MAX_SAMPLES) {
                        mWaveAmpList = mWaveAmpList.subList(0, MAX_SAMPLES);
                    }

                    // Update the waveform
                    updateWaveformSample();

                    mTextLevel.setText(getString(R.string.current_accel_base) + " " + (int) mAccel);
                }
                last_accel_values = accel_values.clone();
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Not needed for this implementation
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Unregister sensor listener
        SensorManager sensorMgr = (SensorManager) getSystemService(SENSOR_SERVICE);
        if (sensorMgr != null) {
            sensorMgr.unregisterListener(this);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
        }
        return true;
    }

    /**
     * When user closes the activity
     */
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}