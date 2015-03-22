/*
 * Copyright 2015 Fabian Schultis, Hauke Oldsen
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
package de.gebatzens.ggvertretungsplan.fragment;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import de.gebatzens.ggvertretungsplan.GGApp;
import de.gebatzens.ggvertretungsplan.NewsFragmentDatabaseHelper;
import de.gebatzens.ggvertretungsplan.R;

public class NewsFragment extends RemoteDataFragment {

    public ListView lv;
    private NewsFragmentListAdapter nfla;
    private NewsFragmentDatabaseHelper mDatabaseHelper;

    public NewsFragment() {
        type = GGApp.FragmentType.NEWS;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup group, Bundle bundle) {
        ViewGroup vg = (ViewGroup) inflater.inflate(R.layout.fragment_news, group, false);
        if(GGApp.GG_APP.news != null)
            createRootView(inflater, vg);
        mDatabaseHelper = new NewsFragmentDatabaseHelper(getActivity().getApplicationContext());
        return vg;
    }

    @Override
    public void onViewCreated(View v, Bundle b) {
        super.onViewCreated(v, b);

        final SwipeRefreshLayout swipeContainer = (SwipeRefreshLayout) v.findViewById(R.id.refresh);
        // Setup refresh listener which triggers new data loading
        swipeContainer.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                GGApp.GG_APP.refreshAsync(new Runnable() {
                    @Override
                    public void run() {
                        swipeContainer.post(new Runnable() {
                            @Override
                            public void run() {
                                swipeContainer.setRefreshing(false);
                            }
                        });

                    }
                }, true, GGApp.FragmentType.NEWS);
            }
        });
        // Configure the refreshing colors
        swipeContainer.setColorSchemeResources(R.color.custom_material_green,
                R.color.custom_material_red,
                R.color.custom_material_blue,
                R.color.custom_material_orange);

    }

    @Override
    public void createView(final LayoutInflater inflater, ViewGroup view) {
        lv = new ListView(getActivity());
        int p = toPixels(10);
        //lv.getDivider().setColorFilter(GGApp.GG_APP.provider.getColor(), PorterDuff.Mode.ADD);
        lv.setDrawSelectorOnTop(true);
        lv.setDivider(getResources().getDrawable(R.drawable.listview_divider));
        ((LinearLayout) view.findViewById(R.id.news_content)).addView(lv);
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                TextView txtTitle = (TextView) view.findViewById(R.id.newsTitle);
                TextView txtContent =  (TextView) view.findViewById(R.id.newsContent);
                String mTitle = txtTitle.getText().toString();
                String mContent = txtContent.getText().toString();
                AlertDialog.Builder ad = new AlertDialog.Builder(getActivity());
                ad.setView(inflater.inflate(R.layout.news_dialog, null));
                ad.setTitle(mTitle);
                ad.setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
                Dialog d = ad.create();
                d.show();
                TextView tv = (TextView) d.findViewById(R.id.newsd_text);
                tv.setMovementMethod(LinkMovementMethod.getInstance());
                tv.setText(Html.fromHtml(mContent));

                Log.w("ggvp", mContent);
                if(!mDatabaseHelper.checkNewsRead(mTitle)) {
                    mDatabaseHelper.addReadNews(mTitle);
                    nfla.notifyDataSetChanged();
                }
            }
        });
        nfla = new NewsFragmentListAdapter(getActivity(), GGApp.GG_APP.news);
        lv.setAdapter(nfla);
    }

    @Override
    public ViewGroup getContentView() {
        return (ViewGroup) getView().findViewById(R.id.news_content);
    }


}
