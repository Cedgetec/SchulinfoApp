/*
 * Copyright (C) 2015 Fabian Schultis
 *
 * This file is part of GGVertretungsplan.
 *
 * GGVertretungsplan is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * GGVertretungsplan is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with GGVertretungsplan.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.gebatzens.ggvertretungsplan;

import android.app.Activity;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import javax.net.ssl.HttpsURLConnection;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;

public class HelpdeskActivity extends Activity {

    Toolbar mToolBar;

    @Override
    public void onCreate(Bundle bundle) {
        setTheme(GGApp.GG_APP.provider.getTheme());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            GGApp.GG_APP.setStatusBarColor(getWindow());
        }
        super.onCreate(bundle);
        setContentView(R.layout.activity_helpdesk);

        mToolBar = (Toolbar) findViewById(R.id.toolbar);
        mToolBar.setBackgroundColor(GGApp.GG_APP.provider.getColor());
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

        String s1 = GGApp.GG_APP.provider.prefs.getString("firstname", null);
        String s2 = GGApp.GG_APP.provider.prefs.getString("lastname", null);
        if(s1 != null && s2 != null) {
            mTextViewName.setText(s1 + " " + s2);
        }

        mToolBar.inflateMenu(R.menu.helpdesk_menu);
        mToolBar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                        String name = mTextViewName.getText().toString();
                        String email = mTextViewEmail.getText().toString();
                        String subject = mTextViewSubject.getText().toString();
                        String message = mTextViewMessage.getText().toString();

                        new AsyncTask<String, Integer, Integer>() {
                            @Override
                            protected Integer doInBackground(String... params) {
                                try {
                                    if ((params[0] != null) && (params[1] != null) && (params[2] != null) && (params[3] != null) && !params[0].equals("") && !params[1].equals("") && !params[2].equals("") && !params[3].equals("")) {
                                        HttpsURLConnection con = (HttpsURLConnection) new URL("https://gymnasium-glinde.logoip.de/infoapp/infoapp_helpdesk.php").openConnection();

                                        con.setRequestMethod("POST");
                                        con.setSSLSocketFactory(GGProvider.sslSocketFactory);

                                        con.setDoOutput(true);
                                        DataOutputStream wr = new DataOutputStream(con.getOutputStream());
                                        String urlParams;
                                        if (!GGApp.GG_APP.provider.getUsername().equals("")) {
                                            urlParams = "name=" + URLEncoder.encode(params[0], "UTF-8") + "&email=" + URLEncoder.encode(params[1], "UTF-8") + "&subject=" + URLEncoder.encode(params[2], "UTF-8") + "&message=" + URLEncoder.encode(params[3], "UTF-8") + "&username=" + URLEncoder.encode(GGApp.GG_APP.provider.getUsername(), "UTF-8");
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
                                    Toast.makeText(getApplicationContext(), getResources().getString(R.string.message_sent_successfully), Toast.LENGTH_LONG).show();
                                    finish();
                                } else if (result == 1) {
                                    Toast.makeText(getApplicationContext(), getResources().getString(R.string.please_fill_out_all_inputs), Toast.LENGTH_LONG).show();
                                } else if (result == 2) {
                                    Toast.makeText(getApplicationContext(), getResources().getString(R.string.error_while_sending_message), Toast.LENGTH_LONG).show();
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
