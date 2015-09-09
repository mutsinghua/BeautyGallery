package org.androidbook.gallery.beauty.ui.view;

import android.view.View;

/**
 * 画廊的监听器,各种回调
 * @author blackiedeng
 *
 */
public interface GalleryListener {
    
    /**
     * 当Item被选中时回调
     * @param parentView 父控件
     * @param selectedView 被选中的控件
     * @param position 被选中的位置
     */
    public void onItemSelected(View parentView,View selectedView,int position);
    
    /**
     * 当超出边界滑动时回调
     * @param isFrontOverScrolled 
     *              若是在最前面超出滑动此值为true,若是在最后面超出滑动此值为false
     */
    public void onOverScrolled(boolean isFrontOverScrolled);
}
