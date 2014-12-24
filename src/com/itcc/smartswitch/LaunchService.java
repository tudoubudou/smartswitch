package com.itcc.smartswitch;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import com.itcc.smartswitch.ui.MainActivity;
import com.itcc.smartswitch.utils.Constant;
import com.itcc.utils.BusinessShardPreferenceUtil;


public class LaunchService extends Service {
	
    private final String TAG = "LaunchService";
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
	    refreshAlarm(this);
		return START_STICKY;
	}

	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		return null;
	}
	public void refreshAlarm(Context context) {
        Log.i(TAG, "refresh alarm in service");
        long alarm = BusinessShardPreferenceUtil.getLong(context,MainActivity.KEY_ALARM,0);
        long current = System.currentTimeMillis();
        if(alarm > current){
            AlarmManager am = (AlarmManager) context
                    .getSystemService(Context.ALARM_SERVICE);
            Intent intent = new Intent(Constant.ACTION_SLEEP_ALARM);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 1,
                    intent, PendingIntent.FLAG_CANCEL_CURRENT);
            am.set(AlarmManager.RTC, alarm,pendingIntent);
            Intent exit_sleep_intent = new Intent(Constant.ACTION_EXIT_SLEEP);
            PendingIntent exit_sleep_pendingIntent = PendingIntent.getBroadcast(context, 2,
                    exit_sleep_intent, PendingIntent.FLAG_CANCEL_CURRENT);
            am.set(AlarmManager.RTC, alarm+MainActivity.INTERVAL,exit_sleep_pendingIntent);
        }
    }

}
