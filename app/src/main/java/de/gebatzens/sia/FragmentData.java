/*
 * Copyright 2016 Hauke Oldsen
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
package de.gebatzens.sia;

import java.util.ArrayList;
import java.util.List;

import de.gebatzens.sia.fragment.RemoteDataFragment;

public class FragmentData {

    public enum FragmentType {
        PLAN, NEWS, MENSA, EXAMS, PDF
    }

    FragmentType type;
    String params;
    String name;
    RemoteDataFragment.RemoteData data;

    public FragmentData(FragmentType type, String params, String name) {
        this.type = type;
        this.params = params;
        this.name = name;
    }

    public FragmentData(FragmentType type, String data) {
        this(type, data, null);
        int name = 0;
        switch(type) {
            case PLAN:
                name = R.string.substitute_schedule;
                break;
            case EXAMS:
                name = R.string.exams;
                break;
            case MENSA:
                name = R.string.cafeteria;
                break;
            case NEWS:
                name = R.string.news;
                break;
            default:
                break;
        }

        this.name = GGApp.GG_APP.getString(name);

    }

    public RemoteDataFragment.RemoteData getData() {
        return data;
    }

    public void setData(RemoteDataFragment.RemoteData data) {
        this.data = data;
    }

    public FragmentType getType() {
        return type;
    }

    public String getParams() {
        return params;
    }

    public int getIconRes() {
        switch(type) {
            case PLAN:
                return R.drawable.ic_substitution;
            case EXAMS:
                return R.drawable.ic_exam;
            case MENSA:
                return R.drawable.ic_mensa;
            case NEWS:
                return R.drawable.ic_news;
            default:
                return 0;
        }
    }

    @Override
    public boolean equals(Object o) {
        if(o instanceof FragmentData) {
            FragmentData f = (FragmentData) o;
            return f.type.equals(this.type) && this.data.equals(f.data);
        } else {
            return false;
        }
    }

    public static class FragmentList extends ArrayList<FragmentData> {

        public List<FragmentData> getData(FragmentType type) {
            ArrayList<FragmentData> list = new ArrayList<>();
            for(FragmentData data : this) {
                if (data.type == type)
                    list.add(data);
            }

            return list;
        }

        public List<FragmentData> getData(FragmentType type, String params) {
            ArrayList<FragmentData> list = new ArrayList<>();
            for(FragmentData fr : this) {
                if (fr.type == type && fr.params.equals(params))
                    list.add(fr);
            }

            return list;
        }

    }
}
