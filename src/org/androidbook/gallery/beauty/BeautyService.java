package org.androidbook.gallery.beauty;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import org.androidbook.gallery.beauty.R;
import org.androidbook.gallery.db.AvailableDB;
import org.androidbook.net.MainController;
import org.androidbook.netdata.DownloadManager;

public class BeautyService extends Service
{

	private static final String TAG = "BeautyService";


	public DownloadManager downloadManager;
	

	public static BeautyService service;

	
	@Override
	public IBinder onBind(Intent intent)
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void onCreate()
	{
		super.onCreate();
		service= this;
		downloadManager = new DownloadManager(this);
		if (MainController.isNull())
		{
			MainController.getInstance().init();

		}
	}

	@Override
	public void onDestroy()
	{
		service= null;
		downloadManager.destroy();
		downloadManager = null;
//		Log.v(TAG, "service ondestory()");
		super.onDestroy();
	}

	
}
