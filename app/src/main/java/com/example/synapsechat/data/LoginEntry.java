package com.example.synapsechat.data;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "login_entries",
        indices = {@Index(value = {"ipPort", "username", "password"}, unique = true)})
public class LoginEntry {

    @PrimaryKey(autoGenerate = true)
    private long id;

    private String ipPort;
    private String username;
    private String password;

    public LoginEntry(String ipPort, String username, String password) {
        this.ipPort = ipPort;
        this.username = username;
        this.password = password;
    }

    // Геттеры и сеттеры
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getIpPort() {
        return ipPort;
    }

    public void setIpPort(String ipPort) {
        this.ipPort = ipPort;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
