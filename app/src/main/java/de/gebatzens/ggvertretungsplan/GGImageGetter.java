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

package de.gebatzens.ggvertretungsplan;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.text.Html;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

public class GGImageGetter implements Html.ImageGetter {

    @Override
    public Drawable getDrawable(final String source) {
        final WrapperDrawable w = new WrapperDrawable();

        new AsyncTask<WrapperDrawable, Void, Void>() {

            @Override
            protected Void doInBackground(WrapperDrawable... params) {
                try {
                    URL url = new URL("http://gymglinde.de/typo40/" + source);
                    Bitmap bitmap = getCachedBitmap(getFilename(url));
                    if(bitmap == null) {
                        InputStream in = url.openStream();
                        bitmap = BitmapFactory.decodeStream(in);
                        saveBitmap(getFilename(url), bitmap);
                    }
                    Drawable d = new BitmapDrawable(GGApp.GG_APP.getResources(), bitmap);
                    d.setBounds(0,0,d.getIntrinsicWidth(),d.getIntrinsicHeight());
                    params[0].drawable = d;
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return null;
            }
        }.execute(w);

        return w;
    }

    private Bitmap getCachedBitmap(String file) {
        try {
            InputStream in = GGApp.GG_APP.openFileInput("cache_" + file);
            return BitmapFactory.decodeStream(in);
        } catch(Exception e) {
            return null;
        }
    }

    private void saveBitmap(String file, Bitmap bitmap) {
        try {
            bitmap.compress(Bitmap.CompressFormat.PNG, 90, GGApp.GG_APP.openFileOutput("cache_" + file, Context.MODE_PRIVATE));
        } catch(Exception e) {
            e.printStackTrace();

        }
    }

    public String getFilename(URL u) {
        String url = u.toString();
        return url.substring(url.lastIndexOf('/') + 1, url.length());
    }

    static class WrapperDrawable extends BitmapDrawable {

        Drawable drawable;

        @Override
        public void draw(Canvas canvas) {
            if(drawable != null)
                drawable.draw(canvas);
        }

    }

}
