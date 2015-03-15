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

import android.annotation.TargetApi;
import android.app.Application;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import java.util.HashMap;

import de.gebatzens.ggvertretungsplan.data.Exams;
import de.gebatzens.ggvertretungsplan.data.Filter;
import de.gebatzens.ggvertretungsplan.data.GGPlan;
import de.gebatzens.ggvertretungsplan.data.Mensa;
import de.gebatzens.ggvertretungsplan.data.News;
import de.gebatzens.ggvertretungsplan.fragment.RemoteDataFragment;
import de.gebatzens.ggvertretungsplan.provider.GGProvider;
import de.gebatzens.ggvertretungsplan.provider.SWSProvider;
import de.gebatzens.ggvertretungsplan.provider.VPProvider;

public class GGApp extends Application {

    public GGPlan.GGPlans plans = null;
    public News news;
    public Mensa mensa;
    public MainActivity activity;
    public VPProvider provider;
    public Exams exams;
    public static final int UPDATE_DISABLE = 0, UPDATE_WLAN = 1, UPDATE_ALL = 2;
    private SharedPreferences preferences;
    public static GGApp GG_APP;
    private HashMap<String, Class<? extends VPProvider>> mProviderList = new HashMap<String, Class<? extends VPProvider>>();
    public Filter.FilterList filters = new Filter.FilterList();

    @Override
    public void onCreate() {
        super.onCreate();
        GG_APP = this;
        registerProviders();
        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        GGBroadcast.createAlarm(this);
        recreateProvider();
        filters = FilterActivity.loadFilter();

    }

    public RemoteDataFragment.RemoteData getDataForFragment(FragmentType type) {
        switch(type) {
            case PLAN:
                return plans;
            case NEWS:
                return news;
            case MENSA:
                return mensa;
            case EXAMS:
                return exams;
            default:
                return null;
        }
    }

    public void registerProviders() {
        mProviderList.clear();
        mProviderList.put("gg", GGProvider.class);
        mProviderList.put("sws", SWSProvider.class);
    }

    public void createNotification(int icon, String title, String message, Intent intent, int id, String... strings) {
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(icon)
                        .setContentTitle(title)
                        .setContentText(message);
        if(strings.length > 1) {
            NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();
            inboxStyle.setBigContentTitle(strings[0]);

            boolean b = true;
            for(String s : strings) {
                if(!b) {
                    inboxStyle.addLine(s);
                }
                b = false;
            }

            mBuilder.setStyle(inboxStyle);
        }
        mBuilder.setColor(GGApp.GG_APP.provider.getDarkColor());

        // The stack builder object will contain an artificial back stack for the
        // started Activity.
        // This ensures that navigating backward from the Activity leads out of
        // your application to the Home screen.
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        // Adds the back stack for the Intent (but not the Intent itself)
        stackBuilder.addParentStack(MainActivity.class);
        // Adds the Intent that starts the Activity to the top of the stack
        stackBuilder.addNextIntent(intent);
        PendingIntent resultPendingIntent =
                stackBuilder.getPendingIntent(
                        0,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );
        mBuilder.setContentIntent(resultPendingIntent);
        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        // mId allows you to update the notification later on.
        mNotificationManager.notify(id, mBuilder.build());
    }

    public String getSelectedProvider() {
        return preferences.getString("schule", "gg");
    }

    public boolean notificationsEnabled() {
        return preferences.getBoolean("benachrichtigungen", true);
    }

    public void showToast(String s) {
        Toast.makeText(getApplicationContext(), s, Toast.LENGTH_LONG).show();
    }

    public void recreateProvider() {
        createProvider(getSelectedProvider());
        if(provider instanceof GGProvider)
            startService(new Intent(this, MQTTService.class));
    }

    public void createProvider(String id) {
        Log.w("ggvp", "createProvider " + id);
        Class<? extends VPProvider> clas = mProviderList.get(id);
        if(clas == null)
            throw new RuntimeException("Provider for " + id + " not found");

        try {
            provider = (VPProvider) clas.getConstructors()[0].newInstance(this, id);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public int translateUpdateType(String s) {
        if(s.equals("disable"))
            return UPDATE_DISABLE;
        else if(s.equals("wifi"))
            return UPDATE_WLAN;
        else if(s.equals("all"))
            return UPDATE_ALL;
        return UPDATE_DISABLE;
    }

    public String translateUpdateType(int i) {
        String[] s = getResources().getStringArray(R.array.appupdatesArray);
        return s[i];
    }

    public int getUpdateType() {
        return translateUpdateType(preferences.getString("appupdates", "wifi"));
    }

    public FragmentType getFragmentType() {
        return FragmentType.valueOf(preferences.getString("fragtype", "PLAN"));
    }

    public void setFragmentType(FragmentType type) {
        preferences.edit().putString("fragtype", type.toString()).apply();
    }

    public boolean appUpdatesEnabled() {
        return preferences.getBoolean("autoappupdates", true);
    }

    public void refreshAsync(final Runnable finished, final boolean updateFragments, final FragmentType type) {
        Log.w("ggvp", "refreshAsync " + type + " " + provider);

        new Thread() {
            @Override
            public void run() {
                boolean update = updateFragments;
                switch(type) {
                    case PLAN:
                        GGPlan.GGPlans nplans = provider.getPlans(updateFragments);
                        //if(plans != null)
                        //    update = update && !(nplans[0].equals(plans[0]) && nplans[1].equals(plans[1]));
                        plans = nplans;
                        break;
                    case NEWS:
                        news = provider.getNews();
                        break;
                    case MENSA:
                        mensa = provider.getMensa();
                        break;
                    case EXAMS:
                        exams = provider.getExams();
                        break;
                }

                if(update)
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                        activity.mContent.updateFragment();
                        }
                    });

                if(finished != null)
                    activity.runOnUiThread(finished);
            }
        }.start();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public void setStatusBarColor(Window w) {
        w.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        w.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        w.setStatusBarColor(GGApp.GG_APP.provider.getDarkColor());
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public void setStatusBarColorTransparent(Window w) {
        w.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        w.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        w.setStatusBarColor(getResources().getColor(R.color.transparent));
    }

    public static enum FragmentType {
        PLAN, NEWS, MENSA, EXAMS
    }


}
