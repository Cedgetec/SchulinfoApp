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

import android.annotation.TargetApi;
import android.app.Application;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
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
import de.gebatzens.ggvertretungsplan.fragment.SubstFragment;

public class GGApp extends Application {

    public static final int UPDATE_DISABLE = 0, UPDATE_WLAN = 1, UPDATE_ALL = 2;
    public static GGApp GG_APP;

    public GGPlan.GGPlans plans;
    public News news;
    public Mensa mensa;
    public Exams exams;

    public MainActivity activity;
    public GGRemote remote;
    public School school;

    public SharedPreferences preferences;

    public Filter.FilterList filters = new Filter.FilterList();
    public HashMap<String, String> subjects = new HashMap<String, String>();

    @Override
    public void onCreate() {
        super.onCreate();
        GG_APP = this;
        remote = new GGRemote();
        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        GGBroadcast.createAlarm(this);
        filters = FilterActivity.loadFilter();
        loadSubjectMap();
        School.loadList();
        school = School.getBySID(preferences.getString("sid", null));
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

    private void loadSubjectMap() {
        subjects.clear();
        String[] array = getResources().getStringArray(R.array.subjects);
        for(String s : array) {
            String[] parts = s.split("\\|");
            String value = parts[parts.length - 1];
            for(int i = 0; i < parts.length - 1; i++) {
                subjects.put(parts[i], value);
            }
        }
    }

    public void createNotification(int icon, String title, String message, Intent intent, int id, boolean important, String... strings) {
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
        mBuilder.setColor(GGApp.GG_APP.school.darkColor);
        if(important) {
            mBuilder.setVibrate(new long[]{0, 1000});
            mBuilder.setLights(Color.argb(255, 0, 0, 255), 1000, 1000);
        }
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

    public void setSchool(String sid) {
        preferences.edit().putString("sid", sid).apply();
        school = School.getBySID(sid);
    }

    public String getDefaultSID() {
        return preferences.getString("sid", null);
    }

    public boolean appUpdatesEnabled() {
        return preferences.getBoolean("autoappupdates", true);
    }

    public void refreshAsync(final Runnable finished, final boolean updateFragments, final FragmentType type) {
        new Thread() {
            @Override
            public void run() {
                switch(type) {
                    case PLAN:
                        GGPlan.GGPlans oldPlans = plans;
                        plans = remote.getPlans(updateFragments);
                        if(activity != null && (oldPlans == null || plans.size() > oldPlans.size())) {
                            activity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    activity.removeAllFragments();
                                    FragmentTransaction transaction = activity.getSupportFragmentManager().beginTransaction();
                                    activity.mContent = activity.createFragment();
                                    transaction.replace(R.id.content_fragment, activity.mContent, "gg_content_fragment");
                                    transaction.commit();
                                }
                            });

                        } else if(activity != null) {
                            Fragment f = activity.getSupportFragmentManager().findFragmentByTag("gg_content_fragment");
                            if(f != null)
                                ((SubstFragment)f).substAdapter.update(plans);
                        }

                        break;
                    case NEWS:
                        news = remote.getNews();
                        break;
                    case MENSA:
                        mensa = remote.getMensa();
                        break;
                    case EXAMS:
                        exams = remote.getExams();
                        break;
                }

                if(updateFragments)
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
    public void setStatusBarColor(Window w, int color) {
        w.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        w.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        w.setStatusBarColor(color);
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
