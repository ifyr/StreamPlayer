package pw.ifyr.streamplayer;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;

import java.util.List;

public class CustomDrawable extends Drawable {
    private final ComboBar mySlider;
    private final Drawable myBase;
    private float mThumbRadius;
    /**
     * paints.
     */
    private final Paint linePaint;
    private List<ComboBar.Dot> mDots;
    private Paint dotPaint;
    private Paint selectedDotPaint;
    private float mDotRadius;
    private Paint textPaint;
    private Paint selectedTextPaint;
    private int mTextSize;
    private float mTextMargin;
    private int mTextHeight;

    public CustomDrawable(Drawable base, ComboBar slider, float thumbRadius, List<ComboBar.Dot> dots, int color, int highlight, int textSize) {
        mySlider = slider;
        myBase = base;
        mDots = dots;
        mTextSize = textSize;

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(color);
        textPaint.setAlpha(255);

        selectedTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        selectedTextPaint.setTypeface(Typeface.DEFAULT_BOLD);
        selectedTextPaint.setColor(highlight);
        selectedTextPaint.setAlpha(255);

        mThumbRadius = thumbRadius;

        linePaint = new Paint();
        linePaint.setColor(color);
        linePaint.setStrokeWidth(toPix(1));

        dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        dotPaint.setColor(color);

        selectedDotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        selectedDotPaint.setColor(highlight);

        Rect textBounds = new Rect();
        selectedTextPaint.setTextSize(mTextSize * 2);
        selectedTextPaint.getTextBounds("M", 0, 1, textBounds);

        textPaint.setTextSize(mTextSize);
        selectedTextPaint.setTextSize(mTextSize);

        mTextHeight = textBounds.height();
        mDotRadius = toPix(5);
        mTextMargin = toPix(3);
    }

    private float toPix(int size) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, size, mySlider.getContext().getResources().getDisplayMetrics());
    }

    @Override
    protected final void onBoundsChange(Rect bounds) {
        myBase.setBounds(bounds);
    }

    @Override
    protected final boolean onStateChange(int[] state) {
        invalidateSelf();
        return false;
    }

    @Override
    public final boolean isStateful() {
        return true;
    }

    @Override
    public final void draw(Canvas canvas) {
        // Log.d("--- draw:" + (getBounds().right - getBounds().left));
        int height = this.getIntrinsicHeight() / 2;
        if (mDots.size() == 0) {
            canvas.drawLine(0, height, getBounds().right, height, linePaint);
            return;
        }
        for (ComboBar.Dot dot : mDots) {
            drawText(canvas, dot, dot.mX, height);
            if (dot.isSelected) {
                canvas.drawCircle(dot.mX, height, mDotRadius, selectedDotPaint);
            } else {
                canvas.drawCircle(dot.mX, height, mDotRadius, dotPaint);
            }
        }
        canvas.drawLine(mDots.get(0).mX, height, mDots.get(mDots.size() - 1).mX, height, linePaint);
    }

    /**
     * @param canvas
     *            canvas.
     * @param dot
     *            current dot.
     * @param x
     *            x cor.
     * @param y
     *            y cor.
     */
    private void drawText(Canvas canvas, ComboBar.Dot dot, float x, float y) {
        final Rect textBounds = new Rect();
        selectedTextPaint.getTextBounds(dot.text, 0, dot.text.length(), textBounds);
        float xres;
        if (dot.id == (mDots.size() - 1)) {
            xres = getBounds().width() - textBounds.width();
        } else if (dot.id == 0) {
            xres = 0;
        } else {
            xres = x - (textBounds.width() / 2);
        }

        float yres = y - (mDotRadius * 2);

        if (dot.isSelected) {
            canvas.drawText(dot.text, xres, yres, selectedTextPaint);
        } else {
            canvas.drawText(dot.text, xres, yres, textPaint);
        }
    }

    @Override
    public final int getIntrinsicHeight() {
        return (int) (mThumbRadius + mTextMargin + mTextHeight + mDotRadius);
    }

    @Override
    public final int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }

    @Override
    public void setAlpha(int alpha) {
    }

    @Override
    public void setColorFilter(ColorFilter cf) {
    }
}
