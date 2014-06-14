package com.police.bjxj.bean;

import net.tsz.afinal.annotation.sqlite.Table;
import net.tsz.afinal.annotation.sqlite.Transient;

@Table(name = "download_book")
public class DownloadBook extends Book {
	@Transient
	public static final int STATE_UNSTART = 0;
	@Transient
	public static final int STATE_DOWNLOADING = 1;
	@Transient
	public static final int STATE_PAUSE = 2;
	@Transient
	public static final int STATE_SUCCESS = 3;
	@Transient
	public static final int STATE_FAILURE = 4;

	@Transient
	public int progress;

	public long startTime;
	public int state;
	public String filePath;
	public long fileLength;

	/** empty construct for finalDb. */
	public DownloadBook() {}

	public DownloadBook(Book book) {
		id = book.id;
		albumId = book.albumId;
		albumName = book.albumName;
		cover = book.cover;
		downloadUrl = book.downloadUrl;
		name = book.name;
	}

	public long getFileLength() {
		return fileLength;
	}

	public void setFileLength(long fileLength) {
		this.fileLength = fileLength;
	}

	public long getStartTime() {
		return startTime;
	}

	public void setStartTime(long startTime) {
		this.startTime = startTime;
	}

	public int getState() {
		return state;
	}

	public void setState(int state) {
		this.state = state;
	}

	public String getFilePath() {
		return filePath;
	}

	public void setFilePath(String filePath) {
		this.filePath = filePath;
	}

	public int getProgress() {
		return progress;
	}

	public void setProgress(int progress) {
		this.progress = progress;
	}

	public boolean canRead() {
		return state == STATE_SUCCESS;
	}

	public boolean isDownloading() {
		return state == STATE_DOWNLOADING;
	}
}
