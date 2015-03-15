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