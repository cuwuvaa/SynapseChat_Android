package com.example.synapsechat.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface LoginEntryDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insert(LoginEntry entry);

    @Query("SELECT COUNT(*) FROM login_entries WHERE ipPort = :ipPort AND username = :username AND password = :password")
    int countExact(String ipPort, String username, String password);

    @Query("SELECT * FROM login_entries ORDER BY id DESC")
    List<LoginEntry> getAllEntries();
}


