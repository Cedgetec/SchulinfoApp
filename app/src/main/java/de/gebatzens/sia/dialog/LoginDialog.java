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
package de.gebatzens.sia.dialog;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Paint;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;

import de.gebatzens.sia.GGApp;
import de.gebatzens.sia.R;
import de.gebatzens.sia.SetupActivity;
import de.gebatzens.sia.TextActivity;

public class LoginDialog extends DialogFragment {

    SetupActivity activity;

    @Override
    public void onAttach(Activity a) {
        super.onAttach(a);
        activity = (SetupActivity) a;
    }

    @Override
    @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final String sid = getArguments().getString("sid");
        final boolean auth = getArguments().getBoolean("auth");

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(getResources().getString(R.string.login));

        builder.setView(((LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.login_dialog, null));

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
                                activity.startDownloading();
                                break;
                            case 1:
                                Snackbar.make(activity.getWindow().getDecorView().findViewById(R.id.coordinator_layout), activity.getString(R.string.username_or_password_wrong), Snackbar.LENGTH_LONG).show();
                                break;
                            case 2:
                                Snackbar.make(activity.getWindow().getDecorView().findViewById(R.id.coordinator_layout), activity.getString(R.string.could_not_connect), Snackbar.LENGTH_LONG).show();
                                break;
                            case 3:
                                Snackbar.make(activity.getWindow().getDecorView().findViewById(R.id.coordinator_layout), activity.getString(R.string.maintenance), Snackbar.LENGTH_LONG).show();
                                break;
                            case 4:
                                Snackbar.make(activity.getWindow().getDecorView().findViewById(R.id.coordinator_layout), activity.getString(R.string.unknown_error_login), Snackbar.LENGTH_LONG).show();
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
        Dialog d = builder.create();
        if(auth)
            d.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        return d;
    }

    @Override
    public void onStart() {
        super.onStart();

        String sid = getArguments().getString("sid");
        boolean auth = getArguments().getBoolean("auth");
        final AlertDialog dialog = (AlertDialog) getDialog();

        ((SetupActivity) getActivity()).currentLoginDialog = dialog;

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

        TextView acceptTermsLink = (TextView) dialog.findViewById(R.id.acceptTermsLink);
        acceptTermsLink.setPaintFlags(acceptTermsLink.getPaintFlags()| Paint.UNDERLINE_TEXT_FLAG);

        acceptTermsLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View widget) {
                Intent i = new Intent(activity, TextActivity.class);
                i.putExtra("title", R.string.terms_title);
                i.putExtra("text", R.array.terms);
                activity.startActivity(i);
            }
        });

        final CheckBox acceptTerms = (CheckBox) dialog.findViewById(R.id.acceptTerms);

        acceptTerms.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked){
                dialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(isChecked);
            }
        });

        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(acceptTerms.isChecked());

    }

}
