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

package de.gebatzens.sia;

import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Debug;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

import de.gebatzens.sia.data.Exams;
import de.gebatzens.sia.fragment.ExamFragment;
import de.gebatzens.sia.fragment.MensaFragment;
import de.gebatzens.sia.fragment.NewsFragment;
import de.gebatzens.sia.fragment.PDFFragment;
import de.gebatzens.sia.fragment.RemoteDataFragment;
import de.gebatzens.sia.fragment.SubstFragment;


public class MainActivity extends AppCompatActivity {

    public RemoteDataFragment mContent;
    public Toolbar mToolbar;
    DrawerLayout mDrawerLayout;
    ActionBarDrawerToggle mDrawerToggle;
    View mNavigationHeader;
    TextView mNavigationSchoolpictureText;
    ImageView mNavigationSchoolpicture;
    View mNavigationSchoolpictureLink;
    public Bundle savedState;
    NavigationView navigationView;
    int selectedItem;

    public RemoteDataFragment getFragment() {
        FragmentData frag = GGApp.GG_APP.school.fragments.get(GGApp.GG_APP.getFragmentIndex());

        RemoteDataFragment fr = null;

        switch(frag.type) {
            case PLAN:
                fr = new SubstFragment();
                break;
            case NEWS:
                fr = new NewsFragment();
                break;
            case MENSA:
                fr = new MensaFragment();
                break;
            case EXAMS:
                fr = new ExamFragment();
                break;
            case PDF:
                fr = new PDFFragment();
                break;
        }

        Bundle bundle = new Bundle();
        bundle.putInt("fragment", GGApp.GG_APP.getFragmentIndex());

        fr.setArguments(bundle);

        return fr;

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
    public void onRequestPermissionsResult(int req, @NonNull String[] permissions, @NonNull int[] results) {
        if(req == 1) {
            boolean b = true;
            for(int i : results) {
                if(i != PackageManager.PERMISSION_GRANTED)
                    b = false;
            }

            if(b) {
                showExamDialog();
            }
        }
    }

    private void showExamDialog() {
        final List<Exams.ExamItem> exams = ((Exams) GGApp.GG_APP.school.fragments.getData(FragmentData.FragmentType.EXAMS).get(0).getData()).getSelectedItems(false);
        if(exams.size() == 0) {
            Snackbar.make(findViewById(R.id.coordinator_layout), R.string.no_exams_selected, Snackbar.LENGTH_SHORT).show();
            return;
        }

        final long calId = GGApp.GG_APP.getCalendarId();
        if(calId == -2) {
            return; // permission request
        }

        if(calId == -1) {
            Snackbar.make(findViewById(R.id.coordinator_layout), R.string.no_cal_avail, Snackbar.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle(R.string.add_to_calendar);
        builder.setMessage(getResources().getQuantityString(R.plurals.n_events_will_be_added, exams.size(), exams.size()));

        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //TODO title could change
                for (Exams.ExamItem e : exams) {
                    GGApp.GG_APP.addToCalendar(calId, e.date, getString(R.string.exam) + ": " + e.subject);
                }
            }
        });

        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        builder.create().show();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(GGApp.GG_APP.school.getTheme());
        super.onCreate(savedInstanceState);
        Log.w("ggvp", "CREATE NEW MAINACTIVITY");
        //Debug.startMethodTracing("sia3");
        GGApp.GG_APP.activity = this;
        savedState = savedInstanceState;

        final FragmentData.FragmentList fragments = GGApp.GG_APP.school.fragments;

        Intent intent = getIntent();
        if(intent != null && intent.getStringExtra("fragment") != null) {
            FragmentData frag = fragments.getData(FragmentData.FragmentType.valueOf(intent.getStringExtra("fragment"))).get(0);
            GGApp.GG_APP.setFragmentIndex(fragments.indexOf(frag));
        }

        if(intent != null && intent.getBooleanExtra("reload", false))
            GGApp.GG_APP.refreshAsync(null, true, fragments.get(GGApp.GG_APP.getFragmentIndex()));


        NotificationManager nm =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        nm.cancel(123);

        setContentView(R.layout.activity_main);

        //removeAllFragments();

        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        mContent = getFragment();
        transaction.replace(R.id.content_fragment, mContent, "gg_content_fragment");
        transaction.commit();

        if(fragments.get(GGApp.GG_APP.getFragmentIndex()).getData() == null)
            GGApp.GG_APP.refreshAsync(null, true, fragments.get(GGApp.GG_APP.getFragmentIndex()));
        

        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        mToolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {

                switch (menuItem.getItemId()) {
                    case R.id.action_refresh:
                        ((SwipeRefreshLayout) mContent.getView().findViewById(R.id.refresh)).setRefreshing(true);
                        GGApp.GG_APP.refreshAsync(new Runnable() {
                            @Override
                            public void run() {
                                ((SwipeRefreshLayout) mContent.getView().findViewById(R.id.refresh)).setRefreshing(false);
                            }
                        }, true, fragments.get(GGApp.GG_APP.getFragmentIndex()));
                        return true;
                    case R.id.action_settings:
                        Intent i = new Intent(MainActivity.this, SettingsActivity.class);
                        startActivityForResult(i, 1);
                        return true;
                    case R.id.action_changeThemeMode:
                        GGApp.GG_APP.setDarkThemeEnabled(!GGApp.GG_APP.isDarkThemeEnabled());
                        GGApp.GG_APP.school.loadTheme();
                        recreate();
                        return true;
                    case R.id.action_addToCalendar:
                        showExamDialog();
                        return true;
                    case R.id.action_help:
                        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                        builder.setTitle(getApplication().getString(R.string.help));
                        builder.setMessage(getApplication().getString(R.string.exam_explain));
                        builder.setPositiveButton(getApplication().getString(R.string.close), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });
                        builder.create().show();
                        return true;
                }

                return false;
            }
        });

        mToolbar.setTitleTextColor(Color.WHITE);
        mToolbar.setSubtitleTextColor(Color.WHITE);
        mToolbar.setBackgroundColor(GGApp.GG_APP.school.getColor());
        mToolbar.setTitle(GGApp.GG_APP.school.name);
        Log.d("ggvp", "fragment type " + fragments.get(GGApp.GG_APP.getFragmentIndex()));
        mToolbar.setSubtitle(fragments.get(GGApp.GG_APP.getFragmentIndex()).name);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            GGApp.GG_APP.setStatusBarColorTransparent(getWindow()); // because of the navigation drawer
        }

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout,
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
        mDrawerLayout.setDrawerListener(mDrawerToggle);
        navigationView = (NavigationView) findViewById(R.id.navigation_view);
        mNavigationHeader = navigationView.getHeaderView(0);
        mNavigationSchoolpictureText = (TextView) mNavigationHeader.findViewById(R.id.drawer_image_text);
        mNavigationSchoolpictureText.setText(GGApp.GG_APP.school.name);
        mNavigationSchoolpicture = (ImageView) mNavigationHeader.findViewById(R.id.navigation_schoolpicture);
        mNavigationSchoolpicture.setImageBitmap(GGApp.GG_APP.school.loadImage());
        mNavigationSchoolpictureLink = mNavigationHeader.findViewById(R.id.navigation_schoolpicture_link);
        mNavigationSchoolpictureLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View viewIn) {
                mDrawerLayout.closeDrawers();
                Intent linkIntent = new Intent(Intent.ACTION_VIEW);
                linkIntent.setData(Uri.parse(GGApp.GG_APP.school.website));
                startActivity(linkIntent);
            }
        });

        Menu menu = navigationView.getMenu();
        menu.clear();
        for(int i = 0; i < fragments.size(); i++) {
            MenuItem item = menu.add(R.id.fragments, Menu.NONE, i, fragments.get(i).name);
            item.setIcon(fragments.get(i).getIconRes());
        }

        menu.add(R.id.settings, R.id.settings_item, fragments.size(), R.string.settings);
        menu.setGroupCheckable(R.id.fragments, true, true);
        menu.setGroupCheckable(R.id.settings, false, false);

        final Menu navMenu = navigationView.getMenu();
        selectedItem = GGApp.GG_APP.getFragmentIndex();
        if(selectedItem != -1)
            navMenu.getItem(selectedItem).setChecked(true);


        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override public boolean onNavigationItemSelected(MenuItem menuItem) {
                if(menuItem.getItemId() == R.id.settings_item) {
                    mDrawerLayout.closeDrawers();
                    Intent i = new Intent(MainActivity.this, SettingsActivity.class);
                    startActivityForResult(i, 1);
                } else {
                    final int index = menuItem.getOrder();
                    if(GGApp.GG_APP.getFragmentIndex() != index) {
                        GGApp.GG_APP.setFragmentIndex(index);
                        menuItem.setChecked(true);
                        mToolbar.setSubtitle(menuItem.getTitle());
                        mContent = getFragment();
                        Animation fadeOut = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fade_out);
                        fadeOut.setAnimationListener(new Animation.AnimationListener() {

                            @Override
                            public void onAnimationStart(Animation animation) {

                            }

                            @Override
                            public void onAnimationEnd(Animation animation) {
                                FrameLayout contentFrame = (FrameLayout) findViewById(R.id.content_fragment);
                                contentFrame.setVisibility(View.INVISIBLE);
                                if(fragments.get(index).getData() == null)
                                    GGApp.GG_APP.refreshAsync(null, true, fragments.get(index));

                                //removeAllFragments();

                                FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                                transaction.replace(R.id.content_fragment, mContent, "gg_content_fragment");
                                transaction.commit();
                            }

                            @Override
                            public void onAnimationRepeat(Animation animation) {

                            }
                        });
                        FrameLayout contentFrame = (FrameLayout) findViewById(R.id.content_fragment);
                        contentFrame.startAnimation(fadeOut);
                        mDrawerLayout.closeDrawers();
                    } else{
                        mDrawerLayout.closeDrawers();
                    }
                }
                return true;
            }
        });
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        mDrawerToggle.syncState();

    }

    @Override
    public void onBackPressed() {
        Debug.stopMethodTracing();
        if (mDrawerLayout.isDrawerOpen(GravityCompat.START)) {
            mDrawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
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

        if(mContent instanceof SubstFragment) {
            ((SubstFragment) mContent).resetScrollPositions();
        }
    }

    public void updateMenu(int menu) {
        mToolbar.getMenu().clear();
        mToolbar.inflateMenu(menu);
        mToolbar.getMenu().findItem(R.id.action_changeThemeMode).setTitle(getResources()
                .getString(GGApp.GG_APP.isDarkThemeEnabled() ? R.string.day_mode : R.string.night_mode));
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

            mContent.updateFragment();
        }

    }

}
