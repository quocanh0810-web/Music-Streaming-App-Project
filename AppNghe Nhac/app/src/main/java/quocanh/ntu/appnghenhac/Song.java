package quocanh.ntu.appnghenhac;

public class Song {
    private String id;
    private String title;
    private String artist;
    private String imageURL;
    private String songURL;
    public Song() {
    }
    public Song(String id, String title, String artist, String imageURL, String songURL) {
        this.id = id;
        this.title = title;
        this.artist = artist;
        this.imageURL = imageURL;
        this.songURL = songURL;
    }
    public String getId() {
        return id;
    }
    public String getTitle() {
        return title;
    }
    public String getArtist() {
        return artist;
    }
    public String getImageURL() {
        return imageURL;
    }
    public String getSongURL() {
        return songURL;
    }
}