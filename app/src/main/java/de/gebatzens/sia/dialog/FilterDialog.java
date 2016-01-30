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
package de.gebatzens.sia.dialog;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.AppCompatSpinner;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import java.util.List;

import de.gebatzens.sia.FilterActivity;
import de.gebatzens.sia.FragmentData;
import de.gebatzens.sia.GGApp;
import de.gebatzens.sia.R;
import de.gebatzens.sia.data.Filter;
import de.gebatzens.sia.data.GGPlan;

public class FilterDialog extends DialogFragment {

    FilterActivity activity;

    public static FilterDialog newInstance(boolean mainFilter, int position, int mainPosition) {
        Bundle bundle = new Bundle();
        bundle.putBoolean("main_filter", mainFilter);
        bundle.putInt("update_position", position);
        bundle.putInt("main_filter_position", mainPosition);
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
        builder.setTitle(getString(isMainFilterDialog() ? (getUpdatePosition() == -1 ? R.string.add_main_filter : R.string.edit_filter) : (getUpdatePosition() == -1 ? R.string.hide_subject : R.string.edit_filter)));
        builder.setView(View.inflate(getActivity(), R.layout.filter_dialog, null));

        builder.setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                activity.changed = true;
                EditText ed = (EditText) ((Dialog) dialog).findViewById(R.id.filter_text);
                CheckBox cb = (CheckBox) ((Dialog) dialog).findViewById(R.id.checkbox_contains);
                AppCompatSpinner spinner = (AppCompatSpinner) ((Dialog) dialog).findViewById(R.id.filter_spinner);

                String filtertext = ed.getText().toString().trim();
                if (filtertext.isEmpty()) {
                    Snackbar.make(getActivity().getWindow().getDecorView().findViewById(R.id.coordinator_layout), getString(R.string.invalid_filter), Snackbar.LENGTH_LONG).show();
                } else {
                    if(isMainFilterDialog()) {
                        Filter.IncludingFilter mainFilter = null;
                        if(getMainFilterPosition() == -1) {
                            mainFilter = new Filter.IncludingFilter(spinner.getSelectedItemPosition() == 0 ? Filter.FilterType.CLASS : Filter.FilterType.TEACHER, filtertext);
                            GGApp.GG_APP.filters.including.add(mainFilter);
                        } else {
                            mainFilter = GGApp.GG_APP.filters.including.get(getMainFilterPosition());
                            mainFilter.setFilter(filtertext);
                            mainFilter.setType(spinner.getSelectedItemPosition() == 0 ? Filter.FilterType.CLASS : Filter.FilterType.TEACHER);
                        }
                    } else {
                        Filter.ExcludingFilter f;
                        Filter.IncludingFilter inc = GGApp.GG_APP.filters.including.get(spinner.getSelectedItemPosition());
                        if(getUpdatePosition() == -1) {
                            f = new Filter.ExcludingFilter(Filter.FilterType.SUBJECT, filtertext, inc);
                            inc.excluding.add(f);
                        } else {
                            f = inc.excluding.get(getUpdatePosition());
                            f.setFilter(filtertext);
                        }

                        f.contains = cb.isChecked();
                    }

                    activity.updateData();
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
                ((InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE)).toggleSoftInput(0, 0);
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

    private int getMainFilterPosition() {
        return getArguments().getInt("main_filter_position");
    }

    @Override
    public void onResume() {
        super.onResume();

        ((InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE)).toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);

        Dialog d = getDialog();

        final Filter.FilterList list = GGApp.GG_APP.filters;
        final AutoCompleteTextView ed = (AutoCompleteTextView) d.findViewById(R.id.filter_text);
        final AppCompatSpinner spinner = (AppCompatSpinner) d.findViewById(R.id.filter_spinner);
        final CheckBox cb = (CheckBox) d.findViewById(R.id.checkbox_contains);
        final TextView label = (TextView) d.findViewById(R.id.filter_label);
        ed.setSelectAllOnFocus(true);

        if(isMainFilterDialog()) {
            ArrayAdapter<String> adapter = new ArrayAdapter<String>(getContext(), R.layout.support_simple_spinner_dropdown_item,  new String[] { getString(R.string.school_class), getString(R.string.teacher) });
            spinner.setAdapter(adapter);
            spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    ed.setHint((String) spinner.getSelectedItem());

                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {

                }
            });

            if(getMainFilterPosition() == -1) {
                ed.setText("");
                GGPlan.GGPlans plans = (GGPlan.GGPlans) GGApp.GG_APP.school.fragments.getData(FragmentData.FragmentType.PLAN).get(0).getData();

                //could confuse people
                /*if(plans != null) {
                    ed.setAdapter(new ArrayAdapter<String>(ed.getContext(), android.R.layout.simple_dropdown_item_1line, plans.getAllClasses()));
                }*/

                spinner.setSelection(0);
            } else {
                ed.setText(list.including.get(getMainFilterPosition()).getFilter());
                spinner.setSelection(list.including.get(getMainFilterPosition()).getType() == Filter.FilterType.CLASS ? 0 : 1);
            }

            cb.setVisibility(View.GONE);
            label.setVisibility(View.GONE);

        } else {
            ed.setHint(getString(R.string.subject_course_name));
            ArrayAdapter<Filter.IncludingFilter> adapter = new ArrayAdapter<>(getContext(), R.layout.support_simple_spinner_dropdown_item);
            adapter.addAll(list.including);
            spinner.setAdapter(adapter);

            cb.setEnabled(false);
            cb.setText(getString(R.string.all_subjects_including_disabled));

            int p = getUpdatePosition();
            if(p != -1) {
                Filter.ExcludingFilter f = list.including.get(getMainFilterPosition()).excluding.get(getUpdatePosition());

                cb.setChecked(f.contains);
                cb.setText(getString(R.string.all_subjects_including, f.getFilter()));
                ed.setText(f.getFilter());

                label.setText(f.getParentFilter().toString());
                label.setVisibility(View.VISIBLE);
                spinner.setVisibility(View.GONE);
            } else {
                label.setVisibility(View.GONE);
            }
        }

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

                int op = isMainFilterDialog() ? getMainFilterPosition() : getUpdatePosition();
                List<? extends Filter> flist = isMainFilterDialog() ? list.including : list.including.get(getMainFilterPosition()).excluding;
                Filter.FilterType currentType = isMainFilterDialog() ? (spinner.getSelectedItemPosition() == 0 ? Filter.FilterType.CLASS : Filter.FilterType.TEACHER) : Filter.FilterType.SUBJECT;

                for(int i = 0; i < flist.size(); i++) {
                    if(i == op) {
                        continue;
                    }

                    Filter f = flist.get(i);

                    if(f.getType() == currentType && f.getFilter().equals(str)) {
                        ed.setError(getString(R.string.filter_exists));
                    }
                }

                if(!isMainFilterDialog()) {

                    if (str.length() == 0) {
                        cb.setEnabled(false);
                        cb.setText(getString(R.string.all_subjects_including_disabled));
                    } else {
                        cb.setEnabled(true);
                        cb.setText(getString(R.string.all_subjects_including, str));
                    }
                }
            }
        });
    }

}
