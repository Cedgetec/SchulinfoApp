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

import de.gebatzens.ggvertretungsplan.GGApp;
import de.gebatzens.ggvertretungsplan.fragment.RemoteDataFragment;

public class News extends ArrayList<String[]> implements RemoteDataFragment.RemoteData {

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
            for(String[] s : this) {
                writer.beginObject();

                writer.name("id").value(s[0]);
                writer.name("date").value(s[1]);
                writer.name("topic").value(s[2]);
                writer.name("source").value(s[3]);
                writer.name("title").value(s[4]);
                writer.name("text").value(s[5]);

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
                String[] s = new String[6];

                while(reader.hasNext()) {
                    String name = reader.nextName();
                    if(name.equals("id"))
                        s[0] = reader.nextString();
                    else if(name.equals("date"))
                        s[1] = reader.nextString();
                    else if(name.equals("topic"))
                        s[2] = reader.nextString();
                    else if(name.equals("source"))
                        s[3] = reader.nextString();
                    else if(name.equals("title"))
                        s[4] = reader.nextString();
                    else if(name.equals("text"))
                        s[5] = reader.nextString();
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
}