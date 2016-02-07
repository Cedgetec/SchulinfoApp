/*
 * Copyright 2015 - 2016 Hauke Oldsen
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

import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import java.util.ArrayList;

import de.gebatzens.sia.GGApp;
import de.gebatzens.sia.MainActivity;
import de.gebatzens.sia.R;
import de.gebatzens.sia.data.Exams;
import de.gebatzens.sia.dialog.TextDialog;

public class ExamFragment extends RemoteDataFragment {


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup vg, Bundle b) {
        ViewGroup v = (ViewGroup) inflater.inflate(R.layout.fragment_exam, vg, false);
        ((MainActivity) getActivity()).updateMenu(R.menu.toolbar_exam_menu);
        if(getFragment().getData() != null)
            createRootView(inflater, v);
        return v;
    }

    @Override
    public void createView(final LayoutInflater inflater, ViewGroup view) {
        LinearLayout lroot = (LinearLayout) view.findViewById(R.id.exam_content);

        if(GGApp.GG_APP.preferences.getBoolean("first_use_exam_filter", true)) {
            TextDialog.newInstance(R.string.explanation, R.string.exam_explain).show(getActivity().getSupportFragmentManager(), "exam_help");
        }
        GGApp.GG_APP.preferences.edit().putBoolean("first_use_exam_filter", false).apply();

        RecyclerView recyclerView = (RecyclerView) inflater.inflate(R.layout.basic_recyclerview, null);
        recyclerView.setPadding(0, 0, 0, 0);
        ExamAdapter sla = new ExamAdapter(this);
        recyclerView.setAdapter(sla);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        recyclerView.setTag("gg_list");
        //recyclerView.setPadding(toPixels(4), toPixels(4), toPixels(4), toPixels(4));
        lroot.addView(recyclerView);

        sla.update(getString(R.string.your_overview), new ArrayList<Exams.ExamItem>(), true);

    }

    @Override
    public ViewGroup getContentView() {
        return (ViewGroup) getView().findViewById(R.id.exam_content);
    }


}
