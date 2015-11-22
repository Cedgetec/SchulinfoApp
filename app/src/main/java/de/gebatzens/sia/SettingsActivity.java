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
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceFragment;
import android.preference.SwitchPreference;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.TextView;

public class SettingsActivity extends AppCompatActivity {

    Toolbar mToolBar;
    private static boolean changed, recreate;
    static String version;
    GGPFragment frag;

    public static class GGPFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {

        @Override
        public void onCreate(Bundle s) {
            super.onCreate(s);
            final GGApp gg = GGApp.GG_APP;

            addPreferencesFromResource(R.xml.preferences);
            SharedPreferences sp = getPreferenceScreen().getSharedPreferences();
            sp.registerOnSharedPreferenceChangeListener(this);

            CheckBoxPreference notifications = (CheckBoxPreference) findPreference("notifications");

            findPreference("background_updates").setEnabled(notifications.isChecked());
            findPreference("notification_led").setEnabled(notifications.isChecked());
            findPreference("vibration").setEnabled(notifications.isChecked());

            Preference pref_buildversion = findPreference("buildversion");
            String versionName = BuildConfig.VERSION_NAME;
            pref_buildversion.setSummary("Version: " + versionName + " (" + BuildConfig.BUILD_TYPE + ")");

            Preference prefGithub = findPreference("githublink");
            prefGithub.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                public boolean onPreferenceClick(Preference preference) {
                    Intent linkIntent = new Intent(Intent.ACTION_VIEW);
                    linkIntent.setData(Uri.parse("https://github.com/Cedgetec/SchulinfoAPP"));
                    startActivity(linkIntent);
                    return true;
                }
            });

