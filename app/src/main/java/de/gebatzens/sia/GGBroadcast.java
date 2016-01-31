/*
 * Copyright 2015 - 2016 Hauke Oldsen
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

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import de.gebatzens.sia.data.GGPlan;
import de.gebatzens.sia.fragment.SubstFragment;

public class GGBroadcast extends BroadcastReceiver {

    public void checkForSubstUpdates(final GGApp gg) {
        if(gg.getUpdateType() == GGApp.UPDATE_DISABLE) {
            Log.w("ggvp", "update disabled");
            return;
        }

        boolean w = isWlanConnected(gg);
        if(!w && gg.getUpdateType() == GGApp.UPDATE_WLAN ) {
            Log.w("ggvp", "wlan not conected");
            return;
        }

        if(gg.school == null || gg.school.fragments.getData(FragmentData.FragmentType.PLAN).size() == 0) {
            Log.i("ggvp", "school does not have a PLAN fragment");
            return;
        }

        FragmentData planFrag = gg.school.fragments.getData(FragmentData.FragmentType.PLAN).get(0);

        final GGPlan.GGPlans newPlans = gg.remote.getPlans(false);
        GGPlan.GGPlans oldPlans = (GGPlan.GGPlans) planFrag.getData();
        planFrag.setData(newPlans);

        if(newPlans.throwable != null)
            return;

        newPlans.save();

        if(oldPlans == null || oldPlans.throwable != null)
            return;

        if(gg.activity != null && oldPlans.shouldRecreateView(newPlans) && !gg.lifecycle.isAppInForeground()) {
            gg.activity.finish();
        } else if(gg.activity != null && gg.activity.mContent instanceof SubstFragment) {
            gg.activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    SubstFragment frag = (SubstFragment) gg.activity.mContent;
                    if(frag != null) {
                        frag.updateTime(newPlans.loadDate);
                    }
                }
            });
        }

        List<GGPlan.Entry> diff = new ArrayList<>();
        for(int i = 0; i < newPlans.size(); i++) {
            GGPlan old = oldPlans.getPlanByDate(newPlans.get(i).date);

            if(old != null) {
                List<GGPlan.Entry> ne = new ArrayList<>();
                ne.addAll(newPlans.get(i).filter(gg.filters));
                ne.removeAll(old.filter(gg.filters));

                diff.addAll(ne);

            } else {
                // New day
                diff.addAll(newPlans.get(i).filter(gg.filters));
            }

        }

        if(diff.size() > 0) {
            Intent intent = new Intent(gg, MainActivity.class);
            intent.putExtra("fragment", "PLAN");
            if (diff.size() == 1) {
                GGPlan.Entry entry = diff.get(0);
                gg.createNotification(R.drawable.ic_notification, entry.lesson + ". " + gg.getString(R.string.lhour) + ": " + entry.type, entry.subject.replace("&#x2192;", ""),
                        intent, 123/*, gg.getString(R.string.affected_lessons) , today.getWeekday() + ": " + stdt,
                        tomo.getWeekday() + ": " + stdtm*/);

            } else {
                gg.createNotification(R.drawable.ic_notification, gg.getString(R.string.schedule_change), diff.size() + " " + gg.getString(R.string.new_entries),
                        intent, 123/*, gg.getString(R.string.affected_lessons) , today.getWeekday() + ": " + stdt,
                        tomo.getWeekday() + ": " + stdtm*/);
            }
        }

    }

    public static void createAlarm(Context context) {
        Intent i = new Intent(context, GGBroadcast.class);
        i.setAction("de.gebatzens.ACTION_ALARM");
        PendingIntent pi = PendingIntent.getBroadcast(context, 0, i, PendingIntent.FLAG_CANCEL_CURRENT);

        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        am.cancel(pi);
        am.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, 60000, AlarmManager.INTERVAL_FIFTEEN_MINUTES, pi);
        //am.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, 10000, 20000, pi);
    }

    public void updateAllFragments(GGApp gg) {
        //update every 2 hours

        int u = gg.preferences.getInt("allFragmentsUpdate", 0);
        if(u < 8)
            u++;
        gg.preferences.edit().putInt("allFragmentsUpdate", u).apply();

        if(gg.getUpdateType() == GGApp.UPDATE_DISABLE || !isWlanConnected(gg)) {
            return;
        }

        if(u != 8)
            return;

        u = 0;
        gg.preferences.edit().putInt("allFragmentsUpdate", u).apply();

        for(FragmentData frag : gg.school.fragments) {
            GGApp.GG_APP.refreshAsync(null, false, frag);
        }

    }

    @SuppressWarnings("deprecation")
    public static boolean isWlanConnected(Context c) {
        ConnectivityManager cm = (ConnectivityManager) c.getSystemService(Context.CONNECTIVITY_SERVICE);
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            for(Network n : cm.getAllNetworks()) {
                NetworkInfo info = cm.getNetworkInfo(n);
                if(info.getType() == ConnectivityManager.TYPE_WIFI && info.isConnected())
                    return true;
            }
            return false;
        } else {
            return cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI).isConnected();
        }

    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if(intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
            createAlarm(context);

        } else if (intent.getAction().equals("de.gebatzens.ACTION_ALARM")) {
            new AsyncTask<GGApp, Void, Void>() {

                @Override
                protected Void doInBackground(GGApp... params) {
                    Log.d("ggvp", "checking for updates");
                    checkForSubstUpdates(params[0]);
                    return null;
                }
            }.execute((GGApp) context.getApplicationContext());

        }
    }

}
