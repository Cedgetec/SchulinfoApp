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
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.util.Log;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Properties;

import de.gebatzens.ggvertretungsplan.data.GGPlan;
import de.gebatzens.ggvertretungsplan.fragment.SubstFragment;
import de.gebatzens.ggvertretungsplan.provider.VPProvider;

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
        VPProvider prov = gg.provider;

        GGPlan.GGPlans plans = prov.getPlans(false);
        gg.plans = plans;
        GGPlan today = plans.today;
        GGPlan tomo = plans.tomorrow;

        if(plans.throwable != null)
            return;

        if(gg.activity != null && gg.getFragmentType() == GGApp.FragmentType.PLAN)
            gg.activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    ((SubstFragment)gg.activity.mContent).mGGFrag.updateFragments();
                }
            });

        Properties p = new Properties();
        try {
            InputStream in;
            p.load(in = gg.openFileInput("ggsavedstate"));
            in.close();
        } catch(Exception e) {
            p.setProperty("todaydate", ""+today.date.getTime());
            p.setProperty("tomdate", ""+tomo.date.getTime());
            p.setProperty("todayles", "");
            p.setProperty("tomoles", "");
        }

        boolean b = false;

        String[] td = p.getProperty("todayles").split(";");
        List<GGPlan.Entry> listtd = today.filter(GGApp.GG_APP.filters);
        String[] tdn = new String[listtd.size()];
        int i = 0;
        for(GGPlan.Entry e : listtd) {
            tdn[i] = e.hour;
            i++;
        }
        if(td[0].isEmpty())
            td = new String[0];
        if(tdn.length != td.length) { //Heute fällt mehr/(weniger) aus
            b = true;
            Log.d("ggvp", "Heutestd nicht gleich " + tdn.length + " " + td.length);

        }

        String[] tm = p.getProperty("tomoles").split(";");
        List<GGPlan.Entry> listtm = tomo.filter(GGApp.GG_APP.filters);
        String[] tmn = new String[listtm.size()];
        i = 0;
        for(GGPlan.Entry e : listtm) {
            tmn[i] = e.hour;
            i++;
        }
        if(tm[0].isEmpty())
            tm = new String[0];
        if(tmn.length != tm.length) { //Morgen fällt mehr/(weniger) aus
            b = true;
            Log.d("ggvp", "Morgenstd nicht gleich " + tmn.length + " " + tm.length);
        }

        if(today.date.getTime() != Long.parseLong(p.getProperty("todaydate"))) {
            b = true;
            Log.d("ggvp", "Datum anders: " + today.date + " " + p.getProperty("todaydate"));
        }

        //Nichts neues, morgen fällt nichts aus
        if(today.date.getTime() == Long.parseLong(p.getProperty("tomdate")) && tmn.length == 0 && tdn.length == tm.length) {
            b = false;
        }

        p.setProperty("todaydate", ""+today.date.getTime());
        p.setProperty("tomdate", ""+tomo.date.getTime());
        String tdnstr = "";
        for(String s : tdn)
            tdnstr += s + ";";
        if(!tdnstr.isEmpty())
            tdnstr = tdnstr.substring(0, tdnstr.length() - 1);
        p.setProperty("todayles", tdnstr);
        String tmnstr = "";
        for(String s : tmn)
            tmnstr += s + ";";
        if(!tmnstr.isEmpty())
            tmnstr = tmnstr.substring(0, tmnstr.length() - 1);
        p.setProperty("tomoles", tmnstr);

        if(b) {
            String stdt = "";
            for(String s : tdn)
                stdt += s + ", ";
            if(!stdt.isEmpty())
                stdt = stdt.substring(0, stdt.length() - 2);
            else
                stdt = gg.getString(R.string.nothing);
            String stdtm = "";
            for(String s : tmn)
                stdtm += s + ", ";
            if(!stdtm.isEmpty())
                stdtm = stdtm.substring(0, stdtm.length() - 2);
            else
                stdtm = gg.getString(R.string.nothing);
            Intent intent = new Intent(gg, MainActivity.class);
            intent.putExtra("fragment", "PLAN");
            gg.createNotification(R.drawable.ic_gg_star, gg.getString(R.string.substitutionplan_change), gg.getString(R.string.the_sp_has_changed),
                    intent, 123, gg.getString(R.string.affected_lessons) , VPProvider.getWeekday(today.date) + ": " + stdt,
                    VPProvider.getWeekday(tomo.date) + ": " + stdtm);
        } else
            Log.d("ggvp", "Up to date!");

        try {
            OutputStream out;
            p.store(out = gg.openFileOutput("ggsavedstate", Context.MODE_PRIVATE), "GGSavedState");
            out.close();
        } catch(Exception e) {
            e.printStackTrace();
            gg.showToast(e.getClass().getName() + " " + e.getMessage());
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
                gg.createNotification(R.drawable.ic_notification_update, gg.getString(R.string.infoappupdate), gg.getString(R.string.appupdate_available),
                        intent, 124);
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
        am.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, 60000, AlarmManager.INTERVAL_HALF_HOUR, pi);
    }

    public static boolean isWlanConnected(Context c) {
        ConnectivityManager cm = (ConnectivityManager) c.getSystemService(Context.CONNECTIVITY_SERVICE);
        return cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI).isConnected();

    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.w("ggvp", "onReceive " + intent.getAction());
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
                                    ((SubstFragment)params[0].activity.mContent).mGGFrag.setFragmentsLoading();
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
