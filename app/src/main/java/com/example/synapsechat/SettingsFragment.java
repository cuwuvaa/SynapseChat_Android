package com.example.synapsechat;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

public class SettingsFragment extends Fragment {
    private Spinner spinnerModels, spinnerAvailableModels;
    private SharedPreferences prefs;
    private TextView installInfo;
    private Button installButton;
    private String serverIp, username, password, modelToInstall;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs    = requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE);
        serverIp = prefs.getString("server_ip", "");
        username = prefs.getString("username", "");
        password = prefs.getString("password", "");
    }

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(
                R.layout.fragment_settings, container, false
        );
        installInfo = root.findViewById(R.id.installinfo);
        installButton = root.findViewById(R.id.installbutton);
        installButton.setOnClickListener(v -> {
            sendInstallRequest();
        });

        spinnerModels = root.findViewById(R.id.spinnerModels);
        spinnerAvailableModels = root.findViewById(R.id.spinnerAvailableModels);
        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        fetchInstalledModels("installed",spinnerModels);
        fetchInstalledModels("available",spinnerAvailableModels);
    }

    private void sendInstallRequest()
    {
        new AsyncTask<Void, Void, Boolean>() {
            String errorMsg;

            @Override
            protected Boolean doInBackground(Void... voids) {
                HttpURLConnection conn = null;
                try {
                    // Кодируем имя модели, чтобы безопасно подставить в URL
                    String nameEnc = URLEncoder.encode(modelToInstall, "UTF-8");
                    URL url = new URL("http://" + serverIp +
                            "/models/" + nameEnc + "/install");
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setConnectTimeout(5000);
                    conn.setReadTimeout(5000);

                    // Basic Auth
                    String creds = username + ":" + password;
                    String auth = "Basic " +
                            Base64.encodeToString(
                                    creds.getBytes("UTF-8"),
                                    Base64.NO_WRAP
                            );
                    conn.setRequestProperty("Authorization", auth);

                    // Выполняем запрос
                    int code = conn.getResponseCode();
                    if (code == HttpURLConnection.HTTP_OK) {
                        return true;
                    } else if (code == HttpURLConnection.HTTP_UNAUTHORIZED) {
                        errorMsg = "401: Неверный логин/пароль";
                        return false;
                    } else {
                        errorMsg = "Сервер вернул код " + code;
                        return false;
                    }
                } catch (Exception e) {
                    errorMsg = "Ошибка: " + e.getMessage();
                    return false;
                } finally {
                    if (conn != null) conn.disconnect();
                }
            }

            @Override
            protected void onPostExecute(Boolean success) {
                Context ctx = requireContext();
                if (success) {
                    Toast.makeText(ctx,
                            "Модель «" + modelToInstall + "» установлена",
                            Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(ctx,
                            errorMsg,
                            Toast.LENGTH_LONG).show();
                }
            }
        }.execute();

    }
    private void fetchInstalledModels(String status, Spinner spinnerModels) {
        new AsyncTask<Void, Void, List<String>>() {
            Exception exception;

            @Override
            protected List<String> doInBackground(Void... voids) {
                List<String> result = new ArrayList<>();
                HttpURLConnection conn = null;
                try {
                    URL url = new URL("http://" + serverIp + "/models");
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    // Basic Auth
                    String creds = username + ":" + password;
                    String auth  = "Basic " + android.util.Base64
                            .encodeToString(creds.getBytes("UTF-8"),
                                    android.util.Base64.NO_WRAP);
                    conn.setRequestProperty("Authorization", auth);

                    int code = conn.getResponseCode();
                    if (code == 401) throw new RuntimeException("UNAUTHORIZED");
                    if (code != 200) throw new RuntimeException("HTTP code: " + code);

                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(conn.getInputStream())
                    );
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line);
                    }
                    reader.close();

                    JSONObject json = new JSONObject(sb.toString());
                    JSONArray arr    = json.optJSONArray(status);
                    if (arr != null) {
                        for (int i = 0; i < arr.length(); i++) {
                            result.add(arr.getString(i));
                        }
                    }
                } catch (Exception e) {
                    exception = e;
                } finally {
                    if (conn != null) conn.disconnect();
                }
                return result;
            }

            @Override
            protected void onPostExecute(List<String> models) {
                if (!isAdded()) return;
                Context ctx = requireContext();

                if (exception != null) {
                    String msg = exception.getMessage();
                    if ("UNAUTHORIZED".equals(msg)) {
                        Toast.makeText(ctx,
                                "Ошибка 401: Неверный логин или пароль",
                                Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(ctx,
                                "Ошибка при загрузке: " + msg,
                                Toast.LENGTH_LONG).show();
                    }
                    return;
                }
                if (models.isEmpty()) {
                    Toast.makeText(ctx,
                            "Нет установленных моделей",
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                // 1) Создаём адаптер и навешиваем на Spinner
                ArrayAdapter<String> adapter = new ArrayAdapter<>(
                        ctx,
                        android.R.layout.simple_spinner_item,
                        models
                );
                adapter.setDropDownViewResource(
                        android.R.layout.simple_spinner_dropdown_item
                );
                spinnerModels.setAdapter(adapter);

                spinnerModels.setOnItemSelectedListener(
                        new AdapterView.OnItemSelectedListener() {
                            @Override
                            public void onItemSelected(AdapterView<?> parent,
                                                       View view,
                                                       int position,
                                                       long id) {
                                String model = adapter.getItem(position);
                                // Сохраняем в prefs если installed
                                if (status == "installed")
                                {
                                    boolean ok = prefs.edit()
                                            .putString("aimodel", model)
                                            .commit();  // или .apply()
                                    Log.d("SettingsFragment",
                                            "Saved aimodel=" + model + " ok=" + ok);

                                }

                                else if (status == "available")
                                {
                                    installInfo.setText("Установить модель: " + model);
                                    modelToInstall = model;
                                }
                                                            }
                            @Override
                            public void onNothingSelected(AdapterView<?> parent) { }
                        }
                );
                // 3) Восстанавливаем предыдущий выбор _после_ навешивания слушателя
                String last = prefs.getString("aimodel", null);
                if (last != null) {
                    int pos = adapter.getPosition(last);
                    if (pos >= 0) {
                        spinnerModels.setSelection(pos);
                        // setSelection вызовет onItemSelected и запишет тот же ключ
                    }
                }
            }
        }.execute();
    }
}
