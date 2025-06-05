package com.example.synapsechat;

import android.annotation.SuppressLint;
import android.os.AsyncTask;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.example.synapsechat.data.AppDatabase;
import com.example.synapsechat.data.LoginEntry;
import com.example.synapsechat.data.LoginEntryDao;
import com.example.synapsechat.ui.LoginEntryAdapter;

import java.util.List;

public class SessionsFragment extends Fragment {

    private RecyclerView rvLoginHistory;
    private LoginEntryAdapter adapter;
    private AppDatabase db;

    @Nullable @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        View v = inflater.inflate(R.layout.fragment_sessions, container, false);
        rvLoginHistory = v.findViewById(R.id.rvLoginHistory);
        return v;
    }
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        db = AppDatabase.getInstance(requireContext());
        adapter = new LoginEntryAdapter();

        rvLoginHistory.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvLoginHistory.setAdapter(adapter);
        loadLoginHistory();
    }
    @SuppressLint("StaticFieldLeak")
    private void loadLoginHistory() {
        new AsyncTask<Void, Void, List<LoginEntry>>() {
            @Override
            protected List<LoginEntry> doInBackground(Void... voids) {
                LoginEntryDao dao = db.loginEntryDao();
                return dao.getAllEntries();
            }

            @Override
            protected void onPostExecute(List<LoginEntry> loginEntries) {
                if (loginEntries == null || loginEntries.isEmpty()) {
                    // Если записей нет, можно показать Toast или заглушку
                    Toast.makeText(requireContext(),
                            "История входов пуста", Toast.LENGTH_SHORT).show();
                }
                adapter.setItems(loginEntries);
            }
        }.execute();
    }
}


