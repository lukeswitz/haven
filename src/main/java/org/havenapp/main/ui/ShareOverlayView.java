package org.havenapp.main.ui;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.AttributeSet;
import android.view.View;
import android.widget.RelativeLayout;

import com.stfalcon.imageviewer.StfalconImageViewer;

import org.havenapp.main.R;

import java.util.List;

public class ShareOverlayView extends RelativeLayout {

    private StfalconImageViewer viewer;
    private List<String> imageUrls; // Store the list of image URLs
    private int currentPosition = 0; // Track current position

    public ShareOverlayView(Context context) {
        super(context);
        init();
    }

    public ShareOverlayView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ShareOverlayView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public void setImageViewer(StfalconImageViewer viewer) {
        this.viewer = viewer;
        // Get current position from the viewer
        this.currentPosition = viewer.currentPosition();
    }

    // Add method to set the image URLs list
    public void setImageUrls(List<String> imageUrls) {
        this.imageUrls = imageUrls;
    }

    // Add method to update current position (call this from image change listener)
    public void setCurrentPosition(int position) {
        this.currentPosition = position;
    }

    private void sendShareIntent() {
        if (imageUrls != null && currentPosition >= 0 && currentPosition < imageUrls.size()) {
            Intent shareIntent = new Intent();
            shareIntent.setAction(Intent.ACTION_SEND);
            shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse(imageUrls.get(currentPosition)));
            shareIntent.setType("*/*");
            getContext().startActivity(shareIntent);
        }
    }

    private void init() {
        View view = inflate(getContext(), R.layout.view_image_overlay, this);
        view.findViewById(R.id.btnShare).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                sendShareIntent();
            }
        });
    }
}