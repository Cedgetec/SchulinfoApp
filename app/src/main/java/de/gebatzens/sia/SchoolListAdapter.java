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

package de.gebatzens.sia;

import android.graphics.drawable.GradientDrawable;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import java.util.List;

public class SchoolListAdapter extends BaseAdapter {

    List<School> list;
    SetupActivity c;

    public SchoolListAdapter(SetupActivity c, List<School> list) {
        this.c = c;
        this.list = list;
    }

    @Override
    public int getCount() {
        return list.size();
    }

    @Override
    public Object getItem(int position) {
        return list.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View v = convertView == null ? c.getLayoutInflater().inflate(R.layout.school_item, parent, false) : convertView;

        School school = list.get(position);

        ((TextView) v.findViewById(R.id.school_name)).setText(school.name);

        ((TextView) v.findViewById(R.id.school_city)).setText(school.city);

        ((GradientDrawable) v.findViewById(R.id.school_firstletter_text).getBackground()).setColor(school.getColor());

        ((TextView) v.findViewById(R.id.school_firstletter_text)).setText("" + school.name.charAt(0));

        ((TextView) v.findViewById(R.id.school_user_number)).setText(school.users + " " + parent.getResources().getString(R.string.user));

        return v;
    }
}
