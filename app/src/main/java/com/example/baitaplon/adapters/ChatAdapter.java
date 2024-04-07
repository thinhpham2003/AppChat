package com.example.baitaplon.adapters;

import static android.content.ContentValues.TAG;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.example.baitaplon.databinding.ItemContainerReceivedMessageBinding;
import com.example.baitaplon.databinding.ItemContainerSentMessageBinding;
import com.example.baitaplon.models.Message;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final List<Message> messages;
    private final String senderId;
    private Bitmap receiverProfileImage;
    public static final int VIEW_TYPE_SENT = 1;
    public static final int VIEW_TYPE_RECEIVED = 2;

    public void setReceiverProfileImage(Bitmap bitmap) {
        receiverProfileImage = bitmap;
    }
    public ChatAdapter(List<Message> messages, String senderId, Bitmap receiverProfileImage) {
        this.messages = messages;
        this.senderId = senderId;
        this.receiverProfileImage = receiverProfileImage;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_SENT) {
            return new SentMessageViewHolder(
                    ItemContainerSentMessageBinding.inflate(
                            LayoutInflater.from(parent.getContext()),
                            parent,
                            false
                    ),
                    parent.getContext()
            );
        } else {
            return new ReceivedMessageViewHolder(
                    ItemContainerReceivedMessageBinding.inflate(
                            LayoutInflater.from(parent.getContext()),
                            parent,
                            false
                    ),
                    parent.getContext()
            );
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Message message = messages.get(position);
        if (getItemViewType(position) == VIEW_TYPE_SENT) {
            ((SentMessageViewHolder) holder).setData(message);
        } else {
            ((ReceivedMessageViewHolder) holder).settData(message, receiverProfileImage);
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    @Override
    public int getItemViewType(int position) {
        if (messages.get(position).senderId.equals(senderId)) {
            return VIEW_TYPE_SENT;
        } else {
            return VIEW_TYPE_RECEIVED;
        }
    }

    static class SentMessageViewHolder extends RecyclerView.ViewHolder {
        private final ItemContainerSentMessageBinding binding;
        private final Context context;

        SentMessageViewHolder(ItemContainerSentMessageBinding itemContainerSentMessageBinding, Context context) {
            super(itemContainerSentMessageBinding.getRoot());
            binding = itemContainerSentMessageBinding;
            this.context = context;
        }

        void setData(Message message) {
            if (message.image != null && !message.image.trim().isEmpty()) {
                binding.imageView.setVisibility(View.VISIBLE);
                binding.imageView.setImageBitmap(getImage(message.image));
                binding.txtMsg.setVisibility(View.GONE);
                binding.txtDate.setVisibility(View.GONE);
                binding.txtFile.setVisibility(View.GONE);
            } else if (message.fileURL != null && !message.fileURL.trim().isEmpty()) {
                binding.txtMsg.setVisibility(View.VISIBLE);
                binding.imageView.setVisibility(View.GONE);
                binding.txtDate.setVisibility(View.VISIBLE);

                // Hiển thị tên file
                //String fileName = getFileNameFromUrl(message.fileName);
                binding.txtMsg.setText(message.fileName);
                binding.txtMsg.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        // Check if file is downloaded
                        if (isDownloaded(message.fileURL)) {
                            // Open file
                            openFile(message.fileURL);
                        } else {
                            // Show message that file is not downloaded
                            Toast.makeText(context, "File chưa được tải xuống", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            } else if (message.location != null ) {
                String location = "Vị trí hiện tại: " + message.location;
                binding.txtMsg.setVisibility(View.VISIBLE);
                binding.txtMsg.setText(message.location);
                binding.imageView.setVisibility(View.GONE);
                binding.txtFile.setVisibility(View.GONE);
                binding.txtDate.setVisibility(View.VISIBLE);
                binding.txtDate.setText(message.dateTime);
                binding.txtMsg.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // Tạo Intent để mở ứng dụng Google Maps
                        Uri gmmIntentUri = Uri.parse("geo:" + message.location + "?q=" + message.location);
                        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                        mapIntent.setPackage("com.google.android.apps.maps");
                        context.startActivity(mapIntent);
                    }
                });
            } else {
                binding.txtMsg.setText(message.message);
                binding.imageView.setVisibility(View.GONE);
                binding.txtMsg.setVisibility(View.VISIBLE);
                binding.txtFile.setVisibility(View.GONE);
                binding.txtDate.setVisibility(View.VISIBLE);
                binding.txtDate.setText(message.dateTime);
            }


            binding.txtDate.setText(message.dateTime);
        }

        private String getFileNameFromUrl(String url) {
            String[] parts = url.split("/");
            Log.d(TAG, "getFileNameFromUrl: " + url);
            return parts[parts.length - 1];
        }
        private Bitmap getImage(String encodeImage) {
            byte[] bytes = Base64.decode(encodeImage, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        }
        private boolean isDownloaded(String fileUrl) {
            // Implement your logic to check if file is downloaded
            return true; // Example: return true if file exists in the device
        }

        private void openFile(String fileUrl) {
            // Implement your logic to open file
            Intent intent = new Intent(Intent.ACTION_VIEW);
            Uri uri = Uri.parse(fileUrl);
            intent.setDataAndType(uri, "application/*");
            context.startActivity(intent);
        }
    }

    static class ReceivedMessageViewHolder extends RecyclerView.ViewHolder {
        private final ItemContainerReceivedMessageBinding binding;
        private final Context context;

        ReceivedMessageViewHolder(ItemContainerReceivedMessageBinding itemContainerReceivedMessageBinding, Context context) {
            super(itemContainerReceivedMessageBinding.getRoot());
            binding = itemContainerReceivedMessageBinding;
            this.context = context;
        }

        void settData(Message message, Bitmap receiverProfileImage) {
            if (message.image != null && !message.image.trim().isEmpty()) {
                binding.imageView.setVisibility(View.VISIBLE);
                binding.imageView.setImageBitmap(getImage(message.image));
                binding.txtMsg.setVisibility(View.GONE);
                binding.txtDate.setVisibility(View.GONE);
            }else if (message.fileURL != null && !message.fileURL.trim().isEmpty()) {
                binding.txtMsg.setVisibility(View.VISIBLE); // Add a view for files (e.g., TextView with filename)
                binding.imageView.setVisibility(View.GONE);
                binding.txtDate.setVisibility(View.VISIBLE);

                binding.txtMsg.setText(message.fileName);
                binding.txtMsg.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        downloadAndOpenFile(context, message.fileURL);
                    }
                });
            } else if (message.location != null ) {
                binding.txtMsg.setVisibility(View.VISIBLE);
                binding.txtMsg.setText(message.location);
                binding.imageView.setVisibility(View.GONE);
                binding.txtFile.setVisibility(View.GONE);
                binding.txtDate.setVisibility(View.VISIBLE);
                binding.txtDate.setText(message.dateTime);
                binding.txtMsg.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // Tạo Intent để mở ứng dụng Google Maps
                        Uri gmmIntentUri = Uri.parse("geo:" + message.location + "?q=" + message.location);
                        Log.d(TAG, "onClick: " + message.location);
                        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                        mapIntent.setPackage("com.google.android.apps.maps");
                        context.startActivity(mapIntent);
                    }
                });
            } else {
                binding.txtMsg.setText(message.message);
                binding.imageView.setVisibility(View.GONE);
                binding.txtMsg.setVisibility(View.VISIBLE);
                binding.txtFile.setVisibility(View.GONE);
                binding.txtDate.setVisibility(View.VISIBLE);
                binding.txtDate.setText(message.dateTime);

            }
            binding.txtDate.setText(message.dateTime);
            if(receiverProfileImage != null){
                binding.imgProfile.setImageBitmap(receiverProfileImage);
            }
        }
        private Bitmap getImage(String encodeImage) {
            byte[] bytes = Base64.decode(encodeImage, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        }
        private String getFileNameFromUrl(String url) {
            // Lấy tên file từ URL
            String[] parts = url.split("/");
            Log.d(TAG, "getFileNameFromUrl: " + url);
            return parts[parts.length - 1];
        }
        // Hàm cho phép người gửi mở file khi bấm vào đoạn tin nhắn chứa file


        // Hàm cho phép người gửi mở file khi bấm vào đoạn tin nhắn chứa file
        private void downloadAndOpenFile(Context context, String fileUrl) {
            // Tạo StorageReference từ đường dẫn file
            StorageReference fileRef = FirebaseStorage.getInstance().getReferenceFromUrl(fileUrl);
            try {
                // Tạo file tạm thời
                File localFile = File.createTempFile("tempFile", ".tmp");
                // Bắt đầu tải file
                fileRef.getFile(localFile).addOnSuccessListener(taskSnapshot -> {
                    // Khi tải file thành công, mở file bằng FileProvider
                    openFile(context, localFile);
                }).addOnFailureListener(e -> {
                    // Xử lý lỗi
                    Log.e(TAG, "Download file failed", e);
                });
            } catch (IOException e) {
                // Xử lý lỗi
                Log.e(TAG, "Create temp file failed", e);
            }
        }

        private void openFile(Context context, File file) {
            Log.d(TAG, "openFile: " + file);
            // Tạo Uri an toàn cho file sử dụng FileProvider

            Uri fileUri = FileProvider.getUriForFile(context, context.getApplicationContext().getPackageName() + ".provider", file);
            // Tạo Intent để mở file
            Intent intent = new Intent(Intent.ACTION_VIEW);
            // Set data và type cho Intent (type có thể thay đổi tùy vào loại file)
            intent.setDataAndType(fileUri, "application/*");
            // Cấp quyền đọc file cho Intent
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            // Start Intent
            context.startActivity(intent);
        }






    }
}
