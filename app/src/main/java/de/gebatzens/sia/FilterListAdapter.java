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

import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.TextView;

import java.util.ArrayList;

import de.gebatzens.sia.data.Filter;
import de.gebatzens.sia.dialog.FilterDialog;

public class FilterListAdapter extends BaseAdapter {

    ArrayList<? extends Filter> list;
    FilterActivity c;

    public FilterListAdapter(FilterActivity c, ArrayList<? extends Filter> list) {
        this.c = c;
        this.list = list;
    }

    public void setList(ArrayList<? extends Filter> f) {
        list = f;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        final Filter filter = list.get(position);
        final ViewGroup vg = (ViewGroup) c.getLayoutInflater().inflate(R.layout.filter_item, parent, false);
        String text = "";
        if(filter instanceof Filter.ExcludingFilter)
            text = ((Filter.ExcludingFilter) filter).getParentFilter().getFilter() + "     " + filter.getFilter();
        else
            text = filter.toString();

        ((TextView) vg.findViewById(R.id.filter_main_text)).setText(text);
        vg.findViewById(R.id.filter_star).setVisibility(filter.contains ? View.VISIBLE : View.GONE);

        FrameLayout edit = (FrameLayout) vg.findViewById(R.id.filter_edit);
        edit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean m = filter instanceof Filter.IncludingFilter;
                int mpos = GGApp.GG_APP.filters.including.indexOf(m ? filter : ((Filter.ExcludingFilter) filter).getParentFilter());

                int upos = position;
                if(!m) {
                    upos = ((Filter.ExcludingFilter) filter).getParentFilter().excluding.indexOf(filter);
                }

                FilterDialog.newInstance(m, upos, mpos).show(c.getSupportFragmentManager(), "edit_dialog");
            }
        });

        vg.findViewById(R.id.filter_delete).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(c);
                builder.setTitle(c.getString(R.string.delete_filter));
                builder.setMessage(c.getString(filter instanceof Filter.IncludingFilter ? R.string.delete_main_filter_message : R.string.delete_filter_message));
                builder.setPositiveButton(c.getString(R.string.delete), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        c.changed = true;
                        list.remove(position);
                        if(filter instanceof Filter.ExcludingFilter)
                            ((Filter.ExcludingFilter) filter).getParentFilter().excluding.remove(filter);

                        c.updateData();
                        FilterActivity.saveFilter(GGApp.GG_APP.filters);
                        dialog.dismiss();
                    }
                });
                builder.setNegativeButton(c.getString(R.string.abort), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
                builder.create().show();

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
