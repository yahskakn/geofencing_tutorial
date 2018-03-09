package com.tinmegali.mylocation;
import android.os.Bundle;
import com.android.volley.Cache;
import com.android.volley.Network;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.BasicNetwork;
import com.android.volley.toolbox.DiskBasedCache;
import com.android.volley.toolbox.HurlStack;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.NoCache;
import com.android.volley.toolbox.StringRequest;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;


/**
 * Created by akankanh on 3/8/18.
 */

class Rest {
    private static final Rest ourInstance = new Rest();

    private RequestQueue mRequestQueue;

    static Rest getInstance() {
        return ourInstance;
    }

    private Rest() {
        // Set up the network to use HttpURLConnection as the HTTP client.
        Network network = new BasicNetwork(new HurlStack());

        // Instantiate the RequestQueue with the cache and network.
        mRequestQueue = new RequestQueue(new NoCache(), network);

        // Start the queue
        mRequestQueue.start();
    }

    void fetch(final FetchReply callback) {
        String url = "http://159.65.107.246/fetch";

        JsonArrayRequest stringRequest = new JsonArrayRequest(Request.Method.GET, url, null,
                new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray response) {
                        // Do something with the response
                        Log.d("skata", "got response! yaya!! " + response);

                        ArrayList<OccupancyDatabase> hList = new ArrayList<OccupancyDatabase>();
                        try {
                            for (int i = 0; i < response.length(); i++) {

                                JSONObject json_geoFenceID = response.getJSONObject(i);
                                String id = json_geoFenceID.getString("id");
                                Log.d("ID IS " + id, "onResponse: ");

                                int max = json_geoFenceID.getInt("max");
                                Log.d("MAX IS " + max, "onResponse: ");

                                int current = json_geoFenceID.getInt("current");
                                Log.d("CURRENT IS " + current, "onResponse: ");

                                double lat = json_geoFenceID.getDouble("latitude");
                                Log.d("LAT IS " + lat, "onResponse: ");

                                double longitude = json_geoFenceID.getDouble("longitude");
                                Log.d("LONG IS " + longitude, "onResponse: ");

                                double rad = json_geoFenceID.getDouble("radius");
                                Log.d("RAD IS " + rad, "onResponse: ");


                                hList.add(new OccupancyDatabase(id, max, current, lat, longitude, rad));

                            }
                            callback.onReply(true, hList);
                        } catch (JSONException e) {
                            callback.onReply(false, null);
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // Handle error
                        callback.onReply(false,null);
                        Log.d("skata", "no response! booo!!");
                    }
                });
        mRequestQueue.add(stringRequest);
    }
}