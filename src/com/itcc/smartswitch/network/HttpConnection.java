
package com.itcc.smartswitch.network;

public class HttpConnection {

    public void cancelAll() {
    	HttpScheduler.getInstance().cancel(this);
    }

}
