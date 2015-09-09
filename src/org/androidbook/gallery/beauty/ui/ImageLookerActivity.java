package org.androidbook.gallery.beauty.ui;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.androidbook.gallery.beauty.BeautyApplication;
import org.androidbook.gallery.beauty.R;
import org.androidbook.gallery.beauty.ui.view.GalleryListener;
import org.androidbook.gallery.beauty.ui.view.ImageLookerSlideGallery;
import org.androidbook.gallery.beauty.ui.view.TouchableImageView;
import org.androidbook.gallery.constants.NetConstants;
import org.androidbook.gallery.db.FavoriteDB;
import org.androidbook.gallery.db.ThumbManager;
import org.androidbook.net.MainController;
import org.androidbook.net.WinHttpRequest;
import org.androidbook.netdata.ImageCacheManager;
import org.androidbook.utils.FileData;
import org.androidbook.utils.SQLUtils;

import android.app.AlertDialog;
import android.app.WallpaperManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;

/**
 * 
 * 功能描述 :图片查看器。（包含查看，放大缩小，发送，取消等） 作者: andrewlin<br>
 */
public class ImageLookerActivity extends BaseActivity implements OnClickListener {

	private static final String TAG = "ImageLooker2";
	/**
	 * Gallery的子控件是否需要拦截touch事件
	 */
	private static boolean mIsGalleryChildRequestTouch = false;
	private static BaseActivity mContext;

	/**
	 * 设置滑动画廊的子控件是否需要拦截touch事件
	 * 
	 * @param isRequestTouch
	 */
	public static void setGalleryChildRequestTouch(boolean isRequestTouch) {
		mIsGalleryChildRequestTouch = isRequestTouch;
	}

	/**
	 * 滑动画廊的子控件是否拦截touch事件
	 * 
	 * @return
	 */
	public static boolean IsGalleryChildRequestTouch() {
		return mIsGalleryChildRequestTouch;
	}

	public static final String SEND_PIC_PATH = "picPath";

	public static final String TYPE = "type";

	public static final String REV_UIN = "revUin";

	public static final String SELF_UIN = "selfUin";

	public static final String PATH = "path";

	public static final String INDEX = "index";

	public static final int RESULT_SEND = RESULT_FIRST_USER;

	public static final int RESULT_INSERT = RESULT_FIRST_USER + 1;

	public static final int RESULT_DOODLE = RESULT_FIRST_USER + 2;

	private static final long BAR_CONTROLS_TIMEOUT = 4000;

	private static final long REPEAT_OPTION_INTERVAL = 500;

	private static final byte MASK_UNDEFINE = 0x00;

	private static final byte MASK_BITMAP = 0x01;

	private static final byte MASK_GIF_DRAWABLE = 0x02;

	private static final byte MASK_UNLOAD = 0x00;

	private static final byte MASK_LOAD_SUCESS = 0x01;

	private static final byte MASK_LOAD_FAILED = 0x02;

	private static final int HIDE_BOTTOM_OPTIONS = 0x7654;

	private ImageLookerSlideGallery mGallery;

	private ImageGalleryAdapter mAdapter;

	protected GalleryListener mGListener;

	protected TouchableImageView mCurSelTouchableView;

	protected int mInitIndex;

	protected int mCurSelIndex;

	private LinearLayout bottomInfoLayout;

	private ImageButton img_turnLeftView;
	private ImageButton img_turnRightView;
	public static ThumbManager thumbManager;
	protected int type;

	protected long revUni;

	protected long selfUin;

	private int mLastOptionType;

	private long mLastOptionTime;

	protected HashMap<String, TouchableImageView> url2ViewMap = new HashMap<String, TouchableImageView>();

	protected HashMap<TouchableImageView, String> View2UrlMap = new HashMap<TouchableImageView, String>();
	private int imageId;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// 3.0 硬件加速用
		getWindow().setFlags(0x01000000, 0x01000000);
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
//		initParams(getIntent());

//		int size = getImageListCount();


