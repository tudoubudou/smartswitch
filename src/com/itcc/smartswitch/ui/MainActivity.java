package com.itcc.smartswitch.ui;

import java.text.SimpleDateFormat;
import java.util.Date;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.format.Time;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.TimePicker.OnTimeChangedListener;
import android.widget.Toast;

import com.itcc.smartswitch.R;
import com.itcc.utils.BusinessShardPreferenceUtil;

public class MainActivity extends Activity {
	Button btn1;
	Button set;
	Button reset;
	Button setTime;
	TextView text1;
	EditText home;
	EditText office;
	TimePicker tm;
	public final static String KEY = "SwitchStatus";
	public final static String KEY_OFFICE_SSID = "key_office";
	public final static String KEY_HOME_SSID = "key_home";
	public final static String KEY_HOUR = "key_hour";
	public final static String KEY_MINUTE = "key_minute";
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
		setTime = (Button)findViewById(R.id.button_set);
		reset = (Button)findViewById(R.id.button3);
		tm = (TimePicker)findViewById(R.id.timePicker1);
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
				if(office_ssid != null && !office_ssid.equals("")){
					BusinessShardPreferenceUtil.setString(MainActivity.this,KEY_OFFICE_SSID,office_ssid);
				}
				String home_ssid = home.getText().toString();
				if(home_ssid != null && !home_ssid.equals("")){
					BusinessShardPreferenceUtil.setString(MainActivity.this,KEY_HOME_SSID,home_ssid);
				}
				String home = BusinessShardPreferenceUtil.getString(MainActivity.this,MainActivity.KEY_HOME_SSID,"Tenda_1BE9E8");
				String office = BusinessShardPreferenceUtil.getString(MainActivity.this,MainActivity.KEY_OFFICE_SSID,"Baiyi_Mobile");
				text1.setText( "Office:"+office +" Home:"+home);
			}
		});
		reset.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				BusinessShardPreferenceUtil.setString(MainActivity.this,KEY_OFFICE_SSID,"Baiyi_Mobile");
				BusinessShardPreferenceUtil.setString(MainActivity.this,KEY_HOME_SSID,"Tenda_1BE9E8");
				text1.setText( "Office:"+"Baiyi_Mobile" +" Home:"+"Tenda_1BE9E8");
			}
		});
		tm.setIs24HourView(true);
		int hour = (int)BusinessShardPreferenceUtil.getLong(MainActivity.this, KEY_HOUR, 22);
		int minute = (int)BusinessShardPreferenceUtil.getLong(MainActivity.this, KEY_MINUTE, 30);
		tm.setCurrentHour(hour);
		tm.setCurrentMinute(minute);
		tm.setOnTimeChangedListener(new OnTimeChangedListener(){
			@Override
			public void onTimeChanged(TimePicker view, int hourOfDay, int minute) {
			}
		});
		setTime.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				Time t = new Time();
				t.setToNow();
				t.hour = tm.getCurrentHour();
				t.minute = tm.getCurrentMinute();
				long setTime = t.toMillis(true);
				long currentTime = System.currentTimeMillis();
				if(setTime < currentTime){
					setTime += 24 * 60 * 60 * 1000;
					cancelAlarm(MainActivity.this);
					setAlarm(MainActivity.this,setTime);
				}else{
					cancelAlarm(MainActivity.this);
					setAlarm(MainActivity.this,setTime);
				}
				SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd-HH:mm");  
		        String hint=format.format(new Date(setTime));
				Toast.makeText(MainActivity.this, "set Alarm at " + hint, Toast.LENGTH_LONG).show();
				BusinessShardPreferenceUtil.setLong(MainActivity.this, KEY_HOUR, tm.getCurrentHour());
				BusinessShardPreferenceUtil.setLong(MainActivity.this, KEY_MINUTE, tm.getCurrentMinute());
			}
		});

	}
	public void setAlarm(Context context,long setTime) {
//		Log.i(TAG, "refresh alarm");
		AlarmManager am = (AlarmManager) context
				.getSystemService(Context.ALARM_SERVICE);
		Intent intent = new Intent("com.itcc.smartswitch.action.ALARM");
		PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 1,
				intent, PendingIntent.FLAG_CANCEL_CURRENT);
		am.set(AlarmManager.RTC, setTime,
				pendingIntent);
	}
	public void cancelAlarm(Context context) {
//		Log.i(TAG, "refresh alarm");
		AlarmManager am = (AlarmManager) context
				.getSystemService(Context.ALARM_SERVICE);
		Intent intent = new Intent("com.itcc.smartswitch.action.ALARM");
		PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 1,
				intent, PendingIntent.FLAG_CANCEL_CURRENT);
		am.cancel(pendingIntent);
	}
	

}
