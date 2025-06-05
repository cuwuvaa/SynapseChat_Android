package com.example.synapsechat.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.synapsechat.R;
import com.example.synapsechat.data.LoginEntry;

import java.util.ArrayList;
import java.util.List;

/**
 * Адаптер для RecyclerView, который отображает список LoginEntry (ipPort, username, password).
 */
public class LoginEntryAdapter extends RecyclerView.Adapter<LoginEntryAdapter.ViewHolder> {

    private final List<LoginEntry> items = new ArrayList<>();

    /**
     * Обновить данные в адаптере (перезаписать весь список).
     */
    public void setItems(List<LoginEntry> newItems) {
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public LoginEntryAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Надуваем макет элемента item_login_entry.xml
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_login_entry, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LoginEntryAdapter.ViewHolder holder, int position) {
        LoginEntry entry = items.get(position);
        holder.tvIpPort.setText(entry.getIpPort());
        holder.tvUsername.setText("User: " + entry.getUsername());
        holder.tvPassword.setText("Pass: " + entry.getPassword());
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    /**
     * ViewHolder, хранящий ссылки на TextView-ы одного элемента.
     */
    public static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView tvIpPort;
        final TextView tvUsername;
        final TextView tvPassword;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvIpPort    = itemView.findViewById(R.id.tvIpPort);
            tvUsername  = itemView.findViewById(R.id.tvUsername);
            tvPassword  = itemView.findViewById(R.id.tvPassword);
        }
    }
}
