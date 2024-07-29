package org.osmdroid.views.overlay;

import static org.osmdroid.util.TileSystem.MapSize;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;

import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.Projection;

/**
 * Place an image on the map, each corner (4) of the image being associated with a {@link GeoPoint}
 * or only top-left and bottom-right corners
 *
 * @author Fabrice Fontaine
 * Triggered by issue 1361 (https://github.com/osmdroid/osmdroid/issues/1361)
 * Inspired by {@link GroundOverlay2} and {@link GroundOverlay4}
 * @since 6.1.1
 */
public class GroundOverlay extends Overlay {

    private final Paint mPaint = new Paint();
    private final Matrix mMatrix = new Matrix();

    private float mBearing;
    private float mTransparency;
    private Bitmap mImage;
    private float mScaleImage = Float.NaN; // resolution of the given image in px/m


    private float[] mMatrixSrc;
    private float[] mMatrixDst;

    private GeoPoint mTopLeft;
    private GeoPoint mTopRight;
    private GeoPoint mBottomRight;
    private GeoPoint mBottomLeft;

    private double mAzimuthRad = Float.NaN;

    // limit the biggest dimension of the ground floor to 5000 px
    private final static int maxWidthOrHeight = 5000;
    // resulting downsizing scale based on the given max width or height
    private float scaleDownsizing = 1f;


    public GroundOverlay() {
        super();
        mBearing = 0.0f;
        setTransparency(0.0f);
    }

    public void setImage(final Bitmap pImage) {
        // check if image is too big, set a limit
        float scale = 1f;

        // scale down the image to the given max height and width
        if(pImage.getWidth() > maxWidthOrHeight) {
            scale = (float) pImage.getWidth() / maxWidthOrHeight;

        }
        if (pImage.getHeight()/scale > maxWidthOrHeight) {
            scale = pImage.getHeight() / scale / maxWidthOrHeight;
        }
        mImage = Bitmap.createScaledBitmap(pImage, (int)(pImage.getWidth()/scale), (int)(pImage.getHeight()/scale) , true );

        scaleDownsizing = scale;
        mMatrixSrc = null;
    }

    public Bitmap getImage() {
        return mImage;
    }

    public void setBearing(final float pBearing) {
        mBearing = pBearing;
    }

    public float getBearing() {
        return mBearing;
    }

    public void setTransparency(final float pTransparency) {
        mTransparency = pTransparency;
        mPaint.setAlpha(255 - (int) (mTransparency * 255));
    }

    public float getTransparency() {
        return mTransparency;
    }

    public GeoPoint getTopLeft() {
        return mTopLeft;
    }

    public GeoPoint getTopRight() {
        return mTopRight;
    }

    public GeoPoint getBottomRight() {
        return mBottomRight;
    }

    public GeoPoint getBottomLeft() {
        return mBottomLeft;
    }

    @Override
    public void draw(final Canvas pCanvas, final Projection pProjection) {
        if (mImage == null) {
            return;
        }
        computeMatrix(pProjection);
        pCanvas.drawBitmap(mImage, mMatrix, mPaint);
    }

    public void setPosition(final GeoPoint pTopLeft, final GeoPoint pTopRight,
                            final GeoPoint pBottomRight, final GeoPoint pBottomLeft) {
        mMatrix.reset();
        mTopLeft = new GeoPoint(pTopLeft);
        mTopRight = new GeoPoint(pTopRight);
        mBottomRight = new GeoPoint(pBottomRight);
        mBottomLeft = new GeoPoint(pBottomLeft);
        mBounds = new BoundingBox(pTopLeft.getLatitude(), pTopRight.getLongitude(),
                pBottomRight.getLatitude(), pTopLeft.getLongitude()
        );
    }

