package org.androidbook.netdata;

import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.http.HttpEntity;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;

import org.androidbook.gallery.beauty.BeautyApplication;
import org.androidbook.gallery.beauty.R;
import org.androidbook.gallery.beauty.ui.ImageDBListActivity;
import org.androidbook.gallery.constants.NetConstants;
import org.androidbook.net.Constant;
import org.androidbook.net.HttpController;
import org.androidbook.net.IHttpListener;
import org.androidbook.net.WinHttpRequest;
import org.androidbook.netdata.xml.data.WinImageDb;
import org.androidbook.utils.FileData;
import org.androidbook.utils.FileData.DownloadCancelCommander;

public class DownloadManager implements IHttpListener
{
	protected static final String TAG = "DownloadManager";
	private static final long MIN_REFRESH_INTERVAL = 2000;
	private HttpController httpController;

	private NotificationManager manager = null;
	private Notification notification;

	private RemoteViews remote;
	public static final int DOWNLOADING = 0;
	public static final int DOWNLOAD_FINISH = 1;
	public static final int DOWNLOAD_FAILED = 2;
	public static final int DOWNLOAD_CANCEL = 3;
	private Context service = null;
	private long lastRefreshTime;
	public static final String DOWNLOAD_STATUS_UPDATE = "com.androidbook.gallery.DOWNLOAD_STATUS_UPDATE";
	// private AtomicInteger downloadTaskCount = new AtomicInteger(0);
	private ArrayList<WinHttpRequest> taskdb = new ArrayList(20);

	private Handler downloadHandler = new Handler()
	{
		public void handleMessage(Message msg)
		{
			int size = msg.arg1;
			int total = msg.arg2;
			WinHttpRequest wr = (WinHttpRequest) msg.obj;
			WinImageDb db = (WinImageDb) wr.tag;
			switch (msg.what)
			{
			case DOWNLOADING:
				long currentTime = System.currentTimeMillis();
				if (currentTime - lastRefreshTime > MIN_REFRESH_INTERVAL)
				{
					addNotify(db, total, size);
					lastRefreshTime = currentTime;
					// Log.v(TAG, "DOWNLOADING");
				}
				break;
			case DOWNLOAD_FINISH:
				manager.cancel(db.id);
				String s = db.discription + service.getString(R.string.download_finish);

				Toast.makeText(service, s, Toast.LENGTH_LONG).show();
				db.installStatus = WinImageDb.INSTALLED;
				sendBroadcastDownloadStatusUpdate();
				taskdb.remove(wr);
				break;
			case DOWNLOAD_FAILED:
				manager.cancel(db.id);
				Toast.makeText(service, R.string.download_failed, Toast.LENGTH_LONG).show();
				db.installStatus = WinImageDb.UNEXISTED;
				sendBroadcastDownloadStatusUpdate();
				taskdb.remove(wr);
				break;
			case DownloadManager.DOWNLOAD_CANCEL:
				manager.cancel(db.id);
				Toast.makeText(service, R.string.download_canceled, Toast.LENGTH_LONG).show();
				db.installStatus = WinImageDb.UNEXISTED;
				sendBroadcastDownloadStatusUpdate();
				taskdb.remove(wr);
				break;
			}
		}
	};

	private void sendBroadcastDownloadStatusUpdate()
	{
		// downloadTaskCount.getAndDecrement();
		Intent i = new Intent(DOWNLOAD_STATUS_UPDATE);
		service.sendBroadcast(i);
	}

	public DownloadManager(Context context)
	{
		service = context;
		httpController = new HttpController(Constant.HTTP_DATA_THREAD_NUMBER);
		manager = (NotificationManager) BeautyApplication.instance.getSystemService(BeautyApplication.instance.NOTIFICATION_SERVICE);
		remote = new RemoteViews(BeautyApplication.instance.getPackageName(), R.layout.progress_notify);
		notification = new Notification();
		notification.icon = R.drawable.ic_launcher;
		notification.flags = Notification.DEFAULT_SOUND | Notification.FLAG_NO_CLEAR;
		Intent intent = new Intent(service, ImageDBListActivity.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		PendingIntent pIntent = PendingIntent.getActivity(service, 0, intent, 0);
		notification.contentIntent = pIntent;
		remote.setImageViewResource(R.id.image, R.drawable.ic_launcher);//

	}

	public int getTaskCount()
	{
		return taskdb.size();
	}

	public void download(WinImageDb db)
	{
		String url = NetConstants.REQUEST_DATA_URL + db.name;
		WinHttpRequest wr = new WinHttpRequest();
		wr.url = url;
		wr.listener = this;
		wr.tag = db;
		httpController.send(wr);
		db.installStatus = WinImageDb.DOWNLOADING;
		// downloadTaskCount.getAndIncrement();
		addNotify(db, 100, 0);
		taskdb.add(wr);
	}

	public void cancelDownload(WinImageDb db)
	{
		db.installStatus = WinImageDb.DOWNLOAD_CANCELING;
		WinHttpRequest cancelTask = findRequestByDb(db);
		if(httpController.cancel(cancelTask) )
		{
			Message msg = Message.obtain();
			msg.what=DOWNLOAD_CANCEL;
			msg.obj = cancelTask;
			downloadHandler.sendMessage(msg);
		}
		
		manager.cancel(db.id);
		taskdb.remove(cancelTask);
	}

	public WinHttpRequest findRequestByDb(WinImageDb db)
	{
		for (int i = 0; i < taskdb.size(); i++)
		{
			if (db.equals(taskdb.get(i).tag))
			{
				return taskdb.get(i);
			}
		}
		return null;
	}

	public void cancelAll()
	{
		for (int i = 0; i < taskdb.size(); i++)
		{
			cancelDownload((WinImageDb) taskdb.get(i).tag);
			manager.cancelAll();
		}

	}

	public void destroy()
	{
		taskdb.clear();
		manager = null;
		remote = null;
		notification = null;
		httpController.close();
		httpController = null;
		service = null;
	}

	public void addNotify(WinImageDb db, int totalsize, int downloadsize)
	{

		// ProgressBar pb = null;
		// pb.setMax(max)
		// ;
		// pb.setProgress(progress);

		remote.setTextViewText(R.id.tv, db.discription);
		remote.setInt(R.id.pb, "setMax", totalsize);
		remote.setInt(R.id.pb, "setProgress", downloadsize);
		notification.contentView = remote;

		manager.notify(db.id, notification);
	}

	@Override
	public void handleData(HttpEntity res, WinHttpRequest request) throws Exception
	{

		// Log.v("Test", "handleData"+ request.url);
		final HttpEntity response = res;
		final WinHttpRequest req = request;

		Uri uri = Uri.parse(req.url);
		String filename = uri.getLastPathSegment();
		String filePath = FileData.getDataPath() + "/" + filename;
		FileData.writeFileFromInputStream(response, filePath, downloadHandler, request, cancelCommander);

	}

	FileData.DownloadCancelCommander cancelCommander = new DownloadCancelCommander()
	{

		@Override
		public boolean isCancel(WinHttpRequest request)
		{
			WinImageDb db = (WinImageDb) request.tag;
			if (db.installStatus == WinImageDb.DOWNLOAD_CANCELING)
			{
				return true;
			}
			return false;
		}

	};

	@Override
	public void onFinish(WinHttpRequest req)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void onError(int errorCode, WinHttpRequest req)
	{
		Message msg = Message.obtain();
		msg.what = DOWNLOAD_FAILED;
		msg.arg1 = errorCode;
		msg.obj = req;
		downloadHandler.sendMessage(msg);
		// Log.v("onError:", "onError:" + errorCode);
	}
}
