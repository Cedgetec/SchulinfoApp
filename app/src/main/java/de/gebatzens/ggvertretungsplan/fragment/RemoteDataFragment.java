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

package de.gebatzens.ggvertretungsplan.fragment;

import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.CardView;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import de.gebatzens.ggvertretungsplan.GGApp;
import de.gebatzens.ggvertretungsplan.R;
import de.gebatzens.ggvertretungsplan.VPLoginException;

public abstract class RemoteDataFragment extends Fragment {

    GGApp.FragmentType type;

    public abstract void createView(LayoutInflater inflater, ViewGroup vg);
    public abstract ViewGroup getContentView();

    public void setFragmentLoading() {
        if(getView() == null)
            return;

        ViewGroup vg = getContentView();
        vg.removeAllViews();

        vg.addView(createLoadingView());
    }

    public void updateFragment() {
        if(getView() == null)
            return;

        ViewGroup vg = getContentView();

        vg.removeAllViews();

        createRootView(getActivity().getLayoutInflater(), vg);
    }

    public CardView createCardView() {
        CardView c2 = new CardView(getActivity());
        CardView.LayoutParams c2params = new CardView.LayoutParams(
                CardView.LayoutParams.MATCH_PARENT,
                CardView.LayoutParams.WRAP_CONTENT
        );
        c2.setLayoutParams(c2params);
        c2.setUseCompatPadding(true);
        c2.setContentPadding(toPixels(16), toPixels(16), toPixels(16), toPixels(16));
        return c2;
    }

    public int toPixels(float dp) {
        float scale = GGApp.GG_APP.getResources().getDisplayMetrics().density;
        return (int) (dp * scale);
    }

    public TextView createTextView(String text, int size, LayoutInflater inflater, ViewGroup group) {
        // TextView t = (TextView) inflater.inflate(R.layout.plan_text, group, true).findViewById(R.id.plan_entry);
        TextView t = new TextView(getActivity());
        t.setText(text);
        t.setPadding(0, 0, toPixels(20), 0);
        t.setTextSize(size);
        if (themeIsLight()) {
            t.setTextColor(Color.parseColor("#212121"));
        } else{
            t.setTextColor(Color.parseColor("#e7e7e7"));
        }
        group.addView(t);
        return t;
    }

    public View createLoadingView() {
        LinearLayout l = new LinearLayout(getActivity());
        l.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        l.setGravity(Gravity.CENTER);

        ProgressBar pb = new ProgressBar(getActivity());
        pb.getIndeterminateDrawable().setColorFilter(GGApp.GG_APP.school.getAccentColor(), PorterDuff.Mode.SRC_IN);
        pb.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        pb.setVisibility(ProgressBar.VISIBLE);

        l.addView(pb);
        return l;
    }

    public void createButtonWithText(ViewGroup l, String text, String button, View.OnClickListener onclick) {
        RelativeLayout r = new RelativeLayout(getActivity());
        r.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        TextView tv = new TextView(getActivity());
        RelativeLayout.LayoutParams tvparams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,ViewGroup.LayoutParams.WRAP_CONTENT);
        tvparams.addRule(RelativeLayout.ABOVE, R.id.reload_button);
        tvparams.addRule(RelativeLayout.CENTER_HORIZONTAL);
        tv.setLayoutParams(tvparams);
        tv.setText(text);
        tv.setTextSize(23);
        tv.setPadding( 0, 0, 0, toPixels(15));
        tv.setGravity(Gravity.CENTER_HORIZONTAL);
        r.addView(tv);

