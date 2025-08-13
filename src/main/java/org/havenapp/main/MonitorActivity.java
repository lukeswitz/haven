/*
 * Copyright (c) 2017 Nathanial Freitas
 *
 *   This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.havenapp.main;

import android.Manifest;
import android.animation.ValueAnimator;
import android.app.PictureInPictureParams;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.wdullaer.materialdatetimepicker.time.TimePickerDialog;

import org.havenapp.main.model.EventTrigger;
import org.havenapp.main.service.MonitorService;
import org.havenapp.main.ui.AccelConfigureActivity;
import org.havenapp.main.ui.CameraConfigureActivity;
import org.havenapp.main.ui.CameraFragment;
import org.havenapp.main.ui.MicrophoneConfigureActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import static org.havenapp.main.Utils.getTimerText;

public class MonitorActivity extends AppCompatActivity implements TimePickerDialog.OnTimeSetListener {

    private PreferenceManager preferences = null;

    private TextView txtTimer;

    private CountDownTimer cTimer;

    private boolean mIsMonitoring = false;
    private boolean mIsInitializedLayout = false;
    private boolean mOnTimerTicking = false;

    private final static int REQUEST_CAMERA = 999;
    private final static int REQUEST_TIMER = 1000;

    private CameraFragment mFragmentCamera;

    private View mBtnCamera, mBtnMic, mBtnAccel;
    private Animation mAnimShake;
    private TextView txtStatus;

    private int lastEventType = -1;

    /**
     * Looper used to update back the UI after motion detection
     */
    private final Handler handler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            if (mIsMonitoring) {

                String message = null;
                View view = null;

                switch (msg.what) {
                    case EventTrigger.CAMERA:
                        view = mBtnCamera;
                        message = getString(R.string.motion_detected);
                        break;
                    case EventTrigger.POWER:
                        view = mBtnAccel;
                        message = getString(R.string.power_detected);
                        break;
                    case EventTrigger.MICROPHONE:
                        view = mBtnMic;
                        message = getString(R.string.sound_detected);
                        break;
                    case EventTrigger.ACCELEROMETER:
                    case EventTrigger.BUMP:
                        view = mBtnAccel;
                        message = getString(R.string.device_move_detected);
                        break;
                    case EventTrigger.LIGHT:
                        view = mBtnCamera;
                        message = getString(R.string.status_light);
                        break;
                }

                if (view != null) {
                    view.startAnimation(mAnimShake);
                }

                if (lastEventType != msg.what) {
                    if (!TextUtils.isEmpty(message)) {
                        txtStatus.setText(message);
                    }
                }

                lastEventType = msg.what;
            }
        }
    };

    BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            int eventType = intent.getIntExtra("type",-1);
            boolean detected = intent.getBooleanExtra("detected",true);
            if (detected)
                handler.sendEmptyMessage(eventType);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        initSetupLayout();

        // Check if service is already running
        if (MonitorService.getInstance() != null && MonitorService.getInstance().isRunning()) {
            initActiveLayout();
        }

        // Request permissions after UI is set up
        askForPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, 1);
    }

    private void initActiveLayout() {

        ((Button) findViewById(R.id.btnStartLater)).setText(R.string.action_cancel);
        findViewById(R.id.btnStartNow).setVisibility(View.INVISIBLE);
        findViewById(R.id.timer_text_title).setVisibility(View.INVISIBLE);
        txtTimer.setText(R.string.status_on);

        mOnTimerTicking = false;
        mIsMonitoring = true;
    }

    private void initSetupLayout() {
        Log.d("MonitorActivity", "initSetupLayout called");
        preferences = new PreferenceManager(getApplicationContext());
        setContentView(R.layout.activity_monitor);
        Log.d("MonitorActivity", "setContentView called");

        // Make fragment visible BEFORE any camera operations
        findViewById(R.id.fragment_camera).setVisibility(View.VISIBLE);

        txtTimer = (TextView) findViewById(R.id.timer_text);
        View viewTimer = findViewById(R.id.timer_container);

        int timeM = preferences.getTimerDelay() * 1000;

        txtTimer.setText(getTimerText(timeM));
        txtTimer.setOnClickListener(v -> {
            if (cTimer == null)
                showTimeDelayDialog();
        });

        findViewById(R.id.timer_text_title).setOnClickListener(v -> {
            if (cTimer == null)
                showTimeDelayDialog();
        });

        findViewById(R.id.btnStartLater).setOnClickListener(v -> {
            Log.d("MonitorActivity", "Start Later clicked");
            doCancel();
        });

        findViewById(R.id.btnStartNow).setOnClickListener(v -> {
            Log.d("MonitorActivity", "Start Now clicked");
            ((Button) findViewById(R.id.btnStartLater)).setText(R.string.action_cancel);
            findViewById(R.id.btnStartNow).setVisibility(View.INVISIBLE);
            findViewById(R.id.timer_text_title).setVisibility(View.INVISIBLE);
            initTimer();
        });

        mBtnAccel = findViewById(R.id.btnAccelSettings);
        mBtnAccel.setOnClickListener(v -> {
            Log.d("MonitorActivity", "Accel button clicked");
            if (!mIsMonitoring)
                startActivity(new Intent(MonitorActivity.this, AccelConfigureActivity.class));
        });

        mBtnMic = findViewById(R.id.btnMicSettings);
        mBtnMic.setOnClickListener(v -> {
            Log.d("MonitorActivity", "Mic button clicked");
            if (!mIsMonitoring)
                startActivity(new Intent(MonitorActivity.this, MicrophoneConfigureActivity.class));
        });

        mBtnCamera = findViewById(R.id.btnCameraSwitch);
        mBtnCamera.setOnClickListener(v -> {
            Log.d("MonitorActivity", "Camera button clicked");
            if (!mIsMonitoring)
                configCamera();
        });

        findViewById(R.id.btnSettings).setOnClickListener(v -> {
            Log.d("MonitorActivity", "Settings button clicked");
            showSettings();
        });

        mFragmentCamera = ((CameraFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_camera));

        // remove fragment, do this in onResume instead

        txtStatus = findViewById(R.id.txtStatus);
        mAnimShake = AnimationUtils.loadAnimation(this, R.anim.shake);
        mIsInitializedLayout = true;
    }

    private void configCamera() {

        mFragmentCamera.stopCamera();
        startActivityForResult(new Intent(this, CameraConfigureActivity.class),REQUEST_CAMERA);
    }



    private void updateTimerValue(int val) {
        preferences.setTimerDelay(val);
        int valM = val * 1000;
        txtTimer.setText(getTimerText(valM));
    }

    private void doCancel() {

        boolean wasTimer = false;

        if (cTimer != null) {
            cTimer.cancel();
            cTimer = null;
            mOnTimerTicking = false;
            wasTimer = true;
        }

        if (mIsMonitoring) {
            mIsMonitoring = false;
            stopService(new Intent(this, MonitorService.class));
            finish();
        } else {

            findViewById(R.id.btnStartNow).setVisibility(View.VISIBLE);
            findViewById(R.id.timer_text_title).setVisibility(View.VISIBLE);

            ((Button) findViewById(R.id.btnStartLater)).setText(R.string.start_later);

            int timeM = preferences.getTimerDelay() * 1000;
            txtTimer.setText(getTimerText(timeM));

            if (!wasTimer)
                finish();
        }

    }

    @Override
    public void onPictureInPictureModeChanged (boolean isInPictureInPictureMode, Configuration newConfig) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig);
        }
        if (isInPictureInPictureMode) {
            // Hide the full-screen UI (controls, etc.) while in picture-in-picture mode.
            findViewById(R.id.buttonBar).setVisibility(View.GONE);
        } else {
            // Restore the full-screen UI.
            findViewById(R.id.buttonBar).setVisibility(View.VISIBLE);

        }
    }

    private void showSettings() {

        Intent i = new Intent(this, SettingsActivity.class);

        if (cTimer != null) {
            cTimer.cancel();
            cTimer = null;
            startActivityForResult(i, REQUEST_TIMER);

        } else {
            startActivity(i);
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_TIMER) {
            initTimer();
        }
        else if (requestCode == REQUEST_CAMERA)
        {
            mFragmentCamera.initCamera();
        }
    }

    @Override
    protected void onDestroy() {
        if (!mIsMonitoring)
        {
            mFragmentCamera.stopCamera();
        }
        super.onDestroy();
    }

    private void initTimer() {
        txtTimer.setTextColor(getResources().getColor(R.color.colorAccent));
        cTimer = new CountDownTimer((preferences.getTimerDelay()) * 1000, 1000) {

            public void onTick(long millisUntilFinished) {
                mOnTimerTicking = true;
                txtTimer.setText(getTimerText(millisUntilFinished));
            }

            public void onFinish() {

                txtTimer.setText(R.string.status_on);
                initMonitor();
                mOnTimerTicking = false;
            }

        };

        cTimer.start();


    }

    private void initMonitor() {

        mIsMonitoring = true;
        //ensure folder exists and will not be scanned by the gallery app

        try {
            File fileImageDir = new File(Environment.getExternalStorageDirectory(), preferences.getDefaultMediaStoragePath());
            fileImageDir.mkdirs();
            new FileOutputStream(new File(fileImageDir, ".nomedia")).write(0);
        } catch (IOException e) {
            Log.e("Monitor", "unable to init media storage directory", e);
        }

        //Do something after 100ms
        startService(new Intent(MonitorActivity.this, MonitorService.class));

    }


    @Override
    public void onUserLeaveHint () {
        if (mIsMonitoring) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                enterPictureInPictureMode(new PictureInPictureParams.Builder().build());
            }
        }
    }
    /**
     * When user closes the activity
     */
    @Override
    public void onBackPressed() {

        if (mIsMonitoring) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                enterPictureInPictureMode(new PictureInPictureParams.Builder().build());
            }
            else
            {
                finish();
            }
        }
        else
        {
            finish();
        }


    }

    private void showTimeDelayDialog() {
        int totalSecs = preferences.getTimerDelay();

        int hours = totalSecs / 3600;
        int minutes = (totalSecs % 3600) / 60;
        int seconds = totalSecs % 60;

        TimePickerDialog mTimePickerDialog = TimePickerDialog.newInstance(this, hours, minutes, seconds, true);
        mTimePickerDialog.enableSeconds(true);
        mTimePickerDialog.show(getSupportFragmentManager(), "TimePickerDialog");
    }

    @Override
    public void onResume() {
        super.onResume();

        // Handle fragment view access here when view is guaranteed to exist
        if (mFragmentCamera != null && mFragmentCamera.getView() != null) {
            mFragmentCamera.getView().setClickable(false);
            mFragmentCamera.getView().setFocusable(false);
            Log.d("MonitorActivity", "Fragment view configured");
        }

        if (mIsInitializedLayout && (!mOnTimerTicking) && (!mIsMonitoring)) {
            int totalMilliseconds = preferences.getTimerDelay() * 1000;
            txtTimer.setText(getTimerText(totalMilliseconds));
        }

        IntentFilter filter = new IntentFilter();
        filter.addAction("event");
        LocalBroadcastManager.getInstance(this).registerReceiver(receiver, filter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case 1:
                askForPermission(Manifest.permission.CAMERA, 2);
                break;
            case 2:
                // Permissions done - UI is already initialized
                break;
        }
    }


    private boolean askForPermission(String permission, Integer requestCode) {
        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {

                //This is called if user has denied the permission before
                //In this case I am just asking the permission again
                ActivityCompat.requestPermissions(this, new String[]{permission}, requestCode);

            } else {

                ActivityCompat.requestPermissions(this, new String[]{permission}, requestCode);
            }
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void onTimeSet(TimePickerDialog view, int hourOfDay, int minute, int second) {
        int delaySeconds = second + minute * 60 + hourOfDay * 60 * 60;
        updateTimerValue(delaySeconds);
    }

}
