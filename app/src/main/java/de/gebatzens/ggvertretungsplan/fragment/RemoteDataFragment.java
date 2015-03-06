/*
 * Copyright (C) 2015 Hauke Oldsen
 *
 * This file is part of GGVertretungsplan.
 *
 * GGVertretungsplan is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * GGVertretungsplan is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with GGVertretungsplan.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.gebatzens.ggvertretungsplan.fragment;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.CardView;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import de.gebatzens.ggvertretungsplan.GGApp;
import de.gebatzens.ggvertretungsplan.MainActivity;
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
        group.addView(t);
        return t;
    }

    public View createLoadingView() {
        LinearLayout l = new LinearLayout(getActivity());
        l.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        l.setGravity(Gravity.CENTER);

        ProgressBar pb = new ProgressBar(getActivity());
        pb.getIndeterminateDrawable().setColorFilter(GGApp.GG_APP.provider.getColor(), PorterDuff.Mode.SRC_IN);
        pb.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        pb.setVisibility(ProgressBar.VISIBLE);

        l.addView(pb);
        return l;
    }

    public void createButtonWithText(Activity activity, ViewGroup l, String text, String button, View.OnClickListener onclick) {
        RelativeLayout r = new RelativeLayout(activity);
        r.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        TextView tv = new TextView(activity);
        RelativeLayout.LayoutParams tvparams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,ViewGroup.LayoutParams.WRAP_CONTENT);
        tvparams.addRule(RelativeLayout.ABOVE, R.id.reload_button);
        tvparams.addRule(RelativeLayout.CENTER_HORIZONTAL);
        tv.setLayoutParams(tvparams);
        tv.setText(text);
        tv.setTextSize(23);
        tv.setPadding( 0, 0, 0, toPixels(15));
        tv.setGravity(Gravity.CENTER_HORIZONTAL);
        r.addView(tv);

        Button b = new Button(activity);
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

    public void createRootView(final LayoutInflater inflater, ViewGroup vg) {
        RemoteData data = GGApp.GG_APP.getDataForFragment(type);
        if(data == null) {
            setFragmentLoading();
        } else if(data.getThrowable() != null) {
            Throwable t = data.getThrowable();
            if(t instanceof VPLoginException) {
                createButtonWithText(getActivity(), vg, getResources().getString(R.string.login_required), getResources().getString(R.string.do_login), new View.OnClickListener() {
                    @Override
                    public void onClick(View c) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                        AlertDialog dialog;
                        builder.setTitle(getResources().getString(R.string.login));
                        builder.setView(inflater.inflate(R.layout.login_dialog, null));

                        builder.setPositiveButton(getResources().getString(R.string.do_login_submit), new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(final DialogInterface dialog, int which) {
                                GGApp.GG_APP.activity.mContent.setFragmentLoading();
                                new AsyncTask<Integer, Integer, Integer>() {

                                    @Override
                                    public void onPostExecute(Integer v) {
                                        switch(v) {
                                            case 1:
                                                GGApp.GG_APP.showToast(getResources().getString(R.string.username_or_password_wrong));
                                                break;
                                            case 2:
                                                GGApp.GG_APP.showToast(getResources().getString(R.string.could_not_contact_logon_server));
                                                break;
                                            case 3:
                                                GGApp.GG_APP.showToast(getResources().getString(R.string.unknown_error_at_logon));
                                                break;
                                        }

                                        if(v != 0)
                                            ((MainActivity) GGApp.GG_APP.activity).mContent.updateFragment();

                                    }

                                    @Override
                                    protected Integer doInBackground(Integer... params) {
                                        String user = ((EditText) ((Dialog) dialog).findViewById(R.id.usernameInput)).getText().toString();
                                        String pass = ((EditText) ((Dialog) dialog).findViewById(R.id.passwordInput)).getText().toString();
                                        return GGApp.GG_APP.provider.login(user, pass);

                                    }

                                }.execute();
                                dialog.dismiss();
                            }
                        });


                        builder.setNegativeButton(getResources().getString(R.string.abort), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });

                        dialog = builder.create();
                        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
                        dialog.show();

                    }
                });
            } else {
                createButtonWithText(getActivity(), vg, getResources().getString(R.string.check_connection_and_repeat), getResources().getString(R.string.again), new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        GGApp.GG_APP.refreshAsync(null, true, GGApp.FragmentType.PLAN);
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
        public void save(String file);
        public boolean load(String file);

    }

}
