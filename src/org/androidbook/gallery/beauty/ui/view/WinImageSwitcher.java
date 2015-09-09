package org.androidbook.gallery.beauty.ui.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.widget.ViewSwitcher;

public class WinImageSwitcher extends ViewSwitcher
{
	public interface OnImageDrawableChangeListener
	{
		public void onDrawableChange(ImageView imageView);
		
	}
	
	private OnImageDrawableChangeListener drawableChangleListener;
	
    public WinImageSwitcher(Context context)
    {
        super(context);
    }
    
    public WinImageSwitcher(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setImageResource(int resid)
    {
        ImageView image = (ImageView)this.getNextView();
        image.setImageResource(resid);
        if( drawableChangleListener != null){
        	drawableChangleListener.onDrawableChange(image);
        }
        showNext();
    }

    public void setImageURI(Uri uri)
    {
        ImageView image = (ImageView)this.getNextView();
        image.setImageURI(uri);
        if( drawableChangleListener != null){
        	drawableChangleListener.onDrawableChange(image);
        }
        showNext();
    }

    public void setImageDrawable(Drawable drawable)
    {
        ImageView image = (ImageView)this.getNextView();
        image.setImageDrawable(drawable);
        if( drawableChangleListener != null){
        	drawableChangleListener.onDrawableChange(image);
        }
        showNext();
    }

    public void setImageBitmap(Bitmap bitmap)
    {
        ImageView image = (ImageView)this.getNextView();
        image.setImageBitmap(bitmap);
        if( drawableChangleListener != null){
        	drawableChangleListener.onDrawableChange(image);
        }
        showNext();
    }
    
    public OnImageDrawableChangeListener getDrawableChangleListener()
	{
		return drawableChangleListener;
	}

    public void setOnDrawableChangleListener(OnImageDrawableChangeListener drawableChangleListener)
	{
		this.drawableChangleListener = drawableChangleListener;
	}
}