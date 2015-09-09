/*
 * Copyright 2015 Lasse Rosenow
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

import android.content.Context;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import java.util.HashMap;

public class FirstUseAdapter extends FragmentPagerAdapter {

    public HashMap<Integer, FirstUseFragment> fragments = new HashMap<>();

    public FirstUseAdapter(FragmentManager fm, Context context) {
        super(fm);

    }

    @Override
    public int getCount() {
        return 4;
    }

    @Override
    public Fragment getItem(int position) {
        return FirstUseFragment.newInstance(position);
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return "";
    }

}
