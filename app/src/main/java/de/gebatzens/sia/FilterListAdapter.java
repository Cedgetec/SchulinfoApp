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
import android.app.Dialog;
import android.content.DialogInterface;
import android.support.design.widget.Snackbar;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;

import de.gebatzens.sia.data.Filter;

public class FilterListAdapter extends BaseAdapter {

    Filter.FilterList list;
    FilterActivity c;

    public FilterListAdapter(FilterActivity c, Filter.FilterList filters) {
        this.c = c;
        list = filters;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        final Filter filter = list.get(position);
        final ViewGroup vg = (ViewGroup) c.getLayoutInflater().inflate(R.layout.filter_item, parent, false);
        ((TextView) vg.findViewById(R.id.filter_main_text)).setText(filter.toString(false));
        FrameLayout edit = (FrameLayout) vg.findViewById(R.id.filter_edit);
        edit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(c);
                builder.setTitle(c.getString(R.string.edit_filter));
                builder.setView(View.inflate(c, R.layout.filter_dialog, null));
                builder.setPositiveButton(c.getString(R.string.refresh), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        c.changed = true;
                        EditText text = (EditText) ((Dialog) dialog).findViewById(R.id.filter_text);
                        Filter f = list.get(position);
                        f.type = Filter.FilterType.SUBJECT;
                        f.filter = text.getText().toString().trim();
                        if (f.filter.isEmpty())
                            Snackbar.make(c.getWindow().getDecorView().findViewById(R.id.coordinator_layout), c.getString(R.string.invalid_filter), Snackbar.LENGTH_LONG).show();
                        else {
                            TextView tv = (TextView) vg.findViewById(R.id.filter_main_text);
                            tv.setText(f.toString(false));

                        }
                        dialog.dismiss();
                    }
                });
                builder.setNegativeButton(c.getString(R.string.abort), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
                AlertDialog d = builder.create();
                d.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
                d.show();
                EditText ed = (EditText) d.findViewById(R.id.filter_text);
                ed.setText(list.get(position).filter);
            }
        });
        vg.findViewById(R.id.filter_delete).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                c.changed = true;
                list.remove(position);
                notifyDataSetChanged();
                FilterActivity.saveFilter(GGApp.GG_APP.filters);
                FilterActivity.setListViewHeightBasedOnChildren(c.listView);

            }
        });
        return vg;
    }

    @Override
    public int getCount() {
        return list.size();
    }

    @Override
    public Object getItem(int position) {
        return list.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }


}
