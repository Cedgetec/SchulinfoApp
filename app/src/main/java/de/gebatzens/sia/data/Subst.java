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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.gebatzens.sia.SIAApp;
import de.gebatzens.sia.R;
import de.gebatzens.sia.fragment.RemoteDataFragment;

public class Subst extends ArrayList<Subst.Entry> {

    public Date date;
    public List<String> special = new ArrayList<>();
    private List<String> classes, lessons;

    public Subst() {

    }

    public String getWeekday() {
        return new SimpleDateFormat("EEEE").format(date);
    }

    public static class GGPlans extends ArrayList<Subst> implements RemoteDataFragment.RemoteData {

        public Throwable throwable;
        public Date loadDate;

        /**
         * true, if this data was loaded from storage rather than downloaded
         */
        public boolean isLocal;

        @Override
        public Throwable getThrowable() {
            return throwable;
        }

        @Override
        public void save() {
            SIAApp.SIA_APP.preferences.edit().putLong("substLoadDate", loadDate.getTime()).commit();

            String[] list = SIAApp.SIA_APP.fileList();

            for(String s : list) {
                if (s.startsWith("schedule")) {
                    SIAApp.SIA_APP.deleteFile(s);
                }
            }

            for(int i = 0; i < size(); i++)
                get(i).save("schedule" + i);
        }

        @Override
        public boolean load() {
            isLocal = true;
            loadDate = new Date(SIAApp.SIA_APP.preferences.getLong("substLoadDate", new Date().getTime()));
            clear();

            try {
                for(int i = 0; ; i++) {
                    Subst plan = new Subst();
                    plan.load("schedule" + i);
                    add(plan);
                }

            } catch(FileNotFoundException e) {
                if(size() == 0)
                    Log.w("ggvp", "Subst file does not exist");
            } catch(Exception e) {
                e.printStackTrace();
            }

            return size() > 0;
        }

        public Subst getPlanByDate(Date d) {
            for(Subst plan : this)
                if(plan.date.equals(d))
                    return plan;
            return null;
        }

        public boolean shouldRecreateView(GGPlans newPlans) {
            if(size() != newPlans.size())
                return true;

            for (int i = 0; i < newPlans.size(); i++) {
                if (!newPlans.get(i).date.equals(this.get(i).date) ||
                        newPlans.get(i).size() != this.get(i).size() || newPlans.get(i).special.size() != this.get(i).special.size())
                    return true;
            }

            return false;
        }

        public List<String> getAllClasses() {
            Set<String> cl = new HashSet<>();
            for(Subst plan : this) {
                cl.addAll(plan.getAllClasses());
            }

            ArrayList<String> list = new ArrayList<>();
            list.addAll(cl);
            return list;
        }
    }

    public void load(String file) throws Exception {
        clear();
        special.clear();

        InputStream in = SIAApp.SIA_APP.openFileInput(file);
        JsonReader reader = new JsonReader(new InputStreamReader(in));
        reader.beginObject();
        while(reader.hasNext()) {
            String name = reader.nextName();
            switch (name) {
                case "date":
                    date = new Date(reader.nextLong());
                    break;
                case "messages":
                    reader.beginArray();
                    while (reader.hasNext()) {
                        special.add(reader.nextString());
                    }
                    reader.endArray();
                    break;
                case "entries":
                    reader.beginArray();
                    while (reader.hasNext()) {
                        reader.beginObject();
                        Entry e = new Entry();
                        e.date = date;
                        while (reader.hasNext()) {
                            String name2 = reader.nextName();
                            switch (name2) {
                                case "class":
                                    e.clazz = reader.nextString();
                                    break;
                                case "lesson":
                                    e.lesson = reader.nextString();
                                    break;
                                case "teacher":
                                    e.teacher = reader.nextString();
                                    break;
                                case "subject":
                                    e.subject = reader.nextString();
                                    break;
                                case "comment":
                                    e.comment = reader.nextString();
                                    break;
                                case "type":
                                    e.type = reader.nextString();
                                    break;
                                case "room":
                                    e.room = reader.nextString();
                                    break;
                                case "repsub":
                                    e.repsub = reader.nextString();
                                    break;
                                default:
                                    reader.skipValue();
                                    break;
                            }

                        }

                        add(e);
                        reader.endObject();
                    }
                    reader.endArray();
                    break;
                default:
                    reader.skipValue();
                    break;
            }
        }
        reader.endObject();
        reader.close();
    }

