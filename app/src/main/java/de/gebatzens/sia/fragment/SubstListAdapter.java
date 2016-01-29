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

import android.graphics.Color;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import de.gebatzens.sia.FragmentData;
import de.gebatzens.sia.GGApp;
import de.gebatzens.sia.R;
import de.gebatzens.sia.data.Filter;
import de.gebatzens.sia.data.GGPlan;

public class SubstListAdapter extends RecyclerView.Adapter {

    /**
     * A list without any labels
     */
    public static final int PLAIN = 0;

    /**
     * A list with class labels
     */
    public static final int ALL_CLASSES = 1;

    /**
     * A list with lesson labels
     */
    public static final int ALL_LESSONS = 2;

    /**
     * Multiple lists for every day with multiple labels
     */
    public static final int OVERVIEW = 3;

    ArrayList<AdapterEntry> entries;
    int type;
    SubstPagerFragment frag;

    public SubstListAdapter(SubstPagerFragment f) {
        this.frag = f;
        entries = new ArrayList<>();
        type = PLAIN;
    }

    public void updateData(GGPlan plan, int type, boolean messages) {
        updateData(plan, type, messages, null);
    }

    public void updateData(GGPlan plan, int type, boolean messages, String header) {
        this.type = type;
        entries.clear();

        if(header != null) {
            AdapterEntry ae = new AdapterEntry();
            ae.data = header;
            ae.type = AdapterEntry.LABEL;
            entries.add(ae);
        }

        if(messages) {
            AdapterEntry me = new AdapterEntry();
            me.type = AdapterEntry.MESSAGES;
            me.data = plan.special;
            entries.add(me);
        }

        if(plan.size() == 0) {
            AdapterEntry ne = new AdapterEntry();
            ne.type = AdapterEntry.NO_ENTRIES;
            entries.add(ne);
        }

        switch(type) {
            case PLAIN:
                for(GGPlan.Entry e : plan) {
                    AdapterEntry ae = new AdapterEntry();
                    ae.data = new Object[] {e, SubstPagerFragment.CARD_LESSON};
                    ae.type = AdapterEntry.ENTRY;
                    entries.add(ae);
                }
                break;
            case ALL_LESSONS:
                List<String> lessons = plan.getAllLessons();
                for(String lesson : lessons) {
                    AdapterEntry ae = new AdapterEntry();
                    ae.type = AdapterEntry.LABEL;
                    ae.data = lesson + ". " + GGApp.GG_APP.getString(R.string.lhour);
                    entries.add(ae);

                    Filter.FilterList fl = new Filter.FilterList();
                    fl.including.add(new Filter.IncludingFilter(Filter.FilterType.LESSON, lesson));
                    for(GGPlan.Entry e : plan.filter(fl)) {
                        ae = new AdapterEntry();
                        ae.data = new Object[] {e, SubstPagerFragment.CARD_CLASS};
                        ae.type = AdapterEntry.ENTRY;
                        entries.add(ae);

                    }
                }
                break;
            case ALL_CLASSES:
                List<String> classes = plan.getAllClasses();
                for(String cl : classes) {
                    AdapterEntry ae = new AdapterEntry();
                    ae.type = AdapterEntry.LABEL;
                    ae.data = cl;
                    entries.add(ae);

                    Filter.FilterList fl = new Filter.FilterList();
                    fl.including.add(new Filter.IncludingFilter(Filter.FilterType.CLASS, cl));
                    for(GGPlan.Entry e : plan.filter(fl)) {
                        ae = new AdapterEntry();
                        ae.data = new Object[] {e, SubstPagerFragment.CARD_LESSON};
                        ae.type = AdapterEntry.ENTRY;
                        entries.add(ae);

                    }
                }
                break;
        }

        notifyDataSetChanged();
    }

