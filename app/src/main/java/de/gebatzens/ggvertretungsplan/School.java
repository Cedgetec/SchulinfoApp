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
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.util.JsonReader;
import android.util.JsonWriter;
import android.util.Log;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;

public class School {

    public String sid;
    public String name;
    private int color, darkColor, accentColor;
    private int theme;
    private int colorArray;
    public String themeName;
    public String image;
    public String website;
    public String city;
    public boolean loginNeeded;

    public static List<School> LIST = new ArrayList<School>();

    public int getTheme() {
        return theme;
    }

    public int getColor() {
        return color;
    }

    public int getDarkColor() {
        return darkColor;
    }

    public int getAccentColor() {
        return accentColor;
    }

    public int getColorArray() {
        return colorArray;
    }

    public void loadTheme() {
        String name = GGApp.GG_APP.getCustomThemeName();
        if(name == null)
            name = themeName;
        theme = GGApp.GG_APP.getResources().getIdentifier(GGApp.GG_APP.isDarkThemeEnabled() ? "AppTheme" + name + "Dark" : "AppTheme" + name + "Light", "style", GGApp.GG_APP.getPackageName());
        colorArray = GGApp.GG_APP.getResources().getIdentifier(GGApp.GG_APP.isDarkThemeEnabled() ? "CardviewColor" + name + "Dark" : "CardviewColor" + name + "Light", "array", GGApp.GG_APP.getPackageName());
        TypedArray ta = GGApp.GG_APP.obtainStyledAttributes(theme, new int [] {R.attr.colorPrimary});
        TypedArray tad = GGApp.GG_APP.obtainStyledAttributes(theme, new int [] {R.attr.colorPrimaryDark});
        TypedArray taa = GGApp.GG_APP.obtainStyledAttributes(theme, new int [] {R.attr.colorAccent});
        color = ta.getColor(0, Color.RED);
        darkColor = tad.getColor(0, Color.RED);
        accentColor = taa.getColor(0, Color.RED);
        ta.recycle();
        tad.recycle();
        taa.recycle();
    }

    @Override
    public String toString() {
        return "School[" + sid + "; " + name + "; " + color + "; " + darkColor + "; " + theme + "; " + colorArray + "; " + image + "; " + city + "; " + website + "; " + loginNeeded + "]";
    }

    @Override
    public boolean equals(Object o) {
        if(!(o instanceof School))
            return false;
        Log.d("ggvp", "equals: " + toString());

        School s = (School) o;

        return sid.equals(s.sid) && name.equals(s.name) && color == s.color && darkColor == s.darkColor && theme == s.theme
                   && colorArray == s.colorArray && image.equals(s.image) && city.equals(s.city) && website.equals(s.website) && loginNeeded == s.loginNeeded;
    }

    public static boolean fetchList() {
        Log.i("ggvp", "Downloading school list");

        GGApp.GG_APP.getResources().getIdentifier("asd", "styles", GGApp.GG_APP.getPackageName());

        List<School> newList = new ArrayList<School>();

        try {
            HttpURLConnection con = GGApp.GG_APP.remote.openConnection("/getSchools", false);
            if(con.getResponseCode() == 200) {
                JsonReader reader = new JsonReader(new InputStreamReader(con.getInputStream()));
                reader.beginArray();
                while(reader.hasNext()) {
                    reader.beginObject();
                    School s = new School();
                    while(reader.hasNext()) {
                        String name = reader.nextName();
                        if(name.equals("sid"))
                            s.sid = reader.nextString();
                        else if(name.equals("name"))
                            s.name = reader.nextString();
                        else if(name.equals("login"))
                            s.loginNeeded = reader.nextBoolean();
                        else if(name.equals("theme")) {
                            s.themeName = reader.nextString();
                            s.loadTheme();
                        } else if(name.equals("image"))
                            s.image = reader.nextString();
                        else if(name.equals("website"))
                            s.website = reader.nextString();
                        else if(name.equals("city"))
                            s.city = reader.nextString();
                        else
                            reader.skipValue();
                    }
                    newList.add(s);
                    reader.endObject();
                }
                reader.endArray();
                reader.close();

                LIST.clear();
                LIST.addAll(newList);

                Log.i("ggvp", "Downloaded school list");

                return true;
            } else {
                Log.e("ggvp", "server returned " + con.getResponseCode());

            }
        } catch(Exception e) {
            Log.w("ggvp", "Failed to download school list", e);

        }

        return false;
    }

    public static void saveList() {
        try {
            OutputStream out = GGApp.GG_APP.openFileOutput("schools", Context.MODE_PRIVATE);
            JsonWriter writer = new JsonWriter(new OutputStreamWriter(out));

            writer.setIndent("  ");
            writer.beginArray();
            for(School s : LIST) {
                writer.beginObject();

                writer.name("sid").value(s.sid);
                writer.name("name").value(s.name);
                writer.name("login").value(s.loginNeeded);
                writer.name("website").value(s.website);
                writer.name("image").value(s.image);
                writer.name("theme").value(s.themeName);
                writer.name("city").value(s.city);

                writer.endObject();
            }
            writer.endArray();
            writer.close();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    public static void loadList() {
        LIST.clear();
        try {
            InputStream in = GGApp.GG_APP.openFileInput("schools");
            JsonReader reader = new JsonReader(new InputStreamReader(in));
            reader.beginArray();
            while(reader.hasNext()) {
                reader.beginObject();
                School s = new School();

                while(reader.hasNext()) {
                    String name = reader.nextName();
                    if(name.equals("sid"))
                        s.sid = reader.nextString();
                    else if(name.equals("name"))
                        s.name = reader.nextString();
                    else if(name.equals("theme")) {
                        s.themeName = reader.nextString();
                        s.loadTheme();
                    } else if(name.equals("website"))
                        s.website = reader.nextString();
                    else if(name.equals("image"))
                        s.image = reader.nextString();
                    else if(name.equals("login"))
                        s.loginNeeded = reader.nextBoolean();
                    else if(name.equals("theme"))
                        s.theme = reader.nextInt();
                    else if(name.equals("city"))
                        s.city = reader.nextString();
                    else
                        reader.skipValue();
                }
                reader.endObject();
                LIST.add(s);
            }
            reader.endArray();
            reader.close();
        } catch(Exception e) {

        }
    }

    public static School getBySID(String sid) {
        for(School s : LIST)
            if(s.sid.equals(sid))
                return s;
        Log.w("ggvp", "School " + sid + " not found");
        return null;
    }

    public Bitmap loadImage() {
        try {
            return BitmapFactory.decodeStream(GGApp.GG_APP.openFileInput(GGApp.GG_APP.school.image));
        } catch (Exception e) {
            e.printStackTrace();
            return BitmapFactory.decodeResource(GGApp.GG_APP.getResources(), R.drawable.no_content);
        }
    }

    public boolean downloadImage() {
        if(new File(image).exists())
            return true;
        try {
            //InputStream in = GGApp.GG_APP.remote.openConnection("/infoapp/images/" + image, false).getInputStream();
            InputStream in = new URL("https://" + BuildConfig.BACKEND_SERVER + "/images/" + image).openStream();
            BitmapFactory.decodeStream(in).compress(Bitmap.CompressFormat.PNG, 90, GGApp.GG_APP.openFileOutput(image, Context.MODE_PRIVATE));
            in.close();
            return true;
        } catch(Exception e) {
            e.printStackTrace();
            return false;
        }
    }

}
