package quocanh.ntu.appnghenhac;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.util.ArrayList;

public class BannerAdapter extends RecyclerView.Adapter<BannerAdapter.BannerViewHolder> {
    private final Context context;
    private final ArrayList<Song> songList;

    public BannerAdapter(Context context, ArrayList<Song> songList) {
        this.context = context;
        this.songList = songList;
    }

    @NonNull
    @Override
    public BannerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        CardView cardView = new CardView(context);
        ViewGroup.LayoutParams layoutParams = new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
        cardView.setLayoutParams(layoutParams);
        cardView.setRadius(32);
        cardView.setCardElevation(8);
        cardView.setClipToPadding(false);

        ImageView imageView = new ImageView(context);
        imageView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        imageView.setId(View.generateViewId());

        cardView.addView(imageView);
        return new BannerViewHolder(cardView);
    }

    @Override
    public void onBindViewHolder(@NonNull BannerViewHolder holder, int position) {
        if (songList != null && !songList.isEmpty()) {
            // Vòng lặp cho banner
            Song song = songList.get(position % songList.size());

            // Lấy ImageView nằm bên trong CardView ra để nạp ảnh
            CardView cardView = (CardView) holder.itemView;
            ImageView imageView = (ImageView) cardView.getChildAt(0);

            // Nạp ảnh online từ Firebase thông qua thư viện Glide mượt mà
            Glide.with(context)
                    .load(song.getImageURL())
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .error(android.R.drawable.ic_menu_report_image)
                    .into(imageView);
        }
    }

    @Override
    public int getItemCount() {
        // Giới hạn hiển thị tối đa 5 banner
        return songList != null ? Math.min(songList.size(), 5) : 0;
    }

    public static class BannerViewHolder extends RecyclerView.ViewHolder {
        public BannerViewHolder(@NonNull CardView itemView) {
            super(itemView);
        }
    }
}