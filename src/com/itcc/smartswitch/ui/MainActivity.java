package com.itcc.smartswitch.ui;

import com.itcc.smartswitch.R;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.itcc.utils.*;

public class MainActivity extends Activity {
	Button btn1;
	Button set;
	Button reset;
	TextView text1;
	EditText home;
	EditText office;
	public final static String KEY = "SwitchStatus";
	public final static String KEY_OFFICE_SSID = "key_office";
	public final static String KEY_HOME_SSID = "key_home";
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		this.getWindow().addFlags(Window.FEATURE_NO_TITLE);
		Intent i = new Intent("com.itcc.smartswitch.action.LAUNCH_SERVICE");
		MainActivity.this.startService(i);
		this.setContentView(R.layout.mainlayout);
		text1 = (TextView)findViewById(R.id.textView1);
		office = (EditText)findViewById(R.id.editText1);
		home = (EditText)findViewById(R.id.editText2);
		btn1 = (Button)findViewById(R.id.button1);
		set = (Button)findViewById(R.id.button2);
		reset = (Button)findViewById(R.id.button3);
		Boolean status_on = BusinessShardPreferenceUtil.getBoolean(this, KEY, true);
		if(status_on){
			text1.setText("Status:On");
			btn1.setText("Turn Off");
		}else{
			text1.setText("Status:Off");
			btn1.setText("Turn On");
		}
		btn1.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Boolean status_on = BusinessShardPreferenceUtil.getBoolean(MainActivity.this, KEY, true);

				if(status_on){
					BusinessShardPreferenceUtil.setBoolean(MainActivity.this, KEY,false	);
					text1.setText("Status:Off");
					btn1.setText("Turn On");
				}else{
					BusinessShardPreferenceUtil.setBoolean(MainActivity.this, KEY,true	);
					text1.setText("Status:On");
					btn1.setText("Turn Off");
					
				}
				
			}
		});
		set.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				String office_ssid = office.getText().toString();
				BusinessShardPreferenceUtil.setString(MainActivity.this,KEY_OFFICE_SSID,office_ssid);
				String home_ssid = home.getText().toString();
				BusinessShardPreferenceUtil.setString(MainActivity.this,KEY_HOME_SSID,home_ssid);
				text1.setText( "Office:"+office_ssid +" Home:"+home_ssid);
			}
		});
		reset.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				BusinessShardPreferenceUtil.setString(MainActivity.this,KEY_OFFICE_SSID,"Baiyi_Mobile");
				BusinessShardPreferenceUtil.setString(MainActivity.this,KEY_HOME_SSID,"NEWHOME");
				text1.setText( "Office:"+"Baiyi_Mobile" +" Home:"+"NEWHOME");
			}
		});
		
	}
	

}
