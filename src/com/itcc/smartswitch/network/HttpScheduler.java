
package com.itcc.smartswitch.network;


import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.FutureTask;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.itcc.smartswitch.utils.LogEx;

public class HttpScheduler {

    private static final String TAG = HttpScheduler.class.getSimpleName();

    private static final int MIN_TASK_COUNT = 5;
    private static final int MAX_TASK_COUNT = 10;
    private static final int THREAD_POOL_TRIM_DELAY = 30 * 1000; // 30s

    private static HttpScheduler mHttpScheduler;

    private HttpScheduler() {

    }

    public synchronized static HttpScheduler getInstance() {
        if (mHttpScheduler == null) {
            mHttpScheduler = new HttpScheduler();
        }
        return mHttpScheduler;
    }

    public synchronized static void close() {
        if (mHttpScheduler != null) {
            mHttpScheduler.cancelAll();
            mHttpScheduler = null;
        }
    }

    private List<HttpRunnable> mWorkingTask = Collections
            .synchronizedList(new ArrayList<HttpRunnable>());

    private static class HttpFutureTask<V> extends FutureTask<V> implements
            Comparable<HttpFutureTask<V>> {
        private HttpRunnable object;

        public HttpFutureTask(Runnable runnable, V result) {
            super(runnable, result);
            object = (HttpRunnable) runnable;
        }

        @Override
        @SuppressWarnings("unchecked")
        public int compareTo(HttpFutureTask<V> o) {
            if (this == o) {
                return 0;
            }

            if (o == null) {
                return -1; // high priority
            }

            if (object != null && o.object != null) {
                if (object.getClass().equals(o.object.getClass())) {
                    if (object instanceof Comparable) {
                        return ((Comparable) object).compareTo(o.object);
                    }
                }
            }

            return 0;
        }
    }

    private static HttpRunnable getHttpRunnable(Runnable r) {
        if (r instanceof HttpFutureTask<?>) {
            return ((HttpFutureTask<?>) r).object;
        } else {
            return null;
        }
    }

