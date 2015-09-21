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

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.gebatzens.sia.GGApp;
import de.gebatzens.sia.R;
import de.gebatzens.sia.fragment.RemoteDataFragment;

public class GGPlan {

    public ArrayList<Entry> entries = new ArrayList<Entry>();
    public Date date;
    public List<String> special = new ArrayList<String>();

    public GGPlan() {

    }

    public String getWeekday() {
        return new SimpleDateFormat("EEEE").format(date);
    }

    public static class GGPlans extends ArrayList<GGPlan> implements RemoteDataFragment.RemoteData {

        public Throwable throwable;
        public String loadDate;

        @Override
        public Throwable getThrowable() {
            return throwable;
        }

        @Override
        public void save() {
            GGApp.GG_APP.preferences.edit().putString("loadDate", loadDate).commit();

            for(int i = 0; i < size(); i++)
                get(i).save("schedule" + i);
        }

        @Override
        public boolean load() {
            loadDate = GGApp.GG_APP.preferences.getString("loadDate", "");
            try {
                for(int i = 0; ; i++) {
                    GGPlan plan = new GGPlan();
                    plan.load("schedule" + i);
                    add(plan);
                }

            } catch(FileNotFoundException e) {
                Log.w("ggvp", "Subst file does not exist");
            } catch(Exception e) {
                e.printStackTrace();
            }
            return size() > 0;
        }

        public GGPlan getPlanByDate(Date d) {
            for(GGPlan plan : this)
                if(plan.date.equals(d))
                    return plan;
            return null;
        }
    }

    public void load(String file) throws Exception {
        entries.clear();
        special.clear();

        InputStream in = GGApp.GG_APP.openFileInput(file);
        JsonReader reader = new JsonReader(new InputStreamReader(in));
        reader.beginObject();
        while(reader.hasNext()) {
            String name = reader.nextName();
            if(name.equals("date")) {
                date = new Date(reader.nextLong());
            } else if(name.equals("messages")) {
                reader.beginArray();
                while(reader.hasNext()) {
                    special.add(reader.nextString());
                }
                reader.endArray();
            } else if(name.equals("entries")) {
                reader.beginArray();
                while(reader.hasNext()) {
                    reader.beginObject();
                    Entry e = new Entry();
                    e.date = date;
                    while(reader.hasNext()) {
                        String name2 = reader.nextName();
                        if(name2.equals("class"))
                            e.clazz = reader.nextString();
                        else if(name2.equals("lesson"))
                            e.lesson = reader.nextString();
                        else if(name2.equals("teacher"))
                            e.teacher = reader.nextString();
                        else if(name2.equals("subject"))
                            e.subject = reader.nextString();
                        else if(name2.equals("comment"))
                            e.comment = reader.nextString();
                        else if(name2.equals("type"))
                            e.type = reader.nextString();
                        else if(name2.equals("room"))
                            e.room = reader.nextString();
                        else if(name2.equals("repsub"))
                            e.repsub = reader.nextString();
                        else
                            reader.skipValue();

                    }
                    //if(!e.isValid()) {
                    //    reader.close();
                    //    return false;
                    //}
                    //Log.w("ggvp", "Loaded " + e);
                    entries.add(e);
                    reader.endObject();
                }
                reader.endArray();
            } else
                reader.skipValue();
        }
        reader.endObject();
        reader.close();
    }

    public void save(String file) {
        Log.w("ggvp", "Saving " + file);
        try {
            OutputStream out = GGApp.GG_APP.openFileOutput(file, Context.MODE_PRIVATE);
            JsonWriter writer = new JsonWriter(new OutputStreamWriter(out, "UTF-8"));

            writer.setIndent("  ");
            writer.beginObject();
            writer.name("date").value(date.getTime());
            writer.name("messages");
            writer.beginArray();
            for(String s : special)
                writer.value(s);
            writer.endArray();

            writer.name("entries");
            writer.beginArray();
            for(Entry e : entries) {
                writer.beginObject();
                writer.name("class").value(e.clazz);
                writer.name("lesson").value(e.lesson);
                writer.name("teacher").value(e.teacher);
                writer.name("subject").value(e.subject);
                writer.name("comment").value(e.comment);
                writer.name("type").value(e.type);
                writer.name("room").value(e.room);
                writer.name("repsub").value(e.repsub);
                writer.endObject();
            }
            writer.endArray();
            writer.endObject();
            writer.close();
        } catch(Exception e) {
            e.printStackTrace();

        }
    }

    public List<String> getAllClasses() {
        ArrayList<String> list = new ArrayList<String>();

        for(Entry e : entries) {
            if(!list.contains(e.clazz)) {
                list.add(e.clazz);
            }
        }
        return list;
    }

    public List<String> getAllLessons() {
        ArrayList<String> list = new ArrayList<String>();

        for(Entry e : entries) {
            if(!list.contains(e.lesson)) {
                list.add(e.lesson);

            }
        }

        Collections.sort(list, new Comparator<String>() {
            @Override
            public int compare(String lhs, String rhs) {
                try {
                    int l1 = Integer.parseInt(lhs);
                    int l2 = Integer.parseInt(rhs);

                    return l1 - l2;
                } catch(Exception e) {
                    Log.d("ggvp", "Lesson parsing failed " + e.getMessage());
                }

                return lhs.compareTo(rhs);
            }
        });

        return list;
    }

    @Override
    public boolean equals(Object o) {
        if(o instanceof GGPlan) {
            GGPlan plan = (GGPlan) o;
            return plan.entries.equals(entries) && plan.date.equals(date) && plan.special.equals(special);
        } else
            return false;
    }

