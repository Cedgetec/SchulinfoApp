/*
 * Copyright 2015 - 2016 Hauke Oldsen
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

import android.content.Context;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.JsonReader;
import android.util.JsonWriter;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;

import de.gebatzens.sia.data.Filter;
import de.gebatzens.sia.dialog.FilterDialog;
import de.gebatzens.sia.dialog.TextDialog;

public class FilterActivity extends AppCompatActivity {

    Toolbar mToolBar;
    public FilterListAdapter incAdapter, excAdapter;
    ListView listViewInc, listViewExc;
    TextView textInc, textExc;

    public boolean changed = false;
    ScrollView sv;

    public ArrayList<Filter.ExcludingFilter> generateExcFilterList() {
        ArrayList<Filter.ExcludingFilter> list = new ArrayList<>();
        for(Filter.IncludingFilter inc : SIAApp.SIA_APP.filters.including) {
            list.addAll(inc.excluding);
        }

        return list;
    }

    public void updateData() {
        excAdapter.setList(generateExcFilterList());
        excAdapter.notifyDataSetChanged();
        incAdapter.notifyDataSetChanged();
        setListViewHeightBasedOnChildren();

        textExc.setVisibility(excAdapter.getCount() != 0 ? View.GONE : View.VISIBLE);
        textInc.setVisibility(incAdapter.getCount() != 0 ? View.GONE : View.VISIBLE);
    }

    @Override
    public void onCreate(Bundle bundle) {
        setTheme(SIAApp.SIA_APP.school.getTheme());
        super.onCreate(bundle);
        setContentView(R.layout.activity_filter);

        if(SIAApp.SIA_APP.preferences.getBoolean("first_use_filter", true)) {
            TextDialog.newInstance(R.string.explanation, R.string.filter_help).show(getSupportFragmentManager(), "filter_help");
        }

        sv = (ScrollView) findViewById(R.id.scrollView);

        SIAApp.SIA_APP.preferences.edit().putBoolean("first_use_filter", false).apply();

        textInc = (TextView) findViewById(R.id.no_entries_inc);
        textExc = (TextView) findViewById(R.id.no_entries_exc);

        listViewInc = (ListView) findViewById(R.id.filter_list_inc);
        incAdapter = new FilterListAdapter(this, SIAApp.SIA_APP.filters.including);
        textInc.setVisibility(incAdapter.getCount() != 0 ? View.GONE : View.VISIBLE);
        listViewInc.setAdapter(incAdapter);
        listViewInc.setDrawSelectorOnTop(true);

        listViewExc = (ListView) findViewById(R.id.filter_list_exc);
        excAdapter = new FilterListAdapter(this, generateExcFilterList());
        textExc.setVisibility(excAdapter.getCount() != 0 ? View.GONE : View.VISIBLE);
        listViewExc.setAdapter(excAdapter);
        listViewExc.setDrawSelectorOnTop(true);

        setListViewHeightBasedOnChildren();

        if(bundle != null)
            changed = bundle.getBoolean("changed", false);

        mToolBar = (Toolbar) findViewById(R.id.toolbar);
        mToolBar.setTitle(getTitle());
        mToolBar.setNavigationIcon(R.drawable.ic_arrow_back);
        mToolBar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        mToolBar.inflateMenu(R.menu.filter_menu);

        mToolBar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                if (menuItem.getItemId() == R.id.action_help) {
                    TextDialog.newInstance(R.string.explanation, R.string.filter_help).show(getSupportFragmentManager(), "filter_help");
                }
                return false;
            }
        });
        Button bt1 = (Button) findViewById(R.id.inc_button);
        Button bt2 = (Button) findViewById(R.id.exc_button);

        bt1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FilterDialog.newInstance(true, -1, -1).show(getSupportFragmentManager(), "add_main_filter");
            }
        });

        bt2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (SIAApp.SIA_APP.filters.including.size() == 0) {
                    Snackbar.make(getWindow().getDecorView().findViewById(R.id.coordinator_layout), getString(R.string.no_main_filter), Snackbar.LENGTH_LONG).show();
                } else {
                    FilterDialog.newInstance(false, -1, 0).show(getSupportFragmentManager(), "add_exc_filter");
                }
            }
        });

    }

    @Override
    public void onBackPressed() {
        finish();
    }

    public static Filter.FilterList loadFilterFallback() {
        Filter.FilterList list = new Filter.FilterList();

        try {
            InputStream in = SIAApp.SIA_APP.openFileInput("ggfilter");
            JsonReader reader = new JsonReader(new InputStreamReader(in));
            reader.beginArray();
            boolean main = true;

            while(reader.hasNext()) {
                reader.beginObject();

                String filter = "";
                Filter.FilterType type = null;
                boolean contains = false;

                while(reader.hasNext()) {
                    String name = reader.nextName();

                    switch (name) {
                        case "type":
                            type = Filter.FilterType.valueOf(reader.nextString());
                            break;
                        case "filter":
                            filter = reader.nextString();
                            break;
                        case "contains":
                            contains = reader.nextBoolean();
                            break;
                        default:
                            reader.skipValue();
                            break;
                    }

                }
                
                if(!filter.isEmpty()) {
                    if (main) {
                        Filter.IncludingFilter f = new Filter.IncludingFilter(type, filter);
                        list.including.add(f);
                        main = false;
                    } else {
                        Filter.ExcludingFilter f = new Filter.ExcludingFilter(type, filter, list.including.get(0));
                        f.contains = contains;
                        list.including.get(0).excluding.add(f);
                    }
                }

                reader.endObject();
            }
            reader.endArray();
            reader.close();
        } catch(Exception e) {
            list.clear();
        }

        return list;
    }

    public static Filter.FilterList loadFilter() {
        Filter.FilterList list = new Filter.FilterList();

        try {
            InputStream in = SIAApp.SIA_APP.openFileInput("ggfilterV2");
            JsonReader reader = new JsonReader(new InputStreamReader(in));
            reader.beginArray();

            while(reader.hasNext()) {
                reader.beginObject();

                Filter.IncludingFilter inc = new Filter.IncludingFilter();
                list.including.add(inc);

                while(reader.hasNext()) {
                    String name = reader.nextName();
                    switch(name) {
                        case "type":
                            inc.setType(Filter.FilterType.valueOf(reader.nextString()));
                            break;
                        case "filter":
                            inc.setFilter(reader.nextString());
                            break;
                        case "excluding":
                            reader.beginArray();
                            while(reader.hasNext()) {
                                reader.beginObject();
                                Filter.ExcludingFilter exc = new Filter.ExcludingFilter(Filter.FilterType.SUBJECT, "", inc);
                                inc.excluding.add(exc);

                                while(reader.hasNext()) {
                                    String en = reader.nextName();
                                    switch(en) {
                                        case "type":
                                            exc.setType(Filter.FilterType.valueOf(reader.nextString()));
                                            break;
                                        case "filter":
                                            exc.setFilter(reader.nextString());
                                            break;
                                        case "contains":
                                            exc.contains = reader.nextBoolean();
                                            break;
                                        default:
                                            reader.skipValue();
                                            break;
                                    }
                                }

                                reader.endObject();
                            }
                            reader.endArray();
                            break;
                        default:
                            reader.skipValue();
                            break;
                    }
                }

                reader.endObject();
            }
            reader.endArray();
            reader.close();
        } catch(Exception e) {
            list.clear();

            return loadFilterFallback();
        }

        return list;
    }

    @Override
    public void finish() {
        setResult(changed ? RESULT_OK : RESULT_CANCELED);
        saveFilter(SIAApp.SIA_APP.filters);
        super.finish();
    }

    public static void saveFilter(Filter.FilterList list) {
        try {
            OutputStream out = SIAApp.SIA_APP.openFileOutput("ggfilterV2", Context.MODE_PRIVATE);
            JsonWriter writer = new JsonWriter(new OutputStreamWriter(out));
            writer.setIndent("  ");
            writer.beginArray();
            for(Filter.IncludingFilter inc : list.including) {
                writer.beginObject();
                writer.name("type").value(inc.getType().toString());
                writer.name("filter").value(inc.getFilter());
                writer.name("excluding").beginArray();
                for (Filter.ExcludingFilter f : inc.excluding) {
                    writer.beginObject();
                    writer.name("type").value(f.getType().toString());
                    writer.name("filter").value(f.getFilter());
                    writer.name("contains").value(f.contains);
                    writer.endObject();
                }
                writer.endArray();
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
        setListViewHeightBasedOnChildren(listViewInc);
        setListViewHeightBasedOnChildren(listViewExc);
    }

    public void setListViewHeightBasedOnChildren(ListView listView) {
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
