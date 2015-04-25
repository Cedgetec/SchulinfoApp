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
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;

public class School {

    public String sid;
    public String name;
    public int color = GGApp.GG_APP.getResources().getColor(R.color.main_orange), darkColor = GGApp.GG_APP.getResources().getColor(R.color.main_orange_dark);
    public String image;
    public String website;
    public String city;
    public boolean loginNeeded;

    public static List<School> LIST;

    public static void fetchList() {
        Log.i("ggvp", "Downloading school list");
        LIST.clear();
        try {
            HttpsURLConnection con = GGApp.GG_APP.remote.openConnection("/infoapp/get_schools.php", false);
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
                        else if(name.equals("color"))
                            s.color = Color.parseColor(reader.nextString());
                        else if(name.equals("darkColor"))
                            s.darkColor = Color.parseColor(reader.nextString());
                        else if(name.equals("image"))
                            s.image = reader.nextString();
                        else if(name.equals("website"))
                            s.website = reader.nextString();
                        else if(name.equals("city"))
                            s.city = reader.nextString();
                        else
                            reader.skipValue();
                    }
                    LIST.add(s);
                    reader.endObject();
                }
                reader.endArray();
                reader.close();

                Log.i("ggvp", "Downloaded school list");
            } else {
                Log.e("ggvp", "server returned " + con.getResponseCode());
            }
        } catch(Exception e) {
            Log.w("ggvp", "Failed to download school list", e);
        }
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
                writer.name("color").value(s.color);
                writer.name("darkColor").value(s.darkColor);
                writer.name("login").value(s.loginNeeded);
                writer.name("website").value(s.website);
                writer.name("image").value(s.image);

                writer.endObject();
            }
            writer.endArray();
            writer.close();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    public static void loadList() {
        LIST = new ArrayList<School>();
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
                    else if(name.equals("color"))
                        s.color = reader.nextInt();
                    else if(name.equals("darkColor"))
                        s.darkColor = reader.nextInt();
                    else if(name.equals("website"))
                        s.website = reader.nextString();
                    else if(name.equals("image"))
                        s.image = reader.nextString();
                    else if(name.equals("login"))
                        s.loginNeeded = reader.nextBoolean();
                    else
                        reader.skipValue();
                }
                reader.endObject();
                LIST.add(s);
            }
            reader.endArray();
            reader.close();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    public static School getBySID(String sid) {
        for(School s : LIST)
            if(s.sid.equals(sid))
                return s;
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
            InputStream in = GGApp.GG_APP.remote.openConnection("/infoapp/images/" + image, false).getInputStream();
            BitmapFactory.decodeStream(in).compress(Bitmap.CompressFormat.PNG, 90, GGApp.GG_APP.openFileOutput(image, Context.MODE_PRIVATE));
            in.close();
            return true;
        } catch(Exception e) {
            e.printStackTrace();
            return false;
        }
    }

}
