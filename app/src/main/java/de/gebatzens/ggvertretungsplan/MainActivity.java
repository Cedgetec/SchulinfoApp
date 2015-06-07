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
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import de.gebatzens.ggvertretungsplan.fragment.ExamFragment;
import de.gebatzens.ggvertretungsplan.fragment.MensaFragment;
import de.gebatzens.ggvertretungsplan.fragment.NewsFragment;
import de.gebatzens.ggvertretungsplan.fragment.RemoteDataFragment;
import de.gebatzens.ggvertretungsplan.fragment.SubstFragment;


public class MainActivity extends FragmentActivity {

    public RemoteDataFragment mContent;
    public Toolbar mToolbar;
    Context context;
    DrawerLayout mDrawerLayout;
    ActionBarDrawerToggle mToggle;
    String[] mStrings;
    ImageView mNacvigationImage;
    View mNavigationSchoolpictureLink;
    public Bundle savedState;
    DrawerLayout drawerLayout;
    NavigationView navigationView;
    int selectedItem;

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
        setTheme(GGApp.GG_APP.school.getTheme());
        super.onCreate(savedInstanceState);
        GGApp.GG_APP.activity = this;
        savedState = savedInstanceState;

        Intent intent = getIntent();
        if(intent != null && intent.getStringExtra("fragment") != null) {
            GGApp.FragmentType type = GGApp.FragmentType.valueOf(intent.getStringExtra("fragment"));
            GGApp.GG_APP.setFragmentType(type);
        }

        if(intent != null && intent.getBooleanExtra("reload", false))
            GGApp.GG_APP.refreshAsync(null, true, GGApp.GG_APP.getFragmentType());

        mStrings = new String[] {getResources().getString(R.string.substitute_schedule), getResources().getString(R.string.news), getResources().getString(R.string.cafeteria),
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
        mToolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {

                if (menuItem.getItemId() == R.id.action_refresh) {
                    ((SwipeRefreshLayout) mContent.getView().findViewById(R.id.refresh)).setRefreshing(true);
                    GGApp.GG_APP.refreshAsync(new Runnable() {
                        @Override
                        public void run() {
                            ((SwipeRefreshLayout) mContent.getView().findViewById(R.id.refresh)).setRefreshing(false);
                        }
                    }, true, GGApp.GG_APP.getFragmentType());
                } else if (menuItem.getItemId() == R.id.action_settings) {
                    Intent i = new Intent(MainActivity.this, SettingsActivity.class);
                    startActivityForResult(i, 1);
                }

                return false;
            }
        });

        mToolbar.setBackgroundColor(GGApp.GG_APP.school.getColor());
        mToolbar.setTitle(GGApp.GG_APP.school.name);
        mToolbar.setSubtitle(mStrings[Arrays.asList(GGApp.FragmentType.values()).indexOf(GGApp.GG_APP.getFragmentType())]);
        mToolbar.inflateMenu(R.menu.toolbar_menu);
        mToolbar.setTitleTextColor(Color.WHITE);
        mToolbar.setSubtitleTextColor(Color.WHITE);

        ((TextView) findViewById(R.id.drawer_image_text)).setText(GGApp.GG_APP.school.name);
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            GGApp.GG_APP.setStatusBarColorTransparent(getWindow());
            mDrawerLayout.setStatusBarBackgroundColor(GGApp.GG_APP.school.getDarkColor());
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

        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        navigationView = (NavigationView) findViewById(R.id.navigation_view);
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
        final int MENU_ITEMS = 5;
        final ArrayList<View> mMenuItems = new ArrayList<>(MENU_ITEMS);
        final Menu navMenu = navigationView.getMenu();
        navigationView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                for (int i = 0, length = navMenu.size(); i < length; i++) {
                    final MenuItem item = navMenu.getItem(i);
                    navigationView.findViewsWithText(mMenuItems, item.getTitle(), View.FIND_VIEWS_WITH_TEXT);
                }
                for (final View menuItem : mMenuItems) {
                    ((TextView) menuItem).setTextSize(14);
                    ((TextView) menuItem).setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
                }
            }
        });
        selectedItem = Arrays.asList(GGApp.FragmentType.values()).indexOf(GGApp.GG_APP.getFragmentType());
        navigationView.getMenu().getItem(selectedItem).setChecked(true);


        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override public boolean onNavigationItemSelected(MenuItem menuItem) {
                if(menuItem.getOrder() == 4){
                    mDrawerLayout.closeDrawers();
                    Intent i = new Intent(MainActivity.this, SettingsActivity.class);
                    startActivityForResult(i, 1);
                }else{
                    if(GGApp.GG_APP.getFragmentType() != GGApp.FragmentType.values()[menuItem.getOrder()]) {
                        GGApp.GG_APP.setFragmentType(GGApp.FragmentType.values()[menuItem.getOrder()]);
                        menuItem.setChecked(true);
                        mToolbar.setSubtitle(menuItem.getTitle());
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
                        drawerLayout.closeDrawers();
                    } else{
                        drawerLayout.closeDrawers();
                    }
                }
                return true;
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

            if(data != null && data.getBooleanExtra("setup", false)) {
                startActivity(new Intent(this, SetupActivity.class));
                finish();
                return;
            }

            //GGApp.GG_APP.recreateProvider();
            //setTheme(GGApp.GG_APP.provider.getTheme());
            mNacvigationImage = (ImageView) findViewById(R.id.navigation_schoolpicture);
            mNacvigationImage.setImageBitmap(GGApp.GG_APP.school.loadImage());
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                GGApp.GG_APP.setStatusBarColorTransparent(getWindow());
                mDrawerLayout.setStatusBarBackgroundColor(GGApp.GG_APP.school.getDarkColor());
            }
            mToolbar.setBackgroundColor(GGApp.GG_APP.school.getColor());
            mToolbar.setTitle(GGApp.GG_APP.school.name);
            ((TextView) findViewById(R.id.drawer_image_text)).setText(GGApp.GG_APP.school.name);

            if(GGApp.GG_APP.getFragmentType() == GGApp.FragmentType.PLAN) {
                ((SubstFragment)mContent).mTabLayout.setBackgroundColor(GGApp.GG_APP.school.getColor());
                mContent.setFragmentLoading();
            }
            GGApp.GG_APP.refreshAsync(null, true, GGApp.GG_APP.getFragmentType());
        }

    }

}
