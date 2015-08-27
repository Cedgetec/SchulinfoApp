/*
 * Copyright 2015 Fabian Schultis
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

package de.gebatzens.ggvertretungsplan;

import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import de.gebatzens.ggvertretungsplan.fragment.FirstUseAdapter;
import de.gebatzens.ggvertretungsplan.fragment.FirstUsePager;

public class FirstUseActivity extends FragmentActivity {

    @Override
    protected void onCreate(Bundle bundle) {
        setTheme(R.style.AppThemeIndigoLight);
        super.onCreate(bundle);

        setContentView(R.layout.activity_firstuse);

        // Get the ViewPager and set it's PagerAdapter so that it can display items
        ViewPager viewPager = (ViewPager) findViewById(R.id.viewpager);
        viewPager.setAdapter(new FirstUseAdapter(getSupportFragmentManager(),
                FirstUseActivity.this));

        // Give the TabLayout the ViewPager
        TabLayout tabLayout = (TabLayout) findViewById(R.id.sliding_tabs);
        tabLayout.setupWithViewPager(viewPager);
        tabLayout.setTabGravity(TabLayout.GRAVITY_CENTER);
        ImageView tab = new ImageView(this);
        tab.setImageResource(R.drawable.mensa_icon);
        GradientDrawable drawable = (GradientDrawable) tab.getDrawable();
        drawable.setColorFilter(Color.argb(255, 0, 0, 0), PorterDuff.Mode.SRC_ATOP);
        tab.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        tab.setBackgroundColor(Color.WHITE);
        tab.setAdjustViewBounds(true);

        ImageView tab2 = new ImageView(this);
        tab2.setImageResource(R.drawable.colored_circle);
        GradientDrawable drawable2 = (GradientDrawable) tab2.getDrawable();
        drawable2.setColorFilter(Color.argb(255, 0, 0, 0), PorterDuff.Mode.SRC_ATOP);
        tab2.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        tab2.setBackgroundColor(Color.WHITE);
        tab2.setAdjustViewBounds(true);

        ImageView tab3 = new ImageView(this);
        tab3.setImageResource(R.drawable.colored_circle);
        GradientDrawable drawable3 = (GradientDrawable) tab3.getDrawable();
        drawable3.setColorFilter(Color.argb(255, 0, 0, 0), PorterDuff.Mode.SRC_ATOP);
        tab3.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        tab3.setBackgroundColor(Color.WHITE);
        tab3.setAdjustViewBounds(true);
        tabLayout.getTabAt(0).setCustomView(tab);
        tabLayout.getTabAt(1).setCustomView(tab2);
        tabLayout.getTabAt(2).setCustomView(tab3);
        /*
        nextStep = (Button) findViewById(R.id.nextStep);
        nextStep.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View viewIn) {
                startActivity(new Intent(FirstUseActivity.this, SetupActivity.class));
                finish();
            }
        });
        */

    }

}