/*
 * Copyright (C) 2015 Fabian Schultis, Hauke Oldsen
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

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.CardView;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import de.gebatzens.ggvertretungsplan.GGApp;
import de.gebatzens.ggvertretungsplan.R;
import de.gebatzens.ggvertretungsplan.data.Mensa;

public class MensaFragment extends RemoteDataFragment {

    SwipeRefreshLayout swipeContainer;
    String cache_file_prefix = "cache_mensa_";
    Boolean screen_orientation_horizotal = false;

    public MensaFragment() {
        type = GGApp.FragmentType.MENSA;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup group, Bundle bundle) {
        ViewGroup vg = (ViewGroup) inflater.inflate(R.layout.fragment_mensa, group, false);
        if(GGApp.GG_APP.mensa != null)
            createRootView(inflater, vg);
        return vg;
    }

    @Override
    public void onViewCreated(View v, Bundle b) {
        super.onViewCreated(v, b);

        swipeContainer = (SwipeRefreshLayout) v.findViewById(R.id.refresh);
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
        Display display = ((WindowManager) getActivity().getApplicationContext().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        int rotation = display.getRotation();
        Log.d("Screen orientation", String.valueOf(rotation));
        if((rotation == 3)||(rotation == 1)) {
            screen_orientation_horizotal = true;
        } else {
            screen_orientation_horizotal = false;
        }
        final ScrollView sv = new ScrollView(getActivity());
        sv.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        sv.setTag("mensa_scroll");
        ((LinearLayout) view.findViewById(R.id.mensa_content)).addView(sv);
        final LinearLayout l = new LinearLayout(getActivity());
        l.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        l.setOrientation(LinearLayout.VERTICAL);
        int p = toPixels(6);
        l.setPadding(p, p, p, p);
        sv.addView(l);
        for(Mensa.MensaItem item : GGApp.GG_APP.mensa) {
            if(!item.isPast())
                l.addView(createCardItem(item, inflater));

        }
    }

    @Override
    public ViewGroup getContentView() {
        return (ViewGroup) getView().findViewById(R.id.mensa_content);
    }

    private CardView createCardItem(Mensa.MensaItem mensa_item, LayoutInflater i) {
        CardView mcv = createCardView();
        mcv.setContentPadding(0, 0, 0, 0);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, toPixels(6));
        mcv.setLayoutParams(params);
        i.inflate(R.layout.mensa_cardview_entry, mcv, true);
        if(mensa_item.isPast())
            mcv.setAlpha(0.65f);
        String[] colors = getActivity().getResources().getStringArray(GGApp.GG_APP.provider.getColorArray());
        ((TextView) mcv.findViewById(R.id.mcv_date)).setText(getFormatedDate(mensa_item.date));
        ((TextView) mcv.findViewById(R.id.mcv_meal)).setText(mensa_item.meal);
        ((TextView) mcv.findViewById(R.id.mcv_garnish)).setText(getResources().getString(R.string.garnish) + ": " + mensa_item.garnish.replace("mit ","").replace("mit",""));
        ((TextView) mcv.findViewById(R.id.mcv_day)).setText(getDayByDate(mensa_item.date));
        ((ImageView) mcv.findViewById(R.id.mcv_imgvegi)).setImageBitmap((Integer.valueOf(mensa_item.vegi) == 1) ? BitmapFactory.decodeResource(getResources(), R.drawable.vegi) : BitmapFactory.decodeResource(getResources(), R.drawable.meat));
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
                    Bitmap bitmap;
                    if(cacheCheckDir()) {
                        bitmap = cacheGetBitmap(params[0].filename);
                        if(bitmap!=null) {
                            params[0].bitmap = bitmap;
                        } else {
                            bitmap = GGApp.GG_APP.provider.getMensaImage(params[0].filename);
                            cacheSetBitmap(params[0].filename, bitmap);
                            params[0].bitmap = bitmap;
                        }
                    } else {
                        bitmap = GGApp.GG_APP.provider.getMensaImage(params[0].filename);
                        cacheSetBitmap(params[0].filename, bitmap);
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
                    ImageView imgView = (ImageView) result.imgview;
                    if(result.bitmap != null) {
                        imgView.setImageBitmap((Bitmap) result.bitmap);
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

    private boolean cacheCheckDir() {
        File schulinfoapp_dir = new File(Environment.getExternalStorageDirectory() + "/SchulinfoAPP");
        if(schulinfoapp_dir.isDirectory()) {
            return true;
        } else {
            schulinfoapp_dir.mkdirs();
            if(schulinfoapp_dir.isDirectory()) {
                return true;
            } else {
                return false;
            }
        }
    }

    public Bitmap cacheGetBitmap(String filename) {
        File bitmap_file = new File(Environment.getExternalStorageDirectory() + "/SchulinfoAPP/" + cache_file_prefix + filename);
        if(bitmap_file.exists()) {
            return BitmapFactory.decodeFile(Environment.getExternalStorageDirectory() + "/SchulinfoAPP/" + cache_file_prefix + filename);
        } else {
            return null;
        }
    }

    private void cacheSetBitmap(String filename, Bitmap image) {
        File bitmap_file = new File(Environment.getExternalStorageDirectory() + "/SchulinfoAPP/" + cache_file_prefix + filename);
        try {
            if(!bitmap_file.exists()) {
                bitmap_file.createNewFile();
            }
            FileOutputStream fos = new FileOutputStream(bitmap_file);
            image.compress(Bitmap.CompressFormat.PNG, 90, fos);
            fos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }

    private class ViewHolder {
        ImageView imgview;
        Bitmap bitmap;
        String filename;
    }


}
