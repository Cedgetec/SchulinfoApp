/*
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

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.joanzapata.pdfview.PDFView;

import java.io.File;

import de.gebatzens.sia.MainActivity;
import de.gebatzens.sia.R;
import de.gebatzens.sia.data.StaticData;

public class PDFFragment extends RemoteDataFragment {

    PDFView pdf;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup group, Bundle bundle) {
        ((MainActivity) getActivity()).updateMenu(R.menu.toolbar_pdf_menu);
        ViewGroup vg = (ViewGroup) inflater.inflate(R.layout.fragment_pdf, group, false);
        if(getFragment().getData() != null)
            createRootView(inflater, (ViewGroup) vg.findViewById(R.id.pdf_content));
        return vg;
    }

    @Override
    public void createView(LayoutInflater inflater, ViewGroup vg) {
        LinearLayout ll = (LinearLayout) vg.findViewById(R.id.pdf_content);
        ll.removeAllViews();
        pdf = new PDFView(getContext(), null);
        pdf.setId(R.id.pdfview);
        pdf.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        File file = ((StaticData) getFragment().getData()).getFile();
        if(file.exists())
            pdf.fromFile(file).enableSwipe(true).load();
        else
            Log.w("ggvp", file + " does not exist");
        ll.addView(pdf);

    }

    @Override
    public ViewGroup getContentView() {
        return (ViewGroup) getView().findViewById(R.id.pdf_content);
    }


}
