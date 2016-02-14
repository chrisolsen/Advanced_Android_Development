package com.example.android.sunshine.app;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

public class WeatherDataService extends WearableListenerService implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    public static final String TEMP_BROADCAST_NAME = "WeatherDataService:WeatherBroadcast";
    public static final String TEMP_HIGH = "tempHigh";
    public static final String TEMP_LOW = "tempLow";

    private static final String TAG = "Wearable:Service";
    private GoogleApiClient mGoogleClientApi;

    @Override
    public void onCreate() {
        super.onCreate();

        mGoogleClientApi = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        mGoogleClientApi.connect();
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        for (DataEvent dataEvent : dataEvents) {
            if (dataEvent.getType() == DataEvent.TYPE_CHANGED) {
                DataMap dataMap = DataMapItem.fromDataItem(dataEvent.getDataItem()).getDataMap();
                String path = dataEvent.getDataItem().getUri().getPath();
                if (path.equals("/weather")) {
                    String tempLow = dataMap.getString("tempLow");
                    String tempHigh = dataMap.getString("tempHigh");

                    LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(this);
                    Intent intent = new Intent(TEMP_BROADCAST_NAME);
                    intent.putExtra(TEMP_LOW, tempLow);
                    intent.putExtra(TEMP_HIGH, tempHigh);

                    lbm.sendBroadcast(intent);
                }
            }
        }
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.d(TAG, "onConnected");
        Wearable.NodeApi.getConnectedNodes(mGoogleClientApi).setResultCallback(new ResultCallback<NodeApi.GetConnectedNodesResult>() {
            @Override
            public void onResult(NodeApi.GetConnectedNodesResult result) {
                for (Node node : result.getNodes()) {
                    Wearable.MessageApi.sendMessage(
                            mGoogleClientApi, node.getId(), "send me the data", null)
                            .setResultCallback(new ResultCallback<MessageApi.SendMessageResult>() {
                                @Override
                                public void onResult(MessageApi.SendMessageResult r) {
                                    if (!r.getStatus().isSuccess()) {
                                        Log.d(TAG, "onConnected: Failed to send message to handheld");
                                    }
                                }
                            });
                }
            }
        });
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }
}
