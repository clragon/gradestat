package com.gradestat;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;
import androidx.preference.PreferenceManager;

import org.threeten.bp.LocalDate;
import org.threeten.bp.format.DateTimeFormatter;
import org.threeten.bp.format.FormatStyle;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;


public class GradeEditor extends DialogFragment {

    private Builder builder;

    private Table table;
    private EditText valueTitle;
    private EditText valueEdit;
    private EditText valueWeight;
    private TextView valueDate;
    private Button valueOK;
    private Button valueDelete;
    private ImageButton valueEditDate;
    private SeekBar valueSeekWeight;
    private ConstraintLayout weightEditor;
    private Integer dialogTheme;
    private final DecimalFormat df = new DecimalFormat("#.##");
    private final DateTimeFormatter dateFormat = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM);

    private final Map<Integer, Double> sliderValues = new HashMap<>();

    {
        sliderValues.put(0, 0.0);
        sliderValues.put(1, 0.25);
        sliderValues.put(2, 0.5);
        sliderValues.put(3, 1.0);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.frag_editor, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (builder == null) {
            dismiss();
        }

        valueTitle = view.findViewById(R.id.value_title);
        TextView valueValue = view.findViewById(R.id.value_value);
        valueEdit = view.findViewById(R.id.value_edit);
        valueWeight = view.findViewById(R.id.value_text1);
        valueDate = view.findViewById(R.id.value_text2);
        valueOK = view.findViewById(R.id.valueOK);
        valueDelete = view.findViewById(R.id.valueDelete);
        Button valueCancel = view.findViewById(R.id.valueCancel);
        valueEditDate = view.findViewById(R.id.valueEditDate);
        valueSeekWeight = view.findViewById(R.id.value_seek_weight);
        weightEditor = view.findViewById(R.id.weight_editor);
        CardView card = view.findViewById(R.id.valueCard);

        int background = MainActivity.getAttr(requireActivity(), android.R.attr.colorBackground);
        card.setCardBackgroundColor(background);

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(requireActivity());

        if (preferences.getBoolean("dark", true)) {
            dialogTheme = R.style.AppTheme_DatePicker;
        } else {
            dialogTheme = R.style.AppTheme_Light_DatePicker;
        }

        valueValue.setVisibility(View.GONE);
        valueEdit.setVisibility(View.VISIBLE);

        valueTitle.setHint(R.string.grade_name);

        if (android.os.Build.VERSION.SDK_INT <= android.os.Build.VERSION_CODES.LOLLIPOP) {
            valueDelete.setText("x");
        }

        FrameLayout editorHolder = view.findViewById(R.id.editorHolder);
        editorHolder.setOnFocusChangeListener((v, hasFocus) -> {

            if (hasFocus) {
                InputMethodManager imm = (InputMethodManager) requireActivity().getSystemService(Activity.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                checkFields();
            }
        });

        ImageView circle = view.findViewById(R.id.value_circle);
        valueEdit.addTextChangedListener(new TextWatcher() {
            int ringColor = MainActivity.getAttr(requireActivity(), android.R.attr.colorPrimary);

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (preferences.getBoolean("colorRings", true)) {
                    GradientDrawable ring = (GradientDrawable) circle.getDrawable().mutate();
                    int pending;
                    try {
                        pending = MainActivity.getGradeColor(requireActivity(), table, Double.parseDouble(s.toString()));
                    } catch (Exception ex) {
                        pending = MainActivity.getAttr(requireActivity(), android.R.attr.colorPrimary);
                    }

                    ValueAnimator colorAnimation = ValueAnimator.ofObject(new ArgbEvaluator(), ringColor, pending);
                    colorAnimation.setDuration(250);
                    colorAnimation.addUpdateListener(animator -> ring.setColor((int) animator.getAnimatedValue()));
                    colorAnimation.start();
                    ringColor = pending;
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });


        valueSeekWeight.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                valueWeight.setText(df.format(sliderValues.get(progress) * table.getFullWeight()));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        valueCancel.setOnClickListener(v -> {
            builder.onNo.onClick(v);
            dismiss();
        });

        if (builder.edit) {
            table = builder.grade.getTable();
            gradeEdit(builder.grade);
        } else {
            table = builder.subject.getTable();
            gradeCreate(builder.subject);
        }
    }

    private void gradeEdit(final Table.Subject.Grade grade) {

        valueTitle.setText(grade.name);
        valueEdit.setText(df.format(grade.value));
        valueWeight.setText(df.format(grade.weight));
        valueDate.setText(dateFormat.format(grade.creation));

        if (!grade.getTable().useWeight) {
            weightEditor.setVisibility(View.GONE);
        }

        int progress = valueSeekWeight.getMax();

        double weight = Double.parseDouble(valueWeight.getText().toString());

        for (Map.Entry<Integer, Double> entry : sliderValues.entrySet()) {
            if ((entry.getValue()) * table.getFullWeight() == weight) {
                progress = entry.getKey();
            }
        }

        valueSeekWeight.setProgress(progress);

        valueEditDate.setOnClickListener(v -> {
            LocalDate current = LocalDate.parse(valueDate.getText().toString(), dateFormat);
            // very weird fix. do not remove +1 and -1. DialogTheme starts months with 1, but threethen with 0 as it seems.
            new DatePickerDialog(getActivity(), dialogTheme, (view, year, month, dayOfMonth) -> valueDate.setText(dateFormat.format(LocalDate.of(year, month + 1, dayOfMonth))), current.getYear(), current.getMonthValue() - 1, current.getDayOfMonth()).show();
        });

        valueOK.setOnClickListener(v -> {
            if (checkFields()) {
                grade.name = valueTitle.getText().toString();
                grade.value = Double.parseDouble(valueEdit.getText().toString());
                grade.weight = Double.parseDouble(valueWeight.getText().toString());
                grade.creation = LocalDate.parse(valueDate.getText().toString(), dateFormat);
                builder.onYes.onClick(v);
                dismiss();
            }
        });

        valueDelete.setOnClickListener(v -> new AlertDialog.Builder(requireActivity())
                .setTitle(getResources().getString(R.string.confirmation))
                .setMessage(String.format(getResources().getString(R.string.delete_object), grade.name))
                .setPositiveButton(android.R.string.yes, (dialog, which) -> {
                    grade.getSubject().remGrade(grade);
                    builder.onDel.onClick(v);
                    dismiss();
                })
                .setNegativeButton(android.R.string.no, null)
                .setIcon(R.drawable.ic_warning)
                .show());

        valueDelete.setVisibility(View.VISIBLE);
    }

    private void gradeCreate(final Table.Subject subject) {

        valueTitle.setText(String.format("%s %x", subject.name, subject.getGrades().size() + 1));
        valueTitle.setSelectAllOnFocus(true);
        valueEdit.requestFocus();
        valueWeight.setText(df.format(1.0));
        valueDate.setText(dateFormat.format(LocalDate.now()));

        if (!subject.getTable().useWeight) {
            weightEditor.setVisibility(View.GONE);
        }

        valueEditDate.setOnClickListener(v -> {
            LocalDate current = LocalDate.parse(valueDate.getText().toString(), dateFormat);
            new DatePickerDialog(getActivity(), dialogTheme, (view, year, month, dayOfMonth) -> valueDate.setText(dateFormat.format(LocalDate.of(year, month + 1, dayOfMonth))), current.getYear(), current.getMonthValue() - 1, current.getDayOfMonth()).show();
        });

        valueOK.setOnClickListener(v -> {
            if (checkFields()) {
                String Name = valueTitle.getText().toString();
                double Value = Double.parseDouble(valueEdit.getText().toString());
                double Weight = Double.parseDouble(valueWeight.getText().toString());
                LocalDate creation = LocalDate.parse(valueDate.getText().toString(), dateFormat);
                subject.addGrade(Value, Weight, Name, creation);
                builder.onYes.onClick(v);
                dismiss();
            }
        });
    }

    private boolean checkFields() {
        boolean valid = true;
        if (valueTitle.getText().toString().replaceAll("\\s+", "").equals("")) {
            valueTitle.setError(getString(R.string.name_cannot_be_empty));
            valid = false;
        } else {
            valueTitle.setError(null);
        }
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(requireActivity());
        try {
            double value = Double.parseDouble(valueEdit.getText().toString());
            if (preferences.getBoolean("useLimits", true)) {
                if (!(value >= table.minGrade)) {
                    valid = false;
                    circleError();
                } else {
                    valueEdit.setError(null);
                }
                if (!(value <= table.maxGrade)) {
                    valid = false;
                    circleError();
                } else {
                    valueEdit.setError(null);
                }
            }
        } catch (Exception ex) {
            valid = false;
            circleError();
        }
        try {
            double weight = Double.parseDouble(valueWeight.getText().toString());
            if (!(weight >= 0)) {
                valueWeight.setError(getString(R.string.value_too_low));
                valid = false;
            } else {
                valueWeight.setError(null);
            }
            if (!(weight <= 10)) {
                valueWeight.setError(getString(R.string.value_too_high));
                valid = false;
            } else {
                valueWeight.setError(null);
            }
        } catch (Exception e) {
            valueWeight.setError(getString(R.string.value_cannot_be_empty));
            valid = false;
        }
        return valid;
    }

    private void circleError() {
        int flash = Color.rgb(237, 30, 26);
        ImageView circle = requireView().findViewById(R.id.value_circle);
        GradientDrawable ring = (GradientDrawable) circle.getDrawable().mutate();
        ring.setColor(flash);

        ValueAnimator flashToPrimary = ValueAnimator.ofObject(new ArgbEvaluator(), flash, MainActivity.getAttr(requireActivity(), android.R.attr.colorPrimary));
        flashToPrimary.setDuration(550);
        flashToPrimary.addUpdateListener(animator -> ring.setColor((int) animator.getAnimatedValue()));
        flashToPrimary.start();
    }

    @SuppressWarnings({"unused", "RedundantSuppression"})
    public static class Builder {

        private Table.Subject.Grade grade = null;
        private Table.Subject subject = null;
        private final boolean edit;
        private View.OnClickListener onYes = v -> {
        };
        private View.OnClickListener onNo = v -> {
        };
        private View.OnClickListener onDel;

        private final FragmentManager manager;

        public Builder(@NonNull FragmentManager manager, @NonNull Table.Subject.Grade grade) {
            this.manager = manager;
            this.grade = grade;
            edit = true;
        }

        public Builder(@NonNull FragmentManager manager, @NonNull Table.Subject subject) {
            this.manager = manager;
            this.subject = subject;
            edit = false;
        }

        public Builder setPositiveButton(View.OnClickListener onYes) {
            this.onYes = onYes;
            return this;
        }

        public Builder setNegativeButton(View.OnClickListener onNo) {
            this.onNo = onNo;
            return this;
        }

        public Builder setDeleteButton(View.OnClickListener onDel) {
            this.onDel = onDel;
            return this;
        }

        public void show() {

            if (this.onDel == null) {
                onDel = onYes;
            }
            GradeEditor editor = new GradeEditor();
            editor.builder = this;
            editor.show(manager, "editor");
        }
    }
}
