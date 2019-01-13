package com.example.rage.lamapp.draw;

import android.graphics.Color;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;

import java.util.HashMap;




public class AreaDrawer {

    private static AreaDrawer signalAreaDrawerInstance;

    private   HashMap<Integer, String> RSSIcolorHmap;

    // Exists only to defeat instantiation.
    private AreaDrawer(){}

    private void defineColorMappings(String gradientColor)
    {
        if(RSSIcolorHmap == null)
        {
            RSSIcolorHmap = new HashMap<Integer, String>();
        }

        if(gradientColor != null) {
            if (gradientColor.equals("Mixed")) {

                RSSIcolorHmap.put(1, "#ffef2828");
                RSSIcolorHmap.put(2, "#ffef8427");
                RSSIcolorHmap.put(3, "#ffefeb27");
                RSSIcolorHmap.put(4, "#ff9fef27");
                RSSIcolorHmap.put(5, "#ff17ef31");

            } else if (gradientColor.equals("Blue")) {

                RSSIcolorHmap.put(1, "#ff42f4eb");
                RSSIcolorHmap.put(2, "#ff4174f4");
                RSSIcolorHmap.put(3, "#ff414ff4");
                RSSIcolorHmap.put(4, "#ff221099");
                RSSIcolorHmap.put(5, "#ff130063");

            } else if (gradientColor.equals("Yellow")) {
                RSSIcolorHmap.put(1, "#ffffffcc");
                RSSIcolorHmap.put(2, "#ffffff99");
                RSSIcolorHmap.put(3, "#ffffff66");
                RSSIcolorHmap.put(4, "#ffffff33");
                RSSIcolorHmap.put(5, "#ffffff00");

            } else if (gradientColor.equals("Green")) {
                RSSIcolorHmap.put(1, "#ff99ff99");
                RSSIcolorHmap.put(2, "#ff66ff66");
                RSSIcolorHmap.put(3, "#ff33ff33");
                RSSIcolorHmap.put(4, "#ff00ff00");
                RSSIcolorHmap.put(5, "#ff00cc00");

            } else if (gradientColor.equals("Red")) {
                RSSIcolorHmap.put(1, "#ffffcccc");
                RSSIcolorHmap.put(2, "#ffff9999");
                RSSIcolorHmap.put(3, "#ffff6666");
                RSSIcolorHmap.put(4, "#ffff3333");
                RSSIcolorHmap.put(5, "#ffff0000");

            }
        }

    }

    public static AreaDrawer getAreaDrawer(){
        if (signalAreaDrawerInstance == null){
            synchronized (AreaDrawer.class){
                if(signalAreaDrawerInstance == null){
                    signalAreaDrawerInstance = new AreaDrawer();
                }
            }
        }
        return signalAreaDrawerInstance;
    }


    // Remaps gradient color hashmap using the color passed as argument
    public void setGradientColor( String gradientColor)
    {
        defineColorMappings(gradientColor);
    }

    // Return color associated with the signal strength value passed as argument
    public String getAreaColor(int maxSignalStrength){

        String areaColor;

        // Choose proper color from color hash map
        switch (maxSignalStrength) {
            case 0:
                areaColor = RSSIcolorHmap.get(1);
                return areaColor;
            case 1:
                areaColor = RSSIcolorHmap.get(2);
                return areaColor;
            case 2:
                areaColor = RSSIcolorHmap.get(3);
                return areaColor;
            case 3:
                areaColor = RSSIcolorHmap.get(3);
                return areaColor;
            case 4:
                areaColor = RSSIcolorHmap.get(5);
                return areaColor;
            default:
                areaColor = "#00000000";
                return areaColor;

        }

    }

    // Return opacity level associated with the number of signal measurement
    public String getOpacityLevel(int signalStrengthOccurrences)
    {

        String opacityLevel = "";
        if(signalStrengthOccurrences < 5)
        {
            // 25% opacity
            opacityLevel = "40";

        } else if(signalStrengthOccurrences > 5 && signalStrengthOccurrences <= 10)
        {
            // 50% opacity
            opacityLevel= "80";

        } else if( signalStrengthOccurrences > 10 && signalStrengthOccurrences <= 15)
        {
            // 75% opacity
            opacityLevel = "bf";

        }else
        {
            // 100% opacity
            opacityLevel = "ff";

        }

        return opacityLevel;

    }



    /* REMINDER:
    latLonMgrsTen are lat and lon value corresponding to mgrs 10 m area origin
    latLonTen are lat and lon value corre
     */
    // Draw a 10x10m square
    public  void drawLocalAreaSquare(GoogleMap mMap,  double latLonMgrsTen[], double latLonTen[], String areaColor) {

        // Creates 10 meter square area and color it based on the signal strength of that area
        Polygon polygon = mMap.addPolygon(new PolygonOptions()
                .add(new LatLng(latLonMgrsTen[0], latLonMgrsTen[1]), new LatLng(latLonTen[0], latLonMgrsTen[1]), new LatLng(latLonTen[0], latLonTen[1]), new LatLng(latLonMgrsTen[0], latLonTen[1]), new LatLng(latLonMgrsTen[0], latLonMgrsTen[1]))
                .strokeColor(Color.argb(0,0,0,0))
                .fillColor(Color.parseColor(areaColor)));

    }








}
