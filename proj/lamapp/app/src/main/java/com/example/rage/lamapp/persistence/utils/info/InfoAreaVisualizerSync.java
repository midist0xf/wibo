package com.example.rage.lamapp.persistence.utils.infowindow;

import android.os.AsyncTask;

import com.example.rage.lamapp.persistence.utils.data.AvgQueryType;
import com.example.rage.lamapp.utils.InfoWindowData;
import com.example.rage.lamapp.activity.MapsActivity;
import com.example.rage.lamapp.persistence.database.SignalAreaDatabase;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;


public class InfoAreaVisualizerSync extends AsyncTask<Void, Void, AvgQueryType[]> {

    private final SignalAreaDatabase DB;
    private  int selectedConnectivityType;
    private LatLng latLng;
    private String mgrsTen;
    private GoogleMap mMap;

    private MapsActivity mapsActivity; /**/
    public InfoAreaVisualizerSync(MapsActivity mapsActivity /**/, SignalAreaDatabase INSTANCE, int selectedConnectivityType, LatLng latLng, String mgrsTen, GoogleMap mMap){

        DB = INSTANCE;
        this.selectedConnectivityType= selectedConnectivityType;
        this.latLng = latLng;
        this.mgrsTen = mgrsTen;
        this.mMap = mMap;
        this.mapsActivity = mapsActivity;
    }

    @Override
    protected AvgQueryType[]  doInBackground(Void... params){

        AvgQueryType[] groupedAvgSignalStrength = null;

        if(selectedConnectivityType == 0)
        {
            groupedAvgSignalStrength = DB.signalAreaDao().getAvgSignalStrength(mgrsTen, "gsm");

        }
        else if (selectedConnectivityType == 1)
        {
            groupedAvgSignalStrength = DB.signalAreaDao().getAvgSignalStrength(mgrsTen, "wcdma");

        }
        else if(selectedConnectivityType == 2)
        {
            groupedAvgSignalStrength = DB.signalAreaDao().getAvgSignalStrength(mgrsTen, "lte");

        } else if(selectedConnectivityType == 3 )
        {
            groupedAvgSignalStrength = DB.signalAreaDao().getAvgSignalStrength(mgrsTen, "wifi");

        }

        return groupedAvgSignalStrength;

    }

    // Runs on the UI thread after doInBackground
    // Parameters passed as arguments are those returned by doInBackground
    @Override
    protected void onPostExecute(AvgQueryType[] groupedAvgSignalStrength) {


        LatLng location = new LatLng(latLng.latitude, latLng.longitude );

        if (groupedAvgSignalStrength != null && groupedAvgSignalStrength.length != 0 )
        {

            String mgrsTen = groupedAvgSignalStrength[0].mgrsTen;
            String avgSignalStrength = String.valueOf(groupedAvgSignalStrength[0].signalStrength);
            String areaOccurrences = String.valueOf(groupedAvgSignalStrength[0].areaOccurrences);

            InfoWindowData info = new InfoWindowData(mgrsTen,areaOccurrences,avgSignalStrength);

            Marker position = mMap.addMarker(new MarkerOptions()
                    .position(location));

            position.setTag(info);

            // Make the marker invisible
            position.setAlpha(0);

            position.showInfoWindow();

            mapsActivity.setMarker(position); /**/

        }
        else {

            Marker  position = mMap.addMarker(new MarkerOptions()
                    .position(location));

            position.setAlpha(0);
            position.showInfoWindow();



        }
    }

}
