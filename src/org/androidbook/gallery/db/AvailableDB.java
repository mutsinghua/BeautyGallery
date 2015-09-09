package org.androidbook.gallery.db;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.androidbook.gallery.beauty.BeautyApplication;
import org.androidbook.gallery.beauty.ui.BaseActivity;
import org.androidbook.gallery.constants.NetConstants;
import org.androidbook.net.IHttpListener;
import org.androidbook.net.MainController;
import org.androidbook.net.WinHttpRequest;
import org.androidbook.netdata.xml.ISAXService;
import org.androidbook.netdata.xml.SAXImageDBService;
import org.androidbook.netdata.xml.data.WinImageDb;
import org.androidbook.utils.FileData;
import org.apache.http.HttpEntity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;


public class AvailableDB implements IHttpListener
{
	private static AvailableDB instance;
	public static final String DEFAULT_LOCAL_NAME = "photo.dat";
	public static final String COPY_FILE_FINISH = "org.androidbook.gallery.COPY_FINISH";
	private int versionCode;
	private final xmlHandler handlerLocal;
	private final xmlHandler handlerNet;
	private ArrayList<WinImageDb> imageDbList = new ArrayList<WinImageDb>();
	private WinImageDb header;
	private boolean isInited = false;
	public final static String PARSE_LOCAL_FINISH = "org.androidbook.gallery.parse_loc_xml_finish";
	public final static String PARSE_NET_FINISH = "org.androidbook.gallery.parse_net_xml_finish";
	public final static String NET_ERROR = "org.androidbook.gallery.NET_ERROR";
	public final static String DATA_PATH = "/androidbook.org/gallery/data";
	protected static final String TAG = "AvailableDB";
	private boolean initedOk = false;

	class xmlHandler extends Handler
	{
		private int type;

		public xmlHandler(Looper looper, int type)
		{
			super(looper);
			this.type = type;
		}

		@Override
		public void handleMessage(Message msg)
		{
//			Log.d(TAG, "handleMessage" + msg);
			if (type == 0)// 本地
			{
				switch (msg.what)
				{
				case ISAXService.PARSE_FINISH:
				
//					Log.d(TAG, msg.obj + "");
					ArrayList<WinImageDb> tempList = new ArrayList<WinImageDb>();
					tempList.addAll(imageDbList);
					if( msg.obj!= null)
					{
						List<WinImageDb> netDbList = ((ArrayList<WinImageDb>) msg.obj);
						tempList.addAll(netDbList);
					}
					imageDbList = tempList;
//					Log.d(TAG, "PARSE_LOCAL_FINISH"+ imageDbList.size() + "");
					initFile();
					BeautyApplication.instance.sendBroadcast(new Intent(PARSE_LOCAL_FINISH));
					setInitedOk(true);
					checkNetDb();

					break;
				}
			} else if (type == 1) // 网络
			{
				switch (msg.what)
				{
				case ISAXService.PARSE_FINISH:
					if (msg.arg1 > versionCode && msg.obj != null)
					{
						List<WinImageDb> netDbList = ((ArrayList<WinImageDb>) msg.obj);
						int size = netDbList.size();
						if (netDbList.size() > 0)
						{
							ArrayList<WinImageDb> tempList = new ArrayList<WinImageDb>(netDbList.size());
							for (int i = 0; i < size; i++)
							{
								WinImageDb db = netDbList.get(i);
								if (!imageDbList.contains(db))
								{
									tempList.add(db);
								}
							}
							ArrayList<WinImageDb> tList = new ArrayList<WinImageDb>();
							tList.addAll(imageDbList);
							tList.addAll(0, tempList);
							imageDbList = tList;
						}
						versionCode = imageDbList.size();
						initFile();
					}

					// checkNetDb();
					break;
				}
				BeautyApplication.instance.sendBroadcast(new Intent(PARSE_NET_FINISH));
			}

		}

	}

	public void checkNetDb()
	{
//		Log.v(TAG, "checkNetDb:");
		WinHttpRequest request = new WinHttpRequest();
		request.url = NetConstants.REQUEST_URL;
		request.queryString = "type=" + (BeautyApplication.isInChina ? NetConstants.DIR_SUB_NM : NetConstants.DIR_SUB_AD) + "&version=" + versionCode;
		request.listener = this;
		MainController.getInstance().send(request);
	}

	private AvailableDB()
	{
		HandlerThread ht = new HandlerThread("xml", android.os.Process.THREAD_PRIORITY_BACKGROUND);
		ht.start();
		Looper looper = ht.getLooper();
		handlerLocal = new xmlHandler(looper, 0);
		handlerNet = new xmlHandler(looper, 1);
	}

	public void initDb()
	{

		Thread t = new Thread()
		{
			public void run()

			{
				imageDbList = LocalSqlDb.getInstance().readWholeList();
				header = WinImageDb.getDefaultOne();
				copyFile();
				if (imageDbList.size() == 0)//没有本地列表
				{
					loadDefaultDb();
				} else
				{
					
					
					versionCode = imageDbList.size();
					Message msg = Message.obtain();
					msg.what = ISAXService.PARSE_FINISH;
					handlerLocal.sendMessage(msg);
				}
			}
		};
		t.start();

		// initFile();

	}

