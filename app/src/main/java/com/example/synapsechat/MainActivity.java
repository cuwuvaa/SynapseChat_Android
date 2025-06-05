package com.example.synapsechat;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Base64;
import android.view.MenuItem;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class MainActivity extends AppCompatActivity {
    private DrawerLayout drawer;
    private ActionBarDrawerToggle toggle;
    private SharedPreferences prefs;
    private LinearLayout menuContainer;

    @SuppressLint("StaticFieldLeak")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        drawer = findViewById(R.id.drawer_layout);
        toggle = new ActionBarDrawerToggle(
                this, drawer,
                R.string.drawer_open,
                R.string.drawer_close
        );
        drawer.addDrawerListener(toggle);
        toggle.syncState();
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        menuContainer = findViewById(R.id.menu_container);

        fetchSessions();

        findViewById(R.id.img_profile).setOnClickListener(v -> {
            loadFragment(new SettingsFragment());
            getSupportActionBar().setTitle("Настройки");
            drawer.closeDrawer(GravityCompat.START);
        });

        findViewById(R.id.btn_history).setOnClickListener(v->
        {
            loadFragment(new SessionsFragment());
            getSupportActionBar().setTitle("История");
            drawer.closeDrawer(GravityCompat.START);
        });

        findViewById(R.id.btn_new_chat).setOnClickListener(v -> {

            new AsyncTask<Void, Void, String>() {
                protected String doInBackground(Void... voids) {
                    try {
                        String serverIp = prefs.getString("server_ip", "");
                        String username = prefs.getString("username", "");
                        String password = prefs.getString("password", "");

                        URL url = new URL("http://" + serverIp + "/history/sessions");
                        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                        conn.setRequestMethod("GET");
                        String auth = "Basic " + Base64.encodeToString(
                                (username + ":" + password)
                                        .getBytes(StandardCharsets.UTF_8),
                                Base64.NO_WRAP
                        );
                        conn.setRequestProperty("Authorization", auth);

                        if (conn.getResponseCode() != 200) {
                            conn.disconnect();
                            return null;
                        }

                        BufferedReader br = new BufferedReader(
                                new InputStreamReader(
                                        conn.getInputStream(),
                                        StandardCharsets.UTF_8
                                )
                        );
                        String json = br.lines().collect(Collectors.joining());
                        br.close();
                        conn.disconnect();

                        JSONArray arr = new JSONArray(json);
                        int max = 0;
                        for (int i = 0; i < arr.length(); i++) {
                            String sid = arr.getJSONObject(i).getString("session_id");
                            if (sid.startsWith(username)) {
                                String num = sid.substring(username.length());
                                if (num.matches("\\d+")) {
                                    int n = Integer.parseInt(num);
                                    if (n > max) max = n;
                                }
                            }
                        }
                        return username + (max + 1);
                    } catch (Exception e) {
                        e.printStackTrace();
                        return null;
                    }
                }

                @Override
                protected void onPostExecute(String newSessionId) {
                    if (newSessionId == null) {
                        Toast.makeText(
                                MainActivity.this,
                                "Не удалось получить список сессий",
                                Toast.LENGTH_SHORT
                        ).show();
                        return;
                    }

                    prefs.edit()
                            .putInt("chat_counter",
                                    Integer.parseInt(newSessionId.substring(
                                            prefs.getString("username", "").length())))
                            .apply();

                    openChat(newSessionId);
                    getSupportActionBar().setTitle("Чат: " + newSessionId);
                    drawer.closeDrawer(GravityCompat.START);

                    fetchSessions();
                }
            }.execute();
        });

        if (getSupportFragmentManager().getBackStackEntryCount() == 0) {
            loadFragment(new LoginFragment());
            getSupportActionBar().setTitle("Вход");
        }
    }

    public void setDrawerEnabled(boolean enabled) {
        if (enabled) {
            drawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
            toggle.setDrawerIndicatorEnabled(true);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            fetchSessions();
        } else {
            drawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
            toggle.setDrawerIndicatorEnabled(false);
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (toggle.onOptionsItemSelected(item)) return true;
        return super.onOptionsItemSelected(item);
    }

    private void loadFragment(Fragment f) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, f)
                .commit();
    }

    private void openChat(String sessionId) {
        ChatFragment f = ChatFragment.newInstance(sessionId);
        loadFragment(f);
    }

    @SuppressLint("StaticFieldLeak")
    private void fetchSessions() {
        new AsyncTask<Void, Void, List<String>>() {
            @Override
            protected List<String> doInBackground(Void... voids) {
                List<String> sessions = new ArrayList<>();
                try {
                    String serverIp = prefs.getString("server_ip", "");
                    String username = prefs.getString("username", "");
                    String password = prefs.getString("password", "");

                    URL url = new URL("http://" + serverIp + "/history/sessions");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    String auth = "Basic " + Base64.encodeToString(
                            (username + ":" + password)
                                    .getBytes(StandardCharsets.UTF_8),
                            Base64.NO_WRAP
                    );
                    conn.setRequestProperty("Authorization", auth);

                    if (conn.getResponseCode() != 200) {
                        conn.disconnect();
                        return sessions;
                    }

                    BufferedReader br = new BufferedReader(
                            new InputStreamReader(
                                    conn.getInputStream(),
                                    StandardCharsets.UTF_8
                            )
                    );
                    String json = br.lines().collect(Collectors.joining());
                    br.close();
                    conn.disconnect();

                    JSONArray arr = new JSONArray(json);
                    for (int i = 0; i < arr.length(); i++) {
                        String sid = arr.getJSONObject(i).getString("session_id");
                        if (sid.startsWith(username)) {
                            String num = sid.substring(username.length());
                            if (num.matches("\\d+")) sessions.add(sid);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return sessions;
            }

            @Override
            protected void onPostExecute(List<String> sessions) {
                menuContainer.removeAllViews();
                for (String sid : sessions) {
                    TextView tv = new TextView(MainActivity.this);
                    tv.setText(sid);
                    tv.setTextSize(16);
                    tv.setPadding(24, 16, 24, 16);
                    tv.setClickable(true);
                    tv.setBackgroundResource(android.R.drawable.list_selector_background);
                    tv.setOnClickListener(v -> {
                        openChat(sid);
                        getSupportActionBar().setTitle("Чат: " + sid);
                        drawer.closeDrawer(GravityCompat.START);
                    });
                    menuContainer.addView(tv);
                }
            }
        }.execute();
    }
}
