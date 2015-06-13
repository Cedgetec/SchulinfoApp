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

import android.app.Activity;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;

import javax.net.ssl.HttpsURLConnection;

public class HelpdeskActivity extends Activity {

    Toolbar mToolBar;

    @Override
    public void onCreate(Bundle bundle) {
        setTheme(GGApp.GG_APP.school.getTheme());
        super.onCreate(bundle);
        setContentView(R.layout.activity_helpdesk);

        mToolBar = (Toolbar) findViewById(R.id.toolbar);
        mToolBar.setBackgroundColor(GGApp.GG_APP.school.getColor());
        mToolBar.setTitleTextColor(Color.WHITE);
        mToolBar.setNavigationIcon(R.drawable.abc_ic_ab_back_mtrl_am_alpha);
        mToolBar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        mToolBar.setTitle(getTitle());

        final TextView mTextViewName = (TextView) findViewById(R.id.reportName);
        final TextView mTextViewEmail = (TextView) findViewById(R.id.reportEmail);
        final TextView mTextViewSubject = (TextView) findViewById(R.id.reportSubject);
        final TextView mTextViewMessage = (TextView) findViewById(R.id.reportMessage);

        String s1 = GGApp.GG_APP.remote.getFirstName();
        String s2 = GGApp.GG_APP.remote.getLastName();
        if(s1 != null && s2 != null) {
            mTextViewName.setText(s1 + " " + s2);
        }

        mToolBar.inflateMenu(R.menu.helpdesk_menu);
        mToolBar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                        String name = mTextViewName.getText().toString().replaceAll("<", "&lt;").replaceAll(">", "&gt;");
                        String email = mTextViewEmail.getText().toString().replaceAll("<", "&lt;").replaceAll(">", "&gt;");
                        String subject = mTextViewSubject.getText().toString().replaceAll("<", "&lt;").replaceAll(">", "&gt;");
                        String message = mTextViewMessage.getText().toString().replaceAll("<", "&lt;").replaceAll(">", "&gt;");

                        new AsyncTask<String, Integer, Integer>() {
                            @Override
                            protected Integer doInBackground(String... params) {
                                try {
                                    if ((params[0] != null) && (params[1] != null) && (params[2] != null) && (params[3] != null) && !params[0].equals("") && !params[1].equals("") && !params[2].equals("") && !params[3].equals("")) {
                                        HttpsURLConnection con = (HttpsURLConnection) new URL("https://" + BuildConfig.BACKEND_SERVER + "/infoapp/infoapp_helpdesk.php").openConnection();

                                        con.setRequestMethod("POST");
                                        //con.setSSLSocketFactory(GGRemote.sslSocketFactory);

                                        con.setDoOutput(true);
                                        DataOutputStream wr = new DataOutputStream(con.getOutputStream());
                                        String urlParams;
                                        if (GGApp.GG_APP.remote.getUsername() != null && !GGApp.GG_APP.remote.getUsername().equals("")) {
                                            urlParams = "name=" + URLEncoder.encode(params[0], "UTF-8") + "&email=" + URLEncoder.encode(params[1], "UTF-8") + "&subject=" + URLEncoder.encode(params[2], "UTF-8") + "&message=" + URLEncoder.encode(params[3], "UTF-8") + "&username=" + URLEncoder.encode(GGApp.GG_APP.remote.getUsername(), "UTF-8");
                                        } else {
                                            urlParams = "name=" + URLEncoder.encode(params[0], "UTF-8") + "&email=" + URLEncoder.encode(params[1], "UTF-8") + "&subject=" + URLEncoder.encode(params[2], "UTF-8") + "&message=" + URLEncoder.encode(params[3], "UTF-8");
                                        }
                                        Log.d("urlParams", urlParams);
                                        wr.writeBytes(urlParams);
                                        wr.flush();
                                        wr.close();

                                        if (con.getResponseCode() == 200) {
                                            BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream()));
                                            String line;
                                            StringBuilder sb = new StringBuilder();
                                            while ((line = br.readLine()) != null) {
                                                sb.append(line);
                                            }
                                            br.close();
                                            if (sb.toString().contains("<state>true</state>")) {
                                                return 0;
                                            } else {
                                                return 2;
                                            }
                                        } else {
                                            return 2;
                                        }

                                    } else {
                                        return 1;
                                    }
                                } catch (MalformedURLException e) {
                                    e.printStackTrace();
                                    return 2;
                                } catch (IOException e) {
                                    e.printStackTrace();
                                    return 2;
                                }
                            }

                            @Override
                            protected void onPostExecute(Integer result) {
                                if (result == 0) {
                                    Snackbar.make(getWindow().getDecorView().findViewById(R.id.coordinator_layout), getString(R.string.message_sent_successfully), Snackbar.LENGTH_LONG).show();
                                    finish();
                                } else if (result == 1) {
                                    Snackbar.make(getWindow().getDecorView().findViewById(R.id.coordinator_layout), getString(R.string.please_fill_out_all_inputs), Snackbar.LENGTH_LONG).show();
                                } else if (result == 2) {
                                    Snackbar.make(getWindow().getDecorView().findViewById(R.id.coordinator_layout), getString(R.string.error_while_sending_message), Snackbar.LENGTH_LONG).show();
                                }
                            }
                        }.execute(name, email, subject, message);
                return false;
            }

            });
        }

            @Override
            public void onBackPressed() {
                finish();
            }
        }
