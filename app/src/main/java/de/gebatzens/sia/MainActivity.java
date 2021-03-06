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
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.graphics.drawable.VectorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Debug;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
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
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import de.gebatzens.sia.data.Exams;
import de.gebatzens.sia.data.Shareable;
import de.gebatzens.sia.dialog.TextDialog;
import de.gebatzens.sia.fragment.ExamFragment;
import de.gebatzens.sia.fragment.MensaFragment;
import de.gebatzens.sia.fragment.NewsFragment;
import de.gebatzens.sia.fragment.PDFFragment;
import de.gebatzens.sia.fragment.RemoteDataFragment;
import de.gebatzens.sia.fragment.SubstFragment;
import de.gebatzens.sia.view.SnowView;
import de.gebatzens.sia.fragment.SubstListAdapter;

public class MainActivity extends AppCompatActivity {

    public RemoteDataFragment mContent;
    public Toolbar mToolBar, shareToolbar;
    DrawerLayout mDrawerLayout;
    ActionBarDrawerToggle mDrawerToggle;
    View mNavigationHeader;
    TextView mNavigationSchoolpictureText;
    ImageView mNavigationSchoolpicture;
    View mNavigationSchoolpictureLink;
    public Bundle savedState;
    NavigationView navigationView;
    int selectedItem;
    SnowView snowView;

    // it is static to keep the list when the device gets rotated
    // TODO: this is not good....
    private static ArrayList<Shareable> shared = new ArrayList<>();

    public void updateToolbar(String s, String st) {
        mToolBar.setTitle(s);
        mToolBar.setSubtitle(st);
    }