            Preference license = findPreference("license");
            license.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                public boolean onPreferenceClick(Preference preference) {
                    Intent linkIntent = new Intent(Intent.ACTION_VIEW);
                    linkIntent.setData(Uri.parse("http://www.apache.org/licenses/LICENSE-2.0"));
                    startActivity(linkIntent);
                    return true;
                }
            });

            Preference prefTerms = findPreference("terms");
            prefTerms.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                public boolean onPreferenceClick(Preference preference) {
                    Intent intent = new Intent(getActivity(), TextActivity.class);
                    intent.putExtra("title", R.string.terms_title);
                    intent.putExtra("text", R.array.terms);
                    startActivity(intent);
                    return true;
                }
            });

            Preference theme_color = findPreference("theme_color");

            GradientDrawable tcgd = (GradientDrawable) ContextCompat.getDrawable(getActivity(), R.drawable.settings_circle);
            tcgd.setColor(GGApp.GG_APP.school.getColor());
            theme_color.setIcon(tcgd);

            final String[] themeNames = getResources().getStringArray(R.array.theme_names);

            final ListAdapter adapter = new ArrayAdapter<String>(
                    getActivity().getApplicationContext(), R.layout.custom_theme_choose_list, themeNames) {

                ViewHolder holder;

                class ViewHolder {
                    ImageView icon;
                    TextView title;
                }

                public View getView(int position, View convertView, ViewGroup parent) {
                    final LayoutInflater inflater = (LayoutInflater) getActivity().getApplicationContext()
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
                    String[] theme_color_names = getResources().getStringArray(R.array.theme_color_names);
                    holder.title.setText(theme_color_names[position]);
                    if (GGApp.GG_APP.isDarkThemeEnabled()) {
                        holder.title.setTextColor(Color.parseColor("#fafafa"));
                    } else{
                        holder.title.setTextColor(Color.parseColor("#424242"));
                    }
                    return convertView;
                }
            };

            theme_color.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {

                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                    builder.setTitle(getResources().getString(R.string.personalisation_pickColor));

                    builder.setAdapter(adapter, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            GGApp.GG_APP.setCustomThemeName(themeNames[which]);
                            GGApp.GG_APP.school.loadTheme();
                            recreate = true;
                            getActivity().recreate();
                        }

                    });
                    builder.setPositiveButton(getResources().getString(R.string.abort), new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            //nothing
                        }
                    });
                    builder.create();
                    builder.show();

                    return false;
                }
            });

            final Preference pref_username = findPreference("authentication_username");

            if(GGApp.GG_APP.school.loginNeeded) {
                pref_username.setSummary(getString(R.string.logged_in_as, GGApp.GG_APP.remote.getUsername()));
            }

            pref_username.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    if(gg.remote.isLoggedIn()) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                        builder.setTitle(getResources().getString(R.string.logout));
                        builder.setMessage(getResources().getString(R.string.logout_confirm));
                        builder.setPositiveButton(getResources().getString(R.string.yes), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                GGApp.GG_APP.remote.logout();
                                Intent i = new Intent();
                                i.putExtra("setup", true);
                                ((SettingsActivity) getActivity()).finish(RESULT_OK, i);
                                dialog.dismiss();
                            }
                        });
                        builder.setNegativeButton(getResources().getString(R.string.no), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });
                        builder.create().show();
                    } else {
                        Snackbar.make(getActivity().getWindow().getDecorView().findViewById(R.id.coordinator_layout), getString(R.string.not_logged_in), Snackbar.LENGTH_LONG).show();
                    }

                    return false;
                }
            });

            Preference filter = findPreference("filter");
            filter.setSummary(GGApp.GG_APP.filters.mainFilter.filter.isEmpty() ? getActivity().getString(R.string.no_filter_active)
                    : GGApp.GG_APP.filters.size() == 0 ? getActivity().getString(R.string.filter_active) :
                    getActivity().getString(R.string.filters_active, GGApp.GG_APP.filters.size() + 1));
            filter.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Intent i = new Intent(getActivity(), FilterActivity.class);
                    getActivity().startActivityForResult(i, 1);
                    return false;
                }
            });

            Preference helpdesk = findPreference("helpdesk");
            LayerDrawable ld = (LayerDrawable) ContextCompat.getDrawable(getActivity(), R.drawable.settings_circle_with_mail_icon);
            GradientDrawable gd = (GradientDrawable) ld.findDrawableByLayerId(R.id.first_image);
            gd.setColor(GGApp.GG_APP.school.getColor());
            helpdesk.setIcon(ld);
            helpdesk.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts(
                            "mailto", "support@cedgetec.com", null));
                    emailIntent.putExtra(Intent.EXTRA_SUBJECT, "SchulinfoApp " + GGApp.GG_APP.school.name);
                    startActivity(Intent.createChooser(emailIntent, "E-Mail senden"));
                    return false;
                }
            });

            SwitchPreference darkMode = (SwitchPreference) findPreference("toggleThemeMode");
            darkMode.setChecked(GGApp.GG_APP.isDarkThemeEnabled());

        }

        @Override
         public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            Preference pref = findPreference(key);

            changed = true;

            if(key.equals("toggleThemeMode")) {
                boolean b = sharedPreferences.getBoolean(key, true);
                if(b != GGApp.GG_APP.isDarkThemeEnabled()) {
                    GGApp.GG_APP.setDarkThemeEnabled(b);
                    GGApp.GG_APP.school.loadTheme();
                    recreate = true;
                    getActivity().recreate();
                }
            }

            if(key.equals("notifications")) {
                CheckBoxPreference no = (CheckBoxPreference) pref;

                findPreference("background_updates").setEnabled(no.isChecked());
                findPreference("notification_led").setEnabled(no.isChecked());
                findPreference("vibration").setEnabled(no.isChecked());
            }

        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        }

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(GGApp.GG_APP.school.getTheme());

        changed = false;
        recreate = false;

        Fragment f = getFragmentManager().findFragmentByTag("gg_settings_frag");
        if(f != null) {
            getFragmentManager().beginTransaction().remove(f).commit();
        }

        super.onCreate(savedInstanceState);

        ViewGroup contentView = (ViewGroup) LayoutInflater.from(this).inflate(
                R.layout.activity_settings, new LinearLayout(this), false);

        if(savedInstanceState != null) {
            changed = savedInstanceState.getBoolean("ggs_changed");
            recreate = savedInstanceState.getBoolean("ggs_recreate");
        }

        mToolBar = (Toolbar) contentView.findViewById(R.id.toolbar);
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

        frag = new GGPFragment();
        getFragmentManager().beginTransaction().replace(R.id.content_wrapper, frag, "gg_settings_frag").commit();

        setContentView(contentView);

    }


    @Override
    public void onSaveInstanceState(Bundle b) {
        super.onSaveInstanceState(b);
        b.putBoolean("ggs_changed", changed);
        b.putBoolean("ggs_recreate", recreate);
    }

    @Override
    public void onActivityResult(int req, int resp, Intent intent) {
        if(req == 1 && resp == RESULT_OK) {
            changed = true;
            Preference filter = frag.findPreference("filter");
            filter.setSummary(GGApp.GG_APP.filters.mainFilter.filter.isEmpty() ? "Kein Filter aktiv" : (GGApp.GG_APP.filters.size() + 1) + " Filter aktiv");
        }
    }

    @Override
    public void onBackPressed() {
        finish();
    }

    public void finish(int r, Intent i) {
        setResult(r, i);
        super.finish();
    }

    @Override
    public void finish() {
        Intent i = new Intent();
        i.putExtra("recreate", recreate);
        setResult(changed ? RESULT_OK : RESULT_CANCELED, i);
        super.finish();
    }

    public static int loadThemeColor(String name) {
        int theme = GGApp.GG_APP.getResources().getIdentifier(GGApp.GG_APP.isDarkThemeEnabled() ? "AppTheme" + name + "Dark" : "AppTheme" + name + "Light", "style", GGApp.GG_APP.getPackageName());
        return GGApp.GG_APP.obtainStyledAttributes(theme, new int [] {R.attr.colorPrimary}).getColor(0, Color.RED);
    }

}
