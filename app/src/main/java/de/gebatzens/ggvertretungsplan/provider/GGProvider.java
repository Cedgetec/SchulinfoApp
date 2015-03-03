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

package de.gebatzens.ggvertretungsplan.provider;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;
import android.util.Xml;
import android.widget.Toast;

import org.xmlpull.v1.XmlPullParser;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.URL;
import java.net.URLEncoder;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import de.gebatzens.ggvertretungsplan.FilterActivity;
import de.gebatzens.ggvertretungsplan.GGApp;
import de.gebatzens.ggvertretungsplan.R;
import de.gebatzens.ggvertretungsplan.VPLoginException;
import de.gebatzens.ggvertretungsplan.data.Exams;
import de.gebatzens.ggvertretungsplan.data.Filter;
import de.gebatzens.ggvertretungsplan.data.GGPlan;
import de.gebatzens.ggvertretungsplan.data.Mensa;
import de.gebatzens.ggvertretungsplan.data.News;

public class GGProvider extends VPProvider {

    public static final String BASE_URL = "https://gymnasium-glinde.logoip.de/";

    GGApp ggapp;
    String sessId;

    public GGProvider(GGApp gg, String id) {
        super(gg, id);

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

        HttpsURLConnection con = openConnection(BASE_URL + "infoapp/token.php?token=" + token);

        if(con.getResponseCode() == 401) {
            logout(true, true);
            sessId = null;
            return;
        }

        Scanner scan = new Scanner(new BufferedInputStream(con.getInputStream()));
        String resp = "";
        while(scan.hasNextLine())
            resp += scan.nextLine();
        scan.close();

        Pattern p = Pattern.compile("<sessid>(.*?)</sessid>");
        Matcher m = p.matcher(resp);
        if(m.find()) {
            sessId = m.group(1);
            Log.w("ggvp", "new session created " + sessId);
            //prefs.edit().putString("sessid", sessId).apply();
        } else {
            sessId = null;
            Log.w("ggvp", "invalid response (token invalid?)");
            //Token invalid
        }

    }

