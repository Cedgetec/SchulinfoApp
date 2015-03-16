/*
 * Copyright (C) 2015 Hauke Oldsen
 *
 * This file is part of GGVertretungsplan.
 *
 * GGVertretungsplan is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * GGVertretungsplan is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with GGVertretungsplan.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.gebatzens.ggvertretungsplan;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.SystemClock;
import android.util.Log;

import org.meqantt.MqttListener;
import org.meqantt.SocketClient;

import java.io.UnsupportedEncodingException;

import de.gebatzens.ggvertretungsplan.provider.GGProvider;
import de.gebatzens.ggvertretungsplan.provider.VPProvider;

public class MQTTService extends IntentService {

    int id = 1000;

    public MQTTService() {
        super("GGService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
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

        SocketClient client2 = new SocketClient("GGSchulinfoApp");
        try {
            client2.connect("gymnasium-glinde.logoip.de", 1883, new String[]{"TLSv1"}, GGProvider.sslSocketFactory);
            Log.w("ggmqtt", "Connected " + token);
            client2.setListener(new MqttListener() {
                @Override
                public void disconnected() {
                    Log.w("ggmqtt", "Disconnected!");
                }

                @Override
                public void publishArrived(String topic, byte[] bytes) {
                    if(!topic.equals("gg/schulinfoapp/"+token))
                        return;
                    String msg = null;
                    try {
                        msg = new String(bytes, "UTF-8");
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                        return;
                    }
                    Log.i("ggmqtt", "RECEIVED MESSAGE " + topic + " " + msg);
                    String[] s = msg.split(";");
                    if(s.length > 1)
                        GGApp.GG_APP.createNotification(R.drawable.ic_gg_star, s[0], s[1], new Intent(MQTTService.this, MainActivity.class), id++);
                }
            });
            client2.subscribe("gg/schulinfoapp/" + token);

        } catch (Exception e) {
            Log.e("ggmqtt", "Failed to connect to Server", e);
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
