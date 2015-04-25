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

package de.gebatzens.ggvertretungsplan.fragment;

import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.CardView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import de.gebatzens.ggvertretungsplan.GGApp;
import de.gebatzens.ggvertretungsplan.R;
import de.gebatzens.ggvertretungsplan.data.Exams;
import de.gebatzens.ggvertretungsplan.data.GGPlan;

public class ExamFragment extends RemoteDataFragment {

    SwipeRefreshLayout swipeContainer;
    int cardColorIndex = 0;

    public ExamFragment() {
        type = GGApp.FragmentType.EXAMS;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup vg, Bundle b) {
        ViewGroup v =  (ViewGroup) inflater.inflate(R.layout.fragment_exam, vg, false);
        if(GGApp.GG_APP.exams != null)
            createRootView(inflater, v);
        return v;
    }

    @Override
    public void onViewCreated(View v, Bundle b) {
        super.onViewCreated(v, b);

        swipeContainer = (SwipeRefreshLayout) v.findViewById(R.id.refresh);
        // Setup refresh listener which triggers new data loading
        swipeContainer.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                GGApp.GG_APP.refreshAsync(new Runnable() {
                    @Override
                    public void run() {
                        swipeContainer.post(new Runnable() {
                            @Override
                            public void run() {
                                swipeContainer.setRefreshing(false);
                            }
                        });

                    }
                }, true, GGApp.FragmentType.EXAMS);
            }
        });
        // Configure the refreshing colors
        swipeContainer.setColorSchemeResources(R.color.custom_material_green,
                R.color.custom_material_red,
                R.color.custom_material_blue,
                R.color.custom_material_orange);

    }

    @Override
    public void createView(LayoutInflater inflater, ViewGroup view) {
        ScrollView sv = new ScrollView(getActivity());
        sv.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        sv.setTag("gg_scroll");
        LinearLayout l = new LinearLayout(getActivity());
        l.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        l.setOrientation(LinearLayout.VERTICAL);
        if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
            l.setPadding(toPixels(55),toPixels(4),toPixels(55),toPixels(4));
        }
        else if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT){
            l.setPadding(toPixels(4),toPixels(4),toPixels(4),toPixels(4));
        }
        sv.addView(l);
        Exams filtered = GGApp.GG_APP.exams.filter(GGApp.GG_APP.filters);
        if(!filtered.isEmpty()) {
            createTextView(getResources().getString(R.string.my_exams), 30, inflater, l);
            for (Exams.ExamItem item : filtered) {
                CardView cv = createCardItem(item, inflater);
                if (cv != null) {
                    l.addView(cv);
                }
            }
        }

        cardColorIndex = 0;

        if(GGApp.GG_APP.exams.size() != 0) {
            createTextView(getResources().getString(R.string.all_exams), 30, inflater, l);
            for (Exams.ExamItem item : GGApp.GG_APP.exams) {
                CardView cv = createCardItem(item, inflater);
                if (cv != null) {
                    l.addView(cv);
                }
            }
            ((LinearLayout) view.findViewById(R.id.exam_content)).addView(sv);
        } else {
            createText(view, getString(R.string.no_entries));
        }
        cardColorIndex = 0;
    }

    @Override
    public ViewGroup getContentView() {
        return (ViewGroup) getView().findViewById(R.id.exam_content);
    }

    private CardView createCardItem(Exams.ExamItem exam_item, LayoutInflater i) {
        CardView ecv = createCardView();
        String[] colors = getActivity().getResources().getStringArray(R.array.orangeColors);
        ecv.setCardBackgroundColor(Color.parseColor(colors[cardColorIndex]));
        cardColorIndex++;
        if(cardColorIndex == colors.length)
            cardColorIndex = 0;
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, toPixels(6));
        ecv.setLayoutParams(params);
        i.inflate(R.layout.exam_cardview_entry, ecv, true);
        Date d = new Date();
        Calendar c = Calendar.getInstance();
        c.setTime(d);
        c.add(Calendar.DAY_OF_YEAR, -1);
        Date dt = c.getTime();
        if(exam_item.date.before(dt)) {
            //ecv.setAlpha(0.35f);
            return null;
        }
        String lesson = exam_item.lesson;
        if(Integer.parseInt(exam_item.length) > 1)
            lesson += ". - " + (Integer.parseInt(exam_item.lesson) + Integer.parseInt(exam_item.length) - 1) + ".";
        ((TextView) ecv.findViewById(R.id.ecv_date)).setText(getFormattedDate(exam_item.date));
        ((TextView) ecv.findViewById(R.id.ecv_lesson)).setText(getDay(exam_item.date));
        ((TextView) ecv.findViewById(R.id.ecv_subject_teacher)).setText(GGPlan.Entry.translateSubject(exam_item.subject) + " [" + exam_item.teacher + "]");
        ((TextView) ecv.findViewById(R.id.ecv_schoolclass)).setText(exam_item.clazz + "\n" + getString(R.string.lessons) + " " + lesson);
        return ecv;
    }

    private String getFormattedDate(Date date) {
        DateFormat dateFormatter;
        if(Locale.getDefault().getLanguage().equals("de")) {
            dateFormatter = new SimpleDateFormat("d. MMM");
        } else if(Locale.getDefault().getLanguage().equals("en")) {
            dateFormatter = new SimpleDateFormat("MM/dd/yyyy");
        } else {
            dateFormatter = new SimpleDateFormat("yyyy-MM-dd");
        }

        return dateFormatter.format(date);
    }

    private String getDay(Date date) {
        try {
            return new SimpleDateFormat("EE").format(date);
        } catch(Exception e) {
            e.printStackTrace();
        }
        return "Bug";
    }
}
