package org.androidbook.netdata;

import android.os.Handler;

public class ImageRequest
{
	public String url = "";
	public Handler callBack;
	
	public ImageRequest (String url, Handler callback)
	{
		this.url = url;
		this.callBack = callback;
	}
	
	@Override
	public boolean equals(Object obj)
	{
		if(obj != null && obj instanceof ImageRequest)
		{
			ImageRequest o = (ImageRequest) obj;
			return url.equals(o.url);
		}
		return false;
	}
	
	@Override
	public int hashCode()
	{
		return url.hashCode();
	}
	
	
}