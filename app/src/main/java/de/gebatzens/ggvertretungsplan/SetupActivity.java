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

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

public class SetupActivity extends Activity {

    SchoolListAdapter adapter;
    ListView list;
    Button refresh;

    @Override
    public void onCreate(Bundle saved) {
        setTheme(R.style.AppThemeBlueGrey);
        super.onCreate(saved);

        if(GGApp.GG_APP.remote.isLoggedIn()) {
            startActivity(new Intent(this, MainActivity.class));
            startDownloadThread(false);
            return;
        }

        setContentView(R.layout.activity_setup);

        refresh = (Button) findViewById(R.id.refresh);
        refresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View viewIn) {
                showDownloadDialog();
            }
        });

        list = (ListView) findViewById(R.id.setup_list);
        adapter = new SchoolListAdapter();
        list.setAdapter(adapter);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                School s = School.LIST.get(position);
                GGApp.GG_APP.setSchool(s.sid);

                if (s.loginNeeded) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(SetupActivity.this);
                    AlertDialog dialog;
                    builder.setTitle(getResources().getString(R.string.login));
                    builder.setView(((LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE)).inflate(R.layout.login_dialog, null));

                    builder.setPositiveButton(getResources().getString(R.string.do_login_submit), new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(final DialogInterface dialog, int which) {
                            new AsyncTask<Integer, Integer, Integer>() {

                                @Override
                                public void onPostExecute(Integer v) {
                                    switch (v) {
                                        case 0:
                                            startDownloading();
                                            break;
                                        case 1:
                                            GGApp.GG_APP.showToast(getString(R.string.username_or_password_wrong));
                                            break;
                                        case 2:
                                            GGApp.GG_APP.showToast(getString(R.string.could_not_connect));
                                            break;
                                        case 3:
                                            GGApp.GG_APP.showToast(getString(R.string.unknown_error_login));
                                            break;
                                    }
                                }

                                @Override
                                protected Integer doInBackground(Integer... params) {
                                    String user = ((EditText) ((Dialog) dialog).findViewById(R.id.usernameInput)).getText().toString();
                                    String pass = ((EditText) ((Dialog) dialog).findViewById(R.id.passwordInput)).getText().toString();
                                    return GGApp.GG_APP.remote.login(user, pass);

                                }

                            }.execute();
                            dialog.dismiss();
                        }
                    });


                    builder.setNegativeButton(getString(R.string.abort), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });

                    dialog = builder.create();
                    dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
                    dialog.show();
                } else {
                    new AsyncTask<Integer, Integer, Integer>() {

                        @Override
                        public void onPostExecute(Integer i) {
                            switch (i) {
                                case 0:
                                    startDownloading();
                                    break;
                                case 1:
                                    //Bug
                                    break;
                                case 2:
                                    GGApp.GG_APP.showToast(getString(R.string.could_not_connect));
                                    break;
                                case 3:
                                    GGApp.GG_APP.showToast(getString(R.string.unknown_error_login));
                                    break;
                            }
                        }

                        @Override
                        protected Integer doInBackground(Integer... params) {
                            return GGApp.GG_APP.remote.login(null, null);
                        }
                    }.execute();

                }
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
                                GGApp.GG_APP.showToast(getString(R.string.no_internet_connection));

                        }
                    });
                GGApp.GG_APP.setSchool(GGApp.GG_APP.getDefaultSID());

            }
        }.start();
    }

    public void showDownloadDialog() {
        final ProgressDialog d = new ProgressDialog(this);
        d.setMessage("Downloading school list...");
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
                        adapter.notifyDataSetChanged();
                        if(!b)
                            GGApp.GG_APP.showToast(getString(R.string.no_internet_connection));
                    }
                });
                GGApp.GG_APP.setSchool(GGApp.GG_APP.getDefaultSID());
                d.dismiss();
            }
        }.start();
    }

    public void startDownloading() {
        final ProgressDialog d = new ProgressDialog(this);
        d.setTitle(GGApp.GG_APP.school.name);
        d.setMessage("Downloading image...");
        d.setCancelable(false);
        d.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        d.show();

        new Thread() {
            @Override
            public void run() {
                if(!GGApp.GG_APP.school.downloadImage()) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            GGApp.GG_APP.showToast(getString(R.string.download_error));
                        }
                    });
                }
                d.dismiss();
                Intent i = new Intent(SetupActivity.this, MainActivity.class);
                i.putExtra("reload", true);
                startActivity(i);
            }
        }.start();
    }

}
