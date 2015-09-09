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

package de.gebatzens.ggvertretungsplan.fragment;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import de.gebatzens.ggvertretungsplan.GGApp;
import de.gebatzens.ggvertretungsplan.R;

public class FirstUseFragment extends Fragment {

    private int mPage;

    public static FirstUseFragment newInstance(int page) {
        Bundle args = new Bundle();
        args.putInt("page", page);
        FirstUseFragment fragment = new FirstUseFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPage = getArguments().getInt("page");
    }

    public static int toPixels(float dp) {
        float scale = GGApp.GG_APP.getResources().getDisplayMetrics().density;
        return (int) (dp * scale);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        RelativeLayout r = new RelativeLayout(getActivity());
        ImageView i = new ImageView(getActivity());
        i.setAdjustViewBounds(true);
        i.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        i.setMaxHeight(toPixels(350));
        i.setPadding(toPixels(20), toPixels(20), toPixels(20), toPixels(20));
        RelativeLayout.LayoutParams iparams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
        iparams.addRule(RelativeLayout.ABOVE, R.id.first_use_text);
        i.setLayoutParams(iparams);
        LinearLayout l = new LinearLayout(getActivity());
        l.setId(R.id.first_use_text);
        l.setOrientation(LinearLayout.VERTICAL);
        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        lp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        l.setLayoutParams(lp);
        l.setPadding(toPixels(48),toPixels(0),toPixels(48),toPixels(96));
        TextView tvhead = new TextView(getActivity());
        TextView tvsub = new TextView(getActivity());
        tvhead.setTextColor(Color.WHITE);
        tvhead.setTextSize(24);
        tvhead.setPadding(0,0,0,toPixels(12));
        tvsub.setTextColor(Color.WHITE);
        tvsub.setTextSize(16);

        switch(mPage) {
            case 1:
                r.setBackgroundColor(Color.parseColor("#1976D2"));
                i.setImageResource(R.drawable.fu_device);
                tvhead.setText("Deine SchulinfoAPP");
                tvsub.setText("Ab sofort immer informiert über den Vertretungsplan und mehr...");
                break;
            case 2:
                r.setBackgroundColor(Color.parseColor("#F4511E"));
                i.setImageResource(R.drawable.fu_overview);
                tvhead.setText("Planänderungen auf dich angepasst");
                tvsub.setText("Personalisiere den Vertretungsplan und passe ihn auf deine Klasse an.");
                break;
            case 3:
                r.setBackgroundColor(Color.parseColor("#43A047"));
                i.setImageResource(R.drawable.fu_filter);
                tvhead.setText("Der Kursfilter");
                tvsub.setText("Trage im Filtermenü Kurse ein, die dich nicht betreffen, um sie von deiner persönlichen Übersicht auszuschließen.");
                break;
            case 4:
                r.setBackgroundColor(Color.parseColor("#00ACC1"));
                i.setImageResource(R.drawable.fu_more);
                tvhead.setText("Noch nicht genug?");
                tvsub.setText("Weitere Funktionen: Falls von der Schule unterstützt, kannst du News-, Mensa-, und Klausurenplan in der App einsehen.");
                break;
        }

        l.addView(tvhead);
        l.addView(tvsub);
        r.addView(i);
        r.addView(l);

        return r;
    }

}
