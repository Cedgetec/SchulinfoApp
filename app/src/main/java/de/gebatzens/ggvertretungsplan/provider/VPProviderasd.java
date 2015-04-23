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
package de.gebatzens.ggvertretungsplan.provider;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import de.gebatzens.ggvertretungsplan.GGApp;
import de.gebatzens.ggvertretungsplan.data.Exams;
import de.gebatzens.ggvertretungsplan.data.GGPlan;
import de.gebatzens.ggvertretungsplan.data.Mensa;
import de.gebatzens.ggvertretungsplan.data.News;

public abstract class VPProviderasd {

    GGApp gg;
    public SharedPreferences prefs;
    public String id;

    public VPProviderasd(GGApp app, String id) {
        this.gg = app;
        this.id = id;
        prefs = gg.getSharedPreferences(id + "user", Context.MODE_PRIVATE);
    }

    public static String decode(String html) {
        if(html == null)
            return null;

        html = html.trim();

        html = html.replaceAll("&uuml;", "ü");
        html = html.replaceAll("&auml;", "ä");
        html = html.replaceAll("&ouml;", "ö");

        html = html.replaceAll("&Uuml;", "Ü");
        html = html.replaceAll("&Auml;", "Ä");
        html = html.replaceAll("&Ouml;", "Ö");

        html = html.replaceAll("&nbsp;", "");
        html = html.replaceAll("---", ""); //SWS '---'

        return html;
    }

    /**
     *
     * @param toast
     * @return GGPlan[2], Elemente können nicht null sein
     */
    public abstract GGPlan.GGPlans getPlans(boolean toast);
    public abstract String getFullName();
    public abstract int getColor();
    public abstract int getDarkColor();
    public abstract int getTheme();
    public abstract int getImage();
    public abstract String getWebsite();
    public abstract boolean loginNeeded();
    public abstract int login(String u, String p);
    public abstract void logout(boolean logoutLocal, boolean deleteToken);
    public abstract News getNews();
    public abstract Mensa getMensa();
    public abstract Exams getExams();
    public abstract Bitmap getMensaImage(String filename) throws IOException;
    public abstract int getColorArray();

    /**
     * Gibt den Benutzernamen oder null, wenn man nicht angemeldet ist, zurück
     * @return
     */
    public String getUsername() {
        return prefs.getString("username", null);
    }

    public String getGroup() {
        return prefs.getString("group", null);
    }

    public static String getWeekday(Date date) {
        return new SimpleDateFormat("EEEE").format(date);
    }

}
