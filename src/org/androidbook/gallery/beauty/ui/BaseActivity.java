package org.androidbook.gallery.beauty.ui;

import java.io.File;
import java.util.ArrayList;

import org.androidbook.gallery.beauty.BeautyApplication;
import org.androidbook.gallery.beauty.BeautyService;
import org.androidbook.gallery.beauty.R;
import org.androidbook.gallery.constants.NetConstants;
import org.androidbook.utils.FileData;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import com.umeng.analytics.MobclickAgent;
import com.umeng.xp.common.ExchangeConstants;
import com.umeng.xp.controller.ExchangeDataService;
import com.umeng.xp.view.ExchangeViewManager;

public class BaseActivity extends Activity
{

	protected static int screenWidth;

	protected static int screenHight;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		
		if( android.os.Build.VERSION.SDK_INT < 8 )
		{
			getWindow().setFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND, WindowManager.LayoutParams.FLAG_DIM_BEHIND);
			getWindow().setBackgroundDrawableResource(R.drawable.forlowversion);
		}
		else if(android.os.Build.VERSION.SDK_INT > 10)
		{
			getWindow().setFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND, WindowManager.LayoutParams.FLAG_DIM_BEHIND);
		}
		else
		{
			getWindow().setFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND, WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
		}
		
		for (LifeCycleListener listener : mListeners)
		{
			listener.onActivityCreated(this);
		}

		Intent intent = new Intent(this, BeautyService.class);
		startService(intent);
		if (screenHight == 0)
		{
			requestUpdateScreen();
		}
	}

	public void requestUpdateScreen()
	{
		DisplayMetrics dm = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(dm);
		screenWidth = dm.widthPixels;
		screenHight = dm.heightPixels;
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{

		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.common_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch (item.getItemId())
		{
			case R.id.menu_recommend:
			{
				new ExchangeViewManager(this, new ExchangeDataService()).addView(null, ExchangeConstants.type_cloud_full);
				break;
//				String path = FileData.getDataPath();
//				path = path + SplashActivity.SCREEN_NAME;
//				File file = new File(path);
//				Intent sharei = new Intent(Intent.ACTION_SEND);
//				sharei.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.recommendation));
//
//				sharei.setType("image/*");
//				// String url = Constant.WEB_SITE+"product/"+dir+"/device1.png";
//				Uri uri = Uri.fromFile(file);
//				sharei.putExtra(Intent.EXTRA_STREAM, uri);
//				// sharei.put
//				StringBuilder sb = new StringBuilder();
//				sb.append(getString(R.string.recommendation_text));
//				sb.append("<" + getString(R.string.app_name) + ">");
//				sb.append(getString(R.string.download_url));
//				sb.append(NetConstants.REQUEST_DOWLOAD_URL);
//
//				sharei.putExtra(Intent.EXTRA_TEXT, sb.toString());
//				sharei.putExtra("sms_body", sb.toString());
//				Intent dd = Intent.createChooser(sharei, getString(R.string.recommendation));
//				if (dd != null)
//				{
//					startActivity(dd);
//				}
			}
//				break;
			case R.id.menu_help:
			{
				Intent intent = new Intent(BaseActivity.this, HelpActivity.class);
				startActivity(intent);
			}
				
				break;
			case R.id.menu_about:
				showAbout();
				break;
//			case R.id.menu_earn_point:
//				AppOffersManager.showAppOffers(this);
//				break;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onResume()
	{
		super.onResume();
		MobclickAgent.onResume(this);
		
	}

	@Override
	protected void onPause()
	{
		super.onPause();
		MobclickAgent.onPause(this);
	}

	protected static void showToast(String s)
	{
		Toast.makeText(BeautyApplication.instance, s, Toast.LENGTH_LONG).show();
	}

	protected static void showToast(int resId)
	{
		String text = BeautyApplication.instance.getResources().getString(resId);
		showToast(text);
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig)
	{
		super.onConfigurationChanged(newConfig);
		requestUpdateScreen();

	}

	private final ArrayList<LifeCycleListener> mListeners = new ArrayList<LifeCycleListener>();

	public static interface LifeCycleListener
	{
		public void onActivityCreated(BaseActivity activity);

		public void onActivityDestroyed(BaseActivity activity);

		public void onActivityPaused(BaseActivity activity);

		public void onActivityResumed(BaseActivity activity);

		public void onActivityStarted(BaseActivity activity);

		public void onActivityStopped(BaseActivity activity);
	}

	public static class LifeCycleAdapter implements LifeCycleListener
	{
		public void onActivityCreated(BaseActivity activity)
		{
		}

		public void onActivityDestroyed(BaseActivity activity)
		{
		}

		public void onActivityPaused(BaseActivity activity)
		{
		}

		public void onActivityResumed(BaseActivity activity)
		{
		}

		public void onActivityStarted(BaseActivity activity)
		{
		}

		public void onActivityStopped(BaseActivity activity)
		{
		}
	}

	public void addLifeCycleListener(LifeCycleListener listener)
	{
		if (mListeners.contains(listener))
			return;
		mListeners.add(listener);
	}

	public void removeLifeCycleListener(LifeCycleListener listener)
	{
		mListeners.remove(listener);
	}

	@Override
	protected void onDestroy()
	{
		super.onDestroy();
		for (LifeCycleListener listener : mListeners)
		{
			listener.onActivityDestroyed(this);
		}
	}

	@Override
	protected void onStart()
	{
		super.onStart();
		for (LifeCycleListener listener : mListeners)
		{
			listener.onActivityStarted(this);
		}
	}

	@Override
	protected void onStop()
	{
		super.onStop();
		for (LifeCycleListener listener : mListeners)
		{
			listener.onActivityStopped(this);
		}
	}
	
	private void showAbout()
	{
		LayoutInflater factory = LayoutInflater.from(this);
		View textEntryView = factory.inflate(R.layout.about, null);
		TextView tv = (TextView) textEntryView.findViewById(R.id.about_text);
		String about = getString(R.string.about_this);
		about = about.replace("APP_NAME", getString(R.string.app_name));
		try
		{
//			Bundle bd = getPackageManager().get.getApplicationInfo(getPackageName(), PackageManager.GET_META_DATA).metaData;
			about = about.replace("VERSION_NAME", getPackageManager().getPackageInfo(getPackageName(),0).versionName);
		} catch (NameNotFoundException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		tv.setText(about);
		AlertDialog alert = new AlertDialog.Builder(this).setView(textEntryView).create();
		alert.show();
	}
}
