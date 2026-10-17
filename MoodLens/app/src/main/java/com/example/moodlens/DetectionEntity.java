package com.example.moodlens;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "detections")
public class DetectionEntity {

    @PrimaryKey(autoGenerate = true)
    public long id;

    public String emotion;
    public float confidence;
    public long timestamp;
    public String source;
    public boolean targetMatched;

    public DetectionEntity(
            String emotion,
            float confidence,
            long timestamp,
            String source,
            boolean targetMatched
    ) {
        this.emotion = emotion;
        this.confidence = confidence;
        this.timestamp = timestamp;
        this.source = source;
        this.targetMatched = targetMatched;
    }
}
