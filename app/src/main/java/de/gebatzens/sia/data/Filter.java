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

    private FilterType type = FilterType.CLASS;
    private String filter = "";
    private String filterAN = "";
    public boolean contains = false;


    public void setType(FilterType t) {
        type = t;
    }

    public void setFilter(String filter) {
        this.type = type;
        this.filter = filter;
        if(filter != null && !filter.isEmpty())
            this.filterAN = GGApp.deleteNonAlphanumeric(filter);
        else
            this.filterAN = filter;
    }

    public FilterType getType() {
        return type;
    }

    public String getFilter() {
        return filter;
    }


    public boolean matches(GGPlan.Entry e) {
        if(filter.isEmpty())
            return false;

        switch(type) {
            case CLASS:
                return e.getClassAN().equals(filterAN);
            case TEACHER:
                return e.getTeacherAN().equals(filterAN) || e.getCommentAN().endsWith(filterAN);
            case SUBJECT:
                if (contains)
                    return e.getSubjectAN().contains(filterAN);
                else
                    return e.getSubjectAN().equals(filterAN);
            case LESSON:
                return e.getLessonAN().equals(filterAN);
        }
        return false;
    }

    public boolean matches(Exams.ExamItem item) {
        if(filter.isEmpty())
            return false;

        switch(type) {
            case CLASS:
                String[] classes = item.clazz.split(",");
                for (String s : classes) {
                    if (filterAN.equals(GGApp.deleteNonAlphanumeric(s)))
                        return true;
                }
                return false;
            case TEACHER:
                return GGApp.deleteNonAlphanumeric(item.teacher).equals(filterAN);
            case SUBJECT:
                if (contains)
                    return GGApp.deleteNonAlphanumeric(item.subject).contains(filterAN);
                else
                    return GGApp.deleteNonAlphanumeric(item.subject).equals(filterAN);
            case LESSON:
                return GGApp.deleteNonAlphanumeric(item.lesson).equals(filterAN);
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