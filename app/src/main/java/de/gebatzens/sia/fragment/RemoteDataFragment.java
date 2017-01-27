/*
 * Copyright 2015 - 2016 Hauke Oldsen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License 00+3at
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
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.CardView;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import de.gebatzens.sia.APIException;
import de.gebatzens.sia.FragmentData;
import de.gebatzens.sia.SIAApp;
import de.gebatzens.sia.R;

public abstract class RemoteDataFragment extends Fragment {

    public FragmentData getFragment() {
        return SIAApp.SIA_APP.school.fragments.get(getArguments().getInt("fragment"));
    }

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

    public static int toPixels(float dp) {
        float scale = SIAApp.SIA_APP.getResources().getDisplayMetrics().density;
        return (int) (dp * scale);
    }

    public void setOrientationPadding(View v) {
        if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            v.setPadding(toPixels(55), toPixels(0), toPixels(55), toPixels(0));
        } else if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            v.setPadding(toPixels(5), toPixels(0), toPixels(5), toPixels(0));
        }
    }

    public TextView createPrimaryTextView(String text, int size, LayoutInflater inflater, ViewGroup group) {
        TextView t = (TextView) inflater.inflate(R.layout.basic_textview_primary, group, false);
        t.setText(text);
        t.setPadding(0, 0, toPixels(20), 0);
        t.setTextSize(size);
        group.addView(t);
        return t;
    }

    public TextView createSecondaryTextView(String text, int size, LayoutInflater inflater, ViewGroup group) {
        TextView t = (TextView) inflater.inflate(R.layout.basic_textview_secondary, group, false);
        t.setText(text);
        t.setPadding(0, 0, toPixels(20), 0);
        t.setTextSize(size);
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
        pb.getIndeterminateDrawable().setColorFilter(SIAApp.SIA_APP.school.getAccentColor(), PorterDuff.Mode.SRC_IN);
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
        LinearLayout.LayoutParams tvparams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        tv.setLayoutParams(tvparams);
        tv.setText(text);
        tv.setTextSize(23);
        tv.setPadding(0, 0, 0, toPixels(15));
        l2.addView(tv);

        if(button != null) {
            Button b = new Button(getActivity());
            LinearLayout.LayoutParams bparams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            bparams.setMargins(toPixels(4), toPixels(0), toPixels(4), toPixels(4));
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

    public void createNoEntriesCard(ViewGroup parent, LayoutInflater inflater) {
        LinearLayout wrapper = new LinearLayout(parent.getContext());
        wrapper.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        setOrientationPadding(wrapper);

        CardView cv = (CardView) inflater.inflate(R.layout.basic_cardview, wrapper, false);
        createPrimaryTextView(getResources().getString(R.string.no_entries), 20, inflater, cv);
        wrapper.addView(cv);
        parent.addView(wrapper);
    }

    /**
     *
     * @return horizontal screen orientation
     */
    public boolean createRootLayout(LinearLayout l) {
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
        RemoteData data = getFragment().getData();
        if(data == null) {
            setFragmentLoading();
        } else if(data.getThrowable() != null) {
            Throwable t = data.getThrowable();
            if(t instanceof APIException) {
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
    public void onViewCreated(View v, Bundle b) {
        super.onViewCreated(v, b);

        if(getFragment().getData() == null && getContentView() != null) {
            getContentView().addView(createLoadingView());
        }

        FrameLayout contentFrame = (FrameLayout) getActivity().findViewById(R.id.content_fragment);
        contentFrame.setVisibility(View.VISIBLE);
        /*ViewGroup fragmentLayout = (ViewGroup) getActivity().findViewById(R.id.fragment_layout);
        Animation fadeIn = AnimationUtils.loadAnimation(getActivity().getApplicationContext(), R.anim.fade_in);
        fragmentLayout.startAnimation(fadeIn);*/

        final SwipeRefreshLayout swipeContainer = (SwipeRefreshLayout) v.findViewById(R.id.refresh);
        if(swipeContainer != null) {
            swipeContainer.setProgressBackgroundColorSchemeColor(ContextCompat.getColor(getContext(), R.color.SwipeRefreshLayout_background));

            // Setup refresh listener which triggers new data loading
            swipeContainer.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
                @Override
                public void onRefresh() {
                    SIAApp.SIA_APP.refreshAsync(new Runnable() {
                        @Override
                        public void run() {
                            swipeContainer.post(new Runnable() {
                                @Override
                                public void run() {
                                    swipeContainer.setRefreshing(false);
                                }
                            });

                        }
                    }, true, getFragment());
                }
            });
            // Configure the refreshing colors
            swipeContainer.setColorSchemeResources(R.color.SwipeRefreshProgressGreen,
                    R.color.SwipeRefreshProgressRed,
                    R.color.SwipeRefreshProgressBlue,
                    R.color.SwipeRefreshProgressOrange);
        }

    }

    public void saveInstanceState(Bundle b) {

    }

    public interface RemoteData {
        Throwable getThrowable();
        void save();
        boolean load();

    }

}
