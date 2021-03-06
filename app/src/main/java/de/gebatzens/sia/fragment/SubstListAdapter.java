/*
 * Copyright 2016 - 2017 Hauke Oldsen
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

import android.content.Intent;
import android.graphics.Color;
import android.support.v7.widget.AppCompatSpinner;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import de.gebatzens.sia.FilterActivity;
import de.gebatzens.sia.FragmentData;
import de.gebatzens.sia.SIAApp;
import de.gebatzens.sia.MainActivity;
import de.gebatzens.sia.R;
import de.gebatzens.sia.data.Filter;
import de.gebatzens.sia.data.Subst;

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
    int cardColorIndex = 0;

    boolean updateHeader = false;

    public SubstListAdapter(SubstPagerFragment f) {
        this.frag = f;
        entries = new ArrayList<>();
        type = PLAIN;
    }

    public void updateData(Subst plan, int type, boolean messages) {
        updateData(plan, type, messages, null);
    }

    public void updateData(Subst plan, int type, boolean messages, String header) {
        this.type = type;
        entries.clear();

        AdapterEntry hc = new AdapterEntry();
        hc.type = AdapterEntry.HEADER_SPINNER;
        entries.add(hc);
        updateHeader = true;

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
                for(Subst.Entry e : plan) {
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
                    ae.data = lesson + ". " + SIAApp.SIA_APP.getString(R.string.lhour);
                    entries.add(ae);

                    Filter.FilterList fl = new Filter.FilterList();
                    fl.including.add(new Filter.IncludingFilter(Filter.FilterType.LESSON, lesson));
                    for(Subst.Entry e : plan.filter(fl)) {
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
                    for(Subst.Entry e : plan.filter(fl)) {
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

        if(SIAApp.SIA_APP.filters.including.size() > 0) {
            AdapterEntry header = new AdapterEntry();
            header.type = AdapterEntry.HEADER;
            entries.add(header);
            updateHeader = true;

            Subst.GGPlans plans = (Subst.GGPlans) SIAApp.SIA_APP.school.fragments.getByType(FragmentData.FragmentType.PLAN).get(0).getData();
            Filter.FilterList filter = SIAApp.SIA_APP.filters;

            for (Subst pl : plans) {
                AdapterEntry date = new AdapterEntry();
                date.type = AdapterEntry.LABEL;
                date.data = translateDay(pl.date);
                entries.add(date);

                AdapterEntry me = new AdapterEntry();
                me.type = AdapterEntry.MESSAGES;
                me.data = pl.special;
                entries.add(me);

                Subst filtered = pl.filter(filter);

                for (Filter.IncludingFilter ifi : filter.including) {
                    if (filter.including.size() > 1) {
                        AdapterEntry clLabel = new AdapterEntry();
                        clLabel.type = AdapterEntry.LABEL;
                        clLabel.data = ifi.getFilter();
                        entries.add(clLabel);
                    }

                    Filter.FilterList clist = new Filter.FilterList();
                    clist.including.add(ifi);

                    Subst cf = filtered.filter(clist);

                    if (cf.size() == 0) {
                        AdapterEntry ne = new AdapterEntry();
                        ne.type = AdapterEntry.NO_ENTRIES;
                        entries.add(ne);
                    }

                    for (Subst.Entry e : cf) {
                        AdapterEntry ae = new AdapterEntry();
                        ae.data = new Object[]{e, ifi.getType() != Filter.FilterType.CLASS ? SubstPagerFragment.CARD_CLASS | SubstPagerFragment.CARD_LESSON : SubstPagerFragment.CARD_LESSON};
                        ae.type = AdapterEntry.ENTRY;
                        entries.add(ae);

                    }
                }
            }
        } else {
            AdapterEntry nf = new AdapterEntry();
            nf.type = AdapterEntry.NO_FILTER;
            entries.add(nf);
        }

        notifyDataSetChanged();
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        switch(viewType) {
            case AdapterEntry.ENTRY:
                return createCardItem(LayoutInflater.from(parent.getContext()), parent);
            case AdapterEntry.LABEL:
                return createLabel(parent);
            case AdapterEntry.MESSAGES:
                return createSMCard(parent, LayoutInflater.from(parent.getContext()));
            case AdapterEntry.NO_ENTRIES:
                LinearLayout wrapper = new LinearLayout(parent.getContext());
                wrapper.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                frag.createNoEntriesCard(wrapper, LayoutInflater.from(parent.getContext()));
                return new RecyclerView.ViewHolder(wrapper) {};
            case AdapterEntry.HEADER:
                HeaderViewHolder hv = createHeader(LayoutInflater.from(parent.getContext()));
                return hv;
            case AdapterEntry.HEADER_SPINNER:
                SpinnerHeaderViewHolder shv = createSpinnerHeader(LayoutInflater.from(parent.getContext()));
                return shv;
            case AdapterEntry.NO_FILTER:
                return createNoFilterCard(LayoutInflater.from(parent.getContext()), parent);
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
                ((SubstViewHolder) holder).update((Subst.Entry) data[0], (int) data[1]);
                break;
            case AdapterEntry.LABEL:
                ((LabelViewHolder) holder).update((String) ae.data);
                break;
            case AdapterEntry.MESSAGES:
                ((MessageViewHolder) holder).update((List<String>) ae.data);
                break;
            case AdapterEntry.NO_ENTRIES:
                break;
            case AdapterEntry.HEADER:
                ((HeaderViewHolder) holder).update(LayoutInflater.from(frag.getContext()));
                break;
            case AdapterEntry.HEADER_SPINNER:
                ((SpinnerHeaderViewHolder) holder).update();
                break;
            case AdapterEntry.NO_FILTER:
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

    public class SubstViewHolder extends RecyclerView.ViewHolder {
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

        public void update(final Subst.Entry entry, int type) {
            hour.setText((type & SubstPagerFragment.CARD_LESSON) != 0 ? entry.lesson : entry.clazz);
            header.setText(entry.type + (entry.teacher.isEmpty() ? "" : " [" + entry.teacher + "]"));
            String detailText = (entry.comment + (entry.room.isEmpty() ? "" : (entry.comment.isEmpty() ? "" : "\n") + SIAApp.SIA_APP.getString(R.string.room) + " " + entry.room)).trim();
            if(detailText.isEmpty()) {
                detail.setVisibility(View.GONE);
            } else {
                detail.setText(detailText);
                detail.setVisibility(View.VISIBLE);
            }

            String subText = ((type & (SubstPagerFragment.CARD_LESSON | SubstPagerFragment.CARD_CLASS)) == (SubstPagerFragment.CARD_LESSON | SubstPagerFragment.CARD_CLASS) ? entry.clazz + " " : "") + entry.subject;

            if(subText.trim().isEmpty())
                subject.setVisibility(View.GONE);
            else
                subject.setText(Html.fromHtml(subText));

            cv.setForeground(entry.markedForSharing ? frag.getActivity().getResources().getDrawable(R.drawable.share_foreground) : null);

            final MainActivity ma = (MainActivity) frag.getActivity();

            cv.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    if(entry.isMarked()) {
                        cv.setForeground(null);
                        entry.setMarked(false);
                        ma.removeShareable(entry);
                    } else {
                        cv.setForeground(frag.getActivity().getResources().getDrawable(R.drawable.share_foreground));
                        entry.setMarked(true);
                        ma.addShareable(entry);
                    }

                    return true;
                }
            });

            cv.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(entry.isMarked()) {
                        entry.setMarked(false);
                        cv.setForeground(null);
                        ma.removeShareable(entry);
                    } else if(ma.getNumberOfMarkedItems() > 0) {
                        cv.setForeground(frag.getActivity().getResources().getDrawable(R.drawable.share_foreground));
                        entry.setMarked(true);
                        ma.addShareable(entry);
                    }
                }
            });
        }
    }

    public class LabelViewHolder extends RecyclerView.ViewHolder {
        TextView tv;

        public LabelViewHolder(View l) {
            super(l);
            tv = (TextView) l.findViewById(R.id.label);
        }

        public void update(String text) {
            tv.setText(text);
        }
    }

    public class MessageViewHolder extends RecyclerView.ViewHolder {
        LinearLayout linearLayout;
        CardView cv;

        public MessageViewHolder(View v) {
            super(v);

            cv = (CardView) v.findViewById(R.id.cvroot);
            linearLayout = (LinearLayout) v.findViewById(R.id.messages_list);
        }

        public void update(List<String> messages) {
            linearLayout.removeAllViews();
            for(TextView tv : createSMViews(messages, linearLayout)) {
                linearLayout.addView(tv);
            }

            cv.setVisibility(messages.size() == 0 ? View.GONE : View.VISIBLE);
        }
    }

    private class AdapterEntry {

        public static final int LABEL = 7, ENTRY = 1, MESSAGES = 2, NO_ENTRIES = 3, HEADER = 4, HEADER_SPINNER = 5, NO_FILTER = 6;

        Object data;
        int type;

    }

    public SubstListAdapter.SubstViewHolder createCardItem(LayoutInflater i, ViewGroup parent) {
        LinearLayout wrapper = new LinearLayout(parent.getContext());
        wrapper.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        frag.setOrientationPadding(wrapper);

        CardView cv = (CardView) i.inflate(R.layout.basic_cardview, wrapper, false);
        cv.setId(R.id.cvroot);
        String[] colors = SIAApp.SIA_APP.getResources().getStringArray(SIAApp.SIA_APP.school.getColorArray());
        cv.setCardBackgroundColor(Color.parseColor(colors[cardColorIndex]));
        cardColorIndex++;
        if(cardColorIndex == colors.length)
            cardColorIndex = 0;
        i.inflate(R.layout.cardview_entry, cv, true);
        wrapper.addView(cv);

        return new SubstListAdapter.SubstViewHolder(wrapper);
    }

    public static ArrayList<TextView> createSMViews(List<String> messages, ViewGroup parent) {
        ArrayList<TextView> tvl = new ArrayList<>();

        for(String special : messages) {
            TextView tv2 = new TextView(parent.getContext());
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            params.setMargins(0, RemoteDataFragment.toPixels(2), 0, 0);
            tv2.setLayoutParams(params);
            tv2.setText(Html.fromHtml(special));
            tv2.setTextSize(15);
            tv2.setTextColor(Color.WHITE);
            tvl.add(tv2);

        }

        return tvl;
    }

    public SubstListAdapter.MessageViewHolder createSMCard(ViewGroup parent, LayoutInflater inflater) {
        LinearLayout wrapper = new LinearLayout(parent.getContext());
        wrapper.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        frag.setOrientationPadding(wrapper);

        CardView cv = (CardView) inflater.inflate(R.layout.basic_cardview, wrapper, false);
        cv.setId(R.id.cvroot);
        cv.setCardBackgroundColor(SIAApp.SIA_APP.school.getColor());
        LinearLayout ls = new LinearLayout(frag.getActivity());
        ls.setId(R.id.messages_list);
        ls.setOrientation(LinearLayout.VERTICAL);
        TextView tv3 = frag.createPrimaryTextView(frag.getResources().getString(R.string.special_messages), 19, inflater, ls);
        tv3.setTextColor(Color.WHITE);
        tv3.setPadding(0, 0, 0, RemoteDataFragment.toPixels(6));
        cv.addView(ls);
        wrapper.addView(cv);

        return new SubstListAdapter.MessageViewHolder(wrapper);

    }

    public SubstListAdapter.LabelViewHolder createLabel(ViewGroup parent) {
        LinearLayout wrapper = new LinearLayout(parent.getContext());
        wrapper.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        frag.setOrientationPadding(wrapper);

        TextView tv = frag.createSecondaryTextView("", 27, LayoutInflater.from(parent.getContext()), wrapper);
        tv.setId(R.id.label);
        tv.setPadding(RemoteDataFragment.toPixels(2.8f), RemoteDataFragment.toPixels(20), 0, 0);

        return new SubstListAdapter.LabelViewHolder(wrapper);
    }

    public SubstListAdapter.SubstViewHolder createNoFilterCard(LayoutInflater inflater, ViewGroup parent) {
        LinearLayout wrapper = new LinearLayout(parent.getContext());
        wrapper.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        frag.setOrientationPadding(wrapper);

        LinearLayout wrapper2 = new LinearLayout(parent.getContext());
        wrapper2.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        wrapper2.setPadding(0, RemoteDataFragment.toPixels(5), 0, 0);

        CardView cv = (CardView) inflater.inflate(R.layout.no_filter_cardview, wrapper, false);
        cv.findViewById(R.id.no_filter_card_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(SIAApp.SIA_APP.activity, FilterActivity.class);
                SIAApp.SIA_APP.activity.startActivityForResult(i, 1);
            }
        });

        wrapper2.addView(cv);
        wrapper.addView(wrapper2);
        parent.addView(wrapper);

        return new SubstListAdapter.SubstViewHolder(wrapper);
    }

    /**
     * Converts a date to a better readable string
     * e.g. "Mittwoch, 08. Juli"
     *
     * @param date
     * @return
     */
    public static String translateDay(Date date) {
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

    public class SpinnerHeaderViewHolder extends RecyclerView.ViewHolder {

        SubstPagerFragment frag;
        AppCompatSpinner spinner;

        public SpinnerHeaderViewHolder(View itemView, SubstPagerFragment frag) {
            super(itemView);
            this.frag = frag;
            spinner = (AppCompatSpinner) itemView.findViewById(R.id.spinner);
        }

        public void update() {
            ArrayList<String> list = new ArrayList<>();
            list.add(frag.getActivity().getString(R.string.all));
            list.addAll(frag.plan.getAllClasses());

            ArrayAdapter<String> adapter = (ArrayAdapter<String>) spinner.getAdapter();
            adapter.clear();
            adapter.addAll(list);
            adapter.notifyDataSetChanged();

        }

    }

    public class HeaderViewHolder extends RecyclerView.ViewHolder {

        LinearLayout l3;
        SubstPagerFragment frag;

        public HeaderViewHolder(View itemView, SubstPagerFragment frag) {
            super(itemView);
            this.frag = frag;
            l3 = (LinearLayout) itemView.findViewById(R.id.header_chips);

        }

        public void update(LayoutInflater inflater) {
            l3.removeAllViews();

            Filter.FilterList filters = SIAApp.SIA_APP.filters;
            int chars = 0;
            int chips = 0;

            for(Filter.IncludingFilter inc : filters.including) {
                String text = chars > 8 ? "+" + (filters.including.size() - chips) + "" : inc.getFilter();
                TextView tv2 = chars > 8 ? frag.createSecondaryTextView(text, 20, inflater, l3) : frag.createPrimaryTextView(text, 15, inflater, l3);
                LinearLayout.LayoutParams pa = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                pa.setMargins(SubstPagerFragment.toPixels(chars > 8 ? 10 : 5), 0, chars > 8 ? SubstPagerFragment.toPixels(-20) : 0, 0);
                tv2.setLayoutParams(pa);
                tv2.setIncludeFontPadding(false);
                if (chars <= 8) {
                    tv2.setBackgroundResource(R.drawable.chip_background);
                } else {
                    tv2.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            frag.startActivity(new Intent(frag.getContext(), FilterActivity.class));
                        }
                    });
                }

                if(chars > 8)
                    break;

                chars += inc.getFilter().length();
                chips++;

            }


        }
    }

    public HeaderViewHolder createHeader(LayoutInflater inflater) {
        CardView cv2 = new CardView(frag.getActivity());
        CardView.LayoutParams params = new CardView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, RemoteDataFragment.toPixels(5));
        cv2.setLayoutParams(params);
        cv2.setRadius(0);

        LinearLayout l2 = new LinearLayout(frag.getActivity());
        l2.setMinimumHeight(RemoteDataFragment.toPixels(50));
        l2.setGravity(Gravity.CENTER_VERTICAL);

        String diff = SubstPagerFragment.getTimeDiff(frag.getActivity(), ((Subst.GGPlans) frag.getFragment().getData()).loadDate);
        TextView tv4 = frag.createPrimaryTextView(diff, 13, inflater, l2);
        tv4.setTag("gg_time");
        tv4.setPadding(RemoteDataFragment.toPixels(16), RemoteDataFragment.toPixels(0), RemoteDataFragment.toPixels(16), RemoteDataFragment.toPixels(0));

        LinearLayout l3 = new LinearLayout(frag.getActivity());
        l3.setGravity(Gravity.END | Gravity.CENTER);
        l3.setPadding(0, 0, RemoteDataFragment.toPixels(16), 0);
        l3.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        l3.setId(R.id.header_chips);


        l2.addView(l3);
        cv2.addView(l2);

        return new HeaderViewHolder(cv2, frag);
    }

    public SpinnerHeaderViewHolder createSpinnerHeader(LayoutInflater inflater) {
        CardView cv2 = new CardView(frag.getActivity());
        CardView.LayoutParams params = new CardView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, RemoteDataFragment.toPixels(5));
        cv2.setLayoutParams(params);
        cv2.setRadius(0);

        LinearLayout l2 = new LinearLayout(frag.getActivity());
        l2.setMinimumHeight(RemoteDataFragment.toPixels(50));
        l2.setGravity(Gravity.CENTER_VERTICAL);

        String diff = SubstPagerFragment.getTimeDiff(frag.getActivity(), ((Subst.GGPlans) frag.getFragment().getData()).loadDate);
        TextView tv5 = frag.createPrimaryTextView(diff, 13, inflater, l2);
        tv5.setTag("gg_time");
        tv5.setPadding(RemoteDataFragment.toPixels(16), RemoteDataFragment.toPixels(0), RemoteDataFragment.toPixels(16), RemoteDataFragment.toPixels(0));

        LinearLayout l3 = new LinearLayout(frag.getActivity());
        l3.setGravity(Gravity.END | Gravity.CENTER);
        l3.setPadding(0, 0, RemoteDataFragment.toPixels(16), 0);
        l3.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        final AppCompatSpinner spinMode = new AppCompatSpinner(frag.getActivity());
        ArrayAdapter<String> modeAdapter = new ArrayAdapter<>(frag.getActivity(), android.R.layout.simple_spinner_item, new String[]{ frag.getString(R.string.classes), frag.getString(R.string.lessons)});
        modeAdapter.setDropDownViewResource(R.layout.support_simple_spinner_dropdown_item);
        spinMode.setAdapter(modeAdapter);
        l3.addView(spinMode);

        final AppCompatSpinner spinClass = new AppCompatSpinner(frag.getActivity());
        spinClass.setId(R.id.spinner);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(frag.getActivity(), android.R.layout.simple_spinner_item, new ArrayList<String>());
        adapter.setDropDownViewResource(R.layout.support_simple_spinner_dropdown_item);
        spinClass.setAdapter(adapter);


        l3.addView(spinClass);

        spinMode.setSelection(frag.modeSpinnerPos);
        spinClass.setSelection(frag.spinnerPos);

        l2.addView(l3);
        cv2.addView(l2);

        spinMode.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            boolean first = true;

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                frag.modeSpinnerPos = position;

                //ignore first call
                if(!first) {
                    spinClass.getOnItemSelectedListener().onItemSelected(spinClass, spinClass, frag.spinnerPos, 0);
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

                frag.spinnerPos = position;

                if (!item.equals(frag.getActivity().getString(R.string.all))) {
                    Filter.FilterList fl = new Filter.FilterList();
                    fl.including.add(new Filter.IncludingFilter(Filter.FilterType.CLASS, item));
                    updateData(frag.plan.filter(fl), SubstListAdapter.PLAIN, true, item);

                } else {
                    boolean sortByLesson = spinMode.getSelectedItemPosition() == 1;
                    updateData(frag.plan, sortByLesson ? SubstListAdapter.ALL_LESSONS : SubstListAdapter.ALL_CLASSES, true);

                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Is this possible?
                //l4.removeAllViews();
            }
        });

        return new SpinnerHeaderViewHolder(cv2, frag);
    }

}
