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

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ShortcutManager;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.customtabs.CustomTabsIntent;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDelegate;
import android.support.v7.preference.CheckBoxPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SettingsActivity extends AppCompatActivity {

    Toolbar mToolBar;
    private static boolean changed, recreate;
    static String version;
    CustomPreferenceFragment frag;

    public static class CustomPreferenceFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {

        @Override
        public void onCreatePreferences(Bundle s, String str) {
            final SIAApp gg = SIAApp.SIA_APP;

            addPreferencesFromResource(R.xml.preferences);
            SharedPreferences sp = getPreferenceScreen().getSharedPreferences();
            sp.registerOnSharedPreferenceChangeListener(this);

            CheckBoxPreference notifications = (CheckBoxPreference) findPreference("notifications");

            findPreference("background_updates").setEnabled(notifications.isChecked());
            findPreference("notification_led_color").setEnabled(notifications.isChecked());
            findPreference("vibration").setEnabled(notifications.isChecked());

            Preference pref_buildversion = findPreference("buildversion");
            String versionName = BuildConfig.VERSION_NAME;
            pref_buildversion.setSummary("Version: " + versionName + " (" + BuildConfig.BUILD_TYPE + ")");

            Preference prefGithub = findPreference("githublink");
            prefGithub.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                public boolean onPreferenceClick(Preference preference) {
                    createCustomTab(getActivity(), "https://github.com/Cedgetec/SchulinfoAPP");
                    return true;
                }
            });

            Preference license = findPreference("license");
            license.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                public boolean onPreferenceClick(Preference preference) {
                    createCustomTab(getActivity(), "http://www.apache.org/licenses/LICENSE-2.0");
                    return true;
                }
            });

            Preference prefTerms = findPreference("terms");
            prefTerms.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                public boolean onPreferenceClick(Preference preference) {
                    Intent intent = new Intent(getActivity(), TextActivity.class);
                    intent.putExtra("title", R.string.terms_title);
                    intent.putExtra("text", R.array.terms);
                    startActivity(intent);
                    return true;
                }
            });

            Preference prefPrivacy = findPreference("privacy");
            prefPrivacy.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                public boolean onPreferenceClick(Preference preference) {
                    Intent intent = new Intent(getActivity(), TextActivity.class);
                    intent.putExtra("title", R.string.privacy_statement);
                    intent.putExtra("text", R.array.privacy_content);
                    startActivity(intent);
                    return true;
                }
            });

            final Preference notification_led_color = findPreference("notification_led_color");

            final List<String> ledColors = new ArrayList<>(Arrays.asList(getResources().getStringArray(R.array.notification_colors)));
            final List<String> ledNames = new ArrayList<>(Arrays.asList(getResources().getStringArray(R.array.notification_color_names)));

            notification_led_color.setSummary(ledNames.get(ledColors.indexOf(SIAApp.SIA_APP.getLedColor())));

            final ListAdapter adapter_notification_led_color = new ArrayAdapter<String>(
                    getActivity().getApplicationContext(), R.layout.settings_custom_list_preference, ledColors) {

                ViewHolder holder;

                class ViewHolder {
                    ImageView icon;
                    TextView title;
                    ImageView selectionIcon;
                }

                public View getView(int position, View convertView, ViewGroup parent) {
                    View v = convertView == null ? getActivity().getLayoutInflater().inflate(R.layout.settings_custom_list_preference, parent, false) : convertView;
                    v.setLayoutParams(new AbsListView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

                    holder = new ViewHolder();
                    holder.icon = (ImageView) v.findViewById(R.id.ThemeIcon);
                    holder.title = (TextView) v.findViewById(R.id.ThemeName);
                    holder.selectionIcon = (ImageView) v.findViewById(R.id.SelectedThemeIcon);
                    v.setTag(holder);

                    if(position == 0) {
                        holder.icon.setBackgroundResource(R.drawable.ic_highlight_off);
                    } else {
                        holder.icon.setBackgroundResource(R.drawable.colored_circle);
                        ((GradientDrawable) holder.icon.getBackground()).setColor(Color.parseColor(ledColors.get(position)));
                    }

                    holder.title.setText(ledNames.get(position));

                    holder.selectionIcon.setColorFilter(SIAApp.SIA_APP.school.getAccentColor() ,PorterDuff.Mode.SRC_ATOP);

                    if(SIAApp.SIA_APP.getLedColor().equals(ledColors.get(position))) {
                        holder.selectionIcon.setVisibility(View.VISIBLE);
                    }
                    else {
                        holder.selectionIcon.setVisibility(View.GONE);
                    }
                    return v;
                }
            };

            notification_led_color.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {

                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                    builder.setTitle(getResources().getString(R.string.personalisation_pickColor));

                    builder.setAdapter(adapter_notification_led_color, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            notification_led_color.setSummary(ledNames.get(which));
                            SIAApp.SIA_APP.setLedColor(ledColors.get(which));
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

            Preference theme_color = findPreference("theme_color");

            boolean winter = SIAApp.SIA_APP.getCurrentThemeName().equals("Winter");
            boolean summer = SIAApp.SIA_APP.getCurrentThemeName().equals("Summer");

            if(winter) {
                LayerDrawable ld = (LayerDrawable) ContextCompat.getDrawable(getActivity(), R.drawable.settings_circle_image);
                ld.setDrawableByLayerId(R.id.image, ContextCompat.getDrawable(getActivity(), R.drawable.snow_circle));
                ((GradientDrawable) ld.findDrawableByLayerId(R.id.background)).setColor(SIAApp.SIA_APP.school.getColor());
                theme_color.setIcon(ld);
            } else if (summer) {
                LayerDrawable ld = (LayerDrawable) ContextCompat.getDrawable(getActivity(), R.drawable.settings_circle_image);
                ld.setDrawableByLayerId(R.id.image, ContextCompat.getDrawable(getActivity(), R.drawable.boat_circle));
                ((GradientDrawable) ld.findDrawableByLayerId(R.id.background)).setColor(SIAApp.SIA_APP.school.getColor());
                theme_color.setIcon(ld);
            } else {
                GradientDrawable tcgd = (GradientDrawable) ContextCompat.getDrawable(getActivity(), R.drawable.settings_circle);
                tcgd.setColor(SIAApp.SIA_APP.school.getColor());
                theme_color.setIcon(tcgd);
            }

            final List<String> themeIds = new ArrayList<>(Arrays.asList(getResources().getStringArray(R.array.theme_names)));
            final List<String> themeNames = new ArrayList<>(Arrays.asList(getResources().getStringArray(R.array.theme_color_names)));

            String st = SIAApp.SIA_APP.getSeasonTheme();
            switch (st) {
                case "Winter":
                    themeIds.add(0, st);
                    themeNames.add(0, getString(R.string.winter));
                    break;
                case "Summer":
                    themeIds.add(0, st);
                    themeNames.add(0, getString(R.string.summer));
                    break;
            }


            final ListAdapter adapter_theme_color = new ArrayAdapter<String>(
                    getActivity().getApplicationContext(), R.layout.settings_custom_list_preference, themeIds) {

                ViewHolder holder;

                class ViewHolder {
                    ImageView icon;
                    TextView title;
                    ImageView selectionIcon;
                }

                public View getView(int position, View convertView, ViewGroup parent) {
                    View v = convertView == null ? getActivity().getLayoutInflater().inflate(R.layout.settings_custom_list_preference, parent, false) : convertView;
                    v.setLayoutParams(new AbsListView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

                    holder = new ViewHolder();
                    holder.icon = (ImageView) v.findViewById(R.id.ThemeIcon);
                    holder.title = (TextView) v.findViewById(R.id.ThemeName);
                    holder.selectionIcon = (ImageView) v.findViewById(R.id.SelectedThemeIcon);
                    v.setTag(holder);

                    boolean winter = themeIds.get(position).equals("Winter");
                    boolean summer = themeIds.get(position).equals("Summer");

                    holder.icon.setBackgroundResource(winter || summer ? R.drawable.colored_circle_image : R.drawable.colored_circle);
                    if(winter) {
                        LayerDrawable ld = (LayerDrawable) holder.icon.getBackground();
                        ld.setDrawableByLayerId(R.id.image, ContextCompat.getDrawable(getActivity(), R.drawable.snow_circle));
                        ((GradientDrawable) ld.findDrawableByLayerId(R.id.background)).setColor(loadThemeColor(themeIds.get(position)));
                    } else if(summer) {
                        LayerDrawable ld = (LayerDrawable) holder.icon.getBackground();
                        ld.setDrawableByLayerId(R.id.image, ContextCompat.getDrawable(getActivity(), R.drawable.boat_circle));
                        ((GradientDrawable) ld.findDrawableByLayerId(R.id.background)).setColor(loadThemeColor(themeIds.get(position)));
                    } else {
                        ((GradientDrawable) holder.icon.getBackground()).setColor(loadThemeColor(themeIds.get(position)));
                    }

                    holder.title.setText(themeNames.get(position));

                    holder.selectionIcon.setColorFilter(SIAApp.SIA_APP.school.getAccentColor() ,PorterDuff.Mode.SRC_ATOP);

                    if(SIAApp.SIA_APP.getCurrentThemeName().equals(themeIds.get(position))) {
                        holder.selectionIcon.setVisibility(View.VISIBLE);
                    }
                    else{
                        holder.selectionIcon.setVisibility(View.GONE);
                    }
                    return v;
                }
            };

            theme_color.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {

                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                    builder.setTitle(getResources().getString(R.string.personalisation_pickColor));

                    builder.setAdapter(adapter_theme_color, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            SIAApp.SIA_APP.setCustomThemeName(themeIds.get(which));
                            SIAApp.SIA_APP.school.loadTheme();
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

            Preference pref_username = findPreference("authentication_username");

            if(SIAApp.SIA_APP.school.loginNeeded) {
                pref_username.setSummary(getString(R.string.logged_in_as, SIAApp.SIA_APP.api.getUsername()));
            }

            pref_username.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    if(gg.api.isLoggedIn()) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                        builder.setTitle(getResources().getString(R.string.logout));
                        builder.setMessage(getResources().getString(R.string.logout_confirm));
                        builder.setPositiveButton(getResources().getString(R.string.yes), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if(Build.VERSION.SDK_INT >= 25 ) {
                                    ShortcutManager shortcutManager = getActivity().getSystemService(ShortcutManager.class);
                                    shortcutManager.removeAllDynamicShortcuts();
                                }
                                SIAApp.SIA_APP.api.logout();
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

            Preference pref_developers = findPreference("developers");

            pref_developers.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                    builder.setTitle(getResources().getString(R.string.developer));
                    builder.setMessage(getResources().getString(R.string.developer_dialog));
                    builder.setPositiveButton(getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
                    builder.create().show();
                    return false;
                }
            });

            Preference filter = findPreference("filter");
            filter.setSummary(SIAApp.SIA_APP.filters.getSummary(true));
            filter.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Intent i = new Intent(getActivity(), FilterActivity.class);
                    getActivity().startActivityForResult(i, 1);
                    return false;
                }
            });

            Preference contact = findPreference("contact");
            LayerDrawable ld = (LayerDrawable) ContextCompat.getDrawable(getActivity(), R.drawable.settings_circle_image);
            ld.setDrawableByLayerId(R.id.image, ContextCompat.getDrawable(getActivity(), R.drawable.ic_mail));
            ((GradientDrawable) ld.findDrawableByLayerId(R.id.background)).setColor(SIAApp.SIA_APP.school.getColor());
            contact.setIcon(ld);
            contact.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts(
                            "mailto", "support@cedgetec.com", null));
                    emailIntent.putExtra(Intent.EXTRA_SUBJECT, "SchulinfoApp " + SIAApp.SIA_APP.school.name);
                    startActivity(Intent.createChooser(emailIntent, "E-Mail senden"));
                    return false;
                }
            });

        }

        @Override
         public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            Preference pref = findPreference(key);

            changed = true;

            if(key.equals("theme_mode")) {
                String b = sharedPreferences.getString(key, "nightfollowsystem");
                switch (b){
                    case "nightauto":
                        SIAApp.SIA_APP.setThemeMode(AppCompatDelegate.MODE_NIGHT_AUTO);
                        break;
                    case "nightyes":
                        SIAApp.SIA_APP.setThemeMode(AppCompatDelegate.MODE_NIGHT_YES);
                        break;
                    case "nightno":
                        SIAApp.SIA_APP.setThemeMode(AppCompatDelegate.MODE_NIGHT_NO);
                        break;
                    default:
                        SIAApp.SIA_APP.setThemeMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                }
                recreate = true;
                getActivity().recreate();
            } else if(key.equals("notifications")) {
                CheckBoxPreference no = (CheckBoxPreference) pref;

                findPreference("background_updates").setEnabled(no.isChecked());
                findPreference("notification_led_color").setEnabled(no.isChecked());
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
        setTheme(SIAApp.SIA_APP.school.getTheme());

        changed = false;
        recreate = false;

        super.onCreate(savedInstanceState);

        ViewGroup contentView = (ViewGroup) LayoutInflater.from(this).inflate(R.layout.activity_settings, new LinearLayout(this), false);

        setContentView(contentView);

        if(savedInstanceState != null) {
            changed = savedInstanceState.getBoolean("s_changed");
            recreate = savedInstanceState.getBoolean("s_recreate");
        } else {
            frag = new CustomPreferenceFragment();
            getSupportFragmentManager().beginTransaction().replace(R.id.content_wrapper, frag, "settings_frag").commit();
        }

        mToolBar = (Toolbar) contentView.findViewById(R.id.toolbar);
        mToolBar.setTitle(getTitle());
        mToolBar.setNavigationIcon(R.drawable.ic_arrow_back);
        mToolBar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

    }

    public static CustomTabsIntent createCustomTab(Activity activity, String url){
        Drawable d = ContextCompat.getDrawable(activity, R.drawable.ic_arrow_back);
        Bitmap bitmap = Bitmap.createBitmap(d.getIntrinsicWidth(), d.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        d.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        d.draw(canvas);
        CustomTabsIntent customTabsIntent = new CustomTabsIntent.Builder()
                .setToolbarColor(SIAApp.SIA_APP.school.getColor())
                .setSecondaryToolbarColor(Color.RED)
                .setCloseButtonIcon(bitmap)
                .setShowTitle(true)
                .build();
        customTabsIntent.launchUrl(activity, Uri.parse(url));
        return customTabsIntent;
    }

    @Override
    public void onSaveInstanceState(Bundle b) {
        super.onSaveInstanceState(b);
        b.putBoolean("s_changed", changed);
        b.putBoolean("s_recreate", recreate);
    }

    @Override
    public void onActivityResult(int req, int resp, Intent intent) {
        if(req == 1 && resp == RESULT_OK) {
            changed = true;
            Preference filter = frag.findPreference("filter");
            filter.setSummary(SIAApp.SIA_APP.filters.getSummary(true));
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
        int theme = SIAApp.SIA_APP.getResources().getIdentifier("AppTheme" + name, "style", SIAApp.SIA_APP.getPackageName());
        TypedArray ta = SIAApp.SIA_APP.obtainStyledAttributes(theme, new int [] {R.attr.colorPrimary});
        int c = ta.getColor(0, Color.RED);
        ta.recycle();
        return c;
    }

}
