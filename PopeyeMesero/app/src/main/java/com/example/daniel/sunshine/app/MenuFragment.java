package com.example.daniel.sunshine.app;

/**
 * Created by Daniel on 5/9/2015.
 */

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.example.daniel.sunshine.app.db.Category;
import com.example.daniel.sunshine.app.db.Product;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class MenuFragment extends Fragment {
    private ArrayAdapter<String> mMenuadapters;
    private ArrayList<Category> categories = new ArrayList<>();
    public MenuFragment() {
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Add this line in order for this fragment to handle menu events
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menufragment, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_refresh){
            updateWeather();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void updateWeather(){
        MenuTask weatherTask = new MenuTask();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        String location = prefs.getString(getString(R.string.pref_location_key), getString(R.string.pref_location_default));
        weatherTask.execute(location);
    }

    @Override
    public void onStart() {
        super.onStart();
        updateWeather();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);
        this.mMenuadapters= new ArrayAdapter<String>(this.getActivity(),R.layout.list_item_menu,R.id.list_item_menu_textview,new ArrayList<String>());
        ListView listView = (ListView) rootView.findViewById(R.id.listview_menu);
        listView.setAdapter(this.mMenuadapters);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener(){
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String menu = mMenuadapters.getItem(position);
                Intent intent = new Intent(getActivity(), DetailActivity.class)
                        .putExtra(Intent.EXTRA_TEXT, menu);
                startActivity(intent);
            }
        });
        return rootView;

    }

    public class MenuTask extends AsyncTask<String,Void,String[]>{
        private final String LOG_TAG = MenuTask.class.getSimpleName();

        /**
         * Take the String representing the complete forecast in JSON Format and
         * pull out the data we need to construct the Strings needed for the wireframes.
         *
         * Fortunately parsing is easy:  constructor takes the JSON string and converts it
         * into an Object hierarchy for us.
         */
        private String[] getMenuDataFromJson(String menuJsonStr)
                throws JSONException {

            ArrayList<Product> products = new ArrayList<>();
            JSONArray menuJson = new JSONArray(menuJsonStr);
            JSONObject temp;
            int category=0;
            for (int i=0; i<menuJson.length(); ++i){
                temp=menuJson.getJSONObject(i);
                for(int j=0; j< categories.size(); ++j){
                    if (temp.getInt("cid")==categories.get(j).getId()) {
                        category = j;
                        break;
                    }
                }
                products.add(new Product(temp.getInt("id"),temp.getString("Name"),temp.getString("Description")
                        , temp.getDouble("Price"), categories.get(category)
                        ,temp.getJSONObject("Image").getString("url")));
            }
            for (Product t : products){
                Log.v(LOG_TAG,"Here is a product:"+t.toString());
            }
            return null;

        }
        @Override
        protected String[] doInBackground(String... params) {
            if (params.length == 0){
                return null;
            }
            // These two need to be declared outside the try/catch
            // so that they can be closed in the finally block.
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;

            // Will contain the raw JSON response as a string.
            String menuJsonStr = null;


            try {


                Uri builUri = Uri.parse("https://apimesero.herokuapp.com/products").buildUpon()
                              .build();
                URL url = new URL(builUri.toString());

                Log.v(LOG_TAG, "Built Products URI "+ builUri.toString());
                // Create the request to OpenWeatherMap, and open the connection
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                // Read the input stream into a String
                InputStream inputStream = urlConnection.getInputStream();
                StringBuffer buffer = new StringBuffer();
                if (inputStream == null) {
                    // Nothing to do.
                    return null;
                }
                reader = new BufferedReader(new InputStreamReader(inputStream));

                String line;
                while ((line = reader.readLine()) != null) {
                    // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                    // But it does make debugging a *lot* easier if you print out the completed
                    // buffer for debugging.
                    buffer.append(line + "\n");
                }

                if (buffer.length() == 0) {
                    // Stream was empty.  No point in parsing.
                    return null;
                }
                menuJsonStr = buffer.toString();
            } catch (IOException e) {
                Log.e(this.LOG_TAG, "Error "+ e);
                // If the code didn't successfully get the weather data, there's no point in attemping
                // to parse it.
                return null;
            } finally{
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                        Log.e(this.LOG_TAG, "Error closing stream", e);
                    }
                }
            }
            Log.v(LOG_TAG,"Here is the Json: "+ menuJsonStr);
            try{
                fillCategories();
                return getMenuDataFromJson(menuJsonStr);
            } catch (JSONException e){
                Log.e(LOG_TAG, e.getMessage(),e);
                e.printStackTrace();
            }
            return null;//Si hubo un problema, retornar null
        }

        public void fillCategories(){
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;

            // Will contain the raw JSON response as a string.
            String menuJsonStr = null;
            try {
                Uri builUri = Uri.parse("https://apimesero.herokuapp.com/categories").buildUpon()
                        .build();
                URL url = new URL(builUri.toString());

                Log.v(LOG_TAG, "Built Category URI "+ builUri.toString());
                // Create the request to OpenWeatherMap, and open the connection
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                // Read the input stream into a String
                InputStream inputStream = urlConnection.getInputStream();
                StringBuffer buffer = new StringBuffer();
                if (inputStream == null) {
                    // Nothing to do.
                    return ;
                }
                reader = new BufferedReader(new InputStreamReader(inputStream));

                String line;
                while ((line = reader.readLine()) != null) {
                    // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                    // But it does make debugging a *lot* easier if you print out the completed
                    // buffer for debugging.
                    buffer.append(line + "\n");
                }

                if (buffer.length() == 0) {
                    // Stream was empty.  No point in parsing.
                    return ;
                }
                menuJsonStr = buffer.toString();
                JSONArray categoryJson = new JSONArray(menuJsonStr);
                JSONObject temp;
                for (int i=0; i<categoryJson.length(); ++i){
                    temp=categoryJson.getJSONObject(i);
                    categories.add(new Category(temp.getInt("id"), temp.getString("Name")));
                }
                for (Category t : categories){
                    Log.v(LOG_TAG,"Here is a category:"+t.toString());
                }
            }catch (Exception e){
                    Log.e(LOG_TAG,"Error while trying to create categories: "+e);
            }

        }

        @Override
        protected void onPostExecute(String[] result) {
            if (result != null){
                mMenuadapters.clear();
                for (String menuStr : result){
                    mMenuadapters.add(menuStr);
                }
                //Se añadió data exitosamente
            }
        }
    }
}




