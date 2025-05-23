package com.example.synapsechat;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;



public class MainActivity extends AppCompatActivity{
    private DrawerLayout drawer;
    private SharedPreferences prefs;
    private ActionBarDrawerToggle toggle;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, new LoginFragment())
                .commit();


        drawer = findViewById(R.id.drawer_layout);
        toggle = new ActionBarDrawerToggle(
                this, drawer,
                R.string.drawer_open,
                R.string.drawer_close
        );


        drawer.addDrawerListener(toggle);
        toggle.syncState();
        prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);


        findViewById(R.id.img_profile).setOnClickListener(v -> {
            // перейти в профиль
            loadFragment(new SettingsFragment());
            getSupportActionBar().setTitle("Настройки");
            drawer.closeDrawer(GravityCompat.START);
        });

        findViewById(R.id.btn_logout).setOnClickListener(v -> {
            // перейти в профиль
            prefs.edit().putBoolean("savelogin", false).apply();
            loadFragment(new LoginFragment());
            getSupportActionBar().setTitle("Вход");
            drawer.closeDrawer(GravityCompat.START);
        });

        findViewById(R.id.btn_new_chat).setOnClickListener(v ->
        {
            loadFragment(new ChatFragment());
            getSupportActionBar().setTitle(prefs.getString("aimodel",""));
            drawer.closeDrawer(GravityCompat.START);
        });

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // обрабатываем клик по «гамбургеру»
        if (toggle.onOptionsItemSelected(item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    private void loadFragment(Fragment f) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, f)
                .commit();
    }

    @Override
    public void onBackPressed() {
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

}
