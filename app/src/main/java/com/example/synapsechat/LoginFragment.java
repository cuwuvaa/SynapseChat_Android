package com.example.synapsechat;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

public class LoginFragment extends Fragment {
    private EditText etServerIp, etUsername, etPassword;
    private Button btnLogin;
    private CheckBox saveenter;

    @Nullable @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        View v = inflater.inflate(R.layout.fragment_login, container, false);

        etServerIp = v.findViewById(R.id.etServerIp);
        etUsername = v.findViewById(R.id.etUsername);
        etPassword = v.findViewById(R.id.etPassword);
        btnLogin    = v.findViewById(R.id.btnLogin);
        saveenter = v.findViewById(R.id.saveentering);

        SharedPreferences prefs = requireContext()
                .getSharedPreferences("app_prefs", Context.MODE_PRIVATE);
        etServerIp.setText(prefs.getString("server_ip", ""));
        etUsername.setText(prefs.getString("username",  ""));
        etPassword.setText(prefs.getString("password",  ""));
        saveenter.setChecked(prefs.getBoolean("savelogin", false));


        if (saveenter.isChecked()) {
            String ip   = etServerIp.getText().toString().trim();
            String user = etUsername .getText().toString().trim();
            String pass = etPassword .getText().toString();
            if (!ip.isEmpty() && !user.isEmpty() && !pass.isEmpty()) {
                new LoginTask(ip, user, pass).execute();
            }
        }

        btnLogin.setOnClickListener(ignored -> {
            String ip   = etServerIp.getText().toString().trim();
            String user = etUsername .getText().toString().trim();
            String pass = etPassword .getText().toString();
            if (ip.isEmpty() || user.isEmpty() || pass.isEmpty()) {
                Toast.makeText(getContext(),
                        "Заполните все поля", Toast.LENGTH_SHORT).show();
                return;
            }
            prefs.edit()
                    .putBoolean("savelogin", saveenter.isChecked())
                    .apply();
            prefs.edit().putBoolean("savelogin",saveenter.isChecked());
            new LoginTask(ip, user, pass).execute();
        });

        return v;
    }

    private class LoginTask extends AsyncTask<Void, Void, Boolean> {
        private final String ip, user, pass;
        private String errorMsg = null;

        LoginTask(String ip, String user, String pass) {
            this.ip   = ip;
            this.user = user;
            this.pass = pass;
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            HttpURLConnection conn = null;
            try {
                URL url = new URL("http://" + ip + "/ping"); // поправьте порт, если нужно
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");

                String creds = user + ":" + pass;
                String auth = "Basic " +
                        Base64.encodeToString(creds.getBytes("UTF-8"),
                                Base64.NO_WRAP);
                conn.setRequestProperty("Authorization", auth);
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);

                int code = conn.getResponseCode();
                if (code == HttpURLConnection.HTTP_OK) {
                    return true;
                } else if (code == HttpURLConnection.HTTP_UNAUTHORIZED) {
                    errorMsg = "Неверный логин или пароль";
                    return false;
                } else {
                    errorMsg = "Ошибка сервера: " + code;
                    return false;
                }
            } catch (IOException e) {
                errorMsg = "Сетевая ошибка: " + e.getMessage();
                return false;
            } finally {
                if (conn != null) conn.disconnect();
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            if (success) {
                // 1) Сохраняем данные в SharedPreferences
                Context ctx = requireActivity();
                SharedPreferences prefs = ctx
                        .getSharedPreferences("app_prefs", Context.MODE_PRIVATE);
                prefs.edit()
                        .putString("server_ip", ip)
                        .putString("username",  user)
                        .putString("password",  pass)
                        .apply();

                AppCompatActivity activity = (AppCompatActivity) requireActivity();
                if (activity.getSupportActionBar() != null) {
                    activity.getSupportActionBar().setTitle("Настройки");
                }
                requireActivity().getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, new SettingsFragment())
                        .addToBackStack(null)
                        .commit();
            } else {
                Toast.makeText(getContext(), errorMsg,
                        Toast.LENGTH_LONG).show();
            }
        }
    }
}
