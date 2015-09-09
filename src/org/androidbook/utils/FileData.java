package org.androidbook.utils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;

import org.androidbook.gallery.beauty.BeautyApplication;
import org.androidbook.gallery.db.AvailableDB;
import org.androidbook.net.WinHttpRequest;
import org.androidbook.netdata.DownloadManager;
import org.apache.http.HttpEntity;


import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class FileData
{
	private static DecimalFormat df = new DecimalFormat("##.00");
	private final static String[] units = new String[]
	{ "GB", "MB", "KB", "B" };
	private final static long[] dividers = new long[]
	{ 1024 * 1024 * 1024, 1024 * 1024, 1024, 1 };
	private static final String TAG = "FileData";

	public static String byteToString(final long value)
	{
		if (value < 1)
			return "0B";
		String result = null;
		for (int i = 0; i < dividers.length; i++)
		{
			final long divider = dividers[i];
			if (value >= divider)
			{
				result = format(value, divider, units[i]);
				break;
			}
		}
		return result;
	}

	private static String format(final long value, final long divider, final String unit)
	{

		final double result = divider > 1 ? (double) value / (double) divider : (double) value;

		String str = df.format(result) + " " + unit;
		return str;
	}

	public static byte[] readFile(String fileName)
	{
		ByteArrayOutputStream bufferReceiver = new ByteArrayOutputStream(200 * 1024);
		try
		{
			FileInputStream fis = new FileInputStream(fileName);
			byte[] buffer = new byte[8096];
			int readNum = fis.read(buffer);
			while (readNum > 0 && readNum <= buffer.length)
			{
				bufferReceiver.write(buffer, 0, readNum);
				readNum = fis.read(buffer);
			}
			bufferReceiver.flush();
			return bufferReceiver.toByteArray();
		} catch (FileNotFoundException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	public static boolean writeDataToNewFile(String fileFullpath, byte[] data)
	{
		File file = new File(fileFullpath);
		if (file.exists())
		{
			file.delete();
		}

		try
		{
			FileOutputStream fos = new FileOutputStream(file);
			fos.write(data);
			fos.flush();
			fos.close();
			return true;
		} catch (FileNotFoundException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return false;

	}

	public static String getDataPath()
	{
		return FileData.getStorePath(AvailableDB.DATA_PATH) + "/";
	}

	/**
	 * 有SD卡，先存sd卡
	 * 
	 * @return
	 */
	public static String getStorePath(String path)
	{

		// 获取SdCard状态

		String state = android.os.Environment.getExternalStorageState();

		// 判断SdCard是否存在并且是可用的

		if (android.os.Environment.MEDIA_MOUNTED.equals(state))
		{

			if (android.os.Environment.getExternalStorageDirectory().canWrite())
			{

				File file = new File(android.os.Environment.getExternalStorageDirectory().getPath() + File.separator + path);
				if (!file.exists())
				{
					file.mkdirs();
				}
				return file.getAbsolutePath();

			}

		}
		else
		{
//			Toast.makeText(BeautyApplication.instance, R.string.sdcard_no_exist, Toast.LENGTH_LONG).show();
			
		}
		return BeautyApplication.instance.getFilesDir().getAbsolutePath();

	}

	public static Bitmap getLimitBitmap(byte[] bitmapData, int maxWidth, int maxHeight)
	{
		Options op = new Options();
		op.inJustDecodeBounds = true;
		Bitmap thumb = BitmapFactory.decodeByteArray(bitmapData, 0, bitmapData.length, op);
		float scalw = (float) (op.outWidth *1.0/ maxWidth);
		float scalh = (float) (op.outHeight * 1.0 / maxHeight);
		int scal = (int) Math.ceil( (scalw > scalh ? scalw : scalh));
		op.inJustDecodeBounds = false;
//		Log.d(TAG, "op.outWidth:"+op.outWidth);
//		Log.d(TAG, "op.outHeight:"+op.outHeight);
		
		op.inSampleSize =  get2Power(scal);
//		Log.d(TAG, "scal:"+op.inSampleSize);
		try
		{
		thumb = BitmapFactory.decodeByteArray(bitmapData, 0, bitmapData.length, op);
		}
		catch (OutOfMemoryError e) {
			op.inSampleSize *=2;
			thumb = BitmapFactory.decodeByteArray(bitmapData, 0, bitmapData.length, op);
		}
//		Log.d(TAG, "out:"+thumb.getWidth()+" "+thumb.getHeight());
		return thumb;
	}
	
	/**
	 * 生成缩略图
	 * 
	 * @param bigdata
	 * @param targetWidth
	 * @param targetHeight
	 * @return
	 */

	public static Bitmap genThumb(byte[] bigdata, int targetWidth, int targetHeight)
	{

		Options op = new Options();
		op.inJustDecodeBounds = true;
		Bitmap thumb = BitmapFactory.decodeByteArray(bigdata, 0, bigdata.length, op);
		int scalw = op.outWidth / targetWidth;
		int scalh = op.outHeight / targetHeight;
		int scal = scalw > scalh ? scalh : scalw;
		op.inJustDecodeBounds = false;
		op.inSampleSize = get2Power(scal);
		thumb = BitmapFactory.decodeByteArray(bigdata, 0, bigdata.length, op);
		return thumb;
	}

	private static  int get2Power(int i)
	{
		int p = 1;
		while (p < i)
		{
			p = p << 1;
		}
		return p;
	}
	
	/**
	 * 序列化图片
	 * @param bitmap
	 * @return
	 */
	public static byte[] serializeBitmap(Bitmap bitmap)
	{
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(baos);
		bitmap.compress(Bitmap.CompressFormat.JPEG, 80, dos);
		return baos.toByteArray();
	}
	
	/**
	 * 序列化图片
	 * @param bitmap
	 * @return
	 */
	public static byte[] serializeBitmapHQ(Bitmap bitmap)
	{
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(baos);
		bitmap.compress(Bitmap.CompressFormat.PNG, 80, dos);
		return baos.toByteArray();
	}
	/**
	 * 读取流到数组中,读完会关流
	 * 
	 * @param is
	 * @return
	 */
	public static byte[] readByteFromInputStream(InputStream is)
	{

		BufferedInputStream bis = new BufferedInputStream(is);
		byte[] buf = new byte[4048];
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try
		{
			int n = 0;

			while ((n = bis.read(buf)) > 0)
			{
				baos.write(buf, 0, n);
			}
			byte[] retdata = baos.toByteArray();
			return retdata;
		} catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		} finally
		{
			try
			{
				if (baos != null)
				{
					baos.close();
				}
			} catch (IOException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			try
			{
				if (bis != null)
				{
					bis.close();
				}
			} catch (IOException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			try
			{
				if (is != null)
				{
					is.close();
				}
			} catch (IOException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		// String s = new String(retdata, "utf-8");
		// Log.v("Test", s);

	}

	public static interface DownloadCancelCommander
	{
		public boolean isCancel(WinHttpRequest request);
	}
	
	/**
	 * 读取流到文件中,读完会关流
	 * 
	 * @param is
	 * @return
	 * @throws Exception 
	 */
	public static void writeFileFromInputStream(HttpEntity entity, String filePath, Handler callback,WinHttpRequest request, DownloadCancelCommander canceler) throws Exception
	{
		if (entity == null || filePath == null)
		{
			return;
		}
		long fileTotalSize = entity.getContentLength();
		if (fileTotalSize < 0)
		{
			fileTotalSize = (20 << 20);
		}

		byte[] buf = new byte[4048*2];
		File file = new File(filePath);
		int received = 0;
		if (file.exists())
		{
			file.delete();
		}
		FileOutputStream baos = null;
		BufferedInputStream bis = null;
	
			bis = new BufferedInputStream(entity.getContent());
			baos = new FileOutputStream(filePath);
			int n = 0;

			while ((n = bis.read(buf)) > 0)
			{
				if( (canceler != null && canceler.isCancel(request)) || request.iscanceled)
				{
					Message msg = Message.obtain();
					msg.what= DownloadManager.DOWNLOAD_CANCEL;
					msg.arg1 = received;
					msg.arg2 = (int) fileTotalSize;
					msg.obj = request;
					callback.sendMessage(msg);
					return;
				}
				baos.write(buf, 0, n);
				received += n;
				Message msg = Message.obtain();
				msg.what= DownloadManager.DOWNLOADING;
				msg.arg1 = received;
				msg.arg2 = (int) fileTotalSize;
				msg.obj = request;
				callback.sendMessage(msg);
				
					if( (canceler != null && canceler.isCancel(request)) || request.iscanceled)
					{
						msg = Message.obtain();
						msg.what= DownloadManager.DOWNLOAD_CANCEL;
						msg.arg1 = received;
						msg.arg2 = (int) fileTotalSize;
						msg.obj = request;
						callback.sendMessage(msg);
						return;
					}
				
			}
			Message msg = Message.obtain();
			msg.what= DownloadManager.DOWNLOAD_FINISH;
			msg.arg1 = received;
			msg.arg2 = (int) fileTotalSize;
			msg.obj = request;
			callback.sendMessage(msg);
			
			baos.flush();
		 
		
		
			try
			{
				if (baos != null)
				{
					baos.close();
				}
			} catch (IOException e)
			{
				e.printStackTrace();
			}
			try
			{
				if (bis != null)
				{
					bis.close();
				}
			} catch (IOException e)
			{
				e.printStackTrace();
			}
	
//			try
//			{
//				if (entity.getContent() != null)
//				{
//					entity.getContent().close();
//				}
//			} catch (IOException e)
//			{
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
	

		// String s = new String(retdata, "utf-8");
		// Log.v("Test", s);

	}

	public static String tranferUrltoLocalPath(String url)
	{
		Uri uri = Uri.parse(url);
		String icon = uri.getPath();
		icon = icon.replace("/", "r");
		icon = icon.replace("\\", "e");
		icon = icon.replace(".", "x");
		icon = icon.replace("?", "z");
		icon = icon.replace("=", "o");
		icon = icon.replace(":", "u");
		return icon;

	}

	/**
	 * 复制单个文件
	 * 
	 * @param sourcePath
	 *            原路径
	 * @param targetPath
	 *            目标路径
	 */
	public static void copyFile(String sourcePath, String targetPath)
	{
		File source = new File(sourcePath);
		if (!source.exists())
		{
			return;
		}
		File target = new File(targetPath);
		if (target.exists())
		{
			target.delete();
		}
		BufferedOutputStream bufferedOutputStream = null;
		BufferedInputStream bufferedInputStream = null;
		try
		{
			bufferedOutputStream = new BufferedOutputStream(new FileOutputStream(target));
			bufferedInputStream = new BufferedInputStream(new FileInputStream(source));

			doCopy(bufferedInputStream, bufferedOutputStream);

		} catch (FileNotFoundException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally
		{
			if (bufferedInputStream != null)
			{
				try
				{
					bufferedInputStream.close();
				} catch (IOException e)
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			if (bufferedOutputStream != null)
			{
				try
				{
					bufferedOutputStream.close();
				} catch (IOException e)
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

		}
	}

	/**
	 * 复制一个文件
	 */
	public static void doCopy(BufferedInputStream dis, BufferedOutputStream dos)
	{
		byte[] buf = new byte[64 * 1024];
		try
		{
			int r = dis.read(buf);
			while (r != -1)
			{
				dos.write(buf, 0, r);
				r = dis.read(buf);

			}

		} catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
