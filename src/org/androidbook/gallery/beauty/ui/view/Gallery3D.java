package org.androidbook.gallery.beauty.ui.view;

import org.androidbook.utils.Util;

import android.app.Activity;
import android.content.Context;
import android.graphics.Camera;
import android.graphics.Matrix;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.animation.Transformation;
import android.widget.Gallery;
import android.widget.ImageView;

/**
 * 用于截图浏览
 * @author rexzou
 *
 */

public class Gallery3D extends Gallery {
    private Camera mCamera = new Camera();//相机类
    private float maxScaleValue = 0.6f;//最大缩放因子
    private int maxSpace = 150; //最大变换距离
    
    private int mCoveflowCenter;//半径值
	public final static int HG_W = 120;
	public final static int HG_H = 160;
    public Gallery3D(Context context) {
        super(context);
       
        //支持转换 ,执行getChildStaticTransformation方法
        this.setStaticTransformationsEnabled(true);
    }
    public Gallery3D(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.setStaticTransformationsEnabled(true);
    }
    public Gallery3D(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.setStaticTransformationsEnabled(true);
    }
    public float getMaxScaleValue() {
        return maxScaleValue;
    }
    public void setMaxScaleValue(float maxScaleValue) {
    	this.maxScaleValue = maxScaleValue;
    }

    private int getCenterOfCoverflow() {
        return (getWidth() - getPaddingLeft() - getPaddingRight()) / 2
                        + getPaddingLeft();
    }
    private static int getCenterOfView(View view) {
        return view.getLeft() + view.getWidth() / 2;
    }
    
    
   //控制gallery中每个图片的旋转(重写的gallery中方法)
    protected boolean getChildStaticTransformation(View child, Transformation t) {  
        //取得当前子view的半径值
        final int childCenter = getCenterOfView(child);
        final int childWidth = child.getWidth();
        //到中间的距离
        int spaceToCenter = 0;
        //重置转换状态
        t.clear();
        //设置转换类型
        t.setTransformationType(Transformation.TYPE_MATRIX);

        spaceToCenter = (int) (mCoveflowCenter - childCenter);
        spaceToCenter = Math.abs(spaceToCenter);
            
        transformImageBitmap((ImageView) child, t, spaceToCenter);

        return true;
    }
    
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        mCoveflowCenter = getCenterOfCoverflow();
        super.onSizeChanged(w, h, oldw, oldh);
    }
    
    /**
     * 变形
     * @param child
     * @param t
     * @param spaceToCenter
     */
    private void transformImageBitmap(ImageView child, Transformation t,
                    int spaceToCenter) {
        //对效果进行保存
        mCamera.save();
        final Matrix imageMatrix = t.getMatrix();
        
    	// 默认图片大小
		int picWidth = HG_W;
		int picHeight =HG_H;
		DisplayMetrics dm= new DisplayMetrics();
		
		
		switch (Util.getDpi((Activity) getContext())) {
		case DisplayMetrics.DENSITY_LOW:
			picWidth = (int) (picWidth * 0.75);
			picHeight = (int) (picHeight * 0.75);
			break;

		case DisplayMetrics.DENSITY_HIGH:
			picWidth = (int) (picWidth * 1.5);
			picHeight = (int) (picHeight * 1.5);
			break;
		}
		

             
        mCamera.getMatrix(imageMatrix);
      
        float scale = getScaleValeFormDeltaXBySquare(spaceToCenter);
        
        //变换
        imageMatrix.postScale(scale, scale, picWidth/2,picHeight/2);


        mCamera.restore();
    }
    
    /**
     * 线性关系
     * y = kx + b
     * 
     * @param spaceToCenter
     * @return
     */
    private float getScaleValeFormDeltaX(int spaceToCenter)
    {
    	if( Math.abs(spaceToCenter) <= maxSpace )
    	{
    		return  ((maxScaleValue -1)/maxSpace)  * Math.abs(spaceToCenter) + 1;
    	}
    	else
    	{
    		return maxScaleValue;
    	}
    }
    
    /**
     * 二次关系
     * y = k*x^2 +b
     * @param spaceToCenter
     * @return
     */
    private float getScaleValeFormDeltaXBySquare(int spaceToCenter)
    {
    	if( Math.abs(spaceToCenter) <= maxSpace )
    	{
    		return ((maxScaleValue - 1)/(maxSpace * maxSpace)) * (spaceToCenter) * (spaceToCenter)+1;
    	}
    	else
    	{
    		return maxScaleValue;
    	}
    }
    
    
}