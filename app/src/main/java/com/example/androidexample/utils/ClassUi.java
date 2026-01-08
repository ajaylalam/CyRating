package com.example.androidexample.utils;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.GridLayout;
import android.widget.TextView;

import com.example.androidexample.R;

import org.json.JSONObject;

import java.util.List;

public class ClassUi {

    // callback so each screen can decide what happens when a card is tapped
    public interface OnCourseClickListener {
        void onCourseClick(JSONObject course);
    }

    /**
     * Renders a grid of class cards into a GridLayout using the shared item_class_card layout.
     *
     * @param ctx       Context (usually Activity)
     * @param grid      The GridLayout to render into
     * @param courses   List of course JSONObjects
     * @param listener  Called when user taps a card (can be null)
     */
    public static void renderCourseGrid(
            Context ctx,
            GridLayout grid,
            List<JSONObject> courses,
            OnCourseClickListener listener
    ) {
        grid.removeAllViews();

        LayoutInflater inflater = LayoutInflater.from(ctx);
        int cols = grid.getColumnCount();
        if (cols <= 0) cols = 1;

        int marginH = (int) (12 * ctx.getResources().getDisplayMetrics().density);
        int marginV = (int) (6 * ctx.getResources().getDisplayMetrics().density);
        int cardHeight = (int) (120 * ctx.getResources().getDisplayMetrics().density); // ← Same height for all

        for (int i = 0; i < courses.size(); i++) {
            JSONObject course = courses.get(i);
            if (course == null) continue;

            View card = inflater.inflate(R.layout.item_class_card, grid, false);

            TextView courseNameView = card.findViewById(R.id.textCourseName);
            TextView courseIdView   = card.findViewById(R.id.textCourseId);

            courseNameView.setText(course.optString("courseName", "Unknown Course"));
            courseIdView.setText(course.optString("courseId", "Unknown Code"));


            int row = i / cols;
            int col = i % cols;

            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.rowSpec = GridLayout.spec(row);
            params.columnSpec = GridLayout.spec(col, 1f);
            params.width = 0;
            params.height = cardHeight;
            params.setMargins(marginH, marginV, marginH, marginV);

            card.setLayoutParams(params);

            if (listener != null) {
                card.setOnClickListener(v -> listener.onCourseClick(course));
            }

            grid.addView(card);
        }
    }
}