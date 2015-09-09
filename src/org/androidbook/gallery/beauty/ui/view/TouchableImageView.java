package org.androidbook.gallery.beauty.ui.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.FloatMath;
import android.view.MotionEvent;
import android.view.WindowManager;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;

/**
 * 用于查看图片的ImageView
 * 
 * @author vokenfan
 * 
 */
public class TouchableImageView extends ImageView {

	private static final String TAG = "TouchableImageView";

	private static final int TOUCH_MODE_NONE = 0;
	private static final int TOUCH_MODE_DRAG = 1;
	private static final int TOUCH_MODE_ZOOM = 2;

	private static final int PIC_OUTOF_BOUND_X_Y = 0;
	private static final int PIC_OUTOF_BOUND_Y = 1;
	private static final int PIC_OUTOF_BOUND_X = 2;
	private static final int PIC_IN_BOUND_X_Y = 3;

	private static final int PIC_DEFAULT = 0;
	private static final int PIC_LEFT = 1;
	private static final int PIC_RIGHT = 2;
	private static final int PIC_TOP = 3;
	private static final int PIC_BOTTOM = 4;
	private static final int PIC_LEFT_TOP = 5;
	private static final int PIC_LEFT_BOTTOM = 6;
	private static final int PIC_RIGHT_TOP = 7;
	private static final int PIC_RIGHT_BOTTOM = 8;

	private static final int MSG_ON_CLICK = 1;
	private static final int MSG_ON_DOUBLECLICK = 2;
	private static final int MSG_ON_CLICK_DELAY = 250;

	private static final float DOUBLE_SCALE = 2.0f;

	private static final float SCALEZ_MAX = 10.0f;// 原来是20.0f

	private Bitmap mBitmap;
	private Drawable mDrawable;

	private int mTouchMode;
	private boolean mMoved;

	private OnClickListener mOnClickListener;

	private int mWLimit;
	private int mHLimit;
	private int mPaddingX;
	private int mPaddingY;

	private Matrix mCurrentMatrix;
	private Matrix mSavedMatrix;
	private Matrix mSavedMatrixRotate;

	private PointF mStartPoint;
	private PointF mMidPoint;

	private float mOldDist;

	private RectF mWindowRect;
	private RectF mImageRect;

	private float mMinScale;
	private float mMaxScale;

	private int mBitmapWidth;
	private int mBitmapHeight;

	private boolean mRevertXY;
	private int mRotatedDegree;
	private Handler mHandler;
	private boolean isDoubleZoomed;
	private boolean isBound;
	private Context mContext;

	public TouchableImageView(Context context) {
		super(context);
		setScaleType(ScaleType.MATRIX);
		mContext = context;
		init(context);
	}

	public TouchableImageView(Context context, AttributeSet as) {
		super(context, as);
		setScaleType(ScaleType.CENTER);
		init(context);
	}

