package com.danvelazco.projectficarrier.service;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.preference.PreferenceManager;
import android.util.Log;
import com.danvelazco.projectficarrier.R;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import org.json.JSONObject;

/**
 * @since 6/12/15
 */
public class NetworkIntentService extends IntentService {

    // Constants
    private static final String TAG = "NetworkIntentService";
    private static final String IP_CHECK_URL = "http://ip-api.com/json";
    private static final long MAX_TIME_UPDATE = 500;
    private static final String KEY_UPDATE_TIME_PREF = "_last_network_request";

    // Members
    private MobileNetworkCallback mNetworkCallback = new MobileNetworkCallback();
    private ConnectivityManager mConnectivityManager;
    private Network mCurrentNetwork;

    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     */
    public NetworkIntentService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (mConnectivityManager == null) {
            mConnectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        }

        if (mCurrentNetwork == null) {
            NetworkRequest.Builder builder = new NetworkRequest.Builder();
            builder.addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
            builder.addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR);

            NetworkRequest networkRequest = builder.build();
            mConnectivityManager.requestNetwork(networkRequest, mNetworkCallback);
        } else {
            // A mobile network has already been selected and set, just make the http request
            makeHttpRequest();
        }
    }

    private void makeHttpRequest() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        long lastUpdateMillis = prefs.getLong(KEY_UPDATE_TIME_PREF, 0);

        if (System.currentTimeMillis() - lastUpdateMillis > MAX_TIME_UPDATE) {
            Log.d(TAG, "Enough time has passed since last update request... allowing...");
            String carrierName = "Unknown";

            try {
                ConnectivityManager connectivityManager = (ConnectivityManager)
                        getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
                if (networkInfo != null && networkInfo.isConnected()) {
                    Request request = new Request.Builder()
                            .url(IP_CHECK_URL)
                            .build();

                    Log.d(TAG, "Executing request...");
                    OkHttpClient httpClient = new OkHttpClient();
                    Response response = httpClient.newCall(request).execute();
                    String jsonResponseString = response.body().string();

                    JSONObject jsonResponse = new JSONObject(jsonResponseString);
                    String status = jsonResponse.optString("status", "fail");
                    if ("success".equalsIgnoreCase(status)) {
                        carrierName = jsonResponse.optString("org", "Unknown");
                        Log.d(TAG, "Carrier name: " + carrierName);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            updateNotification(carrierName);

            // Store the last time we sent an update request (now)
            SharedPreferences.Editor editor = prefs.edit();
            editor.putLong(KEY_UPDATE_TIME_PREF, System.currentTimeMillis());
            editor.apply();
        } else {
            Log.d(TAG, "Too little time has passed since last update request... ignoring...");
        }
    }

    private void updateNotification(String carrierName) {
        Log.d(TAG, "updateNotification()");

        int drawableIcon = R.mipmap.ic_launcher;
        if (carrierName.contains("Sprint")) {
            drawableIcon = R.drawable.logo_sprint;
        } else if (carrierName.contains("T-Mobile")) {
            drawableIcon = R.drawable.logo_tmo;
        }

        Bitmap icon = BitmapFactory.decodeResource(getResources(), drawableIcon);

        Notification notification = new Notification.Builder(this)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(carrierName)
                .setLargeIcon(icon)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setOngoing(true)
                .setPriority(Notification.PRIORITY_MIN)
                .build();

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(-1, notification);
    }

    private class MobileNetworkCallback extends ConnectivityManager.NetworkCallback {
        @Override
        public void onAvailable(Network network) {
            mCurrentNetwork = network;
            ConnectivityManager.setProcessDefaultNetwork(network);
            makeHttpRequest();
        }
    }

}
