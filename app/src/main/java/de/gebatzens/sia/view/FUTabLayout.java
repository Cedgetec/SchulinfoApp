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
import android.support.annotation.NonNull;
import android.support.design.widget.TabLayout;
import android.util.AttributeSet;

import de.gebatzens.sia.R;

public class FUTabLayout  extends TabLayout {

    public FUTabLayout(Context context, AttributeSet attrs) {
        super(context, attrs);

    }

    @Override
    public void addTab(@NonNull Tab t, int position, boolean selected) {
        super.addTab(t, position, selected);
        t.setCustomView(R.layout.fu_tab);
    }

    @Override
    public void addTab(@NonNull Tab t, boolean selected) {
        super.addTab(t, selected);
        t.setCustomView(R.layout.fu_tab);

    }
}
