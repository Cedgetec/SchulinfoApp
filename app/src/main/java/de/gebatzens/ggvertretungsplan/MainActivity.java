/*
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

import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

import java.util.List;


public class MainActivity extends FragmentActivity {

    Toolbar mToolbar;
    int selected = 0;
    public GGContentFragment mContent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        GGApp.GG_APP.mActivity = this;

        selected = GGApp.GG_APP.getSelectedProvider();

        setContentView(getLayoutInflater().inflate(R.layout.activity_main, null));

        List<Fragment> frags = getSupportFragmentManager().getFragments();
        if(frags != null)
            for(Fragment frag : frags) {
                if(frag != null && !frag.getTag().equals("gg_content_fragment"))
                    getSupportFragmentManager().beginTransaction().remove(frag).commit();
            }

        if (savedInstanceState == null) {
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            mContent = new GGContentFragment();
            transaction.replace(R.id.content_fragment, mContent, "gg_content_fragment");
            transaction.commit();
        } else
            mContent = (GGContentFragment) getSupportFragmentManager().findFragmentByTag("gg_content_fragment");

        mToolbar = (Toolbar) findViewById(R.id.toolbar);

        mToolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {

                if(menuItem.getItemId() == R.id.action_refresh) {
                    mContent.mGGFrag.setFragmentsLoading();
                    GGApp.GG_APP.refreshAsync(null, true);
                } else if(menuItem.getItemId() == R.id.action_settings) {
                    Intent i = new Intent(MainActivity.this, SettingsActivity.class);
                    startActivityForResult(i, 1);
                }

                return false;
            }
        });

        mToolbar.setTitle(GGApp.mStrings[selected]);
        mToolbar.inflateMenu(R.menu.toolbar_menu);
        mToolbar.setTitleTextColor(Color.WHITE);
        //toolbar.setNavigationIcon(R.drawable.ic_menu_white);

        //wait for vps
        new AsyncTask<Object, Void, Void>() {

            @Override
            protected Void doInBackground(Object... params) {
                while(GGApp.GG_APP.mVPToday == null || GGApp.GG_APP.mVPTomorrow == null);
                MainActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mContent.mGGFrag.updateFragments();
                    }
                });
                return null;
            }
        }.execute();

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        GGApp.GG_APP.mActivity = null;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == 1 && resultCode == RESULT_OK) { //Settings changed
            mContent.mGGFrag.setFragmentsLoading();
            selected = GGApp.GG_APP.getSelectedProvider();
            GGApp.GG_APP.createProvider(selected);
            mToolbar.setTitle(GGApp.mStrings[selected]);
            GGApp.GG_APP.refreshAsync(null, true);
        }

    }

    @Override
    public void onBackPressed() {
        this.finish();
    }

}
