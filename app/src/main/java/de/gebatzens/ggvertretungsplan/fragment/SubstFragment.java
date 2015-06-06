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
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import de.gebatzens.ggvertretungsplan.GGApp;
import de.gebatzens.ggvertretungsplan.MainActivity;
import de.gebatzens.ggvertretungsplan.R;

public class SubstFragment extends RemoteDataFragment {

    public Toolbar mToolbar;
    public ViewPager mViewPager;
    public SubstAdapter substAdapter;
    public SwipeRefreshLayout swipeContainer;
    public Bundle bundle;
    public TabLayout mTabLayout;

    public SubstFragment() {

    }

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_subst, container, false);
    }

    private int toPixels(int dp) {
        float scale = getResources().getDisplayMetrics().density;
        return (int) (dp * scale + 0.5f);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        bundle = ((MainActivity) getActivity()).savedState;
        mViewPager = (ViewPager) view.findViewById(R.id.viewpager);
        substAdapter = new SubstAdapter(this, savedInstanceState, mViewPager);
        /*if(bundle != null) {
            for(int i = 0; i < substAdapter.fragments.size(); i++)
                substAdapter.fragments.get(i).spinnerPos = bundle.getInt("ggvp_frag_spinner_" + i);
        }*/
        mViewPager.setAdapter(substAdapter);
        mViewPager.setOffscreenPageLimit(3);
        if(bundle != null)
            mViewPager.setCurrentItem(bundle.getInt("ggvp_tab"));

        mToolbar = (Toolbar) ((MainActivity) this.getActivity()).mToolbar;
        ColorDrawable mToolbarColor = (ColorDrawable) mToolbar.getBackground();
        int mToolbarColorId = mToolbarColor.getColor();

        mTabLayout = (TabLayout) view.findViewById(R.id.tab_layout);
        mTabLayout.setBackgroundColor(mToolbarColorId);
        mTabLayout.setupWithViewPager(mViewPager);
        if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
            mTabLayout.setPadding(toPixels(48), 0, toPixels(48), 0);
            mTabLayout.setTabMode(TabLayout.MODE_SCROLLABLE);
        }
        else if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT){
            mTabLayout.setTabMode(TabLayout.MODE_FIXED);
        }

        swipeContainer = (SwipeRefreshLayout) view.findViewById(R.id.refresh);
        if (themeIsLight()) {
            swipeContainer.setProgressBackgroundColorSchemeColor(Color.parseColor("#ffffff"));
        } else{
            swipeContainer.setProgressBackgroundColorSchemeColor(Color.parseColor("#424242"));
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
                }, true, GGApp.FragmentType.PLAN);
            }
        });
        // Configure the refreshing colors
        swipeContainer.setColorSchemeResources(R.color.custom_material_green,
                R.color.custom_material_red,
                R.color.custom_material_blue,
                R.color.custom_material_orange);

        FrameLayout contentFrame = (FrameLayout) getActivity().findViewById(R.id.content_fragment);
        contentFrame.setVisibility(View.VISIBLE);
        LinearLayout fragmentLayout = (LinearLayout) getActivity().findViewById(R.id.fragment_layout);
        Animation fadeIn = AnimationUtils.loadAnimation(getActivity().getApplicationContext(), R.anim.fade_in);
        fragmentLayout.startAnimation(fadeIn);

    }

    @Override
    public void saveInstanceState(Bundle b) {

        /*for(int i = 0; i < substAdapter.fragments.size(); i++)
            b.putInt("ggvp_frag_spinner_" + i, substAdapter.fragments.get(i).spinnerPos);*/
        b.putInt("ggvp_tab", mViewPager.getCurrentItem());
    }

    @Override
    public void createView(LayoutInflater inflater, ViewGroup vg) {

    }

    @Override
    public ViewGroup getContentView() {
        return null;
    }

    @Override
    public void setFragmentLoading() {
        substAdapter.setFragmentsLoading();
    }

    @Override
    public void updateFragment() {
        //for(SubstPagerFragment frag : substAdapter.fragments)
         //   frag.spinnerPos = 0;
        if(substAdapter != null)
            substAdapter.notifyDataSetChanged();
    }
}
