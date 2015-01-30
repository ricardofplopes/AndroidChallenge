package com.rfplopes.androidchallenge.model;

import java.util.List;

public class InstagramTagResponse {

	private String nextMaxTagId;
	private List<InstagramMediaObject> instagramTagObjects;

	public InstagramTagResponse() {
	}

	public String getNextMaxTagId() {
		return nextMaxTagId;
	}

	public void setNextMaxTagId(String nextMaxTagId) {
		this.nextMaxTagId = nextMaxTagId;
	}

	public List<InstagramMediaObject> getInstagramTagObjects() {
		return instagramTagObjects;
	}

	public void setInstagramTagObjects(List<InstagramMediaObject> instagramTagObjects) {
		this.instagramTagObjects = instagramTagObjects;
	}
	
	
}
