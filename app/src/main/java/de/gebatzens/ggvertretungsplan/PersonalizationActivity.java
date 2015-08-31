/*
 * Copyright 2015 Lasse Rosenow
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
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SwitchCompat;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.TextView;

import java.lang.reflect.Array;

public class PersonalizationActivity extends AppCompatActivity {

    Toolbar mToolBar;
    ImageView imgColorCircle;
    TextView tv_toggleSubheader;
    SwitchCompat s_darkThemeSwitchButton;
    Context context;
    boolean recreate;

    @Override
    public void onCreate(Bundle bundle) {
        setTheme(GGApp.GG_APP.school.getTheme());
        super.onCreate(bundle);
        setContentView(R.layout.activity_personalisation);
        recreate = false;

        if(bundle != null)
            recreate = bundle.getBoolean("recreate");

        mToolBar = (Toolbar) findViewById(R.id.toolbar);
        mToolBar.setTitleTextColor(Color.WHITE);
        mToolBar.setBackgroundColor(GGApp.GG_APP.school.getColor());
        mToolBar.setNavigationIcon(R.drawable.abc_ic_ab_back_mtrl_am_alpha);
        mToolBar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        mToolBar.setTitle(getTitle());

        s_darkThemeSwitchButton = (SwitchCompat) findViewById(R.id.personalisation_darkThemeSwitchButton);
        tv_toggleSubheader = (TextView) findViewById(R.id.personalisation_toggleSubheader);
        if (GGApp.GG_APP.isDarkThemeEnabled()) {
            s_darkThemeSwitchButton.setChecked(true);
            tv_toggleSubheader.setText(R.string.dark_theme_is_activated);
        } else {
            s_darkThemeSwitchButton.setChecked(false);
            tv_toggleSubheader.setText(R.string.dark_theme_is_not_activated);
        }
        LinearLayout l_toggleDarkTheme = (LinearLayout) findViewById(R.id.personalisation_toggleDarkTheme);
        l_toggleDarkTheme.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View viewIn) {
                recreate = true;
                if (s_darkThemeSwitchButton.isChecked()) {
                    s_darkThemeSwitchButton.setChecked(false);
                    GGApp.GG_APP.setDarkThemeEnabled(false);
                    tv_toggleSubheader.setText(R.string.dark_theme_is_not_activated);
                    GGApp.GG_APP.school.loadTheme();
                    recreate();
                } else {
                    s_darkThemeSwitchButton.setChecked(true);
                    GGApp.GG_APP.setDarkThemeEnabled(true);
                    tv_toggleSubheader.setText(R.string.dark_theme_is_activated);
                    GGApp.GG_APP.school.loadTheme();
                    recreate();
                }
            }
        });

        imgColorCircle = (ImageView) findViewById(R.id.personalisation_themeColorCircle);
        imgColorCircle.setBackgroundResource(R.drawable.colored_circle);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            GradientDrawable drawable = (GradientDrawable) imgColorCircle.getBackground();
            drawable.setColorFilter(GGApp.GG_APP.school.getColor(), PorterDuff.Mode.SRC_ATOP);
        }

        LinearLayout l_changeColor = (LinearLayout) findViewById(R.id.personalisation_changeColor);
        l_changeColor.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View viewIn) {
                final String[] themeNames = {
                        "Red",
                        "Pink",
                        "Purple",
                        "DeepPurple",
                        "Indigo",
                        "Blue",
                        "LightBlue",
                        "Cyan",
                        "Teal",
                        "Green",
                        "LightGreen",
                        "Lime",
                        "Yellow",
                        "Amber",
                        "Orange",
                        "DeepOrange",
                        "Brown",
                        "Grey",
                        "BlueGrey",
                        "Default"
                };

                ListAdapter adapter = new ArrayAdapter<String>(
                        getApplicationContext(), R.layout.custom_theme_choose_list, themeNames) {

                    ViewHolder holder;

                    class ViewHolder {
                        ImageView icon;
                        TextView title;
                    }

                    public View getView(int position, View convertView, ViewGroup parent) {
                        final LayoutInflater inflater = (LayoutInflater) getApplicationContext()
                                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

                        if (convertView == null) {
                            convertView = inflater.inflate(
                                    R.layout.custom_theme_choose_list, null);

                            holder = new ViewHolder();
                            holder.icon = (ImageView) convertView.findViewById(R.id.ThemeIcon);
                            holder.title = (TextView) convertView.findViewById(R.id.ThemeName);
                            convertView.setTag(holder);
                        } else {
                            holder = (ViewHolder) convertView.getTag();
                        }
                        holder.icon.setBackgroundResource(R.drawable.colored_circle);
                        holder.icon.getBackground().setColorFilter(loadThemeColor(themeNames[position]), PorterDuff.Mode.SRC_ATOP);
                        TypedArray theme_color_names_typedarray = getResources().obtainTypedArray(R.array.theme_color_names);
                        CharSequence[] theme_color_names = theme_color_names_typedarray.getTextArray(0);
                        holder.title.setText(theme_color_names[position]);
                        if (GGApp.GG_APP.isDarkThemeEnabled()) {
                            holder.title.setTextColor(Color.parseColor("#fafafa"));
                        } else{
                            holder.title.setTextColor(Color.parseColor("#424242"));
                        }
                        return convertView;
                    }
                };

                AlertDialog.Builder builder = new AlertDialog.Builder(PersonalizationActivity.this);
                builder.setTitle(getResources().getString(R.string.personalisation_pickColor));

                builder.setAdapter(adapter, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        GGApp.GG_APP.setCustomThemeName(themeNames[which]);
                        GGApp.GG_APP.school.loadTheme();
                        recreate = true;
                        recreate();
                    }

                });
                builder.setPositiveButton(getResources().getString(R.string.abort), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        //nothing
                    }
                });
                builder.create();
                builder.show();
            }
        });

    }

    public int loadThemeColor(String name) {
        int theme = GGApp.GG_APP.getResources().getIdentifier(GGApp.GG_APP.isDarkThemeEnabled() ? "AppTheme" + name + "Dark" : "AppTheme" + name + "Light", "style", GGApp.GG_APP.getPackageName());
        return GGApp.GG_APP.obtainStyledAttributes(theme, new int [] {R.attr.colorPrimary}).getColor(0, Color.RED);
    }

    @Override
    public void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        bundle.putBoolean("recreate", recreate);
    }

    @Override
    public void finish() {
        setResult(recreate ? RESULT_OK : RESULT_CANCELED);
        super.finish();
    }

    @Override
    public void onBackPressed() {
        finish();
    }

}
