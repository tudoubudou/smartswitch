package com.itcc.smartswitch.utils;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.net.Uri;

public class Constant {

    // version update
    public static final int NOTIFY_TYPE_NEW_VERSION_UPDATE_APK = 2;
    public final static int AUTO_CHECK_TYPE = 0;
    public final static int CLICK_CHECK_TYPE = 1;
    public static final String PACKAGENAME = "com.itcc.smartswitch";
    public static final String ACTION_UPDATE_CONFIRM = PACKAGENAME + ".update_confirm";
    public static final String UPDATE_URL = "update_url";
    public static final String UPDATE_TARGET_MAEKET = "update_market";
    public static final String CANCEL_UPDATE_TIME_KRY = "cancel_update_time";
    // public static final ArrayList<String> mNeedChangeWidgetLayout;
    public static final String KEY_NEW_VERSION = "has_new_version";
    // db
    public static final String DATABASE_NAME = "smartswitch.db";
    public static final int DATABASE_VERSION = 1;
    public static final String AUTHORITY = PACKAGENAME + ".main.downloads";
    public static final String ID = "_id";
    public static final String TABLE_DOWNLOAD = "download";
    public static final Uri DOWNLOAD_URI = Uri.parse("content://" + AUTHORITY + "/" + TABLE_DOWNLOAD);
    // download table
    public static final String COLUMN_DOWNLOAD_FILE_NAME = "file_name";
    public static final String COLUMN_DOWNLOAD_DESTINATION = "dest";
    public static final String COLUMN_DOWNLOAD_URL = "url";
    public static final String COLUMN_DOWNLOAD_MIME_TYPE = "mime_type";
    public static final String COLUMN_DOWNLOAD_TOTAL_SIZE = "total_size";
    public static final String COLUMN_DOWNLOAD_CURRENT_SIZE = "current_size";
    public static final String COLUMN_DOWNLOAD_STATUS = "status";
    public static final String COLUMN_DOWNLOAD_DATE = "download_date";
    public static final String COLUMN_DOWNLOAD_TITLE = "title";
    public static final String COLUMN_DOWNLOAD_DESCRIPTION = "description";
    public static final String COLUMN_DOWNLOAD_WIFIONLY = "wifionly";
    // download status
    public static final int RESULT_SUCCESS = 0;
    public static final int RESULT_FAILED = 1;
    public static final int RESULT_CANCELLED = 2;
    public static final int RESULT_FAILED_SDCARD = 3;
    public static final int RESULT_FAILED_NO_NETWORK = 4;
    public static final int RESULT_FAILED_SDCARD_INSUFFICIENT = 5;
    // download notify
    public static final String EXTRA_ID = "extra_id";
    public static final String EXTRA_TOTAL = "extra_total";
    public static final String EXTRA_CURRENT = "extra_current";
    public static final String EXTRA_TITLE = "extra_title";
    public static final String EXTRA_PROGRESS = "extra_progress";
    public static final String EXTRA_RESULT = "extra_result";
    public static final String EXTRA_NOTIFY_TYPE = "extra_notify_type";
    public static final String EXTRA_DEST_PATH = "extra_dest_path";
    public static final String EXTRA_URL = "extra_url";
    public static final String EXTRA_MIMETYPE = "extra_mimetype";
    // action
    public static final String ACTION_DOWNLOAD_ADD = PACKAGENAME + ".download.add";
    public static final String ACTION_DOWNLOAD_STOP = PACKAGENAME + ".download.stop";
    public static final String ACTION_DOWNLOAD_START = PACKAGENAME + ".download_start";
    public static final String ACTION_DOWNLOAD_PROGRESS = PACKAGENAME + ".download_progress";
    public static final String ACTION_DOWNLOAD_COMPOLETED = PACKAGENAME + ".download_completed";
    public static final String ACTION_DOWNLOAD_SHOWFAILMSG = PACKAGENAME + ".showMsg";
    public static final String ACTION_EXIT_SLEEP = PACKAGENAME + ".action.ALARM_EXIT_SLEEP";
    public static final String ACTION_SLEEP_ALARM = PACKAGENAME + ".action.ALARM";
    public static final String FAIL_MSG = "DownloadFailMsg";
    public static final int PROGRESS_INTERVAL = 1000;
    public final static ExecutorService mExecutorService = Executors.newCachedThreadPool();


    /*
     * static { ArrayList<String> list = new ArrayList<String>(); //
     * list.add("com.baidu.launcher.searchbar.BaiduSearchWidgetProvider"); //
     * list.add("com.mediatek.appwidget.worldclock.WorldClockWidgetProvider");
     * //
     * list.add("com.baidu.appsearch.widget.appmanagewidget.AppManageWidgetProvider"
     * ); list.add("com.baidu.searchbox.widget.QuickSearchWidgetProvider");
     * list.add("com.android.mms.widget.MmsWidgetProvider");
     * list.add("com.baidu.gallery3d.gadget.PhotoAppWidgetProvider");
     * list.add("com.android.email.provider.WidgetProvider");
     * list.add("com.android.calendar.widget.CalendarAppWidgetProvider");
     * list.add("com.baidu.hao123.common.BRWidget4x4");
     * list.add("com.asksven.betterbatterystats.widgetproviders.LargeWidgetProvider"
     * ); list.add(
     * "com.asksven.betterbatterystats.widgetproviders.MediumWidgetProvider");
     * list
     * .add("com.asksven.betterbatterystats.widgetproviders.SmallWidgetProvider"
     * ); mNeedChangeWidgetLayout = list; }
     */
}
