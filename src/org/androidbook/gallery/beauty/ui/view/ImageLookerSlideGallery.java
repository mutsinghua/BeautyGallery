package org.androidbook.gallery.beauty.ui.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;

/**
 * 为ImageLooker自定义一些操作的滑动画廊
 * @author blackiedeng
 *
 */
public class ImageLookerSlideGallery extends SlideGallery2 {

    private static boolean mChildRequestTouch;
	/*
     * 构造方法三兄弟
     */
    public ImageLookerSlideGallery(Context context) {
        this(context,null);
    }
    public ImageLookerSlideGallery(Context context, AttributeSet attrs) {
        this(context, attrs,0);
    }
    public ImageLookerSlideGallery(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }
    
    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
    	
    	int action = ev.getAction() & MotionEvent.ACTION_MASK;
    	
        if (isGalleryChildRequestTouch() == true) {
            resetDragStatus();
            return false;
        }
        
        boolean bRet = super.onInterceptTouchEvent(ev);
        
        
        return bRet;
    }

	public static boolean isGalleryChildRequestTouch() {
		return mChildRequestTouch;
	}
	public static void setGalleryChildRequestTouch(boolean mChildRequestTouch) {
		ImageLookerSlideGallery.mChildRequestTouch = mChildRequestTouch;
	}
}
