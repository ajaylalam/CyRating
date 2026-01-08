package com.example.androidexample.utils;

import android.content.Context;

import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.VolleyError;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class AssignmentApi {

    // You can also reuse your NetworkUtils.getQueue(ctx) here if you made it
    private static RequestQueue getQueue(Context ctx) {
        return com.android.volley.toolbox.Volley.newRequestQueue(ctx.getApplicationContext());
    }

    public static void getAssignmentsForCourse(
            Context ctx,
            String courseId,
            Consumer<List<JSONObject>> onSuccess,
            Consumer<VolleyError> onError
    ) {
        String url = "http://coms-3090-019.class.las.iastate.edu:8080/api/assignments/course/" + courseId;

        JsonArrayRequest request = new JsonArrayRequest(
                Request.Method.GET,
                url,
                null,
                response -> {
                    try {
                        List<JSONObject> result = new ArrayList<>();
                        for (int i = 0; i < response.length(); i++) {
                            JSONObject obj = response.getJSONObject(i);
                            result.add(obj);
                        }
                        onSuccess.accept(result);
                    } catch (Exception e) {
                        // wrap parsing exception as VolleyError-ish via onError
                        onError.accept(new VolleyError(e));
                    }
                },
                error -> {
                    onError.accept(error);
                }
        );

        request.setShouldCache(false);
        RequestQueue queue = getQueue(ctx);
        queue.getCache().clear();
        queue.add(request);
    }
}