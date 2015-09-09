package org.androidbook.gallery.beauty.ui;

import java.io.File;
import java.util.ArrayList;

import org.androidbook.gallery.beauty.BeautyService;
import org.androidbook.gallery.beauty.R;
import org.androidbook.gallery.beauty.ui.view.Gallery3D;
import org.androidbook.gallery.db.AvailableDB;
import org.androidbook.gallery.db.FavoriteDB;
import org.androidbook.gallery.db.ThumbManager;
import org.androidbook.netdata.DownloadManager;
import org.androidbook.netdata.ImageCacheManager;
import org.androidbook.netdata.xml.data.WinImageDb;
import org.androidbook.utils.FileData;
import org.androidbook.utils.Util;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.ListView;
import android.widget.TextView;

import com.admogo.AdMogoManager;
import com.tencent.mobwin.AdView;

public class ImageDBListActivity extends BaseActivity
{
	public final static int DEFAULT_HEADER_PIC = 8;
	private ParseReceiver pr;
	public static final String TAG = "ImageDBListActivity";
	ThumbManager thumbManager = new ThumbManager();

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.imagedb_layout);
		lv = (ListView) findViewById(R.id.listView_imagedb);
		adapter = new ImageDbAdapter();
		headerView = getLayoutInflater().inflate(R.layout.list_header_gallery, null);
		g3d = (Gallery3D) headerView.findViewById(R.id.header_galllery);
		g3d.setUnselectedAlpha(0.5f);
		g3d.setGravity(Gravity.CENTER);
		g3d.setSpacing(-20);
		BlankGalleryAdapter bga = new BlankGalleryAdapter();
		g3d.setAdapter(bga);
		lv.addHeaderView(headerView);
		lv.setAdapter(adapter);
		lv.setOnItemClickListener(itemClick);
		pr = new ParseReceiver();
		IntentFilter ift = new IntentFilter();
		ift.addAction(AvailableDB.PARSE_LOCAL_FINISH);
		ift.addAction(AvailableDB.COPY_FILE_FINISH);
		ift.addAction(AvailableDB.PARSE_NET_FINISH);
		ift.addAction(AvailableDB.NET_ERROR);
		ift.addAction(DownloadManager.DOWNLOAD_STATUS_UPDATE);
		registerReceiver(pr, ift);
		Log.d(TAG, thumbManager + "");
		Log.d(TAG, AvailableDB.getInstance() + "" + AvailableDB.getInstance().getHeader());
		if (thumbManager.openDb(FileData.getDataPath() + AvailableDB.getInstance().getHeader().name))
		{
			thumbManager.checkThumb(handlerDecode);
		}
		g3d.setOnItemClickListener(new OnItemClickListener()
		{

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id)
			{
				if (galleryAdatper != null)
				{
					ImageLookerActivity.startActivity(ImageDBListActivity.this, (int) galleryAdatper.getItemId(position), thumbManager);
				} else
				{
					showToast(R.string.please_wait);
				}

			}
		});
		// View emptyView = getLayoutInflater().inflate(R.layout.blank_view,
		// null);
		// lv.setEmptyView(emptyView);
		addFooter();

		adview = (AdView) findViewById(R.id.mobwinview);
		if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE)
		{
			adview.setVisibility(View.GONE);
		}

	}

	private void addHeader()
	{

		galleryAdatper = new GalleryAdapter();
		g3d.setAdapter(galleryAdatper);
		galleryAdatper.notifyDataSetChanged();
		g3d.setSelection(Integer.MAX_VALUE / 4);
	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event)
	{
		if (keyCode == KeyEvent.KEYCODE_BACK)
		{
			if (BeautyService.service.downloadManager.getTaskCount() != 0)
			{
				AlertDialog.Builder builder = new Builder(this);
				builder.setMessage(R.string.have_downloading_task);
				builder.setPositiveButton(R.string.force_quit, new DialogInterface.OnClickListener()
				{

					@Override
					public void onClick(DialogInterface dialog, int which)
					{
						BeautyService.service.downloadManager.cancelAll();
						finish();
					}
				});
				builder.setNeutralButton(R.string.quit_desktop, new DialogInterface.OnClickListener()
				{

					@Override
					public void onClick(DialogInterface dialog, int which)
					{
						moveTaskToBack(false);

					}
				});
				builder.setNegativeButton(R.string.cancel, null);
				builder.create().show();
				return true;
			}

		}

		return super.onKeyUp(keyCode, event);

	}

	@Override
	public void onConfigurationChanged(Configuration newConfig)
	{
		super.onConfigurationChanged(newConfig);
		if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE)
		{
			adview.setVisibility(View.GONE);
		} else
		{
			adview.setVisibility(View.VISIBLE);
		}
	}

	private void addFooter()
	{
		if (adapter.getCount() == 0)
		{
			if (lv.getFooterViewsCount() == 0)
			{
				footview = new TextView(this);
				footview.setText(R.string.net_error_retry);
				lv.addFooterView(footview);
			}
		} else
		{
			if (lv.getFooterViewsCount() > 0)
			{
				lv.removeFooterView(footview);
			}
		}
	}

	class BlankGalleryAdapter extends BaseAdapter
	{

		Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.default_gallery_bg);

		@Override
		public int getCount()
		{
			// TODO Auto-generated method stub
			return Integer.MAX_VALUE / 2;
		}

		@Override
		public Object getItem(int position)
		{
			// TODO Auto-generated method stub
			return bitmap;
		}

		@Override
		public long getItemId(int position)
		{
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent)
		{
			View view = convertView;
			if (view == null)
			{
				ImageView imageView = new ImageView(ImageDBListActivity.this);
				imageView.setImageBitmap((Bitmap) getItem(position));
				Gallery3D.LayoutParams lp = new Gallery3D.LayoutParams(Util.getPxFromDp(Gallery3D.HG_W, ImageDBListActivity.this), Util.getPxFromDp(Gallery3D.HG_H, ImageDBListActivity.this));
				imageView.setLayoutParams(lp);
				imageView.setScaleType(ScaleType.FIT_XY);
				imageView.setBackgroundResource(R.drawable.stack_frame_gold);
				return imageView;
			}
			return view;
		}
	}

	class GalleryAdapter extends BaseAdapter
	{

		@Override
		public int getCount()
		{
			// TODO Auto-generated method stub
			return Integer.MAX_VALUE / 2;
		}

		@Override
		public Object getItem(int position)
		{
			// TODO Auto-generated method stub
			return thumbManager.getImage(position % thumbManager.getImageCount());
		}

		@Override
		public long getItemId(int position)
		{
			// TODO Auto-generated method stub
			return thumbManager.getImageId(position % thumbManager.getImageCount());
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent)
		{
			View view = convertView;
			if (view == null)
			{
				ImageView imageView = new ImageView(ImageDBListActivity.this);
				imageView.setImageBitmap((Bitmap) getItem(position));
				imageView.setBackgroundResource(R.drawable.stack_frame_gold);
				imageView.setScaleType(ScaleType.FIT_XY);
				Gallery3D.LayoutParams lp = new Gallery3D.LayoutParams(Util.getPxFromDp(Gallery3D.HG_W, ImageDBListActivity.this), Util.getPxFromDp(Gallery3D.HG_H, ImageDBListActivity.this));
				imageView.setLayoutParams(lp);
				view = imageView;
			} else
			{
				ImageView imageView = (ImageView) view;
				imageView.setImageBitmap((Bitmap) getItem(position));
			}
			return view;
		}

	}

	class ParseReceiver extends BroadcastReceiver
	{

		@Override
		public void onReceive(Context context, Intent intent)
		{
			// Log.v(TAG, "onReceive:" + intent.getAction());
			if (AvailableDB.PARSE_LOCAL_FINISH.equalsIgnoreCase(intent.getAction()) || AvailableDB.PARSE_NET_FINISH.equalsIgnoreCase(intent.getAction()) )
			{
				adapter.setImageDbList(AvailableDB.getInstance().getImageDbList());
				adapter.notifyDataSetChanged();

			} else if (AvailableDB.COPY_FILE_FINISH.equalsIgnoreCase(intent.getAction()))
			{
				if (!thumbManager.isStarting() && thumbManager.openDb(FileData.getDataPath() + AvailableDB.getInstance().getHeader().name))
				{
					thumbManager.checkThumb(handlerDecode);
				}
				adapter.setImageDbList(AvailableDB.getInstance().getImageDbList());
				adapter.notifyDataSetChanged();
			} else if (DownloadManager.DOWNLOAD_STATUS_UPDATE.equalsIgnoreCase(intent.getAction()))
			{
				
				adapter.notifyDataSetChanged();

			} else if (AvailableDB.NET_ERROR.equalsIgnoreCase(intent.getAction()))
			{
				showToast(R.string.net_error_retry);
			}
			addFooter();
		}

	}

	class ImageDbAdapter extends BaseAdapter
	{

		private ArrayList<WinImageDb> imageDbList = (ArrayList<WinImageDb>) AvailableDB.getInstance().getImageDbList();

		@Override
		public int getCount()
		{

			int count = imageDbList.size();
			return count;

		}

		@Override
		public Object getItem(int position)
		{

			return imageDbList.get(position);
		}

		@Override
		public long getItemId(int position)
		{
			return imageDbList.get(position).id;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent)
		{
			View view = convertView;
			if (view == null)
			{
				view = getLayoutInflater().inflate(R.layout.imagedb_item, null);
			}

			WinImageDb imageDb = (WinImageDb) getItem(position);
			ImageView thumb = (ImageView) view.findViewById(R.id.thumb_pic);
			Bitmap bitmap = ImageCacheManager.getInstance().get(imageDb.getThumbUrl(), handlerIcon);
			if (bitmap == null)
			{
				thumb.setImageResource(R.drawable.default_gallery_bg);
			} else
			{
				thumb.setImageBitmap(bitmap);
			}
			TextView des = (TextView) view.findViewById(R.id.discrption);
			des.setText(imageDb.discription);
			TextView size = (TextView) view.findViewById(R.id.text_size);
			size.setText(FileData.byteToString(imageDb.size));
			TextView bt = (TextView) view.findViewById(R.id.download_bt);
			bt.setTag(imageDb);
			TextView category = (TextView) view.findViewById(R.id.text_category);
			category.setText(imageDb.category);
			view.setTag(imageDb);
			switch (imageDb.installStatus)
			{
			case WinImageDb.INSTALLED:
				bt.setText(R.string.delete);
				bt.setOnClickListener(clickToDelete);
				bt.setEnabled(true);
				bt.setTextColor(0xffffffff);
				break;
			case WinImageDb.DOWNLOADING:
				bt.setText(R.string.download_cancel);
				bt.setOnClickListener(clicktToCancel);
				bt.setEnabled(true);
				bt.setTextColor(0xffffffff);
				break;
			case WinImageDb.UNEXISTED:
				bt.setText(R.string.download);
				bt.setOnClickListener(clickToDownload);
				bt.setEnabled(true);
				bt.setTextColor(0xffffffff);
				break;
			case WinImageDb.DOWNLOAD_CANCELING:
				bt.setText(R.string.download_cancel);
				bt.setOnClickListener(null);
				bt.setEnabled(false);
				bt.setTextColor(0xff888888);
				break;
			}
			return view;
		}

		public ArrayList<WinImageDb> getImageDbList()
		{
			return imageDbList;
		}

		public void setImageDbList(ArrayList<WinImageDb> imageDbList)
		{
			this.imageDbList = imageDbList;
		}
	}

	@Override
	protected void onResume()
	{
		super.onResume();
		if (adapter != null)
		{
			adapter.notifyDataSetChanged();
		}
	}

	private OnItemClickListener itemClick = new OnItemClickListener()
	{

		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position, long id)
		{
			final WinImageDb db = (WinImageDb) view.getTag();
			if( db == null)
			{
				return;
			}
			if (db.installStatus == WinImageDb.INSTALLED)
			{
				ShowThumbActivity.startActivity(db, ImageDBListActivity.this);
			} else if (db.installStatus == WinImageDb.DOWNLOADING)
			{
				showToast(R.string.wait_downloading);
			} else
			{
				AlertDialog.Builder adb = new AlertDialog.Builder(ImageDBListActivity.this);
				adb.setMessage(R.string.pics_not_download_yet);
				adb.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener()
				{

					@Override
					public void onClick(DialogInterface dialog, int which)
					{
						dialog.dismiss();
					}
				});
				adb.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener()
				{

					@Override
					public void onClick(DialogInterface dialog, int which)
					{
						dialog.dismiss();
						startDownload(db);
					}
				});
				adb.create().show();
			}

		}
	};

	@Override
	protected void onDestroy()
	{
		thumbManager.destroy();
		unregisterReceiver(pr);
		Intent intent = new Intent(this, BeautyService.class);
		stopService(intent);
		AvailableDB.destory();
		FavoriteDB.getInstance().destroy();
		AdMogoManager.clear();
		super.onDestroy();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		getMenuInflater().inflate(R.menu.category_menu, menu);
		getMenuInflater().inflate(R.menu.imagedb_menu, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch (item.getItemId())
		{
		case R.id.menu_my_fav:

			ShowFavThumbActivity.startActivity(FavoriteDB.getInstance().getFavWinImageDb(), this);
			break;
		case R.id.menu_download_all:
			for (int i = 0; i < adapter.getCount(); i++)
			{
				WinImageDb db = (WinImageDb) adapter.getItem(i);
				if (db.installStatus == WinImageDb.UNEXISTED)
				{
					startDownload(db);
				}
			}
			break;
		case R.id.menu_filter:
		{
			showFilter();
		}
			break;
		}

		return super.onOptionsItemSelected(item);
	}

	private void showFilter()
	{
		ArrayList<String> favArrayList = new ArrayList<String>();
		favArrayList.add(getString(R.string.unread));
		favArrayList.add(getString(R.string.already_read));
		favArrayList.add(getString(R.string.all_read));
		AlertDialog.Builder ab = new AlertDialog.Builder(this);
		ab.setItems(favArrayList.toArray(new String[] {}), fliterLisener);
		ab.setTitle(R.string.fliter);
		ab.create().show();
	}

	private DialogInterface.OnClickListener fliterLisener = new DialogInterface.OnClickListener()
	{

		@Override
		public void onClick(DialogInterface dialog, int which)
		{
			ArrayList<WinImageDb> alllist = AvailableDB.getInstance().getImageDbList();
			switch (which)
			{
			case 0:
			{
				ArrayList<WinImageDb> unreadlist = new ArrayList<WinImageDb>();
				
				int size = AvailableDB.getInstance().getImageDbList().size();
				for (int i = 0; i < size; i++)
				{
					if (alllist.get(i).installStatus != WinImageDb.INSTALLED)
					{
						unreadlist.add(alllist.get(i));
					}
				}
				adapter.setImageDbList(unreadlist);
				adapter.notifyDataSetChanged();
			}
				break;
			case 1:
			{
				ArrayList<WinImageDb> readlist = new ArrayList<WinImageDb>();
				int size = alllist.size();
				for (int i = 0; i < size; i++)
				{
					if (alllist.get(i).installStatus ==  WinImageDb.INSTALLED)
					{
						readlist.add(alllist.get(i));
					}
				}
				adapter.setImageDbList(readlist);
				adapter.notifyDataSetChanged();
			}
				break;
			case 2:
			{
				adapter.setImageDbList(alllist);
				adapter.notifyDataSetChanged();
			}
				break;
			}
			dialog.dismiss();
		}
	};

	/**
	 * 删除
	 */
	private OnClickListener clickToDelete = new OnClickListener()
	{

		@Override
		public void onClick(View v)
		{
			final WinImageDb imageDb = (WinImageDb) v.getTag();
			AlertDialog.Builder adb = new Builder(ImageDBListActivity.this);
			adb.setMessage(getString(R.string.sure_to_delete) + "\"" + imageDb.discription + "\"");
			adb.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener()
			{

				@Override
				public void onClick(DialogInterface dialog, int which)
				{
					String dataPath = FileData.getDataPath();
					dataPath = dataPath + "/" + imageDb.name;
					File file = new File(dataPath);
					if (file.exists())
					{
						file.delete();
					}
					imageDb.installStatus = WinImageDb.UNEXISTED;
					adapter.notifyDataSetChanged();

				}
			});
			adb.setNegativeButton(R.string.cancel, null);
			adb.create().show();

		}
	};

	private OnClickListener clickToDownload = new OnClickListener()
	{

		@Override
		public void onClick(View v)
		{

			WinImageDb imageDb = (WinImageDb) v.getTag();
			startDownload(imageDb);
		}
	};

	private OnClickListener clicktToCancel = new OnClickListener()
	{

		@Override
		public void onClick(View v)
		{
			final WinImageDb imageDb = (WinImageDb) v.getTag();

			AlertDialog.Builder al = new Builder(ImageDBListActivity.this).setMessage(R.string.download_cancel_sure).setPositiveButton(R.string.ok, new DialogInterface.OnClickListener()
			{

				@Override
				public void onClick(DialogInterface dialog, int which)
				{
					cancelDownload(imageDb);
				}
			}).setNegativeButton(R.string.cancel, null);
			al.create().show();

		}
	};

	private void cancelDownload(WinImageDb db)
	{
		BeautyService.service.downloadManager.cancelDownload(db);

		adapter.notifyDataSetChanged();
	}

	private void startDownload(WinImageDb db)
	{

		BeautyService.service.downloadManager.download(db);

		adapter.notifyDataSetChanged();
	}

	private Handler handlerDecode = new Handler()
	{
		public void handleMessage(Message msg)
		{
			switch (msg.what)
			{
			case ThumbManager.DECODE_FAILED:
			case ThumbManager.DECODe_FINISHED:
				addHeader();
				break;

			}
		}
	};
	private Handler handlerIcon = new Handler()
	{
		public void handleMessage(Message msg)
		{
			switch (msg.what)
			{
			case ImageCacheManager.GET_ICON:
				adapter.notifyDataSetChanged();
				break;

			}
		}
	};
	private ListView lv;
	private ImageDbAdapter adapter;
	private Gallery3D g3d;
	private View headerView;
	private GalleryAdapter galleryAdatper;
	private TextView footview;
	private com.tencent.mobwin.AdView adview;
}