        Button b = new Button(getActivity());
        RelativeLayout.LayoutParams bparams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,ViewGroup.LayoutParams.WRAP_CONTENT);
        bparams.addRule(RelativeLayout.CENTER_VERTICAL);
        bparams.addRule(RelativeLayout.CENTER_HORIZONTAL);
        b.setLayoutParams(bparams);
        b.setId(R.id.reload_button);
        b.setText(button);
        b.setTextSize(23);
        b.setAllCaps(false);
        b.setTypeface(null, Typeface.NORMAL);
        b.setOnClickListener(onclick);
        r.addView(b);

        l.addView(r);
    }

    public void createText(ViewGroup l, String text) {
        RelativeLayout r = new RelativeLayout(getActivity());
        r.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        r.setGravity(Gravity.CENTER);

        TextView tv = new TextView(getActivity());
        RelativeLayout.LayoutParams tvparams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,ViewGroup.LayoutParams.WRAP_CONTENT);
        tv.setLayoutParams(tvparams);
        tv.setText(text);
        tv.setTextSize(30);
        tv.setTextColor(getResources().getColor(R.color.primary_text_default_material_dark));
        tv.setPadding( 0, 0, 0, toPixels(15));
        r.addView(tv);

        l.addView(r);
    }

    public void createNoEntriesCard(ViewGroup vg, LayoutInflater inflater) {
        FrameLayout f2 = new FrameLayout(getActivity());
        f2.setPadding(toPixels(1.3f),toPixels(0.3f),toPixels(1.3f),toPixels(0.3f));
        CardView cv = createCardView();
        if (themeIsLight()) {
            cv.setCardBackgroundColor(Color.parseColor("#fafafa"));
        } else{
            cv.setCardBackgroundColor(Color.parseColor("#424242"));
        }
        f2.addView(cv);
        createTextView(getResources().getString(R.string.no_entries), 20, inflater, cv);
        vg.addView(f2);
    }

    /**
     *
     * @return horizontal screen orientation
     */
    protected boolean createRootLayout(LinearLayout l) {
        l.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        l.setOrientation(LinearLayout.VERTICAL);
        if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            l.setPadding(toPixels(55),toPixels(4),toPixels(55),toPixels(4));
            return true;
        } else if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            l.setPadding(toPixels(4),toPixels(4),toPixels(4),toPixels(4));
        }
        return false;
    }

    public void createRootView(final LayoutInflater inflater, ViewGroup vg) {
        RemoteData data = GGApp.GG_APP.getDataForFragment(type);
        if(data == null) {
            setFragmentLoading();
        } else if(data.getThrowable() != null) {
            Throwable t = data.getThrowable();
            if(t instanceof VPLoginException) {
                createButtonWithText(vg, getResources().getString(R.string.login_required), getResources().getString(R.string.do_login), new View.OnClickListener() {
                    @Override
                    public void onClick(View c) {
                    //TODO: Login dialog (token expired)

                    }
                });
            } else {
                createButtonWithText(vg, getResources().getString(R.string.check_connection), getResources().getString(R.string.again), new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        GGApp.GG_APP.refreshAsync(null, true, type);
                    }
                });
            }
        } else {
            createView(inflater, vg);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup group, Bundle bundle) {
        LinearLayout l = new LinearLayout(getActivity());
        l.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        l.setOrientation(LinearLayout.VERTICAL);
        if(GGApp.GG_APP.getDataForFragment(type) != null)
            createRootView(inflater, l);
        return l;
    }

    @Override
    public void onViewCreated(View v, Bundle b) {
        super.onViewCreated(v, b);

        if(GGApp.GG_APP.getDataForFragment(type) == null) {
            getContentView().addView(createLoadingView());
        }

        FrameLayout contentFrame = (FrameLayout) getActivity().findViewById(R.id.content_fragment);
        contentFrame.setVisibility(View.VISIBLE);
        LinearLayout fragmentLayout = (LinearLayout) getActivity().findViewById(R.id.fragment_layout);
        Animation fadeIn = AnimationUtils.loadAnimation(getActivity().getApplicationContext(), R.anim.fade_in);
        fragmentLayout.startAnimation(fadeIn);

    }

    public void saveInstanceState(Bundle b) {

    }

    public static interface RemoteData {
        public Throwable getThrowable();
        public void save();
        public boolean load();

    }

    public boolean themeIsLight() {
        TypedValue a = new TypedValue();
        getActivity().getTheme().resolveAttribute(android.R.attr.windowBackground, a, true);
        int color = a.data;
        if (color == getResources().getColor(R.color.background_material_light)) {
            return true;
        } else{
            return false;
        }
    }

}
