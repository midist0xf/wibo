package com.example.rage.lamapp.activity;


import android.Manifest;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.telephony.CellInfo;
import android.telephony.CellInfoGsm;
import android.telephony.CellInfoLte;
import android.telephony.CellInfoWcdma;
import android.telephony.CellSignalStrengthGsm;
import android.telephony.CellSignalStrengthLte;
import android.telephony.CellSignalStrengthWcdma;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;


import com.example.rage.lamapp.R;
import com.example.rage.lamapp.coord.CoordinatesCalculator;

import com.example.rage.lamapp.draw.AreaDrawer;
import com.example.rage.lamapp.persistence.utils.infowindow.InfoAreaVisualizerSync;
import com.example.rage.lamapp.persistence.utils.populate.AreaInitializerAsync;
import com.example.rage.lamapp.persistence.database.SignalAreaDatabase;
import com.example.rage.lamapp.utils.InfoWindowData;
import com.example.rage.lamapp.utils.LocationUpdate;
import com.example.rage.lamapp.utils.WifiConnectivity;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;


import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;


public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback {

    public static GoogleMap mMap;
    private  Marker marker;

    boolean receivingPhoneStateChanges = true;
    boolean mRequestingLocationUpdates = true;
    boolean receivingWifiScanResults = true;

    private static int INTERVAL_DURATION;
    private static int SELECTED_CONNECTIVITY_TYPE = -1;
    private static int ACTIVE_CONNECTIVITY_TYPE = -1;
    private final static int MY_PERMISSION_FINE_LOCATION = 101;
    private final static int MY_PERMISSION_COARSE_LOCATION = 102;
    private final static int REQUEST_CHECK_SETTINGS = 1024;


    private FusedLocationProviderClient mFusedLocationClient;
    private LocationCallback mLocationCallback;
    private LocationRequest mLocationRequest;


    private MyPhoneStateListener psListener;
    private LocationManager locationManager;
    private ConnectivityManager connectivityManager;
    private TelephonyManager telephonyManager;
    private WifiManager wifiManager;
    public BroadcastReceiver wifiScanReceiver;

    private static int localMaxSignalStrength = 0;


    private SignalAreaDatabase db;

    private LocationUpdate locationUpdate;
    private static AreaDrawer areaDrawer;
    private WifiConnectivity wifiConnectivity;



    /* +PREFERENCES */

    private static SharedPreferences sharedPref;
    private static boolean backgroundUpdatePref;
    private static String colorMapPref;
    private static int locationIntervalUpdatePref;
    private static  int mapIntervalUpdatePref;

    /* +ACTIVITY_LIFECYCLE */

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        setTheme(R.style.AppTheme);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        /* +PREFERENCES */

        // Set default value for settings if this method has never been called in the past
        //(e.g. doesn't override user's saved settings values)
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        // Get the settings as a SharedPreferences object
        sharedPref = PreferenceManager.getDefaultSharedPreferences(this);

        // Get settings values to properly initialize the application
        backgroundUpdatePref = sharedPref.getBoolean(SettingsActivity.MainSettingsFragment.KEY_ENABLE_BACKGROUND_UPDATE, false);
        colorMapPref = sharedPref.getString(SettingsActivity.MainSettingsFragment.KEY_CHANGE_MAP_COLOR, null);
        locationIntervalUpdatePref = Integer.parseInt(sharedPref.getString(SettingsActivity.MainSettingsFragment.KEY_LOCATION_INTERVAL_UPDATE, null));
        mapIntervalUpdatePref = Integer.parseInt(sharedPref.getString(SettingsActivity.MainSettingsFragment.KEY_MAP_INTERVAL_UPDATE, null));

        // Convert from sec to ms
        INTERVAL_DURATION = locationIntervalUpdatePref*1000;



        /* +DATABASE */

        // Get database instance
        db = SignalAreaDatabase.getDatabase(this);



        /* +DRAWER */

        // Get area drawer instance
        areaDrawer = AreaDrawer.getAreaDrawer();
        // Initialize color hash map with the user's color preference
        areaDrawer.setGradientColor(colorMapPref);


        /* +LOCATION */

        locationUpdate = new LocationUpdate(this, mapIntervalUpdatePref );
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);


        /* +CONNECTIVITY */

        wifiConnectivity = new WifiConnectivity(this);
        connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        telephonyManager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);


        /* +TOOLBAR */

        Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);

        // Set the toolbar
        myToolbar.setTitle("WIBO");
        myToolbar.setSubtitle("A Connectivity Map Builder");
        setSupportActionBar(myToolbar);


        /* +PERMISSION */

        // If location permission is granted initialize Map
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            mapSync();

        } else {

            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, MY_PERMISSION_FINE_LOCATION);
        }
    }


    protected void onResume() {

        // Get last shared preferences state
        sharedPref = PreferenceManager.getDefaultSharedPreferences(this);

        // Set last saved location interval update value (unchanged if the user didn't modify it using settings)
        INTERVAL_DURATION = 1000*Integer.parseInt(sharedPref.getString(SettingsActivity.MainSettingsFragment.KEY_LOCATION_INTERVAL_UPDATE,null));

        backgroundUpdatePref = sharedPref.getBoolean(SettingsActivity.MainSettingsFragment.KEY_ENABLE_BACKGROUND_UPDATE, false);


        // If wifi receiver is not registered
        if (!receivingWifiScanResults) {

            // If wifi connection is active and the user had selected wifi map, then register wifi receiver
            if (wifiConnectivity.checkWifiConnection()  && SELECTED_CONNECTIVITY_TYPE == 3) {

                // Set wifi as active connectivity
                ACTIVE_CONNECTIVITY_TYPE = 3;

                // Register the receiver
                registerReceiver(wifiScanReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));

                // Start scan
                wifiManager.startScan();
            }
        }
        super.onResume();

        if (!mRequestingLocationUpdates) {

            // At the first onResume invokation mLocationCallback and mLocationRequest will be null
            if (mLocationCallback != null  && mLocationRequest != null) {

                mRequestingLocationUpdates = true;

                // Overwrite location request with the last update interval (the user may have changed it using settings)
                mLocationRequest = locationUpdate.createLocationRequest(mLocationRequest, INTERVAL_DURATION);
                locationUpdate.startLocationUpdates(mFusedLocationClient, mLocationCallback, mLocationRequest);
            }
        } else if(backgroundUpdatePref){
            // If the user is already receiving location updates and receives them also when the app is in background
            // just update the location request with the last location interval update value (the user may have changed it using settings)
            mLocationRequest = locationUpdate.createLocationRequest(mLocationRequest, INTERVAL_DURATION);
        }

        // If phone state listener is not registered
        if (!receivingPhoneStateChanges) {

            // Get the active connectivity type
            int connectivityType = checkCellConnectivityType();

            // OnResume the active connectivity can change
            // If active connectivity is gsm, umts or lte and the user had selected the active one then register the phone state listener
            if(connectivityType != -1 &&  SELECTED_CONNECTIVITY_TYPE == connectivityType) {

                // Set the active one as active connectivity type
                ACTIVE_CONNECTIVITY_TYPE = connectivityType;

                // Register the listener
                psListener = new MyPhoneStateListener();
                telephonyManager.listen(psListener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);
            }
        }
    }

    protected void onPause() {

        // Get the actual value of background location update preference
        sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        backgroundUpdatePref = sharedPref.getBoolean(SettingsActivity.MainSettingsFragment.KEY_ENABLE_BACKGROUND_UPDATE, false);

        // If the user call Settings Activity and if there is an open info window it must be closed
        if(marker != null)
        {
            // Close the marker (in turns call onInfoWindowClosed callback which remove the marker
            marker.hideInfoWindow();
        }


        if (receivingWifiScanResults) {

            if(backgroundUpdatePref){
                // ...keep receiving wifiscan update when the app is in background
            }
            else {
                // Unregister wifi broadcast receiver
                receivingWifiScanResults = false;
                if (wifiScanReceiver != null)
                    unregisterReceiver(wifiScanReceiver);
            }
        }

        // If the user is receiving location updates
        if (mRequestingLocationUpdates) {

            // If the user has enabled background location update...
            if (backgroundUpdatePref) {

                // ...keep receiving update when the app is in background

            }else {

                // If the user has background location update disabled
                // check if location client and location callback aren't null and stop updates
                mRequestingLocationUpdates = false;
                if (mFusedLocationClient != null && mLocationCallback != null) {
                    locationUpdate.stopLocationUpdates(mFusedLocationClient, mLocationCallback);
                }
            }
        }

        if (receivingPhoneStateChanges) {

            if(backgroundUpdatePref){
                // ...keep receiving phone state update when the app is in background
            }
            else {
                // Unregister phone state listener
                receivingPhoneStateChanges = false;
                if (psListener != null) {

                    // Unregister phone state listener
                    telephonyManager.listen(psListener, PhoneStateListener.LISTEN_NONE);
                    psListener = null;
                }
            }
        }
        super.onPause();

    }

    /* +MAP */

    // Obtain the SupportMapFragment and get notified when the map is ready to be used.
    public void mapSync() {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }


    // Manipulates the map once available.
    // This callback is triggered when the map is ready to be used.
    @Override
    public void onMapReady(GoogleMap googleMap) {

        mMap = googleMap;

        // Check if localization permission is granted
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            mMap.setMyLocationEnabled(true);

            mMap.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {
                @Override
                public View getInfoContents(Marker marker) {

                    View view = getLayoutInflater().inflate(R.layout.info_window, null);
                    view.setBackgroundColor(Color.rgb(63,81,181));

                    InfoWindowData info = (InfoWindowData)marker.getTag();

                    TextView mgrsTextView = view.findViewById(R.id.mgrs_textView);
                    TextView avgSignalTextView = view.findViewById(R.id.avg_signal_textView);
                    TextView occurencesTextView = view.findViewById(R.id.occurences_textView);

                    mgrsTextView.setText("MGRS: " + info.getMgrs());
                    avgSignalTextView.setText("Avg. Signal: " + info.getAvgSignalStrength());
                    occurencesTextView.setText("Occurrences: " + info.getOccurrences());

                    return view;
                }

                @Override
                public View getInfoWindow(Marker marker) {
                    return null;
                }
            });

            // When the user tap on a point of the map for a long time shows an info window wich contains signal information relative to that area
            mMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
                @Override
                public void onMapLongClick(LatLng latLng) {

                    CoordinatesCalculator coordinatesCalculator = new CoordinatesCalculator();

                    // Get latitude, longitude coordinates on tap
                    // Convert lat lon to mgrsten
                    String mgrs = coordinatesCalculator.toMGRS(latLng.latitude, latLng.longitude);
                    String mgrsTen = coordinatesCalculator.toMGRStenMetersArea(mgrs);

                    MapsActivity mapsActivity = MapsActivity.this;/**/

                    // If there are info for that area in the database, retrieve them and visualize a proper info window
                    new InfoAreaVisualizerSync(mapsActivity /**/,db, SELECTED_CONNECTIVITY_TYPE, latLng, mgrsTen, mMap).execute();

                }
            });

            // When the info windows  is closed delete the associated marker
            mMap.setOnInfoWindowCloseListener(new GoogleMap.OnInfoWindowCloseListener() {
                @Override
                public void onInfoWindowClose(Marker marker) {
                    marker.remove();

                }
            });



            // Create location updates request
            mLocationRequest = locationUpdate.createLocationRequest(mLocationRequest, INTERVAL_DURATION);

            // Create location updates callback
            mLocationCallback = locationUpdate.createLocationCallback(mLocationCallback, db, getApplicationContext());


            // Check if location settings are appropriated for the location request
            locationUpdate.checkLocationSettings(mLocationRequest, REQUEST_CHECK_SETTINGS, mFusedLocationClient, mLocationCallback);


            // If wifi is the active connection start wifi signal scan
            if (wifiConnectivity.checkWifiConnection()) {

                // Connected using wifi
                Toast.makeText(getApplicationContext(), "Connected using Wifi", Toast.LENGTH_SHORT).show();

                ACTIVE_CONNECTIVITY_TYPE = 3;
                SELECTED_CONNECTIVITY_TYPE = 3;

                // Set UI
                TextView connTypeTextView = findViewById(R.id.typetextView);
                connTypeTextView.setText("WIFI");

                // AsyncTask that loads Wifi signal area info on the map
                new AreaInitializerAsync(db, "wifi", mMap).execute();

                // Start wifi signal scan
                wifiConnectivity.scanWifiSignal();


            } else {

                // Another connectivity type is being used
                // Check which type of connectivity is and start the appropriate signal scan

                int connectivityType = checkCellConnectivityType();

                ACTIVE_CONNECTIVITY_TYPE = connectivityType;
                SELECTED_CONNECTIVITY_TYPE = connectivityType;

                TextView connTypeTextView = findViewById(R.id.typetextView);

                switch (ACTIVE_CONNECTIVITY_TYPE) {
                    case 0:
                        // Connected using GSM
                        connTypeTextView.setText("GSM");
                        Toast.makeText(getApplicationContext(), "Connected using GSM", Toast.LENGTH_LONG).show();

                        // AsyncTask that loads Wifi signal area info on the map
                        new AreaInitializerAsync(db, "gsm", mMap).execute();

                        break;
                    case 1:
                        // Connected using UMTS
                        connTypeTextView.setText("UMTS");
                        Toast.makeText(getApplicationContext(), "Connected using UMTS", Toast.LENGTH_LONG).show();

                        // AsyncTask that loads Wifi signal area info on the map
                        new AreaInitializerAsync(db, "wcdma", mMap).execute();

                        break;
                    case 2:
                        // Connected using LTE
                        connTypeTextView.setText("LTE");
                        Toast.makeText(getApplicationContext(), "Connected using LTE", Toast.LENGTH_LONG).show();

                        // AsyncTask that loads Wifi signal area info on the map
                        new AreaInitializerAsync(db, "lte", mMap).execute();

                        break;
                    default:
                        Toast.makeText(getApplicationContext(), "Unknown or inactive connectivity.", Toast.LENGTH_SHORT).show();

                }

                // Reinitialize signal strength variable
                localMaxSignalStrength = 0;

                // Register gsm,umts,lte signal strength changes listener
                psListener = new MyPhoneStateListener();
                telephonyManager.listen(psListener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);

            }

        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, MY_PERMISSION_FINE_LOCATION);
            }
        }
    }

    /* +PERMISSIONS_CALLBACK */

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case MY_PERMISSION_FINE_LOCATION:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        mapSync();
                    }
                } else {
                    Toast.makeText(getApplicationContext(), "This app requires location permission to be granted.", Toast.LENGTH_SHORT).show();
                    finish();
                }
                break;
            case MY_PERMISSION_COARSE_LOCATION:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        mMap.setMyLocationEnabled(true);
                    }
                } else {
                    Toast.makeText(getApplicationContext(), "This app requires location permission to be granted.", Toast.LENGTH_SHORT).show();
                    finish();
                }

                break;

        }
    }

    /* +LOCATION_SETTINGS_CALLBACK */

    /* GPS OFF  WIFI ON   -> -1 debug mode 0 run
       GPS OFF  WIFI OFF  ->  0
       GPS ON   WIFI OFF  ->  0
       GPS ON   WIFI ON   ->  -1
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Check which request we're responding to
        switch (requestCode) {
            case REQUEST_CHECK_SETTINGS:
                switch (resultCode) {
                    case MapsActivity.RESULT_OK:

                        // All required changes were successfully made.
                        // Start location requests.

                        locationUpdate.getLastKnownLocation(mFusedLocationClient);
                        locationUpdate.startLocationUpdates(mFusedLocationClient, mLocationCallback, mLocationRequest);
                        Toast.makeText(getApplicationContext(), "granted", Toast.LENGTH_LONG).show();

                        break;
                    case MapsActivity.RESULT_CANCELED:

                        Toast.makeText(getApplicationContext(), "This app requires location permission to be granted", Toast.LENGTH_LONG).show();
                        finish();

                        break;
                    default:
                        break;
                }
                break;

        }

    }



    /* MENU */

    // Inflate the menu; this adds items to the action bar if it is present.
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    // Invoked when an item in the options menu is clicked
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.connection_type:

                // Creates an alert dialog where the user can choose which connectivity technology to monitor
                final CharSequence connectionsType[] = new CharSequence[]{"GSM", "UMTS", "LTE", "WIFI"};

                AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.Theme_AppCompat_Dialog_Alert);
                builder.setTitle("Choose a wireless technology");
                builder.setItems(connectionsType, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        // Unregister active connectivity related receiver
                        unregisterProperReceiver(ACTIVE_CONNECTIVITY_TYPE);

                        if(marker != null)
                        {
                            // Close the marker (in turns call onInfoWindowClosed callback which remove the marker)
                            marker.hideInfoWindow();
                        }

                        // Clear map after removing reference to added marker
                        mMap.clear();

                        // Initialize phone state listener
                        psListener = new MyPhoneStateListener();

                        // Check which cell connectivity is active (GSM,UMTS,LTE)
                        int connectivityType = checkCellConnectivityType();

                            // The user clicked on connectionsType[which]
                            if (which == 0) {

                                // GSM (2g)
                                SELECTED_CONNECTIVITY_TYPE = 0;

                                // Set UI
                                TextView connTypeTextView = findViewById(R.id.typetextView);
                                connTypeTextView.setText("GSM");

                                // Load GSM signal area map
                                new AreaInitializerAsync(db, "gsm", mMap).execute();

                                // If the active connectivity type is the same as the one chosen by the user (GSM) register phone state receiver
                                if (connectivityType == 0) {

                                    ACTIVE_CONNECTIVITY_TYPE = 0;
                                    telephonyManager.listen(psListener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);
                                    Toast.makeText(getApplicationContext(), "Receiving GSM signal updates.", Toast.LENGTH_SHORT).show();

                                } else {
                                    Toast.makeText(getApplicationContext(), "GSM is not active.", Toast.LENGTH_SHORT).show();
                                }

                            } else if (which == 1) {

                                // UMTS (3g)
                                SELECTED_CONNECTIVITY_TYPE = 1;

                                TextView connTypeTextView = findViewById(R.id.typetextView);
                                connTypeTextView.setText("UMTS");

                                // Load UMTS signal area map
                                new AreaInitializerAsync(db, "wcdma", mMap).execute();

                                // If the active connectivity type is the same as the one chosen by the user (UMTS) register phone state receiver
                                if (connectivityType == 1) {

                                    ACTIVE_CONNECTIVITY_TYPE = 1;
                                    telephonyManager.listen(psListener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);
                                    Toast.makeText(getApplicationContext(), "Receiving UMTS signal updates.", Toast.LENGTH_SHORT).show();

                                } else {
                                    Toast.makeText(getApplicationContext(), "UMTS is not active.", Toast.LENGTH_SHORT).show();
                                }

                            } else if (which == 2) {

                                // LTE (4g)
                                SELECTED_CONNECTIVITY_TYPE = 2;

                                TextView connTypeTextView = findViewById(R.id.typetextView);
                                connTypeTextView.setText("LTE");

                                // Load LTE signal area map
                                new AreaInitializerAsync(db, "lte", mMap).execute();

                                // If the active connectivity type is the same as the one chosen by the user (LTE) register phone state receiver
                                if (connectivityType == 2) {

                                    ACTIVE_CONNECTIVITY_TYPE = 2;
                                    telephonyManager.listen(psListener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);
                                    Toast.makeText(getApplicationContext(), "Receiving LTE signal updates.", Toast.LENGTH_SHORT).show();

                                } else {
                                    Toast.makeText(getApplicationContext(), "LTE is not active.", Toast.LENGTH_SHORT).show();

                                }

                            } else if (which == 3) {
                                //WIFI
                                SELECTED_CONNECTIVITY_TYPE = 3;

                                TextView connTypeTextView = findViewById(R.id.typetextView);
                                connTypeTextView.setText("WIFI");

                                // Load Wifi signal area map
                                new AreaInitializerAsync(db, "wifi", mMap).execute();

                                // If the selected connectivity type is active register appropriate receiver
                                if (wifiConnectivity.checkWifiConnection()) {

                                    ACTIVE_CONNECTIVITY_TYPE = 3;
                                    wifiConnectivity.scanWifiSignal();
                                    Toast.makeText(getApplicationContext(), "Receiving WIFI signal updates.", Toast.LENGTH_SHORT).show();

                                } else {
                                    Toast.makeText(getApplicationContext(), "WIFI is not active.", Toast.LENGTH_SHORT).show();

                                }
                            }
                        }


                    });

                    AlertDialog alertDialog = builder.create();
                    alertDialog.show();
                    alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(-12627531));


                return true;
                case R.id.settings:

                    Intent intent = new Intent(MapsActivity.this, SettingsActivity.class);
                    startActivity(intent);
                    return true;

                default:
                    return super.onOptionsItemSelected(item);
            }
        }


    /* AUXILIARY */

    public void unregisterProperReceiver(int ACTIVE_CONNECTIVITY_TYPE){

        // Unregister proper receiver/listener
        // If active connectivity is one between gsm, umts or lte unregister the phone state listener
        if (ACTIVE_CONNECTIVITY_TYPE == 0 || ACTIVE_CONNECTIVITY_TYPE == 1 || ACTIVE_CONNECTIVITY_TYPE == 2) {

            receivingPhoneStateChanges = false;
            if (psListener != null) {
                telephonyManager.listen(psListener, PhoneStateListener.LISTEN_NONE);
                psListener = null;
            }
            // If active connectivity is wifi unregister wifi scan receiver
        } else if (ACTIVE_CONNECTIVITY_TYPE == 3) {

            if (receivingWifiScanResults) {
                receivingWifiScanResults = false;
                if (wifiScanReceiver != null)
                    unregisterReceiver(wifiScanReceiver);
            }
        }
    }

    // Check if active connectivity is lte, gsm, umts or unknown
    // -1 unknown or unregistered
    // 0 gsm(2g)
    // 1 wcdma(umts-3g)
    // 2 lte (4g)
    public int checkCellConnectivityType() {

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            for (final CellInfo info : telephonyManager.getAllCellInfo()) {

                if (info instanceof CellInfoGsm && info.isRegistered()) {
                    return 0;
                } else if (info instanceof CellInfoWcdma && info.isRegistered()) {
                    return 1;
                } else if (info instanceof CellInfoLte && info.isRegistered()){
                    return 2;
                } else {
                    return -1;
                }
            }

        } else {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, MY_PERMISSION_COARSE_LOCATION);
            }
        }
        return -1;

    }

    /* +GETTERS */

    public WifiManager getWifiManager() {
        return this.wifiManager;
    }

    public ConnectivityManager getConnectivityManager() {
        return this.connectivityManager;
    }

    public int getLocalMaxSignalStrength() {
        return this.localMaxSignalStrength;
    }

    public int getActiveConnectivityType() {
        return this.ACTIVE_CONNECTIVITY_TYPE;
    }

    public  int getSelectedConnectivityType() {
        return this.SELECTED_CONNECTIVITY_TYPE;
    }

    public  String getSelectedConnectivityTypeString( )
    {
        switch(this.SELECTED_CONNECTIVITY_TYPE){
            case 0:
                return "gsm";
            case 1:
                return "wcdma";
            case 2:
                return "lte";
            case 3:
                return "wifi";
        }
        return "unknown";
    }


    /* +SETTERS */

    public void setReceivingWifiScanResults(boolean receivingWifiScanResults) {
        this.receivingWifiScanResults = receivingWifiScanResults;
    }

    public void setLocalMaxSignalStrength(int localMaxSignalStrength) {
        this.localMaxSignalStrength = localMaxSignalStrength;
    }

    public  void setMarker(Marker marker) {
        this.marker = marker;
    }



    /* +PHONE STATE LISTENER INNER CLASS */

    public class MyPhoneStateListener extends PhoneStateListener {
        @Override
        public void onSignalStrengthsChanged(SignalStrength signalStrength)
        {
            super.onSignalStrengthsChanged(signalStrength);

            localMaxSignalStrength=0;

            for (final CellInfo info : telephonyManager.getAllCellInfo()) {

                if (info instanceof CellInfoGsm) {

                    if(info.isRegistered()) {

                        final CellSignalStrengthGsm gsm = ((CellInfoGsm) info).getCellSignalStrength();
                        if (gsm.getLevel() >= localMaxSignalStrength)
                            localMaxSignalStrength = gsm.getLevel();
                        //Toast.makeText(getApplicationContext(), "gsm rssi" + localMaxSignalStrength, Toast.LENGTH_LONG).show();
                    }

                } else if (info instanceof CellInfoWcdma) {

                    if(info.isRegistered()) {

                        final CellSignalStrengthWcdma wcdma = ((CellInfoWcdma) info).getCellSignalStrength();
                        if (wcdma.getLevel() >= localMaxSignalStrength)
                            localMaxSignalStrength = wcdma.getLevel();
                        //Toast.makeText(getApplicationContext(), "wcdma rssi" + localMaxSignalStrength, Toast.LENGTH_LONG).show();
                    }

                } else if (info instanceof CellInfoLte) {

                    if(info.isRegistered()) {

                        final CellSignalStrengthLte lte = ((CellInfoLte) info).getCellSignalStrength();
                        if (lte.getLevel() >= localMaxSignalStrength)
                            localMaxSignalStrength = lte.getLevel();
                        //Toast.makeText(getApplicationContext(), "lte rssi" + localMaxSignalStrength, Toast.LENGTH_LONG).show();
                    }

                } else {

                    Toast.makeText(getApplicationContext(), "None of the monitored cells type is registered", Toast.LENGTH_SHORT).show();

                }
            }
        }
    }











}











