package com.example.androidexample.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.TextView;

import com.example.androidexample.R;

import org.json.JSONObject;

import java.util.List;

public class CommentUi {

    // ======== For professor/admin screen ========
    public interface OnRespondClickListener {
        void onRespond(int commentId, String currentProfessorResponse);
    }

    public interface OnDeleteClickListener {
        void onDelete(int commentId);
    }

    // ======== For CourseRating (student) screen ========
    public interface OnEditClickListener {
        void onEdit(int commentId, String currentText);
    }

    public interface OnUserDeleteClickListener {
        void onDelete(int commentId);
    }

    /**
     * Professor-facing comments.
     * Shows Respond/Delete if userRole is PROFESSOR or ADMIN.
     */
    public static void renderProfessorCommentsGrid(
            Context ctx,
            GridLayout grid,
            List<JSONObject> comments,
            String userRole,
            OnRespondClickListener respondListener,
            OnDeleteClickListener deleteListener
    ) {
        if (grid == null) return;

        grid.removeAllViews();

        LayoutInflater inflater = LayoutInflater.from(ctx);
        int cols = grid.getColumnCount();
        if (cols <= 0) cols = 1;

        int marginPx = (int) (16 * ctx.getResources().getDisplayMetrics().density);

        boolean isProfessor = "PROFESSOR".equalsIgnoreCase(userRole);
        boolean isAdmin     = "ADMIN".equalsIgnoreCase(userRole);

        for (int i = 0; i < comments.size(); i++) {
            JSONObject comment = comments.get(i);
            if (comment == null) continue;

            String className         = comment.optString("className", "");
            String user              = comment.optString("user", "Anonymous");
            String commentText       = comment.optString("comment", "");
            String professorResponse = comment.optString("professorResponse", null);
            int commentId            = comment.optInt("id", -1);

            View card = inflater.inflate(R.layout.professor_comment_card, grid, false);

            TextView classNameView = card.findViewById(R.id.commentClassName);
            TextView userView      = card.findViewById(R.id.commentUser);
            TextView textView      = card.findViewById(R.id.commentText);
            TextView profRespView  = card.findViewById(R.id.commentProfessorResponse);
            Button respondBtn      = card.findViewById(R.id.respondButton);
            Button deleteBtn       = card.findViewById(R.id.deleteButton);

            classNameView.setText(className);
            userView.setText("by " + user);
            textView.setText(commentText);

            if (professorResponse != null &&
                    !"null".equals(professorResponse) &&
                    !professorResponse.isEmpty()) {

                profRespView.setText("Professor: " + professorResponse);
                profRespView.setVisibility(View.VISIBLE);
                respondBtn.setText("Edit Response");
            } else {
                profRespView.setVisibility(View.GONE);
                respondBtn.setText("Respond");
            }

            boolean canRespond = isProfessor || isAdmin;

            if (canRespond && commentId != -1) {
                respondBtn.setVisibility(View.VISIBLE);
                respondBtn.setOnClickListener(v -> {
                    if (respondListener != null) {
                        respondListener.onRespond(commentId, professorResponse);
                    }
                });

                deleteBtn.setVisibility(View.VISIBLE);
                deleteBtn.setOnClickListener(v -> {
                    if (deleteListener != null) {
                        deleteListener.onDelete(commentId);
                    }
                });
            } else {
                respondBtn.setVisibility(View.GONE);
                deleteBtn.setVisibility(View.GONE);
            }

            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            int row = i / cols;
            int col = i % cols;

            params.rowSpec = GridLayout.spec(row);
            params.columnSpec = GridLayout.spec(col, 1f);
            params.width = 0;
            params.setMargins(marginPx, marginPx, marginPx, marginPx);

            card.setLayoutParams(params);
            grid.addView(card);
        }
    }

    /**
     * CourseRating screen.
     * Uses the same card layout, but:
     *  - Shows Edit/Delete only if comment.user == currentUsername
     *  - Professor response (if present) is read-only.
     */
    public static void renderCourseRatingCommentsGrid(
            Context ctx,
            GridLayout grid,
            List<JSONObject> comments,
            String currentUsername,
            OnEditClickListener editListener,
            OnUserDeleteClickListener deleteListener
    ) {
        if (grid == null) return;

        grid.removeAllViews();

        LayoutInflater inflater = LayoutInflater.from(ctx);
        int cols = grid.getColumnCount();
        if (cols <= 0) cols = 1;

        int marginPx = (int) (16 * ctx.getResources().getDisplayMetrics().density);

        for (int i = 0; i < comments.size(); i++) {
            JSONObject comment = comments.get(i);
            if (comment == null) continue;

            String className         = comment.optString("className", "");
            String user              = comment.optString("user", "Anonymous");
            String commentText       = comment.optString("comment", "");
            String professorResponse = comment.optString("professorResponse", null);
            int commentId            = comment.optInt("id", -1);

            View card = inflater.inflate(R.layout.professor_comment_card, grid, false);

            TextView classNameView = card.findViewById(R.id.commentClassName);
            TextView userView      = card.findViewById(R.id.commentUser);
            TextView textView      = card.findViewById(R.id.commentText);
            TextView profRespView  = card.findViewById(R.id.commentProfessorResponse);
            Button respondBtn      = card.findViewById(R.id.respondButton);
            Button deleteBtn       = card.findViewById(R.id.deleteButton);

            classNameView.setText(className);
            userView.setText("by " + user);
            textView.setText(commentText);

            // Show professor response if present
            if (professorResponse != null &&
                    !"null".equals(professorResponse) &&
                    !professorResponse.isEmpty()) {

                profRespView.setText("Professor: " + professorResponse);
                profRespView.setVisibility(View.VISIBLE);
            } else {
                profRespView.setVisibility(View.GONE);
            }

            boolean isOwner = user.equals(currentUsername);

            if (isOwner && commentId != -1) {
                // reuse respondButton as "Edit"
                respondBtn.setText("Edit");
                respondBtn.setVisibility(View.VISIBLE);
                respondBtn.setOnClickListener(v -> {
                    if (editListener != null) {
                        editListener.onEdit(commentId, commentText);
                    }
                });

                deleteBtn.setText("Delete");
                deleteBtn.setVisibility(View.VISIBLE);
                deleteBtn.setOnClickListener(v -> {
                    if (deleteListener != null) {
                        deleteListener.onDelete(commentId);
                    }
                });
            } else {
                respondBtn.setVisibility(View.GONE);
                deleteBtn.setVisibility(View.GONE);
            }

            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            int row = i / cols;
            int col = i % cols;

            params.rowSpec = GridLayout.spec(row);
            params.columnSpec = GridLayout.spec(col, 1f);
            params.width = 0;
            params.setMargins(marginPx, marginPx, marginPx, marginPx);

            card.setLayoutParams(params);
            grid.addView(card);
        }
    }
}