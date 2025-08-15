package org.havenapp.main.service;

import android.app.Activity;
import android.content.Context;
import android.graphics.PixelFormat;
import android.view.Gravity;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;
import com.otaliastudios.cameraview.CameraView;
import org.havenapp.main.ui.CameraViewHolder;

public class BackgroundCamera {

    private WindowManager windowManager;
    private SurfaceView surfaceView;
    private CameraViewHolder cameraViewHolder;
    private boolean isActive = false;

    public void startCamera(Context context) {
        if (isActive) return;

        windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        surfaceView = new SurfaceView(context);

        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams(
                1, 1,
                WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY,
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH |
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
        );
        layoutParams.gravity = Gravity.LEFT | Gravity.TOP;
        windowManager.addView(surfaceView, layoutParams);

        surfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder surfaceHolder) {
                // Initialize camera detection here
                initCameraDetection(context);
            }

            @Override
            public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
                stopCameraDetection();
            }
        });

        isActive = true;
    }

    private void initCameraDetection(Context context) {
        // Create invisible CameraView for background detection
        CameraView hiddenCameraView = new CameraView(context);
        cameraViewHolder = new CameraViewHolder((Activity) context, hiddenCameraView);
        cameraViewHolder.startCamera();
    }

    public void stopCamera() {
        if (!isActive) return;

        stopCameraDetection();
        if (surfaceView != null && windowManager != null) {
            windowManager.removeView(surfaceView);
        }
        isActive = false;
    }

    private void stopCameraDetection() {
        if (cameraViewHolder != null) {
            cameraViewHolder.stopCamera();
            cameraViewHolder.destroy();
            cameraViewHolder = null;
        }
    }
}