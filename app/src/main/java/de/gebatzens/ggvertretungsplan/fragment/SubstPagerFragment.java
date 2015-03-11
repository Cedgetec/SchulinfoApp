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

package de.gebatzens.ggvertretungsplan.fragment;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.widget.CardView;
import android.text.Html;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import de.gebatzens.ggvertretungsplan.GGApp;
import de.gebatzens.ggvertretungsplan.R;
import de.gebatzens.ggvertretungsplan.SettingsActivity;
import de.gebatzens.ggvertretungsplan.data.Filter;
import de.gebatzens.ggvertretungsplan.data.GGPlan;

public class SubstPagerFragment extends RemoteDataFragment {

    public static final int TYPE_OVERVIEW = 0, TYPE_TODAY = 1, TYPE_TOMORROW = 2;

    GGPlan plan, planh, planm;
    int type = -1;
    int spinnerPos = 0;

    public SubstPagerFragment() {
        super.type = GGApp.FragmentType.PLAN;
    }

    public void setParams(int type) {
        this.type = type;
        if(GGApp.GG_APP.plans != null) {
            planh = GGApp.GG_APP.plans.today;
            planm = GGApp.GG_APP.plans.tomorrow;
        }
        if(type == TYPE_TODAY)
            plan = planh;
        else if(type == TYPE_TOMORROW)
            plan = planm;

    }

    private void createCardItems(List<GGPlan.Entry> list, ViewGroup group, LayoutInflater inflater, boolean clas) {
        if(list.size() == 0) {
            FrameLayout f2 = new FrameLayout(getActivity());
            f2.setPadding(toPixels(1.3f),toPixels(0.3f),toPixels(1.3f),toPixels(0.3f));
            CardView cv = createCardView();
            f2.addView(cv);
            createTextView(getResources().getString(R.string.no_entries_in_substitutionplan), 20, inflater, cv);
            group.addView(f2);
        }

        for(GGPlan.Entry e : list) {
            FrameLayout f2 = new FrameLayout(getActivity());
            f2.setPadding(toPixels(1.3f),toPixels(0.3f),toPixels(1.3f),toPixels(0.3f));
            f2.addView(createCardItem(e, inflater, clas));
            group.addView(f2);
        }
    }

    int cardColorIndex = 0;

    private CardView createCardItem(GGPlan.Entry entry, LayoutInflater i, boolean clas) {
        CardView cv = createCardView();
        String[] colors = getActivity().getResources().getStringArray(GGApp.GG_APP.provider.getColorArray());
        cv.setCardBackgroundColor(Color.parseColor(colors[cardColorIndex]));
        cardColorIndex++;
        if(cardColorIndex == colors.length)
            cardColorIndex = 0;
        i.inflate(R.layout.cardview_entry, cv, true);
        ((TextView) cv.findViewById(R.id.cv_hour)).setText(entry.hour);
        ((TextView) cv.findViewById(R.id.cv_header)).setText(entry.type + (entry.subst.isEmpty() ? "" : " [" + entry.subst + "]"));
        TextView tv = (TextView) cv.findViewById(R.id.cv_detail);
        tv.setText(entry.comment + (entry.room.isEmpty() ? "" : (entry.comment.isEmpty() ? "" : "\n") + "Raum " + entry.room));
        if(tv.getText().toString().trim().isEmpty())
            ((ViewGroup) tv.getParent()).removeView(tv);
        ((TextView) cv.findViewById(R.id.cv_subject)).setText(Html.fromHtml((clas ? entry.clazz + " " : "") + entry.subject));
        return cv;
    }



    private ArrayList<TextView> createSMViews(GGPlan plan) {
        ArrayList<TextView> tvl = new ArrayList<TextView>();

        for(String special : plan.special) {
            TextView tv2 = new TextView(getActivity());
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            params.setMargins(0, toPixels(2), 0, 0);
            tv2.setLayoutParams(params);
            tv2.setText(Html.fromHtml(special));
            tv2.setTextSize(15);
            tv2.setTextColor(Color.WHITE);
            tvl.add(tv2);

        }

        return tvl;
    }

