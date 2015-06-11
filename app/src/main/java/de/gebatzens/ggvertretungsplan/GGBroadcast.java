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
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.util.Log;

import java.util.List;

import de.gebatzens.ggvertretungsplan.data.GGPlan;
import de.gebatzens.ggvertretungsplan.fragment.SubstFragment;

public class GGBroadcast extends BroadcastReceiver {

    public void checkForUpdates(final GGApp gg, boolean notification) {
        if(!gg.notificationsEnabled() && notification)
            return;
        if(gg.getUpdateType() == GGApp.UPDATE_DISABLE) {
            Log.w("ggvp", "update disabled");
            return;
        }
        boolean w = isWlanConnected(gg);
        if(!w && gg.getUpdateType() == GGApp.UPDATE_WLAN ) {
            Log.w("ggvp", "wlan not conected");
            return;
        }
        GGRemote r = GGApp.GG_APP.remote;

        GGPlan.GGPlans newPlans = r.getPlans(false);
        GGPlan.GGPlans oldPlans = gg.plans;

        if(newPlans.throwable != null || oldPlans == null || oldPlans.throwable != null)
            return;

        boolean n = false;
        for(int i = 0; i < newPlans.size() && !n; i++) {
            List<GGPlan.Entry> newList = newPlans.get(i).filter(gg.filters);
            GGPlan old = oldPlans.getPlanByDate(newPlans.get(i).date);
            if(old == null || !old.filter(gg.filters).equals(newList)) {
                n = true;
            }

        }

        gg.plans = newPlans;
        if(gg.activity != null && gg.getFragmentType() == GGApp.FragmentType.PLAN)
            gg.activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    gg.activity.mContent.updateFragment();
                }
            });

        if(n) {
            Intent intent = new Intent(gg, MainActivity.class);
            intent.putExtra("fragment", "PLAN");
            gg.createNotification(R.drawable.ic_gg_notification, gg.getString(R.string.schedule_change), gg.getString(R.string.schedule_changed),
                    intent, 123, true/*, gg.getString(R.string.affected_lessons) , today.getWeekday() + ": " + stdt,
                    tomo.getWeekday() + ": " + stdtm*/);
        }

    }

    public void checkForAppUpdates(GGApp gg) {
        if(!gg.appUpdatesEnabled())
            return;
        if(BuildConfig.DEBUG)
            return;
        try {
            String version = SettingsActivity.getVersion();
            if(!version.equals(BuildConfig.VERSION_NAME)) {
                Intent intent = new Intent(gg, SettingsActivity.class);
                intent.putExtra("update", true);
                intent.putExtra("version", version);
                gg.createNotification(R.drawable.ic_notification_update, gg.getString(R.string.app_update_title), gg.getString(R.string.app_update_message),
                        intent, 124, false);
            }
        } catch(Exception e) {
            e.printStackTrace();

        }
    }

    public static void createAlarm(Context context) {
        Intent i = new Intent(context, GGBroadcast.class);
        i.setAction("de.gebatzens.ACTION_ALARM");
        PendingIntent pi = PendingIntent.getBroadcast(context, 0, i, PendingIntent.FLAG_CANCEL_CURRENT);

        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        am.cancel(pi);
        am.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, 60000, AlarmManager.INTERVAL_FIFTEEN_MINUTES, pi);
    }

    public static boolean isWlanConnected(Context c) {
        ConnectivityManager cm = (ConnectivityManager) c.getSystemService(Context.CONNECTIVITY_SERVICE);
        return cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI).isConnected();

    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.w("ggvp", "onReceive " + intent.getAction());
        Intent intent1 = new Intent(context, MQTTService.class);
        context.startService(intent1);
        if(intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
            AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

            Intent i = new Intent(context, GGBroadcast.class);
            i.setAction("de.gebatzens.ACTION_ALARM");
            PendingIntent pi = PendingIntent.getBroadcast(context, 0, i, PendingIntent.FLAG_CANCEL_CURRENT);

            am.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, 1000, AlarmManager.INTERVAL_HALF_HOUR, pi);

        } else if (intent.getAction().equals("de.gebatzens.ACTION_ALARM")) {
            new AsyncTask<GGApp, Void, Void>() {

                @Override
                protected Void doInBackground(GGApp... params) {
                    checkForUpdates(params[0], true);
                    checkForAppUpdates(params[0]);
                    return null;
                }
            }.execute((GGApp) context.getApplicationContext());

        } else if (intent.getAction().equals(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION)) {
                new AsyncTask<GGApp, Void, Void>() {

                    @Override
                    protected Void doInBackground(final GGApp... params) {
                        int s = 0;
                        while(!isWlanConnected(params[0])) {
                            try {
                                Thread.sleep(100);
                            } catch (InterruptedException e) {

                            }
                            s++;
                            if(s > 100)
                                return null;
                        }
                        if(params[0].activity != null && params[0].getFragmentType() == GGApp.FragmentType.PLAN) {
                            params[0].activity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    ((SubstFragment)params[0].activity.mContent).substAdapter.setFragmentsLoading();
                                }
                            });

                            params[0].refreshAsync(null, true, params[0].getFragmentType());
                        } else {
                            checkForUpdates(params[0], false);
                        }
                        return null;
                    }
                }.execute((GGApp) context.getApplicationContext());

        }
    }

}
