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
        WeatherData data = getWeather();

        if (data.weatherId != 0) {
            notifyWearables(mGoogleClientApi, data);
        }
    }

    private WeatherData getWeather() {
        Log.d(TAG, "getWeather");
        String[] FORECAST_COLUMNS = {
                WeatherContract.WeatherEntry.COLUMN_WEATHER_ID,
                WeatherContract.WeatherEntry.COLUMN_MAX_TEMP,
                WeatherContract.WeatherEntry.COLUMN_MIN_TEMP,
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
        int weatherId = data.getInt(0);
        double maxTemp = data.getDouble(1);
        double minTemp = data.getDouble(2);
        data.close();

        return new WeatherData(weatherId,
                Utility.formatTemperature(this, minTemp),
                Utility.formatTemperature(this, maxTemp));
    }

    private void notifyWearables(GoogleApiClient client, WeatherData data) {
        PutDataMapRequest map = PutDataMapRequest.create("/weather");
        map.getDataMap().putInt("weatherId", data.weatherId);
        map.getDataMap().putString("tempLow", data.tempLow);
        map.getDataMap().putString("tempHigh", data.tempHigh);
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

    public class WeatherData {
        int weatherId;
        String tempHigh;
        String tempLow;

        public WeatherData(int weatherId, String tempLow, String tempHigh) {
            this.weatherId = weatherId;
            this.tempLow = tempLow;
            this.tempHigh = tempHigh;
        }
    }
}
