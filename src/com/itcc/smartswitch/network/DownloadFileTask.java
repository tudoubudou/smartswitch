
package com.itcc.smartswitch.network;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;

import com.itcc.smartswitch.R;
import com.itcc.smartswitch.utils.Constant;
import com.itcc.smartswitch.utils.LogEx;
import com.itcc.utils.PhoneInfoStateManager;
import com.itcc.utils.StorageUtil;
import com.itcc.utils.Utilities;

public class DownloadFileTask implements Runnable {
    private static final String TAG = "DownloadFileTask";

    public String mUrl = "";
    public String mDest = "";

    private String mTemp = "";
    private File mTempFile = null;

    public String mMimeType = "";
    public String mTitle = "";
    public String mDescription = "";

    public long old_file_size = 0;
    public long mTotalSize = 0;

    private DefaultHttpClient httpclient;
    private volatile boolean mCancel = false;

    private BufferedOutputStream file = null;
    public long id;
    public boolean bWifiOnly = false;
    public boolean bNeedNotify = false;
    public int notifyType = 0;

    private Context mContext;
    private long last_progress = 0;

    public WeakReference<Callback> callback = null;

//    public boolean bneedToast = false;
    public boolean bSilent = false;

    public DownloadFileTask(Context context, String url, String dst, String type, String title,
            String description,
            long oldSize, long id, Callback cb) {
        mUrl = url;
        mDest = dst;
        mMimeType = type;
        mTitle = title;
        mDescription = description;
        old_file_size = oldSize;
        this.id = id;

        mContext = context;
        mTemp = StorageUtil.getDownloadTempDir() + File.separator + Utilities.md5(mUrl);
        mTempFile = new File(mTemp);

        if (cb != null) {
            callback = new WeakReference<Callback>(cb);
        }
        // LogEx.i(TAG, "task " + id + ", this - " + this);
    }

    private void setup() {
        HttpParams httpParams = new BasicHttpParams();
        HttpProtocolParams.setVersion(httpParams, HttpVersion.HTTP_1_1);
        HttpProtocolParams.setContentCharset(httpParams, "utf-8");
        HttpProtocolParams.setUseExpectContinue(httpParams, false);
        HttpConnectionParams.setConnectionTimeout(httpParams, 6000);
        HttpConnectionParams.setSoTimeout(httpParams, 10000);
        httpclient = new DefaultHttpClient(httpParams);
        httpclient.setHttpRequestRetryHandler(new DefaultHttpRequestRetryHandler(3, false));
    }

    private void start() {

        if (!mCancel) {
            onStart();

            if (!StorageUtil.IsSdCardMounted()) {
                LogEx.e(TAG, "sd card not mounted");
                onFinish(Constant.RESULT_FAILED_SDCARD);

            } else if (bWifiOnly && !PhoneInfoStateManager.isWifiConnection(mContext)) {
                onFinish(Constant.RESULT_FAILED_NO_NETWORK);

            } else if (PhoneInfoStateManager.isNetworkConnectivity(mContext)) {

                long pos = 0;

                if (mTempFile.exists()) {

                    if (old_file_size <= 0) {
                        StorageUtil.deleteDir(mTempFile);
                    } else {
                        pos = mTempFile.length();
                    }
                }

                if (mTempFile.exists() && old_file_size == mTempFile.length()) {
                    // file download success
                    LogEx.i(TAG, "old_file_size == " + mTempFile.length());

                    onFinish(Constant.RESULT_SUCCESS);
                } else {
                    httpGet(pos);
                }
            } else {
                onFinish(Constant.RESULT_FAILED_NO_NETWORK);
            }
        } else {
            onFinish(Constant.RESULT_CANCELLED);
        }
    }

    private void httpGet(long startpos) {
        setup();

        HttpUriRequest req;

        req = new HttpGet(mUrl);

        if (startpos > 0) {
            req.setHeader("RANGE", "bytes=" + startpos + "-");
        }

        HttpEntity resEntity = null;

        try {
            HttpResponse response = httpclient.execute(req);

            if (!mCancel) {

                resEntity = response.getEntity();

                long total = resEntity.getContentLength();

                mTotalSize = total + startpos;

                //
                File oldFile = new File(mDest);

                if (oldFile.exists() && oldFile.length() == mTotalSize) {
                    try {
                        req.abort();
                    } catch (Exception e) {
                        LogEx.e(TAG, "len = total, req.abort() " + e.getLocalizedMessage());
                    }

                    onFinish(Constant.RESULT_SUCCESS);
                } else {
                    if (oldFile.exists()) {
                        oldFile.delete();
                    }

                    if (startpos <= 0 ||
                            (old_file_size > 0 && startpos > 0 && mTotalSize == old_file_size)) {
                        if(!StorageUtil.isExternalSpaceInsufficient(total)){
                            LogEx.e(TAG, "sd card insufficient");
                            try {
                                req.abort();
                            } catch (Exception e) {
                                LogEx.e(TAG, "2 len = total, req.abort() " + e.getLocalizedMessage());
                            }
                            onFinish(Constant.RESULT_FAILED_SDCARD_INSUFFICIENT);
                            return;
                        }

                        if (startpos <= 0) {
                            file = new BufferedOutputStream(new FileOutputStream(mTempFile));
                        } else {
                            file = new BufferedOutputStream(new FileOutputStream(mTempFile, true));
                        }

                        onProgress(mTotalSize, startpos);

                        int statusCode = response.getStatusLine().getStatusCode();
                        if (HttpStatus.SC_OK == statusCode
                                || HttpStatus.SC_PARTIAL_CONTENT == statusCode) {

                            InputStream is = resEntity.getContent();

                            byte[] buf = new byte[1024 * 8];
                            int ch = -1;
                            long len = startpos;

                            while ((ch = is.read(buf)) != -1 && !mCancel) {
                                file.write(buf, 0, ch);

                                len += ch;

                                onProgress(mTotalSize, len);

                                if (mCancel)
                                    LogEx.i(TAG, "aCancel = " + mCancel);
                            }

                            file.flush();
                            file.close();

                            if (mCancel) {
                                onFinish(Constant.RESULT_CANCELLED);

                            } else if (mTempFile.length() != mTotalSize) {
                                LogEx.e(TAG, "download error, length != total");
                                onFinish(Constant.RESULT_FAILED);

                            } else {

                                onFinish(Constant.RESULT_SUCCESS);
                            }
                        }
                    } else {
                        try {
                            req.abort();
                        } catch (Exception e) {
                            LogEx.e(TAG, "req.abort " + e.getLocalizedMessage());
                        }
                        httpGet(0);
                    }
                }
            }
        } catch (ClientProtocolException e) {
            e.printStackTrace();
            LogEx.e(TAG, "1 " + e.getLocalizedMessage());
            onFinish(Constant.RESULT_FAILED);
        } catch (IOException e) {
            e.printStackTrace();
            LogEx.e(TAG, "2 " + e.getLocalizedMessage());
            onFinish(Constant.RESULT_FAILED);
        } catch (Exception e) {
            e.printStackTrace();
            LogEx.e(TAG, "3 " + e.getLocalizedMessage());
            onFinish(Constant.RESULT_FAILED);
        } finally {
            req.abort();
        }
    }