	/**
	 * 读取本地数据库
	 */
	public synchronized void loadLocalDb()
	{

		// if (!isInited)
		// {

		isInited = true;
		initDb();
		// }

	}

	public static AvailableDB getInstance()
	{
		if (instance == null)
		{
			instance = new AvailableDB();
		}
		return instance;
	}

	public static void destory()
	{
		LocalSqlDb.destory();
		if (instance != null)
		{
			instance.imageDbList.clear();
			instance.initedOk = false;
			instance.isInited = false;
		}

		instance = null;
	}

	@Override
	public void handleData(HttpEntity res, WinHttpRequest request)
	{
		InputStream is = null;
		try
		{
			is = res.getContent();
			// byte[] configxml = FileData.readByteFromInputStream(is);
			// String xmlPath = FileData.getDataPath();
			// xmlPath = xmlPath + TEMP_CONFIG;
			// // String xmlPath =
			// // FileData.getStorePath(BeautyApplication.instance,
			// TEMP_CONFIG);
			// FileData.writeDataToNewFile(xmlPath, configxml);
			// File file = new File(xmlPath);
			// // if( file.exists())
			// // {
			// // file.delete();
			// // }
			// // file.createNewFile();
			// is = new FileInputStream(file);
			SAXImageDBService service = new SAXImageDBService();
			service.parse(is, handlerNet);
		} catch (IllegalStateException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * 清除积分
	 * @param db
	 */
	public void spendPoints(WinImageDb db)
	{
		LocalSqlDb.getInstance().updateValue(db);
	}
	
	@Override
	public void onFinish(WinHttpRequest req)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void onError(int errorCode, WinHttpRequest req)
	{

		Intent intent = new Intent(NET_ERROR);
		BeautyApplication.instance.sendBroadcast(intent);
	}

	public ArrayList<WinImageDb> getImageDbList()
	{
		return imageDbList;
	}

	private void copyFile()
	{
		// 把asset中的文件复制到sd卡上
		String dataPath = FileData.getDataPath();
		dataPath = dataPath + WinImageDb.getDefaultOne().name;
		File file = new File(dataPath);
		if (file.exists())
		{
			// 存在初始文件，不管
		} else
		{
			// 复制文件

			BufferedInputStream dis = null;
			InputStream is = null;
			BufferedOutputStream dos = null;
			try
			{
				is = BeautyApplication.instance.getAssets().open(WinImageDb.getDefaultOne().name);

				dis = new BufferedInputStream(is);

				dos = new BufferedOutputStream(new FileOutputStream(file));
				FileData.doCopy(dis, dos);
				dis.close();
				dos.close();

			} catch (Exception e)
			{
				// TODO: handle exception
			}
			// 加载缩略图
			final ThumbManager tm = new ThumbManager();
			tm.openDb(file.getAbsolutePath());
			tm.checkThumb(new Handler(Looper.getMainLooper())
			{
				public void handleMessage(Message msg)
				{
					switch (msg.what)
					{
					case ThumbManager.DECODE_FAILED:
					case ThumbManager.DECODe_FINISHED:
						tm.destroy();
						break;

					}
				}
			});
	
		}
		Intent intent = new Intent(COPY_FILE_FINISH);
		BeautyApplication.instance.sendBroadcast(intent);
	}

	private void initFile()
	{
		// Thread t = new Thread()
		// {
		// public void run()
		// {
//		Log.d(TAG, "copyFile start");

//		Log.d(TAG, "checkFile start");
		checkFile();
//		Log.d(TAG, "sendBroadcast ");

		// }
		// };
		// t.start();
	}

	private void loadDefaultDb()
	{
		try
		{
			InputStream is = BeautyApplication.instance.getAssets().open(DEFAULT_LOCAL_NAME);
			SAXImageDBService service = new SAXImageDBService();
			service.parse(is, handlerLocal);
		} catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void checkFile()
	{

		int size = AvailableDB.getInstance().getImageDbList().size();
		List<WinImageDb> list = AvailableDB.getInstance().getImageDbList();
		for (int i = 0; i < size; i++)
		{
			WinImageDb db = (WinImageDb) list.get(i);
			String dbPath = FileData.getDataPath() + "/" + db.name;
			File file = new File(dbPath);
			if (file.exists())
			{
				db.installStatus = WinImageDb.INSTALLED;
			}
		}
	}

	public boolean isInitedOk()
	{
		return initedOk;
	}

	private final void setInitedOk(boolean initedOk)
	{
		this.initedOk = initedOk;
	}

	public WinImageDb getHeader()
	{
		if( header == null)
		{
			header = WinImageDb.getDefaultOne();
		}
		return header;
	}

}
