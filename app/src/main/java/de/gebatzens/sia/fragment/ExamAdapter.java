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
package de.gebatzens.sia.fragment;

import android.content.res.Configuration;
import android.graphics.Color;
import android.support.v7.widget.AppCompatSpinner;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import de.gebatzens.sia.GGApp;
import de.gebatzens.sia.R;
import de.gebatzens.sia.data.Exams;
import de.gebatzens.sia.data.Filter;

public class ExamAdapter extends RecyclerView.Adapter {

    List<Exams.ExamItem> list;
    ExamFragment frag;
    int cardColorIndex = 0;
    boolean overview;
    String label;

    public ExamAdapter(ExamFragment f) {
        frag = f;
    }

    public void update(String label, List<Exams.ExamItem> list, boolean overview) {
        this.list = list;
        this.label = label;
        this.overview = overview;
        notifyDataSetChanged();
    }

    public void setOrientationPadding(View v) {
        if(frag.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            v.setPadding(RemoteDataFragment.toPixels(55), 0, RemoteDataFragment.toPixels(55), 0);
        } else if(frag.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            v.setPadding(RemoteDataFragment.toPixels(4), 0, RemoteDataFragment.toPixels(4), 0);
        }
    }

    @Override
    public int getItemViewType(int position) {
        switch(position) {
            case 0:
            case 1:
                return position;
            default:
                return list.size() == 0 ? 3 : 2;
        }
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        switch(viewType) {
            case 0:
                CardView cv2 = new CardView(frag.getContext());
                cv2.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                cv2.setRadius(0);
                cv2.setCardBackgroundColor(Color.parseColor(GGApp.GG_APP.isDarkThemeEnabled() ? "#424242" : "#ffffff"));
                LinearLayout l2 = new LinearLayout(frag.getContext());
                cv2.addView(l2);

                final TextView tv5 = frag.createTextView("", 15, inflater, l2);
                tv5.setPadding(RemoteDataFragment.toPixels(16), RemoteDataFragment.toPixels(16), RemoteDataFragment.toPixels(16), RemoteDataFragment.toPixels(16));

                LinearLayout l4 = new LinearLayout(frag.getContext());
                l4.setGravity(Gravity.END | Gravity.CENTER);
                l4.setPadding(0, 0, RemoteDataFragment.toPixels(16), 0);
                l4.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));

                final List<String> classes = new ArrayList<>();
                classes.add(frag.getString(R.string.overview));
                classes.addAll(((Exams) frag.getFragment().getData()).getAllClasses());
                AppCompatSpinner classSpinner = new AppCompatSpinner(frag.getContext());
                ArrayAdapter<String> adapter = new ArrayAdapter<>(frag.getContext(), android.R.layout.simple_spinner_item, classes);
                adapter.setDropDownViewResource(R.layout.support_simple_spinner_dropdown_item);
                classSpinner.setAdapter(adapter);
                int selection = GGApp.GG_APP.preferences.getInt("exam_selected", 0);
                if(selection < classes.size())
                    classSpinner.setSelection(selection);

                l4.addView(classSpinner);
                l2.addView(l4);

                classSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {

                    }

                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        GGApp.GG_APP.preferences.edit().putInt("exam_selected", position).apply();

