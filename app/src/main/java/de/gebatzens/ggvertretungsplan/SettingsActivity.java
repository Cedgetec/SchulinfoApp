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
import android.app.Fragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceFragment;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.net.URL;
import java.util.Scanner;

import javax.net.ssl.HttpsURLConnection;

import de.gebatzens.ggvertretungsplan.provider.GGProvider;

public class SettingsActivity extends Activity {

    Toolbar mToolBar;
    private static boolean changed;
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
            String pref_schule_content = gg.provider.getFullName();

            Preference pref_schule = findPreference("schule");
            pref_schule.setSummary(pref_schule_content);

            Preference update = findPreference("appupdates");
            update.setSummary(gg.translateUpdateType(gg.getUpdateType()));

            Preference pref_buildversion = findPreference("buildversion");
            String versionName = BuildConfig.VERSION_NAME;
            pref_buildversion.setSummary("Version: " + versionName + " (" + BuildConfig.BUILD_TYPE + ") (" + getResources().getString(R.string.touch_to_update) + ")");
            pref_buildversion.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    if(BuildConfig.DEBUG) {
                        Toast.makeText(GGApp.GG_APP, getResources().getString(R.string.not_available_in_debug_mode), Toast.LENGTH_SHORT).show();
                        return false;
                    }
                    new AsyncTask<Object, Void, Void>() {

                        @Override
                        protected Void doInBackground(Object... params) {
                            try {

                                final String version = getVersion();

                                if (!version.toString().equals(BuildConfig.VERSION_NAME)) {
                                    ((SettingsActivity) getActivity()).showUpdateDialog(version);
                                } else {
                                    getActivity().runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            Toast.makeText(getActivity().getApplication(), getResources().getString(R.string.no_new_version_available), Toast.LENGTH_SHORT).show();
                                        }
                                    });
                                }

                            } catch (Exception e) {
                                e.printStackTrace();
                                getActivity().runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(getActivity().getApplication(), getResources().getString(R.string.no_internet_connection), Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }
                            return null;
                        }
                    }.execute();
                    return false;
                }
            });

            Preference pref_githublink = findPreference("githublink");
            pref_githublink.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                public boolean onPreferenceClick(Preference preference) {
                    Intent linkIntent = new Intent(Intent.ACTION_VIEW);
                    linkIntent.setData(Uri.parse("https://github.com/Gebatzens/GG-Vertretung"));
                    startActivity(linkIntent);
                    return true;
                }
            });

            final Preference pref_username = findPreference("authentication_username");

            pref_username.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    if(gg.provider.getUsername() != null) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                        builder.setTitle(getResources().getString(R.string.logout));
                        LinearLayout ll = new LinearLayout(getActivity());
                        ll.setOrientation(LinearLayout.VERTICAL);
                        int p = 25;
                        float d = getActivity().getResources().getDisplayMetrics().density;
                        int padding_left = (int)(p * d);
                        ll.setPadding(padding_left,0,0,0);
                        TextView tv = new TextView(getActivity());
                        tv.setText(getResources().getString(R.string.logout_realy));
                        ll.addView(tv);
                        final CheckBox cb = new CheckBox(getActivity());
                        cb.setText(getResources().getString(R.string.logout_on_all_devices));
                        ll.addView(cb);
                        builder.setView(ll);
                        builder.setPositiveButton(getResources().getString(R.string.yes), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                GGApp.GG_APP.provider.logout(false, (Boolean) cb.isChecked());
                                changed = true;
                                pref_username.setSummary(getResources().getString(R.string.youre_not_logged_in));
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
                        //Überall in der App steht "Du", also sollte das mal einheitlich werden
                        Toast.makeText(getActivity(),getResources().getString(R.string.youre_not_logged_in), Toast.LENGTH_SHORT).show();
                    }

                    return false;
                }
            });

            String username = gg.provider.getUsername();
            if(username != null) {
                    pref_username.setSummary(username + " (" + getResources().getString(R.string.touch_to_logout) + ")");
            }

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
            helpdesk.setSummary(getResources().getString(R.string.contact_us_if_you_have_a_problem_with_this_app));
            helpdesk.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Intent i = new Intent(getActivity(), HelpdeskActivity.class);
                    getActivity().startActivityForResult(i, 1);
                    return false;
                }
            });

        }

        @Override
         public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            Preference pref = findPreference(key);

            changed = true;


            if (key.equals("schule")) {
                ListPreference listPref = (ListPreference) pref;
                pref.setSummary(listPref.getEntry());
                String username = GGApp.GG_APP.provider.getUsername();
                if(username == null)
                    findPreference("authentication_username").setSummary(getResources().getString(R.string.youre_not_logged_in));
                else
                    findPreference("authentication_username").setSummary(username + " (" + getResources().getString(R.string.touch_to_logout) + ")");
            } else if(key.equals("appupdates")) {
                ListPreference listPreference = (ListPreference) pref;
                listPreference.setSummary(listPreference.getEntry());
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
        setTheme(GGApp.GG_APP.provider.getTheme());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            GGApp.GG_APP.setStatusBarColor(getWindow());
        }

        changed = false;
        Fragment f = getFragmentManager().findFragmentByTag("gg_settings_frag");
        if(f != null) {
            getFragmentManager().beginTransaction().remove(f).commit();
        }

        super.onCreate(savedInstanceState);

        ViewGroup contentView = (ViewGroup) LayoutInflater.from(this).inflate(
                R.layout.settings_activity, new LinearLayout(this), false);

        if(savedInstanceState != null) {
            changed = savedInstanceState.getBoolean("ggs_changed");

        }

        mToolBar = (Toolbar) contentView.findViewById(R.id.toolbar);
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

        frag = new GGPFragment();
        getFragmentManager().beginTransaction().replace(R.id.content_wrapper, frag, "gg_settings_frag").commit();

        setContentView(contentView);

        if(getIntent().getBooleanExtra("update", false)) {
            final String version = getIntent().getStringExtra("version");

            new AsyncTask<Object, Object, Object>() {
                @Override
                public Object doInBackground(Object... objects) {
                    try {
                        showUpdateDialog(version);
                    } catch(Exception e) {
                        e.printStackTrace();
                        SettingsActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(getApplication(), getResources().getString(R.string.no_internet_connection), Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                    return null;
                }
            }.execute();
        }
    }


    @Override
    public void onSaveInstanceState(Bundle b) {
        super.onSaveInstanceState(b);
        b.putBoolean("ggs_changed", changed);
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

    @Override
    public void finish() {
        Intent i = new Intent();
        setResult(changed ? RESULT_OK : RESULT_CANCELED, i);
        super.finish();
    }

    public static String getVersion() throws Exception {
        HttpsURLConnection con = (HttpsURLConnection) new URL("https://gymnasium-glinde.logoip.de/infoapp/update.php?version").openConnection();
        con.setRequestMethod("POST");
        con.setSSLSocketFactory(GGProvider.sslSocketFactory);

        if (con.getResponseCode() == 200) {
            BufferedInputStream in = new BufferedInputStream(con.getInputStream());
            Scanner scan = new Scanner(in);
            final StringBuilder resp = new StringBuilder("");
            while (scan.hasNextLine())
                resp.append(scan.nextLine());
            scan.close();
            return resp.toString();
        }
        return null;

    }

    public void showUpdateDialog(final String version) throws Exception {
        HttpsURLConnection con_changelog = (HttpsURLConnection) new URL("https://gymnasium-glinde.logoip.de/infoapp/update.php?changelog="+version).openConnection();
        con_changelog.setRequestMethod("GET");
        con_changelog.setSSLSocketFactory(GGProvider.sslSocketFactory);

        if(con_changelog.getResponseCode() == 200) {
            BufferedInputStream in_changelog = new BufferedInputStream(con_changelog.getInputStream());
            Scanner scan_changelog = new Scanner(in_changelog);
            String resp_changelog = "";
            while (scan_changelog.hasNextLine())
                resp_changelog += scan_changelog.nextLine();
            scan_changelog.close();
            final String final_resp_changelog = resp_changelog;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    AlertDialog.Builder builder = new AlertDialog.Builder(SettingsActivity.this);
                    builder.setTitle(getResources().getString(R.string.update_available));
                    builder.setMessage(Html.fromHtml(getResources().getString(R.string.should_the_app_be_updated) + "<br><br>Changelog:<br>" +
                            final_resp_changelog.replace("|", "<br>").replace("*", "&#8226;")));
                    builder.setPositiveButton(getResources().getString(R.string.yes), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            UpdateActivity ua = new UpdateActivity(SettingsActivity.this, SettingsActivity.this);
                            ua.execute(version.toString());
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
                }
            });
        }
    }

}