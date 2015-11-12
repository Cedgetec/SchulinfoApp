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

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.Spanned;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.LinkMovementMethod;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ListView;

import org.json.JSONObject;

public class SetupActivity extends AppCompatActivity {

    SchoolListAdapter adapter;
    ListView list;

    @Override
    public void onCreate(Bundle saved) {
        setTheme(R.style.AppThemeBlueLight);
        super.onCreate(saved);

        if(GGApp.GG_APP.remote.isLoggedIn()) {
            new Thread() {
                @Override
                public void run() {
                    Log.d("ggvp", "Updating school " + GGApp.GG_APP.school.name);
                    try {
                        GGRemote.APIResponse resp = GGApp.GG_APP.remote.doRequest("/schoolInfo?token=" + GGApp.GG_APP.remote.getToken(), null);
                        if(resp.state == GGRemote.APIState.SUCCEEDED) {
                            School.updateSchool((JSONObject) resp.data);
                            School.saveList();

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

        setContentView(R.layout.activity_setup);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitleTextColor(Color.WHITE);
        toolbar.setTitle(getString(R.string.supported_schools));
        toolbar.setBackgroundColor(ContextCompat.getColor(this, R.color.custom_material_blue));
        toolbar.inflateMenu(R.menu.setup_menu);

        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
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
        adapter = new SchoolListAdapter();
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

    public void showLoginDialog(final String sid, final boolean auth) {

        Spanned link = Html.fromHtml(getResources().getString(R.string.i_accept) +
                " <a href='ggactivity://text?title=" + R.string.terms_title + "&text=" + R.array.terms + "'>" + getResources().getString(R.string.terms_title) + "</a>");


        AlertDialog.Builder builder = new AlertDialog.Builder(SetupActivity.this);
        builder.setTitle(getResources().getString(R.string.login));

        builder.setView(((LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE)).inflate(R.layout.login_dialog, null));

        builder.setPositiveButton(getResources().getString(R.string.do_login_submit), new DialogInterface.OnClickListener() {

            @Override
            public void onClick(final DialogInterface dialog, int which) {
                String user = auth ? ((EditText) ((Dialog) dialog).findViewById(R.id.usernameInput)).getText().toString() : "_anonymous";
                String pass = auth ? ((EditText) ((Dialog) dialog).findViewById(R.id.passwordInput)).getText().toString() : "";
                String lsid = sid == null ? ((EditText) ((Dialog) dialog).findViewById(R.id.sidInput)).getText().toString() : sid;

                new AsyncTask<String, Integer, Integer>() {

                    @Override
                    public void onPreExecute() {

                    }

                    @Override
                    public void onPostExecute(Integer v) {
                        switch (v) {
                            case 0:
                                startDownloading();
                                break;
                            case 1:
                                Snackbar.make(getWindow().getDecorView().findViewById(R.id.coordinator_layout), getString(R.string.username_or_password_wrong), Snackbar.LENGTH_LONG).show();
                                break;
                            case 2:
                                Snackbar.make(getWindow().getDecorView().findViewById(R.id.coordinator_layout), getString(R.string.could_not_connect), Snackbar.LENGTH_LONG).show();
                                break;
                            case 3:
                                Snackbar.make(getWindow().getDecorView().findViewById(R.id.coordinator_layout), getString(R.string.unknown_error_login), Snackbar.LENGTH_LONG).show();
                                break;
                        }
                    }

                    @Override
                    protected Integer doInBackground(String... params) {

                        return GGApp.GG_APP.remote.login(params[0], params[1], params[2]);

                    }

                }.execute(lsid, user, pass);
                dialog.dismiss();
            }
        });


        builder.setNegativeButton(getString(R.string.abort), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        final AlertDialog dialog = builder.create();
        if(auth) {
            dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        }
        dialog.show();

        if(auth) {
            final EditText passwordInput = (EditText) dialog.findViewById(R.id.passwordInput);
            final CheckBox passwordToggle = (CheckBox) dialog.findViewById(R.id.passwordToggle);
            passwordToggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (!isChecked) {
                        passwordInput.setTransformationMethod(PasswordTransformationMethod.getInstance());
                        passwordInput.setSelection(passwordInput.getText().length());
                    } else {
                        passwordInput.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                        passwordInput.setSelection(passwordInput.getText().length());
                    }
                }
            });
        } else {
            dialog.findViewById(R.id.passwordInput).setVisibility(View.GONE);
            dialog.findViewById(R.id.passwordToggle).setVisibility(View.GONE);
            dialog.findViewById(R.id.usernameInput).setVisibility(View.GONE);
        }

        if(sid != null) {
            dialog.findViewById(R.id.sidInput).setVisibility(View.GONE);
        }

        CheckBox acceptTerms = (CheckBox) dialog.findViewById(R.id.acceptTerms);
        acceptTerms.setMovementMethod(new LinkMovementMethod());
        acceptTerms.setClickable(true);
        acceptTerms.setText(link);

        acceptTerms.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    dialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(isChecked);
                }
        });

        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(false);


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
                if(!GGApp.GG_APP.school.downloadImage()) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Snackbar.make(getWindow().getDecorView().findViewById(R.id.coordinator_layout), getString(R.string.download_error), Snackbar.LENGTH_LONG).show();
                        }
                    });
                }
                if(d.isShowing())
                    d.dismiss();
                Intent i = new Intent(SetupActivity.this, MainActivity.class);
                i.putExtra("reload", true);
                startActivity(i);
                finish();

            }
        }.start();
    }

}