                        if (position == 0) {
                            List<Exams.ExamItem> list = ((Exams) frag.getFragment().getData()).getSelectedItems(false);
                            tv5.setText(frag.getString(R.string.entries) + " " + list.size());
                            ExamAdapter.this.update(frag.getString(R.string.your_overview), list, true);
                        } else {
                            String cl = classes.get(position);
                            Filter.FilterList list = new Filter.FilterList();
                            list.including.add(new Filter.IncludingFilter(Filter.FilterType.CLASS, cl));

                            List<Exams.ExamItem> items = ((Exams) frag.getFragment().getData()).filter(list, false);
                            tv5.setText(frag.getString(R.string.entries) + " " + items.size());
                            ExamAdapter.this.update(cl, items, false);
                        }


                    }
                });

                return new ViewHolder(cv2);
            case 3:
                LinearLayout wrapper = new LinearLayout(frag.getContext());
                wrapper.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                setOrientationPadding(wrapper);

                frag.createNoEntriesCard(wrapper, inflater);
                return new ViewHolder(wrapper);
            case 2:
                return createCardItem(inflater);
            case 1:
                wrapper = new LinearLayout(frag.getContext());
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                params.setMargins(0, RemoteDataFragment.toPixels(20), 0, 0);
                wrapper.setLayoutParams(params);
                setOrientationPadding(wrapper);
                TextView tv = frag.createTextView("", 27, inflater, wrapper);
                tv.setPadding(RemoteDataFragment.toPixels(3), 0, 0, 0);
                if (GGApp.GG_APP.isDarkThemeEnabled()) {
                    tv.setTextColor(Color.parseColor("#a0a0a0"));
                } else {
                    tv.setTextColor(Color.parseColor("#6e6e6e"));
                }
                return new LabelViewHolder(wrapper);
            default:
                return null;
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if(position == 1) {
            ((LabelViewHolder) holder).update(label);
        } else if(holder instanceof CardViewHolder) {
            ((CardViewHolder) holder).update(list.get(position - 2), !overview);
        }
    }

    @Override
    public int getItemCount() {
        return 2 + Math.max(list.size(), 1);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        public ViewHolder(View itemView) {
            super(itemView);
        }
    }

    public static class LabelViewHolder extends RecyclerView.ViewHolder {

        TextView label;

        public LabelViewHolder(ViewGroup itemView) {
            super(itemView);
            label = (TextView) itemView.getChildAt(0);
        }

        public void update(String l) {
            label.setText(l);
        }
    }

    public class CardViewHolder extends RecyclerView.ViewHolder {

        TextView date, lesson, subjectTeacher, sclass;
        CheckBox cb;

        public CardViewHolder(View itemView) {
            super(itemView);

            date = (TextView) itemView.findViewById(R.id.ecv_date);
            lesson = (TextView) itemView.findViewById(R.id.ecv_lesson);
            subjectTeacher = (TextView) itemView.findViewById(R.id.ecv_subject_teacher);
            sclass = (TextView) itemView.findViewById(R.id.ecv_schoolclass);
            cb = (CheckBox) itemView.findViewById(R.id.ecv_checkbox);
        }

        public void update(final Exams.ExamItem examItem, boolean checkbox) {
            date.setText(getFormattedDate(examItem.date));
            lesson.setText(getDay(examItem.date));
            String content = examItem.subject;
            if(!examItem.teacher.equals(""))
                content += " [" + examItem.teacher + "]";
            subjectTeacher.setText(content);
            String lessonContent = examItem.clazz;
            if(Integer.parseInt(examItem.lesson) > 0) {
                String lesson = examItem.lesson;
                if(Integer.parseInt(examItem.length) > 1)
                    lesson += ". - " + (Integer.parseInt(examItem.lesson) + Integer.parseInt(examItem.length) - 1) + ".";

                lessonContent += "\n" + frag.getString(R.string.lessons) + " " + lesson;
            }
            sclass.setText(lessonContent);
            final String calendarTitle = frag.getString(R.string.exam) + ": " + content;
            if(checkbox) {
                cb.setVisibility(View.VISIBLE);
                cb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        examItem.selected = isChecked;
                        new Thread() {
                            @Override
                            public void run() {
                                frag.getFragment().getData().save();
                            }
                        }.start();

                    }
                });
                cb.setChecked(examItem.selected);
            } else {
                cb.setVisibility(View.GONE);
                cb.setOnCheckedChangeListener(null);
            }

        }
    }

    private CardViewHolder createCardItem(LayoutInflater i) {
        LinearLayout wrapper = new LinearLayout(frag.getContext());
        wrapper.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        setOrientationPadding(wrapper);

        CardView ecv = (CardView) i.inflate(R.layout.basic_cardview, null);
        String[] colors = frag.getContext().getResources().getStringArray(GGApp.GG_APP.school.getColorArray());
        ecv.setCardBackgroundColor(Color.parseColor(colors[cardColorIndex]));
        cardColorIndex++;
        if(cardColorIndex == colors.length)
            cardColorIndex = 0;
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(RemoteDataFragment.toPixels(3), 0, RemoteDataFragment.toPixels(3), RemoteDataFragment.toPixels(3));
        ecv.setLayoutParams(params);
        i.inflate(R.layout.exam_cardview_entry, ecv, true);
        wrapper.addView(ecv);

        return new CardViewHolder(wrapper);
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
