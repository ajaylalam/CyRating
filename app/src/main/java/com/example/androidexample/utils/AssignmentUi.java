package com.example.androidexample.utils;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.TextView;

import com.example.androidexample.R;

import org.json.JSONObject;

import java.util.List;

public class AssignmentUi {

    public interface OnAssignmentClickListener {
        void onAssignmentClick(JSONObject assignment);
    }

    public interface OnSubmitClickListener {
        void onSubmitClick(JSONObject assignment);
    }

    /**
     * Renders a grid of assignment cards into a GridLayout using item_assignment_card.
     *
     * @param ctx              Context (Activity/Fragment)
     * @param grid             GridLayout container
     * @param assignmentsList  List of assignment JSONObjects
     * @param showSubmitButton Whether the submit button is visible
     * @param onCardClick      Called when user taps the card (can be null)
     * @param onSubmitClick    Called when user taps submit (if showSubmitButton == true; can be null)
     */
    public static void renderAssignmentGrid(
            Context ctx,
            GridLayout grid,
            List<JSONObject> assignmentsList,
            boolean showSubmitButton,
            OnAssignmentClickListener onCardClick,
            OnSubmitClickListener onSubmitClick
    ) {
        grid.removeAllViews();

        LayoutInflater inflater = LayoutInflater.from(ctx);
        int cols = grid.getColumnCount();
        if (cols <= 0) cols = 1; // safety

        int marginPx = (int) (16 * ctx.getResources().getDisplayMetrics().density);

        for (int i = 0; i < assignmentsList.size(); i++) {
            JSONObject obj = assignmentsList.get(i);
            if (obj == null) continue;

            String name        = obj.optString("name", "Unknown Assignment");
            String code        = obj.optString("courseId", "No course id");
            String description = obj.optString("description", "No description");
            String date        = obj.optString("dueDate", "No date given.");

            View card = inflater.inflate(R.layout.item_assignment_card, grid, false);

            TextView nameView        = card.findViewById(R.id.assignmentName);
            TextView classView       = card.findViewById(R.id.assignmentClass);
            TextView descriptionView = card.findViewById(R.id.assignmentDescription);
            TextView dateView        = card.findViewById(R.id.assignmentDueDate);
            Button submitButton      = card.findViewById(R.id.submitAssignmentButton);

            nameView.setText(name);
            classView.setText(code);
            descriptionView.setText(description);
            dateView.setText(date);

            if (showSubmitButton) {
                submitButton.setVisibility(View.VISIBLE);
                if (onSubmitClick != null) {
                    submitButton.setOnClickListener(v -> onSubmitClick.onSubmitClick(obj));
                }
            } else {
                submitButton.setVisibility(View.GONE);
            }

            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            int row = i / cols;
            int col = i % cols;

            params.rowSpec = GridLayout.spec(row);
            params.columnSpec = GridLayout.spec(col, 1f);
            params.width = 0; // equal column widths

            // margins
            int marginH = (int) (16 * ctx.getResources().getDisplayMetrics().density); // left/right
            int marginV = (int) (6 * ctx.getResources().getDisplayMetrics().density);  // top/bottom

            params.setMargins(marginH, marginV, marginH, marginV);

            card.setLayoutParams(params);

            if (onCardClick != null) {
                card.setOnClickListener(v -> onCardClick.onAssignmentClick(obj));
            }

            grid.addView(card);
        }
    }
}