    public RemoteDataFragment getFragment() {
        FragmentData frag = SIAApp.SIA_APP.school.fragments.get(SIAApp.SIA_APP.getFragmentIndex());

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
        bundle.putInt("fragment", SIAApp.SIA_APP.getFragmentIndex());

        fr.setArguments(bundle);

        return fr;

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
        final List<Exams.ExamItem> exams = ((Exams) SIAApp.SIA_APP.school.fragments.getByType(FragmentData.FragmentType.EXAMS).get(0).getData()).getSelectedItems(false);
        if(exams.size() == 0) {
            Snackbar.make(findViewById(R.id.coordinator_layout), R.string.no_exams_selected, Snackbar.LENGTH_SHORT).show();
            return;
        }

        final long calId = SIAApp.SIA_APP.getCalendarId();
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
                    SIAApp.SIA_APP.addToCalendar(calId, e.date, getString(R.string.exam) + ": " + e.subject);
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
        setTheme(SIAApp.SIA_APP.school.getTheme());
        super.onCreate(savedInstanceState);
        Log.w("ggvp", "CREATE NEW MAINACTIVITY");
        //Debug.startMethodTracing("sia3");
        SIAApp.SIA_APP.activity = this;
        savedState = savedInstanceState;

        final FragmentData.FragmentList fragments = SIAApp.SIA_APP.school.fragments;

        Intent intent = getIntent();
        if(intent != null && intent.getStringExtra("fragment") != null) {
            FragmentData frag = fragments.getByType(FragmentData.FragmentType.valueOf(intent.getStringExtra("fragment"))).get(0);
            SIAApp.SIA_APP.setFragmentIndex(fragments.indexOf(frag));
        }

        if(intent != null && intent.getBooleanExtra("reload", false)) {
            SIAApp.SIA_APP.refreshAsync(null, true, fragments.get(SIAApp.SIA_APP.getFragmentIndex()));
            intent.removeExtra("reload");
        }

        setContentView(R.layout.activity_main);


        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        mContent = getFragment();
        transaction.replace(R.id.content_fragment, mContent, "gg_content_fragment");
        transaction.commit();

        Log.d("ggvp", "DATA: " + fragments.get(SIAApp.SIA_APP.getFragmentIndex()).getData());
        if(fragments.get(SIAApp.SIA_APP.getFragmentIndex()).getData() == null)
            SIAApp.SIA_APP.refreshAsync(null, true, fragments.get(SIAApp.SIA_APP.getFragmentIndex()));

        if("Summer".equals(SIAApp.SIA_APP.getCurrentThemeName())){
            ImageView summerNavigationPalm = (ImageView) findViewById(R.id.summer_navigation_palm);
            summerNavigationPalm.setImageResource(R.drawable.summer_palm);
            ImageView summerBackgroundImage = (ImageView) findViewById(R.id.summer_background_image);
            summerBackgroundImage.setImageResource(R.drawable.summer_background);
        }

        mToolBar = (Toolbar) findViewById(R.id.toolbar);
        mToolBar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {

                switch (menuItem.getItemId()) {
                    case R.id.action_refresh:
                        ((SwipeRefreshLayout) mContent.getView().findViewById(R.id.refresh)).setRefreshing(true);
                        SIAApp.SIA_APP.refreshAsync(new Runnable() {
                            @Override
                            public void run() {
                                ((SwipeRefreshLayout) mContent.getView().findViewById(R.id.refresh)).setRefreshing(false);
                            }
                        }, true, fragments.get(SIAApp.SIA_APP.getFragmentIndex()));
                        return true;
                    case R.id.action_settings:
                        Intent i = new Intent(MainActivity.this, SettingsActivity.class);
                        startActivityForResult(i, 1);
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

        updateToolbar(SIAApp.SIA_APP.school.name, fragments.get(SIAApp.SIA_APP.getFragmentIndex()).name);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            SIAApp.SIA_APP.setStatusBarColorTransparent(getWindow()); // because of the navigation drawer
        }

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout,
                mToolBar, R.string.drawer_open, R.string.drawer_close) {

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
        mDrawerLayout.addDrawerListener(mDrawerToggle);
        navigationView = (NavigationView) findViewById(R.id.navigation_view);
        mNavigationHeader = navigationView.getHeaderView(0);
        mNavigationSchoolpictureText = (TextView) mNavigationHeader.findViewById(R.id.drawer_image_text);
        mNavigationSchoolpictureText.setText(SIAApp.SIA_APP.school.name);
        mNavigationSchoolpicture = (ImageView) mNavigationHeader.findViewById(R.id.navigation_schoolpicture);
        mNavigationSchoolpicture.setImageBitmap(SIAApp.SIA_APP.school.loadImage());
        mNavigationSchoolpictureLink = mNavigationHeader.findViewById(R.id.navigation_schoolpicture_link);
        mNavigationSchoolpictureLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View viewIn) {
                mDrawerLayout.closeDrawers();
                Intent linkIntent = new Intent(Intent.ACTION_VIEW);
                linkIntent.setData(Uri.parse(SIAApp.SIA_APP.school.website));
                startActivity(linkIntent);
            }
        });

        final Menu menu = navigationView.getMenu();
        menu.clear();
        for(int i = 0; i < fragments.size(); i++) {
            MenuItem item = menu.add(R.id.fragments, Menu.NONE, i, fragments.get(i).name);
            item.setIcon(fragments.get(i).getIconRes());
        }

        menu.add(R.id.settings, R.id.settings_item, fragments.size(), R.string.settings);
        menu.setGroupCheckable(R.id.fragments, true, true);
        menu.setGroupCheckable(R.id.settings, false, false);

        final Menu navMenu = navigationView.getMenu();
        selectedItem = SIAApp.SIA_APP.getFragmentIndex();
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
                    if(SIAApp.SIA_APP.getFragmentIndex() != index) {
                        SIAApp.SIA_APP.setFragmentIndex(index);
                        menuItem.setChecked(true);
                        updateToolbar(SIAApp.SIA_APP.school.name, menuItem.getTitle().toString());
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
                                    SIAApp.SIA_APP.refreshAsync(null, true, fragments.get(index));

                                //removeAllFragments();

                                FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                                transaction.replace(R.id.content_fragment, mContent, "gg_content_fragment");
                                transaction.commit();

                                snowView.updateSnow();
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

        if(Build.VERSION.SDK_INT >= 25 ) {
            ShortcutManager shortcutManager = getSystemService(ShortcutManager.class);
            shortcutManager.removeAllDynamicShortcuts();

            for (int i = 0; i < fragments.size(); i++) {
                Drawable drawable = getDrawable(fragments.get(i).getIconRes());
                Bitmap icon;
                if (drawable instanceof VectorDrawable) {
                    icon = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
                    Canvas canvas = new Canvas(icon);
                    drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
                    drawable.draw(canvas);
                } else {
                    icon = BitmapFactory.decodeResource(getResources(), fragments.get(i).getIconRes());
                }

                Bitmap connectedIcon = Bitmap.createBitmap(icon.getWidth(), icon.getHeight(), Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(connectedIcon);
                Paint paint = new Paint();
                paint.setAntiAlias(true);
                paint.setColor(Color.parseColor("#f5f5f5"));
                canvas.drawCircle(icon.getWidth() / 2, icon.getHeight() / 2, icon.getWidth() / 2, paint);
                paint.setColorFilter(new PorterDuffColorFilter(SIAApp.SIA_APP.school.getColor(), PorterDuff.Mode.SRC_ATOP));
                canvas.drawBitmap(icon, null, new RectF(icon.getHeight() / 4.0f, icon.getHeight() / 4.0f, icon.getHeight() - icon.getHeight() / 4.0f, icon.getHeight() - icon.getHeight() / 4.0f), paint);

                Intent newTaskIntent = new Intent(this, MainActivity.class);
                newTaskIntent.setAction(Intent.ACTION_MAIN);
                newTaskIntent.putExtra("fragment", fragments.get(i).type.toString());

                ShortcutInfo shortcut = new ShortcutInfo.Builder(this, fragments.get(i).name)
                        .setShortLabel(fragments.get(i).name)
                        .setLongLabel(fragments.get(i).name)
                        .setIcon(Icon.createWithBitmap(connectedIcon))
                        .setIntent(newTaskIntent)
                        .build();

                shortcutManager.addDynamicShortcuts(Arrays.asList(shortcut));
            }
        }

        if(SIAApp.SIA_APP.preferences.getBoolean("app_130_upgrade", true)) {
            if (!SIAApp.SIA_APP.preferences.getBoolean("first_use_filter", true)) {
                TextDialog.newInstance(R.string.upgrade1_3title, R.string.upgrade1_3).show(getSupportFragmentManager(), "upgrade_dialog");
            }

            SIAApp.SIA_APP.preferences.edit().putBoolean("app_130_upgrade", false).apply();
        }

        snowView = (SnowView) findViewById(R.id.snow_view);

        shareToolbar = (Toolbar) findViewById(R.id.share_toolbar);
        shareToolbar.getMenu().clear();
        shareToolbar.inflateMenu(R.menu.share_toolbar_menu);
        shareToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggleShareToolbar(false);
                for(Shareable s : MainActivity.this.shared) {
                    s.setMarked(false);
                }
                MainActivity.this.shared.clear();

                mContent.updateFragment();
            }
        });

        shareToolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                toggleShareToolbar(false);

                HashMap<Date, ArrayList<Shareable>> dates = new HashMap<Date, ArrayList<Shareable>>();

                for(Shareable s : MainActivity.this.shared) {
                    ArrayList<Shareable> list = dates.get(s.getDate());
                    if(list == null) {
                        list = new ArrayList<Shareable>();
                        dates.put(s.getDate(), list);
                    }

                    list.add(s);

                    s.setMarked(false);
                }
                MainActivity.this.shared.clear();

                List<Date> dateList = new ArrayList<Date>(dates.keySet());
                Collections.sort(dateList);
                String content = "";

                for(Date key : dateList) {
                    content += SubstListAdapter.translateDay(key) + "\n\n";

                    Collections.sort(dates.get(key));
                    for(Shareable s : dates.get(key)) {
                        content += s.getShareContent() + "\n";
                    }

                    content += "\n";
                }

                content = content.substring(0, content.length() - 1);

                mContent.updateFragment();

                if(item.getItemId() == R.id.action_copy) {
                    ClipboardManager clipboard = (ClipboardManager)
                            getSystemService(Context.CLIPBOARD_SERVICE);

                    ClipData clip = ClipData.newPlainText(getString(R.string.entries), content);
                    clipboard.setPrimaryClip(clip);
                } else if(item.getItemId() == R.id.action_share) {
                    Intent sendIntent = new Intent();
                    sendIntent.setAction(Intent.ACTION_SEND);
                    sendIntent.putExtra(Intent.EXTRA_TEXT, content);
                    sendIntent.setType("text/plain");
                    startActivity(Intent.createChooser(sendIntent, getResources().getText(R.string.send_to)));
                }

                return true;
            }
        });

        if(shared.size() > 0) {
            shareToolbar.setVisibility(View.VISIBLE);
            updateShareToolbarText();
        }

        // if a fragment is opened via a notification or a shortcut reset the shared entries
        // delete extra fragment because the same intent is used when the device gets rotated and the user could have opened a new fragment
        if(intent != null && intent.hasExtra("fragment")) {
            resetShareToolbar();
            intent.removeExtra("fragment");
        }

    }

    public void toggleShareToolbar(final boolean show) {
        final Toolbar shareToolbar = (Toolbar) findViewById(R.id.share_toolbar);

        AlphaAnimation anim = new AlphaAnimation(show ? 0.f : 1.f, show ? 1.f : 0.f);
        anim.setDuration(200);

        anim.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                if(show) {
                    shareToolbar.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                if(!show) {
                    shareToolbar.setVisibility(View.GONE);
                }
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });

        shareToolbar.startAnimation(anim);
    }

    private void updateShareToolbarText() {
        shareToolbar.setTitle(getResources().getQuantityString(R.plurals.n_entries_marked, shared.size(), shared.size()));
    }

    public void addShareable(Shareable s) {
        if(shared.size() == 0) {
            this.toggleShareToolbar(true);
        }

        shared.add(s);

        updateShareToolbarText();

    }

    public void removeShareable(Shareable s) {
        if(shared.size() == 1) {
            this.toggleShareToolbar(false);
        }

        shared.remove(s);

        if(shared.size() > 0) {
            updateShareToolbarText();
        }
    }

    public void resetShareToolbar() {
        if(shared.size() > 0) {
            for(Shareable s : shared) {
                s.setMarked(false);
            }
            shared.clear();
            toggleShareToolbar(false);
            mContent.updateFragment();
        }

    }

    public int getNumberOfMarkedItems() {
        return shared.size();
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        mDrawerToggle.syncState();

    }

    @Override
    public void onBackPressed() {
        if(shared.size() > 0) {
            resetShareToolbar();
            return;
        }

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
        SIAApp.SIA_APP.activity = null;

    }

    @Override
    public void onResume() {
        super.onResume();
        SIAApp.SIA_APP.activity = this;

        if(mContent instanceof SubstFragment) {
            ((SubstFragment) mContent).resetScrollPositions();
        }

        ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).cancel(123);
    }

    public void updateMenu(int menu) {
        mToolBar.getMenu().clear();
        mToolBar.inflateMenu(menu);
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

            if(data != null && data.getBooleanExtra("recreate", false)) {
                recreate();
                return;
            }

            mContent.updateFragment();
        }

    }

}
