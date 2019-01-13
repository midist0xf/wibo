package com.example.rage.lamapp.utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;

import com.example.rage.lamapp.activity.MapsActivity;

import java.util.List;

public class WifiConnectivity {

    private MapsActivity mapsActivity;
    private BroadcastReceiver wifiScanReceiver;

    public WifiConnectivity(MapsActivity mapsActivity)
    {

        this.mapsActivity = mapsActivity;
    }

    // Check if wifi connection is active
   public boolean checkWifiConnection() {

        ConnectivityManager connectivityManager = mapsActivity.getConnectivityManager();

        NetworkInfo wifiCheck = connectivityManager.getActiveNetworkInfo();

        if (wifiCheck != null && wifiCheck.getType() == connectivityManager.TYPE_WIFI) {

            return true;

        } else {

            return false;
        }
    }

         public void scanWifiSignal(){

            final WifiManager wifiManager;

            wifiManager = mapsActivity.getWifiManager();

            wifiScanReceiver = new BroadcastReceiver() {

                @Override
                public void onReceive(Context context, Intent intent) {

                    mapsActivity.setLocalMaxSignalStrength(0);

                    List<ScanResult> results = wifiManager.getScanResults();

                    for (int i = 0; i < results.size(); i++) {

                        ScanResult el = results.get(i);

                        // Get signal level as a number from 0 to n-1
                        int signalLevel = WifiManager.calculateSignalLevel(el.level, 5);

                        // If signal level value is greater than the registered max, update the max value.
                        if (signalLevel >= mapsActivity.getLocalMaxSignalStrength()){
                            mapsActivity.setLocalMaxSignalStrength(signalLevel);
                        }
                    }

                }
            };
           mapsActivity.registerReceiver(wifiScanReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
           mapsActivity.setReceivingWifiScanResults(true);

           wifiManager.startScan();

        }
}
