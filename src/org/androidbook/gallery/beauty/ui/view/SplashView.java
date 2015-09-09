package org.androidbook.gallery.beauty.ui.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.util.AttributeSet;
import android.view.View;

public class SplashView extends View {

	private int[] colors = { 0xff84ECB9, 0xff52BBB7, 0xffEB4255, 0xffFAB64B };

	private Paint paint = new Paint();

	public SplashView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		// TODO Auto-generated constructor stub
	}

	public SplashView(Context context, AttributeSet attrs) {
		super(context, attrs);
		// TODO Auto-generated constructor stub
	}

	public SplashView(Context context) {
		super(context);
		// TODO Auto-generated constructor stub
	}

	@Override
	protected void onDraw(Canvas canvas) {
		int width = getWidth();
		int height = getHeight();
		int unit = width / 4;
		paint.setColor(colors[0]);
		paint.setStyle(Style.FILL);
		canvas.drawRect(0, 0, unit, height, paint);
		paint.setColor(colors[1]);
		canvas.drawRect(unit, 0, unit * 2, height, paint);
		paint.setColor(colors[2]);
		canvas.drawRect(unit * 2, 0, unit * 3, height, paint);
		paint.setColor(colors[3]);
		canvas.drawRect(unit * 3, 0, width, height, paint);
	}
}
