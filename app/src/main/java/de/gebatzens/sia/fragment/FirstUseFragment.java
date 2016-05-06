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

package de.gebatzens.sia.fragment;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import de.gebatzens.sia.FirstUseActivity;
import de.gebatzens.sia.R;

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
        LinearLayout layout = (LinearLayout) inflater.inflate(R.layout.fu_card, container, false);
        ImageView i = (ImageView) layout.findViewById(R.id.fu_image);
        TextView tvhead = (TextView) layout.findViewById(R.id.fu_header);
        TextView tvsub = (TextView) layout.findViewById(R.id.fu_text);

        switch(mPage) {
            case 0:
                color = Color.parseColor("#1976D2");
                layout.setBackgroundColor(color);
                i.setImageResource(R.drawable.fu_device);
                tvhead.setText(R.string.fu_firstpage_title);
                tvsub.setText(R.string.fu_firstpage_content);
                break;
            case 1:
                color = Color.parseColor("#F4511E");
                layout.setBackgroundColor(color);
                i.setImageResource(R.drawable.fu_overview);
                tvhead.setText(R.string.fu_secondpage_title);
                tvsub.setText(R.string.fu_secondpage_content);
                break;
            case 2:
                color = Color.parseColor("#43A047");
                layout.setBackgroundColor(color);
                i.setImageResource(R.drawable.fu_filter);
                tvhead.setText(R.string.fu_thirdpage_title);
                tvsub.setText(R.string.fu_thirdpage_content);
                break;
            case 3:
                color = Color.parseColor("#d32f2f");
                layout.setBackgroundColor(color);
                i.setImageResource(R.drawable.fu_exam);
                tvhead.setText(R.string.fu_fifthpage_title);
                tvsub.setText(R.string.fu_fifthpage_content);
                break;
            case 4:
                color = Color.parseColor("#00ACC1");
                layout.setBackgroundColor(color);
                i.setImageResource(R.drawable.fu_more);
                tvhead.setText(R.string.fu_fourthpage_title);
                tvsub.setText(R.string.fu_fourthpage_content);
                break;
            case 5:
                color = Color.parseColor("#8BC34A");
                layout.setBackgroundColor(color);
                i.setImageResource(R.drawable.fu_finish);
                tvhead.setText(R.string.fu_sixth_title);
                tvsub.setText(R.string.fu_sixth_content);
                break;
        }

        return layout;
    }

}
