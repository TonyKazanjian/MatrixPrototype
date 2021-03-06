package com.tonykazanjian.matrixprototype;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.ImageView;

public class PodViewActivity extends AppCompatActivity {

    // Matrix is used to move image
    private static Matrix sMatrix;
    private static Bitmap sOriginalImage, sScaledImage;

    private int mImageHeight, mImageWidth;

    private ImageView mImageView;

    private GestureDetector mGestureDetector;
    // needed for detecting the inversed rotations
    private boolean[] quadrantTouched;

    private boolean mAllowRotating;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pod_view);

        // load the image only once
        if (sOriginalImage == null) {
//            sOriginalImage = BitmapFactory.decodeResource(getResources(), R.drawable.android);
            sOriginalImage = createCircleBm(BitmapFactory.decodeResource(getResources(), R.drawable.android));
        }

        // initialize the matrix only once
        if (sMatrix == null) {
            sMatrix = new Matrix();
        } else {
            // not needed, you can also post the matrix immediately to restore the old state
            sMatrix.reset();
        }

        mGestureDetector = new GestureDetector(this, new MyGestureDetector());
        // there is no 0th quadrant, to keep it simple the first value gets ignored
        quadrantTouched = new boolean[] { false, false, false, false, false };

        mAllowRotating = true;

        mImageView = (ImageView) findViewById(R.id.image);
        mImageView.setOnTouchListener(new TouchListener());
        mImageView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                // method called more than once, but the values only need to be initialized one time
                if(mImageHeight == 0 || mImageWidth == 0) {
                    mImageHeight = mImageView.getHeight();
                    mImageWidth = mImageView.getWidth();

                    // resize
                    Matrix resize = new Matrix();
                    float sx = Math.min(mImageWidth, mImageHeight) / (float) sOriginalImage.getWidth();
                    float sy = Math.min(mImageWidth, mImageHeight) / (float) sOriginalImage.getHeight();
                    resize.postScale(sx, sy);
                    sScaledImage = Bitmap.createBitmap(sOriginalImage, 0, 0, sOriginalImage.getWidth(), sOriginalImage.getHeight(), resize, false);


                    // translate to the imageview's center
                    float translateX = mImageWidth / 2 - sScaledImage.getWidth() /2;
                    float translateY = mImageHeight /2 - sScaledImage.getHeight() /2;
                    sMatrix.postTranslate(translateX, translateY);
                    mImageView.setImageBitmap(sScaledImage);
                    mImageView.setImageMatrix(sMatrix);
                }


            }
        });

    }

    private Bitmap createCircleBm(Bitmap bitmap) {
        Bitmap output = Bitmap.createBitmap(bitmap.getWidth(),
                bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        final int color = 0xff424242;
        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, bitmap.getWidth(),
                bitmap.getHeight());

        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(color);
        canvas.drawCircle(bitmap.getWidth() / 2,
                bitmap.getHeight() / 2, bitmap.getWidth() / 2, paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, rect, rect, paint);
        return output;
    }

    private class TouchListener implements ImageView.OnTouchListener{

        private double startAngle;

        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            mImageView = (ImageView) view;

            switch (motionEvent.getAction()) {
                case MotionEvent.ACTION_DOWN:

                    // reset the touched quadrants
                    for (int i = 0; i < quadrantTouched.length; i++) {
                        quadrantTouched[i] = false;
                    }

                    mAllowRotating = false;
                    startAngle = getAngle(motionEvent.getX(), motionEvent.getY());
                    break;
                case MotionEvent.ACTION_MOVE:
                    double currentAngle = getAngle(motionEvent.getX(), motionEvent.getY());
                    rotateImage((float)(startAngle - currentAngle));
                    startAngle = currentAngle;
                    break;
                case MotionEvent.ACTION_UP:
                    mAllowRotating = true;

                    break;

            }

            // set the touched quadrant to true
            quadrantTouched[getQuadrant(motionEvent.getX() - (mImageWidth / 2), mImageHeight - motionEvent.getY() -
                    (mImageHeight / 2))] = true;

            mGestureDetector.onTouchEvent(motionEvent);

            return true;
        }
    }

    /**
     * @return the angle of the unit circle with the iamgeview's center
     */
    private double getAngle(double xTouch, double yTouch) {
        double x = xTouch - (mImageWidth/2d);
        double y = mImageHeight - yTouch - (mImageHeight/2d);

        switch (getQuadrant(x, y)){
            //Math.asin() returns the arc sine of an angle
            case 1:
                return Math.asin(y/ Math.hypot(x, y)) * 180/ Math.PI;
            case 2:
                return 180 - Math.asin(y/ Math.hypot(x, y)) * 180 / Math.PI;
            case 3:
                return 180 + (-1 * Math.asin(y / Math.hypot(x, y)) * 180 / Math.PI);
            case 4:
                return 360 + Math.asin(y / Math.hypot(x, y)) * 180 / Math.PI;
            default:
                return 0;
        }
    }

    /**
     * @return The selected quadrant.
     */
    private static int getQuadrant(double x, double y) {
        if (x >= 0) {
            return y >= 0 ? 1 : 4;
        } else {
            return y >= 0 ? 2 : 3;
        }
    }

    /**
     * Rotate the image
     *
     * @param degrees The degrees the image should be rotated
     */
    private void rotateImage(float degrees){
        sMatrix.postRotate(degrees, mImageWidth / 2, mImageHeight / 2);
        mImageView.setImageMatrix(sMatrix);
    }

    private class MyGestureDetector extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {

            // get the quadrant of the start and the end of the fling
            int q1 = getQuadrant(e1.getX() - (mImageWidth / 2), mImageHeight - e1.getY() -
                    (mImageHeight / 2));
            int q2 = getQuadrant(e2.getX() - (mImageWidth / 2), mImageHeight - e2.getY() -
                    (mImageHeight / 2));

            // the inversed rotations
            if ((q1 == 2 && q2 == 2 && Math.abs(velocityX) < Math.abs(velocityY))
                    || (q1 == 3 && q2 == 3)
                    || (q1 == 1 && q2 == 3)
                    || (q1 == 4 && q2 == 4 && Math.abs(velocityX) > Math.abs(velocityY))
                    || ((q1 == 2 && q2 == 3) || (q1 == 3 && q2 == 2))
                    || ((q1 == 3 && q2 == 4) || (q1 == 4 && q2 == 3))
                    || (q1 == 2 && q2 == 4 && quadrantTouched[3])
                    || (q1 == 4 && q2 == 2 && quadrantTouched[3])) {

                mImageView.post(new FlingRunnable(-1 * (velocityX + velocityY)));
            } else {
                // the normal rotation
                mImageView.post(new FlingRunnable(velocityX + velocityY));
            }
            return true;
        }
    }
    /**
     * A {@link Runnable} for animating the the dialer's fling. Manipulates GUI on main thread
     */
    private class FlingRunnable implements Runnable {

        private float velocity;

        public FlingRunnable(float velocity) {
            this.velocity = velocity;
        }

        @Override
        public void run() {
            if (Math.abs(velocity) > 5 && mAllowRotating) {
                rotateImage(velocity / 75);
                velocity /= 1.0666F;

                // post this instance again
                mImageView.post(this);
            }
        }
    }

}
