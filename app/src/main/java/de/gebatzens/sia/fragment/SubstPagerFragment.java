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

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import java.util.Date;

import de.gebatzens.sia.FragmentData;
import de.gebatzens.sia.GGApp;
import de.gebatzens.sia.R;
import de.gebatzens.sia.data.GGPlan;

public class SubstPagerFragment extends RemoteDataFragment {

    public static final int INDEX_OVERVIEW = -2, INDEX_INVALID = -1;

    public static final int CARD_CLASS = 1, CARD_LESSON = 2;

    GGPlan plan;
    int index = INDEX_INVALID;
    int spinnerPos = 0, modeSpinnerPos = 0;

    public RecyclerView recyclerView;
    ViewGroup vg;

    @Override
    public void updateFragment() {
        switch(index) {
            case INDEX_OVERVIEW:
                if(recyclerView != null) {
                    ((SubstListAdapter) recyclerView.getAdapter()).setToOverview();
                } else {
                    if(vg != null) {
                        vg.removeAllViews();
                        createView(LayoutInflater.from(getContext()), vg);
                    }
                }
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

                if(recyclerView != null) {
                    ((SubstListAdapter) recyclerView.getAdapter()).updateData(plan, SubstListAdapter.ALL_CLASSES, true);
                }

                break;
        }
    }

    @Override
    public void createView(final LayoutInflater inflater, ViewGroup group) {
        vg = group;

        LinearLayout l = new LinearLayout(getActivity());
        l.setOrientation(LinearLayout.VERTICAL);
        l.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        if(index == INDEX_INVALID) {
            /*TextView tv = new TextView(getActivity());
            tv.setText("Error: " + type);
            l.addView(tv);
            Log.w("ggvp", "bundle " + type + " " + this + " " + getParentFragment());*/
            throw new IllegalArgumentException(("index is INDEX_INVALID"));
        } else if(index == INDEX_OVERVIEW) {
            // Overview

            recyclerView = (RecyclerView) inflater.inflate(R.layout.basic_recyclerview, l, false);
            recyclerView.setPadding(0,0,0,toPixels(5));
            final SubstListAdapter sla = new SubstListAdapter(this);
            recyclerView.setAdapter(sla);
            recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

            l.addView(recyclerView);
            sla.setToOverview();

            group.addView(l);
        } else {
            final LinearLayout l4 = new LinearLayout(getActivity());
            l4.setOrientation(LinearLayout.VERTICAL);
            l.addView(l4);

            recyclerView = (RecyclerView) inflater.inflate(R.layout.basic_recyclerview, l4, false);
            recyclerView.setPadding(0,0,0,toPixels(5));
            final SubstListAdapter sla = new SubstListAdapter(this);
            recyclerView.setAdapter(sla);
            recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
            sla.updateData(plan, SubstListAdapter.ALL_CLASSES, true);

            l4.addView(recyclerView);

            group.addView(l);
        }

        recyclerView.scrollToPosition(getArguments() != null ? getArguments().getInt("recyclerview_scroll", 0) : 0);

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

        Log.d("ggvp", "SUBST PAGER FRAGMENT: " + (bundle != null ? bundle.getInt("recyclerview_scroll") : -123));

        LinearLayout l = new LinearLayout(getActivity());
        l.setOrientation(LinearLayout.VERTICAL);
        if(getFragment().getData() != null)
            createRootView(inflater, l);
        return l;
    }

    @Override
    public void onSaveInstanceState(Bundle b) {
        super.onSaveInstanceState(b);

        if(recyclerView != null) {
            b.putParcelable("spf_scroll", recyclerView.getLayoutManager().onSaveInstanceState());
        }
    }

    @Override
    public void onActivityCreated(final Bundle b) {
        super.onActivityCreated(b);

        if(b != null) {
            recyclerView.post(new Runnable() {
                @Override
                public void run() {
                    recyclerView.getLayoutManager().onRestoreInstanceState(b.getParcelable("sfp_scroll"));
                }
            });

        }
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
