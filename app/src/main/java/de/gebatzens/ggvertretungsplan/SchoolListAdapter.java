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

package de.gebatzens.ggvertretungsplan;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.media.Image;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

public class SchoolListAdapter extends BaseAdapter {

    List<School> list = School.LIST;
    Context context;

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
        View v = convertView == null ? ((LayoutInflater) GGApp.GG_APP.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.school_item, parent, false) : convertView;

        ((TextView) v.findViewById(R.id.school_name)).setText(list.get(position).name);

        ((TextView) v.findViewById(R.id.school_city)).setText(list.get(position).city);

        GradientDrawable gd = (GradientDrawable) parent.getResources().getDrawable(R.drawable.colored_circle);
        gd.setColor(list.get(position).getColor());
        ((ImageView) v.findViewById(R.id.school_firstletter_image)).setImageDrawable(gd);

        ((TextView) v.findViewById(R.id.school_firstletter_text)).setText(list.get(position).name.substring(0,1));

        ((TextView) v.findViewById(R.id.school_user_number)).setText(parent.getResources().getString(R.string.user) + ": 36");

        return v;
    }
}
