
package com.itcc.smartswitch.network;

/**
 * callback on non-UI thread
 */
public interface IHttpCallback {
    public void TaskFailed(HttpTask task);

    public void TaskCancelled(HttpTask task);

    public void TaskSuccessed(HttpTask task);

    public void TaskProgress(HttpTask task, int total, int current);

    public void TaskStart(HttpTask task);
}
