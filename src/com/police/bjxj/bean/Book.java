package com.police.bjxj.bean;

import net.tsz.afinal.annotation.sqlite.Id;
import net.tsz.afinal.annotation.sqlite.Table;

import org.json.JSONObject;

@Table(name = "favorite")
public class Book {
	@Id
	public long id;
	public long albumId;
	public String albumName;
	public String name;
	public String cover;
	public String downloadUrl;

	public String getDownloadUrl() {
		return downloadUrl;
	}

	public void setDownloadUrl(String downloadUrl) {
		this.downloadUrl = downloadUrl;
	}

	public long getAlbumId() {
		return albumId;
	}

	public void setAlbumId(long albumId) {
		this.albumId = albumId;
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getAlbumName() {
		return albumName;
	}

	public void setAlbumName(String albumName) {
		this.albumName = albumName;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getCover() {
		return cover;
	}

	public void setCover(String cover) {
		this.cover = cover;
	}

	@Override
	public String toString() {
		return name + "[" + id + "]";
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof Book) {
			return this.id == ((Book) o).getId();
		}
		return super.equals(o);
	}
	
	public static Book parseFrom(JSONObject json) {
		if(json==null) return null;
		
		Book book = new Book();
		book.id = json.optLong("id");
		book.albumId = json.optLong("albumid");
		book.albumName = json.optString("albumname");
		book.name = json.optString("name");
		book.cover = json.optString("cover");
		book.downloadUrl = json.optString("url");
		return book;
	}
}
