/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.itcc.smartswitch.network;

import java.text.DateFormat;
import java.util.HashMap;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.widget.RemoteViews;

import com.itcc.smartswitch.R;
import com.itcc.smartswitch.utils.Constant;
import com.itcc.smartswitch.utils.LogEx;
import com.itcc.utils.PhoneInfoStateManager;
import com.itcc.utils.StorageUtil;

public class DownloadNotifManager {
    static final String TAG = "DownloadNotification";

    private HashMap<Long, NotificationItem> mProgressNotifMap;
    private HashMap<Long, NotificationItem> mSuccessNotifMap;
    private HashMap<Long, NotificationItem> mFailNotifMap;

    Context mContext;

    private static DownloadNotifManager mInstance;

    public static class NotificationItem {
        /** mtitles. */
        private String mTitle; // download title.
        /** Ticker text */
        public Intent mIntent = null;
    }

    public static synchronized DownloadNotifManager getInstance(Context aContext) {
        if (mInstance == null) {
            mInstance = new DownloadNotifManager(aContext);
        }
        return mInstance;
    }

    private DownloadNotifManager(Context ctx) {
        mContext = ctx;
        mProgressNotifMap = null;
        mSuccessNotifMap = null;
        mFailNotifMap = null;
    }

//    private static class DownloadReceiver extends BroadcastReceiver {
//        public static final String TAG = "DownloadReceiver";
//
//        @Override
//        public void onReceive(final Context context, Intent intent) {
//            final String action = intent.getAction();
//            long id = intent.getLongExtra(Constant.EXTRA_ID, -1);
//            if (id != -1) {
//                if (action.equals(Constant.ACTION_DOWNLOAD_PROGRESS)) {
//                    long total = intent.getLongExtra(Constant.EXTRA_TOTAL, 0);
//                    long current = intent.getLongExtra(Constant.EXTRA_CURRENT, 0);
//
//                    DownloadNotifManager.getInstance(context)
//                            .updateProgressNotification(id,
//                                    total, current);
//                }
//                if (action.equals(Constant.ACTION_DOWNLOAD_COMPOLETED)) {
//                    int result = intent.getIntExtra(Constant.EXTRA_RESULT, -1);
//                    DownloadNotifManager.getInstance(context)
//                            .updateCompletedNotification(id,
//                                    result);
//                }
//            }
//        }
//    }

