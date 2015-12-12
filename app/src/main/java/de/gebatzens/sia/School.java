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
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.util.JsonWriter;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

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
    public List<GGApp.FragmentType> fragments;
    public int users;

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
        return "School[" + sid + "; " + name + "; " + color + "; " + darkColor + "; " + theme + "; " + colorArray + "; " + image + "; " + city + "; " + website + "; " + loginNeeded +  ";" + fragments + "]";
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

    public static void updateSchool(JSONObject school) throws JSONException {
        String sid = school.getString("sid");
        School s = getBySID(sid);
        if(s == null) {
            s = new School();
            LIST.add(s);
            s.sid = sid;
        }

        s.name = school.getString("name");
        s.city = school.getString("city");
        s.themeName = school.getString("theme");
        s.website = school.optString("website");
        s.loginNeeded = school.getBoolean("authRequired");
        s.image = school.optString("image", "");
        s.users = school.optInt("users", 0);
        s.loadTheme();

        s.fragments = new ArrayList<>();
        JSONArray frags = school.optJSONArray("fragments");
        if(frags != null) {
            for(int i = 0; i < frags.length(); i++) {
                s.fragments.add(GGApp.FragmentType.valueOf(frags.getJSONObject(i).getString("type")));
            }

        } else {
            s.fragments.add(GGApp.FragmentType.PLAN);
        }

    }

    public static boolean fetchList() {
        Log.i("ggvp", "Downloading school list");

        GGApp.GG_APP.getResources().getIdentifier("asd", "styles", GGApp.GG_APP.getPackageName());

        List<School> newList = new ArrayList<School>();

        try {
            GGRemote.APIResponse re = GGApp.GG_APP.remote.doRequest("/getSchools", null);
            if(re.state == GGRemote.APIState.SUCCEEDED) {
                LIST.clear();
                JSONArray schools = (JSONArray) re.data;
                for(int i = 0; i < schools.length(); i++) {
                    JSONObject school = schools.getJSONObject(i);
                    updateSchool(school);

                }

                Log.i("ggvp", "Downloaded school list");

                return true;
            } else {
                throw new Exception("Received state " + re.state);
            }
        } catch(Exception e) {
            if(e instanceof IOException)
                Log.w("ggvp", "Failed to download school list " + e.getMessage());
            else
                e.printStackTrace();
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
                writer.name("authRequired").value(s.loginNeeded);
                writer.name("website").value(s.website);
                writer.name("image").value(s.image);
                writer.name("theme").value(s.themeName);
                writer.name("city").value(s.city);
                writer.name("users").value(s.users);
                writer.name("fragments").beginArray();
                for(GGApp.FragmentType type : s.fragments) {
                    writer.beginObject().name("type").value(type.toString()).endObject();
                }
                writer.endArray();

                writer.endObject();
            }
            writer.endArray();
            writer.close();
        } catch(Exception e) {
            Log.e("ggvp", e.toString());
            e.printStackTrace();
        }
    }

    public static void loadList() {
        LIST.clear();
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(GGApp.GG_APP.openFileInput("schools")));
            String content = "", line = "";
            while((line = reader.readLine()) != null)
                content += line;

            JSONArray array = new JSONArray(content);
            for(int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                updateSchool(obj);

            }

        } catch(FileNotFoundException e) {
            Log.e("ggvp", e.toString());
        } catch(Exception e) {
            Log.e("ggvp", e.toString());
            e.printStackTrace();
        }
    }

    public static School getBySID(String sid) {
        for(School s : LIST)
            if(s.sid.equals(sid))
                return s;
        //Log.w("ggvp", "School " + sid + " not found");
        return null;
    }

    public Bitmap loadImage() {
        try {
            return BitmapFactory.decodeStream(GGApp.GG_APP.openFileInput(image));
        } catch (Exception e) {
            Log.w("ggvp", "school image not found " + image);
            return BitmapFactory.decodeResource(GGApp.GG_APP.getResources(), R.drawable.no_content);
        }
    }

    public static boolean downloadImage(String image) {
        if(new File(image).exists())
            return true;

        try {
            InputStream in = new URL(BuildConfig.BACKEND_SERVER + "/image?name=" + URLEncoder.encode(image, "utf-8")).openStream();
            BitmapFactory.decodeStream(in).compress(Bitmap.CompressFormat.PNG, 90, GGApp.GG_APP.openFileOutput(image, Context.MODE_PRIVATE));
            in.close();
            return true;
        } catch(Exception e) {
            Log.e("ggvp", e.toString());
            e.printStackTrace();
            return false;
        }
    }

}
