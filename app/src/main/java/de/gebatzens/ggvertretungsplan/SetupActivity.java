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
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;

public class SetupActivity extends Activity {

    @Override
    public void onCreate(Bundle saved) {
        super.onCreate(saved);

        setContentView(R.layout.activity_setup);

        ListView l = (ListView) findViewById(R.id.setup_list);
        l.setAdapter(new SchoolListAdapter());
        l.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                School s = GGApp.GG_APP.schools.get(position);

                if(s.loginNeeded) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(SetupActivity.this);
                    AlertDialog dialog;
                    builder.setTitle(getResources().getString(R.string.login));
                    builder.setView(((LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE)).inflate(R.layout.login_dialog, null));

                    builder.setPositiveButton(getResources().getString(R.string.do_login_submit), new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(final DialogInterface dialog, int which) {
                            GGApp.GG_APP.activity.mContent.setFragmentLoading();
                            new AsyncTask<Integer, Integer, Integer>() {

                                @Override
                                public void onPostExecute(Integer v) {
                                    switch(v) {
                                        case 0:
                                            Intent intent = new Intent(SetupActivity.this, MainActivity.class);
                                            startActivity(intent);
                                            break;
                                        case 1:
                                            GGApp.GG_APP.showToast(getResources().getString(R.string.username_or_password_wrong));
                                            break;
                                        case 2:
                                            GGApp.GG_APP.showToast(getResources().getString(R.string.could_not_contact_logon_server));
                                            break;
                                        case 3:
                                            GGApp.GG_APP.showToast(getResources().getString(R.string.unknown_error_at_logon));
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


                    builder.setNegativeButton(getResources().getString(R.string.abort), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });

                    dialog = builder.create();
                    dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
                    dialog.show();
                }
            }
        });
    }

}
