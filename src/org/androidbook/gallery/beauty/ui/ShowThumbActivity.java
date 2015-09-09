package org.androidbook.gallery.beauty.ui;

import org.androidbook.gallery.beauty.R;
import org.androidbook.gallery.db.ThumbManager;
import org.androidbook.netdata.xml.data.WinImageDb;
import org.androidbook.utils.FileData;
import org.androidbook.utils.Util;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.LayoutAnimationController;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

import com.admogo.AdMogoLayout;

public class ShowThumbActivity extends BaseActivity
{

	protected static final String TAG = "ShowThumbActivity";
	public static ThumbManager thumbManager;
	protected ProgressDialog progressDialog;

	public static WinImageDb winImageDb;
	protected ThumbAdapter ta;
	protected GridView gv;
	private com.admogo.AdMogoLayout adm;
	protected TextView pointView;
	
	public static void startActivity(WinImageDb db, Context context)
	{
		winImageDb = db;
		String path = FileData.getDataPath() + db.name;
		thumbManager = new ThumbManager();
		if (thumbManager.openDb(path))
		{
			Intent intent = new Intent(context, ShowThumbActivity.class);
			intent.putExtra("dbid", db.id);
			context.startActivity(intent);
		} else
		{
			thumbManager = null;
			BaseActivity.showToast(R.string.file_corrupt);
			db.installStatus = WinImageDb.UNEXISTED;
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		if (thumbManager == null)
		{
			finish();
			return;
		}
		setContentView(R.layout.thumb_show);
		gv = (GridView) findViewById(R.id.thumb_grid);
		ta = new ThumbAdapter();
		//动画
		int center = getResources().getDimensionPixelSize(R.dimen.grid_item_size);
		Animation ani = Util.getRanAnimation(center/2, null);
		LayoutAnimationController lac = new LayoutAnimationController(ani);
		lac.setOrder(LayoutAnimationController.ORDER_RANDOM);
		lac.setDelay(0.5f);
		lac.setInterpolator(new AccelerateInterpolator());
		gv.setLayoutAnimation(lac);
		pointView = (TextView) findViewById(R.id.need_points);
//		Log.v(TAG, "winImageDb.value"+winImageDb.value);
		checkValue();
		thumbManager.checkThumb(handle);

		progressDialog = new ProgressDialog(this);
		progressDialog.setCancelable(false);
		progressDialog.setMax(100);
		progressDialog.setMessage(getString(R.string.load_thumb));
		progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		progressDialog.show();

		gv.setOnItemClickListener(new OnItemClickListener()
		{

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id)
			{
				if( checkJF())
				{
					ImageLookerActivity.startActivity(ShowThumbActivity.this, (int) id, thumbManager);
				}
				else
				{
					showJFDialog();
				}

			}
		});
		ad();
		if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE)
		{
			setColsForScreen();
			adm.setVisibility(View.GONE);
		}

	}

	/**
	 * 显示是否要积分
	 */
	protected void checkValue()
	{
//		return true;
//		if( winImageDb.value != 0)
//		{
//			String s = getString(R.string.photo_db_point);
//			s = s.replace("TOTAL_POINT", String.valueOf( AppOffersManager.getPoints(this))).replace("POINT", String.valueOf(winImageDb.value));
//			pointView.setText(s);
//		}
//		else
//		{
			pointView.setVisibility(View.GONE);
//		}
	}
	
	protected void showJFDialog()
	{
//		AlertDialog.Builder adb =new Builder(this);
//		adb.setTitle(R.string.not_enought_point);
//		String s = getString(R.string.not_enought_point_text);
//		s = s.replace("POINT", String.valueOf(AppOffersManager.getPoints(this)));
//		adb.setMessage(s);
//		adb.setPositiveButton(R.string.earn_point, new OnClickListener()
//		{
//			
//			@Override
//			public void onClick(DialogInterface dialog, int which)
//			{
//				AppOffersManager.showAppOffers(ShowThumbActivity.this);
//				
//			}
//		});
//		adb.setNegativeButton(R.string.cancel, null);
//		adb.create().show();
	}

	/**
	 * 检查积分墙的积分
	 * 
	 * @return
	 */
	protected boolean checkJF()
	{
		return true;
//		int spendedPoint = LocalSqlDb.getInstance().getMountPoint();
//		if( spendedPoint > LocalSqlDb.MAX_POINTS)
//			//超过积分上限
//		{
//			return true;
//		}
//		
//		if (winImageDb != null && winImageDb.value > 0)
//		{
//		
//			boolean canopen = AppOffersManager.spendPoints(this, winImageDb.value);
//			if (canopen)
//			{
//				//增加累计积分
//				LocalSqlDb.getInstance().addMountPoint(winImageDb.value);
//				winImageDb.value = 0;
//			
//				AvailableDB.getInstance().spendPoints(winImageDb);
//				String s = getString(R.string.spendpoint);
//
//				s = s.replace("MOUNT_POINT", String.valueOf(LocalSqlDb.getInstance().getMountPoint())).replace("TOTAL_POINT", String.valueOf( AppOffersManager.getPoints(this))).replace("POINT", String.valueOf(winImageDb.value));
//				showToast(s);
//				
//				
//				pointView.setText("");
//				pointView.setVisibility(View.INVISIBLE);
//				return true;
//			} else
//			{
//				return false;
//			}
//		} else
//		{
//			return true;
//		}
	}

	private void ad()
	{
		adm = (AdMogoLayout) findViewById(R.id.admogo_layout);
	}

	private Handler handle = new Handler()
	{
		public void handleMessage(Message msg)
		{
			switch (msg.what)
			{
			case ThumbManager.DECODING:
				if (progressDialog.isShowing())
				{
					progressDialog.setProgress((int) (msg.arg1 * 100.0 / msg.arg2));
				}
				break;
			case ThumbManager.DECODe_FINISHED:
				if (progressDialog.isShowing())
				{
					progressDialog.dismiss();
				}
				gv.setAdapter(ta);
				ta.notifyDataSetChanged();
				if (ta.getCount() == 0)
				{
					showToast(R.string.no_fav);
				}
				adm.setVisibility(View.VISIBLE);
				onDecodeFinish();
				break;
			case ThumbManager.DECODE_FAILED:
				if (progressDialog.isShowing())
				{
					progressDialog.dismiss();
				}
				showToast(R.string.file_corrupt);
				finish();
				break;
			}
		}
	};

	protected void onDecodeFinish()
	{
		
	}

	@Override
	protected void onResume()
	{
		super.onResume();
		checkValue();
	}

	@Override
	protected void onPause()
	{
		// TODO Auto-generated method stub
		super.onPause();
	}

	@Override
	protected void onDestroy()
	{
		if (thumbManager != null)
		{
			thumbManager.destroy();
			thumbManager = null;
		}
		gv.setAdapter(null);
		ta = null;
		super.onDestroy();
	}

	class ThumbAdapter extends BaseAdapter
	{

		@Override
		public int getCount()
		{
			return thumbManager.getImageCount();
		}

		@Override
		public Object getItem(int position)
		{
			return thumbManager.getImage(position);
		}

		@Override
		public long getItemId(int position)
		{
			return thumbManager.getImageId(position);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent)
		{
			View view = convertView;
			if (view == null)
			{
				view = getLayoutInflater().inflate(R.layout.thumb_grid_item, null);
			}
			ImageView iv = (ImageView) view.findViewById(R.id.thumb);
			iv.setImageBitmap((Bitmap) getItem(position));
			return view;
		}

	}

	@Override
	public void onConfigurationChanged(Configuration newConfig)
	{
		super.onConfigurationChanged(newConfig);
		setColsForScreen();
		if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE)
		{
			adm.setVisibility(View.GONE);
		} else
		{
			adm.setVisibility(View.VISIBLE);
		}
	}

	private void setColsForScreen()
	{
		requestUpdateScreen();
		int cols = screenWidth / (getResources().getDimensionPixelSize(R.dimen.grid_item_size) + 10);
		gv.setNumColumns(gv.AUTO_FIT);
		
	}
}
