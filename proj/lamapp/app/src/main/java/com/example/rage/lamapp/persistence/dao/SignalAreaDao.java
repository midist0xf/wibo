package com.example.rage.lamapp.persistence.dao;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Query;

import com.example.rage.lamapp.persistence.entity.SignalArea;
import com.example.rage.lamapp.persistence.utils.data.AvgQueryType;

@Dao
public interface SignalAreaDao {

    // If the app tries to insert a new row and the primary key already exists, replace the old row with the new values
    // (e.g. updates signal strength of that area )

    // Insert signal area info
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertSignalArea( SignalArea signalArea);



    // Get average signal strength of an area for a particular connectivity type
    // Used to get data for info window
    @Query("SELECT ROUND(AVG(signalStrength),0) AS signalStrength, mgrsTen, COUNT(mgrsTen) " +
            "AS areaOccurrences FROM signalarea_table  WHERE mgrsTen = :mgrsTenID AND connectivityType = :connectivityTypeID")
    AvgQueryType[] getAvgSignalStrength(String mgrsTenID, String connectivityTypeID);


    // Get average signal strength grouped by mgrs coordinates for a particular connectivity type
    // Used to get data for initialize a particular connectivity type map
    @Query("SELECT ROUND(AVG(signalStrength),0) AS signalStrength, mgrsTen, COUNT(mgrsTen) " +
            "AS areaOccurrences FROM signalarea_table  WHERE connectivityType= :connectivityType GROUP BY mgrsTen")
    AvgQueryType[] getGroupedAvgSignalStrength(String connectivityType);


    // Delete all signal area info from the database
    @Query("DELETE FROM signalarea_table WHERE connectivityType= :connectivityType ")
    void deleteAllSignalArea(String connectivityType);


    // Get all area info for a particular connectivity type and a specific area
    // Used in AreaSaveAsync
    @Query("SELECT * FROM signalarea_table WHERE mgrsTen = :mgrsTenID AND connectivityType= :connectivityType")
    SignalArea[] fetchSignalArea (String mgrsTenID, String connectivityType);


    // Get the greatest signal area timestamp value for a particular connectivity type
    @Query("SELECT MAX(timestamp) FROM signalarea_table WHERE mgrsTen = :mgrsTenID AND connectivityType= :connectivityType")
    Long fetchSignalAreaMaxTimestamp (String mgrsTenID, String connectivityType);



    // Get all signal strength values for a particular connectivity type
    @Query("SELECT signalStrength FROM signalarea_table WHERE mgrsTen = :mgrsTenID AND connectivityType= :connectivityType")
    Long[] fetchSignalAreaSignalStrength (String mgrsTenID, String connectivityType);







}
