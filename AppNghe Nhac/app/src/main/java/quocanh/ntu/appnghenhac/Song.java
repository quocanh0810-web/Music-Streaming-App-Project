package quocanh.ntu.appnghenhac;

import java.io.Serializable;

public class Song implements Serializable {

    private String id;
    private String title;
    private String artist;
    private String imageURL;
    private String songURL;
    private String lyrics;

    // CONSTRUCTOR RỖNG CHO FIREBASE
    public Song() {
    }

    // CONSTRUCTOR ĐẦY ĐỦ
    public Song(String id,
                String title,
                String artist,
                String imageURL,
                String songURL,
                String lyrics) {

        this.id = id;
        this.title = title;
        this.artist = artist;
        this.imageURL = imageURL;
        this.songURL = songURL;
        this.lyrics = lyrics;
    }

    // GETTER
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

    public String getLyrics() {
        return lyrics;
    }

    // SETTER
    public void setId(String id) {
        this.id = id;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public void setImageURL(String imageURL) {
        this.imageURL = imageURL;
    }

    public void setSongURL(String songURL) {
        this.songURL = songURL;
    }

    public void setLyrics(String lyrics) {
        this.lyrics = lyrics;
    }

}