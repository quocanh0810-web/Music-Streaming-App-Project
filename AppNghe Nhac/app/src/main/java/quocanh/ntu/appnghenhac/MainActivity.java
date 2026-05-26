package quocanh.ntu.appnghenhac;

import android.os.Bundle;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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
    RecyclerView recyclerViewSongs;
    RecyclerView recyclerViewRecent;
    RecyclerView recyclerViewFavorite;
    TextView txtRecentHeader;
    TextView txtFavoriteHeader;
    EditText edtSearch;
    ArrayList<Song> songList = new ArrayList<>();
    ArrayList<Song> filteredList = new ArrayList<>();
    ArrayList<Song> recentList = new ArrayList<>();
    ArrayList<Song> favoriteList = new ArrayList<>();
    SongAdapter adapter;
    SongAdapter recentAdapter;
    SongAdapter favoriteAdapter;

    FirebaseDatabase database;
    DatabaseReference reference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Ánh xạ view
        recyclerViewSongs = findViewById(R.id.recyclerViewSongs);
        recyclerViewRecent = findViewById(R.id.recyclerViewRecent);
        recyclerViewFavorite = findViewById(R.id.recyclerViewFavorite);
        txtRecentHeader = findViewById(R.id.txtRecentHeader);
        txtFavoriteHeader = findViewById(R.id.txtFavoriteHeader);
        edtSearch = findViewById(R.id.edtSearch);

        // RECYCLER VIEW Chính
        recyclerViewSongs.setLayoutManager(new LinearLayoutManager(this));
        adapter = new SongAdapter(this, filteredList);
        recyclerViewSongs.setAdapter(adapter);

        // RECYCLER VIEW Nghe gần đây
        recyclerViewRecent.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        recentAdapter = new SongAdapter(this, recentList, true);
        recyclerViewRecent.setAdapter(recentAdapter);

        txtRecentHeader.setVisibility(View.GONE);
        recyclerViewRecent.setVisibility(View.GONE);

        // RECYCLER VIEW Bài hát yêu thích
        recyclerViewFavorite.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        favoriteAdapter = new SongAdapter(this, favoriteList, true);
        recyclerViewFavorite.setAdapter(favoriteAdapter);

        txtFavoriteHeader.setVisibility(View.GONE);
        recyclerViewFavorite.setVisibility(View.GONE);

        // FIREBASE CONNECTION
        database = FirebaseDatabase.getInstance("https://appnghenhac-8e5b8-default-rtdb.firebaseio.com/");
        reference = database.getReference("Songs");

        // LẤY DỮ LIỆU REALTIME
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                songList.clear();

                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    Song song = dataSnapshot.getValue(Song.class);
                    if (song != null) {
                        songList.add(song);
                    }
                }

                // SẮP XẾP AN TOÀN
                Collections.sort(songList, new Comparator<Song>() {
                    @Override
                    public int compare(Song s1, Song s2) {
                        String t1 = s1.getTitle() != null ? s1.getTitle() : "";
                        String t2 = s2.getTitle() != null ? s2.getTitle() : "";
                        return t1.compareToIgnoreCase(t2);
                    }
                });

                filter(edtSearch.getText().toString());

                // ĐỒNG BỘ DANH SÁCH YÊU THÍCH TỪ USER
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

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });

        // SEARCH BAR
        edtSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filter(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private String removeAccent(String s) {
        if (s == null) return "";
        String temp = Normalizer.normalize(s, Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        return pattern.matcher(temp).replaceAll("").replaceAll("Đ", "D").replaceAll("đ", "d");
    }

    private void filter(String text) {
        filteredList.clear();
        if (text == null || text.trim().isEmpty()) {
            filteredList.addAll(songList);
        } else {
            String query = removeAccent(text.toLowerCase().trim());
            for (Song song : songList) {
                String title = song.getTitle() != null ? song.getTitle() : "";
                String artist = song.getArtist() != null ? song.getArtist() : "";

                if (removeAccent(title.toLowerCase()).contains(query) ||
                        removeAccent(artist.toLowerCase()).contains(query)) {
                    filteredList.add(song);
                }
            }
        }
        adapter.notifyDataSetChanged();
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