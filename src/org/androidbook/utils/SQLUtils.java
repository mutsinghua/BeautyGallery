package org.androidbook.utils;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;

public class SQLUtils
{
	public static final String PIC_TABLE_NAME = "PIC_INFO";
	
	
	public static SQLiteDatabase openDatabase(String databasePath)
	{
		try
		{
			SQLiteDatabase database = SQLiteDatabase.openDatabase(databasePath, null, SQLiteDatabase.OPEN_READWRITE);

			return database;
		} catch (Exception e)
		{
			// TODO: handle exception
		}
		return null;
		
		
	}
	
	public static SQLiteDatabase openorCreateDatabase(String databasePath)
	{
		try
		{
			SQLiteDatabase database = SQLiteDatabase.openOrCreateDatabase(databasePath, null);

			return database;
		} catch (Exception e)
		{
			// TODO: handle exception
		}
		return null;
		
		
	}
	
	public static void close(SQLiteDatabase db)
	{
		db.close();
	}
	
	public static void createTable(SQLiteDatabase db)
	{
		String sql = // add by dragonlin
			
				"create table if not exists PIC_INFO(" +
				"dataid INTEGER PRIMARY KEY AUTOINCREMENT, " +
				"data BLOB," +
				"thumb BLOB);";
		
		db.execSQL(sql);
		sql = // add by dragonlin
			
			"create table if not exists COVER_INFO(" +
			"dbid TEXT PRIMARY KEY, " +
			"title TEXT);";
		db.execSQL(sql);
	}
	
	public static void insertPic(byte[] data,SQLiteDatabase db)
	{
		ContentValues cv = new ContentValues(1);
		cv.put("data", data);
		db.insert(PIC_TABLE_NAME, null, cv);
	}
	
	public static void updatethumb(byte[]data,int id,SQLiteDatabase db)
	{
		ContentValues cv = new ContentValues(1);
		cv.put("thumb", data);
		db.update(PIC_TABLE_NAME, cv, "dataid=?", new String[]{String.valueOf(id)});
	}
	
	public static void insertTitle(int dbid, String title,SQLiteDatabase db)
	{
		ContentValues cv = new ContentValues(2);
		cv.put("dbid", dbid);
		cv.put("title", title);
		db.insert("COVER_INFO", null, cv);
	}
	
	public static byte[] getPic(int dataid, SQLiteDatabase db)
	{
		byte[] data = null;
		Cursor cursor = db.query(PIC_TABLE_NAME, new String[]{"data"}, "dataid=?", new String[]{String.valueOf(dataid)}, null, null, null);
		if( cursor != null)
		{
			if( cursor.moveToFirst())
			{
				data = cursor.getBlob(cursor.getColumnIndex("data"));
				int decryLen = data.length>=1000?1000:data.length;
				for( int i=0;i<decryLen;i++)
				{
					data[i] = (byte) (data[i]^0x99);
				}
			}
			cursor.close();
		}
		return data;
	}
	
	public static byte[] encodePic(byte[] oridata)
	{

				int decryLen = oridata.length>=1000?1000:oridata.length;
				for( int i=0;i<decryLen;i++)
				{
					oridata[i] = (byte) (oridata[i]^0x99);
				}

		return oridata;
	}
	
	public static void saveThumb(Bitmap bitmap, int id, SQLiteDatabase db)
	{
		ContentValues cv = new ContentValues();
		cv.put("thumb", FileData.serializeBitmap(bitmap));
		db.update(SQLUtils.PIC_TABLE_NAME, cv, "dataid=?", new String[]{String.valueOf(id)});
	}
}
