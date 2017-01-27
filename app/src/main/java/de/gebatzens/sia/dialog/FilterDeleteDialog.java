/*
 * Copyright 2017 Hauke Oldsen
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
package de.gebatzens.sia.dialog;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;

import de.gebatzens.sia.FilterActivity;
import de.gebatzens.sia.FilterListAdapter;
import de.gebatzens.sia.SIAApp;
import de.gebatzens.sia.data.Filter;

public class FilterDeleteDialog extends DialogFragment {

    public static FilterDeleteDialog newInstance(int title, int content, int pbtn, int nbtn, int pos, boolean exc) {
        Bundle b = new Bundle();
        b.putInt("title", title);
        b.putInt("content", content);
        b.putInt("pbtn", pbtn);
        b.putInt("nbtn", nbtn);
        b.putInt("position", pos);
        b.putBoolean("excluding", exc);

        FilterDeleteDialog td = new FilterDeleteDialog();
        td.setArguments(b);
        return td;
    }

    @Override
    @NonNull
    public Dialog onCreateDialog(Bundle b) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(getString(getArguments().getInt("title")));
        builder.setMessage(getString(getArguments().getInt("content")));
        builder.setPositiveButton(getString(getArguments().getInt("pbtn")), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int i) {
                int position = FilterDeleteDialog.this.getArguments().getInt("position");

                FilterActivity c = (FilterActivity) FilterDeleteDialog.this.getActivity();
                FilterListAdapter fla = getArguments().getBoolean("excluding") ? c.excAdapter : c.incAdapter;
                c.changed = true;

                Filter filter = fla.list.get(position);
                fla.list.remove(position);
                if(filter instanceof Filter.ExcludingFilter)
                    ((Filter.ExcludingFilter) filter).getParentFilter().excluding.remove(filter);

                c.updateData();
                FilterActivity.saveFilter(SIAApp.SIA_APP.filters);
                dialog.dismiss();
            }
        });

        builder.setNegativeButton(getString(getArguments().getInt("nbtn")), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        return builder.create();
    }

}
