package org.androidbook.netdata.xml;

import java.io.InputStream;

import android.os.Handler;

public interface ISAXService
{
	public static final int  PARSE_FINISH = 0;
	public void parse(InputStream is, Handler callback);
}