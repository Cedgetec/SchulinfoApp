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

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.util.JsonReader;
import android.util.Log;

import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;

public class School {

    public String sid;
    public String name;
    public int color = GGApp.GG_APP.getResources().getColor(R.color.main_orange), darkColor = GGApp.GG_APP.getResources().getColor(R.color.main_orange_dark);
    public String image;
    public String website;
    public boolean loginNeeded;

    public static List<School> fetchList() {
        List<School> list = new ArrayList<School>();

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
                        else
                            reader.skipValue();
                    }
                    list.add(s);
                    reader.endObject();
                }
                reader.endArray();
                reader.close();
            } else {
                Log.e("ggvp", "server returned " + con.getResponseCode());
            }
        } catch(Exception e) {
            return null;
        }
        return list;
    }

    public Bitmap loadImage() {
        try {
            return BitmapFactory.decodeStream(GGApp.GG_APP.openFileInput(GGApp.GG_APP.school.image));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return BitmapFactory.decodeResource(GGApp.GG_APP.getResources(), R.drawable.no_content);
        }
    }

}
