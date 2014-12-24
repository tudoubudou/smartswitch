package com.itcc.smartswitch;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;
import android.widget.Toast;

import com.itcc.smartswitch.network.DownloadFinishTask;
import com.itcc.smartswitch.network.DownloadNotifManager;
import com.itcc.smartswitch.ui.MainActivity;
import com.itcc.smartswitch.utils.Constant;
import com.itcc.utils.BusinessShardPreferenceUtil;
import com.itcc.utils.Logger;

public class EventReceiver extends BroadcastReceiver {
	private static final String TAG = "EventReceiver";
	private static final long INTERVAL =  24 * 60 * 60 * 1000;


	@Override
	public void onReceive(Context context, Intent intent) {

		Boolean status_on = BusinessShardPreferenceUtil.getBoolean(context, MainActivity.KEY, true);
		String home = BusinessShardPreferenceUtil.getString(context,MainActivity.KEY_HOME_SSID,"Tenda_1BE9E8");
		String office = BusinessShardPreferenceUtil.getString(context,MainActivity.KEY_OFFICE_SSID,"Baiyi_Mobile");
		long alarm_time = BusinessShardPreferenceUtil.getLong(context,MainActivity.KEY_ALARM,0);
		long current_time = System.currentTimeMillis();
		boolean sleepping_time = false;
		if(current_time > alarm_time && current_time < alarm_time + 8 * 60 * 60 * 1000){
		    sleepping_time = true;
		}
		String action = intent.getAction();
		Logger.d(TAG, "onReceive intent is " + intent + ", action is " + action);
		if (action.equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
			if (status_on && !sleepping_time && com.itcc.utils.PhoneInfoStateManager
					.isNetworkAvailable(context)) {
				if(!com.itcc.utils.PhoneInfoStateManager.isWifiConnection(context)){
					AudioManager am = (AudioManager) context
							.getSystemService(Context.AUDIO_SERVICE);
					switch (getActualRingState(context)) {
					case SwitchState.STATE_RINGMODE_SILENT:
//						am.setRingerMode(AudioManager.RINGER_MODE_VIBRATE);
//						break;
					case SwitchState.STATE_RINGMODE_VIBRATE:
						am.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
						Toast.makeText(context, "Noly 3g Change To Normal!", Toast.LENGTH_LONG).show();
						break;
					case SwitchState.STATE_RINGMODE_NORMAL:
//						am.setRingerMode(AudioManager.RINGER_MODE_SILENT);
						break;
					}
				}else{
					WifiManager manager = (WifiManager) context
			                .getSystemService(Context.WIFI_SERVICE);
			        if(null != manager){
			            WifiInfo activeInfo = manager.getConnectionInfo();
			            String wifiname = activeInfo.getSSID();
						Logger.d(TAG, "ssid is " + wifiname);
						AudioManager am = (AudioManager) context
								.getSystemService(Context.AUDIO_SERVICE);
			            if(wifiname.indexOf(office)!=-1){
							am.setRingerMode(AudioManager.RINGER_MODE_VIBRATE);
							Toast.makeText(context, "In Office Change To Vibratel!", Toast.LENGTH_LONG).show();
			            }else if(wifiname.indexOf(home)!=-1){
			            	am.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
							Toast.makeText(context, "In Home Change To Normal!", Toast.LENGTH_LONG).show();
			            }
			        }
				}
			} else if(status_on && !sleepping_time) {
				AudioManager am = (AudioManager) context
						.getSystemService(Context.AUDIO_SERVICE);
				Logger.d(TAG, "ring state is " + getActualRingState(context));
				switch (getActualRingState(context)) {
				case SwitchState.STATE_RINGMODE_SILENT:
//					am.setRingerMode(AudioManager.RINGER_MODE_VIBRATE);
//					break;
				case SwitchState.STATE_RINGMODE_VIBRATE:
					am.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
					Toast.makeText(context, "no net change to normal!", Toast.LENGTH_LONG).show();
					break;
				case SwitchState.STATE_RINGMODE_NORMAL:
//					am.setRingerMode(AudioManager.RINGER_MODE_SILENT);
					break;
				}
			}
		}else if (action.equals(Intent.ACTION_BOOT_COMPLETED)) {
		    if(status_on){
		        Intent i = new Intent("com.itcc.smartswitch.action.LAUNCH_SERVICE");
		        context.startService(i);
//			refreshAlarm(context);
		    }
		}else if(action.equals("com.itcc.smartswitch.action.ALARM")){
		    if(status_on){
		        AudioManager am = (AudioManager) context
		                .getSystemService(Context.AUDIO_SERVICE);
		        am.setRingerMode(AudioManager.RINGER_MODE_VIBRATE);
		        Toast.makeText(context, "time up! set to Vibrate!", Toast.LENGTH_LONG).show();
		        refreshAlarm(context);
		    }
		}else if (action.equals(Constant.ACTION_DOWNLOAD_PROGRESS)) {
            long id = intent.getLongExtra(Constant.EXTRA_ID, -1);
            long total = intent.getLongExtra(Constant.EXTRA_TOTAL, 0);
            long current = intent.getLongExtra(Constant.EXTRA_CURRENT, 0);
            DownloadNotifManager.getInstance(context)
                    .updateProgressNotification(id, total, current);
        } else if (action.equals(Constant.ACTION_DOWNLOAD_COMPOLETED)) {
            long id = intent.getLongExtra(Constant.EXTRA_ID, -1);
            int result = intent.getIntExtra(Constant.EXTRA_RESULT, -1);
            DownloadNotifManager.getInstance(context).updateCompletedNotification(id, result);
            String path = intent.getStringExtra(Constant.EXTRA_DEST_PATH);
            int need_notify = intent.getIntExtra(Constant.EXTRA_NOTIFY_TYPE, -1);
            if (need_notify != -1) {
                DownloadFinishTask task = new DownloadFinishTask(context, result, need_notify, path);
                Constant.mExecutorService.submit(task);
            }
        }else if (action.equals(Constant.ACTION_DOWNLOAD_SHOWFAILMSG)) {
            String s = intent.getStringExtra(Constant.FAIL_MSG);
            if (s != null && !s.equals("")){
              Toast.makeText(context, s, Toast.LENGTH_LONG).show();
            }
        }
	}

