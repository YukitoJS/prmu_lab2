package com.example.prmu_lab2.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.prmu_lab2.R;
import com.example.prmu_lab2.models.Contact;

import java.util.ArrayList;
import java.util.List;

public class ContactAdapter extends RecyclerView.Adapter<ContactAdapter.ViewHolder> {

    private List<Contact> contact = new ArrayList<>();

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_contact, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Contact item = contact.get(position);

        holder.textViewContactName.setText(item.getContactName());
        holder.textViewImportance.setText(item.getImportanceContact());
        holder.textViewDate.setText(item.getFormattedDate());
    }

    @Override
    public int getItemCount() {
        return contact.size();
    }

    public void setContact(List<Contact> items) {
        this.contact.clear();
        this.contact.addAll(items);
        notifyDataSetChanged();
    }

    public void clearItems() {
        this.contact.clear();
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textViewContactName, textViewImportance, textViewDate;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewContactName = itemView.findViewById(R.id.textViewContactName);
            textViewImportance = itemView.findViewById(R.id.textViewImportance);
            textViewDate = itemView.findViewById(R.id.textViewDate);
        }
    }
}