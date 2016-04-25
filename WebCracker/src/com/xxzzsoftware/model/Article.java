package com.xxzzsoftware.model;

import java.io.Serializable;

public class Article implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private String title;

	private String imgUrl;

	private String content;

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getImgUrl() {
		return imgUrl;
	}

	public void setImgUrl(String imgUrl) {
		this.imgUrl = imgUrl;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}
}