		mCurSelIndex = thumbManager.getPosition(imageId);
		mInitIndex = mCurSelIndex;
		installView();
	}

	public void initButton() {
		findViewById(R.id.menu_toleft).setOnClickListener(this);
		findViewById(R.id.menu_toright).setOnClickListener(this);
		findViewById(R.id.menu_crop).setOnClickListener(this);
		findViewById(R.id.menu_fav).setOnClickListener(this);
		findViewById(R.id.menu_share).setOnClickListener(this);
	}
	
	public static void startActivity(Context context, int imageid, ThumbManager tm)
	{
		mContext = (BaseActivity) context;
		thumbManager = tm;
		Intent intent = new Intent(context, ImageLookerActivity.class);
		intent.putExtra("IMAGE_ID", imageid);
		context.startActivity(intent);
	}
	/**
	 * 初始化界面。
	 */
	private void installView() {


		img_turnLeftView = (ImageButton) findViewById(R.id.img_turnLeft);
		img_turnLeftView.setOnClickListener(this);

		img_turnRightView = (ImageButton) findViewById(R.id.img_turnRight);
		img_turnRightView.setOnClickListener(this);

		// switcher相关
		mGallery = (ImageLookerSlideGallery) findViewById(R.id.imagegallery);

		initGalleryListener();

		initImageLookerSlideGallery(mGallery, mGListener);

		/* 初始时，底部操作栏是不可见的 */
		bottomInfoLayout = (LinearLayout) findViewById(R.id.menuLayer);
		showBottomOptions(true);

		initButton();
	}

	/**
	 * 显示或隐藏底部的操作栏
	 * 
	 * @param bShow
	 *            是否要显示操作栏
	 */
	private void showBottomOptions(boolean bShow) {
		int nVisibility = bottomInfoLayout.getVisibility();

		if (bShow && nVisibility != View.VISIBLE) {
			bottomOptionsFadeInFadeOut(View.VISIBLE, 0.0f, 1.0f);
			handler.removeMessages(HIDE_BOTTOM_OPTIONS);
			handler.sendEmptyMessageDelayed(HIDE_BOTTOM_OPTIONS, BAR_CONTROLS_TIMEOUT);
		} else if (bShow && nVisibility == View.VISIBLE) {
			handler.removeMessages(HIDE_BOTTOM_OPTIONS);
			handler.sendEmptyMessageDelayed(HIDE_BOTTOM_OPTIONS, BAR_CONTROLS_TIMEOUT);
		} else if (!bShow && nVisibility == View.VISIBLE) {
			bottomOptionsFadeInFadeOut(View.INVISIBLE, 1.0f, 0.0f);
			handler.removeMessages(HIDE_BOTTOM_OPTIONS);
		}
	}

	/**
	 * 底部操作栏的淡入淡出
	 * 
	 * @param visibility
	 * @param startAlpha
	 * @param endAlpha
	 */
	private void bottomOptionsFadeInFadeOut(int visibility, float startAlpha, float endAlpha) {
		AlphaAnimation anim = new AlphaAnimation(startAlpha, endAlpha);
		anim.setDuration(300);
		bottomInfoLayout.startAnimation(anim);
		bottomInfoLayout.setVisibility(visibility);
	}

	@Override
	public void finish() {
		super.finish();
	}

	@Override
	public void onStart() {
		super.onStart();
	}

	@Override
	public void onStop() {
		super.onStop();
	}

	static final int GO_NEXT = 1;
	static final int STOP_NEXT = 2;

	private void stopAutoPlay() {
		// if(wakeLock !=null && wakeLock.isHeld())
		// {
		if (wakeLock != null) {
			wakeLock.release();
		}
		// }
		handlerAutoPlay.removeMessages(GO_NEXT);
	}

	private PowerManager.WakeLock wakeLock;

	private void startAutoPlay() {
		if (wakeLock == null) {
			wakeLock = BeautyApplication.instance.pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "photoshow");
		}
		// if(!wakeLock.isHeld())
		// {
		wakeLock.acquire();
		// }
		handlerAutoPlay.sendEmptyMessageDelayed(GO_NEXT, (switchTime + 2) * 1000);
	}

	private Handler handlerAutoPlay = new Handler() {
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case GO_NEXT:

				startAutoPlay();
				mGallery.switchToNext();
				break;
			}
		}
	};

	@Override
	protected void onPause() {
		if (isautoplay) {
			isautoplay = false;
			stopAutoPlay();
		}
		super.onPause();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		getMenuInflater().inflate(R.menu.show_image_menu, menu);
		return super.onCreateOptionsMenu(menu);
	}

	private int switchTime;

	private void buildTimeChooser(final MenuItem item) {
		LayoutInflater factory = LayoutInflater.from(this);
		final View fontDialogView = factory.inflate(R.layout.dialog_seek, null);
		final AlertDialog seekDialog = new AlertDialog.Builder(this).setView(fontDialogView)
				.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {

					public void onClick(DialogInterface dialog, int whichButton) {

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
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_autoplay:
			if (isautoplay) {
				isautoplay = false;
				stopAutoPlay();
			} else {
				buildTimeChooser(item);
			}
			break;
		case R.id.menu_setwall: {
			try {
				
				Bitmap bitmap = mCurSelTouchableView.getBitmap();
				WallpaperManager.getInstance(ImageLookerActivity.this).setBitmap(bitmap);
			} catch (IOException e) {
				showToast(R.string.set_wall_paper_failed);
				break;
			}
			showToast(R.string.set_wall_paper_success);
		}
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onClick(View v) {
		int id = v.getId();
		isautoplay = false;
		stopAutoPlay();
		long curTime = System.currentTimeMillis();
		if (id == mLastOptionType && java.lang.Math.abs(curTime - mLastOptionTime) < REPEAT_OPTION_INTERVAL) {
			mLastOptionTime = curTime;
			return;
		}
		handler.removeMessages(HIDE_BOTTOM_OPTIONS);
		handler.sendEmptyMessageDelayed(HIDE_BOTTOM_OPTIONS, BAR_CONTROLS_TIMEOUT);
		mLastOptionType = id;
		mLastOptionTime = curTime;
		switch (id) {
		case R.id.img_turnLeft: {
			// mAdapter.setCurrentDirection(currentDirect);
			if (mCurSelTouchableView != null) {
				mCurSelTouchableView.rotateLeft();
			}
			// handler.removeMessages(HIDE_BOTTOM_OPTIONS);
			// handler.sendEmptyMessageDelayed(HIDE_BOTTOM_OPTIONS,
			// BAR_CONTROLS_TIMEOUT);
		}
			break;
		case R.id.img_turnRight: {
			// mAdapter.setCurrentDirection(currentDirect);
			if (mCurSelTouchableView != null) {
				mCurSelTouchableView.rotateRight();
			}

		}
			break;
		case R.id.touch_img: {
			if (bottomInfoLayout.getVisibility() != View.VISIBLE) {
				showBottomOptions(true);
			}
		}
			break;
		case R.id.menu_toleft: {
			mGallery.switchToPrev();
			// if (mCurSelIndex == 0) {
			// findViewById(R.id.menu_toleft).setVisibility(View.GONE);
			// }
			// findViewById(R.id.menu_toright).setVisibility(View.VISIBLE);
		}
			break;
		case R.id.menu_toright: {
			mGallery.switchToNext();
			// if (mCurSelIndex == mImgPathList.size() - 1) {
			// findViewById(R.id.menu_toright).setVisibility(View.GONE);
			// }
			// findViewById(R.id.menu_toleft).setVisibility(View.VISIBLE);
		}
			break;
		case R.id.menu_share: {
			Bitmap bitmap = mCurSelTouchableView.getBitmap();
			if (bitmap != null) {
				shareBitmap(ImageLookerActivity.this, bitmap);
			}
		}
			break;
		case R.id.menu_fav: {
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
						FavoriteDB.getInstance().addImage(mCurSelTouchableView.getBitmap(), ((ShowThumbActivity) mContext).winImageDb);
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

		}
			break;
		case R.id.menu_crop: {
			
			Bitmap bitmap = mCurSelTouchableView.getBitmap();
			if (bitmap != null) {
				CropImageActivity.launchCropperOrFinish(ImageLookerActivity.this, bitmap);
			} else {

			}
		}
			break;

		}
	}

	public static boolean shareBitmap(Context context, Bitmap bitmap) {
		String path = FileData.getDataPath() + "share.jpg";
		byte[] data = FileData.serializeBitmap(bitmap);
		FileData.writeDataToNewFile(path, data);
		Intent sharei = new Intent(Intent.ACTION_SEND);
		sharei.setType("image/*");
		File file = new File(path);
		if (file.exists()) {
			Uri uri = Uri.fromFile(new File(path));
			sharei.putExtra(Intent.EXTRA_STREAM, uri);
			sharei.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			Intent dd = Intent.createChooser(sharei, context.getString(R.string.share));

			if (dd != null) {
				context.startActivity(dd);
				return true;
			} else {
				return false;
			}
		} else {
			// Toast.makeText(context, "收藏失败", Toast.LENGTH_LONG).show();
			return false;
		}
	}



	
	private boolean isautoplay;



//	/**
//	 * 初始化参数
//	 * 
//	 * @param intent
//	 */
//	protected void initParams(Intent intent) {
//		if (intent != null) {
//			type = intent.getExtras().getInt(TYPE);
//			mImgPathList = intent.getStringArrayListExtra(PATH);
//			mInitIndex = intent.getIntExtra(INDEX, 0);
//			revUni = intent.getLongExtra(REV_UIN, 0);
//			selfUin = intent.getLongExtra(SELF_UIN, 0);
//		}
//
//		if (mImgPathList == null || mInitIndex < 0 || mInitIndex >= mImgPathList.size()) {
//			throw new IllegalArgumentException("mImgPathList = " + mImgPathList + ", mInitIndex = " + mInitIndex);
//		}
//	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		MenuItem item = menu.findItem(R.id.menu_autoplay);
		if (isautoplay) {
			item.setTitle(R.string.stopplay);
			item.setIcon(R.drawable.stop_play);

		} else {
			item.setTitle(R.string.autoplay);
			item.setIcon(R.drawable.autoplay);
		}
		return super.onPrepareOptionsMenu(menu);
	}

	public static void startActivity(Context context, ArrayList<String> picList, int position) {
		Intent intent = new Intent(context, ImageLookerActivity.class);
		intent.putStringArrayListExtra(PATH, picList);
		intent.putExtra(INDEX, position);
		context.startActivity(intent);
		mContext = (BaseActivity) context;

	}



	/**
	 * 初始化GalleryListener
	 * 
	 * @param listener
	 */
	protected void initGalleryListener() {
		mGListener = new GalleryListener() {

			@Override
			public void onOverScrolled(boolean isFrontOverScrolled) {
				// TODO Auto-generated method stub

			}

			@Override
			public void onItemSelected(View parentView, View selectedView, int position) {

				if (selectedView != null) {
					mCurSelTouchableView = (TouchableImageView) selectedView.findViewById(R.id.touch_img);
				} else {
					mCurSelTouchableView = null;
				}

				mCurSelIndex = position;
				Log.v(TAG, "mCurselindex:" + mCurSelIndex);
				
				

				if (mCurSelIndex == 0) {
					findViewById(R.id.menu_toleft).setVisibility(View.GONE);
				} else {
					findViewById(R.id.menu_toleft).setVisibility(View.VISIBLE);

				}

				if (mCurSelIndex == thumbManager.getImageCount() - 1) {
					findViewById(R.id.menu_toright).setVisibility(View.GONE);
					if (isautoplay) {
						isautoplay = false;
						stopAutoPlay();
					}
				} else {
					findViewById(R.id.menu_toright).setVisibility(View.VISIBLE);
				}
			}
		};
	}

	/**
	 * 初始化Gallery
	 * 
	 * @param gallery
	 * @param listener
	 */
	protected void initImageLookerSlideGallery(final ImageLookerSlideGallery gallery, GalleryListener listener) {

		ImageGalleryAdapterListener adapterListener = new ImageGalleryAdapterListener() {

			@Override
			public OnClickListener getAdvancedViewClickListener() {
				return ImageLookerActivity.this;
			}

			@Override
			public ImageView getCurSelAdvancedView() {
				return mCurSelTouchableView;
			}

			@Override
			public void notifyLoadFinished(int position, int state) {
				gallery.requestLayout();
			}

			@Override
			public void notifyLoadImgStart(int position) {
			}
		};

		gallery.setResistance(true, true);

		Display display = ImageLookerActivity.this.getWindowManager().getDefaultDisplay();
		int nMaxW = display.getWidth() * 2;
		int nMaxH = display.getHeight() * 2;

		mAdapter = new ImageGalleryAdapter(this, nMaxW, nMaxH, adapterListener);

		
		gallery.setGalleryListener(listener);
		gallery.setAdapter(mAdapter);
		gallery.setSelection(mInitIndex);
	}

	/**
	 * 通知加载异常
	 * 
	 * @param state
	 */
	protected void notifyLoadException(int state) {

	}

	@Override
	protected void onDestroy() {
		if (mGallery != null) {
			mGallery.setAdapter(null);
		}
		url2ViewMap.clear();
		View2UrlMap.clear();
		// if (mImgPathList != null) {
		// for (int i = 0; i < mImgPathList.size(); i++) {
		// String url = mImgPathList.get(i);
		// // Bitmap bitmap =
		// // MainLogicController.Icon.getIconInMemory(url);
		// // if (bitmap != null && !bitmap.isRecycled()) {
		// // MainLogicController.Icon.remove(url);
		// // bitmap.recycle();
		// // }
		// }
		// }
		mContext = null;
		super.onDestroy();
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
	}

	/**
	 * 为了实现异步。而且是更新UI所以需要用Handler执行插入图片动作。
	 */
	private Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			int what = msg.what;
			switch (what) {
			case HIDE_BOTTOM_OPTIONS: /* 隐藏底部操作栏 */
				showBottomOptions(false);
				break;
			default:
				break;
			}
		}
	};

	/**
	 * 图片连续查看的适配器
	 * 
	 * @author lilyxie
	 * 
	 */
	public class ImageGalleryAdapter extends BaseAdapter {

		private Context mContext;

	

		private int maxW, maxH;

		private ImageGalleryAdapterListener mListener;

		public ImageGalleryAdapter(Context context, int maxW, int maxH, ImageGalleryAdapterListener listener) {

			mContext = context;

			this.maxW = maxW;
			this.maxH = maxH;

			this.mListener = listener;
		}


		private Bitmap getSaveBitmap(int position)
		{
			byte[] bitmapdata = SQLUtils.getPic(thumbManager.getImageId(position), thumbManager.getDb());
			return getSafeBitmap(bitmapdata);
		}
		private static final int MAX_BITMAP_SIZE = 1200;
		public Bitmap getSafeBitmap(byte[] data)
		{

			Bitmap bitmap = null;
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
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			Log.v(TAG, "position:" + position);
			if (convertView == null) {
				convertView = LayoutInflater.from(mContext).inflate(R.layout.image_gallery_item, null);
			}
			TouchableImageView imgView = (TouchableImageView) convertView.findViewById(R.id.touch_img);
			if (mListener != null) {
				imgView.setOnClickListener(mListener.getAdvancedViewClickListener());
			}
		
			Bitmap bitmap = getSaveBitmap(position);
			if (bitmap != null) {
				imgView.setBitmap(bitmap);
				imgView.setTag(R.id.TAG_VIEW_FLAG, 1);
				// if (position == mCurSelIndex) {
				// hideProgressBar();
				// }
			} 
			return convertView;
		}

		private void rotateView(TouchableImageView imgView, int diff) {
			// 记录上次旋转的值

			imgView.rotateLeft();

		}

		@Override
		public int getCount() {
			if (thumbManager != null) {
				return thumbManager.getImageCount();
			} else {
				return 0;
			}
		}

		@Override
		public Object getItem(int position) {
			return null;
		}

		@Override
		public long getItemId(int position) {
			return thumbManager.getImageId(position);
		}
	}



	/**
	 * 
	 * @author lilyxie
	 * 
	 */
	public interface ImageGalleryAdapterListener {

		/**
		 * 通知加载图片开始
		 * 
		 * @param position
		 */
		public void notifyLoadImgStart(int position);

		/**
		 * 通知加载图片结束
		 * 
		 * @param position
		 * @param state
		 */
		public void notifyLoadFinished(int position, int state);

		/**
		 * 获取当前选中的View
		 * 
		 * @return
		 */
		public ImageView getCurSelAdvancedView();

		/**
		 * 获取AdvanceView的View.OnClickListener
		 * 
		 * @return
		 */
		public View.OnClickListener getAdvancedViewClickListener();
	}
}
