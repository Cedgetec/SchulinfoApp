/*
 * Copyright 2016 Hauke Oldsen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.gebatzens.sia;

import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.util.ArrayList;

import de.gebatzens.sia.data.Filter;

public class WearDataProvider {

    private void sendDataMap(final String path, final DataMap map) {
        new Thread() {
            @Override
            public void run() {
                GoogleApiClient apiClient = new GoogleApiClient.Builder(SIAApp.GG_APP).addApi(Wearable.API).build();
                if(!apiClient.blockingConnect().isSuccess()) {
                    Log.w("ggvp", "failed to connect to gapi");
                    return;
                }


                PutDataMapRequest putDataMapReq = PutDataMapRequest.create(path);
                putDataMapReq.getDataMap().putAll(map);
                putDataMapReq.setUrgent();
                PutDataRequest putDataReq = putDataMapReq.asPutDataRequest();
                PendingResult<DataApi.DataItemResult> pendingResult =
                        Wearable.DataApi.putDataItem(apiClient, putDataReq);
                if(pendingResult.await().getStatus().isSuccess()) {
                    Log.d("ggvp", "Successfully sent data map " + path);
                } else {
                    Log.w("ggvp", "Failed to sent data map " + path);
                }

                apiClient.disconnect();
            }
        }.start();

    }

    public void updateMainFilters(ArrayList<Filter.IncludingFilter> list) {
        DataMap map = new DataMap();

        ArrayList<String> filters = new ArrayList<>();
        for(Filter.IncludingFilter filter : list) {
            filters.add(filter.getFilter());
        }

        map.putStringArrayList("filters", filters);

        sendDataMap("/filters", map);
    }
}
