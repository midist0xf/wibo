package com.example.rage.lamapp.persistence.utils.save;

import android.os.AsyncTask;

import com.example.rage.lamapp.activity.MapsActivity;
import com.example.rage.lamapp.coord.CoordinatesCalculator;
import com.example.rage.lamapp.draw.AreaDrawer;
import com.example.rage.lamapp.persistence.database.SignalAreaDatabase;
import com.example.rage.lamapp.persistence.entity.SignalArea;
import com.example.rage.lamapp.persistence.utils.data.SaveType;


// Save SignalArea info in the database and possibly update the map
public  class AreaSaveAsync extends AsyncTask<SignalArea, Void, SaveType> {

    private final SignalAreaDatabase DB;
    private MapsActivity mapsActivity;
    private int mapIntervalUpdatePref;

    public AreaSaveAsync(SignalAreaDatabase INSTANCE , MapsActivity mapsActivity, int mapIntervalUpdatePref ){

        DB = INSTANCE;
        this.mapsActivity = mapsActivity;
        this.mapIntervalUpdatePref = mapIntervalUpdatePref;

    }


    @Override
    protected SaveType doInBackground(SignalArea... params) {

        SignalArea[] resSignalArea = null;
        Long maxTs = null;
        Long[] resSs = null;
        String mgrsTen;
        String connectivityType;

        SaveType res = new SaveType();

        // Get connectivity type of the measurement
        connectivityType = params[0].connectivityType;

        // Get mgrs 10 m area coordinates
        mgrsTen = params[0].mgrsTen;

        // Get all saved measures of the same area from the database
        resSignalArea = DB.signalAreaDao().fetchSignalArea(mgrsTen, connectivityType);


        // If there are already some measurements for that area
        if (resSignalArea.length != 0) {

            // Get the timestamp of the last measure (e.g. greatest timestamp)
            maxTs = DB.signalAreaDao().fetchSignalAreaMaxTimestamp(mgrsTen, connectivityType);

            // If the difference between actual measure timestamp and the last saved measure timestamp
            // is greater than the map update interval chosen by the user then update the map and the database
            if ( (params[0].timestamp - maxTs) >= mapIntervalUpdatePref/*(1)*/ ) {

                // Get all signal strength saved for that area from the database
                resSs = DB.signalAreaDao().fetchSignalAreaSignalStrength(mgrsTen, connectivityType);

                // Compute signal strength integer average for that area
                int j;
                int sum = 0;
                int average = 0;

                for (j = 0; j < resSs.length; j++) {
                    sum += resSs[j];
                }

                sum += params[0].signalStrength;
                average = sum / (resSs.length + 1);

                // Initialize result data for the object that will
                // be passed to postExecute method
                res.updateMap = true;
                res.mgrsTen = mgrsTen;
                res.avgSignalStrength = average;
                res.areaOccurrences = resSs.length + 1;

                // Save new signal area in the database
                DB.signalAreaDao().insertSignalArea(params[0]);

                mapsActivity.setLocalMaxSignalStrength(0);


                return res;

            }

            mapsActivity.setLocalMaxSignalStrength(0);

            // If the timestamp constraint is not satisfied just save the new signal area in the database
            DB.signalAreaDao().insertSignalArea(params[0]);


        } else
        {
            // There aren't measurements for the same area
            // Save and draw the first area

            // Save new signal area in the database
            DB.signalAreaDao().insertSignalArea(params[0]);

            res.updateMap = true;
            res.mgrsTen = mgrsTen;
            res.avgSignalStrength = params[0].signalStrength;

            // It will be the first measure of that area
            res.areaOccurrences = 1;

            mapsActivity.setLocalMaxSignalStrength(0);

            return res;

        }


        return null;
    }


        // Runs on the UI thread after doInBackground. Parameters passed as arguments are those returned by doInBackground
        @Override
        protected void onPostExecute(SaveType res){

            double latLonMgrsTen[];
            double latLonTen[];

            AreaDrawer signalAreaDrawer =  AreaDrawer.getAreaDrawer();
            CoordinatesCalculator coordinatesCalculator = new CoordinatesCalculator();

            // Draw the area only if the timestamp constraint is satisfied or it is the first measure occurrence for that area
            if (res != null && res.updateMap)
            {

                // Get area color based on the average signal strength for that area
                String areaColor = signalAreaDrawer.getAreaColor(res.avgSignalStrength);

                // Get color opacity level based on the number of records for that area
                String opacityLevel = signalAreaDrawer.getOpacityLevel(res.areaOccurrences);

                // Set color opacity level
                opacityLevel="#"+opacityLevel;
                areaColor = areaColor.replaceFirst("^#\\w\\w", opacityLevel);

                latLonMgrsTen = coordinatesCalculator.toLatLonMgrsTen(res.mgrsTen);

                latLonTen = coordinatesCalculator.addMetersToLatLon(10, latLonMgrsTen[0],latLonMgrsTen[1]);

                // Draw square area
                signalAreaDrawer.drawLocalAreaSquare(MapsActivity.mMap, latLonMgrsTen, latLonTen, "#00000000");
                signalAreaDrawer.drawLocalAreaSquare(MapsActivity.mMap, latLonMgrsTen, latLonTen, areaColor);


            }
        }


}