	public int getActualRingState(Context context) {
		AudioManager am = (AudioManager) context
				.getSystemService(Context.AUDIO_SERVICE);

		int mode = SwitchState.STATE_RINGMODE_NORMAL;
		if (am != null) {
			switch (am.getRingerMode()) {
			case AudioManager.RINGER_MODE_NORMAL:
				mode = SwitchState.STATE_RINGMODE_NORMAL;
				break;
			case AudioManager.RINGER_MODE_SILENT:
				mode = SwitchState.STATE_RINGMODE_SILENT;
				break;
			case AudioManager.RINGER_MODE_VIBRATE:
				mode = SwitchState.STATE_RINGMODE_VIBRATE;
				break;
			}
		}
		return mode;
	}
	public void refreshAlarm(Context context) {
		Log.i(TAG, "refresh alarm");
		long now_time = System.currentTimeMillis();
		AlarmManager am = (AlarmManager) context
				.getSystemService(Context.ALARM_SERVICE);
		Intent intent = new Intent("com.itcc.smartswitch.action.ALARM");
		PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 1,
				intent, PendingIntent.FLAG_CANCEL_CURRENT);
		am.set(AlarmManager.RTC, now_time + INTERVAL,
				pendingIntent);
	}
//    public static String getModel() {
//        final String prop_key = "ro.product.model";
//        String model = SystemProperties.get(prop_key, "nullmodel");
//        Log.d(TAG, "getModel, it is " + model);
//        return model;
//    }
//    public static long getInterval() {
//        final String prop_key = "log.com.bsf.system.interval";
//        long interval = Long.valueOf(SystemProperties.get(prop_key, String.valueOf(INTERVAL)));
//        return interval;
//    }
    public static String getSettedModel(Context context, String metakey) {
        ApplicationInfo info;
        String metavalue = "100c";
        try {
            info = context.getPackageManager().getApplicationInfo(
                    context.getPackageName(), PackageManager.GET_META_DATA);
            if(info!=null && info.metaData != null) {
                metavalue = info.metaData.getString(metakey);
            }
        } catch (NameNotFoundException e) {
            e.printStackTrace();
        }
        return metavalue;
    }

}
