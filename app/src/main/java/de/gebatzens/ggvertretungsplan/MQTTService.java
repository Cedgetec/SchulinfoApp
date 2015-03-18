/*
 * Copyright 2015 Hauke Oldsen
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

package de.gebatzens.ggvertretungsplan;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.SystemClock;
import android.util.Log;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.nio.charset.Charset;

import de.gebatzens.ggvertretungsplan.provider.GGProvider;
import de.gebatzens.ggvertretungsplan.provider.VPProvider;

public class MQTTService extends IntentService {

    int id = 1000;
    boolean started = false;

    public MQTTService() {
        super("GGService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if(started)
            return;
        started = true;
        Log.w("ggmqtt", "MQTT Service started");
        VPProvider provider = GGApp.GG_APP.provider;
        if(!(provider instanceof GGProvider)) {
            Log.w("ggmqtt", "Provider not GGProvider " + provider);
            return;
        }

        final String token;
        if((token = provider.prefs.getString("token", null)) == null) {
            Log.w("ggmqtt", "Not Logged in");
            return;
        }


        try {
            MqttClient client = new MqttClient("ssl://gymnasium-glinde.logoip.de:1883", "SchulinfoApp/" + provider.getUsername(), new MemoryPersistence());
            MqttConnectOptions options = new MqttConnectOptions();
            options.setSocketFactory(GGProvider.sslSocketFactory);
            options.setCleanSession(true);
            client.connect(options);
            client.subscribe("gg/schulinfoapp/" + token);
            Log.w("ggmqtt", "Connected and subscribed " + token);

            client.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable throwable) {
                    Log.e("ggmqtt", "Connection lost", throwable);
                }

                @Override
                public void messageArrived(String s, MqttMessage mqttMessage) throws Exception {
                    handleMessage(mqttMessage.getPayload());
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {

                }
            });
        } catch (Exception e) {
            Log.e("ggmqtt", "Failed to connect to server", e);
        }

    }

    public void handleMessage(byte[] rawMsg) {
        String msg = new String(rawMsg, 0, rawMsg.length, Charset.forName("UTF-8"));
        String[] lines = msg.split("\n");
        Log.i("ggmqtt", "Received message: " + msg);
        try {
            String[] l1 = lines[0].split(" ");
            if(l1[0].equals("Notification")) {

                String[] s = lines[1].split(";");
                GGApp.GG_APP.createNotification(R.drawable.ic_gg_star, s[0], s[1], new Intent(this, MainActivity.class), id++, l1[1].equals("true"));
            } else if(l1[0].equals("Update")) {
                //TODO
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onTaskRemoved(Intent paramIntent) {
        try {
            if(!paramIntent.getStringExtra("runtimeLevel").trim().equals("exit")) {
                Intent localIntent = new Intent(getApplicationContext(), getClass());
                localIntent.setPackage(getPackageName());
                PendingIntent localPendingIntent = PendingIntent.getService(getApplicationContext(), 1, localIntent, PendingIntent.FLAG_ONE_SHOT);
                ((AlarmManager)getApplicationContext().getSystemService(ALARM_SERVICE)).set(3, 1000L + SystemClock.elapsedRealtime(), localPendingIntent);
                super.onTaskRemoved(paramIntent);
            }
        } catch(NullPointerException e) {
            Intent localIntent = new Intent(getApplicationContext(), getClass());
            localIntent.setPackage(getPackageName());
            PendingIntent localPendingIntent = PendingIntent.getService(getApplicationContext(), 1, localIntent, PendingIntent.FLAG_ONE_SHOT);
            ((AlarmManager)getApplicationContext().getSystemService(ALARM_SERVICE)).set(3, 1000L + SystemClock.elapsedRealtime(), localPendingIntent);
            super.onTaskRemoved(paramIntent);
        }
    }
}
