package com.leo.operatepushlib.http;

import com.leo.operatepushlib.util.IOUtils;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;


/**
 * Create by LEO
 * on 2018/4/28
 * at 14:33
 * in MoeLove Company
 */

public class HttpUtil {
    public static Respond httpPost(String httpUrl, String data, int connectTimeout, int readTimeout) {
        Respond respond = new Respond();
        OutputStream outPut = null;
        HttpURLConnection urlConnection = null;
        InputStream in = null;
        try {
            URL url = new URL(httpUrl);
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("POST");
            urlConnection.setDoOutput(true);
            urlConnection.setDoInput(true);
            urlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
            urlConnection.setConnectTimeout(connectTimeout);
            urlConnection.setReadTimeout(readTimeout);
            urlConnection.connect();
            // POST data
            outPut = urlConnection.getOutputStream();
            outPut.write(data.getBytes("UTF-8"));
            outPut.flush();
            // read response
            int responseCode = urlConnection.getResponseCode();
            if (responseCode < 400) {
                respond.setCode(200);
                in = urlConnection.getInputStream();
            } else {
                respond.setCode(responseCode);
                in = urlConnection.getErrorStream();
            }
            String result = IOUtils.toString(in);
            respond.setBody(result);
            return respond;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            IOUtils.closeQuietly(outPut);
            IOUtils.closeQuietly(in);
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }
        return respond;
    }
}
