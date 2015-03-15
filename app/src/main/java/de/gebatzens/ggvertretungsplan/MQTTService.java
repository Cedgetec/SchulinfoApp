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
import android.os.*;
import android.os.Process;
import android.util.Log;

import org.fusesource.mqtt.client.BlockingConnection;
import org.fusesource.mqtt.client.MQTT;
import org.fusesource.mqtt.client.Message;
import org.fusesource.mqtt.client.QoS;
import org.fusesource.mqtt.client.Topic;

import java.io.InputStream;
import java.net.URISyntaxException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

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

        String token;
        if((token = provider.prefs.getString("token", null)) == null) {
            Log.w("ggmqtt", "Not Logged in");
            return;
        }

        MQTT client = new MQTT();
        try {
            client.setSslContext(GGProvider.sc);
            client.setHost("tlsv1://gymnasium-glinde.logoip.de:1883");
        } catch (URISyntaxException e) {
            Log.e("ggmqtt", "Failed to set Host", e);
        }
        client.setConnectAttemptsMax(1);
        BlockingConnection con = client.blockingConnection();
        try {
            con.connect();
            Log.w("ggmqtt", "Connected " + token);
            con.subscribe(new Topic[]{new Topic("gg/schulinfoapp/" + token, QoS.AT_LEAST_ONCE)});
            Log.w("ggmqtt", "Subscribed to gg/schulinfoapp/"+token);
            while(true) {
                Message message = con.receive();
                String msg = new String(message.getPayload(), "UTF-8");
                Log.i("ggmqtt", "RECEIVED MESSAGE " + message.getTopic() + " " + msg);
                String[] s = msg.split(";");
                if(s.length > 1)
                    GGApp.GG_APP.createNotification(R.drawable.ic_gg_star, s[0], s[1], new Intent(this, MainActivity.class), id++, "test");
                message.ack();
            }
        } catch (Exception e) {
            Log.e("ggmqtt", "Failed to connect to server", e);
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
