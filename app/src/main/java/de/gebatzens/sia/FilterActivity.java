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
package de.gebatzens.sia;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.JsonReader;
import android.util.JsonWriter;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import de.gebatzens.sia.data.Filter;
import de.gebatzens.sia.dialog.FilterDialog;
import de.gebatzens.sia.dialog.TextDialog;

public class FilterActivity extends AppCompatActivity {

    Toolbar mToolBar;
    public FilterListAdapter adapter;
    ListView listView;

    TextView mainFilterCategory;
    public TextView mainFilterContent;
    int selectedMode;
    int mainModePosition;
    public boolean changed = false;
    FloatingActionButton mAddFilterButton;
    ScrollView sv;

    @Override
    public void onCreate(Bundle bundle) {
        setTheme(GGApp.GG_APP.school.getTheme());
        super.onCreate(bundle);
        setContentView(R.layout.activity_filter);

        if(GGApp.GG_APP.preferences.getBoolean("first_use_filter", true)) {
            TextDialog.newInstance(R.string.explanation, R.string.filter_help).show(getSupportFragmentManager(), "filter_help");
        }

        sv = (ScrollView) findViewById(R.id.scrollView);

        GGApp.GG_APP.preferences.edit().putBoolean("first_use_filter", false).apply();

        final String[] main_filterStrings = new String[] { getApplication().getString(R.string.school_class), getApplication().getString(R.string.teacher)};

        listView = (ListView) findViewById(R.id.filter_list);
        adapter = new FilterListAdapter(this, GGApp.GG_APP.filters);
        listView.setAdapter(adapter);
        listView.setDrawSelectorOnTop(true);
        setListViewHeightBasedOnChildren();

        if(bundle != null)
            changed = bundle.getBoolean("changed", false);

        TextView tv = (TextView) findViewById(R.id.filter_sep_1);
        tv.setTextColor(GGApp.GG_APP.school.getAccentColor());
        TextView tv2 = (TextView) findViewById(R.id.filter_sep_2);
        tv2.setTextColor(GGApp.GG_APP.school.getAccentColor());

        Filter.FilterList list = GGApp.GG_APP.filters;
        mainFilterCategory = (TextView) findViewById(R.id.filter_main_category);
        mainFilterCategory.setText(list.mainFilter.type == Filter.FilterType.CLASS ? getApplication().getString(R.string.school_class) : getApplication().getString(R.string.teacher));
        mainFilterContent = (TextView) findViewById(R.id.filter_main_content);
        mainFilterContent.setText(list.mainFilter.filter);

        LinearLayout l_mode = (LinearLayout) findViewById(R.id.mainfilter_mode_layout);
        l_mode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View viewIn) {
                Filter.FilterList list = GGApp.GG_APP.filters;
                selectedMode = list.mainFilter.type == Filter.FilterType.CLASS ? 0 : list.mainFilter.type == Filter.FilterType.TEACHER ? 1 : 2;
                AlertDialog.Builder builder = new AlertDialog.Builder(FilterActivity.this);
                builder.setTitle(getApplication().getString(R.string.set_main_filter_mode))
                        .setSingleChoiceItems(main_filterStrings, selectedMode, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                changed = true;
                                mainModePosition = which == 0 ? 0 : 1;
                                Filter.FilterList list = GGApp.GG_APP.filters;
                                list.mainFilter.type = Filter.FilterType.values()[mainModePosition];
                                mainFilterCategory.setText(list.mainFilter.type == Filter.FilterType.CLASS ? getApplication().getString(R.string.school_class) : getApplication().getString(R.string.teacher));
                                FilterActivity.saveFilter(GGApp.GG_APP.filters);
                                dialog.dismiss();
                            }
                        });
                builder.setNegativeButton(getApplication().getString(R.string.abort), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
                AlertDialog d = builder.create();
                d.show();
            }
        });

        LinearLayout l_content = (LinearLayout) findViewById(R.id.mainfilter_content_layout);
        l_content.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View viewIn) {
                FilterDialog.newInstance(true, -1).show(getSupportFragmentManager(), "main_filter");
            }
        });

        mToolBar = (Toolbar) findViewById(R.id.toolbar);
        mToolBar.setTitleTextColor(Color.WHITE);
        mToolBar.setBackgroundColor(GGApp.GG_APP.school.getColor());
        mToolBar.setNavigationIcon(R.drawable.abc_ic_ab_back_mtrl_am_alpha);
        mToolBar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        mToolBar.inflateMenu(R.menu.filter_menu);
        mToolBar.setTitle(getTitle());

        mToolBar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                if (menuItem.getItemId() == R.id.action_help) {
                    TextDialog.newInstance(R.string.explanation, R.string.filter_help).show(getSupportFragmentManager(), "filter_help");
                }
                return false;
            }
        });

        mAddFilterButton = (FloatingActionButton) findViewById(R.id.addfilter_button);
        mAddFilterButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View viewIn) {
                FilterDialog.newInstance(false, -1).show(getSupportFragmentManager(), "add_filter");
            }
        });

    }

    @Override
    public void onBackPressed() {
        finish();
    }

    public static Filter.FilterList loadFilter() {
        Filter.FilterList list = new Filter.FilterList();
        list.mainFilter = null;

        try {
            InputStream in = GGApp.GG_APP.openFileInput("ggfilter");
            JsonReader reader = new JsonReader(new InputStreamReader(in));
            reader.beginArray();
            while(reader.hasNext()) {
                reader.beginObject();
                Filter f = new Filter();
                if(list.mainFilter == null)
                    list.mainFilter = f;
                else
                    list.add(f);
                while(reader.hasNext()) {
                    String name = reader.nextName();
                    if(name.equals("type"))
                        f.type = Filter.FilterType.valueOf(reader.nextString());
                    else if(name.equals("filter"))
                        f.filter = reader.nextString();
                    else if(name.equals("contains"))
                        f.contains = reader.nextBoolean();
                    else
                        reader.skipValue();

                }
                reader.endObject();
            }
            reader.endArray();
            reader.close();
        } catch(Exception e) {
            list.clear();
        }

        if(list.mainFilter == null) {
            Filter f = new Filter();
            list.mainFilter = f;
            f.type = Filter.FilterType.CLASS;
            f.filter = "";
        }

        return list;
    }

    @Override
    public void finish() {
        setResult(changed ? RESULT_OK : RESULT_CANCELED);
        saveFilter(GGApp.GG_APP.filters);
        super.finish();
    }

    public static void saveFilter(Filter.FilterList list) {
        try {
            OutputStream out = GGApp.GG_APP.openFileOutput("ggfilter", Context.MODE_PRIVATE);
            JsonWriter writer = new JsonWriter(new OutputStreamWriter(out));
            writer.setIndent("  ");
            writer.beginArray();
            writer.beginObject();
            writer.name("type").value(list.mainFilter.type.toString());
            writer.name("filter").value(list.mainFilter.filter);
            writer.endObject();
            for(Filter f : list) {
                writer.beginObject();
                writer.name("type").value(f.type.toString());
                writer.name("filter").value(f.filter);
                writer.name("contains").value(f.contains);
                writer.endObject();
            }
            writer.endArray();
            writer.close();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    protected void onSaveInstanceState(Bundle b) {
        super.onSaveInstanceState(b);
        b.putBoolean("changed", changed);
        b.putIntArray("ARTICLE_SCROLL_POSITION", new int[]{sv.getScrollX(), sv.getScrollY()});
    }

    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        final int[] position = savedInstanceState.getIntArray("ARTICLE_SCROLL_POSITION");
        if(position != null)
            sv.post(new Runnable() {
                public void run() {
                    sv.scrollTo(position[0], position[1]);
                }
            });
    }

    public void setListViewHeightBasedOnChildren() {
        ListAdapter listAdapter = listView.getAdapter();
        if (listAdapter == null)
            return;

        int desiredWidth = View.MeasureSpec.makeMeasureSpec(listView.getWidth(), View.MeasureSpec.UNSPECIFIED);
        int totalHeight = 0;
        View view = null;
        for (int i = 0; i < listAdapter.getCount(); i++) {
            view = listAdapter.getView(i, view, listView);
            if (i == 0)
                view.setLayoutParams(new ViewGroup.LayoutParams(desiredWidth, ViewGroup.LayoutParams.WRAP_CONTENT));

            view.measure(desiredWidth, View.MeasureSpec.UNSPECIFIED);
            totalHeight += view.getMeasuredHeight();
        }
        ViewGroup.LayoutParams params = listView.getLayoutParams();
        params.height = totalHeight + (listView.getDividerHeight() * (listAdapter.getCount() - 1));
        listView.setLayoutParams(params);
        listView.requestLayout();
    }
}
