package de.gebatzens.sia;

import android.content.Intent;
import android.util.Log;

import com.google.android.gms.common.data.FreezableUtils;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.WearableListenerService;

import java.util.ArrayList;

public class DataLayerListenerService extends WearableListenerService {


    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        final ArrayList<DataEvent> events = FreezableUtils
                .freezeIterable(dataEvents);

        for (DataEvent event : events) {
            DataItem item = event.getDataItem();
            String path = item.getUri().getPath();
            Log.d("ggvp", "received data for " + path);

            if(event.getType() == DataEvent.TYPE_CHANGED && path.equals("/filters")) {
                DataMap map = DataMapItem.fromDataItem(item).getDataMap();
                ArrayList<String> filters = map.getStringArrayList("filters");
                WearApp.APP.updateFilters(filters);

            }

        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        Log.d("ggvp", "SIA Wear Service started");

        return START_STICKY;
    }
}