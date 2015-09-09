/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.androidbook.utils;

import java.io.Closeable;
import java.util.Random;

import org.androidbook.gallery.beauty.BeautyApplication;
import org.androidbook.gallery.beauty.R;
import org.androidbook.gallery.beauty.ui.BaseActivity;
import org.androidbook.gallery.beauty.ui.view.Rotate3dAnimation;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Handler;
import android.os.ParcelFileDescriptor;
import android.util.DisplayMetrics;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.webkit.MimeTypeMap;


/**
 * Collection of utility functions used in this package.
 */
public class Util {
    private static final String TAG = "db.Util";
    private static final String MAPS_PACKAGE_NAME = "com.google.android.apps.maps";
    private static final String MAPS_CLASS_NAME = "com.google.android.maps.MapsActivity";

    private Util() {
    }

    // Rotates the bitmap by the specified degree.
    // If a new bitmap is created, the original bitmap is recycled.
    public static Bitmap rotate(Bitmap b, int degrees) {
        if (degrees != 0 && b != null) {
            Matrix m = new Matrix();
            m.setRotate(degrees, (float) b.getWidth() / 2, (float) b.getHeight() / 2);
            try {
                Bitmap b2 = Bitmap.createBitmap(b, 0, 0, b.getWidth(), b.getHeight(), m, true);
                if (b != b2) {
                    b.recycle();
                    b = b2;
                }
            } catch (OutOfMemoryError ex) {
                // We have no memory to rotate. Return the original bitmap.
            }
        }
        return b;
    }

    public static Bitmap transform(Matrix scaler, Bitmap source, int targetWidth, int targetHeight, boolean scaleUp) {
        int deltaX = source.getWidth() - targetWidth;
        int deltaY = source.getHeight() - targetHeight;
        if (!scaleUp && (deltaX < 0 || deltaY < 0)) {
            /*
             * In this case the bitmap is smaller, at least in one dimension,
             * than the target. Transform it by placing as much of the image as
             * possible into the target and leaving the top/bottom or left/right
             * (or both) black.
             */
            Bitmap b2 = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(b2);

            int deltaXHalf = Math.max(0, deltaX / 2);
            int deltaYHalf = Math.max(0, deltaY / 2);
            Rect src = new Rect(deltaXHalf, deltaYHalf, deltaXHalf + Math.min(targetWidth, source.getWidth()), deltaYHalf
                    + Math.min(targetHeight, source.getHeight()));
            int dstX = (targetWidth - src.width()) / 2;
            int dstY = (targetHeight - src.height()) / 2;
            Rect dst = new Rect(dstX, dstY, targetWidth - dstX, targetHeight - dstY);
            c.drawBitmap(source, src, dst, null);
            return b2;
        }
        float bitmapWidthF = source.getWidth();
        float bitmapHeightF = source.getHeight();

        float bitmapAspect = bitmapWidthF / bitmapHeightF;
        float viewAspect = (float) targetWidth / targetHeight;

        if (bitmapAspect > viewAspect) {
            float scale = targetHeight / bitmapHeightF;
            if (scale < .9F || scale > 1F) {
                scaler.setScale(scale, scale);
            } else {
                scaler = null;
            }
        } else {
            float scale = targetWidth / bitmapWidthF;
            if (scale < .9F || scale > 1F) {
                scaler.setScale(scale, scale);
            } else {
                scaler = null;
            }
        }

        Bitmap b1;
        if (scaler != null) {
            // this is used for minithumb and crop, so we want to filter here.
            b1 = Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), scaler, true);
        } else {
            b1 = source;
        }

        int dx1 = Math.max(0, b1.getWidth() - targetWidth);
        int dy1 = Math.max(0, b1.getHeight() - targetHeight);

        Bitmap b2 = Bitmap.createBitmap(b1, dx1 / 2, dy1 / 2, targetWidth, targetHeight);

        if (b1 != source) {
            b1.recycle();
        }

        return b2;
    }

    /**
     * Creates a centered bitmap of the desired size. Recycles the input.
     * 
     * @param source
     */
    public static Bitmap extractMiniThumb(Bitmap source, int width, int height) {
        return Util.extractMiniThumb(source, width, height, true);
    }

    public static Bitmap extractMiniThumb(Bitmap source, int width, int height, boolean recycle) {
        if (source == null) {
            return null;
        }

        float scale;
        if (source.getWidth() < source.getHeight()) {
            scale = width / (float) source.getWidth();
        } else {
            scale = height / (float) source.getHeight();
        }
        Matrix matrix = new Matrix();
        matrix.setScale(scale, scale);
        Bitmap miniThumbnail = transform(matrix, source, width, height, false);

        if (recycle && miniThumbnail != source) {
            source.recycle();
        }
        return miniThumbnail;
    }

    public static <T> int indexOf(T[] array, T s) {
        for (int i = 0; i < array.length; i++) {
            if (array[i].equals(s)) {
                return i;
            }
        }
        return -1;
    }

    public static void closeSilently(Closeable c) {
        if (c == null)
            return;
        try {
            c.close();
        } catch (Throwable t) {
            // do nothing
        }
    }

    public static void closeSilently(ParcelFileDescriptor c) {
        if (c == null)
            return;
        try {
            c.close();
        } catch (Throwable t) {
            // do nothing
        }
    }

    public static void Assert(boolean cond) {
        if (!cond) {
            throw new AssertionError();
        }
    }

    public static boolean equals(String a, String b) {
        // return true if both string are null or the content equals
        return a == b || a.equals(b);
    }

    private static class BackgroundJob extends BaseActivity.LifeCycleAdapter implements Runnable {

        private final BaseActivity mActivity;
        private final ProgressDialog mDialog;
        private final Runnable mJob;
        private final Handler mHandler;
        private final Runnable mCleanupRunner = new Runnable() {
            public void run() {
                mActivity.removeLifeCycleListener(BackgroundJob.this);
                if (mDialog.getWindow() != null)
                    mDialog.dismiss();
            }
        };

        public BackgroundJob(BaseActivity activity, Runnable job, ProgressDialog dialog, Handler handler) {
            mActivity = activity;
            mDialog = dialog;
            mJob = job;
            mActivity.addLifeCycleListener(this);
            mHandler = handler;
        }

        public void run() {
            try {
                mJob.run();
            } finally {
                mHandler.post(mCleanupRunner);
            }
        }

        @Override
        public void onActivityDestroyed(BaseActivity activity) {
            // We get here only when the onDestroyed being called before
            // the mCleanupRunner. So, run it now and remove it from the queue
            mCleanupRunner.run();
            mHandler.removeCallbacks(mCleanupRunner);
        }

        @Override
        public void onActivityStopped(BaseActivity activity) {
            mDialog.hide();
        }

        @Override
        public void onActivityStarted(BaseActivity activity) {
            mDialog.show();
        }
    }

    public static void startBackgroundJob(BaseActivity activity, String title, String message, Runnable job, Handler handler) {
        // Make the progress dialog uncancelable, so that we can gurantee
        // the thread will be done before the activity getting destroyed.
        ProgressDialog dialog = ProgressDialog.show(activity, title, message, true, false);
        new Thread(new BackgroundJob(activity, job, dialog, handler)).start();
    }

    // Returns an intent which is used for "set as" menu items.
    public static Intent createSetAsIntent(Uri uri, String mimeType) {
        // Infer MIME type if missing for file URLs.
        if (uri.getScheme().equals("file")) {
            String path = uri.getPath();
            int lastDotIndex = path.lastIndexOf('.');
            if (lastDotIndex != -1) {
                mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(
                        uri.getPath().substring(lastDotIndex + 1).toLowerCase());
            }
        }

        Intent intent = new Intent(Intent.ACTION_ATTACH_DATA);
        intent.setDataAndType(uri, mimeType);
        intent.putExtra("mimeType", mimeType);
        return intent;
    }

    // Opens Maps application for a map with given latlong. There is a bug
    // which crashes the Browser when opening this kind of URL. So, we open
    // it in GMM instead. For those platforms which have no GMM installed,
    // the default Maps application will be chosen.

