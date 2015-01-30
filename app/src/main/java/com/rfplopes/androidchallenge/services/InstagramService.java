package com.rfplopes.androidchallenge.services;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.Header;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.rfplopes.androidchallenge.model.InstagramMediaObject;
import com.rfplopes.androidchallenge.model.InstagramTagResponse;


public class InstagramService {

	private static final String TAG = InstagramService.class.getSimpleName();
	
	private static final String BASE_URL = "https://api.instagram.com/v1";
	private static final String INSTAGRAM_TAG_SERVICE_URL = BASE_URL + "/tags/{0}/media/recent";

    private static final String CLIENT_ID = "3b942602c90b4cd2946942edd567f1ec";
	private static final int MAX_ELEMENTS = 20;

    // the http client
	private static final AsyncHttpClient client = new AsyncHttpClient();

    // to cancel ongoing requests
	public static void cancelRequests(Context context) {
		client.cancelRequests(context, true);
	}	

    // load instagram tags media asynchronous
	public static void loadTagsMediaRecent(Context context, String tag, String maxTagId, final RequestCompleteHandler requestCompleteHandler) {

    	RequestParams params = new RequestParams();
    	params.put("client_id", InstagramService.CLIENT_ID);
    	params.put("count", InstagramService.MAX_ELEMENTS);
    	
    	if(!TextUtils.isEmpty(maxTagId))
    		params.put("max_tag_id", maxTagId);
    	
    	String requestURL = MessageFormat.format(INSTAGRAM_TAG_SERVICE_URL, tag);
    	client.get(requestURL, params, new JsonHttpResponseHandler() {

        	@Override
        	public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
        		super.onSuccess(statusCode, headers, response);

        		InstagramTagResponse instagramTagResponse = new InstagramTagResponse();
        		List<InstagramMediaObject> instagramObjects = new ArrayList<InstagramMediaObject>();
        		
        		JSONArray dataObjArrayJSON = null;
        		try {
        			
        			instagramTagResponse.setNextMaxTagId(response.getJSONObject("pagination").getString("next_max_tag_id"));
        			
        			dataObjArrayJSON = response.getJSONArray("data");
					for (int i = 0; i < dataObjArrayJSON.length(); i++) {
						
						JSONObject dataObjJSON = dataObjArrayJSON.getJSONObject(i);
						InstagramMediaObject obj = new InstagramMediaObject();
						obj.setId(dataObjJSON.getString("id"));
						obj.setUsername(dataObjJSON.getJSONObject("user").getString("username"));

						if (dataObjJSON.optJSONObject("caption") != null) {
							obj.setCaption(dataObjJSON.getJSONObject("caption").getString("text"));
						} else {
							obj.setCaption("");
						}
						obj.setThumbImageUrl(dataObjJSON.getJSONObject("images").getJSONObject("thumbnail").getString("url"));
						obj.setImageUrl(dataObjJSON.getJSONObject("images").getJSONObject("standard_resolution").getString("url"));
						obj.setImageHeight(dataObjJSON.getJSONObject("images").getJSONObject("standard_resolution").getInt("height"));
						obj.setImageWidth(dataObjJSON.getJSONObject("images").getJSONObject("standard_resolution").getInt("width"));
						obj.setCreatedTime(dataObjJSON.getLong("created_time"));
						obj.setLocation(dataObjJSON.getString("location"));
						instagramObjects.add(obj);
					}
					
					instagramTagResponse.setInstagramTagObjects(instagramObjects);

    				// Notify handler
	        		if(requestCompleteHandler != null)
	        			requestCompleteHandler.onSuccess(instagramTagResponse);
	        		
				} catch (JSONException e) {
					// Notify handler
	        		if(requestCompleteHandler != null)
	        			requestCompleteHandler.onFailure(0, e);	
				}
        	}
        	
        	@Override
			public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
				super.onFailure(statusCode, headers, throwable, errorResponse);
				
        		Log.e(TAG, "Service call failed", throwable);
        		
        		// Notify handler
        		if(requestCompleteHandler != null)
        			requestCompleteHandler.onFailure(statusCode, throwable);
			}
        	
        	@Override
			public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
				super.onFailure(statusCode, headers, responseString, throwable);
				
        		Log.e(TAG, "Service call failed", throwable);
        		
        		// Notify handler
        		if(requestCompleteHandler != null)
        			requestCompleteHandler.onFailure(statusCode, throwable);
			}
        	
        });
	}
	
    public interface RequestCompleteHandler {
    	
    	public void onSuccess(InstagramTagResponse response);
    	public void onFailure(int statusCode, Throwable error);
 
    }
    
   
    
}