    public void stop() {
        mCancel = true;
        // LogEx.e(TAG, "stop " + id + ", " + mCancel + ", " + this);
    }

    @Override
    public void run() {
        start();
    }

    //
    public void onStart() {
        Intent intent = new Intent(Constant.ACTION_DOWNLOAD_START);
        intent.putExtra(Constant.EXTRA_ID, id);

        mContext.sendBroadcast(intent);

        LogEx.i(TAG, "onStart" + id);
    }

    public void onProgress(long total, long current) {

        long _now = SystemClock.elapsedRealtime();
        if (last_progress == 0 || _now - last_progress > Constant.PROGRESS_INTERVAL) {

            last_progress = _now;

            Intent intent = new Intent(Constant.ACTION_DOWNLOAD_PROGRESS);
            intent.putExtra(Constant.EXTRA_ID, id);
            intent.putExtra(Constant.EXTRA_TOTAL, total);
            intent.putExtra(Constant.EXTRA_CURRENT, current);
            intent.putExtra(Constant.EXTRA_URL, mUrl);
            intent.putExtra(Constant.EXTRA_MIMETYPE, mMimeType);

            mContext.sendBroadcast(intent);

            // LogEx.e(TAG, "onProgress " + (current * 100) / total);
        }
    }

    public void onFinish(int result) {
        // private static final int RESULT_SUCCESS = 0;
        // private static final int RESULT_FAILED = 1;
        // private static final int RESULT_CANCELLED = 2;

        if (result == Constant.RESULT_SUCCESS) {
            File dst = new File(mDest);
            if (dst.exists() && dst.length() == mTotalSize) {
                LogEx.d(TAG, "dst file already exists");
            } else {
                StorageUtil.deleteDir(dst);
                dst.getParentFile().mkdirs();
                boolean ret = mTempFile.renameTo(dst);

                if (!ret) {
                    LogEx.e(TAG, "rename error");
                    result = Constant.RESULT_FAILED;
                }
            }
        }

        if (callback != null) {
            Callback cb = callback.get();
            if (cb != null) {
                cb.onFinish(id);
            }
        }

        Intent intent = new Intent(Constant.ACTION_DOWNLOAD_COMPOLETED);
        intent.putExtra(Constant.EXTRA_ID, id);
        intent.putExtra(Constant.EXTRA_RESULT, result);
        intent.putExtra(Constant.EXTRA_DEST_PATH, mDest);

        LogEx.i(TAG, "need nof " + bNeedNotify);

        if (bNeedNotify) {
            intent.putExtra(Constant.EXTRA_NOTIFY_TYPE, notifyType);
        }

        mContext.sendBroadcast(intent);

        ContentValues values = new ContentValues();

        values.put(Constant.COLUMN_DOWNLOAD_TOTAL_SIZE, mTotalSize);
        values.put(Constant.COLUMN_DOWNLOAD_STATUS, result);

        mContext.getContentResolver().update(Constant.DOWNLOAD_URI, values,
                Constant.ID + "=" + id, null);

        // LogEx.e(TAG, "onFinish " + result);
        LogEx.i(TAG, "onFinish " + result + ", " + mDest);
        LogEx.i(TAG, "onFinish " + mUrl);

        if (!bSilent) {
            String msg = null;;
            if (result == Constant.RESULT_FAILED_SDCARD_INSUFFICIENT) {
                msg = mContext.getString(R.string.toast_download_fail_storage);
            }else if (result == Constant.RESULT_FAILED_SDCARD) {
                msg = mContext.getString(R.string.download_sdcard_status_error);
            } else if (result == Constant.RESULT_FAILED) {
                if (PhoneInfoStateManager.isNetworkConnectivity(mContext)) {
                    msg = mContext.getString(R.string.download_failed_toast);
                }else {
                	msg = mContext.getString(R.string.download_network_invalid_toast);
                }
            } else if(result == Constant.RESULT_FAILED_NO_NETWORK){
                msg = mContext.getString(R.string.download_network_invalid_toast);
            }
            Intent i = new Intent(Constant.ACTION_DOWNLOAD_SHOWFAILMSG);
            i.putExtra(Constant.FAIL_MSG, msg);
            mContext.sendBroadcast(i);
        }
    }

    public interface Callback {
        public void onFinish(long id);
    }
}
