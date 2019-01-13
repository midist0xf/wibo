package com.example.rage.lamapp.persistence.database;

import android.arch.persistence.room.Database;
import android.arch.persistence.room.Room;
import android.arch.persistence.room.RoomDatabase;
import android.content.Context;

import com.example.rage.lamapp.persistence.dao.SignalAreaDao;
import com.example.rage.lamapp.persistence.entity.SignalArea;

@Database(entities = {SignalArea.class}, version = 1)
public abstract class SignalAreaDatabase extends RoomDatabase {

    public abstract SignalAreaDao signalAreaDao();

    private static volatile   SignalAreaDatabase INSTANCE;

    public static SignalAreaDatabase getDatabase( final Context context){
        if (INSTANCE == null){
            synchronized (SignalAreaDatabase.class){
                if(INSTANCE == null){
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                            SignalAreaDatabase.class, "signalarea_database").build();
                }
            }
        }
        return INSTANCE;
    }

}
