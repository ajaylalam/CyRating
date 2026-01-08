package com.example.androidexample.utils;

import android.content.Context;

import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class CommentApi {

    private static final String BASE_URL = "http://coms-3090-019.class.las.iastate.edu:8080"; // or your Constants.BASE_URL

    private static RequestQueue getQueue(Context ctx) {
        // If you already have NetworkUtils.getQueue(ctx), use that instead
        return com.android.volley.toolbox.Volley.newRequestQueue(ctx.getApplicationContext());
    }

    /**
     * Fetch comments for a given course code.
     *
     * @param ctx          Context
     * @param courseCode   Course identifier
     * @param onSuccess    Called with list of comment JSONObjects
     * @param onError      Called with VolleyError on failure
     */
    public static void getCommentsForCourse(
            Context ctx,
            String courseCode,
            Consumer<List<JSONObject>> onSuccess,
            Consumer<VolleyError> onError
    ) {
        String encoded = courseCode;
        try {
            encoded = java.net.URLEncoder.encode(courseCode, "UTF-8").replace("+", "%20");
        } catch (Exception ignored) {}
            String url = BASE_URL + "/comments/" + encoded;
            JsonArrayRequest commentsRequest = new JsonArrayRequest(
                Request.Method.GET,
                url,
                null,
                response -> {
                    try {
                        List<JSONObject> result = new ArrayList<>();
                        for (int i = 0; i < response.length(); i++) {
                            JSONObject obj = response.optJSONObject(i);
                            if (obj != null) {
                                result.add(obj);
                            }
                        }
                        onSuccess.accept(result);
                    } catch (Exception e) {
                        onError.accept(new VolleyError(e));
                    }
                },
                onError::accept
        );

        commentsRequest.setShouldCache(false);
        RequestQueue queue = getQueue(ctx);
        queue.getCache().clear();
        queue.add(commentsRequest);
    }

    public static void deleteComment(
            Context ctx,
            long commentId,
            Runnable onSuccess,
            Consumer<VolleyError> onError
    ) {
        String url = BASE_URL + "/comments/" + commentId;

        JsonObjectRequest req = new JsonObjectRequest(
                Request.Method.DELETE,
                url,
                null,
                res -> {
                    if (onSuccess != null) onSuccess.run();
                },
                error -> {
                    if (onError != null) onError.accept(error);
                }
        );

        getQueue(ctx).add(req);
    }

    public static void postComment(
            Context ctx,
            String username,
            String commentText,
            String className,
            Runnable onSuccess,
            Consumer<VolleyError> onError
    ) {
        String url = BASE_URL + "/comments";

        JSONObject body = new JSONObject();
        try {
            body.put("user", username);
            body.put("comment", commentText);
            body.put("className", className);
        } catch (JSONException ignored) {}

        JsonObjectRequest req = new JsonObjectRequest(
                Request.Method.POST,
                url,
                body,
                res -> {
                    if (onSuccess != null) onSuccess.run();
                },
                error -> {
                    if (onError != null) onError.accept(error);
                }
        ) {
            @Override
            public java.util.Map<String, String> getHeaders() {
                java.util.Map<String, String> headers = new java.util.HashMap<>();
                headers.put("Content-Type", "application/json");
                return headers;
            }
        };

        getQueue(ctx).add(req);
    }

    public static void updateComment(
            Context ctx,
            long commentId,
            String username,
            String commentText,
            String className,
            Runnable onSuccess,
            Consumer<VolleyError> onError
    ) {
        String url = BASE_URL + "/comments/" + commentId;

        JSONObject body = new JSONObject();
        try {
            body.put("user", username);
            body.put("comment", commentText);
            body.put("className", className);
        } catch (JSONException ignored) {}

        JsonObjectRequest req = new JsonObjectRequest(
                Request.Method.PUT,
                url,
                body,
                res -> {
                    if (onSuccess != null) onSuccess.run();
                },
                error -> {
                    if (onError != null) onError.accept(error);
                }
        ) {
            @Override
            public java.util.Map<String, String> getHeaders() {
                java.util.Map<String, String> headers = new java.util.HashMap<>();
                headers.put("Content-Type", "application/json");
                return headers;
            }
        };

        getQueue(ctx).add(req);
    }
}