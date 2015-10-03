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

import java.util.ArrayList;

import de.gebatzens.sia.GGApp;
import de.gebatzens.sia.R;

public class Filter {

    public FilterType type = FilterType.CLASS;
    public String filter = "";

    public boolean matches(GGPlan.Entry e) {
        if(filter.isEmpty())
            return false;
        switch(type) {
            case CLASS:
                return e.clazz.toLowerCase().replace(" ", "").equals(filter.toLowerCase().replace(" ", ""));
            case TEACHER:
                return e.teacher.toLowerCase().equals(filter.toLowerCase()) || e.comment.toLowerCase().endsWith(filter.toLowerCase());
            case SUBJECT:
                return e.subject.toLowerCase().replace(" ", "").equals(filter.toLowerCase().replace(" ", ""));
            case LESSON:
                return e.lesson.toLowerCase().equals(filter.toLowerCase());
        }
        return false;
    }

    public boolean matches(Exams.ExamItem item) {
        switch(type) {
            case CLASS:
                String[] classes = item.clazz.split(",");
                for (String s : classes) {
                    if (filter.toLowerCase().contains(s.toLowerCase()))
                        return true;
                }
                return false;
            case TEACHER:
                return item.teacher.equalsIgnoreCase(filter);
            case SUBJECT:
                return item.subject.equalsIgnoreCase(filter);
            case LESSON:
                return item.lesson.equalsIgnoreCase(filter);
        }
        return false;
    }

    public static String getTypeString(FilterType type) {
        String s;
        switch(type) {
            case CLASS:
                s = GGApp.GG_APP.getString(R.string.school_class);
                break;
            case TEACHER:
                s = GGApp.GG_APP.getString(R.string.teacher);
                break;
            case SUBJECT:
                s = GGApp.GG_APP.getString(R.string.subject_course_name);
                break;
            case LESSON:
                s = GGApp.GG_APP.getString(R.string.lhour);
                break;
            default:
                s = "";
        }
        return s;
    }

    @Override
    public String toString() {
        return toString(true);
    }

    public String toString(boolean st) {
        return (st ? (getTypeString(type) + " ") : "") + filter;
    }

    public enum FilterType {
        CLASS, TEACHER, SUBJECT, LESSON
    }

    public static class FilterList extends ArrayList<Filter> {
        public Filter mainFilter;
    }
}