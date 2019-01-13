package com.example.rage.lamapp.activity;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.widget.Toolbar;


import android.support.annotation.Nullable;
import android.view.MenuItem;

import com.example.rage.lamapp.R;
import com.example.rage.lamapp.draw.AreaDrawer;
import com.example.rage.lamapp.persistence.database.SignalAreaDatabase;
import com.example.rage.lamapp.persistence.utils.delete.AreaDeleteAsync;
import com.example.rage.lamapp.persistence.utils.populate.AreaInitializerAsync;


public class SettingsActivity extends AppCompatActivity
{

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings);

        // Set the toolbar
        Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        myToolbar.setTitle("Settings");
        setSupportActionBar(myToolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }




    public  static class MainSettingsFragment extends PreferenceFragmentCompat {

        public final static String KEY_ENABLE_BACKGROUND_UPDATE = "enable_background_update";
        public final static String KEY_LOCATION_INTERVAL_UPDATE ="location_interval_update";
        public final static String KEY_MAP_INTERVAL_UPDATE = "map_interval_update";
        public final static String KEY_DELETE_DB_DATA = "delete_db_data";
        public final static String KEY_CHANGE_MAP_COLOR ="change_map_color";

        private SharedPreferences.OnSharedPreferenceChangeListener preferenceChangeListener;
        @Override
        public void onCreatePreferences(Bundle bundle, String s) {

            // Load the Preferences from the XML file
            addPreferencesFromResource(R.xml.preferences);

            preferenceChangeListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
                @Override
                public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {

                    if(key.equals(KEY_DELETE_DB_DATA))
                    {
                        String connectivityType = sharedPreferences.getString(key, null);

                        // Necessary because as sharedPreferences.edit().remove(key).edit() is called onSharedPreferenceChanged is invoked again
                        // with null argument
                        if (connectivityType != null ) {

                            new AreaDeleteAsync(SignalAreaDatabase.getDatabase(getContext()), connectivityType).execute();
                            sharedPreferences.edit().remove(key).commit();
                        }

                    } else if(key.equals(KEY_CHANGE_MAP_COLOR)){

                        // Get the chosen gradient color preference
                        String gradientColor = sharedPreferences.getString(key, null);

                        // Get area drawer instance
                        AreaDrawer areaDrawer = AreaDrawer.getAreaDrawer();

                        // Remap color hashmap
                        areaDrawer.setGradientColor(gradientColor);

                        // Clear the map
                        MapsActivity.mMap.clear();
                        MapsActivity mapsActivity = new MapsActivity();

                        // Populate the map drawing area using new color
                        new AreaInitializerAsync(SignalAreaDatabase.getDatabase(getContext()), mapsActivity.getSelectedConnectivityTypeString(), MapsActivity.mMap).execute();
                    }

                }
            };

        }

        @Override
        public void onResume() {
            super.onResume();
            getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(preferenceChangeListener);
        }

        @Override
        public void onPause() {
            super.onPause();
            getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(preferenceChangeListener);
        }
    }
}
