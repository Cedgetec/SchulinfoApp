/*
 * Copyright 2015 Fabian Schultis, Hauke Oldsen
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

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
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

import de.gebatzens.sia.SIAApp;
import de.gebatzens.sia.MainActivity;
import de.gebatzens.sia.R;
import de.gebatzens.sia.data.Mensa;

public class MensaFragment extends RemoteDataFragment {

    Boolean screenOrientationHorizontal = false;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup group, Bundle bundle) {
        ((MainActivity) getActivity()).updateMenu(R.menu.toolbar_menu);
        ViewGroup vg = (ViewGroup) inflater.inflate(R.layout.fragment_mensa, group, false);
        if(getFragment().getData() != null)
            createRootView(inflater, vg);
        return vg;
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

        if(((Mensa) getFragment().getData()).isEmpty()) {
            createNoEntriesCard(l, inflater);
        } else {
            for (Mensa.MensaItem item : ((Mensa) getFragment().getData())) {
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
        CardView mcv = (CardView) i.inflate(R.layout.basic_cardview, null);
        mcv.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        mcv.setContentPadding(0, 0, 0, 0);
        i.inflate(R.layout.mensa_cardview_entry, mcv, true);
        if(mensa_item.isPast())
            mcv.setAlpha(0.65f);
        String[] colors = getActivity().getResources().getStringArray(SIAApp.SIA_APP.school.getColorArray());
        ((TextView) mcv.findViewById(R.id.mcv_date)).setText(getFormatedDate(mensa_item.date));
        ((TextView) mcv.findViewById(R.id.mcv_meal)).setText(mensa_item.meal);
        ((TextView) mcv.findViewById(R.id.mcv_garnish)).setText(getResources().getString(R.string.garnish) + ": " + mensa_item.garnish.replace("mit ","").replace("mit",""));
        ((TextView) mcv.findViewById(R.id.mcv_dessert)).setText(getResources().getString(R.string.dessert) + ": " + mensa_item.dessert);
        ((TextView) mcv.findViewById(R.id.mcv_day)).setText(getDayByDate(mensa_item.date));
        ((ImageView) mcv.findViewById(R.id.mcv_imgvegi)).setImageBitmap((Integer.valueOf(mensa_item.vegetarian) == 1) ? BitmapFactory.decodeResource(getResources(), R.drawable.ic_vegetarian) : BitmapFactory.decodeResource(getResources(), R.drawable.ic_meat));
        if(screenOrientationHorizontal) {
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
                        bitmap = SIAApp.SIA_APP.api.getMensaImage(params[0].filename);
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
        switch (Locale.getDefault().getLanguage()) {
            case "de":
                dateFormatter = new SimpleDateFormat("d. MMM");
                break;
            case "en":
                dateFormatter = new SimpleDateFormat("MM/dd/yyyy");
                break;
            default:
                dateFormatter = new SimpleDateFormat("yyyy-MM-dd");
                break;
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
