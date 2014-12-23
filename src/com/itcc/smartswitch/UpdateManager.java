
package com.itcc.smartswitch;

import java.util.HashMap;

import org.json.JSONObject;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.view.WindowManager;
import android.widget.Toast;

import com.baidu.kirin.CheckUpdateListener;
import com.baidu.kirin.KirinConfig;
import com.baidu.kirin.PostChoiceListener;
import com.baidu.kirin.StatUpdateAgent;
import com.baidu.kirin.objects.KirinCheckState;
import com.itcc.smartswitch.network.DownloadFileManager;
import com.itcc.smartswitch.network.DownloadNotifManager;
import com.itcc.smartswitch.utils.Constant;
import com.itcc.smartswitch.utils.LogEx;
import com.itcc.utils.BusinessShardPreferenceUtil;
import com.itcc.utils.PhoneInfoStateManager;
import com.itcc.utils.StorageUtil;

public class UpdateManager extends BroadcastReceiver {
    private static final String TAG = UpdateManager.class.getSimpleName();
    private static UpdateManager mUpdateManager;
    private Context mContext;

	private NotificationManager mNotifyManager;
	private Dialog mConfirmDialog;
	
    public boolean mUpdateing = false;
    private int mCheckType = Constant.AUTO_CHECK_TYPE;
    private String mUrl;
    private String mTargetMarket;
    private String mCurrentVersion;
    private String mNewVersion;

    private String mSavePath;
    private String mSaveName;

    private static final int DIALOG_UPDATE_CONFIRM = 0;
    private static final int NOTIFICATION_UPDATE_CONFIRM = 1;
    private static final int NO_NEW_VERSION_UPDATE = 2;
    protected static final int CONNECTION_ERROR = 3;

    private static final int UPDATE_WARN_NOTIFICATION_ID = 100001;