//    public static void openMaps(Context context, double latitude, double longitude) {
//        try {
//            // Try to open the GMM first
//
//            // We don't use "geo:latitude,longitude" because it only centers
//            // the MapView to the specified location, but we need a marker
//            // for further operations (routing to/from).
//            // The q=(lat, lng) syntax is suggested by geo-team.
//            String url = String.format("http://maps.google.com/maps?f=q&q=(%s,%s)", latitude, longitude);
//            ComponentName compName = new ComponentName(MAPS_PACKAGE_NAME, MAPS_CLASS_NAME);
//            Intent mapsIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url)).setComponent(compName);
//            context.startActivity(mapsIntent);
//        } catch (ActivityNotFoundException e) {
//            // Use the "geo intent" if no GMM is installed
////            Log.e(TAG, "GMM activity not found!", e);
//            String url = String.format("geo:%s,%s", latitude, longitude);
//            Intent mapsIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
//            context.startActivity(mapsIntent);
//        }
//    }
    
    public static int getDpi(Activity context) {
		DisplayMetrics md = new DisplayMetrics();
		(context).getWindowManager().getDefaultDisplay().getMetrics(md);
		return md.densityDpi;
	}
    
    public static int getPxFromDp(int value,Activity context)
    {
    	DisplayMetrics md = new DisplayMetrics();
		(context).getWindowManager().getDefaultDisplay().getMetrics(md);
		
    	return (int) (value * md.density);
		
		
    }
    
    /**
	 * 从90翻转到平面度
	 * @param aniView
	 * @param animationListener
	 * @return
	 */
    public static Animation getTurninAnimation(float center, AnimationListener animationListener) {
		Rotate3dAnimation ra = new Rotate3dAnimation(90, 0, center, center, 200, false);
		ra.setDuration(500);
		ra.setFillAfter(true);
		ra.setInterpolator(new AccelerateInterpolator());
		ra.setAnimationListener(animationListener);
		return ra;
	}
    
    public static Animation getRanAnimation(float center, AnimationListener animationListener)
    {
    	int rand = new Random().nextInt(3);
    	if( rand == 0)
    	{
    		return getTurninAnimation( center,  animationListener);
    	}
    	else if(rand == 1)
    	{
    		return AnimationUtils.loadAnimation(BeautyApplication.instance, android.R.anim.fade_in);
    	}
    	else
    	{
    		return AnimationUtils.loadAnimation(BeautyApplication.instance, R.anim.scale_in);
    	}
    }
}
