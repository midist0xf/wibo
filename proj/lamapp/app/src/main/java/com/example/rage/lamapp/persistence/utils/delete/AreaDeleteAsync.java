package com.example.rage.lamapp.persistence.utils.delete;
import android.os.AsyncTask;

import com.example.rage.lamapp.activity.MapsActivity;
import com.example.rage.lamapp.persistence.database.SignalAreaDatabase;


public  class AreaDeleteAsync extends AsyncTask<Void, Void, Void> {

    private  SignalAreaDatabase DB;
    private String connectivityType;


    public AreaDeleteAsync(SignalAreaDatabase INSTANCE, String connectivityType){

        this.DB = INSTANCE;
        this.connectivityType = connectivityType;
    }

    @Override
    protected Void doInBackground(Void... params){

        if(connectivityType != null) {
            if (connectivityType.equals("GSM")) {

                DB.signalAreaDao().deleteAllSignalArea("gsm");

            } else if (connectivityType.equals("UMTS")) {

                DB.signalAreaDao().deleteAllSignalArea("wcdma");

            } else if (connectivityType.equals("LTE")) {

                DB.signalAreaDao().deleteAllSignalArea("lte");

            } else if (connectivityType.equals("WIFI")) {

                DB.signalAreaDao().deleteAllSignalArea("wifi");

            }
        }

        return  null;

    }

    @Override
    protected void onPostExecute(Void aVoid) {
        super.onPostExecute(aVoid);

        MapsActivity.mMap.clear();
    }
}
