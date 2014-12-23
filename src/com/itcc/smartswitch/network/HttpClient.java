
package com.itcc.smartswitch.network;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HTTP;

import com.itcc.smartswitch.network.HttpParam.Method;
import com.itcc.smartswitch.network.HttpParam.Request;
import com.itcc.smartswitch.utils.LogEx;

public class HttpClient {

    private static final String TAG = HttpClient.class.getSimpleName();

    private static final int CONNECTION_TIME_OUT = 6 * 1000;
    private static final int SOCKET_TIME_OUT = 10 * 1000;
    private static final int RETRY_COUNT = 3;

    public interface Observer {
        public enum Reason {
            Success,
            Failed,
            Canceled,
        }

        public static final int UNKNOWN_LENGTH = Integer.MIN_VALUE;

        public void onStart();

        public void onSend(int total, int newFinished);

        public void onStartReceive(int total);

        public void onReceive(int total, int newFinished, final byte[] buffer);

        public void onFinish(Reason reason);

        public String getPostData();
    }

    private DefaultHttpClient mHttpclient;
    private HttpUriRequest mUriRequest;
    private boolean mCanceled = false;

    HttpClient() {

    }

    public void execute(final Request request, final Observer observer) {
        prepare();

        boolean finished = false;

        observer.onStart();

        if (Method.GET == request.method) {
            mUriRequest = setUrlParams(new HttpGet(request.host), request);
        } else {
            mUriRequest = setUrlParams(new HttpPost(request.host), request);

            String postdata = observer.getPostData();

            if (postdata != null) {

                StringEntity se = null;
                try {
                    se = new StringEntity(postdata, "utf-8");
                    ((HttpPost) mUriRequest).setEntity(se);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        setHttpHeaders(mUriRequest, request);
        setProxy(mHttpclient, request);

        try {
            observer.onSend(Observer.UNKNOWN_LENGTH, 0);
            if (LogEx.DEBUG_ENABLE) {
//                ConnectivityManager manager = (ConnectivityManager) LauncherApplication.getInstance()
//                        .getSystemService(Context.CONNECTIVITY_SERVICE);
//                if (null != manager) {
//                    NetworkInfo activeInfo = manager.getActiveNetworkInfo();
//                    if (null != activeInfo) {
//                        if (activeInfo.isConnected()) {
//                            LogEx.d(TAG, activeInfo.getTypeName() 
//                                    + " connected: " 
//                                    + mUriRequest.getURI().toString());
//                        } else {
//                            LogEx.d(TAG, "network unconnected: " + mUriRequest.getURI().toString());
//                        }
//                    } else {
//                        LogEx.d(TAG, "unknown network: " + mUriRequest.getURI().toString());    
//                    }
//                } else {
//                    LogEx.d(TAG, "unknown network: " + mUriRequest.getURI().toString());
//                }
            }
            HttpResponse response = mHttpclient.execute(mUriRequest);         
            if (LogEx.DEBUG_ENABLE) {
                LogEx.d(TAG, "response: " + response.getStatusLine().getStatusCode() 
                        + " " + response.getStatusLine().getReasonPhrase()
                        + " [ " + mUriRequest.getURI().toString() + " ]");
                HttpEntity httpEntity = response.getEntity();
                if (httpEntity != null) {
                    int bodyLength = (int) httpEntity.getContentLength();
                    LogEx.d(TAG, "response entity length: " + bodyLength);
                } else {
                    LogEx.d(TAG, "response entity length: 0");
                }
            }
            if (!mCanceled) {

                final int statusCode = response.getStatusLine().getStatusCode();
                if (statusCode == HttpStatus.SC_OK || statusCode == HttpStatus.SC_PARTIAL_CONTENT) {
                    // parseHttpHeaders(response, request);

                    HttpEntity httpEntity = response.getEntity();
                    if (httpEntity != null) {
                        InputStream body = httpEntity.getContent();
                        final int bodyLength = (int) httpEntity.getContentLength();

                        observer.onStartReceive(bodyLength);

                        int len = 0;
                        int totalLen = 0;
                        byte[] buf = new byte[1024 * 10];

                        while ((len = body.read(buf)) != -1 && !mCanceled) {
                            totalLen += len;
                            observer.onReceive(bodyLength, len, buf);
                        }
                        
                        if (LogEx.DEBUG_ENABLE) {
                            LogEx.d(TAG, "response content length: " + totalLen);
                        }
                        
                    }

                    finished = true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            LogEx.e(TAG, "http request exception: " + e.getLocalizedMessage());
            LogEx.e(TAG, mUriRequest.getURI().toString());
        } finally {

        }

        if (!mCanceled) {
            shutdown();
        }

        if (mCanceled) {
            observer.onFinish(Observer.Reason.Canceled);
        } else if (finished) {
            observer.onFinish(Observer.Reason.Success);
        } else {
            observer.onFinish(Observer.Reason.Failed);
        }

    }

    public void cancel() {
        mCanceled = true;
    }

    private void prepare() {
        mCanceled = false;

        KeyStore trustStore;
        try {
            trustStore = KeyStore.getInstance(KeyStore.getDefaultType());

            trustStore.load(null, null);

            SSLSocketFactory sf;

            sf = new TrustSSLSocketFactory(trustStore);

            sf.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);

            HttpParams params = new BasicHttpParams();
            HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
            HttpProtocolParams.setContentCharset(params, HTTP.UTF_8);

            SchemeRegistry registry = new SchemeRegistry();
            registry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
            registry.register(new Scheme("https", sf, 443));

            ClientConnectionManager ccm = new ThreadSafeClientConnManager(params, registry);

            HttpConnectionParams.setConnectionTimeout(params, CONNECTION_TIME_OUT);
            HttpConnectionParams.setSoTimeout(params, SOCKET_TIME_OUT);
            mHttpclient = new DefaultHttpClient(ccm, params);

            // mHttpclient = new DefaultHttpClient(params);
            mHttpclient
                    .setHttpRequestRetryHandler(new DefaultHttpRequestRetryHandler(RETRY_COUNT,
                            true));
        } catch (KeyStoreException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (CertificateException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (KeyManagementException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (UnrecoverableKeyException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private void shutdown() {
        try {
            if (mUriRequest != null) {
                // Aborts execution of the request.
                mUriRequest.abort();
                mUriRequest = null;
            }

            if (mHttpclient != null) {
                // When HttpClient instance is no longer needed, shut down the
                // connection
                // manager to ensure immediate deallocation of all system
                // resources
                mHttpclient.getConnectionManager().shutdown();
                mHttpclient = null;
            }
        } catch (Exception e) {
            LogEx.e(TAG, "http request shutdown exception: " + e.getLocalizedMessage());
            if (mUriRequest != null) {
                LogEx.e(TAG, mUriRequest.getURI().toString());
            }
            e.printStackTrace();
        }
    }

    private static void setProxy(DefaultHttpClient httpClient, final Request request) {
        if (request.proxy.isAvailable()) {
            HttpHost proxy = new HttpHost(request.proxy.url, request.proxy.port);
            httpClient.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, proxy);
        }
    }

    private static HttpGet setUrlParams(HttpGet get, final Request request) {
        List<NameValuePair> params = request.params;
        if (null != params && params.size() > 0) {
            String url = request.host + URLEncodedUtils.format(params, HTTP.UTF_8);
            try {
                get.setURI(new URI(url));
            } catch (Exception e) {
            }
        }

        return get;
    }

    private static HttpPost setUrlParams(HttpPost post, final Request request) {
        List<NameValuePair> params = request.params;
        if (null != params && params.size() > 0) {
            try {
                UrlEncodedFormEntity entity = new UrlEncodedFormEntity(params, HTTP.UTF_8);
                post.setEntity(entity);
            } catch (Exception e) {
            }
        }

        return post;
    }

    private void setHttpHeaders(HttpUriRequest uriRequest, final Request request) {
        // add range filed if set
        if (request.range.isAvailable()) {
            if (request.headers == null) {
                request.headers = new HashMap<String, String>();
            }

            request.headers.put("Range", request.range.toText());
        }

        // set all kvs to uriRequest
        if (request.headers != null) {
            for (Iterator<Entry<String, String>> iter = request.headers.entrySet().iterator(); iter
                    .hasNext();) {
                Entry<String, String> element = (Entry<String, String>) iter.next();
                if (element != null) {
                    String name = element.getKey();
                    String value = element.getValue();
                    if (trimString(name) && trimString(value)) {
                        uriRequest.setHeader(name, value);
                    }
                }
            }
        }
    }

    private static boolean trimString(String value) {
        if (value == null) {
            return false;
        } else {
            value = value.trim();
            return !value.isEmpty();
        }
    }
}
