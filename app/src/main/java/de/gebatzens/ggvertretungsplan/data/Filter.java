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