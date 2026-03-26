package com.example.ringtoneplayer.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

public class AudioVisualizerView extends View {
    private Paint mPaint;
    private Path mPath;
    private float mPhase = 0;
    private float mAmplitude = 45; 
    private float mVisualizerMultiplier = 1.0f;
    private final float density;
    private int mStartColor = 0xFF00E5FF;
    private int mEndColor = 0xFF00A3FF;

    public AudioVisualizerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        density = context.getResources().getDisplayMetrics().density;
        init();
    }

    private void init() {
        mPaint = new Paint();
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(3 * density); // 3dp StrokeWidth as requested
        mPaint.setAntiAlias(true);
        mPaint.setStrokeCap(Paint.Cap.ROUND);
        mPath = new Path();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        updateShader();
    }

    private void updateShader() {
        if (getWidth() > 0) {
            Shader shader = new LinearGradient(0, 0, getWidth(), 0,
                    mStartColor, mEndColor, Shader.TileMode.CLAMP);
            mPaint.setShader(shader);
        }
    }

    public void setThemeColors(int startColor, int endColor) {
        this.mStartColor = startColor;
        this.mEndColor = endColor;
        updateShader();
        invalidate();
    }

    public void updateVisualizer(byte[] data) {
        if (data != null && data.length > 0) {
            float sum = 0;
            for (int i = 0; i < data.length; i += 4) {
                sum += Math.abs((data[i] & 0xFF) - 128);
            }
            mVisualizerMultiplier = 0.8f + (sum / (data.length / 4f) / 128f) * 4.0f;
        }
        mPhase += 0.15f; 
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float width = getWidth();
        float height = getHeight();
        float midY = height / 2;

        if (width <= 0 || height <= 0) return;

        // Stacked Sine Waves (4 layers for complexity)
        mPaint.setAlpha(60);
        drawWave(canvas, midY, width, 1.4f * mVisualizerMultiplier, 0.6f, 0.8f);
        
        mPaint.setAlpha(120);
        drawWave(canvas, midY, width, 0.8f * mVisualizerMultiplier, 1.3f, 1.2f);
        
        mPaint.setAlpha(180);
        drawWave(canvas, midY, width, 1.1f * mVisualizerMultiplier, 0.9f, 0.6f);
        
        mPaint.setAlpha(255);
        drawWave(canvas, midY, width, 1.0f * mVisualizerMultiplier, 1.0f, 1.0f);

        if (mVisualizerMultiplier > 1.0f) {
            mVisualizerMultiplier -= 0.04f;
        }
    }

    private void drawWave(Canvas canvas, float midY, float width, float ampMult, float freqMult, float shift) {
        mPath.reset();
        for (float x = 0; x <= width; x += 5) {
            float scaling = (float) (Math.sin(x / width * Math.PI)); 
            float y = midY + (float) (Math.sin(x * 0.018f * freqMult + mPhase * shift) * mAmplitude * ampMult * scaling);
            
            if (x == 0) mPath.moveTo(x, y);
            else mPath.lineTo(x, y);
        }
        canvas.drawPath(mPath, mPaint);
    }
}
