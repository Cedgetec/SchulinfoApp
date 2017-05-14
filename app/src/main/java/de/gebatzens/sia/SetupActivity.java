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

package de.gebatzens.sia;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDelegate;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import org.json.JSONObject;

import de.gebatzens.sia.data.Subst;
import de.gebatzens.sia.dialog.LoginDialog;

public class SetupActivity extends AppCompatActivity {

    Toolbar mToolBar;
    SwipeRefreshLayout swipeRefreshLayout;
    ListView list;
    SchoolListAdapter adapter;
    public Dialog currentLoginDialog;
    public Bundle restoreDialog;

    @Override
    public void onCreate(Bundle saved) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        setTheme(R.style.AppThemeSetup);
        super.onCreate(saved);

        if(SIAApp.SIA_APP.api.isLoggedIn()) {
            new Thread() {
                @Override
                public void run() {
                    Log.d("ggvp", "Updating school " + SIAApp.SIA_APP.school.name);
                    try {
                        SiaAPI.APIResponse resp = SIAApp.SIA_APP.api.doRequest("/schoolInfo?token=" + SIAApp.SIA_APP.api.getToken(), null);
                        if(resp.state == SiaAPI.APIState.SUCCEEDED) {
                            String img = SIAApp.SIA_APP.school.image;

                            School.updateSchool((JSONObject) resp.data);
                            School.saveList();
                            String newImg = ((JSONObject) resp.data).getString("image");
                            if(!img.equals(newImg)) {
                                Log.d("ggvp", "Trying to download new image " + newImg);
                                School.downloadImage(newImg);
                            }

                            // sid could have changed
                            SIAApp.SIA_APP.preferences.edit().putString("sid", ((JSONObject) resp.data).getString("sid")).apply();
                        }
                    } catch (Exception e) {
                        Log.e("ggvp", e.toString());
                        e.printStackTrace();
                    }

                }
            }.start();

            startActivity(new Intent(this, MainActivity.class));
            finish();

            return;
        }

        SIAApp.SIA_APP.setSchool(null);
        SIAApp.SIA_APP.setFragmentIndex(0);

        setContentView(R.layout.activity_setup);

        mToolBar = (Toolbar) findViewById(R.id.toolbar);
        mToolBar.setTitle(getTitle());
        mToolBar.inflateMenu(R.menu.setup_menu);

        mToolBar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                if (menuItem.getItemId() == R.id.setup_refresh) {
                    swipeRefreshLayout.setRefreshing(true);
                    startDownloadSchoollistThread();
                } else if(menuItem.getItemId() == R.id.setup_other_school) {
                    showLoginDialog(false, null, true, "");
                }
                return true;
            }
        });

        swipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.setup_swiperefresh);
        swipeRefreshLayout.setProgressBackgroundColorSchemeColor(ContextCompat.getColor(this, R.color.SwipeRefreshLayout_background));
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                startDownloadSchoollistThread();
            }
        });
        // Configure the refreshing colors
        swipeRefreshLayout.setColorSchemeResources(R.color.ThemeSetupColorAccent);

        list = (ListView) findViewById(R.id.setup_list);
        adapter = new SchoolListAdapter(this, School.LIST);
        list.setAdapter(adapter);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                School s = School.LIST.get(position);
                showLoginDialog(true, s.sid, s.loginNeeded, "");
            }
        });

        if(School.LIST.size() == 0) {
            swipeRefreshLayout.setRefreshing(true);
            startDownloadSchoollistThread();
        } else {
            startDownloadSchoollistThread();
        }

    }

    public void startDownloadSchoollistThread() {
        new Thread() {
            @Override
            public void run() {
                final boolean b = School.fetchList();
                School.saveList();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        adapter.list = School.LIST;
                        adapter.notifyDataSetChanged();
                        swipeRefreshLayout.setRefreshing(false);
                        if (!b)
                            Snackbar.make(getWindow().getDecorView().findViewById(R.id.coordinator_layout), getString(R.string.no_internet_connection), Snackbar.LENGTH_LONG)
                                    .setAction(getString(R.string.again), new View.OnClickListener() {
                                        @Override
                                        public void onClick(View v) {
                                            swipeRefreshLayout.setRefreshing(true);
                                            startDownloadSchoollistThread();
                                        }
                                    })
                                    .show();

                    }
                });

            }
        }.start();
    }

    public void showLoginDialog(boolean hideSid, String sid, boolean auth, String user) {

        boolean restore = restoreDialog != null && hideSid == restoreDialog.getBoolean("hideSid") && (!hideSid || sid.equals(restoreDialog.getString("sid")));

        Bundle args = new Bundle();
        args.putBoolean("auth", auth);
        args.putBoolean("hideSid", hideSid);
        args.putString("sid", restore ? restoreDialog.getString("sid") : sid);
        args.putString("user", restore ? restoreDialog.getString("user") : user);

        LoginDialog l = new LoginDialog();
        l.setArguments(args);
        l.show(getSupportFragmentManager(), "login_dialog");

    }

    public void startDownloadingSchool() {
        final ProgressDialog d = new ProgressDialog(this);
        d.setTitle(SIAApp.SIA_APP.school.name);
        d.setMessage(getString(R.string.downloading_image));
        d.setCancelable(false);
        d.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        d.show();

        new Thread() {
            @Override
            public void run() {
                if(!School.downloadImage(SIAApp.SIA_APP.school.image)) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Snackbar.make(getWindow().getDecorView().findViewById(R.id.coordinator_layout), getString(R.string.download_error), Snackbar.LENGTH_LONG).show();
                        }
                    });
                }

                //workaround for a bug that causes an endless loading screen
                Subst.GGPlans subst = SIAApp.SIA_APP.api.getPlans(false);
                subst.save();
                SIAApp.SIA_APP.school.fragments.getByType(FragmentData.FragmentType.PLAN).get(0).setData(subst);

                if(d.isShowing())
                    d.dismiss();
                Intent i = new Intent(SetupActivity.this, MainActivity.class);
                //i.putExtra("reload", true);
                startActivity(i);
                finish();

            }
        }.start();
    }
}
