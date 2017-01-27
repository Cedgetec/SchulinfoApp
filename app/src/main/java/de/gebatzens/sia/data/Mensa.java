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

package de.gebatzens.sia.data;

import android.content.Context;
import android.util.JsonReader;
import android.util.JsonWriter;
import android.util.Log;

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

import de.gebatzens.sia.SIAApp;
import de.gebatzens.sia.fragment.RemoteDataFragment;

public class Mensa extends ArrayList<Mensa.MensaItem> implements RemoteDataFragment.RemoteData {

    public Throwable throwable;

    @Override
    public Throwable getThrowable() {
        return throwable;
    }

    @Override
    public boolean isEmpty() {
        boolean b = true;
        for(MensaItem item : this)
            if(!item.isPast())
                b = false;
        return b;
    }

    public void save() {
        try {
            OutputStream out = SIAApp.SIA_APP.openFileOutput("mensa", Context.MODE_PRIVATE);
            JsonWriter writer = new JsonWriter(new OutputStreamWriter(out));

            writer.setIndent("  ");
            writer.beginArray();
            for(MensaItem s : this) {
                writer.beginObject();

                writer.name("id").value(s.id);
                writer.name("date").value(s.date);
                writer.name("meal").value(s.meal);
                writer.name("garnish").value(s.garnish);
                writer.name("dessert").value(s.dessert);
                writer.name("vegetarian").value(s.vegetarian);
                writer.name("image").value(s.image);

                writer.endObject();
            }
            writer.endArray();
            writer.close();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    public boolean load() {
        clear();
        try {
            InputStream in = SIAApp.SIA_APP.openFileInput("mensa");
            JsonReader reader = new JsonReader(new InputStreamReader(in));
            reader.beginArray();
            while(reader.hasNext()) {
                reader.beginObject();
                MensaItem item = new MensaItem();

                while(reader.hasNext()) {
                    String name = reader.nextName();
                    switch (name) {
                        case "id":
                            item.id = reader.nextString();
                            break;
                        case "date":
                            item.date = reader.nextString();
                            break;
                        case "meal":
                            item.meal = reader.nextString();
                            break;
                        case "garnish":
                            item.garnish = reader.nextString();
                            break;
                        case "dessert":
                            item.dessert = reader.nextString();
                            break;
                        case "vegetarian":
                            item.vegetarian = reader.nextString();
                            break;
                        case "image":
                            item.image = reader.nextString();
                            break;
                        default:
                            reader.skipValue();
                            break;
                    }
                }
                reader.endObject();
                add(item);
            }
            reader.endArray();
            reader.close();
        } catch(Exception e) {
            Log.w("ggvp", "Mensa file does not exist");
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
        public String dessert;
        public String vegetarian;
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

        @Override
        public boolean equals(Object o) {
            if(!(o instanceof MensaItem))
                return false;
            MensaItem m = (MensaItem) o;
            return m.id.equals(id) && m.date.equals(date) && m.meal.equals(meal) && m.garnish.equals(garnish) &&
                    m.dessert.equals(dessert) && m.vegetarian.equals(vegetarian) && m.image.equals(image);
        }

    }
}