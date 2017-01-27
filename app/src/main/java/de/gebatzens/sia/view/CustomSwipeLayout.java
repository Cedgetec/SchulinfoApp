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

package de.gebatzens.sia.view;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.ListView;

import de.gebatzens.sia.FragmentData;
import de.gebatzens.sia.SIAApp;
import de.gebatzens.sia.MainActivity;
import de.gebatzens.sia.fragment.ExamFragment;
import de.gebatzens.sia.fragment.NewsFragment;
import de.gebatzens.sia.fragment.SubstFragment;
import de.gebatzens.sia.fragment.SubstPagerFragment;

public class CustomSwipeLayout extends SwipeRefreshLayout {

    private int touchSlop;
    private float prevX;

    public CustomSwipeLayout(Context context, AttributeSet attrs) {
        super(context, attrs);

        touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();

    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                MotionEvent m = MotionEvent.obtain(event);
                prevX = m.getX();
                m.recycle();
                return super.onInterceptTouchEvent(event);
            case MotionEvent.ACTION_MOVE:
                if(SIAApp.GG_APP.school.fragments.get(SIAApp.GG_APP.getFragmentIndex()).getType() == FragmentData.FragmentType.PLAN) {
                        float xd = Math.abs(event.getX() - prevX);
                        if (xd > touchSlop)
                            return false;
                }

        }

        return super.onInterceptTouchEvent(event);
    }

    @Override
    public boolean canChildScrollUp() {
        switch(SIAApp.GG_APP.school.fragments.get(SIAApp.GG_APP.getFragmentIndex()).getType()) {
            case PLAN:
                ViewPager vp = ((SubstFragment) ((MainActivity) getContext()).mContent).mViewPager;
                if(vp == null)
                    return false;
                SubstPagerFragment frag = (SubstPagerFragment) vp.getAdapter().instantiateItem(vp, vp.getCurrentItem());
                RecyclerView rv1 = frag.recyclerView;
                return rv1 == null || rv1.canScrollVertically(-1);
            case MENSA:
            case EXAMS:
                RecyclerView rv2 = ((ExamFragment) ((MainActivity) getContext()).mContent).recyclerView;
                return rv2 == null || rv2.canScrollVertically(-1);
            case NEWS:
                ListView lv2 = ((NewsFragment) ((MainActivity) getContext()).mContent).lv;
                if(lv2 == null)
                    return true;
                if(lv2.getChildCount() == 0)
                    return true;

                View c = lv2.getChildAt(0);
                int i = -c.getTop() + lv2.getFirstVisiblePosition() * c.getHeight();

                return i != 0;
            case PDF:
                return true;
        }

        return false;
    }

}