    @Override
    public void createView(final LayoutInflater inflater, ViewGroup group) {

        cardColorIndex = 0;
        ScrollView sv = new ScrollView(getActivity());
        sv.setLayoutParams(new ScrollView.LayoutParams(ScrollView.LayoutParams.MATCH_PARENT, ScrollView.LayoutParams.MATCH_PARENT));
        sv.setFillViewport(true);
        sv.setTag("ggfrag_scrollview");
        LinearLayout l0 = new LinearLayout(getActivity());
        l0.setOrientation(LinearLayout.VERTICAL);
        LinearLayout l = new LinearLayout(getActivity());
        l.setLayoutParams(new ScrollView.LayoutParams(ScrollView.LayoutParams.MATCH_PARENT, ScrollView.LayoutParams.MATCH_PARENT));
        l.setOrientation(LinearLayout.VERTICAL);
        l.setPadding(toPixels(4),toPixels(4),toPixels(4),toPixels(4));
        group.addView(sv);
        if(planh == null || planm == null) {
            TextView tv = new TextView(getActivity());
            tv.setText("Error: " + type);
            l.addView(tv);
            Log.w("ggvp", "setParams not called " + type + " " + this + " " + getParentFragment());
        } else if(type == TYPE_OVERVIEW && !GGApp.GG_APP.filters.mainFilter.filter.equals("")) {
            //normale Übersicht
            Filter.FilterList filters = GGApp.GG_APP.filters;

            List<GGPlan.Entry> list = planh.filter(filters);

            CardView cv2 = new CardView(getActivity());
            cv2.setContentPadding(toPixels(16),toPixels(16),toPixels(16),toPixels(16));
            cv2.setCardBackgroundColor(Color.WHITE);
            cv2.setRadius(0);

            LinearLayout l2 = new LinearLayout(getActivity());

            cv2.addView(l2);
            l0.addView(cv2);

            createTextView(planh.loadDate, 15, inflater, l2);

            TextView tv2 = createTextView(
                    filters.mainFilter.type == Filter.FilterType.CLASS ? getActivity().getString(R.string.schoolclass) + " " + filters.mainFilter.filter :
                    getActivity().getString(R.string.teacher) + " " + filters.mainFilter.filter, 15, inflater, l2);
            tv2.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            tv2.setGravity(Gravity.RIGHT | Gravity.CENTER);

            createTextView(translateDay(planh.date), 30, inflater, l).setPadding(0, toPixels(20), 0, 0);
            if(!planh.special.isEmpty()) {
                FrameLayout f2 = new FrameLayout(getActivity());
                f2.setPadding(toPixels(1.3f), toPixels(0.3f), toPixels(1.3f), toPixels(0.3f));
                CardView cv = createCardView();
                cv.setCardBackgroundColor(GGApp.GG_APP.provider.getColor());
                f2.addView(cv);
                l.addView(f2);
                LinearLayout ls = new LinearLayout(getActivity());
                ls.setOrientation(LinearLayout.VERTICAL);
                TextView tv3 = createTextView(getResources().getString(R.string.special_messages), 19, inflater, ls);
                tv3.setTextColor(Color.WHITE);
                tv3.setPadding(0,0,0,toPixels(6));
                cv.addView(ls);

                for(TextView tv : createSMViews(planh)) {
                    ls.addView(tv);
                }
            }
            createCardItems(list, l, inflater, filters.mainFilter.type != Filter.FilterType.CLASS);

            list = planm.filter(filters);
            createTextView(translateDay(planm.date), 30, inflater, l).setPadding(0, toPixels(20), 0, 0);

            if(!planm.special.isEmpty()) {
                FrameLayout f2 = new FrameLayout(getActivity());
                f2.setPadding(toPixels(1.3f), toPixels(0.3f), toPixels(1.3f), toPixels(0.3f));
                CardView cv = createCardView();
                cv.setCardBackgroundColor(GGApp.GG_APP.provider.getColor());
                f2.addView(cv);
                l.addView(f2);
                LinearLayout ls = new LinearLayout(getActivity());
                ls.setOrientation(LinearLayout.VERTICAL);
                TextView tv3 = createTextView(getResources().getString(R.string.special_messages), 19, inflater, ls);
                tv3.setTextColor(Color.WHITE);
                tv3.setPadding(0,0,0,toPixels(6));
                cv.addView(ls);

                for(TextView tv : createSMViews(planm)) {
                    ls.addView(tv);
                }
            }

            createCardItems(list, l, inflater, filters.mainFilter.type != Filter.FilterType.CLASS);

        } else if(type == TYPE_OVERVIEW) {
            //Keine Klasse
            createButtonWithText(getActivity(), l, getResources().getString(R.string.no_filter_applied), getResources().getString(R.string.settings), new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    Intent i = new Intent(getActivity(), SettingsActivity.class);
                    getActivity().startActivityForResult(i, 1);
                }
            });


        } else {
            CardView cv2 = new CardView(getActivity());
            cv2.setContentPadding(toPixels(16),toPixels(16),toPixels(16),toPixels(16));
            cv2.setCardBackgroundColor(Color.WHITE);
            cv2.setRadius(0);

            LinearLayout l2 = new LinearLayout(getActivity());

            cv2.addView(l2);
            l0.addView(cv2);

            createTextView(plan.loadDate, 15, inflater, l2);

            LinearLayout l4 = new LinearLayout(getActivity());
            l4.setGravity(Gravity.RIGHT);
            
            l4.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

            Spinner spin = new Spinner(getActivity());
            ArrayList<String> items = new ArrayList<String>();
            items.add(getActivity().getString(R.string.all));
            items.addAll(plan.getAllClasses());
            final ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_spinner_item, items);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spin.setAdapter(adapter);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT);
            spin.setLayoutParams(lp);
            l4.addView(spin);
            l2.addView(l4);

            spin.setSelection(spinnerPos);

            if(!plan.special.isEmpty()) {
                FrameLayout f2 = new FrameLayout(getActivity());
                f2.setPadding(toPixels(1.3f), toPixels(0.3f), toPixels(1.3f), toPixels(0.3f));
                CardView cv = createCardView();
                cv.setCardBackgroundColor(GGApp.GG_APP.provider.getColor());
                f2.addView(cv);
                l.addView(f2);
                LinearLayout ls = new LinearLayout(getActivity());
                ls.setOrientation(LinearLayout.VERTICAL);
                TextView tv3 = createTextView(getResources().getString(R.string.special_messages), 19, inflater, ls);
                tv3.setTextColor(Color.WHITE);
                tv3.setPadding(0,0,0,toPixels(6));
                cv.addView(ls);

                for(TextView tv : createSMViews(plan)) {
                    ls.addView(tv);
                }
            }

            final LinearLayout l3 = new LinearLayout(getActivity());
            l3.setOrientation(LinearLayout.VERTICAL);
            l.addView(l3);

            spin.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    String item = adapter.getItem(position);

                    spinnerPos = position;

                    if (!item.equals(getActivity().getString(R.string.all))) {
                        l3.removeAllViews();
                        cardColorIndex = 0;
                        Filter.FilterList fl = new Filter.FilterList();
                        Filter main = new Filter();
                        fl.mainFilter = main;
                        main.type = Filter.FilterType.CLASS;
                        main.filter = item;
                        createCardItems(plan.filter(fl), l3, inflater, false);

                    } else {
                        l3.removeAllViews();
                        cardColorIndex = 0;

                        List<String> classes = plan.getAllClasses();
                        for(String s : classes) {
                            createTextView(s, 30, inflater, l3).setPadding(0, toPixels(20), 0, 0);
                            Filter.FilterList fl = new Filter.FilterList();
                            Filter main = new Filter();
                            fl.mainFilter = main;
                            main.filter = s;
                            main.type = Filter.FilterType.CLASS;
                            createCardItems(plan.filter(fl), l3, inflater, false);
                        }
                    }
                }
                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                    l3.removeAllViews();
                }
            });

        }
        l0.addView(l);
        sv.addView(l0);
    }

    @Override
    public ViewGroup getContentView() {
        return (ViewGroup) getView();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup group, Bundle bundle) {
        LinearLayout l = new LinearLayout(getActivity());
        l.setOrientation(LinearLayout.VERTICAL);
        if(GGApp.GG_APP.plans != null)
            createRootView(inflater, l);
        return l;
    }

    private String translateDay(Date date) {
        StringBuilder sb = new StringBuilder();
        SimpleDateFormat convertedDateFormat;
        if(Locale.getDefault().getLanguage().equals("en")) {
            convertedDateFormat = new SimpleDateFormat("EEEE, MMM dd");
        } else {
            convertedDateFormat = new SimpleDateFormat("EEEE, dd. MMM");
        }

        sb.append(convertedDateFormat.format(date));
        return sb.toString();
    }
}
