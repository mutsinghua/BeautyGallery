package org.androidbook.net;

import org.androidbook.gallery.db.AvailableDB;

import android.content.Context;

public class MainController
{
	private static MainController instance;

	private HttpController httpController;

	private MainController()
	{
		httpController = new HttpController(Constant.HTTP_ICON_THREAD_NUMBER);
	}

	public static boolean isNull()
	{
		return instance==null;
	}
	
	public void init()
	{
		
	
	}

	public void send(WinHttpRequest request)
	{
		httpController.send(request);
	}

	public static void close()
	{
		if (instance != null)
		{
			instance.httpController.close();
			
		}

		instance = null;
	}

	public static MainController getInstance()
	{
		if (instance == null)
		{
			instance = new MainController();
		}
		return instance;
	}


	public static void destory()
	{
		if( instance != null)
		{
			instance.httpController = null;
		}
		
		instance=null;
		
		
	}
}