    @Override
    public void logout(Boolean logout_local_only, final Boolean delete_token) {
        GGApp.GG_APP.deleteFile("gguserinfo");
        GGApp.GG_APP.deleteFile("ggvptoday");
        GGApp.GG_APP.deleteFile("ggvptomorrow");
        GGApp.GG_APP.deleteFile("ggnews");
        GGApp.GG_APP.deleteFile("ggmensa");

        prefs.edit().clear().commit();
        if(!logout_local_only) {
            new AsyncTask<String, String, String>() {
                @Override
                public String doInBackground(String... s) {

                    if ((s[0] != null) && !s[0].isEmpty()) {
                        try {
                            String connect_string;
                            if (delete_token) {
                                connect_string = BASE_URL + "infoapp/logout.php?deltoken=true&sessid=" + s[0];
                            } else {
                                connect_string = BASE_URL + "infoapp/logout.php?sessid=" + s[0];
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
            }.execute(sessId);
        }
        sessId = null;
    }

    @Override
    public GGPlan.GGPlans getPlans(boolean toast) {
        Log.w("ggvp", "Get GG Plans " + sessId);
        GGPlan.GGPlans plans = new GGPlan.GGPlans();
        plans.tomorrow = new GGPlan();
        plans.today = new GGPlan();
        plans.today.date = new Date();
        plans.tomorrow.date = new Date();

        try {
            if (sessId == null || sessId.isEmpty()) {
                startNewSession(prefs.getString("token", null));
                if (sessId == null || sessId.isEmpty())
                    throw new VPLoginException();
            }

            HttpsURLConnection con = openConnection(BASE_URL + "infoapp/infoapp_provider_new.php?site=substitutionplan&sessid=" + sessId);
            con.setRequestMethod("GET");

            if(con.getResponseCode() == 401) {
                logout(true, true);
                throw new VPLoginException();
            }

            XmlPullParser parser = Xml.newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setInput(new BufferedReader(new InputStreamReader(con.getInputStream())));
            parser.nextTag();
            parser.require(XmlPullParser.START_TAG, null, "substitutionplan");

            while(parser.next() != XmlPullParser.END_TAG) {
                if(parser.getEventType() != XmlPullParser.START_TAG)
                    continue;

                String name = parser.getName();
                if(name.equals("metadata")) {
                    parser.require(XmlPullParser.START_TAG, null, "metadata");
                    while(parser.next() != XmlPullParser.END_TAG) {
                        if(parser.getEventType() != XmlPullParser.START_TAG)
                            continue;

                        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
                        String name2 = parser.getName();
                        if(name2.equals("date-today"))
                            plans.today.date = format.parse(parser.nextText());
                        else if(name2.equals("date-tomorrow"))
                            plans.tomorrow.date = format.parse(parser.nextText());

                    }
                } else if(name.equals("list-today")) {
                    getPlan(parser, plans.today);
                } else if(name.equals("list-tomorrow")) {
                    getPlan(parser, plans.tomorrow);
                } else if(name.equals("important-today")) {
                    while(parser.next() != XmlPullParser.END_TAG) {
                        if(parser.getEventType() != XmlPullParser.START_TAG)
                            continue;

                        if(parser.getName().equals("item"))
                            plans.today.special.add("&#8226;  " + parser.nextText());
                    }
                } else if(name.equals("important-tomorrow")) {
                    while(parser.next() != XmlPullParser.END_TAG) {
                        if(parser.getEventType() != XmlPullParser.START_TAG)
                            continue;

                        if(parser.getName().equals("item"))
                            plans.tomorrow.special.add("&#8226;  " + parser.nextText());
                    }
                }
            }

        } catch(Exception e) {
            e.printStackTrace();
            plans.throwable = e;

        }

        if(plans.throwable != null) {
            if (plans.load("ggvp")) {
                final Throwable t = plans.throwable;
                String s = GGApp.GG_APP.getResources().getString(R.string.no_internet_connection)+"\n" + plans.today.loadDate;
                plans.today.loadDate = s;
                plans.tomorrow.loadDate = s;
                plans.throwable = null;
                if(toast)
                    GGApp.GG_APP.activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            GGApp.GG_APP.showToast(t instanceof IOException ? GGApp.GG_APP.getResources().getString(R.string.no_internet_connection) :
                                    t instanceof VPLoginException ? GGApp.GG_APP.getString(R.string.youre_not_logged_in) : GGApp.GG_APP.getResources().getString(R.string.unknown_error));
                        }
                    });
            }
        } else {
            plans.save("ggvp");
        }

        return plans;
    }

    public int getColorArray() {
        return R.array.orangeColors;
    }

    private void getPlan(XmlPullParser parser, GGPlan p) throws Exception {

        while(parser.next() != XmlPullParser.END_TAG) {
            if(parser.getEventType() != XmlPullParser.START_TAG)
                continue;

            if(parser.getName().equals("item"))  {
                GGPlan.Entry e = new GGPlan.Entry();
                p.entries.add(e);
                while(parser.next() != XmlPullParser.END_TAG) {
                    if(parser.getEventType() != XmlPullParser.START_TAG)
                        continue;

                    String name = parser.getName();
                    if(name.equals("class")) {
                        e.clazz = parser.nextText().trim();
                    } else if(name.equals("hour")) {
                        e.hour = parser.nextText().trim();
                    } else if(name.equals("substitutor")) {
                        e.subst = parser.nextText().trim();
                    } else if(name.equals("subject")) {
                        e.subject = parser.nextText().trim();
                    } else if(name.equals("comment")) {
                        e.comment = parser.nextText().trim();
                    } else if(name.equals("date")) {
                        parser.nextText();
                    } else if(name.equals("room")) {
                        e.room = parser.nextText().trim();
                    }
                }
                e.unify();

            }
        }

        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm");
        p.loadDate = GGApp.GG_APP.getResources().getString(R.string.as_of) + ": " + sdf.format(new Date());
    }

    public News getNews() {
        News n = new News();
        try {
            if (sessId == null || sessId.isEmpty()) {
                startNewSession(prefs.getString("token", null));
                if (sessId == null || sessId.isEmpty())
                    throw new VPLoginException();
            }

            HttpsURLConnection con = openConnection(BASE_URL + "infoapp/infoapp_provider_new.php?site=news&sessid="+sessId);
            con.setRequestMethod("GET");

            /*
             * 0 - ID
             * 1 - Date
             * 2 - Topic
             * 3 - Source
             * 4 - Title
             * 5 - Text
             */

            if(con.getResponseCode() == 401) {
                logout(true, true);
                throw new VPLoginException();
            }

            if (con.getResponseCode() == 200) {
                XmlPullParser parser = Xml.newPullParser();
                parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
                parser.setInput(new BufferedReader(new InputStreamReader(con.getInputStream())));
                parser.nextTag();
                parser.require(XmlPullParser.START_TAG, null, "news");

                while (parser.next() != XmlPullParser.END_TAG) {
                    if (parser.getEventType() != XmlPullParser.START_TAG)
                        continue;

                    String name = parser.getName();
                    if (name.equals("item")) {

                        String[] s = new String[6];
                        n.add(s);

                        while (parser.next() != XmlPullParser.END_TAG) {
                            if (parser.getEventType() != XmlPullParser.START_TAG)
                                continue;

                            if (parser.getName().equals("id"))
                                s[0] = parser.nextText();

                            else if (parser.getName().equals("date"))
                                s[1] = parser.nextText();

                            else if (parser.getName().equals("topic"))
                                s[2] = parser.nextText();

                            else if (parser.getName().equals("source"))
                                s[3] = parser.nextText();

                            else if (parser.getName().equals("title"))
                                s[4] = parser.nextText();

                            else if (parser.getName().equals("text"))
                                s[5] = parser.nextText();
                        }
                    }
                }
                n.save("ggnews");
            }
        } catch (Exception e) {
            e.printStackTrace();
            if(!n.load("ggnews")) {
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
            if (sessId == null || sessId.isEmpty()) {
                startNewSession(prefs.getString("token", null));
                if (sessId == null || sessId.isEmpty())
                    throw new VPLoginException();
            }

            HttpsURLConnection con = openConnection(BASE_URL + "infoapp/infoapp_provider_new.php?site=mensa&sessid="+sessId);
            con.setRequestMethod("GET");

            /*
             * 0 - ID
             * 1 - Date
             * 2 - Meal
             * 3 - Garnish
             * 4 - Vegi
             */

            if(con.getResponseCode() == 401) {
                logout(true, true);
                throw new VPLoginException();
            }

            if (con.getResponseCode() == 200) {
                XmlPullParser parser = Xml.newPullParser();
                parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
                parser.setInput(new BufferedReader(new InputStreamReader(con.getInputStream())));
                parser.nextTag();
                parser.require(XmlPullParser.START_TAG, null, "mensa");

                while (parser.next() != XmlPullParser.END_TAG) {
                    if (parser.getEventType() != XmlPullParser.START_TAG)
                        continue;

                    String name = parser.getName();
                    if (name.equals("item")) {

                        String[] s = new String[6];
                        m.add(s);

                        while (parser.next() != XmlPullParser.END_TAG) {
                            if (parser.getEventType() != XmlPullParser.START_TAG)
                                continue;

                            if (parser.getName().equals("id"))
                                s[0] = parser.nextText();

                            else if (parser.getName().equals("date"))
                                s[1] = parser.nextText();

                            else if (parser.getName().equals("meal"))
                                s[2] = parser.nextText();

                            else if (parser.getName().equals("garnish"))
                                s[3] = parser.nextText();

                            else if (parser.getName().equals("vegi"))
                                s[4] = parser.nextText();

                            else if (parser.getName().equals("image"))
                                s[5] = parser.nextText();
                        }
                    }
                }
                m.save("ggmensa");
            }
        } catch (Exception e) {
            e.printStackTrace();
            if(!m.load("ggmensa")) {
                m.throwable = e;
                return m;
            }

        } finally {
            return m;
        }
    }

    public Bitmap getMensaImage(String filename) throws IOException {

        HttpsURLConnection con = openConnection("https://gymnasium-glinde.logoip.de/infoapp/infoapp_provider_new.php?site=mensa_image&sessid=" + sessId + "&filename=" + filename);
        con.setRequestMethod("GET");

        if(con.getResponseCode() == 200) {
            return BitmapFactory.decodeStream(con.getInputStream());
        } else {
            throw new IOException();
        }
    }

    @Override
    public Exams getExams() {
        Log.w("ggvp", "get GG Exams " + sessId);
        Exams exams = new Exams();
        try {
            if (sessId == null || sessId.isEmpty()) {
                startNewSession(prefs.getString("token", null));
                if (sessId == null || sessId.isEmpty())
                    throw new VPLoginException();
            }

            HttpsURLConnection con = openConnection("https://gymnasium-glinde.logoip.de/infoapp/infoapp_provider_new.php?site=examplan&sessid=" + sessId);
            con.setRequestMethod("GET");

            if (con.getResponseCode() == 200) {
                XmlPullParser parser = Xml.newPullParser();
                parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
                parser.setInput(new BufferedReader(new InputStreamReader(con.getInputStream())));
                parser.nextTag();
                parser.require(XmlPullParser.START_TAG, null, "examplan");

                while (parser.next() != XmlPullParser.END_TAG) {
                    if (parser.getEventType() != XmlPullParser.START_TAG)
                        continue;

                    String name = parser.getName();
                    if (name.equals("item")) {

                        Exams.ExamItem s = new Exams.ExamItem();
                        exams.add(s);

                        while (parser.next() != XmlPullParser.END_TAG) {
                            if (parser.getEventType() != XmlPullParser.START_TAG)
                                continue;

                            if (parser.getName().equals("id"))
                                s.id = parser.nextText();

                            else if (parser.getName().equals("date"))
                                s.date = parser.nextText();

                            else if (parser.getName().equals("schoolclass"))
                                s.schoolclass = parser.nextText();

                            else if (parser.getName().equals("lesson"))
                                s.lesson = parser.nextText();

                            else if (parser.getName().equals("length"))
                                s.length = parser.nextText();

                            else if (parser.getName().equals("subject"))
                                s.subject = parser.nextText();

                            else if (parser.getName().equals("teacher"))
                                s.teacher = parser.nextText();
                        }
                    }
                }
            } else if (con.getResponseCode() == 401) {
                logout(true, true);
                throw new VPLoginException();
            }
            exams.save("ggexams");
        } catch (Exception e) {
            e.printStackTrace();
            if(!exams.load("ggexams")) {
                exams.throwable = e;
                return exams;
            }
        }
        return exams;
    }

    @Override
    public int getColor() {
        return GGApp.GG_APP.getResources().getColor(R.color.main_orange);
    }

    @Override
    public int getDarkColor() {
        return GGApp.GG_APP.getResources().getColor(R.color.main_orange_dark);
    }

    @Override
    public int getTheme() {
        return R.style.AppThemeOrange;
    }

    @Override
    public int getImage() {
        return R.drawable.gg_logo;
    }

    @Override
    public String getWebsite() {
        return "http://gymglinde.de/";
    }

    @Override
    public boolean loginNeeded() {
        return true;
    }

    @Override
    public int login(String user, String pass) {
        try {

            HttpsURLConnection con = openConnection(BASE_URL + "infoapp/auth.php");
            con.setRequestMethod("POST");

            con.setDoOutput(true);
            DataOutputStream wr = new DataOutputStream(con.getOutputStream());
            wr.writeBytes("username=" + URLEncoder.encode(user, "utf-8") + "&password=" + URLEncoder.encode(pass, "utf-8"));
            wr.flush();
            wr.close();

            if(con.getResponseCode() == 200) {
                BufferedInputStream in = new BufferedInputStream(con.getInputStream());
                Scanner scan = new Scanner(in);
                String resp = "";
                while(scan.hasNextLine())
                    resp += scan.nextLine() + "\n";
                scan.close();
                if(resp.contains("<state>false</state>")) {
                    return 1;
                } else {
                    XmlPullParser parser = Xml.newPullParser();
                    parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
                    parser.setInput(new StringReader(resp));

                    prefs.edit().apply();

                    while(parser.next() != XmlPullParser.END_DOCUMENT) {
                        if (parser.getEventType() != XmlPullParser.START_TAG)
                            continue;

                        String name = parser.getName();
                        if (name.equals("sessid")) {
                            sessId = parser.nextText();
                            prefs.edit().putString("sessid", sessId).apply();
                        } else if(name.equals("username") || name.equals("token") || name.equals("firstname") || name.equals("lastname") || name.equals("group")) {
                            prefs.edit().putString(name, parser.nextText()).apply();
                        }
                    }

                    String group = prefs.getString("group", null);
                    if(group != null && !group.equals("lehrer")) {
                        gg.filters.mainFilter.type = Filter.FilterType.CLASS;
                        gg.filters.mainFilter.filter = group;
                    } else {
                        gg.filters.mainFilter.type = Filter.FilterType.TEACHER;
                        gg.filters.mainFilter.filter = user;
                    }
                    FilterActivity.saveFilter(GGApp.GG_APP.filters);
                }

                gg.activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        gg.activity.mContent.setFragmentLoading();
                    }
                });
                GGApp.GG_APP.refreshAsync(null, true, GGApp.GG_APP.getFragmentType());

            } else
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

    @Override
    public String getFullName() {
        return "Gymnasium Glinde";
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
            //Receive certificate information from the obtained certificate
            String recveived_pub_key = chain[0].getPublicKey().toString();

            //Extract the public key from the certificate information
            String obtained_key = recveived_pub_key.split("\\{")[1].split("\\}")[0].split(",")[0].split("=")[1];
            if(!pub_key.equals(obtained_key)) {
                //If the public key is not correct throw an exception, to prevent connecting
                // to this evil server
                throw new CertificateException();
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

    static HttpsURLConnection openConnection(String url) throws IOException {
        HttpsURLConnection con = (HttpsURLConnection) new URL(url).openConnection();
        con.setSSLSocketFactory(sslSocketFactory);
        con.setConnectTimeout(3000);
        con.connect();

        Log.w("ggvp", "connection to " + con.getURL().getHost() + " established");

        return con;
    }

}
