package org.androidbook.net;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.androidbook.gallery.beauty.BeautyApplication;
import org.androidbook.gallery.constants.NetConstants;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.SingleClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;

import android.content.Context;
import android.net.Uri;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;

/**
 * 处理http相关的
 * 
 * @author rexzou
 * 
 */
public class HttpTask
{
	/**
	 * 正常返回值
	 */
	public static final int HTTP_OK = 200;

	public static final int NETWORK_ERROR = 0xffffffff;
	/**
	 * 处理http数据
	 */
	private IHttpListener httpListener;

	private WinHttpRequest request;

	private static String available_domain;

	public HttpTask(WinHttpRequest request)
	{
		this.request = request;
	}

	public static PowerManager pm = (PowerManager) BeautyApplication.instance.getSystemService(Context.POWER_SERVICE);

	public void excute()
	{
		httpListener = request.listener;

		if (request.method == Constant.HTTP_POST)
		{
			doPost();
		} else
		{
			doGet();
		}
		if (httpListener != null)
		{
			httpListener.onFinish(request);
		}
	}

	private void doGet()
	{
		String url = request.url.trim();
		if (url.toLowerCase().startsWith("https"))
		{
			getHttps(url, request.queryString);
		} else if (url.toLowerCase().startsWith("http"))
		{
			// // if(request.queryString != null)
			// {
			// try
			// {
			// // sendGet(url,URLEncoder.encode(request.queryString, "utf-8"));
			// } catch (UnsupportedEncodingException e)
			// {
			// e.printStackTrace();
			// // sendGet(url,URLEncoder.encode(request.queryString));
			// }
			// }
			// else
			// {
			sendGet(url, request.queryString);
			// }''
		} else
		{
			httpListener.onError(Constant.PROCOL_NOT_SUPPORT, request);
		}
	}

	private void doPost()
	{
		String url = request.url.trim();
		if (url.toLowerCase().startsWith("https"))
		{
			httpListener.onError(Constant.PROCOL_NOT_SUPPORT, request);
		} else if (url.toLowerCase().startsWith("http"))
		{
			sendPost(url, request.queryString);
		} else
		{
			httpListener.onError(Constant.PROCOL_NOT_SUPPORT, request);
		}
	}

	/**
	 * 处理http的客户端
	 * 
	 * @return
	 */
	private HttpClient getClient()
	{
		HttpParams params = new BasicHttpParams();
		SchemeRegistry schemeRegistry = new SchemeRegistry();
		schemeRegistry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
		SingleClientConnManager cm = new SingleClientConnManager(params, schemeRegistry);
		return new DefaultHttpClient(cm, params);
	}

	/**
	 * 处理https 只支持get
	 * 
	 * @param uri
	 *            带https的网址
	 * @param queryString
	 *            get参数
	 * @return 网络返回值
	 */

	public String getHttps(String uri, String queryString)
	{

		if (!uri.endsWith("?"))
		{
			uri += "?";
		}
		uri += queryString;
		try
		{
			SSLContext sc = SSLContext.getInstance("TLS");

			sc.init(null, new TrustManager[] { new MyTrustManager() }, new SecureRandom());

			HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

			HttpsURLConnection.setDefaultHostnameVerifier(new MyHostnameVerifier());

			HttpsURLConnection con = (HttpsURLConnection) new URL(uri).openConnection();
			con.setRequestProperty("Connection", "Keep-Alive");
			con.setDoOutput(true);

			con.setDoInput(true);

			int rescode = con.getResponseCode();

			// Log.d("TEST", "rescode"+rescode );
			BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream()));

			StringBuffer sb = new StringBuffer();

			String line;

