package org.androidbook.gallery.db;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;

import org.androidbook.netdata.xml.data.WinImageDb;
import org.androidbook.utils.FileData;
import org.androidbook.utils.SQLUtils;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.os.Handler;
import android.os.Message;
import android.os.Parcel;
import android.util.Log;


public class ThumbManager
{

	public static final int THUMB_WIDTH = 145;
	public static final int THUMB_HEIGHT = 115;
	private Hashtable<Integer, Bitmap> thumbList;
	public static final int DECODING = 1;
	public static final int DECODE_FAILED = 2;
	public static final int DECODe_FINISHED = 3;
	private static final String TAG = "ThumbManager";
	private SQLiteDatabase db;
	private ArrayList<Integer> arrayIds = new ArrayList<Integer>();

	public ThumbManager()
	{
		super();
		thumbList = new Hashtable<Integer, Bitmap>();
	}

	public boolean openDb(String path)
	{
		File file = new File(path);
		if (!file.exists())
		{
			return false;
		}

		db = SQLUtils.openDatabase(path);

		if (db == null)
		{
			return false;
		}
		return true;
	}

	public void destroy()
	{
		if (db != null)
		{
			db.close();
		}
		thumbList.clear();
		arrayIds.clear();
	}

	private boolean isStarting =false;
	
	public void checkThumb(final Handler callback)
	{
		isStarting = true;
		DecodeThread dt = new DecodeThread(callback);
		dt.start();
	}

	/**
	 * 生成缩略图
	 * 
	 * @author rexzou
	 * 
	 */
	class DecodeThread extends Thread
	{

		public DecodeThread(Handler callback)
		{
			super("DecodeThread");
			this.callback = callback;
		}

		private Handler callback;

		public void run()
		{
			try
			{
				if (getDb() == null)
				{
					return;
				}

				Cursor cursor = getDb().query(SQLUtils.PIC_TABLE_NAME, new String[]
				{ "dataid,thumb" }, null, null, null, null, null);
				if (cursor != null)
				{
					if (cursor.moveToFirst())
					{
						if (callback != null)
						{
							Message msg = Message.obtain();
							msg.what = DECODING;
							msg.arg1 = 0;
							msg.arg2 = cursor.getCount();
							callback.sendMessage(msg);
						}
						int count = 0;

						do
						{
							byte[] thumbdata = cursor.getBlob(cursor.getColumnIndex("thumb"));
							int id = cursor.getInt(cursor.getColumnIndex("dataid"));
							if (thumbdata != null)
							{
								try
								{
									Bitmap thumb = BitmapFactory.decodeByteArray(thumbdata, 0, thumbdata.length);
									getThumbList().put(id, thumb);
									arrayIds.add(Integer.valueOf(id));
								} catch (Exception e)
								{
									e.printStackTrace();
								}
							} else
							{
								byte[] bigdata = SQLUtils.getPic(id, getDb());
								Bitmap thumb = FileData.genThumb(bigdata, THUMB_WIDTH, THUMB_HEIGHT);
								SQLUtils.saveThumb(thumb, id, getDb());
								getThumbList().put(id, thumb);
								arrayIds.add(Integer.valueOf(id));
							}
							if (callback != null)
							{
								Message msg = Message.obtain();
								msg.what = DECODING;
								msg.arg1 = count++;
								msg.arg2 = cursor.getCount();
								callback.sendMessage(msg);
							}
						} while (cursor.moveToNext());
						if (callback != null)
						{
							Message msg = Message.obtain();
							msg.what = DECODe_FINISHED;
							callback.sendMessage(msg);
						}
						
					} else
					{
						if (callback != null)
						{
							Message msg = Message.obtain();
							msg.what = DECODe_FINISHED;
							callback.sendMessage(msg);
						}
					}
					cursor.close();
				} else
				{
					if (callback != null)
					{
						Message msg = Message.obtain();
						msg.what = DECODE_FAILED;
						callback.sendMessage(msg);
					}
				}
			}

			catch (Exception e)
			{
				e.printStackTrace();
				Message msg = Message.obtain();
				msg.what = DECODe_FINISHED;
				callback.sendMessage(msg);
			}
		}
	};

	

	public Hashtable<Integer, Bitmap> getThumbList()
	{
		return thumbList;
	}

	public Bitmap getImage(int position)
	{
		return thumbList.get(getImageId(position));

	}

	public void removeItem(int id)
	{
		arrayIds.remove(Integer.valueOf(id));
		thumbList.remove(Integer.valueOf(id));
	}
	
	public int getImageId(int position)
	{
		return arrayIds.get(position);
	}

	public int getPosition(int id)
	{
		return arrayIds.indexOf(Integer.valueOf(id));
	}
	
	public int getImageCount()
	{
		return arrayIds.size();
	}

	public SQLiteDatabase getDb()
	{
		return db;
	}

	public boolean isStarting()
	{
		return isStarting;
	}
}