    public void save(String file) {
        Log.w("ggvp", "Saving " + file);
        try {
            OutputStream out = SIAApp.SIA_APP.openFileOutput(file, Context.MODE_PRIVATE);
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
            for(Entry e : this) {
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
        if(classes == null) {
            classes = new ArrayList<>();

            for (Entry e : this) {
                if (!classes.contains(e.clazz)) {
                    classes.add(e.clazz);
                }
            }
        }

        return classes;
    }

    public List<String> getAllLessons() {
        if(lessons == null) {
            lessons = new ArrayList<>();

            for (Entry e : this) {
                if (!lessons.contains(e.lesson)) {
                    lessons.add(e.lesson);

                }
            }

            Collections.sort(lessons, new Comparator<String>() {
                @Override
                public int compare(String lhs, String rhs) {
                    try {
                        int l1 = Integer.parseInt(lhs);
                        int l2 = Integer.parseInt(rhs);

                        return l1 - l2;
                    } catch (Exception e) {
                        Log.d("ggvp", "Lesson parsing failed " + e.getMessage());
                    }

                    return lhs.compareTo(rhs);
                }
            });
        }

        return lessons;
    }

    @Override
    public boolean equals(Object o) {
        if(o instanceof Subst) {
            Subst plan = (Subst) o;
            return super.equals(plan) && plan.date.equals(date) && plan.special.equals(special);
        } else
            return false;

    }

    public Subst filter(Filter.FilterList flist) {
        Subst list = new Subst();
        list.date = date;

        for(Entry e : this) {
            if(flist.matches(e))
                list.add(e);
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

    public static class Entry implements Filter.Filterable, Shareable {

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
        public boolean markedForSharing = false;

        private String classAN;
        private String teacherAN;
        private String subjectAN;
        private String lessonAN;
        private String commentAN;

        public String getClassAN() {
            if(classAN == null)
                classAN = SIAApp.deleteNonAlphanumeric(clazz);
            return classAN;
        }

        public String getTeacherAN() {
            if(teacherAN == null)
                teacherAN = SIAApp.deleteNonAlphanumeric(teacher);
            return teacherAN;
        }

        public String getSubjectAN() {
            if(subjectAN == null)
                subjectAN = SIAApp.deleteNonAlphanumeric(subject);
            return subjectAN;
        }

        public String getLessonAN() {
            if(lessonAN == null)
                lessonAN = SIAApp.deleteNonAlphanumeric(lesson);
            return lessonAN;
        }

        public String getCommentAN() {
            if(commentAN == null)
                commentAN = SIAApp.deleteNonAlphanumeric(comment);
            return commentAN;
        }

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

        @Override
        public void setMarked(boolean m) {
            markedForSharing = m;
        }

        @Override
        public boolean isMarked() {
            return markedForSharing;
        }

        @Override
        public String getShareContent() {
            int id;

            if(teacher.equals("")) {
                if(room.equals("")) {
                    return SIAApp.SIA_APP.getResources().getString(R.string.share_content_base, this.type, this.clazz, this.lesson);
                } else {
                    return SIAApp.SIA_APP.getResources().getString(R.string.share_content_room, this.type, this.clazz, this.lesson, this.room);
                }
            } else {
                if(room.equals("")) {
                    return SIAApp.SIA_APP.getResources().getString(R.string.share_content_teacher, this.type, this.clazz, this.teacher, this.lesson);
                } else {
                    return SIAApp.SIA_APP.getResources().getString(R.string.share_content_teacher_room, this.type, this.clazz, this.teacher, this.lesson, this.room);
                }
            }
        }

        @Override
        public Date getDate() {
            return date;
        }


        public void unify() {

            Matcher task = Pattern.compile("task (.*)").matcher(comment);

            switch (type) {
                case "entf":
                    type = SIAApp.SIA_APP.getString(R.string.canceled);

                    if (task.find())
                        comment = SIAApp.SIA_APP.getString(R.string.task_through) + " " + task.group(1);
                    break;
                case "eva":
                    type = SIAApp.SIA_APP.getString(R.string.work_autonomous);

                    if (task.find())
                        comment = SIAApp.SIA_APP.getString(R.string.task_through) + " " + task.group(1);
                    break;
                case "teacher":
                    type = SIAApp.SIA_APP.getString(R.string.substitute);

                    if (task.find())
                        comment = SIAApp.SIA_APP.getString(R.string.task_through) + " " + task.group(1);
                    break;
                case "exam":
                    type = SIAApp.SIA_APP.getString(R.string.exam);
                    break;
                case "lesson":
                    type = SIAApp.SIA_APP.getString(R.string.lesson);
                    break;
                case "shifted": {
                    type = SIAApp.SIA_APP.getString(R.string.canceled) + " / " + SIAApp.SIA_APP.getResources().getString(R.string.shift);

                    Matcher m = Pattern.compile("shift (\\S*) (\\S*)").matcher(comment);

                    if (m.find()) {
                        String sdates = m.group(1);
                        Date sdate = sdates.contains("today") ? date : parseDate(sdates);
                        String lesson = m.group(2);

                        if (sdate == null)
                            comment = "";
                        else if (sdate.equals(date))
                            comment = SIAApp.SIA_APP.getString(R.string.shifted_to_today) + " " + lesson + ". " + SIAApp.SIA_APP.getString(R.string.lhour);
                        else
                            comment = SIAApp.SIA_APP.getString(R.string.shifted_to) + " " + new SimpleDateFormat("EEE").format(sdate) + ", " +
                                    DateFormat.getDateInstance().format(sdate) + " " + lesson + ". " + SIAApp.SIA_APP.getString(R.string.lhour);
                    }
                    break;
                }
                case "instead": {
                    type = SIAApp.SIA_APP.getString(R.string.substitute) + " / " + SIAApp.SIA_APP.getResources().getString(R.string.shift);

                    Matcher m = Pattern.compile("instead (\\S*) (\\S*)").matcher(comment);

                    if (m.find()) {
                        String idates = m.group(1);
                        Date idate = idates.contains("today") ? date : parseDate(idates);
                        String lesson = m.group(2);

                        if (idate == null)
                            comment = "";
                        else if (idate.equals(date))
                            comment = SIAApp.SIA_APP.getString(R.string.instead_of) + " " + lesson + ". " + SIAApp.SIA_APP.getString(R.string.lhour);
                        else
                            comment = SIAApp.SIA_APP.getString(R.string.instead_of) + " " + new SimpleDateFormat("EEE").format(idate) + ", " +
                                    DateFormat.getDateInstance().format(idate) + " " + lesson + ". " + SIAApp.SIA_APP.getString(R.string.lhour);
                    }

                    break;
                }
                case "supervision":
                    type = SIAApp.SIA_APP.getString(R.string.supervision);
                    break;
                case "lunch":
                    type = SIAApp.SIA_APP.getString(R.string.lunch);
                    break;
                case "subst":
                    type = SIAApp.SIA_APP.getString(R.string.substitute);

                    if (task.find())
                        comment = SIAApp.SIA_APP.getString(R.string.task_through) + " " + task.group(1);
                    break;
            }

            if(subject.isEmpty() && !repsub.isEmpty())
                subject = "&#x2192; " + repsub;
            else if(!subject.isEmpty() && !repsub.isEmpty() && !subject.equals(repsub))
                subject += " &#x2192; " + repsub;

            comment = comment.replace("  ", " ");

        }

        public static String translateSubject(String s) {
            String t = SIAApp.SIA_APP.subjects.get(s.toLowerCase());
            if(t == null)
                return s;
            else
                return t;
        }

        @Override
        public int compareTo(Shareable shareable) {
            if(shareable instanceof Entry) {
                return lesson.compareTo(((Entry) shareable).lesson);
            }

            return 0;
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