			while ((line = br.readLine()) != null)
			{
				sb.append(line);
			}
			return sb.toString();

		} catch (Exception e)
		{
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * 发送数据,阻塞函数
	 * 
	 * @param uri
	 *            网址
	 * @param params
	 *            数据对
	 * @param method
	 *            post or get
	 */
	public String sendPost(String uri, String params)
	{
		HttpEntity ent = null;
		HttpClient cli = getClient();
		HttpPost post = new HttpPost(uri);
		HttpContext localContext = new BasicHttpContext();
		HttpResponse resp;
		String strResult = "";
		try
		{
			post.setEntity(new StringEntity(params, "UTF-8"));
			resp = cli.execute(post, localContext);
			StatusLine status = resp.getStatusLine();
			if (httpListener != null)
			{
				if (status != null && status.getStatusCode() == HTTP_OK) // 正常接收
				{
					// strResult = EntityUtils.toString(resp.getEntity());
					httpListener.handleData(resp.getEntity(), request);

				} else
				// 出错啊
				{
					httpListener.onError(status.getStatusCode(), request);
				}
			}

		} catch (ClientProtocolException e)
		{
			e.printStackTrace();
			httpListener.onError(-1, request);
		} catch (IOException e)
		{
			httpListener.onError(-2, request);
			e.printStackTrace();
		} catch (Throwable t)
		{
			httpListener.onError(-4, request);
			t.printStackTrace();
		}
		return strResult;
	}

	/**
	 * 发送数据,阻塞函数
	 * 
	 * @param uri
	 *            网址
	 * @param params
	 *            数据对
	 * @param method
	 *            post or get
	 */
	public void sendPost(String uri, List<NameValuePair> params)
	{
		HttpEntity ent = null;
		HttpClient cli = getClient();
		HttpPost post = new HttpPost(uri);
		HttpContext localContext = new BasicHttpContext();
		HttpResponse resp;
		// 联网时禁止休眠

		PowerManager.WakeLock wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "httpNet");
		// 联网时禁止休眠

		wakeLock.acquire();
		try
		{
			ent = new UrlEncodedFormEntity(params, HTTP.UTF_8);
			post.setEntity(ent);
			resp = cli.execute(post, localContext);
			StatusLine status = resp.getStatusLine();
			if (request.iscanceled)
			{
				return;
			}
			if (httpListener != null)
			{
				if (status != null && status.getStatusCode() == HTTP_OK) // 正常接收
				{
					// String strResult =
					// EntityUtils.toString(resp.getEntity());
					httpListener.handleData(resp.getEntity(), request);

				} else
				// 出错啊
				{
					httpListener.onError(status.getStatusCode(), request);
				}
			}

		} catch (ClientProtocolException e)
		{
			e.printStackTrace();
			httpListener.onError(-1, request);
		} catch (IOException e)
		{
			httpListener.onError(-2, request);
			e.printStackTrace();
		} catch (Throwable t)
		{
			httpListener.onError(-4, request);
			t.printStackTrace();
		} finally
		{
			wakeLock.release();
		}
	}

	public void setHttpListener(IHttpListener httpListener)
	{
		this.httpListener = httpListener;
	}

	public String sendGet(String uri, String queryString)
	{

		if (uri.startsWith("https"))
		{
			return getHttps(uri, queryString);
		}
		String strResult = null;
		int trytime = 0;
		
		if (queryString != null)
		{
			if (!uri.endsWith("?") && !queryString.startsWith("?"))
			{
				uri += "?";
			}
			uri += queryString;
		}
		
		do
		{
			HttpClient cli = getClient();
			HttpGet get = null;
			HttpContext localContext = new BasicHttpContext();
			HttpResponse resp;

			try
			{

			
				// 处理网络域
				if (available_domain != null)
				{
					if (!uri.startsWith(available_domain))
					{
						if (uri.startsWith(NetConstants.WEBSITE))
						{
							uri = uri.replace(NetConstants.WEBSITE, available_domain);
						} else
						{
							uri = uri.replace(NetConstants.BK_DOMAIN, available_domain);
						}
					}
				}
				// Log.d("TEST", "http get"+uri);
				get = new HttpGet(uri);
				resp = cli.execute(get, localContext);
				StatusLine status = resp.getStatusLine();
				// Log.d("TEST", "http"+status.getStatusCode());
				if (httpListener != null)
				{
					if (status != null && status.getStatusCode() == HTTP_OK) // 正常接收
					{

						httpListener.handleData(resp.getEntity(), request);

					} else
					// 出错啊
					{
						httpListener.onError(status.getStatusCode(), request);
					}
				}
				

				if (uri.startsWith(NetConstants.WEBSITE))
				{
					available_domain = NetConstants.WEBSITE;
				} else
				{
					available_domain = NetConstants.BK_DOMAIN;
				}
				break;
			}
			// catch (ClientProtocolException e)
			// {
			// e.printStackTrace();
			// httpListener.onError(-1, request);
			// } catch (IOException e)
			// {
			// // TODO Auto-generated catch block
			// e.printStackTrace();
			// httpListener.onError(-2, request);
			// }
			catch (Throwable t)
			{
				trytime++;
				if (trytime < 2)
				{
					if (uri.startsWith(NetConstants.WEBSITE))
					{
						available_domain = NetConstants.BK_DOMAIN;
					} else
					{
						available_domain = NetConstants.WEBSITE;
					}
				} else
				{
					httpListener.onError(-4, request);
				}
				t.printStackTrace();
			}

		} while (trytime < 2);
		return strResult;
	}

	/**
	 * 发送数据,阻塞函数
	 * 
	 * @param uri
	 *            网址
	 * @param params
	 *            数据对
	 * @param method
	 *            post or get
	 */
	public void sendGet(String uri, Map params)
	{

		HttpClient cli = getClient();
		HttpGet get = null;
		HttpContext localContext = new BasicHttpContext();
		HttpResponse resp;

		try
		{
			String paramStr = "";

			Iterator iter = params.entrySet().iterator();
			while (iter.hasNext())
			{
				Map.Entry entry = (Map.Entry) iter.next();
				Object key = entry.getKey();
				Object val = entry.getValue();
				paramStr += paramStr = "&" + key + "=" + val;
			}

			if (!paramStr.equals(""))
			{
				paramStr = paramStr.replaceFirst("&", "?");
				uri += paramStr;
			}

			get = new HttpGet(uri);
			resp = cli.execute(get, localContext);
			StatusLine status = resp.getStatusLine();
			if (httpListener != null)
			{
				if (status != null && status.getStatusCode() == HTTP_OK) // 正常接收
				{
					// String strResult =
					// EntityUtils.toString(resp.getEntity());
					httpListener.handleData(resp.getEntity(), request);

				} else
				// 出错啊
				{
					httpListener.onError(status.getStatusCode(), request);
				}
			}

		} catch (ClientProtocolException e)
		{
			e.printStackTrace();
			httpListener.onError(-1, request);
		} catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
			httpListener.onError(-2, request);
		} catch (Throwable t)
		{
			httpListener.onError(-4, request);
			t.printStackTrace();
		}
	}

	private class MyHostnameVerifier implements HostnameVerifier
	{

		@Override
		public boolean verify(String hostname, SSLSession session)
		{

			return true;

		}

	}

	/**
	 * 
	 * MyTrustManager
	 */

	private class MyTrustManager implements X509TrustManager
	{

		@Override
		public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException
		{
			// TODO Auto-generated method stub

		}

		@Override
		public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException
		{
			// TODO Auto-generated method stub

		}

		@Override
		public java.security.cert.X509Certificate[] getAcceptedIssuers()
		{
			// TODO Auto-generated method stub
			return null;
		}

	}

}