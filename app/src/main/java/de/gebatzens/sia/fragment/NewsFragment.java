/*
 * Copyright 2015 Fabian Schultis, Hauke Oldsen
 * Copyright 2016 Hauke Oldsen
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
package de.gebatzens.sia.fragment;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;

import de.gebatzens.sia.MainActivity;
import de.gebatzens.sia.NewsFragmentDatabaseHelper;
import de.gebatzens.sia.R;
import de.gebatzens.sia.data.News;

public class NewsFragment extends RemoteDataFragment {

    public ListView lv;
    private NewsFragmentListAdapter nfla;
    private NewsFragmentDatabaseHelper mDatabaseHelper;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup group, Bundle bundle) {
        ((MainActivity) getActivity()).updateMenu(R.menu.toolbar_menu);
        ViewGroup vg = (ViewGroup) inflater.inflate(R.layout.fragment_news, group, false);
        if(getFragment().getData() != null)
            createRootView(inflater, vg);
        mDatabaseHelper = new NewsFragmentDatabaseHelper(getActivity().getApplicationContext());
        return vg;
    }

    @Override
    public void createView(final LayoutInflater inflater, ViewGroup view) {
        LinearLayout lroot = (LinearLayout) view.findViewById(R.id.news_content);
        if(((News) getFragment().getData()).isEmpty()) {
            ScrollView sv = new ScrollView(getActivity());
            sv.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            sv.setTag("gg_scroll");
            LinearLayout l = new LinearLayout(getActivity());
            createRootLayout(l);
            lroot.addView(sv);
            sv.addView(l);
            createNoEntriesCard(l, inflater);
        } else {
            lv = new ListView(getActivity());
            lv.setDrawSelectorOnTop(true);
            lroot.addView(lv);
            lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    News news = ((News) getFragment().getData());
                    String title = news.get(position).title;
                    String content = news.get(position).text;

                    AlertDialog.Builder ad = new AlertDialog.Builder(getActivity());
                    ad.setView(inflater.inflate(R.layout.news_dialog, null));
                    ad.setTitle(title);
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
                    tv.setText(Html.fromHtml(content, null, null));

                    if (!mDatabaseHelper.checkNewsRead(title)) {
                        mDatabaseHelper.addReadNews(title);
                        nfla.notifyDataSetChanged();
                    }
                }
            });
            nfla = new NewsFragmentListAdapter(getActivity(), ((News) getFragment().getData()));
            lv.setAdapter(nfla);
        }
    }

    @Override
    public ViewGroup getContentView() {
        return (ViewGroup) getView().findViewById(R.id.news_content);
    }


}
