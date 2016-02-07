/*
 * Copyright 2015 - 2016 Hauke Oldsen
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import de.gebatzens.sia.GGApp;
import de.gebatzens.sia.fragment.RemoteDataFragment;

public class Exams extends ArrayList<Exams.ExamItem> implements RemoteDataFragment.RemoteData {

    public Throwable throwable;

    @Override
    public Throwable getThrowable() {
        return throwable;
    }

    public void save() {
        Log.d("ggvp", "Saving exams");

        try {
            OutputStream out = GGApp.GG_APP.openFileOutput("exams", Context.MODE_PRIVATE);
            JsonWriter writer = new JsonWriter(new OutputStreamWriter(out));

            writer.setIndent("  ");
            writer.beginArray();
            for(ExamItem s : this) {
                writer.beginObject();

                writer.name("date").value(s.date.getTime());
                writer.name("clazz").value(s.clazz);
                writer.name("lesson").value(s.lesson);
                writer.name("length").value(s.length);
                writer.name("subject").value(s.subject);
                writer.name("teacher").value(s.teacher);
                writer.name("selected").value(s.selected);

                writer.endObject();
            }
            writer.endArray();
            writer.close();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    public void sort() {
        Collections.sort(this, new Comparator<ExamItem>() {
            @Override
            public int compare(ExamItem lhs, ExamItem rhs) {
                return lhs.date.compareTo(rhs.date);
            }

        });
    }

    public Exams filter(Filter.FilterList filters, boolean past) {
        Exams e = new Exams();
        for(ExamItem item : this) {
            if(filters.matches(item) && (past || item.date.after(new Date(System.currentTimeMillis() - 86400000L)))) {
                e.add(item);
            }
        }
        return e;
    }

    public List<String> getAllClasses() {
        ArrayList<String> list = new ArrayList<String>();

        for(ExamItem e : this) {
            if(!list.contains(e.clazz)) {
                list.add(e.clazz);
            }
        }

        //TODO
        Collections.sort(list);

        return list;
    }

    public List<ExamItem> getSelectedItems(boolean past) {
        ArrayList<ExamItem> list = new ArrayList<>();

        for(ExamItem e : this) {
            if (e.selected && (past || e.date.after(new Date(System.currentTimeMillis() - 86400000L))))
                list.add(e);
        }

        return list;
    }

    public void reuseSelected(Exams exams) {
        for(ExamItem e : exams) {
            int index = indexOf(e);
            if(index != -1) {
                e.selected = get(index).selected;
            }
        }
    }

    public boolean load() {
        clear();
        try {
            InputStream in = GGApp.GG_APP.openFileInput("exams");
            JsonReader reader = new JsonReader(new InputStreamReader(in));
            reader.beginArray();
            while(reader.hasNext()) {
                reader.beginObject();
                ExamItem s = new ExamItem();

                while(reader.hasNext()) {
                    String name = reader.nextName();
                    if(name.equals("date"))
                        s.date = new Date(reader.nextLong());
                    else if(name.equals("clazz"))
                        s.clazz = reader.nextString();
                    else if(name.equals("lesson"))
                        s.lesson = reader.nextString();
                    else if(name.equals("length"))
                        s.length = reader.nextString();
                    else if(name.equals("subject"))
                        s.subject = reader.nextString();
                    else if(name.equals("teacher"))
                        s.teacher = reader.nextString();
                    else if(name.equals("selected"))
                        s.selected = reader.nextBoolean();
                    else
                        reader.skipValue();
                }
                reader.endObject();
                add(s);
            }
            reader.endArray();
            reader.close();
        } catch(Exception e) {
            Log.w("ggvp", "Exams file does not exist");
            return false;
        }

        return true;
    }

    public static class ExamItem implements Filter.Filterable {
        public Date date;
        public String clazz;
        public String lesson;
        public String length;
        public String subject;
        public String teacher;
        public boolean selected;

        @Override
        public boolean equals(Object o) {
            if(!(o instanceof ExamItem))
                return false;
            ExamItem e = (ExamItem) o;
            return e.date.equals(date) && e.clazz.equals(clazz) && e.lesson.equals(lesson) &&
                    e.length.equals(length) && e.subject.equals(subject) && e.teacher.equals(teacher);
        }
    }
}