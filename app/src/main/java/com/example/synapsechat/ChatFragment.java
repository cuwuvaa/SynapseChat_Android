package com.example.synapsechat;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import io.noties.markwon.Markwon;

public class ChatFragment extends Fragment {
    private static final String ARG_SESSION = "session_id";

    public static ChatFragment newInstance(String sessionId) {
        ChatFragment f = new ChatFragment();
        Bundle args = new Bundle();
        args.putString(ARG_SESSION, sessionId);
        f.setArguments(args);
        return f;
    }

    private String sessionId;
    private RecyclerView rvMessages;
    private EditText     etMessage;
    private Button       btnSend;
    private TextView     tvStatus;
    private MessageAdapter adapter;
    private List<Message>  messages = new ArrayList<>();

    private String serverIp, username, password, model;
    private Markwon markwon;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            sessionId = getArguments().getString(ARG_SESSION);
        }
    }

    @Nullable @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_chat, container, false);
    }

    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rvMessages = view.findViewById(R.id.rvMessages);
        etMessage  = view.findViewById(R.id.etMessage);
        btnSend    = view.findViewById(R.id.btnSend);
        tvStatus   = view.findViewById(R.id.tvStatus);
        tvStatus.setVisibility(View.GONE);

        markwon = Markwon.create(requireContext());

        rvMessages.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new MessageAdapter(messages, markwon);
        rvMessages.setAdapter(adapter);

        SharedPreferences prefs = requireContext()
                .getSharedPreferences("app_prefs", Context.MODE_PRIVATE);
        serverIp = prefs.getString("server_ip", "");
        username = prefs.getString("username", "");
        password = prefs.getString("password", "");
        model    = prefs.getString("aimodel", "");

        new FetchHistoryTask().execute();

        btnSend.setOnClickListener(v -> {
            String prompt = etMessage.getText().toString().trim();
            if (prompt.isEmpty()) return;

            messages.add(new Message(username, prompt));
            adapter.notifyItemInserted(messages.size() - 1);
            rvMessages.scrollToPosition(messages.size() - 1);
            etMessage.setText("");

            new SendMessageTask(prompt).execute();
        });
    }

    private class FetchHistoryTask extends AsyncTask<Void, Void, List<Message>> {
        @Override
        protected List<Message> doInBackground(Void... voids) {
            List<Message> out = new ArrayList<>();
            try {
                URL url = new URL("http://" + serverIp + "/history/" + sessionId);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                String auth = "Basic " + Base64.encodeToString(
                        (username + ":" + password)
                                .getBytes(StandardCharsets.UTF_8),
                        Base64.NO_WRAP);
                conn.setRequestProperty("Authorization", auth);

                if (conn.getResponseCode() != 200) throw new Exception();
                BufferedReader br = new BufferedReader(
                        new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8)
                );
                String json = br.lines().collect(Collectors.joining());
                br.close();

                JSONArray arr = new JSONArray(json);
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject o = arr.getJSONObject(i);
                    String role = o.getString("role");
                    String text = o.getString("content");
                    out.add(new Message(role.equals("user") ? username : "Bot", text));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return out;
        }

        @Override
        protected void onPostExecute(List<Message> history) {
            if (!history.isEmpty()) {
                messages.clear();
                messages.addAll(history);
                adapter.notifyDataSetChanged();
                rvMessages.scrollToPosition(messages.size() - 1);
            }
        }
    }

    private class SendMessageTask extends AsyncTask<Void, Void, String> {
        private final String prompt;
        private String error;
        SendMessageTask(String prompt) {
            this.prompt = prompt;
        }
        @Override
        protected void onPreExecute() {
            btnSend.setEnabled(false);
            tvStatus.setText("Бот думает…");
            tvStatus.setVisibility(View.VISIBLE);
        }
        @Override
        protected String doInBackground(Void... voids) {
            try {
                URL url = new URL("http://" + serverIp + "/chat/" + sessionId);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty(
                        "Content-Type", "application/json; charset=UTF-8");
                String auth = "Basic " + Base64.encodeToString(
                        (username + ":" + password)
                                .getBytes(StandardCharsets.UTF_8),
                        Base64.NO_WRAP);
                conn.setRequestProperty("Authorization", auth);
                conn.setDoOutput(true);

                JSONObject body = new JSONObject();
                body.put("model", model);
                body.put("prompt", prompt);
                try (OutputStream os = conn.getOutputStream()) {
                    os.write(body.toString().getBytes(StandardCharsets.UTF_8));
                }

                int code = conn.getResponseCode();
                InputStream in = (code == HttpURLConnection.HTTP_OK)
                        ? conn.getInputStream()
                        : conn.getErrorStream();
                String resp = new BufferedReader(
                        new InputStreamReader(in, StandardCharsets.UTF_8))
                        .lines().collect(Collectors.joining("\n"));

                if (code == HttpURLConnection.HTTP_OK) {
                    JSONObject json = new JSONObject(resp);
                    return json.optString("response", "");
                } else {
                    error = "Ошибка " + code + ": " + resp;
                }
            } catch (Exception e) {
                error = e.getMessage();
            }
            return null;
        }
        @Override
        protected void onPostExecute(String botResponse) {
            btnSend.setEnabled(true);
            tvStatus.setVisibility(View.GONE);

            if (botResponse != null) {
                messages.add(new Message("Bot", botResponse));
                adapter.notifyItemInserted(messages.size() - 1);
                rvMessages.scrollToPosition(messages.size() - 1);
            } else {
                Toast.makeText(requireContext(),
                        "Не удалось отправить: " + error,
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    public static class Message {
        public final String sender, text;
        public Message(String sender, String text) {
            this.sender = sender;
            this.text   = text;
        }
    }

    public static class MessageAdapter
            extends RecyclerView.Adapter<MessageAdapter.VH> {
        private final List<Message> items;
        private final Markwon     markwon;

        public MessageAdapter(List<Message> items, Markwon markwon) {
            this.items   = items;
            this.markwon = markwon;
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(
                @NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(android.R.layout.simple_list_item_2,
                            parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(
                @NonNull VH holder, int position) {
            Message m = items.get(position);
            holder.tv1.setText(m.sender);
            markwon.setMarkdown(holder.tv2, m.text);
        }

        @Override public int getItemCount() {
            return items.size();
        }

        static class VH extends RecyclerView.ViewHolder {
            TextView tv1, tv2;
            VH(@NonNull View itemView) {
                super(itemView);
                tv1 = itemView.findViewById(android.R.id.text1);
                tv2 = itemView.findViewById(android.R.id.text2);
            }
        }
    }
}
