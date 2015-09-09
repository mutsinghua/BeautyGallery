package org.androidbook.net;

import org.apache.http.HttpEntity;

/**
 * http相关的接口
 * @author rexzou
 *
 */
public interface IHttpListener
{
	public final static int NET_ERROR = 404;
	/**
	 * 处理http返回来的数据
	 * @param str http中得到的数据
	 * @return 正常返回true,异常返回false
	 * @throws Exception 
	 */
	public void handleData(HttpEntity res, WinHttpRequest request) throws Exception;
	
	/**
	 * 正常完成调用
	 */
	public void onFinish(WinHttpRequest req);
	
	/**
	 * 出错时调用
	 * @param errorCode
	 */
	public void onError(int errorCode,WinHttpRequest req);
}