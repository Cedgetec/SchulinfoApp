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

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.Spannable;
import android.text.Spanned;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.LinkMovementMethod;
import android.text.method.PasswordTransformationMethod;
import android.text.util.Linkify;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.util.regex.Pattern;

import de.gebatzens.ggvertretungsplan.fragment.RemoteDataFragment;

public class SetupActivity extends AppCompatActivity {

    SchoolListAdapter adapter;
    ListView list;

    @Override
    public void onCreate(Bundle saved) {
        setTheme(R.style.AppThemeIndigoLight);
        super.onCreate(saved);

        if(GGApp.GG_APP.remote.isLoggedIn()) {
            startActivity(new Intent(this, MainActivity.class));
            startDownloadThread(false);
            return;
        }

        setContentView(R.layout.activity_setup);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitleTextColor(Color.WHITE);
        toolbar.setTitle(getString(R.string.app_name));
        toolbar.inflateMenu(R.menu.setup_menu);

        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                if (menuItem.getItemId() == R.id.setup_refresh) {
                    showDownloadDialog();
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
                GGApp.GG_APP.setSchool(s.sid);

                Spanned link = Html.fromHtml(getResources().getString(R.string.i_accept) +
                        " <a href='ggactivity://text?title=" + R.string.terms_title + "&text=" + R.string.terms + "'>" + getResources().getString(R.string.terms_title) + "</a>");

                if (s.loginNeeded) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(SetupActivity.this);
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

                    final AlertDialog dialog = builder.create();
                    dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
                    dialog.show();
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


                } else {
                    AlertDialog.Builder builder = new AlertDialog.Builder(SetupActivity.this);
                    builder.setTitle(getResources().getString(R.string.login));

                    TextView text = new TextView(SetupActivity.this);
                    text.setTextSize(15);
                    int p = RemoteDataFragment.toPixels(20);
                    text.setPadding(p, p, p, p);
                    text.setMovementMethod(new LinkMovementMethod());
                    text.setText(link);
                    builder.setView(text);

                    builder.setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();

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
                                            Snackbar.make(getWindow().getDecorView().findViewById(R.id.coordinator_layout), getString(R.string.could_not_connect), Snackbar.LENGTH_LONG).show();
                                            break;
                                        case 3:
                                            Snackbar.make(getWindow().getDecorView().findViewById(R.id.coordinator_layout), getString(R.string.unknown_error_login), Snackbar.LENGTH_LONG).show();
                                            break;
                                    }
                                }

                                @Override
                                protected Integer doInBackground(Integer... params) {
                                    return GGApp.GG_APP.remote.login(null, null);
                                }
                            }.execute();
                        }
                    });
                    builder.setNegativeButton(getString(R.string.no), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
                    builder.create().show();

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
                GGApp.GG_APP.setSchool(GGApp.GG_APP.getDefaultSID());

            }
        }.start();
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
                        if(!b)
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
                            Snackbar.make(getWindow().getDecorView().findViewById(R.id.coordinator_layout), getString(R.string.download_error), Snackbar.LENGTH_LONG).show();
                        }
                    });
                }
                d.dismiss();
                Intent i = new Intent(SetupActivity.this, MainActivity.class);
                i.putExtra("reload", true);
                i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(i);

            }
        }.start();
    }

}
