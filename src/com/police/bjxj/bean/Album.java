package com.police.bjxj.bean;

import org.json.JSONObject;

public class Album {
	long id;
	String name;
	String cover;

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
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

	public static Album parseFrom(JSONObject json) {
		Album album = new Album();
		album.id = json.optLong("id");
		album.name = json.optString("name");
		album.cover = json.optString("cover");
		return album;
	}

}