    public void setToOverview() {
        this.type = OVERVIEW;
        entries.clear();

        GGPlan.GGPlans plans = (GGPlan.GGPlans) GGApp.GG_APP.school.fragments.getData(FragmentData.FragmentType.PLAN).get(0).getData();
        Filter.FilterList filter = GGApp.GG_APP.filters;

        for(GGPlan pl : plans) {
            AdapterEntry date = new AdapterEntry();
            date.type = AdapterEntry.LABEL;
            date.data = translateDay(pl.date);
            entries.add(date);

            AdapterEntry me = new AdapterEntry();
            me.type = AdapterEntry.MESSAGES;
            me.data = pl.special;
            entries.add(me);

            GGPlan filtered = pl.filter(filter);

            for(Filter.IncludingFilter ifi : filter.including) {
                AdapterEntry clLabel = new AdapterEntry();
                clLabel.type = AdapterEntry.LABEL;
                clLabel.data = ifi.getFilter();
                entries.add(clLabel);

                if(filtered.size() == 0) {
                    AdapterEntry ne = new AdapterEntry();
                    ne.type = AdapterEntry.NO_ENTRIES;
                    entries.add(ne);
                }

                for(GGPlan.Entry e : filtered) {
                    if (ifi.matches(e)) {
                        AdapterEntry ae = new AdapterEntry();
                        ae.data = new Object[] {e, ifi.getType() != Filter.FilterType.CLASS ? SubstPagerFragment.CARD_CLASS | SubstPagerFragment.CARD_LESSON : SubstPagerFragment.CARD_LESSON};
                        ae.type = AdapterEntry.ENTRY;
                        entries.add(ae);
                    }
                }
            }
        }
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        switch(viewType) {
            case AdapterEntry.ENTRY:
                return frag.createCardItem(LayoutInflater.from(parent.getContext()), parent);
            case AdapterEntry.LABEL:
                LinearLayout wrapper = new LinearLayout(frag.getActivity());
                TextView tv = frag.createTextView("", 27, LayoutInflater.from(parent.getContext()), wrapper);
                tv.setId(R.id.label);
                tv.setPadding(RemoteDataFragment.toPixels(2.8f), RemoteDataFragment.toPixels(20), 0, 0);
                tv.setTextColor(Color.parseColor(GGApp.GG_APP.isDarkThemeEnabled() ? "#a0a0a0" : "#6e6e6e"));
                return new LabelViewHolder(wrapper);
            case AdapterEntry.MESSAGES:
                return frag.createSMCard(parent, LayoutInflater.from(parent.getContext()));
            case AdapterEntry.NO_ENTRIES:
                LinearLayout wr = new LinearLayout(frag.getActivity());
                wr.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                frag.createNoEntriesCard(wr, LayoutInflater.from(frag.getActivity()));
                return new RecyclerView.ViewHolder(wr) {};
            default:
                return null;
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        AdapterEntry ae = entries.get(position);
        switch(ae.type) {
            case AdapterEntry.ENTRY:
                Object[] data = (Object[]) ae.data;
                ((SubstViewHolder) holder).update((GGPlan.Entry) data[0], (int) data[1]);
                break;
            case AdapterEntry.LABEL:
                ((LabelViewHolder) holder).update((String) ae.data);
                break;
            case AdapterEntry.MESSAGES:
                ((MessageViewHolder) holder).update((List<String>) ae.data);
                break;
            case AdapterEntry.NO_ENTRIES:
                break;
        }

    }

    @Override
    public int getItemViewType(int position) {
        return entries.get(position).type;
    }

    @Override
    public int getItemCount() {
        return entries.size();
    }

    public static class SubstViewHolder extends RecyclerView.ViewHolder {
        CardView cv;
        TextView hour, subject, header, detail;

        public SubstViewHolder(View v) {
            super(v);
            cv = (CardView) v.findViewById(R.id.cvroot);
            hour = (TextView) v.findViewById(R.id.cv_hour);
            subject = (TextView) v.findViewById(R.id.cv_subject);
            header = (TextView) v.findViewById(R.id.cv_header);
            detail = (TextView) v.findViewById(R.id.cv_detail);
        }

        public void update(GGPlan.Entry entry, int type) {
            hour.setText((type & SubstPagerFragment.CARD_LESSON) != 0 ? entry.lesson : entry.clazz);
            header.setText(entry.type + (entry.teacher.isEmpty() ? "" : " [" + entry.teacher + "]"));
            TextView tv = detail;
            tv.setText(entry.comment + (entry.room.isEmpty() ? "" : (entry.comment.isEmpty() ? "" : "\n") + GGApp.GG_APP.getString(R.string.room) + " " + entry.room));
            if(tv.getText().toString().trim().isEmpty())
                tv.setVisibility(View.GONE);

            String subText = ((type & (SubstPagerFragment.CARD_LESSON | SubstPagerFragment.CARD_CLASS)) == (SubstPagerFragment.CARD_LESSON | SubstPagerFragment.CARD_CLASS) ? entry.clazz + " " : "") + entry.subject;

            if(subText.trim().isEmpty())
                subject.setVisibility(View.GONE);
            else
                subject.setText(Html.fromHtml(subText));
        }
    }

    public static class LabelViewHolder extends RecyclerView.ViewHolder {
        TextView tv;

        public LabelViewHolder(View l) {
            super(l);
            tv = (TextView) l.findViewById(R.id.label);
        }

        public void update(String text) {
            tv.setText(text);
        }
    }

    public static class MessageViewHolder extends RecyclerView.ViewHolder {
        LinearLayout linearLayout;
        CardView cv;

        public MessageViewHolder(View v) {
            super(v);

            cv = (CardView) v.findViewById(R.id.cvroot);
            linearLayout = (LinearLayout) v.findViewById(R.id.messages_list);
        }

        public void update(List<String> messages) {
            linearLayout.removeAllViews();
            for(TextView tv : SubstPagerFragment.createSMViews(messages, linearLayout)) {
                linearLayout.addView(tv);
            }

            cv.setVisibility(messages.size() == 0 ? View.GONE : View.VISIBLE);
        }
    }

    private class AdapterEntry {

        public static final int LABEL = 0, ENTRY = 1, MESSAGES = 2, NO_ENTRIES = 3;

        Object data;
        int type;

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

}