    public void setPosition(final GeoPoint pTopLeft, final GeoPoint pBottomRight) {
        mMatrix.reset();
        mMatrixSrc = null;
        mMatrixDst = null;
        mTopLeft = new GeoPoint(pTopLeft);
        mTopRight = null;
        mBottomRight = new GeoPoint(pBottomRight);
        mBottomLeft = null;
        mBounds = new BoundingBox(pTopLeft.getLatitude(), pBottomRight.getLongitude(),
                pBottomRight.getLatitude(), pTopLeft.getLongitude()
        );
    }
    private double radiusEquator = 6378137.0; // in meters
    private double radiusPoles = 6356752.314; // in metes
    /**
     * Set the position of the ground overlay based on an anchor point, a given scaling and rotation
     *
     * @param pBottomLeft
     * @param imageResolution in pixel per meter
     * @param azimuth
     */
    public void setPosition(final GeoPoint pBottomLeft, float imageResolution, float azimuth) {
        // reset everything
        mMatrix.reset();
        mMatrixSrc = null;
        mMatrixDst = null;
        mBottomLeft = new GeoPoint(pBottomLeft);
        mScaleImage = imageResolution/ scaleDownsizing;
        mAzimuthRad = azimuth*Math.PI/180;

        double latRad = mBottomLeft.getLatitude() * Math.PI/180;

        double rEarthLat = Math.sqrt(Math.pow(radiusEquator,2) * Math.pow(Math.cos(latRad),2) +
                Math.pow(radiusPoles,2) * Math.pow(Math.sin(latRad),2));

        float heightMeter = mImage.getHeight()/mScaleImage;
        float widthMeter = mImage.getWidth()/mScaleImage;

        double deltaXHeight = Math.sin(mAzimuthRad) * heightMeter;
        double deltaYHeight = Math.cos(mAzimuthRad) * heightMeter;

        double deltaLonHeight = 360 * deltaXHeight / (2 * Math.PI * rEarthLat * Math.cos(latRad));
        double deltaLatHeight = 360 * deltaYHeight / (2 * Math.PI * rEarthLat);

        double lat = mBottomLeft.getLatitude() + deltaLatHeight;
        double lon = mBottomLeft.getLongitude() + deltaLonHeight;
        mTopLeft = new GeoPoint(lat, lon);

        double deltaXWidth = Math.cos(mAzimuthRad) * widthMeter;
        double deltaYWidth = - Math.sin(mAzimuthRad) * widthMeter;

        double deltaLonWidth = 360 * deltaXWidth / (2 * Math.PI * rEarthLat * Math.cos(latRad));
        double deltaLatWidth = 360 * deltaYWidth / (2 * Math.PI * rEarthLat);

        lat = mBottomLeft.getLatitude() + deltaLatWidth;
        lon = mBottomLeft.getLongitude() + deltaLonWidth;
        mBottomRight = new GeoPoint(lat, lon);

        lat = mBottomRight.getLatitude() + deltaLatHeight;
        lon = mBottomRight.getLongitude() + deltaLonHeight;
        mTopRight = new GeoPoint(lat, lon);

    }


    // TODO check if performance-wise it would make sense to use the mMatrix.setPolyToPoly option
    // TODO even for the 2 corner case
    private void computeMatrix(final Projection pProjection) {
        if (mTopRight == null) { // only 2 corners
            final long x0 = pProjection.getLongPixelXFromLongitude(mTopLeft.getLongitude());
            final long y0 = pProjection.getLongPixelYFromLatitude(mTopLeft.getLatitude());
            final long x1 = pProjection.getLongPixelXFromLongitude(mBottomRight.getLongitude());
            final long y1 = pProjection.getLongPixelYFromLatitude(mBottomRight.getLatitude());
            final float widthOnTheMap = x1 - x0;
            final float heightOnTheMap = y1 - y0;
            final float scaleX = widthOnTheMap / mImage.getWidth();
            final float scaleY = heightOnTheMap / mImage.getHeight();
            mMatrix.setScale(scaleX, scaleY);
            mMatrix.postTranslate(x0, y0);
            return;
        }
        // 4 corners
        if (mMatrixSrc == null) {
            mMatrixSrc = new float[8];
            final int width = mImage.getWidth();
            final int height = mImage.getHeight();
            mMatrixSrc[0] = 0;
            mMatrixSrc[1] = 0;
            mMatrixSrc[2] = width;
            mMatrixSrc[3] = 0;
            mMatrixSrc[4] = width;
            mMatrixSrc[5] = height;
            mMatrixSrc[6] = 0;
            mMatrixSrc[7] = height;
        }
        if (mMatrixDst == null) {
            mMatrixDst = new float[8];
        }
        final long topLeftCornerX = pProjection.getLongPixelXFromLongitude(mTopLeft.getLongitude());
        final long topLeftCornerY = pProjection.getLongPixelYFromLatitude(mTopLeft.getLatitude());
        final long topRightCornerX = pProjection.getLongPixelXFromLongitude(mTopRight.getLongitude());
        final long topRightCornerY = pProjection.getLongPixelYFromLatitude(mTopRight.getLatitude());
        final long bottomRightCornerX = pProjection.getLongPixelXFromLongitude(mBottomRight.getLongitude());
        final long bottomRightCornerY = pProjection.getLongPixelYFromLatitude(mBottomRight.getLatitude());
        final long bottomLeftCornerX = pProjection.getLongPixelXFromLongitude(mBottomLeft.getLongitude());
        final long bottomLeftCornerY = pProjection.getLongPixelYFromLatitude(mBottomLeft.getLatitude());
        mMatrixDst[0] = (float) topLeftCornerX;
        mMatrixDst[1] = (float) topLeftCornerY;
        mMatrixDst[2] = (float) topRightCornerX;
        mMatrixDst[3] = (float) topRightCornerY;
        mMatrixDst[4] = (float) bottomRightCornerX;
        mMatrixDst[5] = (float) bottomRightCornerY;
        mMatrixDst[6] = (float) bottomLeftCornerX;
        mMatrixDst[7] = (float) bottomLeftCornerY;

        mMatrix.setPolyToPoly(mMatrixSrc, 0, mMatrixDst, 0, 4);


    }
}
