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

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

import de.gebatzens.ggvertretungsplan.GGApp;
import de.gebatzens.ggvertretungsplan.MainActivity;
import de.gebatzens.ggvertretungsplan.R;
import de.gebatzens.ggvertretungsplan.data.GGPlan;

public class SubstAdapter extends FragmentPagerAdapter {

    SubstPagerFragment overview;
    List<SubstPagerFragment> fragments = new ArrayList<SubstPagerFragment>();
    MainActivity mActivity;

    public SubstAdapter(Fragment m, Bundle savedState, MainActivity ma) {
        super(m.getChildFragmentManager());
        createFragments();
        mActivity = ma;

    }

    private void createFragments() {
        overview = new SubstPagerFragment();
        overview.setParams(SubstPagerFragment.INDEX_OVERVIEW);

        fragments.clear();
        if(GGApp.GG_APP.plans != null) {
            for (int i = 0; i < GGApp.GG_APP.plans.size(); i++) {
                SubstPagerFragment frag = new SubstPagerFragment();
                frag.setParams(i);
                fragments.add(frag);
            }
            notifyDataSetChanged();
        }

    }

    public void updateFragments() {
        overview.setParams(SubstPagerFragment.INDEX_OVERVIEW);
        overview.updateFragment();

        if(GGApp.GG_APP.plans != null) {
            if(fragments.size() != GGApp.GG_APP.plans.size()) {
                for(int i = fragments.size(); i < GGApp.GG_APP.plans.size(); i++)
                    fragments.add(new SubstPagerFragment());

                notifyDataSetChanged();
            }

            for(int i = 0; i < fragments.size(); i++) {
                SubstPagerFragment frag = fragments.get(i);
                frag.setParams(i);
                frag.updateFragment();

            }
        }
        ((SubstFragment)mActivity.mContent).mSlidingTabLayout.setViewPager(((SubstFragment)mActivity.mContent).mViewPager);
    }

    public void setFragmentsLoading() {
        overview.setFragmentLoading();
        for(SubstPagerFragment f : fragments)
            f.setFragmentLoading();
        ((SubstFragment)mActivity.mContent).mSlidingTabLayout.setViewPager(((SubstFragment)mActivity.mContent).mViewPager);
    }

    @Override
    public Fragment getItem(int position) {
        switch(position) {
            case 0:
                return overview;
            default:
                return fragments.get(position - 1);
        }
    }

    @Override
    public Object instantiateItem(ViewGroup view, int pos) {
        Object o = super.instantiateItem(view, pos);
        ((SubstPagerFragment)o).setParams(pos == 0 ? SubstPagerFragment.INDEX_OVERVIEW : pos - 1);
        return o;
    }

    @Override
    public CharSequence getPageTitle(int p) {
        switch(p) {
            case 0:
                return GGApp.GG_APP.getResources().getString(R.string.overview);
            default:
                return fragments.get(p - 1).plan.getWeekday();
        }
    }

    @Override
    public int getCount() {
        if(GGApp.GG_APP.plans == null)
            return 1;
        else
            return GGApp.GG_APP.plans.size() + 1;
    }

}