    private ThreadFactory mThreadFactory = new ThreadFactory() {
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(r);
            thread.setPriority(Thread.MIN_PRIORITY);
            return thread;
        }
    };

    private ThreadPoolExecutor mHttpThreadExecutor = new ThreadPoolExecutor(MIN_TASK_COUNT,
            MAX_TASK_COUNT,
            THREAD_POOL_TRIM_DELAY, TimeUnit.MILLISECONDS, new PriorityBlockingQueue<Runnable>(),
            mThreadFactory) {

        @Override
        protected void beforeExecute(Thread t, Runnable r) {
            mWorkingTask.add(getHttpRunnable(r));
        }

        @Override
        protected void afterExecute(Runnable r, Throwable t) {
            mWorkingTask.remove(getHttpRunnable(r));
        }

        @Override
        protected <T> RunnableFuture<T> newTaskFor(Runnable runnable, T value) {
            return new HttpFutureTask<T>(runnable, value);
        }

    };

    /*
     * Send http request.
     */
    synchronized boolean send(HttpTask task) {
        if (task == null)
            return false;

        if (task.mRequest.isAvailable()) {
            if (exist(task)) {
                LogEx.e(TAG, "HttpTask exist!");
            } else {
                mHttpThreadExecutor.submit(new HttpRunnable(task));
                return true;
            }
        } else {
            // Log.e(TAG, "HttpTask invalid!");
        }

        task.mProgress.mState = HttpTask.State.Failed;
        task.onProcessed();
        return false;
    }

    /*
     * Cancel the specified task.
     */
    synchronized int cancel(HttpTask task) {
        if (task == null)
            return 0;

        // cancel waiting task
        Iterator<Runnable> waiting = getWaitingTaskIterator();
        while (waiting.hasNext()) {
            final HttpRunnable runnable = getHttpRunnable(waiting.next());
            if (runnable != null && runnable.getHttpTask() == task) {
                runnable.cancel();
                waiting.remove();
                return 1;
            }
        }

        // cancel executing task
        for (int i = 0; i < mWorkingTask.size(); i++) {
            final HttpRunnable runnable = mWorkingTask.get(i);
            if (runnable.getHttpTask() == task) {
                runnable.cancel();
                return 1;
            }
        }

        return 0;
    }

    /*
     * Cancel all tasks sent via same connection.
     */
    synchronized int cancel(HttpConnection connection) {
        if (connection == null)
            return 0;

        int count = 0;

        // cancel waiting task
        Iterator<Runnable> waiting = getWaitingTaskIterator();
        while (waiting.hasNext()) {
            final HttpRunnable runnable = getHttpRunnable(waiting.next());
            if (runnable != null && runnable.getHttpTask().viaConnection(connection)) {
                runnable.cancel();
                waiting.remove();
                count++;
            }
        }

        // cancel executing task
        for (int i = 0; i < mWorkingTask.size(); i++) {
            final HttpRunnable runnable = mWorkingTask.get(i);
            if (runnable.getHttpTask().viaConnection(connection)) {
                runnable.cancel();
                count++;
            }
        }

        return count;
    }

    /*
     * Cancel all tasks including executing & waiting task.
     */
    synchronized public int cancelAll() {
        int count = 0;

        // cancel waiting task
        BlockingQueue<Runnable> waitingTask = mHttpThreadExecutor.getQueue();
        Iterator<Runnable> waiting = waitingTask.iterator();
        while (waiting.hasNext()) {
            final HttpRunnable runnable = getHttpRunnable(waiting.next());
            if (runnable != null) {
                runnable.cancel();
            }
        }
        count += waitingTask.size();
        waitingTask.clear();

        // cancel executing task
        for (int i = 0; i < mWorkingTask.size(); i++) {
            final HttpRunnable runnable = mWorkingTask.get(i);
            runnable.cancel();
            count++;
        }

        return count;
    }

    private boolean exist(HttpTask task) {
        if (task == null)
            return false;

        // check waiting task
        Iterator<Runnable> waiting = getWaitingTaskIterator();
        while (waiting.hasNext()) {
            final HttpRunnable runnable = getHttpRunnable(waiting.next());
            if (runnable != null && runnable.getHttpTask() == task) {
                return true;
            }
        }

        // check executing task
        for (final HttpRunnable working : mWorkingTask) {
            if (working.getHttpTask() == task) {
                return true;
            }
        }

        return false;
    }

    private Iterator<Runnable> getWaitingTaskIterator() {
        BlockingQueue<Runnable> waitingTask = mHttpThreadExecutor.getQueue();
        return waitingTask.iterator();
    }

    private class HttpRunnable implements Runnable, Comparable<HttpRunnable>, HttpClient.Observer {
        private final HttpTask mHttpTask;
        private HttpClient mHttpClient;

        final static int NOTIFY_PER_PERCENT = 10;
        private int mNotifyPercent;

        public HttpRunnable(HttpTask httpTask) {
            mHttpTask = httpTask;
        }

        public HttpTask getHttpTask() {
            return mHttpTask;
        }

        public void cancel() {
            if (mHttpClient != null) {
                mHttpClient.cancel();
            } else {
                onFinish(Reason.Canceled);
            }
        }

        @Override
        public void run() {
//            LogEx.e(TAG, "task run");

            mHttpClient = new HttpClient();
            mHttpClient.execute(mHttpTask.mRequest, this);
        }

        @Override
        public int compareTo(HttpRunnable another) {
            return -mHttpTask.mPriority.compareTo(another.mHttpTask.mPriority);
        }

        @Override
        public void onStart() {
            mHttpTask.mProgress.mState = HttpTask.State.Start;
            mHttpTask.onProcessed();
        }

        @Override
        public void onSend(int total, int newFinished) {
            HttpTask.Progress progress = mHttpTask.mProgress;
            progress.mState = HttpTask.State.Sending;
            progress.mTotal = total;

            if (total == HttpClient.Observer.UNKNOWN_LENGTH) { // begin to send
                progress.mTotal = 0;
                progress.mFinished = 0;
                progress.reset();
                mNotifyPercent = 0;
                mHttpTask.onProcessed();
            } else if (newFinished > 0) { // on sending
                progress.mFinished += newFinished;

                final int percent = progress.percent();
                // notify observer per NOTIFY_PER_PERCENT
                if (mNotifyPercent + NOTIFY_PER_PERCENT < percent) {
                    mNotifyPercent = percent;
                    mHttpTask.onProcessed();
                }
            }
        }

        @Override
        public void onStartReceive(int total) {
            HttpTask.Progress progress = mHttpTask.mProgress;

            progress.mTotal = total + (int) mHttpTask.mRequest.range.begin;
            progress.mFinished = (int) mHttpTask.mRequest.range.begin;
            mNotifyPercent = 0;
            progress.mState = HttpTask.State.StartReceive;
            mHttpTask.onProcessed();
        }

        @Override
        public void onReceive(int total, int newFinished, final byte[] buffer) {
            HttpTask.Progress progress = mHttpTask.mProgress;
            progress.mState = HttpTask.State.Receiving;
            progress.mTotal = total;

            if (total == HttpClient.Observer.UNKNOWN_LENGTH) { // begin to
                                                               // receive
                progress.mTotal = 0;
                progress.mFinished = (int) mHttpTask.mRequest.range.begin;
                mNotifyPercent = 0;
                mHttpTask.onProcessed();
            } else if (newFinished > 0) { // on receiving
                mHttpTask.mBuffer = buffer;
                mHttpTask.mBufferLen = newFinished;

                progress.mFinished += newFinished;

                mHttpTask.onProcessed();
            }
        }

        @Override
        public void onFinish(Reason reason) {
            HttpTask.Progress progress = mHttpTask.mProgress;

            // fix length
            if (progress.mTotal < 0) {
                progress.mTotal = progress.mFinished;
            }

            // set state
            switch (reason) {
                case Success:
                    progress.mState = HttpTask.State.Finished;
                    break;

                case Failed:
                    progress.mState = HttpTask.State.Failed;
                    break;

                case Canceled:
                    progress.mState = HttpTask.State.Cancelled;
                    break;
            }

            boolean ret = mWorkingTask.remove(this);

            mHttpTask.onProcessed();
        }
  
        @Override
        public String getPostData() {
            return mHttpTask.getPostData();
        }
    }

}
