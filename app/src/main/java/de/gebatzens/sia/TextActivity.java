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
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

public class TextActivity extends AppCompatActivity {

    Toolbar mToolBar;

    @Override
    public void onCreate(Bundle bundle) {
        setTheme(SIAApp.GG_APP.school == null ? R.style.AppThemeSetup : SIAApp.GG_APP.school.getTheme());
        super.onCreate(bundle);
        setContentView(R.layout.activity_text);

        Intent intent = getIntent();

        int titleRes = intent.getIntExtra("title", -1);
        int textRes = intent.getIntExtra("text", -1);

        if(titleRes == -1 || textRes == -1) {
            titleRes = Integer.parseInt(intent.getData().getQueryParameter("title"));
            textRes = Integer.parseInt(intent.getData().getQueryParameter("text"));
        }

        mToolBar = (Toolbar) findViewById(R.id.toolbar);
        mToolBar.setTitle(getResources().getString(titleRes));
        mToolBar.setNavigationIcon(R.drawable.ic_arrow_back);
        mToolBar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        LinearLayout lcontent = (LinearLayout) findViewById(R.id.textactivityContent);
        if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            lcontent.setPadding(toPixels(55), toPixels(5), toPixels(55), toPixels(5));
        } else if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            lcontent.setPadding(toPixels(5), toPixels(5), toPixels(5), toPixels(5));
        }

        String[] terms = getResources().getStringArray(textRes);

        for (int i = 0; i < terms.length; i++){
            CardView cv = (CardView) getLayoutInflater().inflate(R.layout.basic_cardview, lcontent, false);
            LinearLayout l = new LinearLayout(this);
            l.setOrientation(LinearLayout.HORIZONTAL);
            TextView tv = (TextView) getLayoutInflater().inflate(R.layout.basic_textview_primary, l, false);
            tv.setText(terms[i]);
            TextView tv2 = (TextView) getLayoutInflater().inflate(R.layout.basic_textview_secondary, l, false);
            tv2.setText(i+1 + ". ");
            l.addView(tv2);
            l.addView(tv);
            cv.addView(l);
            lcontent.addView(cv);
        }
    }

    public static int toPixels(float dp) {
        float scale = SIAApp.GG_APP.getResources().getDisplayMetrics().density;
        return (int) (dp * scale);
    }

    @Override
    public void onBackPressed() {
        finish();
    }

}