	private void init(Context context) {

		mTouchMode = TOUCH_MODE_NONE;

		mCurrentMatrix = new Matrix();
		mSavedMatrix = new Matrix();
		mSavedMatrixRotate = new Matrix();

		mStartPoint = new PointF();
		mMidPoint = new PointF();

		mImageRect = new RectF();

		mMinScale = 1.0f;
		mMaxScale = SCALEZ_MAX;
		mRotatedDegree = 0;

		mMoved = false;
		mRevertXY = false;
		isDoubleZoomed = false;
		isBound = false;

		mWLimit = 0;
		mHLimit = 0;
		mPaddingX = 0;
		mPaddingY = 0;

		mHandler = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				if (msg.what == MSG_ON_CLICK && mOnClickListener != null) {
					mOnClickListener.onClick(TouchableImageView.this);
				} else if (msg.what == MSG_ON_DOUBLECLICK) {
					PointF point = (PointF) msg.obj;
					float[] values = new float[9];
					mCurrentMatrix.getValues(values);
					float scaleX = getScaleX(values);
					onDoubleTap(scaleX, point);
				}
			}
		};
	}

	public void onDoubleTap(PointF point) {
		if (mDrawable != null) {
			return;
		}

		float[] values = new float[9];
		mCurrentMatrix.getValues(values);
		float scaleX = getScaleX(values);
		onDoubleTap(scaleX, point);
	}

	private void onDoubleTap(float fromScale, PointF point) {
		if (mDrawable != null || point == null) {
			return;
		}

		final float x = point.x;
		final float y = point.y;
		final float[] doubleTapImagePoint = new float[2];
		Matrix inverseMatrix = new Matrix();
		mCurrentMatrix.reset();
		mCurrentMatrix.set(getImageMatrix());
		mCurrentMatrix.invert(inverseMatrix);
		doubleTapImagePoint[0] = x;
		doubleTapImagePoint[1] = y;
		inverseMatrix.mapPoints(doubleTapImagePoint);
		final float scale = fromScale;
		final float targetScale = scale > mMinScale ? mMinScale : mMinScale * DOUBLE_SCALE;
		final float finalX;
		final float finalY;
		if (targetScale != mMinScale) {
			isDoubleZoomed = true;
		}

		// assumption: if targetScale is less than 1, we're zooming out to fit
		// the screen
		if (targetScale <= 1.0f) {
			// scaling the image to fit the screen, we want the resulting image
			// to be centred. We need to take
			// into account the shift that is applied to zoom on the tapped
			// point, easiest way is to reuse
			// the transformation matrix.
			RectF imageBounds = new RectF(getDrawable().getBounds());
			// set up matrix for target
			mCurrentMatrix.reset();
			mCurrentMatrix.postTranslate(-doubleTapImagePoint[0], -doubleTapImagePoint[1]);
			mCurrentMatrix.postScale(targetScale, targetScale);
			mCurrentMatrix.mapRect(imageBounds);
			finalX = ((getWidth() - imageBounds.width()) / 2.0f) - imageBounds.left;
			finalY = ((getHeight() - imageBounds.height()) / 2.0f) - imageBounds.top;
		} else { // else zoom around the double-tap point
			finalX = x;
			finalY = y;
		}

		final Interpolator interpolator = new LinearInterpolator();
		final long startTime = System.currentTimeMillis();
		final long duration = 400;

		post(new Runnable() {
			@Override
			public void run() {
				float t = (float) (System.currentTimeMillis() - startTime) / duration;
				t = t > 1.0f ? 1.0f : t;
				float interpolatedRatio = interpolator.getInterpolation(t);
				float tempScale = scale + interpolatedRatio * (targetScale - scale);
				float tempX = x + interpolatedRatio * (finalX - x);
				float tempY = y + interpolatedRatio * (finalY - y);
				mCurrentMatrix.reset();
				// translate initialPoint to 0,0 before applying zoom
				mCurrentMatrix.postTranslate(-doubleTapImagePoint[0], -doubleTapImagePoint[1]);
				// zoom
				mCurrentMatrix.postScale(tempScale, tempScale);
				// translate back to equivalent point
				mCurrentMatrix.postTranslate(tempX, tempY);
				center(true, true);
				// 如果图片有旋转的话，需要先旋转
				if (mRotatedDegree > 0) {
					mCurrentMatrix.postRotate(mRotatedDegree, mWindowRect.centerX(), mWindowRect.centerY());
				}
				setImageMatrix(mCurrentMatrix);
				float values[] = new float[9];
				mCurrentMatrix.getValues(values);
				if (t < 1f) {
					post(this);
				}
			}
		});

	}

	// Center as much as possible in one or both axis. Centering is
	// defined as follows: if the image is scaled down below the
	// view's dimensions then center it (literally). If the image
	// is scaled larger than the view and is translated out of view
	// then translate it back into view (i.e. eliminate black bars).
	protected void center(boolean horizontal, boolean vertical) {
		if (mBitmap == null) {
			return;
		}

		RectF rect = new RectF(0, 0, mBitmap.getWidth(), mBitmap.getHeight());

		mCurrentMatrix.mapRect(rect);

		float height = rect.height();
		float width = rect.width();

		float deltaX = 0, deltaY = 0;

		if (vertical) {
			int viewHeight = getHeight();
			if (height < viewHeight) {
				deltaY = (viewHeight - height) / 2 - rect.top;
			} else if (rect.top > 0) {
				deltaY = -rect.top;
			} else if (rect.bottom < viewHeight) {
				deltaY = getHeight() - rect.bottom;
			}
		}

		if (horizontal) {
			int viewWidth = getWidth();
			if (width < viewWidth) {
				deltaX = (viewWidth - width) / 2 - rect.left;
			} else if (rect.left > 0) {
				deltaX = -rect.left;
			} else if (rect.right < viewWidth) {
				deltaX = viewWidth - rect.right;
			}
		}
		mCurrentMatrix.postTranslate(deltaX, deltaY);
	}

	private RectF getWindowRect() {
		if (mWindowRect == null) {
			mWindowRect = new RectF(mPaddingX, mPaddingY, mWLimit - mPaddingX, mHLimit - mPaddingY);
		}
		return mWindowRect;
	}

	private void resetImageRect() {
		mImageRect.left = 0;
		mImageRect.top = 0;
		mImageRect.right = mBitmapWidth;
		mImageRect.bottom = mBitmapHeight;
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
		getWindowRect();
	}

	@Override
	protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
		super.onLayout(changed, left, top, right, bottom);
		if (mHLimit == 0 || mWLimit == 0 || changed) {
			mHLimit = bottom - top;
			mWLimit = right - left;
			setVisableRectLimit(mPaddingX, mPaddingY);

			if (mBitmap != null) {
				resetImageBitmap(mBitmap);
			} else if (mDrawable != null) {
				resetImageDrawable(mDrawable);
			}
		}
	}

	private void setMinScaleForRotata() {
		if (mBitmap == null) {
			return;
		}

		mMinScale = 1.0f;
		mMaxScale = SCALEZ_MAX * mMinScale;

		int screenWidth = getHeight();
		int screenHeight = getWidth();

		// if (mBitmapWidth < screenWidth && mBitmapHeight < screenHeight) {
		// //若是图片本身就比较小,就不要缩放啦
		// return;
		// }

		float widthScale = (float) screenWidth / (float) mBitmapWidth;
		float heightScale = (float) screenHeight / (float) mBitmapHeight;
		if (screenWidth > screenHeight) {// 横屏
			if (widthScale > heightScale) {
				mMinScale = heightScale;
				mMaxScale = SCALEZ_MAX * mMinScale;// 20
				return;
			} else {
				mMinScale = Math.min(widthScale, heightScale);
				mMaxScale = SCALEZ_MAX * mMinScale;
				return;
			}
		} else {// 竖屏
			if (widthScale < heightScale) {
				mMinScale = widthScale;
				mMaxScale = SCALEZ_MAX * mMinScale;
				return;
			} else {
				mMinScale = Math.min(widthScale, heightScale);
				mMaxScale = SCALEZ_MAX * mMinScale;
				return;
			}
		}
	}

	private void setMinScale() {
		if (mBitmap == null) {
			return;
		}

		mMinScale = 1.0f;
		mMaxScale = SCALEZ_MAX * mMinScale;

		int screenWidth = getWidth();
		int screenHeight = getHeight();

		// if (mBitmapWidth < screenWidth && mBitmapHeight < screenHeight) {
		// //若是图片本身就比较小,就不要缩放啦
		// return;
		// }

		float widthScale = (float) screenWidth / (float) mBitmapWidth;
		float heightScale = (float) screenHeight / (float) mBitmapHeight;
		if (screenWidth > screenHeight) {// 横屏
			if (widthScale > heightScale) {
				mMinScale = heightScale;
				mMaxScale = SCALEZ_MAX * mMinScale;// 20
				return;
			} else {
				mMinScale = Math.min(widthScale, heightScale);
				mMaxScale = SCALEZ_MAX * mMinScale;
				return;
			}
		} else {// 竖屏
			if (widthScale < heightScale) {
				mMinScale = widthScale;
				mMaxScale = SCALEZ_MAX * mMinScale;
				return;
			} else {
				mMinScale = Math.min(widthScale, heightScale);
				mMaxScale = SCALEZ_MAX * mMinScale;
				return;
			}
		}
	}

	/**
	 * 向右旋转
	 */
	public void rotateRight() {
		if (mBitmap == null) {
			return;
		}

		mRevertXY = !mRevertXY;
		mRotatedDegree = (mRotatedDegree + 90) % 360;

		if (mBitmap != null) {
			int cx = mBitmap.getWidth() / 2;
			int cy = mBitmap.getHeight() / 2;
			mCurrentMatrix.preTranslate(-cx, -cy);
		}

		mCurrentMatrix.postRotate(90);
		mCurrentMatrix.postTranslate(getWidth() / 2, getHeight() / 2);

		fixPositionLimit();
		fixPositionCenter(true);
		setImageMatrix(mCurrentMatrix);
	}

	/**
	 * 向左旋转
	 */
	public void rotateLeft() {
		if (mBitmap == null) {
			return;
		}

		mRevertXY = !mRevertXY;
		mRotatedDegree = (mRotatedDegree + 270) % 360;
		RectF windowRect = getWindowRect();
		mCurrentMatrix.postRotate(270, windowRect.centerX(), windowRect.centerY());
		fixPositionLimit();
		fixPositionCenter(true);
		setImageMatrix(mCurrentMatrix);
	}

	/***
	 * 放大
	 */
	public void zoomIn(float scale) {
		mSavedMatrix.set(mCurrentMatrix);
		mCurrentMatrix.postScale(scale, scale);
		fixPositionLimit();
		fixPositionCenter(true);
		setImageMatrix(mCurrentMatrix);
	}

	public void setBitmap(Bitmap bitmap) {
		mRevertXY = false;
		mRotatedDegree = 0;

		if (bitmap != null) {
			mDrawable = null;

		}

		resetImageBitmap(bitmap);
		if (needRotate(bitmap)) {

//			setMinScaleForRotata();
			rotateRight();
		}
	}

	private boolean needRotate(Bitmap map) {
		DisplayMetrics dm = new DisplayMetrics();
		WindowManager wm = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
		wm.getDefaultDisplay().getMetrics(dm);
		int screenWidth = dm.widthPixels;
		int screenHight = dm.heightPixels;
		int bitmapwith = mBitmap.getWidth();
		int bitmapheight = mBitmap.getHeight();
		if (screenHight >= screenWidth && bitmapheight >= bitmapwith) {
			return false;
		} else if (screenHight <= screenWidth && bitmapheight <= bitmapwith) {
			return false;
		}
		return true;
	}

	public void setDrawable(Drawable drawable) {
		mRevertXY = false;
		mRotatedDegree = 0;

		if (drawable != null) {
			mBitmap = null;
		}

		resetImageDrawable(drawable);
	}

	/**
	 * 设置图片，无论当前是否有正在查看的图片
	 * 
	 * @param bitmap
	 */
	public void resetImageBitmap(Bitmap bitmap) {
		if (bitmap != null) {
			mBitmap = bitmap;

			mWindowRect = null;

			super.setImageBitmap(mBitmap);

			mBitmapWidth = mBitmap.getWidth();
			mBitmapHeight = mBitmap.getHeight();

			mCurrentMatrix.reset();
			resetImageRect();

			RectF windowRect = getWindowRect();
			mCurrentMatrix.postRotate(mRotatedDegree, windowRect.centerX(), windowRect.centerY());
			fixPositionLimit();
			fixPositionCenter(true);
			mSavedMatrix.set(mCurrentMatrix);
			setScaleType(ScaleType.MATRIX);

			setMinScale();
			zoomIn(mMinScale);
		}
	}

	public void resetImageDrawable(Drawable drawable) {
		if (drawable != null) {
			mDrawable = drawable;

			mWindowRect = null;
			super.setImageDrawable(mDrawable);

			mBitmapWidth = mDrawable.getIntrinsicWidth();
			mBitmapHeight = mDrawable.getIntrinsicHeight();

			mCurrentMatrix.reset();
			resetImageRect();

			RectF windowRect = getWindowRect();
			mCurrentMatrix.postRotate(mRotatedDegree, windowRect.centerX(), windowRect.centerY());
			fixPositionLimit();
			fixPositionCenter(true);
			mSavedMatrix.set(mCurrentMatrix);
			setScaleType(ScaleType.MATRIX);

			setMinScale();
			zoomIn(mMinScale);
		}
	}

	/**
	 * 设置图片，为了重载系统接口并保护正在查看的图片不被刷新
	 */
	@Override
	public void setImageBitmap(Bitmap bitmap) {
		if (bitmap != null) {
			setBitmap(bitmap);
		} else {
			super.setImageBitmap(null);
			// float[] values = new float[9];
			// mCurrentMatrix.getValues(values);
			// float scale = this.getScaleX(values);
			// if(scale != mMinScale){
			// zoomIn(mMinScale);
			// fixPositionLimit();
			// fixPositionCenter(true);
			// setImageMatrix(mCurrentMatrix);
			// }
		}
	}

	public void zoomFill() {
		if (mBitmap == null) {
			return;
		}
		DisplayMetrics dm = new DisplayMetrics();
		WindowManager wm = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
		wm.getDefaultDisplay().getMetrics(dm);
		int screenWidth = dm.widthPixels;
		int screenHight = dm.heightPixels;
		int bitmapwith = mBitmap.getWidth();
		int bitmapheight = mBitmap.getHeight();
		double scalw = bitmapwith * 1.0 / screenWidth;
		double scalh = bitmapheight * 1.0 / screenHight;
		float scal = 1 / (float) (scalw > scalh ? scalw : scalh);
		mCurrentMatrix = new Matrix();
		mCurrentMatrix.postScale(scal, scal);

		setImageMatrix(mCurrentMatrix);

	}

	@Override
	public void setOnClickListener(OnClickListener l) {
		mOnClickListener = l;
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {

		RectF curImageRect = new RectF();
		mCurrentMatrix.mapRect(curImageRect, mImageRect);
		int action = event.getAction() & MotionEvent.ACTION_MASK;
		float[] lastPoint = new float[2];

		// QLog.v(TAG, "onTouchEvent() action = " + action);

		switch (action) {
		case MotionEvent.ACTION_DOWN: {
			mMoved = false;
			mSavedMatrix.set(mCurrentMatrix);
			mStartPoint.set(event.getX(), event.getY());
			lastPoint[0] = mStartPoint.x;
			lastPoint[1] = mStartPoint.y;

			mTouchMode = TOUCH_MODE_DRAG;

			ImageLookerSlideGallery.setGalleryChildRequestTouch(true);
		}
			break;
		case MotionEvent.ACTION_POINTER_DOWN: {
			mMoved = true;
			mOldDist = spacing(event);
			if (mOldDist > 6f) {
				mSavedMatrix.set(mCurrentMatrix);
				mMidPoint = setMidPoint(mMidPoint, event);
				mTouchMode = TOUCH_MODE_ZOOM;
			}
		}
			break;
		case MotionEvent.ACTION_CANCEL:
		case MotionEvent.ACTION_UP:
		case MotionEvent.ACTION_POINTER_UP: {
			if (isDoubleZoomed || mTouchMode == TOUCH_MODE_ZOOM) {
				float[] values = new float[9];
				mCurrentMatrix.getValues(values);
				float s = getScaleX(values);
				if (s < mMinScale) {
					mCurrentMatrix.postScale((float) mMinScale / s, (float) mMinScale / s, mMidPoint.x, mMidPoint.y);
				}
				fixPositionCenter(false);
				setImageMatrix(mCurrentMatrix);
			}
			if (isBound && mTouchMode == TOUCH_MODE_DRAG) {
				isBound = false;
			}

			mTouchMode = TOUCH_MODE_NONE;
			mSavedMatrix.set(mCurrentMatrix);
			if (!mMoved) {
				if (mHandler.hasMessages(MSG_ON_CLICK)) {
					mHandler.removeMessages(MSG_ON_CLICK);
					Message msg = mHandler.obtainMessage();
					PointF point = new PointF();
					point.x = event.getX();
					point.y = event.getY();
					msg.obj = point;
					msg.what = MSG_ON_DOUBLECLICK;
					mHandler.sendMessage(msg);
				} else {
					mHandler.sendEmptyMessageDelayed(MSG_ON_CLICK, MSG_ON_CLICK_DELAY);
				}
			}

			ImageLookerSlideGallery.setGalleryChildRequestTouch(false);
		}
			break;
		case MotionEvent.ACTION_MOVE: {
			if (!mMoved && (Math.abs(mStartPoint.x - event.getX()) >= 2 || Math.abs(mStartPoint.y - event.getY()) >= 2)) {
				mMoved = true;
			}

			if (mMoved && mDrawable != null) {
				ImageLookerSlideGallery.setGalleryChildRequestTouch(false);
			}

			mCurrentMatrix.set(mSavedMatrix);

			if (mTouchMode == TOUCH_MODE_DRAG) {

				float[] values = new float[9];
				mCurrentMatrix.getValues(values);
				float scale = Math.max(getScaleX(values), 1f);
				float[] point = new float[2];
				float x = event.getX();
				float y = event.getY();
				mCurrentMatrix.mapPoints(point, new float[] { x, y });
				int nBitmapState = getBitmapState(0);

				float dx = (x - mStartPoint.x) * scale;
				float dy = (y - mStartPoint.y) * scale;

				switch (nBitmapState) {
				case PIC_OUTOF_BOUND_Y: {
					int result = getBitmapTranslateState();
					boolean bToTrans = true;
					switch (result) {
					case PIC_TOP:
						if (dy >= 0) {
							bToTrans = false;
						}
						break;
					case PIC_BOTTOM:
						if (dy <= 0) {
							bToTrans = false;
						}
						break;
					}

					// QLog.v(TAG, "PIC_OUTOF_BOUND_Y, dy = " + dy);

					if (!bToTrans && mMoved) {
						ImageLookerSlideGallery.setGalleryChildRequestTouch(false);
					} else {
						mCurrentMatrix.postTranslate(0, dy);
					}

					break;
				}
				case PIC_OUTOF_BOUND_X: {
					int result = getBitmapTranslateState();
					boolean bToTrans = true;
					switch (result) {
					case PIC_LEFT:
						if (dx >= 0) {
							bToTrans = false;
						}
						break;
					case PIC_RIGHT:
						if (dx <= 0) {
							bToTrans = false;
						}
						break;
					default:
						break;
					}

					// QLog.v(TAG, "PIC_OUTOF_BOUND_X, result = " + result +
					// ", dx = " + dx);
					if (!bToTrans && mMoved) {
						ImageLookerSlideGallery.setGalleryChildRequestTouch(false);
					} else {
						mCurrentMatrix.postTranslate(dx, 0);
					}

					break;
				}
				case PIC_OUTOF_BOUND_X_Y: {
					int result = getBitmapTranslateState();
					boolean bToTrans = true;
					switch (result) {
					case PIC_LEFT_TOP: // 左上角
						if (dx >= 0 && dy >= 0) {
							bToTrans = false;
						}
						break;
					case PIC_LEFT_BOTTOM:
						if (dx >= 0 && dy <= 0) {
							bToTrans = false;
						}
						break;
					case PIC_LEFT:
						if (dx >= 0 && java.lang.Math.abs(dy) < 10) {
							bToTrans = false;
						}
						break;
					case PIC_RIGHT:
						if (dx <= 0 && java.lang.Math.abs(dy) < 10) {
							bToTrans = false;
						}
						break;
					case PIC_RIGHT_TOP:
						if (dx <= 0 && dy >= 0) {
							bToTrans = false;
						}
						break;
					case PIC_RIGHT_BOTTOM:
						if (dx <= 0 && dy <= 0) {
							bToTrans = false;
						}
						break;
					}

					// QLog.v(TAG, "PIC_OUTOF_BOUND_X_Y, result = " + result +
					// ", dx = " + dx + ", dy = " + dy);
					if (!bToTrans && mMoved) {
						ImageLookerSlideGallery.setGalleryChildRequestTouch(false);
					} else {
						mCurrentMatrix.postTranslate(dx, dy);
					}
					break;
				}
				default:
					if (mMoved) {
						ImageLookerSlideGallery.setGalleryChildRequestTouch(false);
					}
					break;
				}
				lastPoint[0] = x;
				lastPoint[1] = y;

				if (getXOffset() <= 10) {
					isBound = true;
				}
			} else if (mTouchMode == TOUCH_MODE_ZOOM) {

				float newDist = spacing(event);
				if (newDist > 10f) {
					float scale = newDist / mOldDist;
					mCurrentMatrix.postScale(scale, scale, mMidPoint.x, mMidPoint.y);
					float[] values = new float[9];
					mCurrentMatrix.getValues(values);
					float s = getScaleX(values);
					if (s > mMaxScale) {
						mCurrentMatrix.postScale(mMaxScale / s, mMaxScale / s, mMidPoint.x, mMidPoint.y);
					}
					fixPositionCenter(false);
				}
			}

			fixPositionLimit();
		}
			break;
		}

		if (mBitmap == null) {
			mCurrentMatrix.reset();
		} else if (!getImageMatrix().equals(mCurrentMatrix)) {
			setImageMatrix(mCurrentMatrix);
			mMoved = true;
		}

		return ImageLookerSlideGallery.isGalleryChildRequestTouch();
	}

	/**
	 * 获取图片的偏移状态
	 * 
	 * @return
	 */
	private int getBitmapTranslateState() {

		int result = PIC_DEFAULT;

		RectF rect = new RectF(0, 0, mBitmap.getWidth(), mBitmap.getHeight());
		mCurrentMatrix.mapRect(rect);

		int w = getWidth();
		int h = getHeight();

		int left, top, right, bottom;
		left = (int) rect.left;
		top = (int) rect.top;
		right = (int) rect.right;
		bottom = (int) rect.bottom;

		if (left == 0) {
			if (top == 0) {
				result = PIC_LEFT_TOP;
			} else if (bottom == h) {
				result = PIC_LEFT_BOTTOM;
			} else {
				result = PIC_LEFT;
			}
		} else if (right == w) {
			if (top == 0) {
				result = PIC_RIGHT_TOP;
			} else if (bottom == h) {
				result = PIC_RIGHT_BOTTOM;
			} else {
				result = PIC_RIGHT;
			}
		} else if (top == 0) {
			result = PIC_TOP;
		} else if (bottom == h) {
			result = PIC_BOTTOM;
		}

		return result;
	}

	/**
	 * 得到图片的高宽状态：是否超高超宽
	 * 
	 * @param scale
	 * @return
	 */
	private int getBitmapState(float scale) {
		int w, h;
		int result = PIC_OUTOF_BOUND_X_Y;

		if (scale == 0) {
			float[] values = new float[9];
			mCurrentMatrix.getValues(values);
			w = (int) (mBitmapWidth * getScaleX(values));
			h = (int) (mBitmapHeight * getScaleY(values));
		} else {
			w = (int) (mBitmapWidth * scale);
			h = (int) (mBitmapHeight * scale);
		}

		if (mRevertXY) {
			int t = w;
			w = h;
			h = t;
		}

		RectF wr = getWindowRect();

		if (w <= wr.width()) {
			result = (h <= wr.height()) ? PIC_IN_BOUND_X_Y : PIC_OUTOF_BOUND_Y;
		} else if (h <= wr.height()) {
			result = PIC_OUTOF_BOUND_X;
		}

		return result;
	}

	/**
	 * 计算两点触摸点的距离
	 * 
	 * @param event
	 *            两点的触摸事件
	 * @return
	 */
	private float spacing(MotionEvent event) {
		float x = event.getX(0) - event.getX(1);
		float y = event.getY(0) - event.getY(1);
		return FloatMath.sqrt(x * x + y * y);
	}

	/**
	 * 得到两点的中点坐标
	 * 
	 * @param point
	 *            输出的中点坐标
	 * @param event
	 *            两点的触摸事件
	 * @return
	 */
	private PointF setMidPoint(PointF point, MotionEvent event) {
		float x = event.getX(0) + event.getX(1);
		float y = event.getY(0) + event.getY(1);
		point.set(x / 2, y / 2);
		return point;
	}

	private float getXOffset() {
		float w, h;
		float[] values = new float[9];
		mCurrentMatrix.getValues(values);

		w = mBitmapWidth * getScaleX(values);
		h = mBitmapHeight * getScaleY(values);
		if (mRevertXY) {
			float t = w;
			w = h;
			h = t;
		}

		RectF curImageRect = new RectF();
		RectF windowRect = getWindowRect();
		mCurrentMatrix.mapRect(curImageRect, mImageRect);

		float deltaX = 0;
		if (curImageRect.width() > windowRect.width()) {
			if (curImageRect.left > windowRect.left) {
				deltaX = windowRect.left - curImageRect.left;
			} else if (curImageRect.right < windowRect.right) {
				deltaX = windowRect.right - curImageRect.right;
			}
		}
		return deltaX;
	}

	/**
	 * 如果图片超宽或超高，让图片边缘不留空隙 *
	 */
	private void fixPositionLimit() {
		float w, h;
		float[] values = new float[9];
		mCurrentMatrix.getValues(values);

		w = mBitmapWidth * getScaleX(values);
		h = mBitmapHeight * getScaleY(values);
		if (mRevertXY) {
			float t = w;
			w = h;
			h = t;
		}

		RectF curImageRect = new RectF();
		RectF windowRect = getWindowRect();
		mCurrentMatrix.mapRect(curImageRect, mImageRect);

		float deltaX = 0;
		float deltaY = 0;
		if (curImageRect.width() > windowRect.width()) {
			if (curImageRect.left > windowRect.left) {
				deltaX = windowRect.left - curImageRect.left;
			} else if (curImageRect.right < windowRect.right) {
				deltaX = windowRect.right - curImageRect.right;
			}
		}
		if (curImageRect.height() > windowRect.height()) {
			if (curImageRect.top > windowRect.top) {
				deltaY = windowRect.top - curImageRect.top;
			} else if (curImageRect.bottom < windowRect.bottom) {
				deltaY = windowRect.bottom - curImageRect.bottom;
			}
		}
		if (deltaX != 0 || deltaY != 0) {
			mCurrentMatrix.postTranslate(deltaX, deltaY);
		}
	}

	/**
	 * 使图片居中显示
	 * 
	 * @param force
	 *            true：强制居中，不考虑当前图片的大小和位置
	 */
	private void fixPositionCenter(boolean force) {
		float w, h;
		float[] values = new float[9];
		mCurrentMatrix.getValues(values);
		w = mBitmapWidth * getScaleX(values);
		h = mBitmapHeight * getScaleY(values);
		if (mRevertXY) {
			float t = w;
			w = h;
			h = t;
		}
		if (force || w < mWLimit - mPaddingX - mPaddingX) {
			float tX = mWLimit / 2 - w / 2; // 缩放后应移动
			float post = tX - values[Matrix.MTRANS_X] + getRotatedDeltaX(w, values);
			mCurrentMatrix.postTranslate(post, 0);
			mMidPoint.x = mWLimit / 2;
		}
		if (force || h < mHLimit - mPaddingY - mPaddingY) {
			float tY = mHLimit / 2 - h / 2; // 缩放后应移动
			float post = tY - values[Matrix.MTRANS_Y] + getRotatedDeltaY(h, values);
			mCurrentMatrix.postTranslate(0, post);
			mMidPoint.y = mHLimit / 2;
		}
	}

	private void fixImageAlignTop() {
		float[] values = new float[9];
		mCurrentMatrix.getValues(values);
		float deltaY = values[Matrix.MTRANS_Y];
		if (deltaY < 0) {
			mCurrentMatrix.postTranslate(0, -deltaY);
		}
	}

	private void fixImageAlignLeft() {
		float[] values = new float[9];
		mCurrentMatrix.getValues(values);
		float deltaX = values[Matrix.MTRANS_X];
		if (deltaX < 0) {
			mCurrentMatrix.postTranslate(0, -deltaX);
		}
	}

	private float calculateFixedScale(int widthLimit, int heightLimit) {
		float scale;
		boolean divWithScreen = ((float) mBitmapWidth / mBitmapHeight) > (float) widthLimit / heightLimit;
		if (divWithScreen) {
			scale = (float) widthLimit / mBitmapWidth;
		} else {
			scale = (float) heightLimit / mBitmapHeight;
		}
		scale = Math.max(scale, mMinScale);
		scale = Math.min(scale, mMaxScale);
		return scale;
	}

	/**
	 * 使图片充满屏幕
	 * */
	public void setImageFullScreenAndFitCenter() {
		float scale = calculateFixedScale(mWLimit, mHLimit);
		mCurrentMatrix.postScale(scale, scale);
		fixPositionCenter(true);
		setImageMatrix(mCurrentMatrix);
	}

	/**
	 * 使图片充满设置的可视区域
	 */
	public void setImageFullWindow() {
		getWindowRect();
		float scale = calculateFixedScale((int) mWindowRect.width(), (int) mWindowRect.height());
		mCurrentMatrix.postScale(scale, scale);
		fixPositionCenter(true);
		setImageMatrix(mCurrentMatrix);
	}

	/**
	 * 平移时需要修改的X偏移量
	 * 
	 * @param width
	 *            图片宽度
	 * @param values
	 * @return
	 */
	private float getRotatedDeltaX(float width, float[] values) {
		float x = Math.min(values[Matrix.MSCALE_X], values[Matrix.MSKEW_X]);
		return x < 0 ? width : 0;
	}

	/**
	 * 平移时需要修改的Y偏移量
	 * 
	 * @param height
	 *            图片高度
	 * @param values
	 * @return
	 */
	private float getRotatedDeltaY(float height, float[] values) {
		float y = Math.min(values[Matrix.MSCALE_Y], values[Matrix.MSKEW_Y]);
		return y < 0 ? height : 0;
	}

	/**
	 * 获得X缩放比例
	 * 
	 * @param values
	 * @return
	 */
	private float getScaleX(float[] values) {
		return Math.abs(mRevertXY ? values[Matrix.MSKEW_X] : values[Matrix.MSCALE_X]);
		// boolean isVertical = getHeight() > getWidth();
		// boolean isRevert = isVertical ^ mRevertXY;
		// return Math.abs(isRevert ? values[Matrix.MSCALE_X] :
		// values[Matrix.MSKEW_X]);
	}

	/**
	 * 获得Y缩放比例
	 * 
	 * @param values
	 * @return
	 */
	private float getScaleY(float[] values) {
		return Math.abs(mRevertXY ? values[Matrix.MSKEW_Y] : values[Matrix.MSCALE_Y]);
		// boolean isVertical = getHeight() > getWidth();
		// boolean isRevert = isVertical ^ mRevertXY;
		// return Math.abs(isRevert ? values[Matrix.MSCALE_Y] :
		// values[Matrix.MSKEW_Y]);
	}

	public Bitmap getBitmap() {
		return mBitmap;
	}

	/**
	 * 设置图片可被拖动的位置和View边界的距离
	 * 
	 * @param paddingX
	 *            左右的距离
	 * @param paddingY
	 *            上下的距离
	 * */
	public void setVisableRectLimit(int paddingX, int paddingY) {
		mPaddingX = paddingX;
		mPaddingY = paddingY;
		if (mPaddingX != 0 || mPaddingY != 0) {
			resetImageRect();
			mWindowRect = new RectF(mPaddingX, mPaddingY, mWLimit - mPaddingX, mHLimit - mPaddingY);
		} else {
			mWindowRect = null;
		}
	}

	/**
	 * 获取在选择框中可显示图片的原图 如果没有设置选择框，默认是整个ImageView区域
	 * 
	 * @return
	 */
	public Bitmap getBitmapInWindow() {
		final int X = 0;
		final int Y = 1;
		Bitmap output = null;
		getWindowRect();
		if (mBitmap != null && mWindowRect != null) {
			float[] imageLeftTop = new float[] { mImageRect.left, mImageRect.top };
			float[] windowLeftTop = new float[2];

			if (mRotatedDegree % 360 == 0) {
				windowLeftTop[X] = mWindowRect.left;
				windowLeftTop[Y] = mWindowRect.top;
			} else if (mRotatedDegree % 360 == 90) {
				windowLeftTop[X] = mWindowRect.right;
				windowLeftTop[Y] = mWindowRect.top;
			} else if (mRotatedDegree % 360 == 180) {
				windowLeftTop[X] = mWindowRect.right;
				windowLeftTop[Y] = mWindowRect.bottom;
			} else if (mRotatedDegree % 360 == 270) {
				windowLeftTop[X] = mWindowRect.left;
				windowLeftTop[Y] = mWindowRect.bottom;
			}
			mCurrentMatrix.mapPoints(imageLeftTop);

			float[] values = new float[9];
			mCurrentMatrix.getValues(values);
			float scaleX = mRevertXY ? values[Matrix.MSKEW_X] : values[Matrix.MSCALE_X];
			float scaleY = mRevertXY ? values[Matrix.MSKEW_Y] : values[Matrix.MSCALE_Y];
			float x = (windowLeftTop[X] - imageLeftTop[X]) / scaleX;
			float y = (windowLeftTop[Y] - imageLeftTop[Y]) / scaleY;

			if (mRevertXY) {
				float t = x;
				x = y;
				y = t;
			}

			scaleX = Math.abs(scaleX);
			scaleY = Math.abs(scaleY);
			int outputWidth = (int) (mWindowRect.width() / scaleX);
			int outputHeight = (int) (mWindowRect.height() / scaleY);
			output = Bitmap.createBitmap(outputWidth, outputHeight, Config.ARGB_8888);
			Canvas canvas = new Canvas(output);
			canvas.drawBitmap(mBitmap, new Rect((int) x, (int) y, (int) (x + outputWidth), (int) (y + outputHeight)),
					new RectF(0, 0, output.getWidth(), output.getHeight()), new Paint());

			if (mRotatedDegree % 360 != 0) {
				// TODO: 当选择区域不是正方形时，这里有bug
				// 如果只是0度或270度，就把新建绘图区域的宽和高调换
				// （output = Bitmap.createBitmap(outputHeight, outputWidth,
				// Config.ARGB_8888)
				// 未经测试验证
				Bitmap outputRotate = output;
				output = Bitmap.createBitmap(outputWidth, outputHeight, Config.ARGB_8888);
				canvas = new Canvas(output);

				Matrix rotate = new Matrix();
				rotate.postRotate(mRotatedDegree, outputWidth / 2, outputHeight / 2);
				canvas.drawBitmap(outputRotate, rotate, new Paint());
			}
		}
		return output;
	}

	// 得到变换矩阵，只得到旋转角度，不需要带有缩放的变换矩阵，会产生crash问题
	public Matrix getCurrentMatrix() {
		mSavedMatrixRotate.setRotate(mRotatedDegree);
		return mSavedMatrixRotate;
	}

	public int getBitmapWidth() {
		return mBitmapWidth;
	}

	public int getBitmapHeight() {
		return mBitmapHeight;
	}

	public boolean isZoomed() {
		float[] values = new float[9];
		mCurrentMatrix.getValues(values);
		float scale = getScaleX(values);
		// 0.9-1.1倍数直接的scale都视为没有缩放
		return !(scale >= mMinScale * 0.9 && scale <= mMinScale * 1.1);
	}

	// 回复变换矩阵
	public void resetImageView() {
		init(mContext);
	}

	public Bitmap drawableToBitmap(Drawable drawable) {
		int width = drawable.getIntrinsicWidth();
		int height = drawable.getIntrinsicHeight();
		Bitmap bitmap = Bitmap.createBitmap(width, height, drawable.getOpacity() != PixelFormat.OPAQUE ? Bitmap.Config.ARGB_8888 : Bitmap.Config.RGB_565);
		Canvas canvas = new Canvas(bitmap);
		drawable.setBounds(0, 0, width, height);
		drawable.draw(canvas);
		return bitmap;
	}
}
