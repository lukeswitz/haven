package org.havenapp.main.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.github.paolorotolo.appintro.AppIntro;
import com.github.paolorotolo.appintro.AppIntroFragment;

import org.havenapp.main.MonitorActivity;
import org.havenapp.main.PreferenceManager;
import org.havenapp.main.R;

public class PPAppIntro extends AppIntro {

    private static final int REQUEST_ACCEL_CONFIG = 1001;
    private static final int REQUEST_MIC_CONFIG = 1002;
    private static final int REQUEST_CAMERA_CONFIG = 1003;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Configure intro animations and behavior
        setFadeAnimation();
        setWizardMode(true);
        setBackButtonVisibilityWithDone(true);
        showPagerIndicator(true);
        setIndicatorColor(
                getResources().getColor(R.color.colorAccent),
                getResources().getColor(R.color.colorPrimaryLight)
        );

        // Slide 1: Welcome & App Purpose
        addSlide(AppIntroFragment.newInstance(
                getString(R.string.intro1_title),
                getString(R.string.intro1_desc),
                R.drawable.web_hi_res_512,
                getResources().getColor(R.color.colorPrimaryDark)
        ));

        // Slide 2: Privacy & Security Focus
        CustomSlideBigText privacySlide = CustomSlideBigText.newInstance(R.layout.custom_slide_big_text);
        privacySlide.setTitle(getString(R.string.intro2_title));
        addSlide(privacySlide);

        // Slide 3: Sensor Configuration
        CustomSlideBigText configSlide = CustomSlideBigText.newInstance(R.layout.custom_slide_big_text);
        configSlide.setTitle(getString(R.string.intro3_desc));
        configSlide.showButton(getString(R.string.action_configure), new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startConfigurationFlow();
            }
        });
        addSlide(configSlide);

        // Slide 4: How It Works
        CustomSlideBigText howItWorksSlide = CustomSlideBigText.newInstance(R.layout.custom_slide_big_text);
        howItWorksSlide.setTitle(getString(R.string.intro4_desc));
        addSlide(howItWorksSlide);

        // Slide 5: Notifications Setup
        final CustomSlideNotify notifySlide = CustomSlideNotify.newInstance(R.layout.custom_slide_notify);
        notifySlide.setSaveListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String phoneNumber = notifySlide.getPhoneNumber();
                if (isValidPhoneNumber(phoneNumber)) {
                    PreferenceManager pm = new PreferenceManager(PPAppIntro.this);
                    pm.setRemotePhoneNumber(phoneNumber);
                    Toast.makeText(PPAppIntro.this, R.string.phone_saved, Toast.LENGTH_SHORT).show();
                    moveToNextSlide();
                } else {
                    Toast.makeText(PPAppIntro.this, R.string.invalid_phone_number, Toast.LENGTH_SHORT).show();
                }
            }
        });
        addSlide(notifySlide);

        // Slide 6: Ready to Protect
        addSlide(AppIntroFragment.newInstance(
                getString(R.string.intro5_title),
                getString(R.string.intro5_desc),
                R.drawable.web_hi_res_512,
                getResources().getColor(R.color.colorPrimaryDark)
        ));

        // Customize button text
        setDoneText(getString(R.string.onboarding_action_end));

        // Hide skip button for security app
        showSkipButton(false);
    }

    private void startConfigurationFlow() {
        // Start with accelerometer configuration first
        Intent accelIntent = new Intent(this, AccelConfigureActivity.class);
        accelIntent.putExtra("from_onboarding", true);
        startActivityForResult(accelIntent, REQUEST_ACCEL_CONFIG);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case REQUEST_ACCEL_CONFIG:
                // After accelerometer config, start microphone config
                Intent micIntent = new Intent(this, MicrophoneConfigureActivity.class);
                micIntent.putExtra("from_onboarding", true);
                startActivityForResult(micIntent, REQUEST_MIC_CONFIG);
                break;

            case REQUEST_MIC_CONFIG:
                // After microphone config, start camera config
                Intent cameraIntent = new Intent(this, CameraConfigureActivity.class);
                cameraIntent.putExtra("from_onboarding", true);
                startActivityForResult(cameraIntent, REQUEST_CAMERA_CONFIG);
                break;

            case REQUEST_CAMERA_CONFIG:
                // Configuration flow complete
                Toast.makeText(this, R.string.sensors_configured, Toast.LENGTH_SHORT).show();
                break;
        }
    }

    private boolean isValidPhoneNumber(String phoneNumber) {
        // Basic phone number validation
        return phoneNumber != null &&
                phoneNumber.trim().length() >= 10 &&
                phoneNumber.matches("^[+]?[0-9\\s\\-\\(\\)]+$");
    }

    private void moveToNextSlide() {
        getPager().setCurrentItem(getPager().getCurrentItem() + 1);
    }

    @Override
    public void onSkipPressed(Fragment currentFragment) {
        super.onSkipPressed(currentFragment);
        // Set default preferences if user skips
        setDefaultPreferences();
        launchMainActivity();
    }

    @Override
    public void onDonePressed(Fragment currentFragment) {
        super.onDonePressed(currentFragment);

        // Mark onboarding as complete
        PreferenceManager pm = new PreferenceManager(this);
        pm.setFirstLaunch(false);

        // Show completion message
        Toast.makeText(this, R.string.setup_complete, Toast.LENGTH_SHORT).show();

        launchMainActivity();
    }

    private void launchMainActivity() {
        Intent intent = new Intent(this, MonitorActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.putExtra("onboarding_complete", true);
        startActivity(intent);

        setResult(RESULT_OK);
        finish();
    }

    private void setDefaultPreferences() {
        PreferenceManager pm = new PreferenceManager(this);
        // Set reasonable defaults if user skips setup
        pm.setMicrophoneSensitivity("Medium");
        pm.setAccelerometerSensitivity("50");
        pm.setCameraSensitivity(30000);
        pm.setTimerDelay(30); // 30 second countdown
    }

    @Override
    public void onSlideChanged(@Nullable Fragment oldFragment, @Nullable Fragment newFragment) {
        super.onSlideChanged(oldFragment, newFragment);

        // Optional: Add slide-specific logic here
        int position = getPager().getCurrentItem();
        switch (position) {
            case 0:
                // Welcome slide
                break;
            case 2:
                // Configuration slide - check sensor availability
                checkSensorAvailability();
                break;
            case 4:
                // Notification slide - prefill existing phone number
                prefillPhoneNumber();
                break;
        }
    }

    private void checkSensorAvailability() {
        // Check if required sensors are available and show warnings if not
        // This could be implemented to inform users about missing sensors
    }

    private void prefillPhoneNumber() {
        // If user has already entered a phone number, prefill it
        PreferenceManager pm = new PreferenceManager(this);
        String existingNumber = pm.getRemotePhoneNumber();
        if (!existingNumber.isEmpty()) {
            // Prefill the phone number field if it exists
        }
    }

    @Override
    public void onBackPressed() {
        // Custom back button behavior for security app
        if (getPager().getCurrentItem() == 0) {
            // On first slide, show exit confirmation
            new AlertDialog.Builder(this)
                    .setTitle(R.string.exit_setup_title)
                    .setMessage(R.string.exit_setup_message)
                    .setPositiveButton(R.string.continue_setup, null)
                    .setNegativeButton(R.string.exit_anyway, (dialog, which) -> {
                        setDefaultPreferences();
                        finish();
                    })
                    .show();
        } else {
            super.onBackPressed();
        }
    }
}