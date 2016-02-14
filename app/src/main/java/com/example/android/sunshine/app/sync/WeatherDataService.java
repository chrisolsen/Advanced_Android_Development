package com.example.android.sunshine.app.sync;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import com.example.android.sunshine.app.Utility;
import com.example.android.sunshine.app.data.WeatherContract;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

import java.util.Date;

public class WeatherDataService extends WearableListenerService implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private static final String TAG = "HandheldService";
    private GoogleApiClient mGoogleClientApi;

    @Override
    public void onCreate() {
        super.onCreate();

        Log.d(TAG, "initializing");
        mGoogleClientApi = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        mGoogleClientApi.connect();
    }
    
    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        super.onMessageReceived(messageEvent);
        Log.d(TAG, "onMessageReceived: " + messageEvent.getPath());
        sendDataToWearable();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand");

        sendDataToWearable();
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onPeerConnected(Node peer) {
        super.onPeerConnected(peer);
        Log.d(TAG, "onPeerConnected: " + peer.getDisplayName());
        sendDataToWearable();
    }

    private void sendDataToWearable() {
        String[] temps = getCurrentTemps();

        if (temps != null && temps.length == 2) {
            Log.d(TAG, String.format("onPeerConnected: temps %s %s", temps[0], temps[1]));
            notifyWearables(mGoogleClientApi, temps[0], temps[1]);
        }
    }

    private String[] getCurrentTemps() {
        Log.d(TAG, "getCurrentTemps");
        String[] FORECAST_COLUMNS = {
                WeatherContract.WeatherEntry.COLUMN_MAX_TEMP,
                WeatherContract.WeatherEntry.COLUMN_MIN_TEMP
        };

        // Get today's data from the ContentProvider
        String location = Utility.getPreferredLocation(this);
        Uri weatherForLocationUri = WeatherContract.WeatherEntry.buildWeatherLocationWithStartDate(
                location, System.currentTimeMillis());
        Cursor data = getContentResolver().query(
                weatherForLocationUri, FORECAST_COLUMNS, null,
                null, WeatherContract.WeatherEntry.COLUMN_DATE + " ASC");

        if (data == null) {
            return null;
        }
        if (!data.moveToFirst()) {
            data.close();
            return null;
        }

        if (!data.moveToFirst()) {
            data.close();
            return null;
        }

        // Extract the weather data from the Cursor
        double maxTemp = data.getDouble(0);
        double minTemp = data.getDouble(1);
        data.close();

        return new String[]{
                Utility.formatTemperature(this, minTemp),
                Utility.formatTemperature(this, maxTemp)
        };
    }

    private void notifyWearables(GoogleApiClient client, String low, String high) {
        Log.d(TAG, String.format("notifyWearables: %s %s", low, high));

        PutDataMapRequest map = PutDataMapRequest.create("/weather");
        map.getDataMap().putString("tempLow", low);
        map.getDataMap().putString("tempHigh", high);
        map.getDataMap().putLong("timestamp", new Date().getTime());

        PutDataRequest request = map.asPutDataRequest();

        Wearable.DataApi.putDataItem(client, request).setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
            @Override
            public void onResult(DataApi.DataItemResult result) {
                Log.d(TAG, String.format("onResult, %s", result.getStatus().getStatusMessage()));
                if (!result.getStatus().isSuccess()) {
                    Log.d(TAG, "onResult: Failed to send data");
                }
            }
        });

    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.d(TAG, "onConnected");
        Wearable.DataApi.addListener(mGoogleClientApi, this);
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d(TAG, "onConnectionSuspended");
        Wearable.DataApi.removeListener(mGoogleClientApi, this);
    }

    @Override
    public void onPeerDisconnected(Node peer) {
        super.onPeerDisconnected(peer);
        Log.d(TAG, "onPeerDisconnected: " + peer.getId());
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.d(TAG, "onConnectionFailed: ");
    }
}
