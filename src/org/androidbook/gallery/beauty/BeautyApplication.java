package org.androidbook.gallery.beauty;

import java.util.Locale;
import org.androidbook.gallery.beauty.R;

import android.app.Application;
import android.content.Context;
import android.os.PowerManager;
import android.util.Log;

public class BeautyApplication extends Application
{
	public static final String TAG = "BeautyApplication";
	public static BeautyApplication instance;

	public static boolean isInChina = true;
	
	static {
		isInChina = isInChina();
	}

	public PowerManager pm ;
	
	@Override
	public void onCreate()
	{
		super.onCreate();
		instance = this;
		isInChina = isInChina();
		pm = (PowerManager) instance.getSystemService(Context.POWER_SERVICE);
//		Log.v(TAG, "isInchina"+isInChina);
	}

	@Override
	public void onTerminate()
	{
		// TODO Auto-generated method stub
		super.onTerminate();
		pm = null;
	}

	@Override
	public void onLowMemory()
	{
		// TODO Auto-generated method stub
		super.onLowMemory();
	}

	public static boolean isInChina()
	{
		Locale lo = Locale.getDefault();
		if (lo.getCountry().equalsIgnoreCase(Locale.CHINA.getCountry()))
		{
			return true;
		} else
		{
			return false;
		}

	}

}
