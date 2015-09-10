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

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.widget.LinearLayout;

import de.gebatzens.ggvertretungsplan.fragment.FirstUseAdapter;
import de.gebatzens.ggvertretungsplan.fragment.FirstUseFragment;

public class FirstUseActivity extends FragmentActivity {

    public FirstUseAdapter adapter;
    LinearLayout ib;

    @Override
    protected void onCreate(Bundle bundle) {
        setTheme(R.style.SetupTheme);
        adapter = new FirstUseAdapter(getSupportFragmentManager(), FirstUseActivity.this);

        super.onCreate(bundle);

        if(GGApp.GG_APP.preferences.getBoolean("first_use", false)) {
            startActivity(new Intent(this, SetupActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_firstuse);

        ib = (LinearLayout) findViewById(R.id.firstuse_finish);

        ib.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                GGApp.GG_APP.preferences.edit().putBoolean("first_use", true).apply();
                startActivity(new Intent(FirstUseActivity.this, SetupActivity.class));
                finish();
            }
        });

        final ViewPager viewPager = (ViewPager) findViewById(R.id.viewpager);
        viewPager.setAdapter(adapter);
        viewPager.setOffscreenPageLimit(3);
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int i, float v, int i1) {
                FirstUseAdapter adapter = (FirstUseAdapter) viewPager.getAdapter();

                if (i >= 3)
                    return;

                FirstUseFragment frag1 = adapter.fragments.get(i);
                FirstUseFragment frag2 = adapter.fragments.get(i + 1);


                int red = (int) (Color.red(frag1.color) * (1f - v) + Color.red(frag2.color) * v);
                int green = (int) (Color.green(frag1.color) * (1f - v) + Color.green(frag2.color) * v);
                int blue = (int) (Color.blue(frag1.color) * (1f - v) + Color.blue(frag2.color) * v);
                int newColor = Color.argb(255, red, green, blue);

                if (frag1.getView() != null)
                    frag1.getView().setBackgroundColor(newColor);
                if (frag2.getView() != null)
                    frag2.getView().setBackgroundColor(newColor);

            }

            @Override
            public void onPageSelected(int i) {
                if (i == 3) {
                    ib.setVisibility(View.VISIBLE);
                } else {
                    ib.setVisibility(View.INVISIBLE);
                }
            }

            @Override
            public void onPageScrollStateChanged(int i) {

            }
        });

        TabLayout tabLayout = (TabLayout) findViewById(R.id.sliding_tabs);
        tabLayout.setupWithViewPager(viewPager);

    }

}