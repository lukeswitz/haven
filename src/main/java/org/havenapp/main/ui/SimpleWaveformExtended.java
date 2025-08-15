package org.havenapp.main.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import com.masoudss.lib.WaveformSeekBar;

/**
 * Created by n8fr8 on 10/30/17.
 */
public class SimpleWaveformExtended extends WaveformSeekBar {

    private int mThreshold = 0;
    private int lineY;
    private int maxVal = 100; // default max value of slider
    private Paint thresholdPaint;

    public SimpleWaveformExtended(Context context) {
        super(context);
        init();
    }

    public SimpleWaveformExtended(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        // Initialize the threshold line paint
        thresholdPaint = new Paint();
        thresholdPaint.setStrokeWidth(5);
        thresholdPaint.setColor(0xFF00FF00); // Green color for threshold line
    }

    public void setMaxVal(int max_val) {
        this.maxVal = max_val;
    }

    public void setThreshold(int threshold) {
        mThreshold = threshold;
        invalidate(); // Redraw the view
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Draw threshold line
        int midY = getHeight() / 2;
        lineY = midY - (int) (((float) mThreshold / maxVal) * midY);
        canvas.drawLine(0, lineY, getWidth(), lineY, thresholdPaint);
    }
}