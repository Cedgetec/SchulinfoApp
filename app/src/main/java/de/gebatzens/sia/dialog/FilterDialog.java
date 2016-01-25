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
package de.gebatzens.sia.dialog;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.DialogFragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.EditText;

import de.gebatzens.sia.FilterActivity;
import de.gebatzens.sia.GGApp;
import de.gebatzens.sia.R;
import de.gebatzens.sia.data.Filter;

public class FilterDialog extends DialogFragment {

    FilterActivity activity;

    public static FilterDialog newInstance(boolean mainFilter, int position) {
        Bundle bundle = new Bundle();
        bundle.putBoolean("main_filter", mainFilter);
        bundle.putInt("update_position", position);
        FilterDialog filterDialog = new FilterDialog();
        filterDialog.setArguments(bundle);
        return filterDialog;
    }

    @Override
    public void onAttach(Activity a) {
        activity = (FilterActivity) a;
        super.onAttach(a);
    }

    @Override
    @NonNull
    public Dialog onCreateDialog(Bundle bundle) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(getString(isMainFilterDialog() ? R.string.set_main_filter : (getUpdatePosition() == -1 ? R.string.hide_subject : R.string.edit_filter)));
        builder.setView(View.inflate(getActivity(), R.layout.filter_dialog, null));

        builder.setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                activity.changed = true;
                EditText ed = (EditText) ((Dialog) dialog).findViewById(R.id.filter_text);
                CheckBox cb = (CheckBox) ((Dialog) dialog).findViewById(R.id.checkbox_contains);

                String filtertext = ed.getText().toString().trim();
                if (filtertext.isEmpty()) {
                    Snackbar.make(getActivity().getWindow().getDecorView().findViewById(R.id.coordinator_layout), getString(R.string.invalid_filter), Snackbar.LENGTH_LONG).show();
                } else {
                    if(isMainFilterDialog()) {
                        Filter.FilterList list = GGApp.GG_APP.filters;
                        list.mainFilter.setFilter(filtertext);
                        activity.mainFilterContent.setText(list.mainFilter.getFilter());
                    } else {
                        Filter f;
                        if(getUpdatePosition() == -1) {
                            f = new Filter();
                        } else {
                            f = GGApp.GG_APP.filters.get(getUpdatePosition());
                        }
                        f.setType(Filter.FilterType.SUBJECT);
                        f.setFilter(filtertext);
                        f.contains = cb.isChecked();

                        if(getUpdatePosition() == -1) {
                            GGApp.GG_APP.filters.add(f);
                        }

                        activity.adapter.notifyDataSetChanged();
                        FilterActivity.saveFilter(GGApp.GG_APP.filters);
                        activity.setListViewHeightBasedOnChildren();
                    }

                    FilterActivity.saveFilter(GGApp.GG_APP.filters);
                }

                ((InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE)).toggleSoftInput(0, 0);
                dialog.dismiss();
            }

        });

        builder.setNegativeButton(getString(R.string.abort), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        return builder.create();
    }

    private boolean isMainFilterDialog() {
        return getArguments().getBoolean("main_filter");
    }

    private int getUpdatePosition() {
        return getArguments().getInt("update_position");
    }

    @Override
    public void onResume() {
        super.onResume();

        ((InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE)).toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);

        Dialog d = getDialog();

        Filter.FilterList list = GGApp.GG_APP.filters;
        EditText ed = (EditText) d.findViewById(R.id.filter_text);
        ed.setSelectAllOnFocus(true);

        if(isMainFilterDialog()) {
            ed.setHint(list.mainFilter.getType() == Filter.FilterType.CLASS ? getString(R.string.school_class_name) : getString(R.string.teacher_shortcut));
            ed.setText(list.mainFilter.getFilter());

            d.findViewById(R.id.checkbox_contains).setVisibility(View.GONE);
        } else {
            ed.setHint(getString(R.string.subject_course_name));

            final CheckBox cb = (CheckBox) d.findViewById(R.id.checkbox_contains);
            cb.setEnabled(false);
            cb.setText(getString(R.string.all_subjects_including_disabled));

            ed.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {

                }

                @Override
                public void afterTextChanged(Editable s) {
                    String str = s.toString().trim();
                    if(str.length() == 0) {
                        cb.setEnabled(false);
                        cb.setText(getString(R.string.all_subjects_including_disabled));
                    } else {
                        cb.setEnabled(true);
                        cb.setText(getString(R.string.all_subjects_including, str));
                    }
                }
            });

            int p = getUpdatePosition();
            if(p != -1) {
                Filter f = GGApp.GG_APP.filters.get(p);

                cb.setChecked(f.contains);
                cb.setText(getString(R.string.all_subjects_including, f.getFilter()));
                ed.setText(f.getFilter());
            }
        }
    }

}
