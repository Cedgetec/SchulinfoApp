/*
 * Copyright 2015 - 2016 Hauke Oldsen
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

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.widget.AppCompatSpinner;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Date;

import de.gebatzens.sia.FilterActivity;
import de.gebatzens.sia.FragmentData;
import de.gebatzens.sia.GGApp;
import de.gebatzens.sia.R;
import de.gebatzens.sia.data.Filter;
import de.gebatzens.sia.data.GGPlan;

public class SubstPagerFragment extends RemoteDataFragment {

    public static final int INDEX_OVERVIEW = -2, INDEX_INVALID = -1;

    public static final int CARD_CLASS = 1, CARD_LESSON = 2;

    GGPlan plan;
    int index = INDEX_INVALID;
    int spinnerPos = 0, modeSpinnerPos = 0;

    public RecyclerView recyclerView;

    /**
     * Creates a card for the given entry
     *
     * @return the view
     */

    /**
     * Creates TextViews containing the special messages of the given plan
     *
     */

    @Override
    public void updateFragment() {
        switch(index) {
            case INDEX_OVERVIEW:
                super.updateFragment();
                break;
            case INDEX_INVALID:
                return;
            default:
                GGPlan.GGPlans plans = (GGPlan.GGPlans) GGApp.GG_APP.school.fragments.getData(FragmentData.FragmentType.PLAN).get(0).getData();
                if(plans.size() <= index) {
                    // This fragment will be deleted in a few seconds
                    break;
                }
                plan = plans.get(index);

                ArrayList<String> items = new ArrayList<>();
                items.add(getActivity().getString(R.string.all));
                items.addAll(plan.getAllClasses());
                final ArrayAdapter<String> adapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_item, items);

                AppCompatSpinner spinner = (AppCompatSpinner) getContentView().findViewById(R.id.spinner);
                spinner.setAdapter(adapter);
                spinner.getOnItemSelectedListener().onItemSelected(spinner, spinner, spinnerPos, 0);
                break;

        }
    }

    @Override
    public void createView(final LayoutInflater inflater, ViewGroup group) {
        LinearLayout l = new LinearLayout(getActivity());
        l.setOrientation(LinearLayout.VERTICAL);
        l.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        if(index == INDEX_INVALID) {
            /*TextView tv = new TextView(getActivity());
            tv.setText("Error: " + type);
            l.addView(tv);
            Log.w("ggvp", "bundle " + type + " " + this + " " + getParentFragment());*/
            throw new IllegalArgumentException(("index is INDEX_INVALID"));
        } else if(index == INDEX_OVERVIEW && GGApp.GG_APP.filters.including.size() > 0) {
            // Overview, filter applied

            Filter.FilterList filters = GGApp.GG_APP.filters;

            CardView cv2 = new CardView(getActivity());
            cv2.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            cv2.setRadius(0);

            LinearLayout l2 = new LinearLayout(getActivity());
            l2.setMinimumHeight(toPixels(50));
            l2.setGravity(Gravity.CENTER_VERTICAL);

            String diff = getTimeDiff(getActivity(), ((GGPlan.GGPlans) getFragment().getData()).loadDate);
            TextView tv4 = createPrimaryTextView(diff, 13, inflater, l2);
            tv4.setTag("gg_time");
            tv4.setPadding(toPixels(16), toPixels(0), toPixels(16), toPixels(0));

            LinearLayout l3 = new LinearLayout(getActivity());
            l3.setGravity(Gravity.END | Gravity.CENTER);
            l3.setPadding(0, 0, toPixels(16), 0);
            l3.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

            int chars = 0;
            int chips = 0;

            for(Filter.IncludingFilter inc : filters.including) {
                String text = chars > 8 ? "+" + (filters.including.size() - chips) + "" : inc.getFilter();

                TextView tv2 = createPrimaryTextView(text, chars > 8 ? 20 : 15, inflater, l3);
                LinearLayout.LayoutParams pa = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                pa.setMargins(toPixels(chars > 8 ? 10 : 5), 0, chars > 8 ? toPixels(-20) : 0, 0);
                tv2.setLayoutParams(pa);
                tv2.setIncludeFontPadding(false);
                if (chars <= 8) {
                    tv2.setBackgroundResource(R.drawable.chip_background);
                } else {
                    tv2.setTextColor(Color.parseColor("#A0A0A0"));
                   // tv2.setText(Html.fromHtml(text));
                    tv2.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            startActivity(new Intent(getContext(), FilterActivity.class));
                        }
                    });
                }

                if(chars > 8)
                    break;

                chars += inc.getFilter().length();
                chips++;

            }

            l2.addView(l3);
            cv2.addView(l2);
            l.addView(cv2);

            recyclerView = (RecyclerView) inflater.inflate(R.layout.basic_recyclerview, l, false);
            recyclerView.setPadding(0,0,0,toPixels(5));
            final SubstListAdapter sla = new SubstListAdapter(this);
            recyclerView.setAdapter(sla);
            recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
            l.addView(recyclerView);
            sla.setToOverview();

            group.addView(l);

        } else if(index == INDEX_OVERVIEW) {
            //Overview, no filter applied

            createMessage(l, getResources().getString(R.string.no_filter_applied), getResources().getString(R.string.settings), new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    Intent i = new Intent(getActivity(), FilterActivity.class);
                    getActivity().startActivityForResult(i, 1);
                }
            });

            ScrollView sv = new ScrollView(getActivity());
            sv.setLayoutParams(new ScrollView.LayoutParams(ScrollView.LayoutParams.MATCH_PARENT, ScrollView.LayoutParams.MATCH_PARENT));
            sv.setFillViewport(true);

            sv.addView(l);
            group.addView(sv);

        } else {
            CardView cv2 = new CardView(getActivity());
            cv2.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            cv2.setRadius(0);

            LinearLayout l2 = new LinearLayout(getActivity());
            l2.setMinimumHeight(toPixels(50));
            l2.setGravity(Gravity.CENTER_VERTICAL);

            String diff = getTimeDiff(getActivity(), ((GGPlan.GGPlans) getFragment().getData()).loadDate);
            TextView tv5 = createPrimaryTextView(diff, 13, inflater, l2);
            tv5.setTag("gg_time");
            tv5.setPadding(toPixels(16), toPixels(0), toPixels(16), toPixels(0));

            LinearLayout l3 = new LinearLayout(getActivity());
            l3.setGravity(Gravity.END | Gravity.CENTER);
            l3.setPadding(0, 0, toPixels(16), 0);
            l3.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

            final AppCompatSpinner spinMode = new AppCompatSpinner(getActivity());
            ArrayAdapter<String> modeAdapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_item, new String[]{getString(R.string.classes), getString(R.string.lessons)});
            modeAdapter.setDropDownViewResource(R.layout.support_simple_spinner_dropdown_item);
            spinMode.setAdapter(modeAdapter);
            l3.addView(spinMode);

            final AppCompatSpinner spinClass = new AppCompatSpinner(getActivity());
            spinClass.setId(R.id.spinner);

            ArrayList<String> items = new ArrayList<>();
            items.add(getActivity().getString(R.string.all));
            items.addAll(plan.getAllClasses());
            ArrayAdapter<String> adapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_item, items);
            adapter.setDropDownViewResource(R.layout.support_simple_spinner_dropdown_item);
            spinClass.setAdapter(adapter);
            l3.addView(spinClass);

            spinMode.setSelection(modeSpinnerPos);
            spinClass.setSelection(spinnerPos);

            l2.addView(l3);
            cv2.addView(l2);
            l.addView(cv2);

            final LinearLayout l4 = new LinearLayout(getActivity());
            l4.setOrientation(LinearLayout.VERTICAL);
            l.addView(l4);

            recyclerView = (RecyclerView) inflater.inflate(R.layout.basic_recyclerview, l4, false);
            recyclerView.setPadding(0,0,0,toPixels(5));
            final SubstListAdapter sla = new SubstListAdapter(this);
            recyclerView.setAdapter(sla);
            recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

            l4.addView(recyclerView);

            group.addView(l);

            spinMode.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

                boolean first = true;

                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    modeSpinnerPos = position;

                    //ignore first call
                    if(!first) {
                        spinClass.getOnItemSelectedListener().onItemSelected(spinClass, spinClass, spinnerPos, 0);
                    }

                    first = false;
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {

                }
            });

            spinClass.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    String item = ((ArrayAdapter<String>) spinClass.getAdapter()).getItem(position);

                    spinnerPos = position;

                    if (!item.equals(getActivity().getString(R.string.all))) {
                        Filter.FilterList fl = new Filter.FilterList();
                        fl.including.add(new Filter.IncludingFilter(Filter.FilterType.CLASS, item));
                        sla.updateData(plan.filter(fl), SubstListAdapter.PLAIN, true, item);

                    } else {
                        boolean sortByLesson = spinMode.getSelectedItemPosition() == 1;
                        sla.updateData(plan, sortByLesson ? SubstListAdapter.ALL_LESSONS : SubstListAdapter.ALL_CLASSES, true);

                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                    l4.removeAllViews();
                }
            });

        }
    }

    @Override
    public ViewGroup getContentView() {
        return (ViewGroup) getView();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup group, Bundle bundle) {
        this.index = getArguments().getInt("index");
        if(getFragment().getData() != null && index >= 0) {
            plan = ((GGPlan.GGPlans) getFragment().getData()).get(index);
        }

        LinearLayout l = new LinearLayout(getActivity());
        l.setOrientation(LinearLayout.VERTICAL);
        if(getFragment().getData() != null)
            createRootView(inflater, l);
        return l;
    }



    public static String getTimeDiff(Context ctx, Date old) {
        long diff = new Date().getTime() - old.getTime();
        int minutes = (int) (diff / (1000 * 60));

        if(minutes > 60) {
            int hours = (int) Math.floor((float) minutes / 60.0f);
            return ctx.getResources().getString(R.string.time_diff_hours, hours);
        } else {
            if(minutes == 0)
                return ctx.getString(R.string.just_now);
            else
                return ctx.getResources().getString(R.string.time_diff, minutes);
        }

    }
}
