package quocanh.ntu.appnghenhac;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class SongAdapter extends RecyclerView.Adapter<SongAdapter.SongViewHolder> {

    Context context;
    ArrayList<Song> songList;
    private boolean isHorizontal = false;

    private DatabaseReference favRef;
    private String userId;

    // CONSTRUCTOR 1: DANH SÁCH DỌC
    public SongAdapter(Context context, ArrayList<Song> songList) {
        this.context = context;
        this.songList = songList;
        initFirebaseFavorite();
    }

    // CONSTRUCTOR 2: DANH SÁCH NGANG
    public SongAdapter(Context context, ArrayList<Song> songList, boolean isHorizontal) {
        this.context = context;
        this.songList = songList;
        this.isHorizontal = isHorizontal;
        initFirebaseFavorite();
    }

    // Khởi tạo thông tin User
    private void initFirebaseFavorite() {
        try {
            userId = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
            favRef = FirebaseDatabase.getInstance("https://appnghenhac-8e5b8-default-rtdb.firebaseio.com/")
                    .getReference("Users").child(userId).child("favorites");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @NonNull
    @Override
    public SongViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_song, parent, false);

        if (isHorizontal) {
            ViewGroup.LayoutParams params = view.getLayoutParams();
            params.width = (int) (280 * context.getResources().getDisplayMetrics().density);
            view.setLayoutParams(params);
        }
        return new SongViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SongViewHolder holder, int position) {
        int currentPos = holder.getAdapterPosition();
        if (currentPos == RecyclerView.NO_POSITION) return;

        Song song = songList.get(currentPos);
        if (song == null) return;

        // Hiển thị chữ chữ thường
        holder.txtTitle.setText(song.getTitle() != null ? song.getTitle() : "Unknown Song");
        holder.txtArtist.setText(song.getArtist() != null ? song.getArtist() : "Unknown Artist");

        // LOAD ẢNH BIỂU DIỄN
        String imgStr = song.getImageURL();
        if (imgStr != null && !imgStr.isEmpty()) {
            if (imgStr.startsWith("data:image")) {
                try {
                    String base64Image = imgStr.substring(imgStr.indexOf(",") + 1);
                    byte[] decodedString = android.util.Base64.decode(base64Image, android.util.Base64.DEFAULT);
                    Glide.with(context).asBitmap().load(decodedString).placeholder(R.drawable.ic_launcher_background).centerCrop().into(holder.imgSong);
                } catch (Exception e) {
                    holder.imgSong.setImageResource(R.drawable.ic_launcher_background);
                }
            } else {
                Glide.with(context).load(imgStr).placeholder(R.drawable.ic_launcher_background).centerCrop().into(holder.imgSong);
            }
        } else {
            holder.imgSong.setImageResource(R.drawable.ic_launcher_background);
        }

        // LOGIC XỬ LÝ NÚT TRÁI TIM
        if (song.getId() != null && holder.imgFavorite != null) {
            // Kiểm tra trạng thái bài hát đã thích chưa từ Firebase
            favRef.child(song.getId()).addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        // Nếu tồn tại trên mục yêu thích -> Hiện ngôi sao/trái tim sáng màu bật
                        holder.imgFavorite.setImageResource(android.R.drawable.btn_star_big_on);
                    } else {
                        // Nếu không thích -> Trả về màu tắt
                        holder.imgFavorite.setImageResource(android.R.drawable.btn_star_big_off);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {}
            });

            // Sự kiện khi bấm vào nút Thả tim
            holder.imgFavorite.setOnClickListener(v -> {
                favRef.child(song.getId()).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            // Đang thích thì -> Hủy thích
                            favRef.child(song.getId()).removeValue();
                        } else {
                            // Chưa thích thì -> Thêm vào yêu thích
                            favRef.child(song.getId()).setValue(true);
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
            });
        }

        // ANIMATION FADE
        AlphaAnimation animation = new AlphaAnimation(0f, 1f);
        animation.setDuration(400);
        holder.itemView.startAnimation(animation);

        if (isHorizontal) {
            holder.cardSong.setCardBackgroundColor(Color.parseColor("#24243A"));
        } else {
            holder.cardSong.setCardBackgroundColor(Color.parseColor("#1A1A2E"));
        }

        // SỰ KIỆN CLICK ITEM ĐỂ PHÁT NHẠC
        holder.itemView.setOnClickListener(v -> {
            if (context instanceof MainActivity) {
                ((MainActivity) context).addToRecent(song);
            }
            Intent intent = new Intent(context, MusicPlayerActivity.class);
            intent.putExtra("songList", songList);
            intent.putExtra("position", currentPos);
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return songList != null ? songList.size() : 0;
    }

    public static class SongViewHolder extends RecyclerView.ViewHolder {
        ImageView imgSong;
        ImageView imgFavorite;
        TextView txtTitle, txtArtist;
        CardView cardSong;

        public SongViewHolder(@NonNull View itemView) {
            super(itemView);
            imgSong = itemView.findViewById(R.id.imgSong);
            txtTitle = itemView.findViewById(R.id.txtTitle);
            txtArtist = itemView.findViewById(R.id.txtArtist);
            cardSong = itemView.findViewById(R.id.cardSong);
            imgFavorite = itemView.findViewById(R.id.imgFavorite);
        }
    }
}