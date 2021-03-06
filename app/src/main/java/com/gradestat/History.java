package com.gradestat;


import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.utils.Utils;

import org.threeten.bp.LocalDate;
import org.threeten.bp.format.DateTimeFormatter;
import org.threeten.bp.format.FormatStyle;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static org.threeten.bp.temporal.ChronoUnit.DAYS;

public class History extends Fragment {

    Table table;
    int portraitCount;
    int landscapeCount;
    private final DecimalFormat df = new DecimalFormat("#.##");
    private final DateTimeFormatter dateFormat = DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT);

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.frag_history, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        table = (Table) requireArguments().getSerializable("table");
        Objects.requireNonNull(((AppCompatActivity) requireActivity()).getSupportActionBar()).setTitle(R.string.history);

        ((TextView) view.findViewById(R.id.emptyText)).setText(R.string.no_subjects);

        // check if any subjects are available else display appropriate text
        // prevent chart from loading on empty list of subjects
        if (!checkList()) {
            return;
        }

        int textColor = MainActivity.getAttr(requireActivity(), android.R.attr.textColorPrimary);

        LineChart chart = view.findViewById(R.id.linechart);

        List<ILineDataSet> dataSets = new ArrayList<>();

        LocalDate first = table.getFirst();
        LocalDate last = table.getLast();
        // single subject average mix is not ready for production.
        boolean mergeAverage = true;

        //noinspection ConstantConditions
        if (mergeAverage) {
            // display average of table over time
            List<Entry> mergeEntries = new ArrayList<>();
            List<Entry> singleEntries = new ArrayList<>();

            List<Table.Subject.Grade> grades = new ArrayList<>();

            for (Table.Subject s : table.getSubjects()) {
                if (s.isValid()) {
                    for (Table.Subject.Grade g : s.getGrades()) {
                        if (g.isValid()) {
                            grades.add(g);
                        }
                    }
                }
            }

            for (Table.Subject.Grade g : sortGrades(grades)) {
                // prevent get average rounding to 0.5 and instead round to 0.1
                // display is more accurate.
                mergeEntries.add(new Entry(DAYS.between(first, g.creation), (float) Math.round(table.getAverage(g.creation, false) * 10) / 10));
                singleEntries.add(new Entry(DAYS.between(first, g.creation), (float) g.value));
            }

            LineDataSet mergeSet = new LineDataSet(mergeEntries, getString(R.string.average));
            int primary = ContextCompat.getColor(requireActivity(), R.color.design_default_color_primary);
            mergeSet.setColor(primary);
            mergeSet.setCircleHoleColor(primary);
            mergeSet.setCircleColor(primary);
            mergeSet.setCircleRadius(5);
            mergeSet.setLineWidth(2);

            LineDataSet singleSet = new LineDataSet(singleEntries, getString(R.string.grades));
            singleSet.setColor(textColor);
            singleSet.setCircleColor(textColor);
            singleSet.setCircleHoleColor(textColor);
            singleSet.setCircleRadius(5);
            singleSet.setLineWidth(2);

            dataSets.add(mergeSet);
            dataSets.add(singleSet);

            chart.getLegend().setTextColor(textColor);
        } else {
            // display average for each subject over time
            for (Table.Subject s : table.getSubjects()) {
                List<Entry> entries = new ArrayList<>();

                for (Table.Subject.Grade g : sortGrades(s.getGrades())) {
                    entries.add(new Entry(DAYS.between(first, g.creation), (float) g.value));
                }

                LineDataSet dataSet = new LineDataSet(entries, s.name);
                dataSets.add(dataSet);
            }
        }

        LineData data = new LineData(dataSets);
        data.setValueTextSize(12);
        // format values to match others
        data.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return df.format(value);
            }
        });
        data.setValueTextColor(textColor);
        data.setHighlightEnabled(false);


        chart.setData(data);
        chart.setMinOffset(24f);
        chart.getXAxis().setAxisMaximum(chart.getLineData().getXMax() + 2f);
        chart.getXAxis().setAxisMinimum(chart.getLineData().getXMin() - 2f);
        chart.setScaleYEnabled(false);
        chart.getDescription().setEnabled(false);
        chart.invalidate();

        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextColor(textColor);

        String text = dateFormat.format(first);
        if (first.getYear() == last.getYear()) {
            text = text.substring(0, text.length() - 3);
        }

        // this block of dark magic calculates the max amount of labels
        // for each screen orientation
        {
            Rect bounds = new Rect();
            Paint textPaint = new Paint();
            textPaint.setTextSize(16f);
            textPaint.getTextBounds(text, 0, text.length(), bounds);
            int labelWidth = bounds.width();
            DisplayMetrics displayMetrics = requireContext().getResources().getDisplayMetrics();
            portraitCount = (int) ((displayMetrics.widthPixels / displayMetrics.density) / 3 * 2) / labelWidth;
            landscapeCount = (int) ((displayMetrics.heightPixels / displayMetrics.density) / 2) / labelWidth;
        }

        xAxis.setLabelCount(portraitCount);
        xAxis.setTextSize(16);
        xAxis.setGranularity(1f);
        xAxis.setDrawGridLines(false);
        xAxis.setDrawAxisLine(false);
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getAxisLabel(float value, AxisBase axis) {
                // LocalDate target = first.plusDays((long) value);
                String target = dateFormat.format(first.plusDays((long) value));
                if (first.getYear() == last.getYear()) {
                    return target.substring(0, target.length() - 3);
                } else {
                    return target;
                }
            }
        });

        YAxis yAxis = chart.getAxisLeft();
        if (table.minGrade == 1) {
            yAxis.setAxisMinimum((float) table.minGrade - 1);
        } else {
            yAxis.setAxisMinimum((float) table.minGrade);
        }
        yAxis.setAxisMaximum((float) table.maxGrade);
        yAxis.setTextColor(textColor);
        yAxis.setTextSize(16);
        yAxis.setDrawGridLines(false);
        yAxis.setDrawAxisLine(false);
        yAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getAxisLabel(float value, AxisBase axis) {
                return df.format(value);
            }
        });

        YAxis yAxis2 = chart.getAxisRight();
        yAxis2.setDrawLabels(false);
        yAxis2.setDrawGridLines(false);
        yAxis2.setDrawAxisLine(false);

    }

    @Override
    public void onResume() {
        super.onResume();
        requireActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR);
    }

    @Override
    public void onPause() {
        super.onPause();
        requireActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // when orientation changes,
        // change the diagram size as well as the max label count.
        LineChart chart = requireView().findViewById(R.id.linechart);
        XAxis xAxis = chart.getXAxis();
        ViewGroup.LayoutParams params = chart.getLayoutParams();
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            params.height = ViewGroup.LayoutParams.MATCH_PARENT;
            xAxis.setLabelCount(landscapeCount);
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            params.height = (int) Utils.convertDpToPixel(420);
            xAxis.setLabelCount(portraitCount);
        }
    }

    private List<Table.Subject.Grade> sortGrades(List<Table.Subject.Grade> l) {
        ArrayList<Table.Subject.Grade> s = new ArrayList<>(l);
        Collections.sort(s, (g1, g2) -> g1.creation.compareTo(g2.creation));
        return s;
    }

    private boolean checkList() {
        View view = requireView();
        View linechart = view.findViewById(R.id.linechart);
        if (table.isValid()) {
            linechart.setVisibility(View.VISIBLE);
            view.findViewById(R.id.emptyCard).setVisibility(CardView.GONE);
            return true;
        } else {
            linechart.setVisibility(View.GONE);
            view.findViewById(R.id.emptyCard).setVisibility(CardView.VISIBLE);
            return false;
        }
    }

}
