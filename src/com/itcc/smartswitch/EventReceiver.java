package com.itcc.smartswitch;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.widget.Toast;

import com.itcc.smartswitch.ui.MainActivity;
import com.itcc.utils.BusinessShardPreferenceUtil;
import com.itcc.utils.Logger;

public class EventReceiver extends BroadcastReceiver {
	private static final String TAG = "EventReceiver";

	@Override
	public void onReceive(Context context, Intent intent) {

		Boolean status_on = BusinessShardPreferenceUtil.getBoolean(context, MainActivity.KEY, true);
		String home = BusinessShardPreferenceUtil.getString(context,MainActivity.KEY_HOME_SSID,"NEWHOME");
		String office = BusinessShardPreferenceUtil.getString(context,MainActivity.KEY_OFFICE_SSID,"Baiyi_Mobile");
		if(!status_on){
			return;
		}
		String action = intent.getAction();
		Logger.d(TAG, "onReceive intent is " + intent + ", action is " + action);
		if (action.equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
			if (com.itcc.utils.PhoneInfoStateManager
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
						Toast.makeText(context, "noly 3g change to normal!", Toast.LENGTH_LONG).show();
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
							Toast.makeText(context, "in office change to Vibratel!", Toast.LENGTH_LONG).show();
			            }else if(wifiname.indexOf(home)!=-1){
			            	am.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
							Toast.makeText(context, "IN HOME change to normal!", Toast.LENGTH_LONG).show();
			            }
			        }
				}
			} else {
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
			Intent i = new Intent("com.itcc.smartswitch.action.LAUNCH_SERVICE");
			context.startService(i);
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

}
