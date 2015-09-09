package org.androidbook.net;

import java.io.IOException;
import java.net.Socket;
import java.security.SecureRandom;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.http.conn.ssl.SSLSocketFactory;

public class CustomSSLSocketFactory
{

	private javax.net.ssl.SSLSocketFactory FACTORY = HttpsURLConnection.getDefaultSSLSocketFactory();

	public CustomSSLSocketFactory()
	{
		try
		{
			SSLContext context = SSLContext.getInstance("TLS");
		
			context.init(null, new TrustManager[]{   
	                new X509TrustManager() {   
	                    public java.security.cert.X509Certificate[] getAcceptedIssuers() {   
	                        return null;   
	                    }   
	                    public void checkClientTrusted(   
	                        java.security.cert.X509Certificate[] certs, String authType) {   
	                    }   
	                    public void checkServerTrusted(   
	                        java.security.cert.X509Certificate[] certs, String authType) {   
	                    }   
	                }   
	            }, new SecureRandom());   

			FACTORY = context.getSocketFactory();
		} catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	public Socket createSocket() throws IOException
	{
		return FACTORY.createSocket();
	}

}