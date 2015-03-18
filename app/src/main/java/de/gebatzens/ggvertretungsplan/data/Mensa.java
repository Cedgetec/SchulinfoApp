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

package de.gebatzens.ggvertretungsplan.data;

import android.content.Context;
import android.util.JsonReader;
import android.util.JsonWriter;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import de.gebatzens.ggvertretungsplan.GGApp;
import de.gebatzens.ggvertretungsplan.fragment.RemoteDataFragment;

public class Mensa extends ArrayList<Mensa.MensaItem> implements RemoteDataFragment.RemoteData {

    public Throwable throwable;

    @Override
    public Throwable getThrowable() {
        return throwable;
    }

    public void save(String file) {
        try {
            OutputStream out = GGApp.GG_APP.openFileOutput(file, Context.MODE_PRIVATE);
            JsonWriter writer = new JsonWriter(new OutputStreamWriter(out));

            writer.setIndent("  ");
            writer.beginArray();
            for(MensaItem s : this) {
                writer.beginObject();

                writer.name("id").value(s.id);
                writer.name("date").value(s.date);
                writer.name("meal").value(s.meal);
                writer.name("garnish").value(s.garnish);
                writer.name("vegi").value(s.vegi);
                writer.name("image").value(s.image);

                writer.endObject();
            }
            writer.endArray();
            writer.close();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    public boolean load(String file) {
        clear();
        try {
            InputStream in = GGApp.GG_APP.openFileInput(file);
            JsonReader reader = new JsonReader(new InputStreamReader(in));
            reader.beginArray();
            while(reader.hasNext()) {
                reader.beginObject();
                MensaItem item = new MensaItem();

                while(reader.hasNext()) {
                    String name = reader.nextName();
                    if(name.equals("id"))
                        item.id = reader.nextString();
                    else if(name.equals("date"))
                        item.date = reader.nextString();
                    else if(name.equals("meal"))
                        item.meal = reader.nextString();
                    else if(name.equals("garnish"))
                        item.garnish = reader.nextString();
                    else if(name.equals("vegi"))
                        item.vegi = reader.nextString();
                    else if(name.equals("image"))
                        item.image = reader.nextString();
                    else
                        reader.skipValue();
                }
                reader.endObject();
                add(item);
            }
            reader.endArray();
            reader.close();
        } catch(Exception e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    private static Date getDate(String date) throws ParseException {
        DateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        return sdf.parse(date);
    }

    public static class MensaItem {
        public String id;
        public String date;
        public String meal;
        public String garnish;
        public String vegi;
        public String image;

        public boolean isPast() {
            Date d = new Date();
            Calendar c = Calendar.getInstance();
            c.setTime(d);
            c.add(Calendar.DAY_OF_YEAR, -1);
            Date dt = c.getTime();
            try {
                if(getDate(this.date).before(dt)) {
                    return true;
                }
            } catch (ParseException e) {
                e.printStackTrace();
            }
            return false;
        }

    }
}