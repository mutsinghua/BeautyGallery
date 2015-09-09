package org.androidbook.gallery.beauty.ui;

import org.androidbook.gallery.beauty.R;
import org.androidbook.gallery.db.LocalSqlDb;

import android.os.Bundle;
import android.text.Html;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.method.LinkMovementMethod;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.TextView;

public class HelpActivity extends BaseActivity
{

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
	
	
		setContentView(R.layout.help);
		TextView textview =(TextView) findViewById(R.id.help);
		String help = getString(R.string.help_text);
		help = help.replace("MAX_POINT", String.valueOf(LocalSqlDb.MAX_POINTS));
		help = help.replace('[', '<').replace(']', '>');	
		help = help.replace("\t", "&nbsp;&nbsp;&nbsp;&nbsp;");
		help = help.replace("\n", "<br />");
		textview.setText(Html.fromHtml(help));
		textview.setMovementMethod(LinkMovementMethod.getInstance());
		
	}

}
