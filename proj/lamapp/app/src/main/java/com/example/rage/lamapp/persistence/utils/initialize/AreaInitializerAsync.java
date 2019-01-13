package com.example.rage.lamapp.persistence.utils.populate;
import android.os.AsyncTask;

import com.example.rage.lamapp.coord.CoordinatesCalculator;
import com.example.rage.lamapp.draw.AreaDrawer;
import com.example.rage.lamapp.persistence.database.SignalAreaDatabase;
import com.example.rage.lamapp.persistence.utils.data.AvgQueryType;
import com.google.android.gms.maps.GoogleMap;


// Populate  wifi map
public  class AreaInitializerAsync extends AsyncTask<Void, Void, AvgQueryType[]> {

    private final SignalAreaDatabase DB;
    private  String connectivityType;
    private final GoogleMap mMap;
    private AreaDrawer areaDrawer;

    public AreaInitializerAsync(SignalAreaDatabase INSTANCE, String connectivityType, GoogleMap mMap){

        DB = INSTANCE;

        this.mMap=mMap;
        this.connectivityType=connectivityType;

    }

    @Override
    protected AvgQueryType[] doInBackground(Void... params){

        AvgQueryType[] groupedAvgSignalStrength = null;

        groupedAvgSignalStrength = DB.signalAreaDao().getGroupedAvgSignalStrength(connectivityType);

        return groupedAvgSignalStrength;

    }

    // Runs on the UI thread after doInBackground
    // Parameters passed as arguments are those returned by doInBackground
    @Override
    protected void onPostExecute(AvgQueryType[] groupedAvgSignalStrength) {

        CoordinatesCalculator coordinatesCalculator = new CoordinatesCalculator();

        areaDrawer = AreaDrawer.getAreaDrawer();


        if (groupedAvgSignalStrength.length != 0 && groupedAvgSignalStrength != null)
        {
            // Draw signal strength map
            for (int i = 0 ; i< groupedAvgSignalStrength.length ; i++)
            {
                // Get area color basing on signal strength
                String areaColor = areaDrawer.getAreaColor(groupedAvgSignalStrength[i].signalStrength);

                // Get color opacity level based on the number of records for that area
                String opacityLevel = areaDrawer.getOpacityLevel(groupedAvgSignalStrength[i].areaOccurrences);

                opacityLevel="#"+opacityLevel;
                areaColor = areaColor.replaceFirst("^#\\w\\w", opacityLevel);

                // From mgrs 10 m area coordinates to latitude longitude
                double latLonMgrsTen[] = coordinatesCalculator.toLatLonMgrsTen(groupedAvgSignalStrength[i].mgrsTen);

                // From latitude longitude to latitude plus 10m , longitude plus 10m
                double latLonTen[] = coordinatesCalculator.addMetersToLatLon(10, latLonMgrsTen[0], latLonMgrsTen[1]);

                // Draw local area square
                areaDrawer.drawLocalAreaSquare(mMap,latLonMgrsTen, latLonTen, areaColor);

            }

        }
        else {

        }
    }
}

