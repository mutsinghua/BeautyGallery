package org.androidbook.gallery.beauty.ui;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;

import com.tencent.mobwin.MobinWINBrowserActivity;
import org.androidbook.gallery.beauty.R;
import org.androidbook.gallery.db.FavoriteDB;
import org.androidbook.gallery.db.ThumbManager;
import org.androidbook.netdata.xml.data.WinImageDb;
import org.androidbook.utils.FileData;

public class ShowFavThumbActivity extends ShowThumbActivity
{


	public static void startActivity(WinImageDb db, Context context)
	{
		winImageDb= db;
		String path = FileData.getDataPath() + db.name;
		thumbManager = new ThumbManager();
		if (thumbManager.openDb(path))
		{
			Intent intent = new Intent(context, ShowFavThumbActivity.class);
			context.startActivity(intent);
		} else
		{
			thumbManager = null;
			BaseActivity.showToast(R.string.file_corrupt);
			db.installStatus = WinImageDb.UNEXISTED;
		}
		
	}

	@Override
	protected void checkValue()
	{
	}
	
	@Override
	protected boolean checkJF()
	{
		
		return true;
	}




	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		
		gv.setOnItemLongClickListener(new OnItemLongClickListener() {

			@Override
			public boolean onItemLongClick(AdapterView<?> parent, View view,
					int position, long id) {
				final int iid = (int) id;
				AlertDialog.Builder al = new Builder(ShowFavThumbActivity.this).setMessage(R.string.fav_delete_sure).setPositiveButton(R.string.ok, new OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						FavoriteDB.getInstance().removeImage((int) iid);
						showToast(R.string.fav_delete_success);
						thumbManager.removeItem(iid);
						ta.notifyDataSetChanged();
					}
				}).setNegativeButton(R.string.cancel, null);
				al.create().show();
				return true;
			}
		});
		
		pointView.setText(R.string.my_fav);
	}

	
	
}
