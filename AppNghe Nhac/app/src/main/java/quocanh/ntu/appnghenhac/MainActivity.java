package quocanh.ntu.appnghenhac;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.CompositePageTransformer;
import androidx.viewpager2.widget.MarginPageTransformer;
import androidx.viewpager2.widget.ViewPager2;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {
    // Thành phần giao diện
    private RecyclerView recyclerViewSongs;
    private RecyclerView recyclerViewNewSongs;
    private RecyclerView recyclerViewRecent;
    private RecyclerView recyclerViewFavorite;
    private ViewPager2 viewPagerBanner;
    private LinearLayout layoutIndicators;

    private TextView txtRecentHeader;
    private TextView txtFavoriteHeader;
    private EditText edtSearch;

    // Danh sách dữ liệu
    private final ArrayList<Song> songList = new ArrayList<>();
    private final ArrayList<Song> filteredPopularList = new ArrayList<>();
    private final ArrayList<Song> filteredNewList = new ArrayList<>();
    private final ArrayList<Song> recentList = new ArrayList<>();
    private final ArrayList<Song> favoriteList = new ArrayList<>();

    // Bộ điều phối giao diện (Adapters)
    private SongAdapter popularAdapter;
    private SongAdapter newSongsAdapter;
    private SongAdapter recentAdapter;
    private SongAdapter favoriteAdapter;
    private BannerAdapter bannerAdapter;

    private FirebaseDatabase database;
    private DatabaseReference reference;

    // Quản lý luồng chạy Auto-slide cho Banner an toàn
    private final Handler sliderHandler = new Handler(Looper.getMainLooper());
    private Runnable sliderRunnable;

    // Quản lý luồng delay tìm kiếm
    private final Handler searchHandler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 1. Ánh xạ các View từ XML
        initViews();

        // 2. Cấu hình các danh sách hiển thị (RecyclerViews & ViewPager2)
        setupComponents();

        // 3. Khởi tạo luồng xử lý tự động trượt Banner
        initSliderRunnable();

        // 4. Kết nối và đồng bộ dữ liệu Realtime từ Firebase
        initFirebaseConnection();

        // 5. Cấu hình thanh tìm kiếm Search Bar thông minh (Có Debounce)
        setupSearchBar();
    }

    private void initViews() {
        recyclerViewSongs = findViewById(R.id.recyclerViewSongs);
        recyclerViewNewSongs = findViewById(R.id.recyclerViewNewSongs);
        recyclerViewRecent = findViewById(R.id.recyclerViewRecent);
        recyclerViewFavorite = findViewById(R.id.recyclerViewFavorite);
        viewPagerBanner = findViewById(R.id.viewPagerBanner);
        layoutIndicators = findViewById(R.id.layoutIndicators);
        txtRecentHeader = findViewById(R.id.txtRecentHeader);
        txtFavoriteHeader = findViewById(R.id.txtFavoriteHeader);
        edtSearch = findViewById(R.id.edtSearch);
    }

    private void setupComponents() {
        // Cấu hình danh sách lưới 2 cột cho Top thịnh hành
        recyclerViewSongs.setLayoutManager(new GridLayoutManager(this, 2));
        popularAdapter = new SongAdapter(this, filteredPopularList);
        recyclerViewSongs.setAdapter(popularAdapter);

        // Cấu hình danh sách lưới 2 cột cho Bài hát mới
        recyclerViewNewSongs.setLayoutManager(new GridLayoutManager(this, 2));
        newSongsAdapter = new SongAdapter(this, filteredNewList);
        recyclerViewNewSongs.setAdapter(newSongsAdapter);

        // Cấu hình Cuộn ngang (Nghe gần đây)
        recyclerViewRecent.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        recentAdapter = new SongAdapter(this, recentList, true);
        recyclerViewRecent.setAdapter(recentAdapter);

        // Cấu hình Cuộn ngang (Bài hát yêu thích)
        recyclerViewFavorite.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        favoriteAdapter = new SongAdapter(this, favoriteList, true);
        recyclerViewFavorite.setAdapter(favoriteAdapter);

        // Cấu hình Banner chuyển động 3D
        bannerAdapter = new BannerAdapter(this, songList);
        viewPagerBanner.setAdapter(bannerAdapter);

        CompositePageTransformer transformer = new CompositePageTransformer();
        transformer.addTransformer(new MarginPageTransformer(30));
        transformer.addTransformer((page, position) -> {
            float r = 1 - Math.abs(position);
            page.setScaleY(0.85f + r * 0.15f);
        });
        viewPagerBanner.setPageTransformer(transformer);

        // đổi trang trên Banner để điều khiển các chấm tròn
        viewPagerBanner.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                updateIndicators(position);
                // Đẩy bộ đếm thời gian lướt trang ra xa nếu người dùng đang chủ động vuốt tay
                sliderHandler.removeCallbacks(sliderRunnable);
                sliderHandler.postDelayed(sliderRunnable, 3000);
            }
        });
    }

    private void initSliderRunnable() {
        sliderRunnable = new Runnable() {
            @Override
            public void run() {
                int count = bannerAdapter.getItemCount();
                if (count > 0) {
                    int nextItem = (viewPagerBanner.getCurrentItem() + 1) % count;
                    viewPagerBanner.setCurrentItem(nextItem, true);
                }
            }
        };
    }

    private void initFirebaseConnection() {
        database = FirebaseDatabase.getInstance("https://appnghenhac-8e5b8-default-rtdb.firebaseio.com/");
        reference = database.getReference("Songs");

        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                songList.clear();
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    Song song = dataSnapshot.getValue(Song.class);
                    if (song != null) {
                        song.setId(dataSnapshot.getKey());
                        songList.add(song);
                    }
                }

                // Sắp xếp bài hát theo ký tự ABC để hiển thị ngay ngắn
                Collections.sort(songList, (s1, s2) -> {
                    String t1 = s1.getTitle() != null ? s1.getTitle() : "";
                    String t2 = s2.getTitle() != null ? s2.getTitle() : "";
                    return t1.compareToIgnoreCase(t2);
                });

                bannerAdapter.notifyDataSetChanged();
                setupBannerIndicators(bannerAdapter.getItemCount());

                // Đổ dữ liệu ban đầu vào 2 ô lưới bài hát
                filter(edtSearch.getText().toString());

                // Đồng bộ danh mục yêu thích theo thiết bị (ANDROID_ID)
                syncFavoriteSongs();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void syncFavoriteSongs() {
        String userId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        DatabaseReference favRef = database.getReference("Users").child(userId).child("favorites");

        favRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot favSnapshot) {
                favoriteList.clear();
                for (DataSnapshot ds : favSnapshot.getChildren()) {
                    String favSongId = ds.getKey();
                    for (Song song : songList) {
                        if (song.getId() != null && song.getId().equals(favSongId)) {
                            favoriteList.add(song);
                        }
                    }
                }

                // Ẩn/Hiện khu vực Bài hát yêu thích thông minh
                if (favoriteList.isEmpty()) {
                    txtFavoriteHeader.setVisibility(View.GONE);
                    recyclerViewFavorite.setVisibility(View.GONE);
                } else {
                    txtFavoriteHeader.setVisibility(View.VISIBLE);
                    recyclerViewFavorite.setVisibility(View.VISIBLE);
                }
                favoriteAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void setupSearchBar() {
        edtSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Hủy luồng tìm kiếm cũ nếu người dùng vẫn đang gõ chữ liên tục
                if (searchRunnable != null) {
                    searchHandler.removeCallbacks(searchRunnable);
                }

                // Tạo luồng tìm kiếm mới, đợi sau 300ms dừng gõ mới thực thi (Debounce)
                searchRunnable = () -> filter(s.toString());
                searchHandler.postDelayed(searchRunnable, 300);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    // VẼ CHẤM TRÒN
    private void setupBannerIndicators(int count) {
        layoutIndicators.removeAllViews();
        if (count <= 0) return;

        ImageView[] indicators = new ImageView[count];
        int sizeInPx = (int) (6 * getResources().getDisplayMetrics().density);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(sizeInPx, sizeInPx);
        params.setMargins(8, 0, 8, 0);

        // Tạo sẵn kiểu dáng hình tròn mờ mặc định
        GradientDrawable inactiveShape = new GradientDrawable();
        inactiveShape.setShape(GradientDrawable.OVAL);
        inactiveShape.setColor(Color.parseColor("#44FFFFFF")); // Trắng mờ

        for (int i = 0; i < count; i++) {
            indicators[i] = new ImageView(this);
            indicators[i].setImageDrawable(inactiveShape);
            layoutIndicators.addView(indicators[i], params);
        }

        // Tạo hình dáng tròn cho trang đầu tiên đang được chọn
        if (indicators.length > 0) {
            GradientDrawable activeShape = new GradientDrawable();
            activeShape.setShape(GradientDrawable.OVAL);
            activeShape.setColor(Color.parseColor("#1DB954")); // Màu xanh Spotify rực rỡ
            indicators[0].setImageDrawable(activeShape);
        }
    }

    // CẬP NHẬT TRẠNG THÁI CHẤM TRÒN KHI LƯỚT BANNER
    private void updateIndicators(int position) {
        int childCount = layoutIndicators.getChildCount();

        GradientDrawable activeShape = new GradientDrawable();
        activeShape.setShape(GradientDrawable.OVAL);
        activeShape.setColor(Color.parseColor("#1DB954"));

        GradientDrawable inactiveShape = new GradientDrawable();
        inactiveShape.setShape(GradientDrawable.OVAL);
        inactiveShape.setColor(Color.parseColor("#44FFFFFF"));

        for (int i = 0; i < childCount; i++) {
            ImageView imageView = (ImageView) layoutIndicators.getChildAt(i);
            if (imageView != null) {
                if (i == position) {
                    imageView.setImageDrawable(activeShape);
                    imageView.animate().scaleX(1.2f).scaleY(1.2f).setDuration(150).start();
                } else {
                    imageView.setImageDrawable(inactiveShape);
                    imageView.animate().scaleX(1.0f).scaleY(1.0f).setDuration(150).start();
                }
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Hủy toàn bộ luồng chạy ngầm khi người dùng thoát ứng dụng để bảo vệ bộ nhớ RAM
        sliderHandler.removeCallbacks(sliderRunnable);
        if (searchRunnable != null) {
            searchHandler.removeCallbacks(searchRunnable);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Tái khởi động tự động trượt khi người dùng mở lại app
        sliderHandler.postDelayed(sliderRunnable, 3000);
    }

    private String removeAccent(String s) {
        if (s == null) return "";
        String temp = Normalizer.normalize(s, Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        return pattern.matcher(temp).replaceAll("").replaceAll("Đ", "D").replaceAll("đ", "d");
    }

    private void filter(String text) {
        filteredPopularList.clear();
        filteredNewList.clear();

        ArrayList<Song> temporaryList = new ArrayList<>();

        if (text == null || text.trim().isEmpty()) {
            temporaryList.addAll(songList);
        } else {
            String query = removeAccent(text.toLowerCase().trim());
            for (Song song : songList) {
                String title = song.getTitle() != null ? song.getTitle() : "";
                String artist = song.getArtist() != null ? song.getArtist() : "";

                if (removeAccent(title.toLowerCase()).contains(query) ||
                        removeAccent(artist.toLowerCase()).contains(query)) {
                    temporaryList.add(song);
                }
            }
        }

        int halfSize = temporaryList.size() / 2;
        for (int i = 0; i < temporaryList.size(); i++) {
            if (i < halfSize) {
                filteredPopularList.add(temporaryList.get(i));
            } else {
                filteredNewList.add(temporaryList.get(i));
            }
        }

        popularAdapter.notifyDataSetChanged();
        newSongsAdapter.notifyDataSetChanged();
    }

    public void addToRecent(Song song) {
        if (song == null) return;
        for (int i = 0; i < recentList.size(); i++) {
            if (recentList.get(i).getTitle() != null && recentList.get(i).getTitle().equals(song.getTitle())) {
                recentList.remove(i);
                break;
            }
        }
        recentList.add(0, song);
        if (recentList.size() > 5) {
            recentList.remove(recentList.size() - 1);
        }
        txtRecentHeader.setVisibility(View.VISIBLE);
        recyclerViewRecent.setVisibility(View.VISIBLE);
        recentAdapter.notifyDataSetChanged();
    }
}