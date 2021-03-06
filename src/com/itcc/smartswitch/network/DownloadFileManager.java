
package com.itcc.smartswitch.network;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import com.itcc.smartswitch.utils.Constant;
import com.itcc.smartswitch.utils.LogEx;

public class DownloadFileManager {
    private static final String TAG = "DownloadFileManager";

    private static DownloadFileManager instance = new DownloadFileManager();
    private ExecutorService mExecutorService = Executors.newSingleThreadExecutor();
    private List<DownloadFileTask> taskList = Collections
            .synchronizedList(new ArrayList<DownloadFileTask>());

    private DownloadCallback cb = new DownloadCallback();

    private DownloadFileManager() {
    }

    public static DownloadFileManager getInstance() {
        return instance;
    }

    public long insertTask(Context context, String url, String dst, String mimetype,
            String title, String description, boolean wifiOnly, boolean needNotify, int notifyType) {
        return insertTask(context, url, dst, mimetype, title, description, wifiOnly, needNotify,
                notifyType, true);
    }

    public long insertTask(Context context, String url, String dst, String mimetype,
            String title, String description, boolean wifiOnly, boolean needNotify, int notifyType,
            boolean silent) {

        long id = -1;
        long total = -1;

        boolean exist = false;

        LogEx.i(TAG, "insert task " + url + ", dst " + dst);

        // query task
        for (DownloadFileTask t : taskList) {

            if (t.mUrl.equalsIgnoreCase(url) &&
                    t.mDest.equalsIgnoreCase(dst)) {
                id = t.id;

                t.bNeedNotify = needNotify;
                t.notifyType = notifyType;
                t.bWifiOnly = wifiOnly || t.bWifiOnly;
                t.bSilent = silent && t.bSilent;

                exist = true;
                LogEx.i(TAG, "find exist task " + id);
                break;
            }
        }

        // query download db

        if (id == -1) {

            String[] projection = new String[] {
                    Constant.ID,
                    Constant.COLUMN_DOWNLOAD_TOTAL_SIZE
            };

            String selection = Constant.COLUMN_DOWNLOAD_URL + "=? and " +
                    Constant.COLUMN_DOWNLOAD_DESTINATION + "=? and " +
                    Constant.COLUMN_DOWNLOAD_MIME_TYPE + "=?";

            String[] selectionArgs = new String[] {
                    url, dst, mimetype
            };

            Cursor cursor = context.getContentResolver().query(Constant.DOWNLOAD_URI, projection,
                    selection, selectionArgs, "_id DESC");

            if (cursor != null && cursor.moveToNext()) {
                id = cursor.getLong(cursor.getColumnIndex(Constant.ID));
                total = cursor.getLong(cursor.getColumnIndex(Constant.COLUMN_DOWNLOAD_TOTAL_SIZE));

            }

            if (cursor != null) {
                cursor.close();
                cursor = null;
            }
        }

        // new task
        if (id == -1) {
            ContentValues values = new ContentValues();

            values.put(Constant.COLUMN_DOWNLOAD_DESTINATION, dst);
            values.put(Constant.COLUMN_DOWNLOAD_URL, url);

            values.put(Constant.COLUMN_DOWNLOAD_MIME_TYPE, mimetype);
            values.put(Constant.COLUMN_DOWNLOAD_STATUS, HttpTask.State.Idle.ordinal());
            values.put(Constant.COLUMN_DOWNLOAD_DATE, (new Date()).getTime() / 1000);
            values.put(Constant.COLUMN_DOWNLOAD_TITLE, title);
            values.put(Constant.COLUMN_DOWNLOAD_DESCRIPTION, description);
            values.put(Constant.COLUMN_DOWNLOAD_WIFIONLY, wifiOnly);

            Uri uri = context.getContentResolver().insert(Constant.DOWNLOAD_URI, values);

            id = ContentUris.parseId(uri);

            LogEx.i(TAG, "new task " + id);
        }

        if (!exist) {
            DownloadFileTask down = new DownloadFileTask(context, url, dst, "type", title,
                    description, total, id, cb);
            down.bWifiOnly = wifiOnly;
            down.bNeedNotify = needNotify;
            down.notifyType = notifyType;
            down.bSilent = silent;

            taskList.add(down);
            mExecutorService.submit(down);
        }

        return id;
    }

    public void stopTask(Context context, long id) {

        LogEx.i(TAG, "stop task " + id + " taskList = " + taskList.size());

        if (id >= 0) {
            for (DownloadFileTask t : taskList) {
                LogEx.i(TAG, "t " + t.id);

                if (t.id == id) {

                    LogEx.i(TAG, "stop task " + id);
                    t.stop();

                    ContentValues values = new ContentValues();

                    values.put(Constant.COLUMN_DOWNLOAD_TOTAL_SIZE, t.mTotalSize);

                    context.getContentResolver().update(Constant.DOWNLOAD_URI, values,
                            Constant.ID + "=" + t.id, null);

                    taskList.remove(t);

                    break;
                }
            }
        }
    }

    public long isInDownloadTask(String url, String dst, boolean silent) {
        long id = -1;

        for (DownloadFileTask t : taskList) {

            if (t.mUrl.equalsIgnoreCase(url) &&
                    t.mDest.equalsIgnoreCase(dst) &&
                    t.bSilent == silent) {
                id = t.id;
            }
        }
        return id;
    }

    private class DownloadCallback implements DownloadFileTask.Callback {

        @Override
        public void onFinish(long id) {

            Iterator<DownloadFileTask> it = taskList.iterator();
            while (it.hasNext()) {
                DownloadFileTask task = (DownloadFileTask) it.next();
                if (task.id == id) {
                    it.remove();
                }
            }
        }
    }

}
