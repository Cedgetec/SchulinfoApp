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

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;

import de.gebatzens.sia.SIAApp;
import de.gebatzens.sia.R;
import de.gebatzens.sia.data.Subst;

public class SubstAdapter extends FragmentStatePagerAdapter {

    ViewPager viewPager;
    Subst.GGPlans plans;
    SubstFragment fragment;
    
    public SubstAdapter(SubstFragment m, Bundle savedState, ViewPager vp) {
        super(m.getChildFragmentManager());
        this.viewPager = vp;
        plans = (Subst.GGPlans) m.getFragment().getData();
        SIAApp.GG_APP.activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                notifyDataSetChanged();
            }
        });
        this.fragment = m;

    }

    public void update(Subst.GGPlans pl) {
        final int os = plans == null ? 1 : plans.size() + 1;
        plans = pl;

        for(int i = 0; i < os; i++) {
            ((SubstPagerFragment) instantiateItem(viewPager, i)).updateFragment();
        }
        notifyDataSetChanged();

    }

    public SubstPagerFragment getOverview() {
        return (SubstPagerFragment) instantiateItem(viewPager, 0);
    }

    public SubstPagerFragment getFragment(Subst plan) {
        return (SubstPagerFragment) instantiateItem(viewPager, plans.indexOf(plan) + 1);
    }

    public void setFragmentsLoading() {
        getOverview().setFragmentLoading();
        for(Subst p : plans)
            getFragment(p).setFragmentLoading();
    }

    @Override
    public Fragment getItem(int position) {
        SubstPagerFragment fragment = new SubstPagerFragment();
        Bundle params = new Bundle();
        params.putInt("fragment", SIAApp.GG_APP.school.fragments.indexOf(this.fragment.getFragment()));
        if(position == 0)
            params.putInt("index", SubstPagerFragment.INDEX_OVERVIEW);
        else
            params.putInt("index", position - 1);
        fragment.setRetainInstance(true);
        fragment.setArguments(params);
        return fragment;
    }

    @Override
    public CharSequence getPageTitle(int p) {
        switch(p) {
            case 0:
                return SIAApp.GG_APP.getResources().getString(R.string.overview);
            default:
                return plans.get(p - 1).getWeekday();
        }
    }

    @Override
    public int getItemPosition(Object o) {
        SubstPagerFragment frag = (SubstPagerFragment) o;
        if(frag.index == SubstPagerFragment.INDEX_OVERVIEW) {
            frag.updateFragment();
            return 0;
        } else if(frag.index == SubstPagerFragment.INDEX_INVALID)
            return POSITION_NONE;
        else {
            int i = plans.indexOf(frag.plan);
            if(i >= 0) {
                return i + 1;
            } else
                return POSITION_NONE;
        }

    }

    @Override
    public int getCount() {
        if(plans == null)
            return 1;
        else
            return plans.size() + 1;
    }

}
