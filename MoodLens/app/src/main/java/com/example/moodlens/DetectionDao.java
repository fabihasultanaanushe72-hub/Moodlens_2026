package com.example.moodlens;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface DetectionDao {

    @Insert
    void insert(DetectionEntity detection);

    @Query("SELECT * FROM detections ORDER BY timestamp DESC")
    List<DetectionEntity> getAll();

    @Query("SELECT * FROM detections WHERE :source = 'ALL' OR (:source = 'LIVE' AND source IN ('LIVE','TARGET')) OR (:source = 'CAMERA' AND source IN ('CAMERA','CAMERA PHOTO','PHOTO')) OR (:source = 'GALLERY' AND source = 'GALLERY') ORDER BY timestamp DESC, id DESC LIMIT :limit OFFSET :offset")
    List<DetectionEntity> getPage(String source, int limit, int offset);

    @Query("SELECT COUNT(*) FROM detections WHERE :source = 'ALL' OR (:source = 'LIVE' AND source IN ('LIVE','TARGET')) OR (:source = 'CAMERA' AND source IN ('CAMERA','CAMERA PHOTO','PHOTO')) OR (:source = 'GALLERY' AND source = 'GALLERY')")
    int countFiltered(String source);

    @Query("DELETE FROM detections")
    void clearAll();
}
