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

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Build;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Base64;
import android.util.Log;
import android.view.View;

import com.google.firebase.messaging.FirebaseMessaging;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.zip.GZIPInputStream;

import de.gebatzens.sia.data.Exams;
import de.gebatzens.sia.data.Filter;
import de.gebatzens.sia.data.Subst;
import de.gebatzens.sia.data.Mensa;
import de.gebatzens.sia.data.News;
import de.gebatzens.sia.data.StaticData;

public class SiaAPI {

    public static final String PREFS_NAME = "remoteprefs";

    SharedPreferences prefs;

    public SiaAPI() {
        prefs = SIAApp.GG_APP.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

    }

    public void showReloadSnackbar(final String msg) {
        if(SIAApp.GG_APP.activity == null)
            return;

        SIAApp.GG_APP.activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Snackbar.make(SIAApp.GG_APP.activity.getWindow().getDecorView().findViewById(R.id.coordinator_layout), msg, Snackbar.LENGTH_LONG)
                        .setAction(R.string.again, new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                final View rv = SIAApp.GG_APP.activity.getWindow().getDecorView();
                                ((SwipeRefreshLayout) rv.findViewById(R.id.refresh)).setRefreshing(true);
                                SIAApp.GG_APP.refreshAsync(new Runnable() {

                                    @Override
                                    public void run() {
                                        ((SwipeRefreshLayout) rv.findViewById(R.id.refresh)).setRefreshing(false);
                                    }

                                }, true, SIAApp.GG_APP.school.fragments.get(SIAApp.GG_APP.getFragmentIndex()));
                            }
                        }).show();
            }
        });

    }

    public void logout() {
        final String token = getToken();

        FirebaseMessaging.getInstance().unsubscribeFromTopic("sia_sid_" + SIAApp.GG_APP.school.sid);

        for(String name : SIAApp.GG_APP.fileList())
            if(name.startsWith("schedule"))
                SIAApp.GG_APP.deleteFile(name);
        SIAApp.GG_APP.deleteFile("news");
        SIAApp.GG_APP.deleteFile("mensa");
        SIAApp.GG_APP.deleteFile("exams");
        SIAApp.GG_APP.deleteFile("ggfilter");
        SIAApp.GG_APP.deleteFile("ggfilterV2");
        SIAApp.GG_APP.filters.clear();
        SIAApp.GG_APP.school = null;

        prefs.edit().clear().apply();
        SIAApp.GG_APP.preferences.edit().remove("customTheme").remove("sid").apply();

        new Thread() {
            @Override
            public void run() {
                try {
                    APIResponse re = doRequest("/logout?token=" + token, null);
                    if(re.state != APIState.SUCCEEDED) {
                        Log.w("ggvp", "Warning: Logout received " + re.state);
                    }
                } catch(Exception e) {
                    Log.w("ggvp", "Warning: Logout failed " + e.getMessage());
                }
            }
        }.start();

    }

    public Subst.GGPlans getPlans(boolean toast) {

        Subst.GGPlans plans = new Subst.GGPlans();

        String snackMessage = "";

        try {
            APIResponse re = doRequest("/subst?token=" + getToken(), null);

            if(re.state == APIState.SUCCEEDED) {
                Iterator<String> days = ((JSONObject) re.data).keys();
                while (days.hasNext()) {
                    String date = days.next();
                    JSONObject obj = ((JSONObject) re.data).getJSONObject(date);
                    Subst plan = new Subst();
                    plan.date = Subst.parseDate(date);
                    plans.add(plan);
                    JSONArray entries = obj.getJSONArray("entries");
                    getPlan(entries, plan);

                    JSONArray messages = obj.getJSONArray("messages");
                    for (int i = 0; i < messages.length(); i++) {
                        if(i < messages.length())
                            plan.special.add(messages.getString(i) + "\n");
                        else
                            plan.special.add(messages.getString(i));
                    }
                }
            } else {
                throw new APIException(re.reason);
            }
        } catch(Exception e) {
            if(e instanceof IOException || e instanceof APIException) {
                Log.w("ggvp", "Failed to get plans " + e.getMessage());
                snackMessage = e instanceof IOException ? SIAApp.GG_APP.getString(R.string.no_internet_connection) : e.getMessage();
            } else {
                snackMessage = SIAApp.GG_APP.getString(R.string.unknown_error);
                e.printStackTrace();
            }
            plans.throwable = e;

        }

        Collections.sort(plans, new Comparator<Subst>() {
            @Override
            public int compare(Subst lhs, Subst rhs) {
                return lhs.date.compareTo(rhs.date);
            }
        });

        if(plans.throwable != null) {
            if (plans.load()) {
                final Throwable t = plans.throwable;
                plans.throwable = null;
                if(toast)
                    showReloadSnackbar(snackMessage);
            }
        } else {
            SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm");
            plans.loadDate = new Date();

        }

        return plans;
    }

    private void getPlan(JSONArray array, Subst p) throws Exception {
        for(int i = 0; i < array.length(); i++) {
            Subst.Entry e = new Subst.Entry();
            e.date = p.date;
            p.add(e);
            JSONObject entry = array.getJSONObject(i);
            e.clazz = entry.getString("class");
            e.lesson = "" + entry.getInt("lesson");
            e.teacher = entry.getString("substitutor");
            e.missing = entry.getString("missing");
            e.subject = entry.getString("subject");
            e.repsub = entry.getString("substitutionsubject");
            e.type = entry.getString("substitutionplantype");
            e.comment = entry.getString("comment");
            e.room = entry.getString("room");

            e.unify();

        }

    }

    public News getNews(boolean toast) {
        News n = new News();
        try {

            DateFormat parser = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");

            APIResponse re = doRequest("/news?token=" + getToken(), null);

            if(re.state == APIState.SUCCEEDED) {

                JSONArray entries = (JSONArray) re.data;
                for(int i = 0; i < entries.length(); i++) {
                    JSONObject cobj = entries.getJSONObject(i);
                    News.Entry e = new News.Entry();
                    n.add(e);
                    e.id = cobj.getString("id");
                    e.date = parser.parse(cobj.getString("date"));
                    e.source = cobj.getString("source");
                    e.topic = cobj.getString("topic");
                    e.title = cobj.getString("title");
                    e.text = cobj.getString("text");

                }
            } else {
                throw new APIException(re.reason);
            }
        } catch (final Exception e) {
            e.printStackTrace();
            if(toast)
                showReloadSnackbar(e instanceof IOException ? SIAApp.GG_APP.getString(R.string.no_internet_connection) : e.getMessage());
            if(!n.load()) {
                n.throwable = e;
            }
        }

        return n;

    }

    public StaticData downloadStaticFile(String name, boolean snack) {
        StaticData data = new StaticData();
        data.name = name;

        try {
            APIResponse re = doRequest("/static?token=" + getToken() + "&file=" + URLEncoder.encode(name, "UTF-8"), null);

            if(re.state == APIState.SUCCEEDED) {
                data.data = Base64.decode((String) re.data, Base64.DEFAULT);
            } else {
                throw new APIException(re.reason);
            }
        } catch(Exception e) {
            e.printStackTrace();
            if(snack)
                showReloadSnackbar(e instanceof IOException ? SIAApp.GG_APP.getString(R.string.no_internet_connection) : e.getMessage());
            if(!data.load()) {
                data.throwable = e;
            }

        }

        return data;
    }

    public Mensa getMensa(boolean toast) {
        Mensa m = new Mensa();
        try {

            APIResponse re = doRequest("/cafeteria?token=" + getToken(), null);

            if(re.state == APIState.SUCCEEDED) {
                JSONArray entries = (JSONArray) re.data;
                for(int i = 0; i < entries.length(); i++) {
                    JSONObject obj = entries.getJSONObject(i);
                    Mensa.MensaItem mi = new Mensa.MensaItem();
                    m.add(mi);
                    mi.date = obj.getString("date");
                    mi.id = obj.getString("id");
                    mi.meal = obj.getString("meal");
                    mi.garnish = obj.getString("garnish");
                    mi.dessert = obj.getString("dessert");
                    mi.vegetarian = obj.getString("vegetarian");
                    mi.image = obj.getString("image");

                }
            } else {
                throw new APIException(re.reason);
            }

        } catch (final Exception e) {
            e.printStackTrace();
            if(toast)
                showReloadSnackbar(e instanceof IOException ? SIAApp.GG_APP.getString(R.string.no_internet_connection) : e.getMessage());
            if(!m.load()) {
                m.throwable = e;
            }
        }

        return m;

    }

    public Bitmap getMensaImage(String filename) throws IOException {

        //TODO not implemented yet

        /*HttpsURLConnection con = openConnection("/infoapp/infoapp_provider_new.php?site=mensa_image&sessid=" + session.id + "&filename=" + filename, true);
        con.setRequestMethod("GET");

        if(con.getResponseCode() == 200) {
            return BitmapFactory.decodeStream(con.getInputStream());
        } else {
            throw new IOException();
        }*/

        throw new IOException();
    }

    public Exams getExams(boolean toast) {
        Exams exams = new Exams();
        DateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        try {
            APIResponse re = doRequest("/exams?token=" + getToken(), null);

            if(re.state == APIState.SUCCEEDED) {
                JSONArray entries = (JSONArray) re.data;
                for(int i = 0; i < entries.length(); i++) {
                    JSONObject obj = entries.getJSONObject(i);
                    Exams.ExamItem e = new Exams.ExamItem();
                    exams.add(e);

                    e.date = sdf.parse(obj.getString("date"));
                    e.clazz = obj.getString("class");
                    e.lesson = obj.getString("lesson");
                    e.length = obj.getString("length");
                    e.subject = obj.getString("subject");
                    e.teacher = obj.optString("teacher");

                }
                exams.sort();
            } else {
                throw new APIException(re.reason);
            }

        } catch (final Exception e) {
            e.printStackTrace();
            if(toast)
                showReloadSnackbar(e instanceof IOException ? SIAApp.GG_APP.getString(R.string.no_internet_connection) : e.getMessage());

            if(!exams.load()) {
                exams.throwable = e;
            }
        }

        return exams;

    }

    /**
     * Sends an authentication request
     * @param user
     * @param pass
     * @return 0: ok, 1: invalid user/passwd, 2: no connection, 3: maintenance, 4: everything else
     */
    public int login(String sid, String user, String pass) {
        try {
            JSONObject post = new JSONObject();
            post.put("username", user);
            post.put("passwd", pass);
            post.put("sid", sid);

            APIResponse re = doRequest("/auth", post);

            switch(re.state) {
                case SUCCEEDED:
                    JSONObject data = (JSONObject) re.data;

                    String group = prefs.getString("group", null);
                    Filter.FilterList filters = SIAApp.GG_APP.filters;
                    if (group != null && !group.equals("lehrer")) {
                        filters.including.add(new Filter.IncludingFilter(Filter.FilterType.CLASS, group));
                    } else if (group != null) {
                        filters.including.add(new Filter.IncludingFilter(Filter.FilterType.TEACHER, user));
                    }
                    FilterActivity.saveFilter(SIAApp.GG_APP.filters);

                    if(data.has("username")) {
                        if(!data.getString("username").equals(user) || !data.getString("sid").equals(sid))
                            return 3;
                    }

                    SharedPreferences.Editor edit = prefs.edit();
                    edit.putString("username", user);
                    edit.putString("token", data.getString("token"));
                    edit.apply();

                    APIResponse resp = doRequest("/schoolInfo?token=" + data.getString("token"), null);
                    if(resp.state == APIState.FAILED && resp.reason.equals(API_MAINTENANCE))
                        return 3;
                    else if(resp.state == APIState.FAILED)
                        return 4;

                    School.updateSchool((JSONObject) resp.data);
                    School.saveList();

                    SIAApp.GG_APP.setSchool(sid);
                    FirebaseMessaging.getInstance().subscribeToTopic("sia_sid_" + sid);

                    return 0;
                case FAILED:
                    switch(re.reason) {
                        case API_INVALID_TOKEN:
                            return 1;
                        case API_MAINTENANCE:
                            return 3;
                        default:
                            return 4;
                    }
            }

            return 4;

        } catch (Exception e) {
            if (e instanceof IOException) {
                Log.w("ggvp", "Login failed " + e.getMessage());
                return 2;
            } else {
                e.printStackTrace();
                return 4;
            }
        }
    }

    public String getToken() {
        return prefs.getString("token", null);
    }

    public String getUsername() {
        return prefs.getString("username", isLoggedIn() ? SIAApp.GG_APP.getString(R.string.anonymous) : null);
    }

    public boolean isLoggedIn() {
        return getToken() != null;
    }

    public APIResponse doRequest(String url, JSONObject request) throws IOException {
        HttpURLConnection con = (HttpURLConnection) new URL(BuildConfig.BACKEND_SERVER + url).openConnection();

        con.setRequestProperty("User-Agent", "SchulinfoAPP/" + BuildConfig.VERSION_NAME + " (" +
                BuildConfig.VERSION_CODE + " " + BuildConfig.BUILD_TYPE + " Android " + Build.VERSION.RELEASE + " " + Build.PRODUCT + ")");
        con.setRequestProperty("Accept-Encoding", "gzip");
        con.setConnectTimeout(3000);
        con.setRequestMethod(request == null ? "GET" : "POST");
        con.setInstanceFollowRedirects(false);

        if(request != null) {
            con.setDoOutput(true);
            con.setRequestProperty("Content-Type", "application/json");
            DataOutputStream wr = new DataOutputStream(con.getOutputStream());
            wr.writeBytes(request.toString());
            wr.flush();
            wr.close();
        }

        if(BuildConfig.DEBUG)
            Log.d("ggvp", "connection to " + con.getURL() + " established");

        InputStream in = con.getResponseCode() != 200 ? con.getErrorStream() : con.getInputStream();
        String encoding = con.getHeaderField("Content-Encoding");
        if(encoding != null && encoding.equalsIgnoreCase("gzip")) {
            in = new GZIPInputStream(in);
        }

        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        String response = "";
        String line = "";
        while((line = reader.readLine()) != null)
            response += line;
        JSONObject json = null;
        try {
            json = new JSONObject(response);
            String state = json.getString("state");
            Object data = json.opt("data");
            String reason = json.optString("reason", "");

            Log.d("ggvp", "received state " + state + " " + con.getResponseCode() + " reason: " + reason);

            return new APIResponse(state.equals("succeeded") ? APIState.SUCCEEDED : APIState.FAILED, data, reason);

        } catch(JSONException e) {
            Log.e("ggvp", e.toString());
            e.printStackTrace();
            return new APIResponse(APIState.FAILED);
        }

    }

    public enum APIState {
        SUCCEEDED, FAILED
    }

    public static final String API_TOKEN_EXPIRED = "token expired";
    public static final String API_INVALID_TOKEN = "invalid token";
    public static final String API_NOT_FOUND = "not found";
    public static final String API_METHOD_NOT_ALLOWED = "method not allowed";
    public static final String API_MAINTENANCE = "maintenance";

    public static class APIResponse {
        APIState state;
        Object data;
        String reason;

        public APIResponse(APIState state) {
            this(state, null, "");
        }

        public APIResponse(APIState state, Object data) {
            this(state, data, "");
        }

        public APIResponse(APIState state, Object data, String reason) {
            this.state = state;
            this.data = data;
            this.reason = reason;
        }
    }

}