    public List<Entry> filter(Filter.FilterList filters) {
        ArrayList<Entry> list = new ArrayList<Entry>();
        for(Entry e : entries) {
            if(filters.mainFilter.matches(e))
                list.add(e);
        }

        for(Filter f : filters) {
            ArrayList<Entry> rlist = new ArrayList<Entry>();
            for(Entry e : list)
                if(f.matches(e))
                    rlist.add(e);
            for(Entry e : rlist)
                list.remove(e);
        }

        Collections.sort(list, new Comparator<Entry>() {
            @Override
            public int compare(Entry lhs, Entry rhs) {
                try {
                    int l1 = Integer.parseInt(lhs.lesson);
                    int l2 = Integer.parseInt(rhs.lesson);

                    return l1 - l2;
                } catch(Exception e) {
                    Log.d("ggvp", "Lesson parsing failed " + e.getMessage());
                }

                return lhs.lesson.compareTo(rhs.lesson);
            }
        });

        return list;

    }

    public static class Entry {
        public String type;
        public String clazz;
        public String missing = "";
        public String teacher = "";
        public String subject = "";
        public String repsub = "";
        public String comment = "";
        public String lesson = "";
        public String room = "";
        public Date date;

        @Override
        public boolean equals(Object o) {
            if(o instanceof Entry) {
                Entry e = (Entry) o;
                return e.type.equals(type) && e.clazz.equals(clazz) && e.subject.equals(subject)
                        && e.teacher.equals(teacher) && e.comment.equals(comment)
                        && e.lesson.equals(lesson) && e.room.equals(room) && e.repsub.equals(repsub);
            } else
                return false;
        }

        @Override
        public String toString() {
            return "Entry[" + type + " " + clazz + " " + subject + " " + teacher + " " + comment + " " + lesson + " " + room + " " + repsub + "]";
        }

        public void unify() {

            Matcher task = Pattern.compile("task (.*)").matcher(comment);

            if(type.equals("entf")) {
                type = GGApp.GG_APP.getString(R.string.canceled);

                if(task.find())
                    comment = GGApp.GG_APP.getString(R.string.task_through) + " " + task.group(1);
            } else if(type.equals("eva")) {
                type = "EVA";

                if(task.find())
                    comment = GGApp.GG_APP.getString(R.string.task_through) + " " + task.group(1);
            } else if(type.equals("teacher")) {
                type = GGApp.GG_APP.getString(R.string.substitute);

                if(task.find())
                    comment = GGApp.GG_APP.getString(R.string.task_through) + " " + task.group(1);
            } else if(type.equals("exam")) {
                type = GGApp.GG_APP.getString(R.string.exam);
            } else if(type.equals("lesson")) {
                type = GGApp.GG_APP.getString(R.string.lesson);
            } else if(type.equals("shifted")) {
                type = GGApp.GG_APP.getString(R.string.canceled) + " / " +  GGApp.GG_APP.getResources().getString(R.string.shift);

                Matcher m = Pattern.compile("shift (\\S*) (\\S*)").matcher(comment);

                if(m.find()) {
                    String sdates = m.group(1);
                    Date sdate = sdates.contains("today") ? date : parseDate(sdates);
                    String lesson = m.group(2);

                    if(sdate.equals(date))
                        comment = GGApp.GG_APP.getString(R.string.shifted_to_today) + " " + lesson + ". " + GGApp.GG_APP.getString(R.string.lhour);
                    else
                        comment = GGApp.GG_APP.getString(R.string.shifted_to) + " " + new SimpleDateFormat("EEE").format(sdate) + ", " +
                                DateFormat.getDateInstance().format(sdate) + " " + lesson + ". " + GGApp.GG_APP.getString(R.string.lhour);
                }
            } else if(type.equals("instead")) {
                type = GGApp.GG_APP.getString(R.string.substitute) + " / " +  GGApp.GG_APP.getResources().getString(R.string.shift);

                Matcher m = Pattern.compile("instead (\\S*) (\\S*)").matcher(comment);

                if(m.find()) {
                    String idates = m.group(1);
                    Date idate = idates.contains("today") ? date : parseDate(idates);
                    String lesson = m.group(2);

                    if(idate.equals(date))
                        comment = GGApp.GG_APP.getString(R.string.instead_of) + " " + lesson + ". " + GGApp.GG_APP.getString(R.string.lhour);
                    else
                        comment = GGApp.GG_APP.getString(R.string.instead_of) + " " + new SimpleDateFormat("EEE").format(idate) + ", " +
                                DateFormat.getDateInstance().format(idate) + " " + lesson + ". " + GGApp.GG_APP.getString(R.string.lhour);
                }

            } else if(type.equals("supervision")) {
                type = GGApp.GG_APP.getString(R.string.supervision);
            } else if(type.equals("lunch")) {
                type = GGApp.GG_APP.getString(R.string.lunch);
            } else if(type.equals("subst")) {
                type = GGApp.GG_APP.getString(R.string.substitute);

                if(task.find())
                    comment = GGApp.GG_APP.getString(R.string.task_through) + " " + task.group(1);
            }

            if(subject.isEmpty() && !repsub.isEmpty())
                subject = "&#x2192; " + repsub;
            else if(!subject.isEmpty() && !repsub.isEmpty() && !subject.equals(repsub))
                subject += " &#x2192; " + repsub;

            comment = comment.replace("  ", " ");

        }

        public static String translateSubject(String s) {
            String t = GGApp.GG_APP.subjects.get(s.toLowerCase());
            if(t == null)
                return s;
            else
                return t;
        }
    }

    public static Date parseDate(String date) {
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd");
        try {
            return fmt.parse(date);
        } catch(Exception e) {
            Log.w("ggvp", "WARNING: Invalid date " + date);
            return null;
        }
    }

}
