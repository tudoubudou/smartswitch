
package com.itcc.smartswitch.network;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

public class HttpParam {

    public enum Method {
        GET,
        POST,
    }

    public static class Host {
        final static int DEFAULT_PORT = 80;

        public String url = "";
        public int port = DEFAULT_PORT;

        Host() {

        }

        public boolean isAvailable() {
            return (url != null && url.trim().length() > 0);
        }

        public String toHostString() {
            if (isAvailable()) {
                url = url.trim();
                if (port != DEFAULT_PORT && url.indexOf(':') < 0) {
                    return url + ":" + port;
                } else {
                    return url;
                }
            } else {
                return "";
            }
        }

        public void reset() {
            url = "";
            port = DEFAULT_PORT;
        }
    }

    public static class Range {
        public long begin = -1;
        public long end = -1;
        public long total = -1;

        Range() {

        }

        public void reset() {
            begin = end = total = -1;
        }

        public void set(long begin, long length) {
            this.begin = begin;
            end = begin + length - 1;
        }

        public void setStart(long begin) {
            this.begin = begin;
            end = -1;
        }

        public void next(int length) {
            begin = end + 1;
            end = begin + length - 1;
        }

        public boolean isFinished() {
            return (end + 1 >= total);
        }

        boolean isAvailable() {
            return (begin >= 0 || end > begin);
        }

        /*
         * "bytes=begin-end" : from (begin pos) to (end pos) bytes
         * "bytes=begin-" : from (begin pos) bytes to the end "bytes=-end" :
         * last (end pos) bytes
         */
        String toText() {
            if (isAvailable()) {
                String range = new String("bytes=");
                if (begin >= 0)
                    range += begin;
                range += "-";
                if (end >= 0)
                    range += end;
                return range;
            } else {
                return "";
            }
        }

        /*
         * parse "bytes 0-500/1000"
         */
//        void fromText(String value) {
//            String[] section = value.split(" ");
//            if (section.length > 1) {
//                section = section[1].split("/");
//                if (section.length > 1) {
//                    total = Integer.parseInt(section[1]);
//                    section = section[0].split("-");
//                    if (section.length > 1) {
//                        if (section[0].length() > 0) {
//                            begin = Integer.parseInt(section[0]);
//                        } else {
//                            begin = -1;
//                        }
//
//                        if (section[1].length() > 0) {
//                            end = Integer.parseInt(section[1]);
//                        } else {
//                            end = -1;
//                        }
//                    }
//                }
//            }
//        }
    }

    public static class Request {
        public Method method = Method.GET;
        public String host = "";
        public Host proxy = new Host();
        public Range range = new Range();

        List<NameValuePair> params;
        Map<String, String> headers;

        Request() {

        }

        public void set(Method method, String url) {
            this.method = method;
            host = url;
        }

        public boolean isAvailable() {
            return (host != null && host.trim().length() > 0);
        }

        public void reset() {
            method = Method.GET;
            host = "";

            proxy.reset();
            range.reset();

            params = null;
            headers = null;
        }

        public void setUrlParams(String name, String value) {
            if (params == null) {
                params = new ArrayList<NameValuePair>();
            }
            params.add(new BasicNameValuePair(name, value));
        }

        public void setHttpHeaders(String name, String value) {
            if (headers == null) {
                headers = new HashMap<String, String>();
            }
            headers.put(name, value);
        }
    }

}