    private Handler mHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case DIALOG_UPDATE_CONFIRM:
                    showConfirmDialod();
                    break;
                case NOTIFICATION_UPDATE_CONFIRM:
                    sendConfirmNotification();
                    break;
                case NO_NEW_VERSION_UPDATE:
                    Toast.makeText(mContext, "Already the latest version", Toast.LENGTH_LONG).show();
                    break;
                case CONNECTION_ERROR:
                    Toast.makeText(mContext, "Update failure, please check the network status", Toast.LENGTH_LONG).show();
                default:
                    break;
            }
        }

    };
    
	public void setmContext(Context mContext) {
		this.mContext = mContext;
	}
	
	public Context getmContext() {
		return mContext;
	}

    @Override
    public void onReceive(final Context context, Intent intent) {
        final String action = intent.getAction();
        if (action.equals(Constant.ACTION_UPDATE_CONFIRM)) {
            Message msg = Message.obtain();
            msg.what = DIALOG_UPDATE_CONFIRM;
            mHandler.sendMessage(msg);
        }
    }
    private CheckUpdateListener mCheckUpdateResponse = new CheckUpdateListener(){

        @Override
        public void checkUpdateResponse(KirinCheckState state,
                HashMap<String, String> dataContainer) {
            if (state == KirinCheckState.ALREADY_UP_TO_DATE) {
                LogEx.d(TAG, "stat == KirinCheckState.ALREADY_UP_TO_DATE");
                BusinessShardPreferenceUtil.setBoolean(mContext,
                        Constant.KEY_NEW_VERSION, false);
                if (mCheckType == Constant.CLICK_CHECK_TYPE) {
                    Message msg = Message.obtain();
                    msg.what = NO_NEW_VERSION_UPDATE;
                    mHandler.sendMessage(msg);
                }
            } else if (state == KirinCheckState.ERROR_CHECK_VERSION) {
                LogEx.d(TAG, "KirinCheckState.ERROR_CHECK_VERSION");
                BusinessShardPreferenceUtil.setBoolean(mContext,
                        Constant.KEY_NEW_VERSION, false);
                if (mCheckType == Constant.CLICK_CHECK_TYPE) {
                    Message msg = Message.obtain();
                    msg.what = CONNECTION_ERROR;
                    mHandler.sendMessage(msg);
                }
            } else if (state == KirinCheckState.NEWER_VERSION_FOUND) {
                LogEx.d(TAG, "KirinCheckState.NEWER_VERSION_FOUND" + dataContainer.toString());
                String isForce = dataContainer.get("updatetype");
                String noteInfo = dataContainer.get("note");
                String publicTime = dataContainer.get("time");
                String appUrl = dataContainer.get("appurl");
                String appName = dataContainer.get("appname");
                String newVersionName = dataContainer.get("version");
                String newVersionCode = dataContainer.get("buildid");
                String attachInfo = dataContainer.get("attach");

                // 这些信息都是在mtj.baidu.com上您选择的小流量定制信息
//                utestUpdate.doUpdate(appUrl, noteInfo);
                BusinessShardPreferenceUtil.setBoolean(mContext,
                        Constant.KEY_NEW_VERSION, true);
                mUrl = appUrl;
                mSaveName = appName + ".apk";
//                mTargetMarket = selectMarket(info.getMarket());
                mNewVersion = newVersionName;
                if (mCheckType == Constant.AUTO_CHECK_TYPE) {
                    Message msg = Message.obtain();
                    msg.what = NOTIFICATION_UPDATE_CONFIRM;
                    mHandler.sendMessage(msg);
                } else if (mCheckType == Constant.CLICK_CHECK_TYPE) {
                    Message msg = Message.obtain();
                    msg.what = DIALOG_UPDATE_CONFIRM;
                    mHandler.sendMessage(msg);
                }
            }
        }
        
    };
    protected PostChoiceListener mPostChoiceListener = new PostChoiceListener(){

        @Override
        public void PostUpdateChoiceResponse(JSONObject arg0) {
            // TODO Auto-generated method stub
            
        }
        
    };

    private UpdateManager(Context aContext) {
        mContext = aContext;
        mNotifyManager = (NotificationManager) mContext
                .getSystemService(Context.NOTIFICATION_SERVICE);
        mCurrentVersion = PhoneInfoStateManager.getVersionName(aContext);
    }

    protected void sendConfirmNotification() {
        String notificationTitle = mContext.getString(R.string.app_name);
    	if(new PhoneInfoStateManager().IsSdCardMounted()) {
    		mSavePath = StorageUtil.getVersionUpdateFileDir();
    		
	    	DownloadFileManager.getInstance().insertTask(mContext, mUrl,
	                mSavePath + mSaveName, "application",
	                notificationTitle, "", true, false,
	                Constant.NOTIFY_TYPE_NEW_VERSION_UPDATE_APK);
    	}
    	
        Intent intent = new Intent(Constant.ACTION_UPDATE_CONFIRM);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(mContext, 0, intent, 0);

        Notification notify = new Notification();
        notify.icon = R.drawable.statusbar_download_icon;
        notify.tickerText = mContext.getString(R.string.auto_check_update);
        notify.flags = Notification.FLAG_AUTO_CANCEL;
        String title = mContext.getString(R.string.update_notify);
        String msg = mContext.getString(R.string.notification_update_ask);
        notify.setLatestEventInfo(mContext, title, msg, pendingIntent);
        mNotifyManager.notify(UPDATE_WARN_NOTIFICATION_ID, notify);
    }

    protected void showConfirmDialod() {
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setTitle(R.string.toast_update_tip);
        String msg = mContext.getString(R.string.cur_version_tip) + mCurrentVersion + "\n\n"
                + mContext.getString(R.string.new_version_tip)
                + mNewVersion + ", " + mContext.getString(R.string.update_ask);
        builder.setMessage(msg);
        builder.setPositiveButton("Update", new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                if (!StorageUtil.IsSdCardMounted()) {
                    Toast.makeText(mContext, "Please add SD card.", Toast.LENGTH_SHORT)
                            .show();
                } else {
                	boolean update = mTargetMarket != null && updateFromMarket(mTargetMarket);
                	if(!update) {
                		mSavePath = StorageUtil.getVersionUpdateFileDir();
                        String notificationTitle = mContext.getString(R.string.app_name);
                         long id = DownloadFileManager.getInstance().insertTask(mContext, mUrl,
                                 mSavePath + mSaveName, "application",
                                 notificationTitle, null, false, true,
                                 Constant.NOTIFY_TYPE_NEW_VERSION_UPDATE_APK,false);
                         DownloadNotifManager.getInstance(mContext).addNotification(id, true, null,
                                 true, null, true, null);
                         StatUpdateAgent.postUserChoice(mContext,
                                 KirinConfig.CONFIRM_UPDATE, mPostChoiceListener );
                	}                	
                }
            }
        });
        builder.setNegativeButton("Not Now", new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
//            	SharedPreferences pref = PreferenceManager
//        				.getDefaultSharedPreferences(mContext);
//            	pref.edit().putLong(Constant.CANCEL_UPDATE_TIME_KRY, System.currentTimeMillis()).commit();
            	StatUpdateAgent.postUserChoice(mContext,KirinConfig.LATER_UPDATE, mPostChoiceListener);
            }
        });
        mConfirmDialog = builder.create();
        mConfirmDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
        mConfirmDialog.show();
    }

    public synchronized static UpdateManager getInastance(Context aContext) {
        if (null == mUpdateManager) {
            mUpdateManager = new UpdateManager(aContext);
        }
        return mUpdateManager;

    }

    public int getCheckType() {
        return mCheckType;
    }

    public void setCheckType(int aCheckType) {
        this.mCheckType = aCheckType;
    }

    /**
     * get target market from aMarkets
     * 
     * @param aMarkets
     * @return
     */
    public String selectMarket(String[] aMarkets) {
        if (null == aMarkets) {
            return null;
        } else {
            PackageManager pm = mContext.getPackageManager();
            for (String pk : aMarkets) {
                try {
                    PackageInfo info = pm.getPackageInfo(pk, 0);
                    if(info.applicationInfo != null && info.applicationInfo.enabled) {
                    	return pk;
                    }
                } catch (Exception e) {
                    continue;
                }
            }
        }
        return null;
    }

    public boolean updateFromMarket(String aMarketPackageName) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            Uri data = Uri.parse("market://details?id=" + mContext.getPackageName());
            intent.setData(data);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.setPackage(aMarketPackageName);
            mContext.startActivity(intent);
            return true;
        } catch (Exception e) {
			return false;
		}
    }

    public void checkUpdate(int aCheckType) {
      if (mUpdateing) {
          Toast.makeText(mContext, "Updateing", Toast.LENGTH_LONG).show();
      }else{
          setCheckType(aCheckType);
          if(aCheckType == Constant.AUTO_CHECK_TYPE){
              StatUpdateAgent.checkUpdate(mContext, true,mCheckUpdateResponse);
          }else{
              StatUpdateAgent.checkUpdate(mContext, false,mCheckUpdateResponse);
          }
      }
    }
    public void onComeBackHome() {
    	if(mConfirmDialog != null) {
    		if(mConfirmDialog.isShowing()) {
    			mConfirmDialog.dismiss();
    			StatUpdateAgent.postUserChoice(mContext,KirinConfig.LATER_UPDATE, mPostChoiceListener);
    		}
    	}
    }
}
