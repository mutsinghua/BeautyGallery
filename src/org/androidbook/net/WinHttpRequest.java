package org.androidbook.net;

import org.apache.http.HttpRequest;

public class WinHttpRequest
{
	/**
	 * 请求地址
	 */
	public String url;

	/**
	 * 数据对, 原始数据,编码前的 a=1&b=2
	 */
	public String queryString;

	/**
	 * get 还是 post HTTP_GET HTTP_POST
	 */
	public int method;

	/**
	 * 当为post时,发送的数据
	 */
	public byte[] data;

	/**
	 * 处理函数
	 */
	public IHttpListener listener;

	/**
	 * 其他对象
	 */
	public Object tag;

	public boolean iscanceled = false;
	
	public WinHttpRequest()
	{

	}

	public WinHttpRequest(String url, IHttpListener lis)
	{
		this.url = url;
		this.listener = lis;
	}

	@Override
	public boolean equals(Object o)
	{
		if( o == this)
		{
			return true;
		}
		if (o instanceof WinHttpRequest)
		{
			WinHttpRequest req = (WinHttpRequest) o;
			if (req.url.equals(url) && ((req.queryString != null && queryString != null && req.queryString.equalsIgnoreCase(queryString)) || req.queryString == null && queryString == null) && req.data == data)
			{
				return true;
			}
		}
		return false;
	}

	@Override
	public int hashCode()
	{
		String s =url + queryString;
		
		return s.hashCode();
	}

}