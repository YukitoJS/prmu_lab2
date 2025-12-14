package com.example.prmu_lab2.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.prmu_lab2.R;
import com.example.prmu_lab2.models.Contact;

import java.util.ArrayList;
import java.util.List;

public class ContactAdapter extends RecyclerView.Adapter<ContactAdapter.ViewHolder> {

    private List<Contact> contacts = new ArrayList<>();

    private OnItemClickListener itemClickListener;
    private OnDeleteClickListener deleteClickListener;

    public interface OnItemClickListener {
        void onItemClick(Contact item);
    }

    public interface OnDeleteClickListener {
        void onDeleteClick(String itemId, String itemName);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.itemClickListener = listener;
    }

    public void setOnDeleteClickListener(OnDeleteClickListener listener) {
        this.deleteClickListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_contact, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Contact item = contacts.get(position);

        holder.textViewContactName.setText(item.getContactName());
        holder.textViewImportance.setText(String.valueOf(item.getImportanceContact()));
        holder.textViewDate.setText(item.getFormattedDate());

        holder.currentItemId = item.getId();

        // Обработчик клика на весь элемент (для редактирования)
        holder.itemView.setOnClickListener(v -> {
            if (itemClickListener != null) {
                itemClickListener.onItemClick(item);
            }
        });

        // Обработчик долгого нажатия (альтернатива для редактирования)
        holder.itemView.setOnLongClickListener(v -> {
            if (itemClickListener != null) {
                itemClickListener.onItemClick(item);
                return true;
            }
            return false;
        });

        // Обработчик кнопки удаления
        holder.buttonDelete.setOnClickListener(v -> {
            if (deleteClickListener != null) {
                deleteClickListener.onDeleteClick(item.getId(), item.getContactName());
            }
        });
    }

    @Override
    public int getItemCount() {
        return contacts.size();
    }

    public void setContact(List<Contact> items) {
        this.contacts.clear();
        this.contacts.addAll(items);
        notifyDataSetChanged();
    }

    public void clearItems() {
        this.contacts.clear();
        notifyDataSetChanged();
    }
    public void updateItem(Contact updatedItem) {
        for (int i = 0; i < contacts.size(); i++) {
            if (contacts.get(i).getId().equals(updatedItem.getId())) {
                contacts.set(i, updatedItem);
                notifyItemChanged(i);
                break;
            }
        }
    }
    public void removeItem(String itemId) {
        for (int i = 0; i < contacts.size(); i++) {
            if (contacts.get(i).getId().equals(itemId)) {
                contacts.remove(i);
                notifyItemRemoved(i);
                break;
            }
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textViewContactName, textViewImportance, textViewDate;
        ImageButton buttonDelete;
        String currentItemId;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewContactName = itemView.findViewById(R.id.textViewContactName);
            textViewImportance = itemView.findViewById(R.id.textViewImportance);
            textViewDate = itemView.findViewById(R.id.textViewDate);
        }
    }
}