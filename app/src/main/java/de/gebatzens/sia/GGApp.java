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

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Application;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Build;
import android.preference.PreferenceManager;
import android.provider.CalendarContract;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatDelegate;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.TimeZone;

import de.gebatzens.sia.data.Exams;
import de.gebatzens.sia.data.Filter;
import de.gebatzens.sia.data.GGPlan;
import de.gebatzens.sia.data.Mensa;
import de.gebatzens.sia.data.News;
import de.gebatzens.sia.data.StaticData;
import de.gebatzens.sia.fragment.RemoteDataFragment;
import de.gebatzens.sia.fragment.SubstFragment;

public class GGApp extends Application {

    public static final int UPDATE_DISABLE = 0, UPDATE_WLAN = 1, UPDATE_ALL = 2;
    public static GGApp GG_APP;

    public MainActivity activity;
    public GGRemote remote;
    public School school;

    public SharedPreferences preferences;

    public Filter.FilterList filters = new Filter.FilterList();
    public HashMap<String, String> subjects = new HashMap<>();

    public LifecycleHandler lifecycle;

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

        //this is required because SetupActivity might change the school in background
        school = School.getBySID(preferences.getString("sid", null));
        if(school != null) {
            school = new School(school);
        }

        loadSavedData();
        lifecycle = new LifecycleHandler();
        registerActivityLifecycleCallbacks(lifecycle);

