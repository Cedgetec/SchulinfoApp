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
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

    // current list for non-overview fragments

    /**
     * Creates cards for the given list of entries
     *
     * @param list
     * @param group The cards are added to this view group
     * @param inflater
     * @param type bitwise CARD_CLASS CARD_LESSON
     */
    private void createCardItems(List<GGPlan.Entry> list, ViewGroup group, LayoutInflater inflater, int type) {
        if(list.size() == 0) {
            createNoEntriesCard(group, inflater);
        } else {
            for (GGPlan.Entry e : list) {
                SubstListAdapter.SubstViewHolder cv = createCardItem(inflater, group);
                cv.update(e, type);
                group.addView(cv.itemView);
            }
        }

    }

    int cardColorIndex = 0;

    /**
     * Creates a card for the given entry
     *
     * @return the view
     */
    public SubstListAdapter.SubstViewHolder createCardItem(LayoutInflater i, ViewGroup parent) {
        FrameLayout f2 = new FrameLayout(getActivity());
        f2.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        f2.setPadding(RemoteDataFragment.toPixels(1.3f), RemoteDataFragment.toPixels(0.3f), RemoteDataFragment.toPixels(1.3f), RemoteDataFragment.toPixels(0.3f));
        CardView cv = (CardView) i.inflate(R.layout.basic_cardview, parent, false);
        f2.addView(cv);
        cv.setId(R.id.cvroot);
        String[] colors = GGApp.GG_APP.getResources().getStringArray(GGApp.GG_APP.school.getColorArray());
        cv.setCardBackgroundColor(Color.parseColor(colors[cardColorIndex]));
        cardColorIndex++;
        if(cardColorIndex == colors.length)
            cardColorIndex = 0;
        i.inflate(R.layout.cardview_entry, cv, true);

        return new SubstListAdapter.SubstViewHolder(f2);
    }

    /**
     * Creates TextViews containing the special messages of the given plan
     *
     */
    public static ArrayList<TextView> createSMViews(List<String> messages, ViewGroup parent) {
        ArrayList<TextView> tvl = new ArrayList<TextView>();

        for(String special : messages) {
            TextView tv2 = new TextView(parent.getContext());
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

    public SubstListAdapter.MessageViewHolder createSMCard(ViewGroup parent, LayoutInflater inflater) {
        FrameLayout f2 = new FrameLayout(getActivity());
        f2.setPadding(toPixels(1.3f), toPixels(0.3f), toPixels(1.3f), toPixels(0.3f));
        CardView cv = (CardView) inflater.inflate(R.layout.basic_cardview, f2, false);
        cv.setId(R.id.cvroot);
        cv.setCardBackgroundColor(GGApp.GG_APP.school.getColor());
        f2.addView(cv);
        LinearLayout ls = new LinearLayout(getActivity());
        ls.setId(R.id.messages_list);
        ls.setOrientation(LinearLayout.VERTICAL);
        TextView tv3 = createTextView(getResources().getString(R.string.special_messages), 19, inflater, ls);
        tv3.setTextColor(Color.WHITE);
        tv3.setPadding(0,0,0,toPixels(6));
        cv.addView(ls);

        return new SubstListAdapter.MessageViewHolder(f2);

    }

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

                ArrayList<String> items = new ArrayList<String>();
                items.add(getActivity().getString(R.string.all));
                items.addAll(plan.getAllClasses());
                final ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_spinner_item, items);

                Spinner spinner = (Spinner) getContentView().findViewById(R.id.spinner);
                spinner.setAdapter(adapter);
                spinner.getOnItemSelectedListener().onItemSelected(spinner, spinner, spinnerPos, 0);
                break;

        }
    }

    @Override
    public void createView(final LayoutInflater inflater, ViewGroup group) {

        cardColorIndex = 0;

        LinearLayout l0 = new LinearLayout(getActivity());
        l0.setOrientation(LinearLayout.VERTICAL);
        LinearLayout l = new LinearLayout(getActivity());
        l.setOrientation(LinearLayout.VERTICAL);

        if(index == INDEX_INVALID) {
            /*TextView tv = new TextView(getActivity());
            tv.setText("Error: " + type);
            l.addView(tv);
            Log.w("ggvp", "bundle " + type + " " + this + " " + getParentFragment());*/
            throw new IllegalArgumentException(("index is INDEX_INVALID"));
        } else if(index == INDEX_OVERVIEW && !GGApp.GG_APP.filters.mainFilter.getFilter().equals("")) {
            // Overview, filter applied

            Filter.FilterList filters = GGApp.GG_APP.filters;

            CardView cv2 = new CardView(getActivity());
            cv2.setRadius(0);
            cv2.setCardBackgroundColor(Color.parseColor(GGApp.GG_APP.isDarkThemeEnabled() ? "#424242" : "#ffffff"));

            LinearLayout l2 = new LinearLayout(getActivity());

            cv2.addView(l2);
            l0.addView(cv2);

            TextView tv4 = createTextView(getTimeDiff(getActivity(), ((GGPlan.GGPlans) getFragment().getData()).loadDate), 13, inflater, l2);
            tv4.setTag("gg_time");
            tv4.setPadding(toPixels(16), toPixels(16), toPixels(16), toPixels(16));

            TextView tv2 = createTextView(
                    filters.mainFilter.getType() == Filter.FilterType.CLASS ? getActivity().getString(R.string.school_class) + " " + filters.mainFilter.getFilter() :
                    getActivity().getString(R.string.teacher) + " " + filters.mainFilter.getFilter(), 15, inflater, l2);
            tv2.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            tv2.setGravity(Gravity.END | Gravity.CENTER);
            tv2.setPadding(0,0,toPixels(16),0);

            for(GGPlan plan : ((GGPlan.GGPlans) getFragment().getData())) {
                List<GGPlan.Entry> list = plan.filter(filters);
                TextView tv = createTextView(translateDay(plan.date), 27, inflater, l);
                tv.setPadding(toPixels(2.8f), toPixels(20), 0, 0);
                tv.setTextColor(Color.parseColor(GGApp.GG_APP.isDarkThemeEnabled() ? "#a0a0a0" : "#6e6e6e"));

                SubstListAdapter.MessageViewHolder messages = createSMCard(l, inflater);
                messages.update(plan.special);
                l.addView(messages.itemView);

                createCardItems(list, l, inflater, filters.mainFilter.getType() != Filter.FilterType.CLASS ? CARD_LESSON | CARD_CLASS : CARD_LESSON);
            }

            ScrollView sv = new ScrollView(getActivity());
            sv.setLayoutParams(new ScrollView.LayoutParams(ScrollView.LayoutParams.MATCH_PARENT, ScrollView.LayoutParams.MATCH_PARENT));
            sv.setFillViewport(true);
            sv.setTag("gg_scroll");

            sv.addView(l0);
            group.addView(sv);

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
            sv.setTag("gg_scroll");

            sv.addView(l0);
            group.addView(sv);

        } else {
            CardView cv2 = new CardView(getActivity());
            cv2.setRadius(0);
            cv2.setCardBackgroundColor(Color.parseColor(GGApp.GG_APP.isDarkThemeEnabled() ? "#424242" : "#ffffff"));

            LinearLayout l2 = new LinearLayout(getActivity());

            cv2.addView(l2);
            l0.addView(cv2);

            TextView tv5 = createTextView(getTimeDiff(getActivity(), ((GGPlan.GGPlans) getFragment().getData()).loadDate), 13, inflater, l2);
            tv5.setTag("gg_time");
            tv5.setPadding(toPixels(16), toPixels(16), toPixels(16), toPixels(16));

            LinearLayout l4 = new LinearLayout(getActivity());
            l4.setGravity(Gravity.END | Gravity.CENTER);
            l4.setPadding(0, 0, toPixels(16), 0);
            
            l4.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

            final Spinner spinMode = new Spinner(getActivity());
            ArrayAdapter<String> modeAdapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_spinner_item, new String[] {getString(R.string.classes), getString(R.string.lessons)});
            modeAdapter.setDropDownViewResource(R.layout.support_simple_spinner_dropdown_item);
            spinMode.setAdapter(modeAdapter);
            l4.addView(spinMode);

            final Spinner spin = new Spinner(getActivity());
            spin.setId(R.id.spinner);

            ArrayList<String> items = new ArrayList<String>();
            items.add(getActivity().getString(R.string.all));
            items.addAll(plan.getAllClasses());
            ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_spinner_item, items);
            adapter.setDropDownViewResource(R.layout.support_simple_spinner_dropdown_item);
            spin.setAdapter(adapter);
            l4.addView(spin);

            l2.addView(l4);

            spinMode.setSelection(modeSpinnerPos);
            spin.setSelection(spinnerPos);

            final LinearLayout l3 = new LinearLayout(getActivity());
            l3.setOrientation(LinearLayout.VERTICAL);
            l.addView(l3);

            RecyclerView recyclerView = (RecyclerView) inflater.inflate(R.layout.basic_recyclerview, null);
            final SubstListAdapter sla = new SubstListAdapter(this);
            recyclerView.setAdapter(sla);
            recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
            recyclerView.setTag("gg_list");
            recyclerView.setPadding(toPixels(4), toPixels(4), toPixels(4), toPixels(4));
            l3.addView(recyclerView);

            group.addView(l0);

            spinMode.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

                boolean first = true;

                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    modeSpinnerPos = position;

                    //ignore first call
                    if(!first) {
                        spin.getOnItemSelectedListener().onItemSelected(spin, spin, spinnerPos, 0);
                    }

                    first = false;
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {

                }
            });

            spin.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    String item = ((ArrayAdapter<String>) spin.getAdapter()).getItem(position);

                    spinnerPos = position;

                    TextView tv = (TextView) parent.getChildAt(0);
                    if(tv != null)
                        tv.setTextColor(Color.parseColor(GGApp.GG_APP.isDarkThemeEnabled() ? "#e7e7e7" : "#212121"));


                    if (!item.equals(getActivity().getString(R.string.all))) {
                        cardColorIndex = 0;
                        Filter.FilterList fl = new Filter.FilterList();
                        Filter main = new Filter();
                        fl.mainFilter = main;
                        main.setType(Filter.FilterType.CLASS);
                        main.setFilter(item);
                        sla.updateData(plan.filter(fl), SubstListAdapter.PLAIN, true, item);

                    } else {
                        cardColorIndex = 0;

                        boolean sortByLesson = spinMode.getSelectedItemPosition() == 1;
                        sla.updateData(plan, sortByLesson ? SubstListAdapter.ALL_LESSONS : SubstListAdapter.ALL_CLASSES, true);

                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                    l3.removeAllViews();
                }
            });

        }

        l0.addView(l);
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

    /**
     * Converts a date to a better readable string
     * e.g. "Mittwoch, 08. Juli"
     *
     * @param date
     * @return
     */
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
