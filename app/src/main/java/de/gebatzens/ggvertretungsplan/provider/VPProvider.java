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

public abstract class VPProvider {

    GGApp gg;
    public SharedPreferences prefs;
    public String id;

    public VPProvider(GGApp app, String id) {
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
