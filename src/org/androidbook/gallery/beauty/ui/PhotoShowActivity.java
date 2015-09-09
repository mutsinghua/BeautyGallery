package org.androidbook.gallery.beauty.ui;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;

import android.app.AlertDialog;
import android.app.WallpaperManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.util.FloatMath;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.animation.AnimationUtils;
import android.widget.Gallery.LayoutParams;
import android.widget.ImageSwitcher;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.ViewSwitcher.ViewFactory;

import com.admogo.AdMogoLayout;
import com.admogo.AdMogoListener;

import org.androidbook.gallery.beauty.BeautyApplication;
import org.androidbook.gallery.beauty.R;
import org.androidbook.gallery.beauty.ui.view.WinImageSwitcher;
import org.androidbook.gallery.beauty.ui.view.WinImageSwitcher.OnImageDrawableChangeListener;
import org.androidbook.gallery.db.AvailableDB;
import org.androidbook.gallery.db.FavoriteDB;
import org.androidbook.gallery.db.ThumbManager;
import org.androidbook.utils.FileData;
import org.androidbook.utils.SQLUtils;
import org.androidbook.utils.Util;

public class PhotoShowActivity extends BaseActivity implements OnTouchListener, ViewFactory
{

	static BaseActivity mContext = null;

	private int switchTime = 3;
	boolean isautoplay = false;
	private static final int MENU_LAYOUT_TIMEOUT = 5000;
	private static final int AD_LAYOUT_TIMEOUT = 100000;
	private static final int MAX_BITMAP_SIZE = 1200;

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{

		getMenuInflater().inflate(R.menu.show_image_menu, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch (item.getItemId())
		{
		case R.id.menu_autoplay:
			if (isautoplay)
			{
				isautoplay = false;
				stopAutoPlay();
			} else
			{
				buildTimeChooser(item);
			}
			break;
		case R.id.menu_setwall:
		{
			try
			{
				WallpaperManager.getInstance(PhotoShowActivity.this).setBitmap(bitmap);
			} catch (IOException e)
			{
				showToast(R.string.set_wall_paper_failed);
				break;
			}
			showToast(R.string.set_wall_paper_success);
		}
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	private static final String TAG = "Touch";

	// These matrices will be used to move and zoom image
	Matrix matrix = new Matrix();
	Matrix savedMatrix = new Matrix();
	PointF start = new PointF();
	PointF mid = new PointF();
	float oldDist;

	// We can be in one of these 3 states
	static final int NONE = 0;
	static final int DRAG = 1;
	static final int ZOOM = 2;
	int mode = NONE;

	private int imageId;
	static final int HIDE_BUTTON_LAYER = 1;
	static final int SHOW_BUTTON_LAYER = 2;

	static AtomicInteger tapCount = new AtomicInteger();
	static final int FIRST_TAP = 3;
	static final int SECOND_TAP = 4;
	static final int TIMEOUT = 5;

	static final int GO_NEXT = 1;
	static final int STOP_NEXT = 2;
	public static Bitmap bitmap;
	private Queue<WeakReference<Bitmap>> usedBitmap = new LinkedList<WeakReference<Bitmap>>();
	public static ThumbManager thumbManager;
	private View buttonLayout;
	private WinImageSwitcher mainImageViewSwitch;

	private Handler timeHandler = new Handler()
	{
		public void handleMessage(Message msg)
		{
			switch (msg.what)
			{
			case HIDE_BUTTON_LAYER:
				buttonLayout.startAnimation(AnimationUtils.loadAnimation(PhotoShowActivity.this, android.R.anim.fade_out));
				buttonLayout.setVisibility(View.INVISIBLE);
				break;
			case SHOW_BUTTON_LAYER:
				buttonLayout.startAnimation(AnimationUtils.loadAnimation(PhotoShowActivity.this, android.R.anim.fade_in));
				buttonLayout.setVisibility(View.VISIBLE);
				break;
			case FIRST_TAP:
				timeHandler.sendEmptyMessageDelayed(TIMEOUT, 700);
				tapCount.getAndIncrement();
				break;
			case SECOND_TAP:
				if (tapCount.get() == 1)
				{
					timeHandler.removeMessages(TIMEOUT);
					doDoubleTap((ImageView) msg.obj);
					tapCount.set(0);
				}
				break;
			case TIMEOUT:
				timeHandler.removeMessages(TIMEOUT);
				tapCount.set(0);
				break;

			}
		}
	};

	private void doDoubleTap(ImageView view)
	{
		relocateBitmap(view);
	}

	private Handler handlerAutoPlay = new Handler()
	{
		public void handleMessage(Message msg)
		{
			switch (msg.what)
			{
			case GO_NEXT:

				handlerAutoPlay.sendEmptyMessageDelayed(GO_NEXT, (switchTime + 2) * 1000);
				goNext();
				break;
			}
		}
	};
	
	private PowerManager.WakeLock wakeLock;
	
	private void startAutoPlay()
	{
		if (wakeLock == null)
		{
			wakeLock = BeautyApplication.instance.pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "photoshow");
		}
		// if(!wakeLock.isHeld())
		// {
		wakeLock.acquire();
		// }
		handlerAutoPlay.sendEmptyMessageDelayed(GO_NEXT, (switchTime + 2) * 1000);
	}

	private void stopAutoPlay()
	{
		// if(wakeLock !=null && wakeLock.isHeld())
		// {
		if (wakeLock != null)
		{
			wakeLock.release();
		}
		// }
		handlerAutoPlay.removeMessages(GO_NEXT);
	}

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
		setContentView(R.layout.image_show);
		if (thumbManager == null)
		{
			finish();
			return;
		}

		imageId = -1;
		if (savedInstanceState == null)
		{
			Intent intent = getIntent();
			imageId = intent.getIntExtra("IMAGE_ID", -1);
		} else
		{
			imageId = savedInstanceState.getInt("IMAGE_ID", -1);
		}
		if (imageId == -1)
		{
			showDialog(R.string.find_no_pic_id);
			finish();
		}

		mainImageViewSwitch = (WinImageSwitcher) findViewById(R.id.image);
		mainImageViewSwitch.setFactory(this);
		mainImageViewSwitch.setInAnimation(AnimationUtils.loadAnimation(this, android.R.anim.fade_in));
		mainImageViewSwitch.setOutAnimation(AnimationUtils.loadAnimation(this, android.R.anim.fade_out));

		mainImageViewSwitch.setOnDrawableChangleListener(new OnImageDrawableChangeListener()
		{

			@Override
			public void onDrawableChange(ImageView imageView)
			{
				relocateBitmap(imageView);

			}
		});
		byte[] bitmapdata = SQLUtils.getPic(imageId, thumbManager.getDb());

		bitmap = getSafeBitmap(bitmapdata);
		WeakReference<Bitmap> wf = new WeakReference<Bitmap>(bitmap);
		usedBitmap.add(wf);

		mainImageViewSwitch.setImageBitmap(bitmap);
		initButton();
		buttonLayout = findViewById(R.id.menuLayer);
		timeHandler.sendEmptyMessageDelayed(HIDE_BUTTON_LAYER, MENU_LAYOUT_TIMEOUT);

		admogo = (AdMogoLayout) findViewById(R.id.admogo_layout);
		if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE)
		{
			hideAd();
		}
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig)
	{
		super.onConfigurationChanged(newConfig);
		if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE)
		{
			hideAd();
		} else
		{
			showAd();
		}
	}

	private void showAd()
	{
		admogo.setVisibility(View.VISIBLE);
		handlerAd.sendEmptyMessageDelayed(HIDE_ADMOGO, AD_LAYOUT_TIMEOUT);
	}

	private void hideAd()
	{
		admogo.setVisibility(View.GONE);
		handlerAd.removeMessages(HIDE_ADMOGO);
	}

	private static final int HIDE_ADMOGO = 1;
	private Handler handlerAd = new Handler()
	{
		public void handleMessage(Message msg)
		{
			switch (msg.what)
			{
			case HIDE_ADMOGO:
				hideAd();
				break;

			}
		}
	};

	@Override
	public View makeView()
	{
		ImageView imageview = new ImageView(this);
		imageview.setScaleType(ImageView.ScaleType.MATRIX);
		imageview.setLayoutParams(new ImageSwitcher.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
		imageview.setOnTouchListener(this);

		return imageview;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu)
	{
		MenuItem item = menu.findItem(R.id.menu_autoplay);
		if (isautoplay)
		{
			item.setTitle(R.string.stopplay);
			item.setIcon(R.drawable.stop_play);

		} else
		{
			item.setTitle(R.string.autoplay);
			item.setIcon(R.drawable.autoplay);
		}
		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	protected void onDestroy()
	{
		while (usedBitmap.peek() != null)
		{
			WeakReference<Bitmap> wr = usedBitmap.poll();
			Bitmap bm = wr.get();
			if (bm != null && !bm.isRecycled())
			{
				bm.recycle();
			}
		}
		mContext = null;
		bitmap = null;
		thumbManager = null;
		super.onDestroy();
	}

	@Override
	protected void onResume()
	{
		super.onResume();
		handlerAd.sendEmptyMessageDelayed(HIDE_ADMOGO, AD_LAYOUT_TIMEOUT);

	}

	public void initButton()
	{
		findViewById(R.id.menu_toleft).setOnClickListener(onclickListener);
		findViewById(R.id.menu_toright).setOnClickListener(onclickListener);
		findViewById(R.id.menu_crop).setOnClickListener(onclickListener);
		findViewById(R.id.menu_fav).setOnClickListener(onclickListener);
		findViewById(R.id.menu_share).setOnClickListener(onclickListener);
	}

	private OnClickListener onclickListener = new OnClickListener()
	{

		@Override
		public void onClick(View v)
		{
			timeHandler.removeMessages(HIDE_BUTTON_LAYER);
			timeHandler.sendEmptyMessageDelayed(HIDE_BUTTON_LAYER, MENU_LAYOUT_TIMEOUT);
			switch (v.getId())
			{
			case R.id.menu_toleft:
			{
				int position = thumbManager.getPosition(imageId);
				int oldposition = position;
				position--;

				if (position < 0)// 修正
				{
					position = thumbManager.getImageCount() - 1;
				}
				if (oldposition == position)
				{
					showToast(R.string.only_one);
				} else
				{
					imageId = thumbManager.getImageId(position);
					switchBitmap();
				}
			}
				break;
			case R.id.menu_toright:
			{
				goNext();
			}
				break;
			case R.id.menu_share:
			{
				shareBitmap(PhotoShowActivity.this, bitmap);
				break;
			}
			case R.id.menu_fav:
				if (mContext instanceof ShowFavThumbActivity)
				{
					showToast(R.string.already_fav);
				} else if (!(mContext instanceof ShowThumbActivity))
				{

					showToast(R.string.cannot_fav);
				} else
				{
					Thread t = new Thread()
					{
						public void run()
						{
							FavoriteDB.getInstance().addImage(bitmap, ((ShowThumbActivity) mContext).winImageDb);
							runOnUiThread(new Runnable()
							{

								@Override
								public void run()
								{
									showToast(R.string.fav_success);
								}
							});
						}
					};
					t.start();

				}

				break;

			case R.id.menu_crop:
			{
				CropImageActivity.launchCropperOrFinish(PhotoShowActivity.this, bitmap);
			}
				break;
			}

		}
	};

	private com.admogo.AdMogoLayout admogo;



	private void goNext()
	{
		int position = thumbManager.getPosition(imageId);
		int oldposition = position;

		position++;

		if (position >= thumbManager.getImageCount())// 修正
		{
			position = 0;
		}

		if (oldposition == position)
		{
			showToast(R.string.only_one);
			stopAutoPlay();
			isautoplay = false;
		} else
		{
			imageId = thumbManager.getImageId(position);
			switchBitmap();
		}

	}

	public static boolean shareBitmap(Context context, Bitmap bitmap)
	{
		String path = FileData.getDataPath() + "tmp.dat";
		byte[] data = FileData.serializeBitmap(bitmap);
		FileData.writeDataToNewFile(path, data);
		Intent sharei = new Intent(Intent.ACTION_SEND);
		sharei.setType("image/*");
		Uri uri = Uri.fromFile(new File(path));
		sharei.putExtra(Intent.EXTRA_STREAM, uri);
		sharei.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		Intent dd = Intent.createChooser(sharei, context.getString(R.string.share));

		if (dd != null)
		{
			context.startActivity(dd);
			return true;
		} else
		{
			return false;
		}
	}

	public Bitmap getSafeBitmap(byte[] data)
	{

		try
		{

			bitmap = FileData.getLimitBitmap(data, MAX_BITMAP_SIZE, MAX_BITMAP_SIZE);
		} catch (OutOfMemoryError ee)
		{
			// Log.e(TAG, "oom2");
			System.gc();
			showToast(R.string.out_of_memery);
			bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.default_gallery_bg);
		}

		return bitmap;
	}

	private final void switchBitmap()
	{
		byte[] bitmapdata = SQLUtils.getPic(imageId, thumbManager.getDb());
		bitmap = getSafeBitmap(bitmapdata);
		mainImageViewSwitch.setImageBitmap(bitmap);

		if (imageId % 2 == 0)
		{
			bitmap = getSafeBitmap(bitmapdata);
			mainImageViewSwitch.setImageBitmap(bitmap);
			WeakReference<Bitmap> wf = new WeakReference<Bitmap>(bitmap);
			usedBitmap.add(wf);
			while (usedBitmap.size() >= 4)
			{
				WeakReference<Bitmap> wr = usedBitmap.poll();
				Bitmap bm = wr.get();
				if (bm != null && !bm.isRecycled())
				{
					bm.recycle();
				}
			}
		}
		// bitmap = getSafeBitmap(bitmapdata);

	}

	public void relocateBitmap(ImageView imageView)
	{

		int bitmapwith = bitmap.getWidth();
		int bitmapheight = bitmap.getHeight();
		double scalw = bitmapwith * 1.0 / screenWidth;
		int pureScreenHight = (screenHight - Util.getPxFromDp(50, this));
		double scalh = bitmapheight * 1.0 / pureScreenHight; // 减去下面按钮的高度
		float scal = 1 / (float) (scalw > scalh ? scalw : scalh);
		matrix = new Matrix();
		if (bitmapwith > bitmapheight)// 作修正
		{
			// 如果是横副
			if (screenWidth > screenHight) // 如果屏幕也是横的
			{
				matrix.postScale(scal, scal);
				// do nothing
			} else
			{
				// 图片是横的，屏幕是竖的
				scal = (float) (1 / scalh);
				matrix.postScale(scal, scal);
				matrix.postTranslate(-((bitmapwith * scal) - screenWidth) / 2, 0);
			}
		} else
		{// 竖幅
			if (screenWidth > screenHight) // 如果屏幕也是横的
			{
				matrix.postScale(scal, scal);
				// scal = (float) (1/ scalw);
				// matrix.postScale(scal, scal);
				// matrix.postTranslate(dx, 0);
			} else
			{
				// 图片是竖的，屏幕是竖的

				// do nothing
				matrix.postScale(scal, scal);
			}
		}

		// matrix.postScale(scal, scal,(bitmapwith * scal)/2,(bitmapheight
		// *scal)/2);

		imageView.setImageMatrix(matrix);

	}

	@Override
	protected void onSaveInstanceState(Bundle outState)
	{
		outState.putInt("IMAGE_ID", imageId);
		super.onSaveInstanceState(outState);
	}

	public static void startActivity(Context context, int imageid, ThumbManager tm)
	{
		mContext = (BaseActivity) context;
		thumbManager = tm;
		Intent intent = new Intent(context, PhotoShowActivity.class);
		intent.putExtra("IMAGE_ID", imageid);
		context.startActivity(intent);
	}

	public boolean onTouch(View v, MotionEvent event)
	{
		// Handle touch events here...
		ImageView view = (ImageView) v;
		timeHandler.removeMessages(HIDE_BUTTON_LAYER);
		timeHandler.removeMessages(SHOW_BUTTON_LAYER);
		if (isautoplay)
		{
			isautoplay = (false);
			stopAutoPlay();

		}
		if (buttonLayout.getVisibility() == View.INVISIBLE)
		{
			timeHandler.sendEmptyMessage(SHOW_BUTTON_LAYER);
		}
		// Dump touch event to log
		// dumpEvent(event);

		// Handle touch events here...
		switch (event.getAction() & MotionEvent.ACTION_MASK)
		{
		case MotionEvent.ACTION_DOWN:
			savedMatrix.set(matrix);
			start.set(event.getX(), event.getY());
			// Log.d(TAG, "mode=DRAG");
			mode = DRAG;
			break;
		case MotionEvent.ACTION_UP:
			mode = NONE;
			// Log.d(TAG, "mode=NONE");
			timeHandler.sendEmptyMessageDelayed(HIDE_BUTTON_LAYER, MENU_LAYOUT_TIMEOUT);
			if (tapCount.get() == 0)
			{
				timeHandler.sendEmptyMessage(FIRST_TAP);
			} else if (tapCount.get() == 1)
			{
				Message msg = Message.obtain();
				msg.what = SECOND_TAP;
				msg.obj = view;
				timeHandler.sendMessage(msg);
			}
			break;
		case MotionEvent.ACTION_POINTER_UP:
			mode = NONE;
			// Log.d(TAG, "mode=NONE");
			timeHandler.sendEmptyMessageDelayed(HIDE_BUTTON_LAYER, MENU_LAYOUT_TIMEOUT);
			tapCount.set(0);
			break;
		case MotionEvent.ACTION_POINTER_DOWN:
			oldDist = spacing(event);
			// Log.d(TAG, "oldDist=" + oldDist);
			if (oldDist > 10f)
			{
				savedMatrix.set(matrix);
				midPoint(mid, event);
				mode = ZOOM;
				// Log.d(TAG, "mode=ZOOM");
			}
			tapCount.set(0);
			break;
		case MotionEvent.ACTION_MOVE:
			if (mode == DRAG)
			{
				matrix.set(savedMatrix);
				matrix.postTranslate(event.getX() - start.x, event.getY() - start.y);
			} else if (mode == ZOOM)
			{
				float newDist = spacing(event);
				// Log.d(TAG, "newDist=" + newDist);
				if (newDist > 10f)
				{
					matrix.set(savedMatrix);
					float scale = newDist / oldDist;
					matrix.postScale(scale, scale, mid.x, mid.y);
				}
			}
			tapCount.set(0);
			break;
		}

		// Perform the transformation
		view.setImageMatrix(matrix);
		return true; // indicate event was handled
	}

	/** Show an event in the LogCat view, for debugging */
	private void dumpEvent(MotionEvent event)
	{
		String names[] = { "DOWN", "UP", "MOVE", "CANCEL", "OUTSIDE", "POINTER_DOWN", "POINTER_UP", "7?", "8?", "9?" };
		StringBuilder sb = new StringBuilder();
		int action = event.getAction();
		int actionCode = action & MotionEvent.ACTION_MASK;
		sb.append("event ACTION_").append(names[actionCode]);
		if (actionCode == MotionEvent.ACTION_POINTER_DOWN || actionCode == MotionEvent.ACTION_POINTER_UP)
		{
			sb.append("(pid ").append(action >> MotionEvent.ACTION_POINTER_ID_SHIFT);
			sb.append(")");
		}
		sb.append("[");
		for (int i = 0; i < event.getPointerCount(); i++)
		{
			sb.append("#").append(i);
			sb.append("(pid ").append(event.getPointerId(i));
			sb.append(")=").append((int) event.getX(i));
			sb.append(",").append((int) event.getY(i));
			if (i + 1 < event.getPointerCount())
				sb.append(";");
		}
		sb.append("]");
		// Log.d(TAG, sb.toString());
	}

	private float spacing(MotionEvent event)
	{
		float x = event.getX(0) - event.getX(1);
		float y = event.getY(0) - event.getY(1);
		return FloatMath.sqrt(x * x + y * y);
	}

	private void midPoint(PointF point, MotionEvent event)
	{
		float x = event.getX(0) + event.getX(1);
		float y = event.getY(0) + event.getY(1);
		point.set(x / 2, y / 2);
	}

	private void buildTimeChooser(final MenuItem item)
	{
		LayoutInflater factory = LayoutInflater.from(this);
		final View fontDialogView = factory.inflate(R.layout.dialog_seek, null);
		final AlertDialog seekDialog = new AlertDialog.Builder(this).setView(fontDialogView).setPositiveButton(R.string.ok, new DialogInterface.OnClickListener()
		{
			public void onClick(DialogInterface dialog, int whichButton)
			{

				SeekBar seekbar = (SeekBar) fontDialogView.findViewById(R.id.font_sekkbar);
				int position = seekbar.getProgress();
				switchTime = position;
				isautoplay = true;
				item.setTitle(R.string.stopplay);
				startAutoPlay();
			}
		}).setNegativeButton(R.string.cancel, null).create();

		SeekBar seekbar = (SeekBar) fontDialogView.findViewById(R.id.font_sekkbar);
		seekbar.setMax(60);

		seekbar.setProgress(switchTime);
		seekDialog.show();

	}

	@Override
	protected void onPause()
	{
		isautoplay = false;
		stopAutoPlay();
		super.onPause();
	}

}
