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

package de.gebatzens.sia;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Build;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.View;

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
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.zip.GZIPInputStream;

import de.gebatzens.sia.data.Exams;
import de.gebatzens.sia.data.Filter;
import de.gebatzens.sia.data.GGPlan;
import de.gebatzens.sia.data.Mensa;
import de.gebatzens.sia.data.News;

public class GGRemote {

    public static final String PREFS_NAME = "remoteprefs";
    public static final GGImageGetter IMAGE_GETTER = new GGImageGetter();

    SharedPreferences prefs;

    public GGRemote() {
        prefs = GGApp.GG_APP.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

    }

    public void showReloadSnackbar() {
        if(GGApp.GG_APP.activity == null)
            return;

        GGApp.GG_APP.activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Snackbar.make(GGApp.GG_APP.activity.getWindow().getDecorView().findViewById(R.id.coordinator_layout), R.string.no_internet_connection, Snackbar.LENGTH_LONG)
                        .setAction(R.string.again, new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                final View rv = GGApp.GG_APP.activity.getWindow().getDecorView();
                                ((SwipeRefreshLayout) rv.findViewById(R.id.refresh)).setRefreshing(true);
                                GGApp.GG_APP.refreshAsync(new Runnable() {
                                    @Override
                                    public void run() {
                                        ((SwipeRefreshLayout) rv.findViewById(R.id.refresh)).setRefreshing(false);
                                    }
                                }, true, GGApp.GG_APP.getFragmentType());
                            }
                        }).show();
            }
        });

    }

    public void logout() {
        final String token = getToken();

        for(String name : GGApp.GG_APP.fileList())
            if(name.startsWith("schedule"))
                GGApp.GG_APP.deleteFile(name);
        GGApp.GG_APP.deleteFile("news");
        GGApp.GG_APP.deleteFile("mensa");
        GGApp.GG_APP.deleteFile("exams");
        GGApp.GG_APP.stopService(new Intent(GGApp.GG_APP, MQTTService.class));
        GGApp.GG_APP.filters.clear();
        GGApp.GG_APP.filters.mainFilter = new Filter();

        prefs.edit().clear().apply();

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

    public GGPlan.GGPlans getPlans(boolean toast) {

        GGPlan.GGPlans plans = new GGPlan.GGPlans();

        try {
            Log.i("ggvp", "getPlans " + getToken());
            APIResponse re = doRequest("/subst?token=" + getToken(), null);

            if(re.state == APIState.SUCCEEDED) {
                Iterator<String> days = ((JSONObject) re.data).keys();
                while (days.hasNext()) {
                    String date = days.next();
                    JSONObject obj = ((JSONObject) re.data).getJSONObject(date);
                    GGPlan plan = new GGPlan();
                    plan.date = GGPlan.parseDate(date);
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
            } else if(re.state == APIState.INVALID_AUTH) {
                throw new VPLoginException();
            } else {
                throw new Exception("Received state " + re.state);
            }

        } catch(Exception e) {
            if(e instanceof IOException || e instanceof VPLoginException)
                Log.w("ggvp", "Failed to get plans " + e.getMessage());
            else
                e.printStackTrace();
            plans.throwable = e;

        }

        Collections.sort(plans, new Comparator<GGPlan>() {
            @Override
            public int compare(GGPlan lhs, GGPlan rhs) {
                return lhs.date.compareTo(rhs.date);
            }
        });

        if(plans.throwable != null) {
            if (plans.load()) {
                final Throwable t = plans.throwable;
                plans.throwable = null;
                if(toast)
                    showReloadSnackbar();
            }
        } else {
            SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm");
            plans.loadDate = GGApp.GG_APP.getResources().getString(R.string.as_of) + ": " + sdf.format(new Date());

        }

        return plans;
    }

    private void getPlan(JSONArray array, GGPlan p) throws Exception {
        for(int i = 0; i < array.length(); i++) {
            GGPlan.Entry e = new GGPlan.Entry();
            e.date = p.date;
            p.entries.add(e);
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
            } else if(re.state == APIState.INVALID_AUTH)
                throw new VPLoginException();
            else
                throw new Exception("Received state " + re.state);
        } catch (final Exception e) {
            e.printStackTrace();
            if(toast)
                showReloadSnackbar();
            if(!n.load()) {
                n.throwable = e;
            }
        }

        return n;

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
            } else if(re.state == APIState.INVALID_AUTH)
                throw new VPLoginException();
            else
                throw new Exception("Received state " + re.state);

        } catch (final Exception e) {
            e.printStackTrace();
            if(toast)
                showReloadSnackbar();
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
            } else if(re.state == APIState.INVALID_AUTH)
                throw new VPLoginException();
            else
                throw new Exception("Received state " + re.state);

        } catch (final Exception e) {
            e.printStackTrace();
            if(toast)
                showReloadSnackbar();

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
     * @return 0: ok, 1: invalid user/passwd, 2: no connection, 3: everything else
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

                    GGApp.GG_APP.startService(new Intent(GGApp.GG_APP, MQTTService.class));

                    String group = prefs.getString("group", null);
                    Filter.FilterList filters = GGApp.GG_APP.filters;
                    if (group != null && !group.equals("lehrer")) {
                        filters.mainFilter.type = Filter.FilterType.CLASS;
                        filters.mainFilter.filter = group;
                    } else if (group != null) {
                        filters.mainFilter.type = Filter.FilterType.TEACHER;
                        filters.mainFilter.filter = user;
                    }
                    FilterActivity.saveFilter(GGApp.GG_APP.filters);

                    if(data.has("username")) {
                        if(!data.getString("username").equals(user) || !data.getString("sid").equals(sid))
                            return 3;
                    }

                    SharedPreferences.Editor edit = prefs.edit();
                    edit.putString("username", user);
                    edit.putString("token", data.getString("token"));
                    edit.apply();

                    APIResponse resp = doRequest("/schoolInfo?token=" + data.getString("token"), null);
                    if(resp.state != APIState.SUCCEEDED)
                        return 3;

                    School.updateSchool((JSONObject) resp.data);
                    School.saveList();

                    GGApp.GG_APP.setSchool(sid);

                    return 0;
                case INVALID_AUTH:
                    return 1;
                default:
                    return 3;
            }

        } catch (Exception e) {
            if (e instanceof IOException) {
                Log.w("ggvp", "Login failed " + e.getMessage());
                return 2;
            } else {
                e.printStackTrace();
                return 3;
            }
        }
    }

    public String getToken() {
        return prefs.getString("token", null);
    }

    public String getUsername() {
        return prefs.getString("username", isLoggedIn() ? GGApp.GG_APP.getString(R.string.anonymous) : null);
    }

    public String getFirstName() {
        return prefs.getString("firstname", null);
    }

    public String getLastName() {
        return prefs.getString("lastname", null);
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
            if(BuildConfig.DEBUG)
                Log.d("ggvp", "received state " + state + " " + con.getResponseCode());

            Object data = json.opt("data");
            switch(state) {
                case "not found":
                    return new APIResponse(APIState.NOT_FOUND);
                case "invalid json":
                    return new APIResponse(APIState.INVALID_JSON);
                case "invalid auth":
                    return new APIResponse(APIState.INVALID_AUTH);
                case "method not allowed":
                    return new APIResponse(APIState.METHOD_NOT_ALLOWED);
                case "missing parameter":
                    return new APIResponse(APIState.MISSING_PARAMETER);
                case "succeeded":
                    return new APIResponse(APIState.SUCCEEDED, data);
                case "error":
                default:
                    return new APIResponse(APIState.ERROR);
            }

        } catch(JSONException e) {
            e.printStackTrace();
            return new APIResponse(APIState.ERROR);
        }

    }

    public enum APIState {
        SUCCEEDED, ERROR, NOT_FOUND, METHOD_NOT_ALLOWED, INVALID_JSON, INVALID_AUTH, MISSING_PARAMETER
    }

    public static class APIResponse {
        APIState state;
        Object data;

        public APIResponse(APIState state) {
            this(state, null);
        }

        public APIResponse(APIState state, Object data) {
            this.state = state;
            this.data = data;
        }
    }

}
