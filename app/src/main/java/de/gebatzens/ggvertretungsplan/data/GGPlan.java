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
import android.util.Log;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.gebatzens.ggvertretungsplan.GGApp;
import de.gebatzens.ggvertretungsplan.R;
import de.gebatzens.ggvertretungsplan.fragment.RemoteDataFragment;

public class GGPlan {

    public ArrayList<Entry> entries = new ArrayList<Entry>();
    public Date date;
    public List<String> special = new ArrayList<String>();
    public String loadDate = "";

    public GGPlan() {

    }

    public static class GGPlans implements RemoteDataFragment.RemoteData {

        public Throwable throwable;
        public GGPlan today, tomorrow;

        @Override
        public Throwable getThrowable() {
            return throwable;
        }

        @Override
        public void save(String file) {
            today.save(file + "today");
            tomorrow.save(file + "tomorrow");
        }

        @Override
        public boolean load(String file) {
            return today.load(file + "today") && tomorrow.load(file + "tomorrow");
        }
    }

    public boolean load(String file) {
        Log.w("ggvp", "Lade " + file);
        entries.clear();
        special.clear();
        loadDate = "";

        try {
            InputStream in = GGApp.GG_APP.openFileInput(file);
            JsonReader reader = new JsonReader(new InputStreamReader(in));
            reader.beginObject();
            while(reader.hasNext()) {
                String name = reader.nextName();
                if(name.equals("loadDate"))
                    loadDate = reader.nextString();
                else if(name.equals("date")) {
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
                        while(reader.hasNext()) {
                            String name2 = reader.nextName();
                            if(name2.equals("class"))
                                e.clazz = reader.nextString();
                            else if(name2.equals("hour"))
                                e.hour = reader.nextString();
                            else if(name2.equals("subst"))
                                e.subst = reader.nextString();
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

        } catch(Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public void save(String file) {
        Log.w("ggvp", "Speichere " + file);
        try {
            OutputStream out = GGApp.GG_APP.openFileOutput(file, Context.MODE_PRIVATE);
            JsonWriter writer = new JsonWriter(new OutputStreamWriter(out, "UTF-8"));

            writer.setIndent("  ");
            writer.beginObject();
            writer.name("loadDate").value(loadDate);
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
                writer.name("hour").value(e.hour);
                writer.name("subst").value(e.subst);
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

        String last = "";
        for(Entry e : entries) {
            if(!last.equals(e.clazz)) {
                list.add(e.clazz);
                last = e.clazz;
            }
        }
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
        return list;

    }

    public static class Entry {
        public String type;
        public String clazz;
        //String missing;
        public String subst = "";
        public String subject = "";
        //Neues Fach
        public String repsub = "";
        public String comment = "";
        public String hour = "";
        public String room = "";

        @Override
        public boolean equals(Object o) {
            if(o instanceof Entry) {
                Entry e = (Entry) o;
                return e.type.equals(type) && e.clazz.equals(clazz) && e.subject.equals(subject)
                        && e.subst.equals(subst) && e.comment.equals(comment)
                        && e.hour.equals(hour) && e.room.equals(room) && e.repsub.equals(repsub);
            } else
                return false;
        }

        @Override
        public String toString() {
            return "Entry[" + type + " " + clazz + " " + subject + " " + subst + " " + comment + " " + hour + " " + room + " " + repsub;
        }

        /**
         * Comment hat Inhalt, type noch nicht
         *
         */
        public void unify() {
            Pattern aufg = Pattern.compile("Aufg. durch (\\w+)");
            String commentl = comment.toLowerCase();

            Matcher aufgm = aufg.matcher(comment);

            if(commentl.contains("eigenverantwortlich") || commentl.contains("eva")) {
                type = "EVA";

                if(aufgm.find()) {
                    comment = GGApp.GG_APP.getResources().getString(R.string.task_through) + " " + aufgm.group(1);
                } else
                    comment = "";

            } else if(commentl.contains("siehe")) {
                type =  GGApp.GG_APP.getResources().getString(R.string.elemination) + " / " +  GGApp.GG_APP.getResources().getString(R.string.shift);

                Matcher m = Pattern.compile("[Ss]iehe (.*)").matcher(comment);
                if (m.find())
                    if (m.group(1).contains("heute")) {
                        comment =  GGApp.GG_APP.getResources().getString(R.string.shifted_to_today) + " " + m.group(1).replaceAll("heute,? ", "");
                    } else {
                        comment =  GGApp.GG_APP.getResources().getString(R.string.shifted_to) + " " + m.group(1);
                    }
            } else if(commentl.contains("f.a.") || commentl.contains("fällt aus") || commentl.contains("entfall")) {
                type =  GGApp.GG_APP.getResources().getString(R.string.elemination);

                if(aufgm.find())
                    comment =  GGApp.GG_APP.getResources().getString(R.string.task_through) + " " + aufgm.group(1);
                else
                    comment = "";
            } else if(commentl.contains("klausur")) {
                type =  GGApp.GG_APP.getResources().getString(R.string.exam);
                comment = "";
            } else if(commentl.contains("unterricht findet statt") || commentl.contains("absenz")) {
                type =  GGApp.GG_APP.getResources().getString(R.string.lesson);
            } else if(commentl.contains("statt")) {
                type =  GGApp.GG_APP.getResources().getString(R.string.substitute) + " / " +  GGApp.GG_APP.getResources().getString(R.string.shift);

                Matcher m1 = Pattern.compile("[Ss]tatt (.*?)Std.").matcher(comment);
                Matcher m2 = Pattern.compile(":(.*)").matcher(comment);

                if(m1.find())
                    comment =  GGApp.GG_APP.getResources().getString(R.string.instead_of) + " " + m1.group(1).replaceAll("\\w+ \\(heute\\)", "").replaceAll("heute,? ", "") + " " +  GGApp.GG_APP.getResources().getString(R.string.lhour);

                if(m2.find())
                    repsub = m2.group(1).trim();
            } else if(commentl.contains("betreuung")) {
                type =  GGApp.GG_APP.getResources().getString(R.string.supervision);
                comment = "";
            } else {
                type =  GGApp.GG_APP.getResources().getString(R.string.substitute);

                if(aufgm.find())
                    comment =  GGApp.GG_APP.getResources().getString(R.string.task_through) + " " + aufgm.group(1);

                if(commentl.contains("mittag"))
                    comment =  GGApp.GG_APP.getResources().getString(R.string.lunch);
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

}
