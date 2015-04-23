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

package de.gebatzens.ggvertretungsplan;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.List;

import de.gebatzens.ggvertretungsplan.fragment.ExamFragment;
import de.gebatzens.ggvertretungsplan.fragment.MensaFragment;
import de.gebatzens.ggvertretungsplan.fragment.NewsFragment;
import de.gebatzens.ggvertretungsplan.fragment.RemoteDataFragment;
import de.gebatzens.ggvertretungsplan.fragment.SubstFragment;
import de.gebatzens.ggvertretungsplan.view.NavigationListAdapter;


public class MainActivity extends FragmentActivity {

    public RemoteDataFragment mContent;
    public Toolbar mToolbar;
    ListView mDrawerList;
    TextView mDrawerSettings;
    DrawerLayout mDrawerLayout;
    ActionBarDrawerToggle mToggle;
    String[] mStrings;
    int[] mIcons = new int[] {R.drawable.vertretungsplan_icon, R.drawable.news_icon, R.drawable.mensa_icon,
                                R.drawable.exam_icon};
    ImageView mNacvigationImage;
    View mNavigationSchoolpictureLink;
    public Bundle savedState;
    NavigationListAdapter nla;

    public RemoteDataFragment createFragment() {
        switch(GGApp.GG_APP.getFragmentType()) {
            case PLAN:
                return new SubstFragment();
            case NEWS:
                return new NewsFragment();
            case MENSA:
                return new MensaFragment();
            case EXAMS:
                return new ExamFragment();
            default:
                return null;
        }

    }

