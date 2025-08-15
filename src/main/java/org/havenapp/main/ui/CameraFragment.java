
/*
 * Copyright (c) 2017 Nathanial Freitas / Guardian Project
 *  * Licensed under the GPLv3 license.
 *
 * Copyright (c) 2013-2015 Marco Ziccardi, Luca Bonato
 * Licensed under the MIT license.
 */
package org.havenapp.main.ui;

import android.content.Intent;
import android.hardware.SensorEvent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.otaliastudios.cameraview.CameraView;
import com.otaliastudios.cameraview.controls.Audio;

import org.havenapp.main.PreferenceManager;
import org.havenapp.main.R;
import org.havenapp.main.model.EventTrigger;

public final class CameraFragment extends Fragment {

    private CameraViewHolder cameraViewHolder;
    private ImageView newImage;
    private PreferenceManager prefs;
    private boolean isInInvisibleMode = false;



    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.camera_fragment, container, false);
        newImage = view.findViewById(R.id.new_image);

        return view;

    }

    public void setMotionSensitivity (int threshold)
    {
        cameraViewHolder.setMotionSensitivity(threshold);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = new PreferenceManager(getContext());
    }

    @Override
    public void onPause() {
        super.onPause();
        stopCamera(); // Stop camera to prevent surface issues
    }

    @Override
    public void onResume() {
        super.onResume();
        initCamera();

        cameraViewHolder.setMotionSensitivity(prefs.getCameraSensitivity());
    }

    public void updateCamera ()
    {
        if (cameraViewHolder != null) {
            cameraViewHolder.updateCamera();
        }
    }

    public void switchToInvisibleMode() {
        if (cameraViewHolder != null && !isInInvisibleMode) {
            // Hide the CameraView but keep it in the layout
            View cameraView = getView().findViewById(R.id.camera_view);
            if (cameraView != null) {
                cameraView.setVisibility(View.INVISIBLE);
                // Set size to 1x1 to minimize resources
                ViewGroup.LayoutParams params = cameraView.getLayoutParams();
                params.width = 1;
                params.height = 1;
                cameraView.setLayoutParams(params);
            }
            isInInvisibleMode = true;
        }
    }

    public void switchToVisibleMode() {
        if (cameraViewHolder != null && isInInvisibleMode) {
            View cameraView = getView().findViewById(R.id.camera_view);
            if (cameraView != null) {
                cameraView.setVisibility(View.VISIBLE);
                // Restore full size
                ViewGroup.LayoutParams params = cameraView.getLayoutParams();
                params.width = ViewGroup.LayoutParams.MATCH_PARENT;
                params.height = ViewGroup.LayoutParams.MATCH_PARENT;
                cameraView.setLayoutParams(params);
            }
            isInInvisibleMode = false;
        }
    }

    public void stopCamera() {
        if (cameraViewHolder != null) {
            cameraViewHolder.stopCamera();
        }
    }

    public void initCamera ()
    {


        PreferenceManager prefs = new PreferenceManager(getActivity());

        if (prefs.getCameraActivation()) {
            //Uncomment to see the camera

            CameraView cameraView = getActivity().findViewById(R.id.camera_view);
            cameraView.setAudio(Audio.OFF);

            if (cameraViewHolder == null) {
                cameraViewHolder = new CameraViewHolder(getActivity(), cameraView);

                cameraViewHolder.addListener((percChanged, rawBitmap, motionDetected) -> {

                    if (!isDetached()) {
                        Intent iEvent = new Intent("event");
                        iEvent.putExtra("type", EventTrigger.CAMERA);
                        iEvent.putExtra("detected",motionDetected);
                        iEvent.putExtra("changed",percChanged);

                        LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(iEvent);
                    }

                });
            }

        }

        cameraViewHolder.startCamera();

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopCamera(); // Ensure camera is stopped
        if (cameraViewHolder != null) {
            cameraViewHolder.destroy();
        }
    }

    public void onSensorChanged(SensorEvent event) {

    }
}