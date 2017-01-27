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

import android.content.res.Configuration;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import java.util.Date;
import java.util.List;

import de.gebatzens.sia.GGApp;
import de.gebatzens.sia.MainActivity;
import de.gebatzens.sia.R;
import de.gebatzens.sia.data.Subst;

public class SubstFragment extends RemoteDataFragment {

    public ViewPager mViewPager;
    public SubstAdapter substAdapter;
    public Bundle bundle;
    public TabLayout mTabLayout;

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        ((MainActivity) getActivity()).updateMenu(R.menu.toolbar_menu);
        return inflater.inflate(R.layout.fragment_subst, container, false);
    }

    private int toPixels(int dp) {
        float scale = getResources().getDisplayMetrics().density;
        return (int) (dp * scale + 0.5f);
    }

    public void updateTime(Date newTime) {
        List<Fragment> frags = getChildFragmentManager().getFragments();
        if(frags != null) {
            for(Fragment fr : frags) {
                if(fr != null) {
                    View v = fr.getView();
                    if (v != null) {
                        v = v.findViewWithTag("gg_time");
                        if (v != null) {
                            String diff = SubstPagerFragment.getTimeDiff(getActivity(), newTime);
                            TextView tv = (TextView) v;
                            tv.setText(diff);
                        }
                    }
                }
            }
        }
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        bundle = ((MainActivity) getActivity()).savedState;
        mViewPager = (ViewPager) view.findViewById(R.id.viewpager);
        substAdapter = new SubstAdapter(this, savedInstanceState, mViewPager);
        /*if(bundle != null) {
            for(int i = 0; i < substAdapter.fragments.size(); i++)
                substAdapter.fragments.get(i).spinnerPos = bundle.getInt("ggvp_frag_spinner_" + i);
        }*/
        mViewPager.setAdapter(substAdapter);
        mViewPager.setOffscreenPageLimit(2);
        if(bundle != null)
            mViewPager.setCurrentItem(bundle.getInt("ggvp_tab"));

        mTabLayout = (TabLayout) view.findViewById(R.id.tab_layout);
        mTabLayout.setupWithViewPager(mViewPager);
        if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            mTabLayout.setPadding(toPixels(48), 0, toPixels(48), 0);
            mTabLayout.setTabMode(TabLayout.MODE_SCROLLABLE);
        } else if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            mTabLayout.setTabMode(TabLayout.MODE_FIXED);
        }

        FrameLayout contentFrame = (FrameLayout) getActivity().findViewById(R.id.content_fragment);
        contentFrame.setVisibility(View.VISIBLE);

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
        GGApp.GG_APP.activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(substAdapter != null) {
                    substAdapter.update((Subst.GGPlans) getFragment().getData());
                    mTabLayout.setupWithViewPager(mViewPager);
                }
            }
        });

    }

    public void resetScrollPositions() {
        List<Fragment> frags = getChildFragmentManager().getFragments();
        if(frags != null) {
            for (Fragment fr : frags) {
                if(fr != null) {
                    View v = fr.getView();
                    if (v != null) {
                        v = v.findViewWithTag("gg_scroll");
                        if (v != null) {
                            v.setScrollY(0);
                        }
                    }
                }
            }
        }
    }
}
