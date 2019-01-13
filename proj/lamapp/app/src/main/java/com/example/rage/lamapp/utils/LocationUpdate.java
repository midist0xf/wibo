package com.example.rage.lamapp.utils;

import android.content.Context;
import android.content.IntentSender;
import android.location.Location;
import android.support.annotation.NonNull;
import android.widget.Toast;

import com.example.rage.lamapp.activity.MapsActivity;
import com.example.rage.lamapp.coord.CoordinatesCalculator;
import com.example.rage.lamapp.persistence.database.SignalAreaDatabase;
import com.example.rage.lamapp.persistence.entity.SignalArea;
import com.example.rage.lamapp.persistence.utils.save.AreaSaveAsync;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;


public class LocationUpdate {

    private MapsActivity mapsActivity;
    private int mapIntervalUpdatePref;


    public LocationUpdate(MapsActivity mapsActivity, int mapIntervalUpdatePref)
    {
        this.mapsActivity=mapsActivity;
        this.mapIntervalUpdatePref = mapIntervalUpdatePref;
    }


    //createLocationRequest
    public LocationRequest createLocationRequest(LocationRequest mLocationRequest, int INTERVAL_DURATION) {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(INTERVAL_DURATION);
        mLocationRequest.setFastestInterval(INTERVAL_DURATION);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        return mLocationRequest;
    }



    // Callback for location updates of fused provider client
    public LocationCallback createLocationCallback(LocationCallback mLocationCallback,  final SignalAreaDatabase db, final Context context) {
        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                for (Location location : locationResult.getLocations()) {

                    //Toast.makeText(mapsActivity.getApplicationContext(),"locationcallback", Toast.LENGTH_LONG).show();
                    CoordinatesCalculator coordinatesCalculator= new CoordinatesCalculator();

                    String mgrsTen;
                    String mgrsCoord;

                    // From latitude, longitude to mgrs coordinates
                    mgrsCoord = coordinatesCalculator.toMGRS(location.getLatitude(), location.getLongitude());

                    // From mgrs to mgrs 10m area
                    mgrsTen= coordinatesCalculator.toMGRStenMetersArea(mgrsCoord);

                    // Get current timestamp
                    Long timestamp = System.currentTimeMillis()/1000;


                    int activeConnectivityType = mapsActivity.getActiveConnectivityType();
                    int selectedConnectivityType = mapsActivity.getSelectedConnectivityType();

                    // If the connectivity Map's type the user is visualizing is the same as the active connectivity type
                    // update the database
                    if (activeConnectivityType == selectedConnectivityType)
                    {
                        // Create the proper object and save it accordingly to the connectivity type
                        switch (activeConnectivityType)
                        {
                            case 0:
                                // GSM
                                SignalArea gsmSignalArea = new SignalArea(mgrsTen, "gsm", mapsActivity.getLocalMaxSignalStrength(), timestamp);
                                new AreaSaveAsync(db, mapsActivity, mapIntervalUpdatePref).execute(gsmSignalArea);
                                break;
                            case 1:
                                // UMTS(3g)
                                SignalArea wcdmaSignalArea = new SignalArea(mgrsTen, "wcdma", mapsActivity.getLocalMaxSignalStrength(), timestamp);
                                new AreaSaveAsync(db, mapsActivity, mapIntervalUpdatePref).execute(wcdmaSignalArea);
                                break;
                            case 2:
                                //  LTE(4g)
                                SignalArea lteSignalArea = new SignalArea(mgrsTen, "lte", mapsActivity.getLocalMaxSignalStrength(), timestamp);
                                new AreaSaveAsync(db, mapsActivity, mapIntervalUpdatePref).execute(lteSignalArea);
                                break;
                            case 3:
                                // WIFI
                                SignalArea wifiSignalArea = new SignalArea( mgrsTen, "wifi", mapsActivity.getLocalMaxSignalStrength(), timestamp);
                               new AreaSaveAsync(db, mapsActivity, mapIntervalUpdatePref).execute(wifiSignalArea);
                                break;
                            default:
                                // TODO gsm unrecognized

                        }

                    }

                }
            }
        };

        return mLocationCallback;
    }



    //startLocationUpdates
    public void startLocationUpdates(FusedLocationProviderClient mFusedLocationClient, LocationCallback mLocationCallback, LocationRequest mLocationRequest) {
        mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback,null  );
    }


    //stopLocationUpdates
    public void stopLocationUpdates(FusedLocationProviderClient mFusedLocationClient, LocationCallback mLocationCallback) {
        mFusedLocationClient.removeLocationUpdates(mLocationCallback);
    }


    //getLastKnownLocation
    // Get last known location
    public void getLastKnownLocation(FusedLocationProviderClient mFusedLocationClient) {
        mFusedLocationClient.getLastLocation()
                .addOnSuccessListener(mapsActivity, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        // Got last known location. In some rare situations this can be null.
                        if (location != null) {

                            double latitude = location.getLatitude();
                            double longitude = location.getLongitude();

                            setCurrentLocation(latitude, longitude);
                        }
                    }
                });
    }





    //setCurrentLocation
    public void setCurrentLocation(double latitude, double longitude){
        // Add a marker  and move the camera
        LatLng mCurrentLocation = new LatLng(latitude, longitude);
        MapsActivity.mMap.addMarker(new MarkerOptions().position(mCurrentLocation).title("Current position"));
        MapsActivity.mMap.moveCamera(CameraUpdateFactory.newLatLng(mCurrentLocation));

        // Set zoom level
        MapsActivity.mMap.animateCamera(CameraUpdateFactory.zoomTo(19.0f));
        //Toast.makeText(getApplicationContext(), "value is " + latitude + "poi" + longitude, Toast.LENGTH_SHORT).show();

    }




    //checkLocationSettings

    public void checkLocationSettings(final LocationRequest mLocationRequest, final int REQUEST_CHECK_SETTINGS,  final FusedLocationProviderClient mFusedLocationClient, final LocationCallback mLocationCallback) {

        // Get and check location services settings

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(mLocationRequest)
                .setAlwaysShow(true);


        SettingsClient client = LocationServices.getSettingsClient(mapsActivity);
        Task<LocationSettingsResponse> task = client.checkLocationSettings(builder.build());

        task.addOnSuccessListener(mapsActivity, new OnSuccessListener<LocationSettingsResponse>() {
            @Override
            public void onSuccess(LocationSettingsResponse locationSettingsResponse) {

                // All location settings are satisfied. The client can initialize
                // location requests
                getLastKnownLocation(mFusedLocationClient);
                startLocationUpdates(mFusedLocationClient, mLocationCallback, mLocationRequest);

            }
        });

        task.addOnFailureListener(mapsActivity, new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                if (e instanceof ResolvableApiException) {
                    // Location settings are not satisfied, but this can be fixed
                    // by showing the user a dialog.
                    try {
                        // Show the dialog by calling startResolutionForResult(),
                        // and check the result in onActivityResult().
                        ResolvableApiException resolvable = (ResolvableApiException) e;
                        resolvable.startResolutionForResult(mapsActivity,
                                REQUEST_CHECK_SETTINGS);
                    } catch (IntentSender.SendIntentException sendEx) {
                        // Ignore the error.
                    }
                }
            }
        });


    }



}
