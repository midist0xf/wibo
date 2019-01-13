package com.example.rage.lamapp.coord;

import android.location.Location;

public class CoordinatesCalculator {


    // Add distance in meters to latitude longitude coordinates
    //https://gis.stackexchange.com/questions/2951/algorithm-for-offsetting-a-latitude-longitude-by-some-amount-of-meters
    public  double[] addMetersToLatLon(double meters, double latitude, double longitude) {

        //Position, decimal degrees
        double lat = latitude;
        double lon = longitude;

        //Earth’s radius, sphere (meters)
        double R = 6378137;

        //offsets in meters
        double dn = meters;
        double de = meters;

        //Coordinate offsets in radians
        double dLat = dn / R;
        double dLon = de / (R * Math.cos(Math.PI * lat / 180));

        //OffsetPosition, decimal degrees
        double latO = lat + dLat * 180 / Math.PI;
        double lonO = lon + dLon * 180 / Math.PI;

        double res[] = new double[2];
        res[0] = latO;
        res[1] = lonO;
        return res;

    }


    // toMGRStenMetersArea
    // convert mgrs coordinates to mgrs coordinate with 10 m area
    public  String toMGRStenMetersArea(String mgrs) {
        // MGRS format e.g.: 4QFJ 12345 67890
        String gridSquare = mgrs.substring(0, 5);
        String easternTen = mgrs.substring(6, 10);
        String northernTen = mgrs.substring(12, 16);

        String mgrsTen = gridSquare + " " + easternTen + " " + northernTen;

        return mgrsTen;
    }


    public String toMGRS( double latitude, double longitude)
    {

        // From latitude, longitude to mgrs
        com.berico.coords.Coordinates c = new com.berico.coords.Coordinates();
        String mgrsCoord = c.mgrsFromLatLon(latitude, longitude);
        //Toast.makeText(getApplicationContext(), "mgrs " + mgrsCoord, Toast.LENGTH_LONG).show();

        return mgrsCoord;

    }

    public double[] toLatLonMgrsTen (String mgrsTen){
        // From mgrs with 10 m area to latitude, longitude
        com.berico.coords.Coordinates q = new com.berico.coords.Coordinates();
        double latLonMgrsTen[] = q.latLonFromMgrs(mgrsTen);

        return latLonMgrsTen;
    }

}
