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

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.provider.Settings;
import android.util.JsonReader;
import android.util.Log;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLEncoder;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import de.gebatzens.ggvertretungsplan.data.Exams;
import de.gebatzens.ggvertretungsplan.data.Filter;
import de.gebatzens.ggvertretungsplan.data.GGPlan;
import de.gebatzens.ggvertretungsplan.data.Mensa;
import de.gebatzens.ggvertretungsplan.data.News;

public class GGRemote {

    public static final String SERVER = "https://gymnasium-glinde.logoip.de";
    public static final String PREFS_NAME = "remoteprefs";
    public static final GGImageGetter IMAGE_GETTER = new GGImageGetter();

    SharedPreferences prefs;
    Session session;

    public GGRemote() {

        prefs = GGApp.GG_APP.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        new Thread() {
            @Override
            public void run() {
                try {
                    startNewSession(prefs.getString("token", null));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }.start();

    }

    private void startNewSession(String token) throws IOException {
        if(token == null || token.isEmpty())
            return;

        HttpsURLConnection con = openConnection("/infoapp/token2.php?token=" + token, false);

        if(con.getResponseCode() == 401) {
            Log.w("ggvp", "startSession: Received " + con.getResponseCode() + ": logging out");
            logout(true, true);
            session = null;
            return;
        }

        JsonReader reader = new JsonReader(new InputStreamReader(con.getInputStream()));
        reader.beginObject();

        while(reader.hasNext()) {
            String name = reader.nextName();
            if(name.equals("state") && !reader.nextString().equals("succeeded")) {
                //Token is invalid
                Log.w("ggvp", "Token is invalid");
                session = null;
            } else if(name.equals("sessid")) {
                session = new Session();
                session.id = reader.nextString();
                Log.i("ggvp", "New session " + session.id);
            }


        }

        reader.close();

    }

    public void logout(boolean logout_local_only, final boolean delete_token) {
        GGApp.GG_APP.deleteFile("stoday");
        GGApp.GG_APP.deleteFile("stomorrow");
        GGApp.GG_APP.deleteFile("news");
        GGApp.GG_APP.deleteFile("mensa");
        GGApp.GG_APP.stopService(new Intent(GGApp.GG_APP, MQTTService.class));
        GGApp.GG_APP.filters.clear();
        GGApp.GG_APP.filters.mainFilter = new Filter();

        prefs.edit().clear().apply();
        /*if(!logout_local_only) {
            new AsyncTask<String, String, String>() {
                @Override
                public String doInBackground(String... s) {

                    if ((s[0] != null) && !s[0].isEmpty()) {
                        try {
                            String connect_string;
                            if (delete_token) {
                                connect_string = SERVER + "/infoapp/logout.php?deltoken=true&sessid=" + s[0];
                            } else {
                                connect_string = SERVER + "/infoapp/logout.php?sessid=" + s[0];
                            }
                            HttpsURLConnection con = (HttpsURLConnection) new URL(connect_string).openConnection();
                            con.setRequestMethod("GET");
                            con.setSSLSocketFactory(sslSocketFactory);
                            BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream()));
                            StringBuilder sb = new StringBuilder();
                            String line;
                            while ((line = br.readLine()) != null) {
                                sb.append(line);
                            }
                            Log.e("Response", sb.toString());
                            if (sb.toString().contains("<state>true</state>")) {
                                GGApp.GG_APP.activity.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(GGApp.GG_APP.activity.getApplicationContext(), GGApp.GG_APP.getResources().getString(R.string.logout_successfull), Toast.LENGTH_LONG).show();
                                    }
                                });
                            } else {
                                GGApp.GG_APP.activity.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(GGApp.GG_APP.activity.getApplicationContext(), GGApp.GG_APP.getResources().getString(R.string.logout_error), Toast.LENGTH_LONG).show();
                                    }
                                });
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        GGApp.GG_APP.activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                GGApp.GG_APP.activity.mContent.updateFragment();
                            }
                        });

                    }
                    return null;
                }
            }.execute(session.id);
        }*/
        session = null;
    }

    public GGPlan.GGPlans getPlans(boolean toast) {

        GGPlan.GGPlans plans = new GGPlan.GGPlans();

        try {
            if (session == null) {
                startNewSession(prefs.getString("token", null));

                // token expired
                if (session == null)
                    throw new VPLoginException();
            }
            Log.i("ggvp", "getPlans " + session.id);
            HttpsURLConnection con = openConnection("/infoapp/provider2.php?site=schedule&sessid=" + session.id, true);
            con.setRequestMethod("GET");

            /*if(con.getResponseCode() == 401) {
                logout(true, true);
                throw new VPLoginException();
            } else */
            if(con.getResponseCode() == 419) {
                startNewSession(prefs.getString("token", null));
                if(session == null) {
                    //Should happen when token expires
                    throw new VPLoginException();
                } else {
                    return getPlans(toast);
                }
            }

            if(con.getResponseCode() != 200)
                throw new Exception("response code " + con.getResponseCode());

            JsonReader reader = new JsonReader(new InputStreamReader(con.getInputStream()));
            reader.beginObject();

            while(reader.hasNext()) {
                String date = reader.nextName();
                GGPlan plan = new GGPlan();
                plan.date = GGPlan.parseDate(date);
                plans.add(plan);
                reader.beginObject();
                while(reader.hasNext()) {
                    String name = reader.nextName();
                    if(name.equals("entries"))
                        getPlan(reader, plan);
                    else if(name.equals("messages")) {
                        reader.beginArray();
                        while(reader.hasNext()) {
                            plan.special.add("&#8226;  " + reader.nextString());
                        }
                        reader.endArray();
                    } else
                        reader.skipValue();
                }
                reader.endObject();


            }

        } catch(Exception e) {
            e.printStackTrace();
            plans.throwable = e;

        }

        if(plans.throwable != null) {
            if (plans.load()) {
                final Throwable t = plans.throwable;
                plans.throwable = null;
                if(toast)
                    GGApp.GG_APP.activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            GGApp.GG_APP.showToast(t instanceof IOException ? GGApp.GG_APP.getResources().getString(R.string.no_internet_connection) :
                                    t instanceof VPLoginException ? GGApp.GG_APP.getString(R.string.not_logged_in) : GGApp.GG_APP.getResources().getString(R.string.unknown_error));
                        }
                    });
            }
        } else {
            plans.save();
            SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm");
            plans.loadDate = GGApp.GG_APP.getResources().getString(R.string.as_of) + ": " + sdf.format(new Date());
        }

