package com.example.synapsechat;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ChatFragment extends Fragment {
    private String serverIp, username, password;

    private RecyclerView rvMessages;
    private EditText etMessage;
    private Button btnSend;
    private MessageAdapter adapter;
    private List<Message> messages = new ArrayList<>();

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_chat, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 2) Инициализируем UI
        rvMessages = view.findViewById(R.id.rvMessages);
        etMessage  = view.findViewById(R.id.etMessage);
        btnSend    = view.findViewById(R.id.btnSend);

        adapter = new MessageAdapter(messages);
        rvMessages.setLayoutManager(new LinearLayoutManager(getContext()));
        rvMessages.setAdapter(adapter);

        // 3) Загружаем историю чата (если нужно)
        new LoadHistoryTask().execute();

        // 4) Отправка сообщения
        btnSend.setOnClickListener(v -> {
            String text = etMessage.getText().toString().trim();
            if (text.isEmpty()) return;
            new SendMessageTask(text).execute();
            etMessage.setText("");
        });
    }

    // Задача для отправки сообщения на сервер
    private class SendMessageTask extends AsyncTask<Void, Void, Message> {
        private final String text;
        private String error;

        SendMessageTask(String text) { this.text = text; }

        @Override
        protected Message doInBackground(Void... voids) {
            HttpURLConnection conn = null;
            try {
                URL url = new URL("http://" + serverIp + "/chat");
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");

                String creds = username + ":" + password;
                String auth = "Basic " +
                        Base64.encodeToString(creds.getBytes("UTF-8"), Base64.NO_WRAP);
                conn.setRequestProperty("Authorization", auth);

                conn.setDoOutput(true);
                JSONObject body = new JSONObject();
                body.put("message", text);
                try (OutputStream os = conn.getOutputStream()) {
                    os.write(body.toString().getBytes("UTF-8"));
                }

                int code = conn.getResponseCode();
                if (code == HttpURLConnection.HTTP_OK) {
                    // читаем ответ
                    InputStream in = conn.getInputStream();
                    String resp = new BufferedReader(new InputStreamReader(in))
                            .lines().collect(Collectors.joining("\n"));
                    JSONObject json = new JSONObject(resp);
                    // допустим, сервер возвращает {"user":"...", "message":"...", "timestamp":...}
                    return new Message(
                            json.getString("user"),
                            json.getString("message"),
                            json.getLong("timestamp")
                    );
                } else {
                    error = "Ошибка сервера: " + code;
                    return null;
                }
            } catch (Exception e) {
                error = e.getMessage();
                return null;
            } finally {
                if (conn != null) conn.disconnect();
            }
        }

        @Override
        protected void onPostExecute(Message msg) {
            if (msg != null) {
                messages.add(msg);
                adapter.notifyItemInserted(messages.size() - 1);
                rvMessages.smoothScrollToPosition(messages.size() - 1);
            } else {
                Toast.makeText(getContext(),
                        "Не удалось отправить: " + error,
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Задача для загрузки истории
    private class LoadHistoryTask extends AsyncTask<Void, Void, List<Message>> {
        @Override
        protected List<Message> doInBackground(Void... voids) {
            // аналогично: GET http://serverIp:8000/history
            // и собираете List<Message>
            return new ArrayList<>(); // заглушка
        }
        @Override
        protected void onPostExecute(List<Message> list) {
            messages.clear();
            messages.addAll(list);
            adapter.notifyDataSetChanged();
        }
    }

    // Модель сообщения
    public static class Message {
        public final String user;
        public final String text;
        public final long timestamp;
        public Message(String user, String text, long timestamp) {
            this.user = user; this.text = text; this.timestamp = timestamp;
        }
    }

    // Простой адаптер для RecyclerView
    public static class MessageAdapter
            extends RecyclerView.Adapter<MessageAdapter.VH> {
        private final List<Message> items;
        public MessageAdapter(List<Message> items) { this.items = items; }

        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(android.R.layout.simple_list_item_2, parent, false);
            return new VH(v);
        }
        @Override
        public void onBindViewHolder(@NonNull VH holder, int pos) {
            Message m = items.get(pos);
            holder.tv1.setText(m.user);
            holder.tv2.setText(m.text);
        }
        @Override public int getItemCount() { return items.size(); }

        static class VH extends RecyclerView.ViewHolder {
            TextView tv1, tv2;
            VH(@NonNull View v) {
                super(v);
                tv1 = v.findViewById(android.R.id.text1);
                tv2 = v.findViewById(android.R.id.text2);
            }
        }
    }
}
