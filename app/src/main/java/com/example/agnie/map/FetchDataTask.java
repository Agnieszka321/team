package com.example.agnie.map;

import android.os.AsyncTask;
import android.util.Log;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by olety on 5/30/17.
 */

public class FetchDataTask extends AsyncTask<String, Void, JSONArray> {

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }

    @Override
    protected JSONArray doInBackground(String... params) {
        // Init the connection
        URL urlCould;
        HttpURLConnection connection;
        InputStream inputStream = null;
        try {
            String url = params[0];
            urlCould = new URL(url);
            connection = (HttpURLConnection) urlCould.openConnection();
            connection.setConnectTimeout(3000000);
            connection.setReadTimeout(3000000);
            connection.setRequestMethod("GET");
            System.out.println(urlCould);
            connection.setConnectTimeout(30000);
            connection.setReadTimeout(30000);
            connection.setRequestMethod("GET");
            connection.connect();

            inputStream = connection.getInputStream();

        } catch (MalformedURLException MEx){

        } catch (IOException IOEx){
            IOEx.printStackTrace();
            Log.e("Utils", "HTTP failed to fetch data");
            return null;
        }
        // Get the res. string
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        StringBuilder sb = new StringBuilder();
        String line;
        try {
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                inputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        // Convert the res. string to json
        JSONArray json = null;
        try {
            json = new JSONArray(sb.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return json;
    }

    protected void onPostExecute(String string) {
    }

}
