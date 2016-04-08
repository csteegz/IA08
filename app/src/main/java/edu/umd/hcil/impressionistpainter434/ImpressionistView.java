package edu.umd.hcil.impressionistpainter434;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.v4.view.VelocityTrackerCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.widget.ImageView;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Random;

import static edu.umd.hcil.impressionistpainter434.BrushType.*;

/**
 * Created by jon on 3/20/2016.
 */
public class ImpressionistView extends View {

    private ImageView _imageView;

    private Canvas _offScreenCanvas = null;
    private Bitmap _offScreenBitmap = null;
    private Paint _paint = new Paint();

    private int _alpha = 150;
    private int _defaultRadius = 25;
    private Point _lastPoint = null;
    private long _lastPointTime = -1;
    private boolean _useMotionSpeedForBrushStrokeSize = true;
    private Paint _paintBorder = new Paint();
    private BrushType _brushType = Square;
    private float _minBrushRadius = 5;
    private VelocityTracker v;

    public ImpressionistView(Context context) {
        super(context);
        init(null, 0);
    }

    public ImpressionistView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0);
    }

    public ImpressionistView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs, defStyle);
    }

    /**
     * Because we have more than one constructor (i.e., overloaded constructors), we use
     * a separate initialization method
     * @param attrs
     * @param defStyle
     */
    private void init(AttributeSet attrs, int defStyle){

        // Set setDrawingCacheEnabled to true to support generating a bitmap copy of the view (for saving)
        // See: http://developer.android.com/reference/android/view/View.html#setDrawingCacheEnabled(boolean)
        //      http://developer.android.com/reference/android/view/View.html#getDrawingCache()
        this.setDrawingCacheEnabled(true);

        _paint.setColor(Color.RED);
        _paint.setAlpha(_alpha);
        _paint.setAntiAlias(true);
        _paint.setStyle(Paint.Style.FILL);
        _paint.setStrokeWidth(4);

        _paintBorder.setColor(Color.BLACK);
        _paintBorder.setStrokeWidth(3);
        _paintBorder.setStyle(Paint.Style.STROKE);
        _paintBorder.setAlpha(50);

        //_paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.MULTIPLY));
    }

    @Override
    protected void onSizeChanged (int w, int h, int oldw, int oldh){

        Bitmap bitmap = getDrawingCache();
        Log.v("onSizeChanged", MessageFormat.format("bitmap={0}, w={1}, h={2}, oldw={3}, oldh={4}", bitmap, w, h, oldw, oldh));
        if(bitmap != null) {
            _offScreenBitmap = getDrawingCache().copy(Bitmap.Config.ARGB_8888, true);
            _offScreenCanvas = new Canvas(_offScreenBitmap);
        }
    }

    /**
     * Sets the ImageView, which hosts the image that we will paint in this view
     * @param imageView
     */
    public void setImageView(ImageView imageView){
        _imageView = imageView;
    }

    /**
     * Sets the brush type. Feel free to make your own and completely change my BrushType enum
     * @param brushType
     */
    public void setBrushType(BrushType brushType){
        _brushType = brushType;
    }

    /**
     * Clears the painting
     */
    public void clearPainting(){
        _offScreenCanvas.drawColor(Color.WHITE);
        invalidate();
    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if(_offScreenBitmap != null) {
            canvas.drawBitmap(_offScreenBitmap, 0, 0, _paint);
        }

        // Draw the border. Helpful to see the size of the bitmap in the ImageView
        canvas.drawRect(getBitmapPositionInsideImageView(_imageView), _paintBorder);
    }

    @Override
    public boolean onTouchEvent(MotionEvent motionEvent){
        float brushRadius = 15;
        float curTouchX = motionEvent.getX() ;
        float curTouchY = motionEvent.getY() ;
        switch(motionEvent.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (v == null) {
                    v = VelocityTracker.obtain();
                } else {
                    v.clear();
                }
                v.addMovement(motionEvent);
                break;
            case MotionEvent.ACTION_MOVE:
                v.addMovement(motionEvent);
                int historySize = motionEvent.getHistorySize();
                v.computeCurrentVelocity(1000);
                double xV = VelocityTrackerCompat.getXVelocity(v, motionEvent.getPointerId(motionEvent.getActionIndex()));
                double yV = VelocityTrackerCompat.getYVelocity(v, motionEvent.getPointerId(motionEvent.getActionIndex()));
                double velocity = Math.sqrt(Math.pow(xV, 2) + Math.pow(yV, 2)) / 1000;
                brushRadius = (float) velocity * brushRadius;
                for (int i = 0; i < historySize; i++) {
                    float touchX = motionEvent.getHistoricalX(i);
                    float touchY = motionEvent.getHistoricalY(i);
                    _paint.setColor(getColor(touchX,touchY));
                    switch (_brushType) {
                        case Circle:
                            _offScreenCanvas.drawCircle(touchX, touchY, brushRadius, _paint);
                            break;
                        case Square:
                            _offScreenCanvas.drawRect(touchX, touchY, touchX + brushRadius, touchY + brushRadius, _paint);
                            break;
                        case CircleSplatter:
                            Random r = new Random();
                            int num = r.nextInt(5) + 2;
                            for (int j = 0; j < num; j++){
                                float dx = (((float).5)-r.nextFloat()) * brushRadius;
                                float dy = (((float).5)-r.nextFloat()) * brushRadius;
                                float randRadius = r.nextFloat() * brushRadius/2;
                                _paint.setColor(getColor(touchX+dx,touchY+dy));
                                _offScreenCanvas.drawCircle(touchX+dx,touchY+dy,randRadius,_paint);
                            }
                            break;

                    }
                }
                _paint.setColor(getColor(curTouchX,curTouchY));
                switch (_brushType) {
                    case Circle:
                        _offScreenCanvas.drawCircle(curTouchX, curTouchY, brushRadius, _paint);
                        break;
                    case Square:
                        _offScreenCanvas.drawRect(curTouchX, curTouchY, curTouchX + brushRadius, curTouchY + brushRadius, _paint);
                        break;
                    case CircleSplatter:
                        Random r = new Random();
                        int num = r.nextInt(5) + 2;
                        for (int j = 0; j < num; j++){
                            float dx = (((float).5)-r.nextFloat()) * brushRadius;
                            float dy = (((float).5)-r.nextFloat()) * brushRadius;
                            float randRadius = r.nextFloat() * brushRadius/2;
                            _paint.setColor(getColor(curTouchX+dx,curTouchY+dy));
                            _offScreenCanvas.drawCircle(curTouchX+dx,curTouchY+dy,randRadius,_paint);
                        }
                        break;
                }
                invalidate();
                break;
            case MotionEvent.ACTION_UP:
                break;
        }
        return true;
    }

    private int getColor(float touchX, float touchY) {
        ImageView imageView = _imageView;
        Bitmap bitmap = ((BitmapDrawable)imageView.getDrawable()).getBitmap();
        if (bitmap == null){
            return Color.WHITE;
        }
        Rect r = getBitmapPositionInsideImageView(imageView);
        touchX = touchX/r.width();
        touchY = touchY/r.height();
        int x = Math.round(touchX * bitmap.getWidth());
        int y = Math.round(touchY * bitmap.getHeight());
        int h = bitmap.getHeight();
        int w = bitmap.getWidth();
        if (x >= w || x <= 0 || y <= 0 || y >= h) {
            return Color.WHITE;
        }
        int pixel = bitmap.getPixel(x,y);
        return pixel;
    }

    public void sortImage(){
        //IDea borrowed from http://codegolf.stackexchange.com/questions/62686/sort-the-pixels
        int width = _offScreenBitmap.getWidth();
        int height = _offScreenBitmap.getHeight();
        int pixels[] = new int[width*height];
        _offScreenBitmap.getPixels(pixels,0,width,0,0,width,height);
        Arrays.sort(pixels);
        _offScreenBitmap.setPixels(pixels,0,width,0,0,width,height);
        invalidate();
    }


    /**
     * This method is useful to determine the bitmap position within the Image View. It's not needed for anything else
     * Modified from:
     *  - http://stackoverflow.com/a/15538856
     *  - http://stackoverflow.com/a/26930938
     * @param imageView
     * @return
     */
    private static Rect getBitmapPositionInsideImageView(ImageView imageView){
        Rect rect = new Rect();

        if (imageView == null || imageView.getDrawable() == null) {
            return rect;
        }

        // Get image dimensions
        // Get image matrix values and place them in an array
        float[] f = new float[9];
        imageView.getImageMatrix().getValues(f);

        // Extract the scale values using the constants (if aspect ratio maintained, scaleX == scaleY)
        final float scaleX = f[Matrix.MSCALE_X];
        final float scaleY = f[Matrix.MSCALE_Y];

        // Get the drawable (could also get the bitmap behind the drawable and getWidth/getHeight)
        final Drawable d = imageView.getDrawable();
        final int origW = d.getIntrinsicWidth();
        final int origH = d.getIntrinsicHeight();

        // Calculate the actual dimensions
        final int widthActual = Math.round(origW * scaleX);
        final int heightActual = Math.round(origH * scaleY);

        // Get image position
        // We assume that the image is centered into ImageView
        int imgViewW = imageView.getWidth();
        int imgViewH = imageView.getHeight();

        int top = (int) (imgViewH - heightActual)/2;
        int left = (int) (imgViewW - widthActual)/2;

        rect.set(left, top, left + widthActual, top + heightActual);

        return rect;
    }
}