        return plans;
    }

    private void getPlan(JsonReader reader, GGPlan p) throws Exception {

        reader.beginArray();

        while(reader.hasNext()) {
            reader.beginObject();
            GGPlan.Entry e = new GGPlan.Entry();
            e.date = p.date;
            p.entries.add(e);
            while(reader.hasNext()) {
                String name = reader.nextName();
                if(name.equals("class"))
                    e.clazz = reader.nextString();
                else if(name.equals("lesson"))
                    e.lesson = reader.nextString();
                else if(name.equals("subsitutor"))
                    e.subst = reader.nextString();
                else if(name.equals("subject"))
                    e.subject = reader.nextString();
                else if(name.equals("subst_subject"))
                    e.repsub = reader.nextString();
                else if(name.equals("type"))
                    e.type = reader.nextString();
                else if(name.equals("comment"))
                    e.comment = reader.nextString();
                else if(name.equals("room"))
                    e.room = reader.nextString();
                else
                    reader.skipValue();

            }
            e.unify();

            reader.endObject();
        }

        reader.endArray();
    }

    public News getNews() {
        News n = new News();
        try {
            if (session == null) {
                startNewSession(prefs.getString("token", null));
                if (session == null)
                    throw new VPLoginException();
            }

            DateFormat parser = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");

            HttpsURLConnection con = openConnection("/infoapp/provider2.php?site=news&sessid=" + session.id, true);
            con.setRequestMethod("GET");

            /*if(con.getResponseCode() == 401) {
                logout(true, true);
                throw new VPLoginException();
            } else */if(con.getResponseCode() == 419) {
                startNewSession(prefs.getString("token", null));
                if(session == null) {
                    throw new VPLoginException();
                } else {
                    return getNews();
                }
            } else if (con.getResponseCode() == 200) {
                JsonReader reader = new JsonReader(new InputStreamReader(con.getInputStream()));
                reader.beginArray();
                while(reader.hasNext()) {
                    reader.beginObject();
                    News.Entry e = new News.Entry();
                    n.add(e);
                    while(reader.hasNext()) {
                        String name = reader.nextName();
                        if(name.equals("id"))
                            e.id = reader.nextString();
                        else if(name.equals("date"))
                            e.date = parser.parse(reader.nextString());
                        else if(name.equals("source"))
                            e.source = reader.nextString();
                        else if(name.equals("topic"))
                            e.topic = reader.nextString();
                        else if(name.equals("title"))
                            e.title = reader.nextString();
                        else if(name.equals("text"))
                            e.text = reader.nextString();
                        else
                            reader.skipValue();
                    }
                    reader.endObject();
                }

                n.save();
            }
        } catch (Exception e) {
            e.printStackTrace();
            if(!n.load()) {
                n.throwable = e;
                return n;
            }

        } finally {
            return n;
        }
    }

    public Mensa getMensa() {
        Mensa m = new Mensa();
        try {
            if (session == null) {
                startNewSession(prefs.getString("token", null));
                if (session == null)
                    throw new VPLoginException();
            }

            HttpsURLConnection con = openConnection("/infoapp/provider2.php?site=mensa&sessid=" + session.id, true);
            con.setRequestMethod("GET");

            /*if(con.getResponseCode() == 401) {
                logout(true, true);
                throw new VPLoginException();
            } else */if(con.getResponseCode() == 419) {
                startNewSession(prefs.getString("token", null));
                if(session == null) {
                    throw new VPLoginException();
                } else {
                    return getMensa();
                }
            } else if (con.getResponseCode() == 200) {
                JsonReader reader = new JsonReader(new InputStreamReader(con.getInputStream()));
                reader.beginArray();
                while(reader.hasNext()) {
                    Mensa.MensaItem i = new Mensa.MensaItem();
                    m.add(i);
                    reader.beginObject();
                    while(reader.hasNext()) {
                        String name = reader.nextName();
                        if(name.equals("id"))
                            i.id = reader.nextString();
                        else if(name.equals("date"))
                            i.date = reader.nextString();
                        else if(name.equals("meal"))
                            i.meal = reader.nextString();
                        else if(name.equals("garnish"))
                            i.garnish = reader.nextString();
                        else if(name.equals("veg"))
                            i.vegi = reader.nextString();
                        else if(name.equals("image"))
                            i.image = reader.nextString();
                        else
                            reader.skipValue();
                    }
                    reader.endObject();
                }

                m.save();
            }
        } catch (Exception e) {
            e.printStackTrace();
            if(!m.load()) {
                m.throwable = e;
                return m;
            }

        } finally {
            return m;
        }
    }

    public Bitmap getMensaImage(String filename) throws IOException {

        HttpsURLConnection con = openConnection("/infoapp/infoapp_provider_new.php?site=mensa_image&sessid=" + session.id + "&filename=" + filename, true);
        con.setRequestMethod("GET");

        if(con.getResponseCode() == 200) {
            return BitmapFactory.decodeStream(con.getInputStream());
        } else {
            throw new IOException();
        }
    }

    public Exams getExams() {
        Exams exams = new Exams();
        try {
            if (session == null) {
                startNewSession(prefs.getString("token", null));
                if (session == null)
                    throw new VPLoginException();
            }

            HttpsURLConnection con = openConnection("/infoapp/provider2.php?site=exams&sessid=" + session.id, true);
            con.setRequestMethod("GET");

            /*if(con.getResponseCode() == 401) {
                logout(true, true);
                throw new VPLoginException();
            } else */if(con.getResponseCode() == 419) {
                startNewSession(prefs.getString("token", null));
                if(session == null) {
                    throw new VPLoginException();
                } else {
                    return getExams();
                }
            } else if (con.getResponseCode() == 200) {
                JsonReader reader = new JsonReader(new InputStreamReader(con.getInputStream()));
                reader.beginArray();

                DateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

                while (reader.hasNext()) {
                    reader.beginObject();
                    Exams.ExamItem e = new Exams.ExamItem();
                    exams.add(e);
                    while (reader.hasNext()) {
                        String name = reader.nextName();
                        if (name.equals("id"))
                            e.id = reader.nextString();
                        else if(name.equals("date"))
                            e.date = sdf.parse(reader.nextString());
                        else if (name.equals("class"))
                            e.clazz = reader.nextString();
                        else if (name.equals("lesson"))
                            e.lesson = reader.nextString();
                        else if (name.equals("subject"))
                            e.subject = reader.nextString();
                        else if (name.equals("length"))
                            e.length = reader.nextString();
                        else if (name.equals("teacher"))
                            e.teacher = reader.nextString();
                        else
                            reader.skipValue();
                    }
                    reader.endObject();
                }
            }
            exams.save();
        } catch (Exception e) {
            e.printStackTrace();
            if(!exams.load()) {
                exams.throwable = e;
                return exams;
            }
        }
        return exams;
    }

    public int login(String user, String pass) {
        try {

            HttpsURLConnection con = openConnection("/infoapp/auth2.php", false);
            con.setRequestMethod("POST");

            con.setDoOutput(true);
            DataOutputStream wr = new DataOutputStream(con.getOutputStream());
            String s = "sid=" + URLEncoder.encode(GGApp.GG_APP.school.sid, "utf-8") + "&uid=" +
                    URLEncoder.encode(Settings.Secure.getString(GGApp.GG_APP.getContentResolver(), Settings.Secure.ANDROID_ID), "utf-8");
            if(user != null && pass != null)
                s += "&username=" + URLEncoder.encode(user, "utf-8") + "&password=" + URLEncoder.encode(pass, "utf-8");
            wr.writeBytes(s);
            wr.flush();
            wr.close();

            if(con.getResponseCode() == 200) {
                JsonReader reader = new JsonReader(new InputStreamReader(con.getInputStream()));
                reader.beginObject();
                while(reader.hasNext()) {
                    String name = reader.nextName();
                    if(name.equals("state")) {
                        if(!reader.nextString().equals("succeeded"))
                            return 1;
                    } else if(name.equals("session")) {
                        session = new Session();
                        session.id = reader.nextString();
                    } else if(name.equals("username") || name.equals("token") || name.equals("firstname") || name.equals("lastname") || name.equals("group")) {
                        prefs.edit().putString(name, reader.nextString()).commit();
                    } else
                        reader.skipValue();
                }

                GGApp.GG_APP.startService(new Intent(GGApp.GG_APP, MQTTService.class));

                String group = prefs.getString("group", null);
                Filter.FilterList filters = GGApp.GG_APP.filters;
                if(group != null && !group.equals("lehrer")) {
                    filters.mainFilter.type = Filter.FilterType.CLASS;
                    filters.mainFilter.filter = group;
                } else if (group != null) {
                    filters.mainFilter.type = Filter.FilterType.TEACHER;
                    filters.mainFilter.filter = user;
                }
                FilterActivity.saveFilter(GGApp.GG_APP.filters);

                /*GGApp.GG_APP.activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                GGApp.GG_APP.activity.mContent.setFragmentLoading();
            }
        });
        GGApp.GG_APP.refreshAsync(null, true, GGApp.GG_APP.getFragmentType());*/

            } else if(con.getResponseCode() == 401)
                return 1;
            else
                return 3;


        } catch (Exception e) {
            e.printStackTrace();
            if (e instanceof IOException)
                return 2;
            else
                return 3;


        }
        return 0;
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


    private static TrustManager[] ggTrustMgr = new TrustManager[]{ new X509TrustManager() {

        String pub_key = "fa095201ee4f03c32022f11b0c7352eba684d48c09220be0d26fa7c81c26d" +
                "120cbf0fe6c3bdf669de6dd04046c3146641e4131f2113e18b59c01673fe222323" +
                "8dcbd319e58939637affab79367ea3305b5f8ad6b723c6b1cadd5586cc108592d6" +
                "d5fcd7c927909c42c5be56ac54152efaa18557333fc84bfb2d18a182fc66604139" +
                "7873b991e8e6d37efb182c9afa5fcc841025d4d77e76ed9d49de89a0c20fc6eaa8" +
                "09c52c789f15fe6807ab1c61ac5908b427d0ca9012ef86fe18eaf5fef684954c2b" +
                "2e36e68d7b5f2a76500832df8a133e14a4b424bbd818da58f739da7a578e66dfe9" +
                "4ba16506e7c88c66ff25f7f90ac8b2c3f9f347d5b54351dfd971f29";


        @Override
        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
            return null;
        }

        @Override
        public void checkClientTrusted(X509Certificate[] certs, String authType) {

        }

        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            try {
                InputStream inStream = GGApp.GG_APP.getAssets().open("ca.crt");
                CertificateFactory cf = CertificateFactory.getInstance("X.509");
                X509Certificate ca = (X509Certificate) cf.generateCertificate(inStream);
                inStream.close();

                chain[0].verify(ca.getPublicKey());


            } catch (Exception ex) {
                Log.w("ggca", "Falling back to old public key");
                //Receive certificate information from the obtained certificate
                String recveived_pub_key = chain[0].getPublicKey().toString();

                //Extract the public key from the certificate information
                String obtained_key = recveived_pub_key.split("\\{")[1].split("\\}")[0].split(",")[0].split("=")[1];
                if(!pub_key.equals(obtained_key)) {
                    //If the public key is not correct throw an exception, to prevent connecting
                    // to this evil server
                    Log.e("ggca", "Failed to verify server identity");
                    throw new CertificateException();
                } else
                    Log.w("ggca", "Server is using old certificate");

            }
        }
    }
    };
    public static SSLContext sc;
    public static SSLSocketFactory sslSocketFactory;
    static {
        try {
            sc = SSLContext.getInstance("TLS");
            sc.init(null, ggTrustMgr, new java.security.SecureRandom());
            sslSocketFactory = sc.getSocketFactory();

        } catch(Exception e) {
            throw new RuntimeException(e);
        }
    }

    public HttpsURLConnection openConnection(String url, boolean checkSession) throws IOException {
        if(checkSession && session != null && session.isExpired())
            startNewSession(prefs.getString("token", null));
        HttpsURLConnection con = (HttpsURLConnection) new URL(SERVER + url).openConnection();
        con.setSSLSocketFactory(sslSocketFactory);
        con.setRequestProperty("User-Agent", "SchulinfoAPP/" + BuildConfig.VERSION_NAME + " (" +
                BuildConfig.VERSION_CODE + " " + BuildConfig.BUILD_TYPE + " Android " + Build.VERSION.RELEASE + " " + Build.PRODUCT + ")");
        con.setConnectTimeout(3000);

        Log.w("ggvp", "connection to " + con.getURL().getHost() + " established " + url);

        return con;
    }

    static class Session {
        String id;
        Date start;

        public Session() {
            start = new Date();
        }

        public boolean isExpired() {
            return (System.currentTimeMillis() - start.getTime()) > (1000 * 3600 * 20);
        }

        @Override
        public String toString() {
            return start + " " + id;
        }
    }

}
