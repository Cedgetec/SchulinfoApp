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
    int icon;

    public FragmentData(FragmentType type, String params) {
        this.type = type;
        this.params = params;

        switch(type) {
            case PLAN:
                icon = R.drawable.ic_substitution;
                break;
            case EXAMS:
                icon = R.drawable.ic_exam;
                break;
            case MENSA:
                icon = R.drawable.ic_mensa;
                break;
            case NEWS:
                icon = R.drawable.ic_news;
                break;
            default:
                icon = 0;
                break;
        }

        int namer = 0;
        switch(type) {
            case PLAN:
                namer = R.string.substitute_schedule;
                break;
            case EXAMS:
                namer = R.string.exams;
                break;
            case MENSA:
                namer = R.string.cafeteria;
                break;
            case NEWS:
                namer = R.string.news;
                break;
            default:
                break;
        }

        if(namer != 0) {
            this.name = SIAApp.GG_APP.getString(namer);
        } else {
            this.name = "unknown";
        }
    }

    public void setName(String name) {
        this.name = name;
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
        return icon;
    }

    public void setIconRes(int res) {
        this.icon = res;
    }

    @Override
    public boolean equals(Object o) {
        if(o instanceof FragmentData) {
            FragmentData f = (FragmentData) o;
            return f.type.equals(this.type) && (this.data == null ? f.data == null : this.data.equals(f.data));
        } else {
            return false;
        }
    }

    public static class FragmentList extends ArrayList<FragmentData> {

        public List<FragmentData> getByType(FragmentType type) {
            ArrayList<FragmentData> list = new ArrayList<>();
            for(FragmentData data : this) {
                if (data.type == type)
                    list.add(data);
            }

            return list;
        }

        public List<FragmentData> getByType(FragmentType type, String params) {
            ArrayList<FragmentData> list = new ArrayList<>();
            for(FragmentData fr : this) {
                if (fr.type == type && fr.params.equals(params))
                    list.add(fr);
            }

            return list;
        }

    }
}
