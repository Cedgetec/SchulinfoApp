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
import java.util.ArrayList;
import java.util.Date;

import de.gebatzens.ggvertretungsplan.GGApp;
import de.gebatzens.ggvertretungsplan.fragment.RemoteDataFragment;

public class News extends ArrayList<News.Entry> implements RemoteDataFragment.RemoteData {

    public Throwable throwable;

    @Override
    public Throwable getThrowable() {
        return throwable;
    }

    @Override
    public void save() {
        try {
            OutputStream out = GGApp.GG_APP.openFileOutput("news", Context.MODE_PRIVATE);
            JsonWriter writer = new JsonWriter(new OutputStreamWriter(out));

            writer.setIndent("  ");
            writer.beginArray();
            for(Entry s : this) {
                writer.beginObject();

                writer.name("id").value(s.id);
                writer.name("date").value(s.date.getTime());
                writer.name("topic").value(s.topic);
                writer.name("source").value(s.source);
                writer.name("title").value(s.title);
                writer.name("text").value(s.text);

                writer.endObject();
            }
            writer.endArray();
            writer.close();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean load() {
        clear();
        try {
            InputStream in = GGApp.GG_APP.openFileInput("news");
            JsonReader reader = new JsonReader(new InputStreamReader(in));
            reader.beginArray();
            while(reader.hasNext()) {
                reader.beginObject();
                Entry s = new Entry();

                while(reader.hasNext()) {
                    String name = reader.nextName();
                    if(name.equals("id"))
                        s.id = reader.nextString();
                    else if(name.equals("date"))
                        s.date = new Date(reader.nextLong());
                    else if(name.equals("topic"))
                        s.topic = reader.nextString();
                    else if(name.equals("source"))
                        s.source = reader.nextString();
                    else if(name.equals("title"))
                        s.title = reader.nextString();
                    else if(name.equals("text"))
                        s.text = reader.nextString();
                    else
                        reader.skipValue();
                }
                reader.endObject();
                add(s);
            }
            reader.endArray();
            reader.close();
        } catch(Exception e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    public static class Entry {
        public String id, topic, source, title, text;
        public Date date;

        @Override
        public boolean equals(Object o) {
            if(!(o instanceof Entry))
                return false;
            Entry e = (Entry) o;
            return e.date.equals(date) && e.id.equals(id) && e.topic.equals(topic) &&
                    e.source.equals(source) && e.title.equals(title) && e.text.equals(text);
        }
    }
}