    public void removeAllFragments() {
        List<Fragment> frags = getSupportFragmentManager().getFragments();
        if(frags != null)
            for(Fragment frag : frags) {
                if(frag != null && !frag.getTag().equals("gg_content_fragment"))
                    getSupportFragmentManager().beginTransaction().remove(frag).commit();
            }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //setTheme(GGApp.GG_APP.provider.getTheme());
        super.onCreate(savedInstanceState);
        GGApp.GG_APP.activity = this;
        savedState = savedInstanceState;

        Intent intent = getIntent();
        if(intent != null && intent.getStringExtra("fragment") != null) {
            GGApp.FragmentType type = GGApp.FragmentType.valueOf(intent.getStringExtra("fragment"));
            GGApp.GG_APP.setFragmentType(type);
        }

        mStrings = new String[] {getResources().getString(R.string.substitutionplan), getResources().getString(R.string.news), getResources().getString(R.string.cafeteria),
                getResources().getString(R.string.exams)};

        NotificationManager nm =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        nm.cancel(123);

        setContentView(getLayoutInflater().inflate(R.layout.activity_main, null));

        removeAllFragments();

        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        mContent = createFragment();
        transaction.replace(R.id.content_fragment, mContent, "gg_content_fragment");
        transaction.commit();

        if(GGApp.GG_APP.getDataForFragment(GGApp.GG_APP.getFragmentType()) == null)
            GGApp.GG_APP.refreshAsync(null, true, GGApp.GG_APP.getFragmentType());
        

        mToolbar = (Toolbar) findViewById(R.id.toolbar);

        mToolbar.setBackgroundColor(GGApp.GG_APP.school.color);
        mToolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {

                if(menuItem.getItemId() == R.id.action_refresh) {
                    ((SwipeRefreshLayout) mContent.getView().findViewById(R.id.refresh)).setRefreshing(true);
                    GGApp.GG_APP.refreshAsync(new Runnable() {
                        @Override
                        public void run() {
                            ((SwipeRefreshLayout) mContent.getView().findViewById(R.id.refresh)).setRefreshing(false);
                        }
                    }, true, GGApp.GG_APP.getFragmentType());
                } else if(menuItem.getItemId() == R.id.action_settings) {
                    Intent i = new Intent(MainActivity.this, SettingsActivity.class);
                    startActivityForResult(i, 1);
                }

                return false;
            }
        });

        mToolbar.setTitle(GGApp.GG_APP.school.name);
        mToolbar.setSubtitle(mStrings[Arrays.asList(GGApp.FragmentType.values()).indexOf(GGApp.GG_APP.getFragmentType())]);
        mToolbar.inflateMenu(R.menu.toolbar_menu);
        mToolbar.setTitleTextColor(Color.WHITE);
        mToolbar.setSubtitleTextColor(Color.WHITE);

        ((TextView) findViewById(R.id.drawer_image_text)).setText(GGApp.GG_APP.school.name);

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            GGApp.GG_APP.setStatusBarColorTransparent(getWindow());
            mDrawerLayout.setStatusBarBackgroundColor(GGApp.GG_APP.school.darkColor);
        }

        mToggle = new ActionBarDrawerToggle(this, mDrawerLayout,
                mToolbar, R.string.drawer_open, R.string.drawer_close) {

            /** Called when a drawer has settled in a completely closed state. */
            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }

            /** Called when a drawer has settled in a completely open state. */
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }
        };

        mNacvigationImage = (ImageView) findViewById(R.id.navigation_schoolpicture);
        mNacvigationImage.setImageBitmap(GGApp.GG_APP.school.loadImage());
        mNavigationSchoolpictureLink = (View) findViewById(R.id.navigation_schoolpicture_link);
        mNavigationSchoolpictureLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View viewIn) {
                mDrawerLayout.closeDrawers();
                Intent linkIntent = new Intent(Intent.ACTION_VIEW);
                linkIntent.setData(Uri.parse(GGApp.GG_APP.school.website));
                startActivity(linkIntent);
            }
        });

        mDrawerLayout.setDrawerListener(mToggle);

        mDrawerList = (ListView) findViewById(R.id.left_drawer);
        //ArrayAdapter<String> aa = new ArrayAdapter<String>(this, R.layout.drawer_list_item, mStrings);
        nla = new NavigationListAdapter(this, mStrings, mIcons);
        mDrawerList.setAdapter(nla);
        nla.mSelected = Arrays.asList(GGApp.FragmentType.values()).indexOf(GGApp.GG_APP.getFragmentType());
        nla.mColor = GGApp.GG_APP.school.color;
        mDrawerList.setItemChecked(nla.mSelected, true);

        mDrawerList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if(GGApp.GG_APP.getFragmentType() != GGApp.FragmentType.values()[position]) {
                    GGApp.GG_APP.setFragmentType(GGApp.FragmentType.values()[position]);
                    nla.mSelected = position;
                    nla.notifyDataSetChanged();
                    mDrawerLayout.closeDrawers();
                    mToolbar.setSubtitle(mStrings[position]);
                    mContent = createFragment();
                    Animation fadeOut = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fade_out);
                    fadeOut.setAnimationListener(new Animation.AnimationListener() {
                        @Override
                        public void onAnimationStart(Animation animation) {
                            // Called when the Animation starts
                        }
                        @Override
                        public void onAnimationEnd(Animation animation) {
                            FrameLayout contentFrame = (FrameLayout) findViewById(R.id.content_fragment);
                            contentFrame.setVisibility(View.INVISIBLE);
                            if(GGApp.GG_APP.getDataForFragment(GGApp.GG_APP.getFragmentType()) == null)
                                GGApp.GG_APP.refreshAsync(null, true, GGApp.GG_APP.getFragmentType());

                            removeAllFragments();

                            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                            transaction.replace(R.id.content_fragment, mContent, "gg_content_fragment");
                            transaction.commit();
                        }
                        @Override
                        public void onAnimationRepeat(Animation animation) {
                            // This is called each time the Animation repeats
                        }
                    });
                    FrameLayout contentFrame = (FrameLayout) findViewById(R.id.content_fragment);
                    contentFrame.startAnimation(fadeOut);
                } else{
                    mDrawerLayout.closeDrawers();
                }
            }
        });
        ListviewHelper.getListViewSize(mDrawerList);

        /*mDrawerFirstUse = (TextView) findViewById(R.id.left_drawer_firstuse);
        mDrawerFirstUse.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View viewIn) {
                mDrawerLayout.closeDrawers();
                Intent i = new Intent(MainActivity.this, FirstUseActivity.class);
                startActivityForResult(i, 1);
            }
        });*/

        mDrawerSettings = (TextView) findViewById(R.id.left_drawer_settings);
        mDrawerSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View viewIn) {
                mDrawerLayout.closeDrawers();
                Intent i = new Intent(MainActivity.this, SettingsActivity.class);
                startActivityForResult(i, 1);
            }
        });
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        mToggle.syncState();

    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public void onSaveInstanceState(Bundle b) {
        super.onSaveInstanceState(b);

        mContent.saveInstanceState(b);
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        GGApp.GG_APP.activity = null;
    }

    @Override
    public void onResume() {
        super.onResume();

        GGApp.GG_APP.activity = this;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == 1 && resultCode == RESULT_OK) { //Settings changed
            //GGApp.GG_APP.recreateProvider();
            //setTheme(GGApp.GG_APP.provider.getTheme());
            mNacvigationImage = (ImageView) findViewById(R.id.navigation_schoolpicture);
            mNacvigationImage.setImageBitmap(GGApp.GG_APP.school.loadImage());
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                GGApp.GG_APP.setStatusBarColorTransparent(getWindow());
                mDrawerLayout.setStatusBarBackgroundColor(GGApp.GG_APP.school.darkColor);
            }
            mToolbar.setBackgroundColor(GGApp.GG_APP.school.color);
            mToolbar.setTitle(GGApp.GG_APP.school.name);
            ((TextView) findViewById(R.id.drawer_image_text)).setText(GGApp.GG_APP.school.name);

            if(GGApp.GG_APP.getFragmentType() == GGApp.FragmentType.PLAN) {
                ((SubstFragment)mContent).mSlidingTabLayout.setBackgroundColor(GGApp.GG_APP.school.color);
                mContent.setFragmentLoading();
            }
            GGApp.GG_APP.refreshAsync(null, true, GGApp.GG_APP.getFragmentType());
        }

    }

}
