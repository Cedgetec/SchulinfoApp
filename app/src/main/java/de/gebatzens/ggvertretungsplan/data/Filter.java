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

import java.util.ArrayList;

import de.gebatzens.ggvertretungsplan.GGApp;
import de.gebatzens.ggvertretungsplan.R;

public class Filter {
    public FilterType type;
    public String filter;

    public boolean matches(GGPlan.Entry e) {
        if(filter.isEmpty())
            return false;
        switch(type) {
            case CLASS:
                return e.clazz.toLowerCase().equals(filter.toLowerCase());
            case TEACHER:
                return e.subst.toLowerCase().equals(filter.toLowerCase()) || e.comment.toLowerCase().endsWith(filter.toLowerCase());
            case SUBJECT:
                return e.subject.toLowerCase().replace(" ", "").equals(filter.toLowerCase().replace(" ", ""));
        }
        return false;
    }

    public boolean matches(Exams.ExamItem item) {
        switch(type) {
            case CLASS:
                return item.schoolclass.toLowerCase().contains(filter.toLowerCase());
            case TEACHER:
                return item.teacher.toLowerCase().contains(filter.toLowerCase());
            case SUBJECT:
                return item.subject.toLowerCase().equals(filter.toLowerCase());
        }
        return false;
    }

    public static String getTypeString(FilterType type) {
        String s;
        switch(type) {
            case CLASS:
                s = GGApp.GG_APP.getString(R.string.schoolclass);
                break;
            case TEACHER:
                s = GGApp.GG_APP.getString(R.string.teacher);
                break;
            case SUBJECT:
                s = GGApp.GG_APP.getString(R.string.subject_course);
                break;
            default:
                s = "";
        }
        return s;
    }

    public static FilterType getTypeFromString(String s) {
        if(s.equals(GGApp.GG_APP.getString(R.string.teacher)))
            return FilterType.TEACHER;
        else if(s.equals(GGApp.GG_APP.getString(R.string.schoolclass)))
            return FilterType.CLASS;
        else if(s.equals(GGApp.GG_APP.getString(R.string.subject_course)))
            return FilterType.SUBJECT;
        else
            return null;
    }

    @Override
    public String toString() {
        return toString(true);
    }

    public String toString(boolean st) {
        return (st ? (getTypeString(type) + " ") : "") + filter;
    }

    public static enum FilterType {
        CLASS, TEACHER, SUBJECT
    }

    public static class FilterList extends ArrayList<Filter> {
        public Filter mainFilter;
    }
}