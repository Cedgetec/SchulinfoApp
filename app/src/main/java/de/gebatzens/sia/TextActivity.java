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

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.TextView;

public class TextActivity extends AppCompatActivity {


    @Override
    public void onCreate(Bundle bundle) {
        setTheme(R.style.AppThemeIndigoLight);
        super.onCreate(bundle);
        setContentView(R.layout.activity_text);

        Toolbar mToolBar = (Toolbar) findViewById(R.id.toolbar);
        mToolBar.setTitleTextColor(Color.WHITE);
        mToolBar.setBackgroundColor(ContextCompat.getColor(this, R.color.setupColor));
        mToolBar.setNavigationIcon(R.drawable.abc_ic_ab_back_mtrl_am_alpha);
        mToolBar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        Intent intent = getIntent();

        int titleRes = intent.getIntExtra("title", -1);
        int textRes = intent.getIntExtra("text", -1);

        if(titleRes == -1 || textRes == -1) {
            titleRes = Integer.parseInt(intent.getData().getQueryParameter("title"));
            textRes = Integer.parseInt(intent.getData().getQueryParameter("text"));
        }

        mToolBar.setTitle(getString(titleRes));

        TextView tv = (TextView) findViewById(R.id.content_text);

        String[] terms = getResources().getStringArray(textRes);

        tv.setText(terms[0]);
    }

    @Override
    public void onBackPressed() {
        finish();
    }

}
