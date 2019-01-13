package com.example.rage.lamapp.persistence.entity;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;
import android.support.annotation.NonNull;

// Each entity is a table in the database
// Signal area table
@Entity(tableName= "signalarea_table")
public class SignalArea {

    public String mgrsTen;

    public String connectivityType;

    public int signalStrength;

    @NonNull
    @PrimaryKey
    public Long timestamp;

    // Constructor needed to let know to Rooom how to instantiate WifiSignalArea objects
    public SignalArea( @NonNull String mgrsTen, @NonNull String connectivityType, @NonNull int signalStrength, @NonNull Long timestamp)
    {

        this.mgrsTen = mgrsTen;

        this.connectivityType = connectivityType;

        this.signalStrength = signalStrength;

        this.timestamp = timestamp;

    }

    public String getMgrsTen() {
        return this.mgrsTen;
    }

}

