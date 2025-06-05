package com.example.synapsechat;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class SettingsFragment extends Fragment {
    private Spinner spinnerModels;
    private Spinner spinnerAvailableModels;
    private Spinner spinnerAvailableVariants;
    private TextView installInfo;
    private Button installButton, logoutButton;

    private SharedPreferences prefs;
    private String serverIp, username, password;
    private String modelToInstall;
    private String variantToInstall;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE);
        serverIp = prefs.getString("server_ip", "");
        username = prefs.getString("username", "");
        password = prefs.getString("password", "");
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_settings, container, false);
        installInfo = root.findViewById(R.id.installinfo);
        installButton = root.findViewById(R.id.installbutton);
        spinnerModels = root.findViewById(R.id.spinnerModels);
        spinnerAvailableModels = root.findViewById(R.id.spinnerAvailableModels);
        spinnerAvailableVariants = root.findViewById(R.id.spinnerAvailableVariants);
        logoutButton = root.findViewById(R.id.btn_logout);
        logoutButton.setOnClickListener(v -> {
            prefs.edit().putBoolean("savelogin", false).apply();
            requireActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new LoginFragment())
                    .commit();
            getActivity().setTitle("Вход");
        });
        installButton.setOnClickListener(v -> sendInstallRequest());
        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        fetchInstalledModels("installed", spinnerModels);
        fetchInstalledModels("available", spinnerAvailableModels);
    }

    @SuppressLint("StaticFieldLeak")
    private void fetchInstalledModels(String status, Spinner spinner) {
        ProgressDialog pd = new ProgressDialog(requireContext());
        pd.setMessage("Загрузка");
        pd.setIndeterminate(true);
        pd.setCancelable(false);
        pd.show();
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
                    String auth = "Basic " + Base64.encodeToString(
                            (username + ":" + password).getBytes("UTF-8"),
                            Base64.NO_WRAP
                    );
                    conn.setRequestProperty("Authorization", auth);
                    int code = conn.getResponseCode();
                    if (code != 200) throw new RuntimeException("HTTP " + code);
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(conn.getInputStream())
                    );
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line);
                    }
                    JSONObject json = new JSONObject(sb.toString());
                    JSONArray arr = json.optJSONArray(status);
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
                if (pd.isShowing()) pd.dismiss();
                if (!isAdded()) return;
                Context ctx = requireContext();
                if (exception != null) {
                    Toast.makeText(ctx,
                            "Ошибка загрузки моделей: " + exception.getMessage(),
                            Toast.LENGTH_LONG).show();
                    return;
                }
                if (models.isEmpty()) {
                    String msg = status.equals("installed")
                            ? "Нет установленных моделей"
                            : "Нет доступных моделей";
                    Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show();
                    return;
                }
                ArrayAdapter<String> adapter = new ArrayAdapter<>(
                        ctx, android.R.layout.simple_spinner_item, models
                );
                adapter.setDropDownViewResource(
                        android.R.layout.simple_spinner_dropdown_item
                );
                spinner.setAdapter(adapter);

                spinner.setOnItemSelectedListener(
                        new AdapterView.OnItemSelectedListener() {
                            @Override
                            public void onItemSelected(AdapterView<?> parent,
                                                       View view,
                                                       int position,
                                                       long id) {
                                String selected = adapter.getItem(position);
                                if ("installed".equals(status)) {
                                    prefs.edit().putString("aimodel", selected).apply();
                                } else if ("available".equals(status)) {
                                    modelToInstall = selected;
                                    installInfo.setText("Выбрано: " + selected);
                                    fetchVariants(selected);
                                }
                            }
                            @Override
                            public void onNothingSelected(AdapterView<?> parent) { }
                        }
                );
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    @SuppressLint("StaticFieldLeak")
    private void fetchVariants(String modelName) {
        new AsyncTask<Void, Void, List<String>>() {
            Exception exception;
            @Override
            protected List<String> doInBackground(Void... voids) {
                List<String> variants = new ArrayList<>();
                HttpURLConnection conn = null;
                try {
                    String enc = URLEncoder.encode(modelName, "UTF-8");
                    URL url = new URL("http://" + serverIp + "/models/" + enc + "/variants");
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    String auth = "Basic " + Base64.encodeToString(
                            (username + ":" + password).getBytes("UTF-8"),
                            Base64.NO_WRAP
                    );
                    conn.setRequestProperty("Authorization", auth);
                    int code = conn.getResponseCode();
                    if (code != 200) throw new RuntimeException("HTTP " + code);
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(conn.getInputStream())
                    );
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line);
                    }
                    JSONArray arr = new JSONArray(sb.toString());
                    for (int i = 0; i < arr.length(); i++) {
                        variants.add(arr.getString(i));
                    }
                } catch (Exception e) {
                    exception = e;
                } finally {
                    if (conn != null) conn.disconnect();
                }
                return variants;
            }

            @Override
            protected void onPostExecute(List<String> variants) {
                if (!isAdded()) return;
                Context ctx = requireContext();
                if (exception != null) {
                    Toast.makeText(ctx,
                            "Не удалось получить варианты: " + exception.getMessage(),
                            Toast.LENGTH_LONG).show();
                    return;
                }
                if (variants.isEmpty()) {
                    Toast.makeText(ctx,
                            "Нет вариантов для " + modelName,
                            Toast.LENGTH_SHORT).show();
                    return;
                }
                ArrayAdapter<String> adapter = new ArrayAdapter<>(
                        ctx, android.R.layout.simple_spinner_item, variants
                );
                adapter.setDropDownViewResource(
                        android.R.layout.simple_spinner_dropdown_item
                );
                spinnerAvailableVariants.setAdapter(adapter);

                spinnerAvailableVariants.setOnItemSelectedListener(
                        new AdapterView.OnItemSelectedListener() {
                            @Override
                            public void onItemSelected(AdapterView<?> parent,
                                                       View view,
                                                       int position,
                                                       long id) {
                                variantToInstall = adapter.getItem(position);
                                installInfo.setText("Установить: " + variantToInstall);
                            }
                            @Override
                            public void onNothingSelected(AdapterView<?> parent) { }
                        }
                );
            }
        }.execute();
    }

    @SuppressLint("StaticFieldLeak")
    private void sendInstallRequest() {
        if (variantToInstall == null) {
            Toast.makeText(requireContext(),
                    "Выберите вариант модели", Toast.LENGTH_SHORT).show();
            return;
        }
        new AsyncTask<Void, Void, Boolean>() {
            String errorMsg;
            @Override
            protected Boolean doInBackground(Void... voids) {
                HttpURLConnection conn = null;
                try {
                    String nameEnc = URLEncoder.encode(variantToInstall, "UTF-8");
                    URL url = new URL("http://" + serverIp + "/models/" + nameEnc + "/install");
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setConnectTimeout(5000);
                    conn.setReadTimeout(5000);
                    String auth = "Basic " + Base64.encodeToString(
                            (username + ":" + password).getBytes(StandardCharsets.UTF_8),
                            Base64.NO_WRAP
                    );
                    conn.setRequestProperty("Authorization", auth);
                    int code = conn.getResponseCode();
                    if (code == HttpURLConnection.HTTP_OK) {
                        return true;
                    } else if (code == HttpURLConnection.HTTP_UNAUTHORIZED) {
                        errorMsg = "401: Неверный логин/пароль";
                    } else {
                        errorMsg = "Ошибка сервера: " + code;
                    }
                    return false;
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
                            "Установлен " + variantToInstall,
                            Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(ctx,
                            errorMsg, Toast.LENGTH_LONG).show();
                }
            }
        }.execute();
    }
}
