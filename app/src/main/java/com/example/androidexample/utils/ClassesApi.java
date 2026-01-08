package com.example.androidexample.utils;

import android.content.Context;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class ClassesApi {
    private static RequestQueue getQueue(Context ctx) {
        return com.android.volley.toolbox.Volley.newRequestQueue(ctx.getApplicationContext());
        // or use your NetworkUtils.getQueue(ctx) if you made one
    }

    /**
     * Fetch ALL courses from the backend.
     */
    public static void getAllCourses(
            Context ctx,
            Consumer<List<JSONObject>> onSuccess,
            Consumer<VolleyError> onError
    ) {
        String url = "http://coms-3090-019.class.las.iastate.edu:8080/courses";

        JsonArrayRequest request = new JsonArrayRequest(
                Request.Method.GET,
                url,
                null,
                response -> {
                    try {
                        List<JSONObject> result = new ArrayList<>();
                        for (int i = 0; i < response.length(); i++) {
                            result.add(response.getJSONObject(i));
                        }
                        onSuccess.accept(result);
                    } catch (Exception e) {
                        onError.accept(new VolleyError(e));
                    }
                },
                onError::accept
        );

        request.setShouldCache(false);
        RequestQueue queue = getQueue(ctx);
        queue.getCache().clear();
        queue.add(request);
    }

    /**
     * Fetch courses for a specific professor (filtered client-side).
     */
    public static void getCoursesForProfessor(
            Context ctx,
            String professorNetId,
            Consumer<List<JSONObject>> onSuccess,
            Consumer<VolleyError> onError
    ) {
        getAllCourses(ctx, allCourses -> {
            List<JSONObject> filtered = new ArrayList<>();
            for (JSONObject obj : allCourses) {
                String profId = obj.optString("professor_net_id", "");
                if (profId.equals(professorNetId)) {
                    filtered.add(obj);
                }
            }
            onSuccess.accept(filtered);
        }, onError);
    }

    /**
     * Fetches all enrolled classes for a given user.
     *
     * @param ctx Context
     * @param username User's netId
     * @param onSuccess called with List<JSONObject> of classes
     * @param onError called with VolleyError if request fails
     */
    public static void getEnrolledClasses(
            Context ctx,
            String username,
            Consumer<List<JSONObject>> onSuccess,
            Consumer<VolleyError> onError
    ) {
        String url = "http://coms-3090-019.class.las.iastate.edu:8080/users/" + username + "/enrolled";

        JsonArrayRequest req = new JsonArrayRequest(
                Request.Method.GET,
                url,
                null,
                response -> {
                    try {
                        List<JSONObject> classes = new ArrayList<>();

                        for (int i = 0; i < response.length(); i++) {
                            classes.add(response.getJSONObject(i));
                        }

                        onSuccess.accept(classes);
                    } catch (Exception e) {
                        onError.accept(new VolleyError(e));
                    }
                },
                error -> onError.accept(error)
        );

        req.setShouldCache(false);
        getQueue(ctx).getCache().clear();
        getQueue(ctx).add(req);
    }
}