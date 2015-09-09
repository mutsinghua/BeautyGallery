package org.androidbook.netdata.xml.data;

import org.androidbook.gallery.constants.NetConstants;

public class WinImageDb
{
	public String discription = "";
	public int id;
	public String name = "";
	public int size;
	public int adault = 0;
	public int installStatus = 0;
	public int value = 0;
	public String category = "";

	public static final int INSTALLED = 2;
	public static final int DOWNLOADING = 1;
	public static final int UNEXISTED = 0;
	public static final int DOWNLOAD_CANCELING = 3;

	public static final String IMAGEDB = "imagedb";
	public static final String IMAGEDB_NAME = "name";
	public static final String IMAGEDB_ID = "id";

	public static final String IMAGEDB_DESC = "discription";
	public static final String IMAGEDB_SIZE = "size";
	public static final String IMAGEDB_VALUE = "value";
	public static final String IMAGEDB_OTHER = "other";
	public static final String IMAGEDB_VERSION = "version";
	public static final String IMAGEDB_VERSION_CODE = "code";
	public static final String IMAGEDB_FORCE_UPDATE = "force";
	public static final String IMAGEDB_CATEGORY = "category";
	public static final String IMAGEDB_AD = "ad";

	public static final int FORCE = 1;
	public static final int UNFORCE = 0;

	public String getThumbUrl()
	{
		return NetConstants.REQUEST_DATA_URL + name + ".jpg";
	}

	public static WinImageDb defaultDb;

	/**
	 * 获取程序内置的图片库
	 * 
	 * @return
	 */
	public static WinImageDb getDefaultOne()
	{
		if (defaultDb == null)
		{
			WinImageDb db = new WinImageDb();
			db.discription = "日韩系列风俗娘美女";
			db.id = 0;
			db.name = "sample";
			db.size = 1018880;
			db.value = 0;
			defaultDb = db;
		}

		return defaultDb;
	}

	@Override
	public boolean equals(Object o)
	{
		if (o == this)
		{
			return true;
		}
		if (o instanceof WinImageDb)
		{
			WinImageDb oo = (WinImageDb) o;
			if (oo.id == id)
			{
				return true;
			}
		}
		return false;
	}

	@Override
	public int hashCode()
	{
		return Integer.valueOf(id).hashCode();
	}

}