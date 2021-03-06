package com.gradestat;

import android.content.SharedPreferences;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.threeten.bp.format.DateTimeFormatter;
import org.threeten.bp.format.FormatStyle;

import java.text.DecimalFormat;
import java.util.Objects;

import static androidx.core.content.ContextCompat.getDrawable;


public class Grades extends Fragment {

    private Table.Subject subject;
    private RecyclerView recycler;
    private SharedPreferences preferences;
    private final DecimalFormat doubleFormat = new DecimalFormat("#.##");
    private final DateTimeFormatter dateFormat = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM);

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.frag_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        subject = (Table.Subject) requireArguments().getSerializable("subject");
        Objects.requireNonNull(((AppCompatActivity) requireActivity()).getSupportActionBar()).setTitle(subject.name);

        preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());

        recycler = view.findViewById(R.id.recyclerView);

        FloatingActionButton fab = view.findViewById(R.id.addItem);
        fab.setOnClickListener(v -> new GradeEditor.Builder(getParentFragmentManager(), subject)
                .setPositiveButton(v1 -> {
                    subject.getTable().save();
                    Objects.requireNonNull(((RecyclerView) view.findViewById(R.id.recyclerView)).getAdapter()).notifyDataSetChanged();
                    checkList();
                }).show());

        recycler.setLayoutManager(new LinearLayoutManager(getActivity()));
        recycler.setAdapter(new Adapter());

        ItemTouchHelper.Callback callback = new ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                if (viewHolder.getItemViewType() != target.getItemViewType()) {
                    return false;
                }

                // Notify the adapter of the move
                Objects.requireNonNull(recyclerView.getAdapter()).notifyItemMoved(viewHolder.getAdapterPosition(), target.getAdapterPosition());
                Table.Subject.Grade g = subject.getGrades().get(viewHolder.getAdapterPosition());
                // move grade to new position
                subject.movGrade(g, target.getAdapterPosition());
                subject.getTable().save();
                return true;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
            }
        };

        new ItemTouchHelper(callback).attachToRecyclerView(recycler);

        ((TextView) view.findViewById(R.id.emptyText)).setText(R.string.no_grades);

        checkList();

    }

    private void checkList() {
        if (!subject.getGrades().isEmpty()) {
            recycler.setVisibility(View.VISIBLE);
            requireView().findViewById(R.id.emptyCard).setVisibility(CardView.GONE);
        } else {
            recycler.setVisibility(View.GONE);
            requireView().findViewById(R.id.emptyCard).setVisibility(CardView.VISIBLE);
        }
    }

    public class Adapter extends RecyclerView.Adapter<Adapter.ViewHolder> {

        private class ViewHolder extends RecyclerView.ViewHolder {

            final TextView name;
            final TextView value;
            final TextView text1;
            final TextView text2;
            final ImageButton edit;
            final ImageView icon1;
            final ImageView icon2;
            final ImageView circle;


            ViewHolder(View itemView) {
                super(itemView);

                value = itemView.findViewById(R.id.value_value);
                name = itemView.findViewById(R.id.value_title);
                text1 = itemView.findViewById(R.id.value_text1);
                text2 = itemView.findViewById(R.id.value_text2);
                icon1 = itemView.findViewById(R.id.value_icon1);
                icon2 = itemView.findViewById(R.id.value_icon2);
                edit = itemView.findViewById(R.id.value_edit);
                circle = itemView.findViewById(R.id.value_circle);

                itemView.setOnClickListener(v -> edit.performClick());
            }
        }

        @Override
        public int getItemCount() {
            return subject.getGrades().size();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
            return new ViewHolder(LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.card_value, viewGroup, false));
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder view, final int i) {
            final Table.Subject.Grade g = subject.getGrades().get(i);

            view.value.setText(doubleFormat.format(g.value));
            view.name.setText(g.name);
            view.text1.setText(String.format("%s: %s", getResources().getString(R.string.weight), doubleFormat.format(g.weight)));
            view.icon1.setImageDrawable(getDrawable(requireActivity(), R.drawable.ic_weight));
            if (g.getTable().useWeight) {
                view.text1.setVisibility(View.VISIBLE);
                view.icon1.setVisibility(View.VISIBLE);
            } else {
                view.text1.setVisibility(View.GONE);
                view.icon1.setVisibility(View.GONE);
            }

            view.text2.setText(dateFormat.format(g.creation));
            view.icon2.setImageDrawable(getDrawable(requireActivity(), R.drawable.ic_calendar));
            if (preferences.getBoolean("colorRings", true)) {
                ((GradientDrawable) view.circle.getDrawable().mutate()).setColor((MainActivity.getGradeColor(getActivity(), g.getTable(), g.value)));
            }

            view.edit.setOnClickListener(v -> new GradeEditor.Builder(getParentFragmentManager(), g)
                    .setPositiveButton(v1 -> {
                        Objects.requireNonNull(recycler.getAdapter()).notifyDataSetChanged();
                        subject.getTable().save();
                        checkList();
                    }).show());
        }

        @Override
        public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
            super.onAttachedToRecyclerView(recyclerView);
        }

    }

}
