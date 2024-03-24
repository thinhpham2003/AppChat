package com.example.baitaplon.adapters;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.baitaplon.databinding.ItemContainerRecentConversionBinding;
import com.example.baitaplon.listener.ConversionListener;
import com.example.baitaplon.models.Message;
import com.example.baitaplon.models.User;

import java.util.List;

public class RecentConversationsAdapter extends RecyclerView.Adapter<RecentConversationsAdapter.ConverionViewHolder>{

    private final List<Message> messages;
    private final ConversionListener conversionListener;

    public RecentConversationsAdapter(List<Message> messages, ConversionListener conversionListener) {
        this.messages = messages;
        this.conversionListener = conversionListener;
    }

    @NonNull
    @Override
    public ConverionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ConverionViewHolder(
                ItemContainerRecentConversionBinding.inflate(
                        LayoutInflater.from(parent.getContext()),
                        parent,
                        false
                )
        );
    }

    @Override
    public void onBindViewHolder(@NonNull ConverionViewHolder holder, int position) {
        holder.setData(messages.get(position));
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    class ConverionViewHolder extends RecyclerView.ViewHolder {
        ItemContainerRecentConversionBinding binding;

        ConverionViewHolder(ItemContainerRecentConversionBinding itemContainerRecentConversionBinding) {
            super(itemContainerRecentConversionBinding.getRoot());
            binding = itemContainerRecentConversionBinding;

        }

        void setData(Message message) {
            binding.avatarProfile.setImageBitmap(getConversionImage(message.conversionImage));
            binding.txtName.setText(message.conversionName);
            binding.txtRecentMsg.setText(message.message);
            binding.getRoot().setOnClickListener(v -> {
                User user = new User();
                user.id = message.conversionId;
                user.name = message.conversionName;
                user.image = message.conversionImage;
                conversionListener.onConversionClicked(user);
            });
        }
    }

    private Bitmap getConversionImage(String encodeImage) {
        byte[] bytes = Base64.decode(encodeImage, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }
}
