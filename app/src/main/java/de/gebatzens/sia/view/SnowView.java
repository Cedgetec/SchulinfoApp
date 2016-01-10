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

package de.gebatzens.sia.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.View;

import de.gebatzens.sia.GGApp;
import de.gebatzens.sia.R;

public class SnowView extends View {

    Bitmap[] bitmaps;
    int[][] objs;
    float theight;

    public SnowView(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.SnowView, 0, 0);
        theight = a.getDimension(R.styleable.SnowView_theight, getHeight());
        a.recycle();

        bitmaps = new Bitmap[5];
        bitmaps[0] = BitmapFactory.decodeResource(getResources(), R.drawable.snow8);
        bitmaps[1] = BitmapFactory.decodeResource(getResources(), R.drawable.snow9);
        bitmaps[2] = BitmapFactory.decodeResource(getResources(), R.drawable.snow4);
        bitmaps[3] = BitmapFactory.decodeResource(getResources(), R.drawable.snow5);
        bitmaps[4] = BitmapFactory.decodeResource(getResources(), R.drawable.snow6);
        generate();

    }

    private void generate() {
        objs = new int[(int) Math.floor(Math.random() * 10 + 10)][3];
        float width = getWidth();

        for(int i = 0; i < objs.length; i++) {
            objs[i] = new int[] {(int) (width * Math.random()), (int) (theight * Math.random() * 1.5f), (int) Math.floor(Math.random() * bitmaps.length)};
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if("Winter".equals(GGApp.GG_APP.getCurrentThemeName())) {
            for (int[] obj : objs) {
                canvas.drawBitmap(bitmaps[obj[2]], obj[0], obj[1], null);
            }
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        generate();
    }

}
