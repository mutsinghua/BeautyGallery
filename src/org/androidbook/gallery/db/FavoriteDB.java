package org.androidbook.gallery.db;

import org.androidbook.netdata.xml.data.WinImageDb;
import org.androidbook.utils.FileData;
import org.androidbook.utils.SQLUtils;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;

public class FavoriteDB
{

	private static FavoriteDB instance = null;

	private static final String favDBPath = "favDB";

	/**
	 * "dataid INTEGER PRIMARY KEY AUTOINCREMENT, " + "data BLOB," +
	 * "datacategory TEXT," + "datadate INTEGER," + "datamark INTEGER," +
	 * "datadesc TEXT," + "thumb BLOB);";
	 */
	public static final String COL_dataid = "dataid";
	public static final String COL_data = "data";
	public static final String COL_datacategory = "datacategory";
	public static final String COL_datadate = "datadate";
	public static final String COL_datamark = "datamark";
	public static final String COL_datadesc = "datadesc";
	public static final String COL_thumb = "thumb";

	private SQLiteDatabase database;

	private WinImageDb winImageDb;

	private FavoriteDB()
	{
		winImageDb = new WinImageDb();
		winImageDb.name = favDBPath;
	}

	public static FavoriteDB getInstance()
	{
		if (instance == null)
		{
			instance = new FavoriteDB();

		}
		return instance;
	}

	public WinImageDb getFavWinImageDb()
	{
		return winImageDb;
	}

	public void init()
	{
		database = SQLUtils.openorCreateDatabase(FileData.getDataPath() + favDBPath);
		createTable();
	}

	private void createTable()
	{
		String sql = // add by dragonlin

		"create table if not exists PIC_INFO(" + "dataid INTEGER PRIMARY KEY AUTOINCREMENT, " + "data BLOB," + "datacategory TEXT," + "datadate INTEGER," + "datamark INTEGER," + "datadesc TEXT,"
				+ "datatag TEXT," + "thumb BLOB);";

		database.execSQL(sql);
	}

	public void destroy()
	{
		if (database != null && database.isOpen())
		{
			database.close();
		}
		database = null;
		instance = null;
	}

	public void addImage(Bitmap bitmap, WinImageDb db)
	{
		byte[] oridata = FileData.serializeBitmapHQ(bitmap);
		oridata = SQLUtils.encodePic(oridata);
		ContentValues cv = new ContentValues(1);
		cv.put(COL_data, oridata);
		cv.put(COL_datacategory, db.category);
		cv.put(COL_datadesc, db.category);
		cv.put(COL_datadate, System.currentTimeMillis());
		database.insert(SQLUtils.PIC_TABLE_NAME, null, cv);
	}

	public void removeImage(int id)
	{
		database.delete(SQLUtils.PIC_TABLE_NAME, COL_dataid + "=?", new String[] { String.valueOf(id) });
	}
}