    /**
     * add notification when need, you can specify which notification should be
     * show, and the intent when the notification be clicked.
     * 
     * @param aId : download task id
     * @param aShowProgressNotif : does the progress notification should be show
     * @param aProgressIntent : intent when the progress notification be
     *            clicked,if it be null, there will be a default intent, which
     *            will show a DownloadAlert.
     * @param aShowSuccessNotif : does the download success notification should
     *            be show
     * @param aSuccessIntent : the intent, if it be null, nothing will happen
     *            when click the notification
     * @param aShowFailNotif : does the download fail notification should be
     *            show
     * @param aFailIntent : the intent, if it be null, nothing will happen when
     *            click the notification
     */
    public void addNotification(final long aId,
            final boolean aShowProgressNotif, Intent aProgressIntent,
            final boolean aShowSuccessNotif, final Intent aSuccessIntent,
            final boolean aShowFailNotif, final Intent aFailIntent) {

        LogEx.e(TAG, "add " + aId);
        if (aId >= 0) {

            String[] projection = new String[] {
                    Constant.COLUMN_DOWNLOAD_TITLE, Constant.COLUMN_DOWNLOAD_MIME_TYPE
            };

            String selection = Constant.ID + "=" + aId;

            Cursor cursor = mContext.getContentResolver().query(Constant.DOWNLOAD_URI, projection,
                    selection, null, "_id DESC");

            String title = "", mime_type = "";

            if (cursor != null && cursor.getCount() > 0 && cursor.moveToFirst()) {
                title = cursor.getString(cursor.getColumnIndex(Constant.COLUMN_DOWNLOAD_TITLE));
                mime_type = cursor.getString(cursor
                        .getColumnIndex(Constant.COLUMN_DOWNLOAD_MIME_TYPE));
            }

            if (cursor != null) {
                cursor.close();
            }

            if (aShowProgressNotif) {
                if (mProgressNotifMap == null) {
                    mProgressNotifMap = new HashMap<Long, NotificationItem>();
                }
                if (aProgressIntent == null) {
//                    aProgressIntent = new Intent(mContext, DownloadAlert.class);
//                    aProgressIntent.putExtra(Constant.EXTRA_TITLE, title);
//                    aProgressIntent.putExtra(Constant.EXTRA_ID, aId);
//                    aProgressIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                }
                addNotification(aId, title, aProgressIntent, mProgressNotifMap);
            }

            if (aShowSuccessNotif) {
                if (mSuccessNotifMap == null) {
                    mSuccessNotifMap = new HashMap<Long, NotificationItem>();
                }
                addNotification(aId, title, aSuccessIntent, mSuccessNotifMap);
            }

            if (aShowFailNotif) {
                if (mFailNotifMap == null) {
                    mFailNotifMap = new HashMap<Long, NotificationItem>();
                }
                addNotification(aId, title, aFailIntent, mFailNotifMap);
            }
        }
    }
    
    /**
     * show progress notification atOnce
     * 
     * @param aId
     */
    public void showProgressNotifAtOnce(long aId) {
        if (!StorageUtil.IsSdCardMounted() 
                || !PhoneInfoStateManager.isNetworkConnectivity(mContext)) {
            LogEx.e(TAG, "sd card not mounted or network not connected");
            return;

        }
         updateProgressNotification(aId, 100, 0);
    }

	private void addNotification(long id, String title, Intent intent,
            HashMap<Long, NotificationItem> notifMap) {
        if (notifMap == null)
            return;
        if(intent == null){
            intent = new Intent();
        }
        NotificationItem item = new NotificationItem();
        item.mTitle = title;
        item.mIntent = intent;
        notifMap.put(id, item);
    }

    public void updateProgressNotification(long aId, long total, long current) {

        if (mProgressNotifMap == null)
            return;
        int progress = -1;
        if (total > 0) {
            if (total > current) {
                progress = (int) (current * 100 / total);
            }
        }
        if (progress < 0) {
            return;
        }

        NotificationItem ni = mProgressNotifMap.get(aId);
        if (ni != null) {

            int iconResource = R.drawable.statusbar_download_icon;
            Notification notif = new Notification();

            ni.mIntent.putExtra(Constant.EXTRA_PROGRESS, progress);
            PendingIntent contentIntent = PendingIntent.getActivity(mContext, 0, ni.mIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT);

            notif.icon = iconResource;
            notif.tickerText = mContext.getResources().getString(R.string.downloading_notify, ni.mTitle);
//            notif.contentIntent =  contentIntent;
            notif.flags = Notification.FLAG_ONGOING_EVENT | Notification.FLAG_AUTO_CANCEL;

            RemoteViews contentView = new RemoteViews(mContext.getPackageName(), R.layout.notification_download);
            contentView.setTextViewText(R.id.statusbar_download_title, ni.mTitle);
            contentView.setTextViewText(R.id.statusbar_download_progress, String.valueOf(progress)+"%");
            contentView.setProgressBar(R.id.statusbar_progress, 100, progress,false);  
            contentView.setImageViewResource(R.id.statusbar_download_icon, iconResource);

            DateFormat df = DateFormat.getTimeInstance(DateFormat.SHORT);
            String time = df.format(System.currentTimeMillis());
            contentView.setTextViewText(R.id.statusbar_time, time);

            notif.contentView = contentView;

            NotificationManager nm = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
            nm.notify((int) aId, notif);
        }
    }

    public void canlelNotif(long aId) {
        if (mProgressNotifMap!=null && mProgressNotifMap.containsKey(aId)) {
            mProgressNotifMap.remove(aId);
        }

        NotificationManager nm = (NotificationManager) mContext
                .getSystemService(Context.NOTIFICATION_SERVICE);
        nm.cancel((int) aId);
    }

    public void cancelCompleteNotif(long aId){
        if(mSuccessNotifMap!=null && mSuccessNotifMap.containsKey(aId)){
            mSuccessNotifMap.remove(aId);
        }
        if(mFailNotifMap!=null && mFailNotifMap.containsKey(aId)){
            mFailNotifMap.remove(aId);
        }
    }

    public void updateCompletedNotification(long aId, int aResult) {
        canlelNotif(aId);

        if(aResult == Constant.RESULT_FAILED_SDCARD 
                || aResult == Constant.RESULT_FAILED_NO_NETWORK
                || aResult == Constant.RESULT_FAILED_SDCARD_INSUFFICIENT){
            return;
        }

        HashMap<Long, NotificationItem> notifMap = null;
        if (aResult == Constant.RESULT_SUCCESS) {
            notifMap = mSuccessNotifMap;
        } else if (aResult == Constant.RESULT_FAILED) {
            notifMap = mFailNotifMap;
        }

        if (notifMap == null)
            return;

        NotificationItem ni = notifMap.get(aId);
        if (ni != null) {
            String ticker = "", text = "";
            if (aResult == Constant.RESULT_FAILED) {
                text = mContext.getString(R.string.downloaded_failure);
                ticker = ni.mTitle
                        + mContext.getResources().getString(R.string.downloaded_failure, "");
            } else if (aResult == Constant.RESULT_SUCCESS) {
                text = mContext.getString(R.string.downloaded_success);
                ticker = ni.mTitle
                        + mContext.getResources().getString(R.string.downloaded_success, "");
            }
            int iconResource = R.drawable.statusbar_download_icon;
            Notification notification = new Notification(iconResource, ticker,
                    System.currentTimeMillis());
            notification.flags = Notification.FLAG_AUTO_CANCEL;
            PendingIntent pendingIntent = null;
            if (ni.mIntent != null) {
            pendingIntent = PendingIntent.getActivity(mContext, 0, ni.mIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT);
            }
            notification.setLatestEventInfo(mContext, ni.mTitle, text, pendingIntent);

            NotificationManager nm = (NotificationManager) mContext
                    .getSystemService(Context.NOTIFICATION_SERVICE);
            nm.notify((int) aId, notification);
        }
        cancelCompleteNotif(aId);
    }
}
