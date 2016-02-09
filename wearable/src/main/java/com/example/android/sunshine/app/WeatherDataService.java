package com.example.android.sunshine.app;

import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.WearableListenerService;

public class WeatherDataService extends WearableListenerService {

    public static final String TEMP_BROADCAST_NAME = "WeatherDataService:WeatherBroadcast";
    public static final String TEMP_HIGH = "tempHigh";
    public static final String TEMP_LOW = "tempLow";

    private static final String TAG = "Wearable:Service";

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
}
