
package com.itcc.smartswitch.network;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;

import com.itcc.smartswitch.network.HttpParam.Request;
import com.itcc.smartswitch.utils.LogEx;

public class HttpTask {

    private static final String TAG = "HttpTask";

    // only allowed add new state in the last
    public enum State {
        Idle,
        Start,
        Sending,
        StartReceive,
        Receiving,
        Finished,
        Failed,
        Cancelled,
    }

    public static class Priority {
        public final static int HIGH = 1;
        public final static int NORMAL = 0;
        public final static int LOW = -1;
    }

    public class Progress {
        public State mState = State.Idle;
        public int mTotal = -1;
        public int mFinished;

        Progress() {

        }

        public void reset() {
            // mState = State.Idle;
            mTotal = -1;
            mFinished = 0;
        }

        public int percent() {
            switch ((int) mTotal) {
                case -1:
                    return 0;
                case 0:
                    return 100;
                default:
                    return (int) (mFinished * 100 / mTotal);
            }
        }
    }

    private HttpConnection mConnection;

    public Integer mPriority = Priority.NORMAL;
    public Request mRequest = new Request();
    public Progress mProgress = new Progress();
    public byte[] mBuffer;
    public int mBufferLen = 0;
    public String mPostData = null;

    protected WeakReference<IHttpCallback> callbackRef;

    private ByteArrayOutputStream baos = new ByteArrayOutputStream();

    public HttpTask() {
    }

    public HttpTask(IHttpCallback callback) {
        callbackRef = new WeakReference<IHttpCallback>(callback);
    }

    public HttpTask(HttpConnection connection, IHttpCallback callback) {
        this(callback);
        mConnection = connection;
    }

    final public void reset() {
        mRequest.reset();
        mProgress.reset();
        mProgress.mState = HttpTask.State.Idle;
        mBuffer = null;
    }

    final public boolean send() {
        mProgress.reset();
        mBuffer = null;
        return HttpScheduler.getInstance().send(this);
    }

    final public boolean send(HttpParam.Method method, String host) {
        mRequest.method = method;
        mRequest.host = host;
        return send();
    }

    final public void cancel() {
        mBufferLen = 0;
        mBuffer = null;
        HttpScheduler.getInstance().cancel(this);
    }

    /* final */protected void onProcessed() {

        if (HttpTask.State.Idle == mProgress.mState) {

        } else if (HttpTask.State.Sending == mProgress.mState) {

        } else if (HttpTask.State.StartReceive == mProgress.mState) {
            baos.reset();

        } else if (HttpTask.State.Receiving == mProgress.mState) {
            baos.write(mBuffer, 0, mBufferLen);

        } else if (HttpTask.State.Finished == mProgress.mState) {
            try {
                baos.flush();
                baos.close();
            } catch (IOException e) {
                e.printStackTrace();
                LogEx.e(TAG, "");
            }

            if (callbackRef != null) {
                IHttpCallback callback = callbackRef.get();

                if (callback != null) {
                    callback.TaskSuccessed(this);
                }
            }

        } else if (HttpTask.State.Failed == mProgress.mState) {

            if (callbackRef != null) {
                IHttpCallback callback = callbackRef.get();

                if (callback != null) {
                    callback.TaskFailed(this);
                }
            }

        } else if (HttpTask.State.Cancelled == mProgress.mState) {

            if (callbackRef != null) {
                IHttpCallback callback = callbackRef.get();

                if (callback != null) {
                    callback.TaskCancelled(this);
                }
            }
        }
    }

    protected String getPostData() {
        return mPostData;
    }

    final boolean viaConnection(HttpConnection connection) {
        return (connection != null && mConnection == connection);
    }

    public String getHttpResult() {
        return baos.toString();
    }
}
