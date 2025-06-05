package com.example.synapsechat;

import android.content.Context;
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

import com.example.synapsechat.data.AppDatabase;
import com.example.synapsechat.data.LoginEntry;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

public class LoginFragment extends Fragment {
    private EditText etServerIp, etUsername, etPassword;
    private Button btnLogin;
    private CheckBox saveenter;

    // Экземпляр базы данных
    private AppDatabase db;

    @Nullable @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        View v = inflater.inflate(R.layout.fragment_login, container, false);
        ((MainActivity) requireActivity()).setDrawerEnabled(false);

        etServerIp = v.findViewById(R.id.etServerIp);
        etUsername = v.findViewById(R.id.etUsername);
        etPassword = v.findViewById(R.id.etPassword);
        btnLogin    = v.findViewById(R.id.btnLogin);
        saveenter   = v.findViewById(R.id.saveentering);

        // Получаем базу данных
        db = AppDatabase.getInstance(requireContext());

        SharedPreferences prefs = requireContext()
                .getSharedPreferences("app_prefs", Context.MODE_PRIVATE);
        etServerIp.setText(prefs.getString("server_ip", ""));
        etUsername.setText(prefs.getString("username", ""));
        etPassword.setText(prefs.getString("password", ""));
        saveenter.setChecked(prefs.getBoolean("savelogin", false));

        // Если включён автологин и поля не пустые — запускаем задачу
        if (saveenter.isChecked()) {
            String ipPort = etServerIp.getText().toString().trim();
            String user   = etUsername.getText().toString().trim();
            String pass   = etPassword.getText().toString();
            if (!ipPort.isEmpty() && !user.isEmpty() && !pass.isEmpty()) {
                new LoginTask(ipPort, user, pass).execute();
            }
        }

        btnLogin.setOnClickListener(ignored -> {
            String ipPort = etServerIp.getText().toString().trim();
            String user   = etUsername.getText().toString().trim();
            String pass   = etPassword.getText().toString();
            if (ipPort.isEmpty() || user.isEmpty() || pass.isEmpty()) {
                Toast.makeText(getContext(),
                        "Заполните все поля", Toast.LENGTH_SHORT).show();
                return;
            }
            // Сохраняем только один раз флаг savelogin
            prefs.edit()
                    .putBoolean("savelogin", saveenter.isChecked())
                    .apply();

            new LoginTask(ipPort, user, pass).execute();
        });

        return v;
    }

    private class LoginTask extends AsyncTask<Void, Void, Boolean> {
        private final String ipPort, user, pass;
        private String errorMsg = null;

        LoginTask(String ipPort, String user, String pass) {
            this.ipPort = ipPort;
            this.user   = user;
            this.pass   = pass;
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            HttpURLConnection conn = null;
            try {
                // Предполагаем, что ipPort уже содержит "IP:PORT"
                URL url = new URL("http://" + ipPort + "/ping");
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
                // 1) Сохраняем в SharedPreferences
                Context ctx = requireActivity();
                SharedPreferences prefs = ctx
                        .getSharedPreferences("app_prefs", Context.MODE_PRIVATE);
                prefs.edit()
                        .putString("server_ip", ipPort)
                        .putString("username",  user)
                        .putString("password",  pass)
                        .apply();

                // 2) Вставляем в базу (в фоновом потоке)
                new Thread(() -> {
                    // Метод insert вернёт -1, если запись с такими же полями уже есть
                    long newId = db.loginEntryDao().insert(
                            new LoginEntry(ipPort, user, pass)
                    );
                    // Можно при желании логировать результат:
                    // Log.d("DB", "Insert login entry result = " + newId);
                }).start();

                // 3) Переходим к экрану настроек
                AppCompatActivity activity = (AppCompatActivity) requireActivity();
                if (activity.getSupportActionBar() != null) {
                    activity.getSupportActionBar().setTitle("Настройки");
                }
                ((MainActivity) requireActivity()).setDrawerEnabled(true);
                requireActivity().getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, new SettingsFragment())
                        .commit();
            } else {
                Toast.makeText(getContext(), errorMsg,
                        Toast.LENGTH_LONG).show();
            }
        }
    }
}
