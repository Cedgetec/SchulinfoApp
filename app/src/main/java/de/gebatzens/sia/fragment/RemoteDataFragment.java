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

package de.gebatzens.sia.fragment;

import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.CardView;
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
import android.widget.ScrollView;
import android.widget.TextView;

import de.gebatzens.sia.GGApp;
import de.gebatzens.sia.R;
import de.gebatzens.sia.VPLoginException;

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

    /**
     * Recreates the content
     */
    public void updateFragment() {
        if(getView() == null)
            return;

        ViewGroup vg = getContentView();

        vg.removeAllViews();

        createRootView(getActivity().getLayoutInflater(), vg);
    }

    /**
     * Creates an empty card view for app-wide use
     * @return
     */
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

    public static int toPixels(float dp) {
        float scale = GGApp.GG_APP.getResources().getDisplayMetrics().density;
        return (int) (dp * scale);
    }

    public TextView createTextView(String text, int size, LayoutInflater inflater, ViewGroup group) {
        TextView t = new TextView(getActivity());
        t.setText(text);
        t.setPadding(0, 0, toPixels(20), 0);
        t.setTextSize(size);
        t.setTextColor(Color.parseColor(GGApp.GG_APP.isDarkThemeEnabled() ? "#e7e7e7" : "#212121"));

        group.addView(t);
        return t;
    }

    public View createLoadingView() {
        ScrollView sv = new ScrollView(getActivity());
        sv.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        sv.setTag("gg_scroll");
        sv.setFillViewport(true);
        LinearLayout l = new LinearLayout(getActivity());
        l.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        l.setGravity(Gravity.CENTER);

        ProgressBar pb = new ProgressBar(getActivity());
        pb.getIndeterminateDrawable().setColorFilter(GGApp.GG_APP.school.getAccentColor(), PorterDuff.Mode.SRC_IN);
        pb.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        pb.setVisibility(ProgressBar.VISIBLE);

        l.addView(pb);
        sv.addView(l);
        return sv;
    }

    public void createMessage(ViewGroup l, String text, String button, View.OnClickListener onclick) {
        RelativeLayout r = new RelativeLayout(getActivity());
        r.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        LinearLayout l2 = new LinearLayout(getActivity());
        l2.setOrientation(LinearLayout.VERTICAL);
        RelativeLayout.LayoutParams layoutparams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);
        layoutparams.addRule(RelativeLayout.CENTER_VERTICAL);
        l2.setLayoutParams(layoutparams);
        l2.setGravity(Gravity.CENTER_HORIZONTAL);

        TextView tv = new TextView(getActivity());
        LinearLayout.LayoutParams tvparams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        tv.setLayoutParams(tvparams);
        tv.setText(text);
        tv.setTextSize(23);
        tv.setPadding(0, 0, 0, toPixels(15));
        l2.addView(tv);

        if(button != null) {
            Button b = new Button(getActivity());
            LinearLayout.LayoutParams bparams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            b.setLayoutParams(bparams);
            b.setId(R.id.reload_button);
            b.setText(button);
            b.setTextSize(23);
            b.setAllCaps(false);
            b.setTypeface(null, Typeface.NORMAL);
            b.setOnClickListener(onclick);
            l2.addView(b);

        }

        r.addView(l2);
        l.addView(r);
    }

    public void createNoEntriesCard(ViewGroup vg, LayoutInflater inflater) {
        FrameLayout f2 = new FrameLayout(getActivity());
        f2.setPadding(toPixels(1.3f),toPixels(0.3f),toPixels(1.3f),toPixels(0.3f));
        CardView cv = createCardView();
        if (GGApp.GG_APP.isDarkThemeEnabled()) {
            cv.setCardBackgroundColor(Color.parseColor("#424242"));
        } else{
            cv.setCardBackgroundColor(Color.parseColor("#fafafa"));
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
            l.setPadding(toPixels(55), toPixels(4), toPixels(55), toPixels(4));
            return true;
        } else if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            l.setPadding(toPixels(4), toPixels(4), toPixels(4), toPixels(4));
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
                createMessage(vg, getResources().getString(R.string.login_required), getResources().getString(R.string.do_login), new View.OnClickListener() {
                    @Override
                    public void onClick(View c) {
                        //TODO: Login dialog (token expired)

                    }
                });
            } else {
                //TODO: Error view?
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
        ViewGroup fragmentLayout = (ViewGroup) getActivity().findViewById(R.id.fragment_layout);
        Animation fadeIn = AnimationUtils.loadAnimation(getActivity().getApplicationContext(), R.anim.fade_in);
        fragmentLayout.startAnimation(fadeIn);

    }

    public void saveInstanceState(Bundle b) {

    }

    public interface RemoteData {
        Throwable getThrowable();
        void save();
        boolean load();

    }

}
