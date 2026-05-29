package quocanh.ntu.appnghenhac;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.util.ArrayList;

public class BannerAdapter extends RecyclerView.Adapter<BannerAdapter.BannerViewHolder> {
    private Context context;
    private ArrayList<Song> songList;

    public BannerAdapter(Context context, ArrayList<Song> songList) {
        this.context = context;
        this.songList = songList;
    }

    @NonNull
    @Override
    public BannerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Tận dụng trực tiếp ImageView để làm banner toàn màn hình trượt mượt mà
        ImageView imageView = new ImageView(context);
        imageView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        return new BannerViewHolder(imageView);
    }

    @Override
    public void onBindViewHolder(@NonNull BannerViewHolder holder, int position) {
        if (songList != null && !songList.isEmpty()) {
            Song song = songList.get(position % songList.size()); // Vòng lặp vô tận
            // Dùng thư viện Glide load mượt link ảnh trực tuyến từ Firebase Realtime Database về máy
            Glide.with(context).load(song.getImageURL()).into((ImageView) holder.itemView);
        }
    }

    @Override
    public int getItemCount() {
        // Giới hạn hiển thị tối đa 5 banner lớn chạy trượt
        return songList != null ? Math.min(songList.size(), 5) : 0;
    }

    public static class BannerViewHolder extends RecyclerView.ViewHolder {
        public BannerViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }
}