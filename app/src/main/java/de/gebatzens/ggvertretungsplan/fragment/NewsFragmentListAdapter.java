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

package de.gebatzens.ggvertretungsplan.fragment;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import de.gebatzens.ggvertretungsplan.GGApp;
import de.gebatzens.ggvertretungsplan.NewsFragmentDatabaseHelper;
import de.gebatzens.ggvertretungsplan.R;
import de.gebatzens.ggvertretungsplan.data.News;

public class NewsFragmentListAdapter extends BaseAdapter {
    private Context context;
    private News mArrayList;
    private LayoutInflater inflater;
    private String formattedDate;
    private NewsFragmentDatabaseHelper mDatabaseHelper;

    /*public NewsFragmentListAdapter(Context pContext, String[] pTitle, String[] pContent, int[] pIcon) {*/
    public NewsFragmentListAdapter(Context pContext, News pArrayList) {
        context = pContext;
        mArrayList = pArrayList;
        mDatabaseHelper = new NewsFragmentDatabaseHelper(context);
    }
 
    @SuppressLint("ViewHolder")
	public View getView(int position, View convertView, ViewGroup parent) {
        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View itemView = inflater.inflate(R.layout.news_fragment_list_item, parent, false);
        TextView txtDate = (TextView) itemView.findViewById(R.id.newsDate);
        TextView txtTitle = (TextView) itemView.findViewById(R.id.newsTitle);
        TextView txtContent = (TextView) itemView.findViewById(R.id.newsContent);
        //ImageView imgIcon = (ImageView) itemView.findViewById(R.id.newsIcon);

        DateFormat parser = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        DateFormat dateFormatter = new SimpleDateFormat("d. MMM yy");
        try
        {
            String startDate = mArrayList.get(position).date;
            Date parsedDate = parser.parse(startDate);
            formattedDate = dateFormatter.format(parsedDate);
        }
        catch (ParseException e)
        {
            e.printStackTrace();
        }

        txtDate.setText(formattedDate);
        txtDate.setTextColor(GGApp.GG_APP.school.color);
        txtTitle.setText(mArrayList.get(position).title);
        txtContent.setText(Html.fromHtml(mArrayList.get(position).text));
        //imgIcon.setImageResource(R.drawable.news_icon_white);
       // imgIcon.setBackgroundResource(R.drawable.news_img_background);
        //GradientDrawable drawable = (GradientDrawable) imgIcon.getBackground();
        //drawable.setColor(GGApp.GG_APP.provider.getColor());
        //imgIcon.setImageResource(mIcnewson[position]);

        if(mDatabaseHelper.checkNewsRead(mArrayList.get(position).title)) {
            txtDate.setTextColor(Color.parseColor("#727272"));
            txtDate.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
            txtTitle.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
            txtContent.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
        }

        return itemView;
    }

    @Override
    public int getCount() {
        return mArrayList.size();
    }

    @Override
    public Object getItem(int position) {
        return mArrayList.get(position).title;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }
}