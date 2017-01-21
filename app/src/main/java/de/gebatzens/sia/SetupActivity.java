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
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.ListView;

import org.json.JSONObject;

import de.gebatzens.sia.dialog.LoginDialog;

public class SetupActivity extends AppCompatActivity {

    Toolbar mToolBar;
    SchoolListAdapter adapter;
    ListView list;
    public Dialog currentLoginDialog;

    @Override
    public void onCreate(Bundle saved) {
        setTheme(R.style.AppThemeSetup);
        super.onCreate(saved);

        if(GGApp.GG_APP.api.isLoggedIn()) {
            new Thread() {
                @Override
                public void run() {
                    Log.d("ggvp", "Updating school " + GGApp.GG_APP.school.name);
                    try {
                        SiaAPI.APIResponse resp = GGApp.GG_APP.api.doRequest("/schoolInfo?token=" + GGApp.GG_APP.api.getToken(), null);
                        if(resp.state == SiaAPI.APIState.SUCCEEDED) {
                            String img = GGApp.GG_APP.school.image;

                            School.updateSchool((JSONObject) resp.data);
                            School.saveList();
                            String newImg = ((JSONObject) resp.data).getString("image");
                            if(!img.equals(newImg)) {
                                Log.d("ggvp", "Trying to download new image " + newImg);
                                School.downloadImage(newImg);
                            }

                            // sid could have changed
                            GGApp.GG_APP.preferences.edit().putString("sid", ((JSONObject) resp.data).getString("sid")).apply();
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

        GGApp.GG_APP.setSchool(null);
        GGApp.GG_APP.setFragmentIndex(0);

        setContentView(R.layout.activity_setup);

        mToolBar = (Toolbar) findViewById(R.id.toolbar);
        mToolBar.setTitle(getTitle());
        mToolBar.inflateMenu(R.menu.setup_menu);

        mToolBar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                if (menuItem.getItemId() == R.id.setup_refresh) {
                    showDownloadDialog();
                } else if(menuItem.getItemId() == R.id.setup_other_school) {
                    showLoginDialog(null, true);
                }
                return true;
            }
        });

        list = (ListView) findViewById(R.id.setup_list);
        adapter = new SchoolListAdapter(this, School.LIST);
        list.setAdapter(adapter);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                School s = School.LIST.get(position);
                showLoginDialog(s.sid, s.loginNeeded);
            }
        });

        if(School.LIST.size() == 0) {
            showDownloadDialog();
        } else {
            startDownloadThread(true);
        }

    }

    public void startDownloadThread(final boolean update) {
        new Thread() {
            @Override
            public void run() {
                final boolean b = School.fetchList();
                School.saveList();
                if(update)
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            adapter.notifyDataSetChanged();
                            if (!b)
                                Snackbar.make(getWindow().getDecorView().findViewById(R.id.coordinator_layout), getString(R.string.no_internet_connection), Snackbar.LENGTH_LONG)
                                        .setAction(getString(R.string.again), new View.OnClickListener() {
                                            @Override
                                            public void onClick(View v) {
                                                startDownloadThread(true);
                                            }
                                        })
                                        .show();

                        }
                    });

            }
        }.start();
    }

    public void showLoginDialog(String sid, boolean auth) {
        Bundle args = new Bundle();
        args.putBoolean("auth", auth);
        args.putString("sid", sid);

        LoginDialog l = new LoginDialog();
        l.setArguments(args);
        l.show(getSupportFragmentManager(), "login_dialog");

    }

    public void showDownloadDialog() {
        final ProgressDialog d = new ProgressDialog(this);
        d.setMessage(GGApp.GG_APP.getString(R.string.download_schools));
        d.setCancelable(false);
        d.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        d.show();

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
                        if (!b)
                            Snackbar.make(getWindow().getDecorView().findViewById(R.id.coordinator_layout), getString(R.string.no_internet_connection), Snackbar.LENGTH_LONG)
                                    .setAction(getString(R.string.again), new View.OnClickListener() {
                                        @Override
                                        public void onClick(View v) {
                                            showDownloadDialog();
                                        }
                                    })
                                    .show();
                    }
                });
                if(d.isShowing())
                    d.dismiss();
            }
        }.start();
    }

    public void startDownloading() {
        final ProgressDialog d = new ProgressDialog(this);
        d.setTitle(GGApp.GG_APP.school.name);
        d.setMessage(getString(R.string.downloading_image));
        d.setCancelable(false);
        d.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        d.show();

        new Thread() {
            @Override
            public void run() {
                if(!School.downloadImage(GGApp.GG_APP.school.image)) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Snackbar.make(getWindow().getDecorView().findViewById(R.id.coordinator_layout), getString(R.string.download_error), Snackbar.LENGTH_LONG).show();
                        }
                    });
                }

                //workaround for a bug that causes an endless loading screen
                GGApp.GG_APP.school.fragments.getByType(FragmentData.FragmentType.PLAN).get(0).setData(GGApp.GG_APP.api.getPlans(false));

                if(d.isShowing())
                    d.dismiss();
                Intent i = new Intent(SetupActivity.this, MainActivity.class);
                //i.putExtra("reload", true);
                startActivity(i);
                finish();

            }
        }.start();
    }

    @Override
    public void onActivityResult(int req, int res, Intent data) {
        if(currentLoginDialog != null && currentLoginDialog.isShowing()) {
            ((CheckBox) currentLoginDialog.findViewById(R.id.acceptTerms)).setChecked(req == 1);
        }
    }

}
