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

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import de.gebatzens.ggvertretungsplan.FirstUseActivity;
import de.gebatzens.ggvertretungsplan.GGApp;
import de.gebatzens.ggvertretungsplan.R;
import de.gebatzens.ggvertretungsplan.SetupActivity;

public class FirstUseFragment extends Fragment {

    private int mPage;
    public int color;

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
        ((FirstUseActivity) getActivity()).adapter.fragments.put(mPage, this);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        RelativeLayout layout = (RelativeLayout) inflater.inflate(R.layout.fu_card, container, false);
        ImageView i = (ImageView) layout.findViewById(R.id.fu_image);
        TextView tvhead = (TextView) layout.findViewById(R.id.fu_header);
        TextView tvsub = (TextView) layout.findViewById(R.id.fu_text);

        switch(mPage) {
            case 0:
                color = Color.parseColor("#1976D2");
                layout.setBackgroundColor(color);
                i.setImageResource(R.drawable.fu_device);
                tvhead.setText("Deine SchulinfoAPP");
                tvsub.setText("Ab sofort immer informiert über den Vertretungsplan und mehr...");
                break;
            case 1:
                color = Color.parseColor("#F4511E");
                layout.setBackgroundColor(color);
                i.setImageResource(R.drawable.fu_overview);
                tvhead.setText("Planänderungen auf dich angepasst");
                tvsub.setText("Personalisiere den Vertretungsplan und passe ihn auf deine Klasse an.");
                break;
            case 2:
                color = Color.parseColor("#43A047");
                layout.setBackgroundColor(color);
                i.setImageResource(R.drawable.fu_filter);
                tvhead.setText("Der Kursfilter");
                tvsub.setText("Trage im Filtermenü Kurse ein, die dich nicht betreffen, um sie von deiner persönlichen Übersicht auszuschließen.");
                break;
            case 3:
                color = Color.parseColor("#00ACC1");
                layout.setBackgroundColor(color);
                i.setImageResource(R.drawable.fu_more);
                tvhead.setText("Noch nicht genug?");
                tvsub.setText("Weitere Funktionen: Falls von der Schule unterstützt, kannst du News-, Mensa-, und Klausurenplan in der App einsehen.");
                break;
            case 4:
                layout = (RelativeLayout) inflater.inflate(R.layout.fu_card_finish, container, false);
                Button bu = (Button) layout.findViewById(R.id.fu_button);
                bu.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        GGApp.GG_APP.preferences.edit().putBoolean("first_use", true).apply();
                        startActivity(new Intent(getActivity(), SetupActivity.class));
                        getActivity().finish();
                    }
                });
                color = Color.parseColor("#F57C00");
                layout.setBackgroundColor(color);
                i.setImageResource(R.drawable.fu_more);
                tvhead.setText("Noch nicht genug?");
                tvsub.setText("Weitere Funktionen: Falls von der Schule unterstützt, kannst du News-, Mensa-, und Klausurenplan in der App einsehen.");
                break;
        }

        return layout;
    }

}
