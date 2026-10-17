package com.example.moodlens;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(
        entities = {DetectionEntity.class},
        version = 1,
        exportSchema = false
)
public abstract class MoodLensDatabase extends RoomDatabase {

    private static volatile MoodLensDatabase INSTANCE;

    public abstract DetectionDao detectionDao();

    public static MoodLensDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (MoodLensDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            MoodLensDatabase.class,
                            "moodlens.db"
                    ).build();
                }
            }
        }
        return INSTANCE;
    }
}