        //1.3 upgrade
        if(preferences.contains("notification_led")) {
            preferences.edit().putString("notification_led_color", preferences.getBoolean("notification_led", true) ? "#2196F3" : "#000000").remove("notification_led").apply();
        }

    }

    private void loadSavedData() {
        if(school != null) {
            for(FragmentData frag : school.fragments) {

                RemoteDataFragment.RemoteData rd = null;

                switch(frag.type) {
                    case PLAN:
                        rd = new GGPlan.GGPlans();
                        break;
                    case EXAMS:
                        rd = new Exams();
                        break;
                    case NEWS:
                        rd = new News();
                        break;
                    case MENSA:
                        rd = new Mensa();
                        break;
                    case PDF:
                        rd = new StaticData();
                        ((StaticData) rd).name = frag.params;
                        break;
                }

                if(!rd.load())
                    rd = null;

                frag.setData(rd);
            }
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

    public void createNotification(int icon, String title, String message, Intent intent, int id, String... strings) {
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(icon)
                        .setContentTitle(title)
                        .setContentText(message);
        if (strings.length > 1) {
            NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();
            inboxStyle.setBigContentTitle(strings[0]);

            boolean b = true;
            for (String s : strings) {
                if (!b) {
                    inboxStyle.addLine(s);
                }
                b = false;
            }

            mBuilder.setStyle(inboxStyle);
        }
        mBuilder.setColor(GGApp.GG_APP.school.getDarkColor());
        if (Color.parseColor(getLedColor()) != Color.BLACK) {
            mBuilder.setLights(Color.parseColor(getLedColor()), 1000, 1000);
        }

        String vibration = preferences.getString("vibration", "off");
        switch (vibration) {
            case "short":
                mBuilder.setVibrate(new long[]{0, 500});
                break;
            case "default":
                mBuilder.setVibrate(new long[]{0, 200, 200, 200, 200, 200});
                break;
            case "long":
                mBuilder.setVibrate(new long[]{0, 200, 100, 400, 200, 800, 300, 1000, 1200, 200});
                break;
        }

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addParentStack(MainActivity.class);
        stackBuilder.addNextIntent(intent);
        PendingIntent resultPendingIntent =
                stackBuilder.getPendingIntent(
                        0,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );
        mBuilder.setContentIntent(resultPendingIntent);
        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(id, mBuilder.build());
    }

    public long getCalendarId() {
        //TODO select default calendar and try catch

        boolean hasPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_CALENDAR) == PackageManager.PERMISSION_GRANTED;

        if(!hasPermission && lifecycle.isAppInForeground()) {
            ActivityCompat.requestPermissions(activity, new String[]{ Manifest.permission.WRITE_CALENDAR, Manifest.permission.READ_CALENDAR }, 1);
            return -2;
        } else if(hasPermission) {
            String[] projection = new String[]{CalendarContract.Calendars._ID, CalendarContract.Calendars.NAME};
            Cursor cursor = getContentResolver().query(CalendarContract.Calendars.CONTENT_URI, projection, null, null, null);
            if (cursor.moveToFirst()) {
                long id = cursor.getLong(0);
                cursor.close();
                return id;
            }
        }

        Log.w("ggvp", "No calendar available");
        return -1;

    }

    public void addToCalendar(long calId, Date date, String title) {
        //TODO getTime should be enough
        long start = date.getTime() + 1000 * 60 * 60 * 6;
        ContentValues values = new ContentValues();
        values.put(CalendarContract.Events.DTSTART, start);
        values.put(CalendarContract.Events.DTEND, start);
        values.put(CalendarContract.Events.CALENDAR_ID, calId);
        values.put(CalendarContract.Events.ACCESS_LEVEL, CalendarContract.Events.ACCESS_DEFAULT);
        values.put(CalendarContract.Events.ALL_DAY, 1);
        values.put(CalendarContract.Events.TITLE, title);
        values.put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().getID());
        getContentResolver().insert(CalendarContract.Events.CONTENT_URI, values).getLastPathSegment();
    }

    public int translateUpdateType(String s) {
        switch (s) {
            case "disable":
                return UPDATE_DISABLE;
            case "wifi":
                return UPDATE_WLAN;
            case "all":
                return UPDATE_ALL;
        }
        return UPDATE_DISABLE;
    }

    public int getUpdateType() {
        if(preferences.getBoolean("notifications", true))
            return translateUpdateType(preferences.getString("background_updates", "all"));
        else
            return UPDATE_DISABLE;
    }

    public int getFragmentIndex() {
        return preferences.getInt("fragindex", 0);
    }

    public void setFragmentIndex(int index) {
        preferences.edit().putInt("fragindex", index).apply();
    }

    public String getLedColor() {
        return preferences.getString("notification_led_color", "#2196F3");
    }

    public void setLedColor(String ledColor) {
        preferences.edit().putString("notification_led_color", ledColor).apply();
    }

    public String getCustomThemeName() {
        return preferences.getString("customTheme", null);
    }

    public void setCustomThemeName(String customTheme) {
        preferences.edit().putString("customTheme", customTheme).apply();
    }

    public int getThemeMode() {
        return preferences.getInt("themeMode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
    }

    public void setThemeMode(int e) {
        preferences.edit().putInt("themeMode", e).apply();
        AppCompatDelegate.setDefaultNightMode(e);
    }

    public void setSchool(String sid) {
        preferences.edit().putString("sid", sid).apply();
        if(sid == null)
            return;

        School b = school;
        school = School.getBySID(sid);
        if(school != null) {
            school = new School(school);
        }

        if(activity != null && b != null && !b.equals(school)) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    activity.recreate();
                }
            });

        }

    }

    public String getCurrentThemeName() {
        String s = getCustomThemeName();
        return s == null ? school.themeName : s;
    }

    public void refreshAsync(final Runnable finished, final boolean updateFragments, final FragmentData frag) {
        new Thread() {
            @Override
            public void run() {
                boolean update = false;
                switch(frag.type) {
                    case PLAN:
                        GGPlan.GGPlans oldPlans = (GGPlan.GGPlans) frag.getData();
                        final GGPlan.GGPlans plans = remote.getPlans(updateFragments);
                        frag.setData(plans);
                        if(plans.throwable == null)
                            plans.save();


                        if(activity != null && (oldPlans == null || plans.size() != oldPlans.size())) {
                            update = true;
                        } else if (updateFragments) {
                            update = oldPlans.shouldRecreateView(plans);

                        }

                        if(activity != null) {
                            activity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Fragment frag = activity.mContent;
                                    if(frag instanceof SubstFragment)
                                     ((SubstFragment) frag).updateTime(plans.loadDate);
                                }
                            });

                        }

                        break;
                    case NEWS:
                        News on = (News) frag.getData();
                        News news = remote.getNews(updateFragments);
                        frag.setData(news);
                        if(news.throwable == null)
                            on.save();
                        update = on == null || !on.equals(news);
                        break;
                    case MENSA:
                        Mensa om = (Mensa) frag.getData();
                        Mensa mensa = remote.getMensa(updateFragments);
                        frag.setData(mensa);
                        if(mensa.throwable == null)
                            mensa.save();
                        update = om == null || !om.equals(mensa);
                        break;
                    case EXAMS:
                        Exams newExams = remote.getExams(updateFragments);
                        Exams exams = (Exams) frag.getData();
                        if(exams != null) {
                            exams.reuseSelected(newExams);
                        }
                        exams = newExams;
                        frag.setData(exams);
                        if(exams.throwable == null)
                            exams.save();
                        update = true;
                        break;
                    case PDF:
                        StaticData od = (StaticData) frag.getData();
                        StaticData data = remote.downloadStaticFile(frag.getParams(), updateFragments);
                        frag.setData(data);
                        if(data.throwable == null)
                            data.save();

                        update = data.throwable == null;
                        break;
                }

                if(activity != null && updateFragments && update)
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            activity.mContent.updateFragment();
                        }
                    });

                if(activity != null && finished != null)
                    activity.runOnUiThread(finished);
            }
        }.start();
    }

    public String getSeasonTheme() {
        int m = Calendar.getInstance().get(Calendar.MONTH);
        switch(m) {
            case Calendar.DECEMBER:
            case Calendar.JANUARY:
            case Calendar.FEBRUARY:
                return "Winter";
            case Calendar.MAY:
            case Calendar.JUNE:
            case Calendar.JULY:
            case Calendar.AUGUST:
                return "Summer";
            default:
                return null;
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public void setStatusBarColorTransparent(Window w) {
        w.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        w.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        w.setStatusBarColor(ContextCompat.getColor(this, R.color.transparent));
    }


    public static String deleteNonAlphanumeric(String s) {
        return s.replaceAll("\\W", "").toLowerCase();
    }

}
