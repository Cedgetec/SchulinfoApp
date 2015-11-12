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
package de.gebatzens.sia.fragment;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.CardView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.Patterns;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Pattern;

import de.gebatzens.sia.GGApp;
import de.gebatzens.sia.MainActivity;
import de.gebatzens.sia.R;
import de.gebatzens.sia.data.Exams;
import de.gebatzens.sia.data.Filter;

public class ExamFragment extends RemoteDataFragment {

    SwipeRefreshLayout swipeContainer;
    int cardColorIndex = 0;

    public ExamFragment() {
        type = GGApp.FragmentType.EXAMS;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup vg, Bundle b) {
        ViewGroup v = (ViewGroup) inflater.inflate(R.layout.fragment_exam, vg, false);
        ((MainActivity) getActivity()).updateMenu(R.menu.toolbar_exam_menu);
        if(GGApp.GG_APP.exams != null)
            createRootView(inflater, v);
        return v;
    }

    @Override
    public void onViewCreated(View v, Bundle b) {
        super.onViewCreated(v, b);

        swipeContainer = (SwipeRefreshLayout) v.findViewById(R.id.refresh);
        if (GGApp.GG_APP.isDarkThemeEnabled()) {
            swipeContainer.setProgressBackgroundColorSchemeColor(Color.parseColor("#424242"));
        } else{
            swipeContainer.setProgressBackgroundColorSchemeColor(Color.parseColor("#ffffff"));
        }
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

    private void createContentList(List<Exams.ExamItem> list, String title, ViewGroup l, LayoutInflater inflater, boolean checkbox) {
        TextView tv = createTextView(title, 27, inflater, l);
        tv.setPadding(toPixels(2.8f), 0, 0, 0);
        if (GGApp.GG_APP.isDarkThemeEnabled()) {
            tv.setTextColor(Color.parseColor("#a0a0a0"));
        } else {
            tv.setTextColor(Color.parseColor("#6e6e6e"));
        }
        for (Exams.ExamItem item : list) {
            CardView cv = createCardItem(item, inflater, checkbox);
            l.addView(cv);
        }
    }

    @Override
    public void createView(final LayoutInflater inflater, ViewGroup view) {
        LinearLayout lroot = (LinearLayout) view.findViewById(R.id.exam_content);

        if(GGApp.GG_APP.preferences.getBoolean("first_use_exam_filter", true)) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle(getString(R.string.explanation));
            builder.setMessage(getString(R.string.exam_explain));
            builder.setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
            builder.create().show();
        }
        GGApp.GG_APP.preferences.edit().putBoolean("first_use_exam_filter", false).apply();

        ScrollView sv = new ScrollView(getActivity());
        sv.setLayoutParams(new ScrollView.LayoutParams(ScrollView.LayoutParams.MATCH_PARENT, ScrollView.LayoutParams.MATCH_PARENT));
        sv.setFillViewport(true);
        sv.setTag("gg_scroll");
        lroot.addView(sv);

        LinearLayout l = new LinearLayout(getActivity());
        l.setOrientation(LinearLayout.VERTICAL);
        l.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
        sv.addView(l);


        CardView cv2 = new CardView(getActivity());
        cv2.setRadius(0);
        cv2.setCardBackgroundColor(Color.parseColor(GGApp.GG_APP.isDarkThemeEnabled() ? "#424242" : "#ffffff"));
        LinearLayout l2 = new LinearLayout(getActivity());
        cv2.addView(l2);
        l.addView(cv2);

        final TextView tv5 = createTextView("", 15, inflater, l2);
        tv5.setPadding(toPixels(16), toPixels(16), toPixels(16), toPixels(16));

        LinearLayout l4 = new LinearLayout(getActivity());
        l4.setGravity(Gravity.END | Gravity.CENTER);
        l4.setPadding(0, 0, toPixels(16), 0);
        l4.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));

        final List<String> classes = new ArrayList<>();
        classes.add(getString(R.string.overview));
        classes.addAll(GGApp.GG_APP.exams.getAllClasses());
        Spinner classSpinner = new Spinner(getActivity());
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_spinner_item, classes);
        adapter.setDropDownViewResource(R.layout.support_simple_spinner_dropdown_item);
        classSpinner.setAdapter(adapter);
        int selection = GGApp.GG_APP.preferences.getInt("exam_selected", 0);
        if(selection < classes.size())
            classSpinner.setSelection(selection);

        l4.addView(classSpinner);
        l2.addView(l4);

        final LinearLayout lcontent = new LinearLayout(getActivity());
        lcontent.setOrientation(LinearLayout.VERTICAL);
        lcontent.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
        l.addView(lcontent);

        classSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                GGApp.GG_APP.preferences.edit().putInt("exam_selected", position).apply();

                lcontent.removeAllViews();

                final LinearLayout l = new LinearLayout(getActivity());
                createRootLayout(l);
                lcontent.addView(l);

                if (position == 0) {
                    List<Exams.ExamItem> list = GGApp.GG_APP.exams.getSelectedItems(false);
                    tv5.setText(getString(R.string.entries) + " " + list.size());
                    if (list.size() == 0) {
                        createNoEntriesCard(l, inflater);
                    } else {
                        createContentList(list, getString(R.string.your_overview), l, inflater, false);
                    }
                } else {
                    String cl = classes.get(position);
                    Filter.FilterList list = new Filter.FilterList();
                    list.mainFilter = new Filter();
                    list.mainFilter.type = Filter.FilterType.CLASS;
                    list.mainFilter.filter = cl;

                    List<Exams.ExamItem> items = GGApp.GG_APP.exams.filter(list, false);
                    tv5.setText(getString(R.string.entries) + " " + items.size());
                    if (GGApp.GG_APP.exams.size() != 0) {
                        createContentList(items, cl, l, inflater, true);
                    } else {
                        createNoEntriesCard(l, inflater);
                    }
                }


            }
        });

        cardColorIndex = 0;



    }

    @Override
    public ViewGroup getContentView() {
        return (ViewGroup) getView().findViewById(R.id.exam_content);
    }

    private CardView createCardItem(final Exams.ExamItem examItem, LayoutInflater i, boolean checkbox) {
        CardView ecv = createCardView();
        String[] colors = getActivity().getResources().getStringArray(GGApp.GG_APP.school.getColorArray());
        ecv.setCardBackgroundColor(Color.parseColor(colors[cardColorIndex]));
        cardColorIndex++;
        if(cardColorIndex == colors.length)
            cardColorIndex = 0;
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, toPixels(6));
        ecv.setLayoutParams(params);
        i.inflate(R.layout.exam_cardview_entry, ecv, true);
        ((TextView) ecv.findViewById(R.id.ecv_date)).setText(getFormattedDate(examItem.date));
        ((TextView) ecv.findViewById(R.id.ecv_lesson)).setText(getDay(examItem.date));
        String content = examItem.subject;
        if(!examItem.teacher.equals(""))
            content += " [" + examItem.teacher + "]";
        ((TextView) ecv.findViewById(R.id.ecv_subject_teacher)).setText(content);
        String lessonContent = examItem.clazz;
        if(Integer.parseInt(examItem.lesson) > 0) {
            String lesson = examItem.lesson;
            if(Integer.parseInt(examItem.length) > 1)
                lesson += ". - " + (Integer.parseInt(examItem.lesson) + Integer.parseInt(examItem.length) - 1) + ".";

            lessonContent += "\n" + getString(R.string.lessons) + " " + lesson;
        }
        ((TextView) ecv.findViewById(R.id.ecv_schoolclass)).setText(lessonContent);
        final String calendarTitle = getResources().getString(R.string.exam) + ": " + content;
        CheckBox cb = (CheckBox) ecv.findViewById(R.id.ecv_checkbox);
        if(checkbox) {
            cb.setChecked(examItem.selected);
            cb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    examItem.selected = isChecked;
                    new Thread() {
                        @Override
                        public void run() {
                            GGApp.GG_APP.exams.save();
                        }
                    }.start();

                }
            });
        } else {
            cb.setVisibility(View.GONE);
        }

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
