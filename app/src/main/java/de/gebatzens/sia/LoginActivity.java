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

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Paint;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDelegate;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

public class LoginActivity extends AppCompatActivity {

    Toolbar mToolBar;

    @Override
    protected void onCreate(Bundle bundle) {
        AppCompatDelegate.setDefaultNightMode(SIAApp.SIA_APP.getThemeMode());
        setTheme(R.style.AppThemeSetup);
        super.onCreate(bundle);

        setContentView(R.layout.activity_login);

        mToolBar = (Toolbar) findViewById(R.id.toolbar);
        mToolBar.setTitle(getTitle());

        TextView acceptTermsLink = (TextView) findViewById(R.id.acceptTermsLink);
        acceptTermsLink.setPaintFlags(acceptTermsLink.getPaintFlags()| Paint.UNDERLINE_TEXT_FLAG);

        acceptTermsLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View widget) {
                Intent i = new Intent(LoginActivity.this, TextActivity.class);
                i.putExtra("title", R.string.terms_title);
                i.putExtra("text", R.array.terms);
                LoginActivity.this.startActivity(i);
            }
        });

        CheckBox acceptTerms = (CheckBox) findViewById(R.id.acceptTerms);
        final Button loginButton = (Button) findViewById(R.id.login_button);

        acceptTerms.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked){
                loginButton.setEnabled(isChecked);
            }
        });

        loginButton.setEnabled(acceptTerms.isChecked());
    }

}