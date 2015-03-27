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

public class Exams extends ArrayList<Exams.ExamItem> implements RemoteDataFragment.RemoteData {

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
            for(ExamItem s : this) {
                writer.beginObject();

                writer.name("id").value(s.id);
                writer.name("date").value(s.date);
                writer.name("schoolclass").value(s.schoolclass);
                writer.name("lesson").value(s.lesson);
                writer.name("length").value(s.length);
                writer.name("subject").value(s.subject);
                writer.name("teacher").value(s.teacher);

                writer.endObject();
            }
            writer.endArray();
            writer.close();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    public Exams filter(Filter.FilterList filters) {
        Exams e = new Exams();
        for(ExamItem item : this) {
            boolean b = filters.mainFilter.matches(item);
            if(b) {
                for(Filter f : filters) {
                    if(f.matches(item))
                        b = false;
                }
                if(b) {
                    e.add(item);
                }
            }
        }
        return e;
    }

    public boolean load(String file) {
        clear();
        try {
            InputStream in = GGApp.GG_APP.openFileInput(file);
            JsonReader reader = new JsonReader(new InputStreamReader(in));
            reader.beginArray();
            while(reader.hasNext()) {
                reader.beginObject();
                ExamItem s = new ExamItem();

                while(reader.hasNext()) {
                    String name = reader.nextName();
                    if(name.equals("id"))
                        s.id = reader.nextString();
                    else if(name.equals("date"))
                        s.date = reader.nextString();
                    else if(name.equals("schoolclass"))
                        s.schoolclass = reader.nextString();
                    else if(name.equals("lesson"))
                        s.lesson = reader.nextString();
                    else if(name.equals("length"))
                        s.length = reader.nextString();
                    else if(name.equals("subject"))
                        s.subject = reader.nextString();
                    else if(name.equals("teacher"))
                        s.teacher = reader.nextString();
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

    public static class ExamItem {
        public String id;
        public String date;
        public String schoolclass;
        public String lesson;
        public String length;
        public String subject;
        public String teacher;
    }
}