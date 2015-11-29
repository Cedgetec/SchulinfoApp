/*
 * Copyright 2015 Fabian Schultis, Hauke Oldsen
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

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.CardView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.IOException;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import de.gebatzens.sia.GGApp;
import de.gebatzens.sia.MainActivity;
import de.gebatzens.sia.R;
import de.gebatzens.sia.data.Mensa;

public class MensaFragment extends RemoteDataFragment {

    SwipeRefreshLayout swipeContainer;
    Boolean screen_orientation_horizotal = false;

    public MensaFragment() {
        type = GGApp.FragmentType.MENSA;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup group, Bundle bundle) {
        ((MainActivity) getActivity()).updateMenu(R.menu.toolbar_menu);
        ViewGroup vg = (ViewGroup) inflater.inflate(R.layout.fragment_mensa, group, false);
        if(GGApp.GG_APP.mensa != null)
            createRootView(inflater, vg);
        return vg;
    }

    @Override
    public void onViewCreated(View v, Bundle b) {
        super.onViewCreated(v, b);

        swipeContainer = (SwipeRefreshLayout) v.findViewById(R.id.refresh);
        if (GGApp.GG_APP.isDarkThemeEnabled()) {
            swipeContainer.setProgressBackgroundColorSchemeColor(Color.parseColor("#424242"));
        } else{
            swipeContainer.setProgressBackgroundColorSchemeColor(Color.parseColor("#ffffff"));
        }
        // Setup refresh listener which triggers new data loading
        swipeContainer.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                GGApp.GG_APP.refreshAsync(new Runnable() {
                    @Override
                    public void run() {
                        swipeContainer.post(new Runnable() {
                            @Override
                            public void run() {
                                swipeContainer.setRefreshing(false);
                            }
                        });

                    }
                }, true, GGApp.FragmentType.MENSA);
            }
        });
        // Configure the refreshing colors
        swipeContainer.setColorSchemeResources(R.color.custom_material_green,
                R.color.custom_material_red,
                R.color.custom_material_blue,
                R.color.custom_material_orange);

    }

    @Override
    public void createView(LayoutInflater inflater, ViewGroup view) {
        LinearLayout lroot = (LinearLayout) view.findViewById(R.id.mensa_content);
        ScrollView sv = new ScrollView(getActivity());
        sv.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        sv.setTag("gg_scroll");
        LinearLayout l = new LinearLayout(getActivity());
        createRootLayout(l);
        lroot.addView(sv);
        sv.addView(l);

        if(GGApp.GG_APP.mensa.isEmpty()) {
            createNoEntriesCard(l, inflater);
        } else {
            for (Mensa.MensaItem item : GGApp.GG_APP.mensa) {
                if (!item.isPast()) {
                    //try {
                        l.addView(createCardItem(item, inflater));
                    //} catch (Exception e) {
                    //    e.printStackTrace();
                    //}
                }
            }
        }
    }

    @Override
    public ViewGroup getContentView() {
        return (ViewGroup) getView().findViewById(R.id.mensa_content);
    }

    private CardView createCardItem(Mensa.MensaItem mensa_item, LayoutInflater i) {
        CardView mcv = createCardView();
        if (GGApp.GG_APP.isDarkThemeEnabled()) {
            mcv.setCardBackgroundColor(Color.DKGRAY);
        } else{
            mcv.setCardBackgroundColor(Color.WHITE);
        }
        mcv.setContentPadding(0, 0, 0, 0);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, toPixels(6));
        mcv.setLayoutParams(params);
        i.inflate(R.layout.mensa_cardview_entry, mcv, true);
        if(mensa_item.isPast())
            mcv.setAlpha(0.65f);
        String[] colors = getActivity().getResources().getStringArray(GGApp.GG_APP.school.getColorArray());
        ((TextView) mcv.findViewById(R.id.mcv_date)).setText(getFormatedDate(mensa_item.date));
        ((TextView) mcv.findViewById(R.id.mcv_meal)).setText(mensa_item.meal);
        ((TextView) mcv.findViewById(R.id.mcv_garnish)).setText(getResources().getString(R.string.garnish) + ": " + mensa_item.garnish.replace("mit ","").replace("mit",""));
        ((TextView) mcv.findViewById(R.id.mcv_dessert)).setText(getResources().getString(R.string.dessert) + ": " + mensa_item.dessert);
        ((TextView) mcv.findViewById(R.id.mcv_day)).setText(getDayByDate(mensa_item.date));
        ((ImageView) mcv.findViewById(R.id.mcv_imgvegi)).setImageBitmap((Integer.valueOf(mensa_item.vegetarian) == 1) ? BitmapFactory.decodeResource(getResources(), R.drawable.ic_vegetarian) : BitmapFactory.decodeResource(getResources(), R.drawable.ic_meat));
        if(screen_orientation_horizotal) {
            LinearLayout mcvImageContainer = (LinearLayout) mcv.findViewById(R.id.mcv_image_container);
            ViewGroup.LayoutParams mcvImageContainerLayoutParams = mcvImageContainer.getLayoutParams();
            mcvImageContainerLayoutParams.height = toPixels(240);
        }
        ViewHolder vh = new ViewHolder();
        vh.imgview = (ImageView) mcv.findViewById(R.id.mcv_image);
        vh.filename = mensa_item.image;
        new AsyncTask<ViewHolder, Void, ViewHolder>() {

            @Override
            protected ViewHolder doInBackground(ViewHolder... params) {
                //params[0].bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.block_house_steak);
                try {
                    Bitmap bitmap = cacheGetBitmap(params[0].filename);
                    if(bitmap!=null) {
                        params[0].bitmap = bitmap;
                    } else {
                        bitmap = GGApp.GG_APP.remote.getMensaImage(params[0].filename);
                        cacheSaveBitmap(params[0].filename, bitmap);
                        params[0].bitmap = bitmap;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    params[0].bitmap = null;
                }

                return params[0];
            }

            @Override
            protected void onPostExecute(ViewHolder result) {
                try {
                    ImageView imgView = result.imgview;
                    if(result.bitmap != null) {
                        imgView.setImageBitmap(result.bitmap);
                    } else {
                        imgView.setImageBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.no_content));
                    }
                    imgView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.execute(vh);
        return mcv;
    }

    private String getDayByDate(String date) {
        String formattedDate;
        DateFormat parser = new SimpleDateFormat("yyyy-MM-dd");
        DateFormat dateFormatter = new SimpleDateFormat("EEE");
        try
        {
            Date parsedDate = parser.parse(date);
            formattedDate = dateFormatter.format(parsedDate);
            return formattedDate;
        }
        catch (ParseException e)
        {
            e.printStackTrace();
            return "";
        }
    }

    private String getFormatedDate(String date) {
        String formattedDate;
        DateFormat parser = new SimpleDateFormat("yyyy-MM-dd");
        DateFormat dateFormatter;
        if(Locale.getDefault().getLanguage().equals("de")) {
            dateFormatter = new SimpleDateFormat("d. MMM");
        } else if(Locale.getDefault().getLanguage().equals("en")) {
            dateFormatter = new SimpleDateFormat("MM/dd/yyyy");
        } else {
            dateFormatter = new SimpleDateFormat("yyyy-MM-dd");
        }
        try
        {
            Date parsedDate = parser.parse(date);
            formattedDate = dateFormatter.format(parsedDate);
            return formattedDate;
        }
        catch (ParseException e)
        {
            e.printStackTrace();
            return "";
        }
    }

    public Bitmap cacheGetBitmap(String filename) {
        try {
            return BitmapFactory.decodeStream(getActivity().openFileInput("cache_" + filename));
        } catch(Exception e) {
            return null;
        }
    }

    private void cacheSaveBitmap(String filename, Bitmap image) {
        try {
            OutputStream fos = getActivity().openFileOutput("cache_" + filename, Context.MODE_PRIVATE);
            image.compress(Bitmap.CompressFormat.PNG, 90, fos);
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private class ViewHolder {
        ImageView imgview;
        Bitmap bitmap;
        String filename;
    }


}
