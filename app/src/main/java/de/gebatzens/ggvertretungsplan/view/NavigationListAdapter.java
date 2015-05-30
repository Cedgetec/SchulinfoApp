/*
 * Copyright 2015 Fabian Schultis
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

package de.gebatzens.ggvertretungsplan.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import de.gebatzens.ggvertretungsplan.R;

public class NavigationListAdapter extends BaseAdapter {
    private Context context;
    private String[] mTitle;
    private int[] mIcon;
    private LayoutInflater inflater;
    public int mSelected = -1;
    public int mColor = Color.GRAY;
 
    public NavigationListAdapter(Context pContext, String[] pTitle, int[] pIcon) {
        context = pContext;
        mTitle = pTitle;
        mIcon = pIcon;
    }

    public boolean themeIsLight() {
        TypedValue a = new TypedValue();
        context.getTheme().resolveAttribute(android.R.attr.windowBackground, a, true);
        int color = a.data;
        if (color == context.getResources().getColor(R.color.background_material_light)) {
            return true;
        } else{
            return false;
        }
    }
 
    @SuppressLint("ViewHolder")
	public View getView(int position, View convertView, ViewGroup parent) {
        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View itemView = inflater.inflate(R.layout.navigation_item, parent, false);
        TextView txtTitle = (TextView) itemView.findViewById(R.id.menuTitle);
        ArrayAdapter a;
        ImageView imgIcon = (ImageView) itemView.findViewById(R.id.menuIcon);
        LinearLayout navigationitem = (LinearLayout) itemView.findViewById(R.id.navigationButton);
        txtTitle.setText(mTitle[position]);
        imgIcon.setImageResource(mIcon[position]);
        if (themeIsLight()) {
            txtTitle.setTextColor(Color.parseColor("#212121"));
            imgIcon.setColorFilter(Color.parseColor("#727272"));
        } else{
            imgIcon.setColorFilter(Color.parseColor("#aeaeae"));
            txtTitle.setTextColor(Color.parseColor("#e7e7e7"));
        }
        if(mSelected==position) {
            if (themeIsLight()) {
                navigationitem.setBackgroundColor(Color.parseColor("#eeeeee"));
            } else{
                navigationitem.setBackgroundColor(Color.parseColor("#5a5a5a"));
            }
            txtTitle.setTextColor(mColor);
            imgIcon.setColorFilter(mColor);
        }
        return itemView;
    }

    @Override
    public int getCount() {
        return mTitle.length;
    }

    @Override
    public Object getItem(int position) {
        return mTitle[position];
    }
 
    @Override
    public long getItemId(int position) {
        return position;
    }
}