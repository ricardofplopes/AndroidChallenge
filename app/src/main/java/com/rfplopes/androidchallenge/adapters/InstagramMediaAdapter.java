package com.rfplopes.androidchallenge.adapters;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.content.Context;
import android.os.Looper;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.rfplopes.androidchallenge.BuildConfig;
import com.rfplopes.androidchallenge.R;
import com.rfplopes.androidchallenge.model.InstagramMediaObject;
import com.rfplopes.androidchallenge.utils.CircleTransform;
import com.squareup.picasso.Picasso;

public class InstagramMediaAdapter extends BaseAdapter {

    private final Context mContext;
	
    private List<InstagramMediaObject> mInstagramObjects = new ArrayList<InstagramMediaObject>();

    public InstagramMediaAdapter(Context context) {
    	this.mContext = context;
    }
    
    public void updateInstagramTagObjects(List<InstagramMediaObject> instagramObjects) {
    	// make sure the adapter is used only from one thread
    	if (BuildConfig.DEBUG) {
            if (Thread.currentThread() != Looper.getMainLooper().getThread()) {
                throw new IllegalStateException("This method should be called from the Main Thread");
            }
        }
        
    	this.mInstagramObjects = instagramObjects;
    	notifyDataSetChanged();
    }
    
    public void addInstagramTagObjects(List<InstagramMediaObject> instagramObjects) {
    	// make sure the adapter is used only from one thread
    	if (BuildConfig.DEBUG) {
            if (Thread.currentThread() != Looper.getMainLooper().getThread()) {
                throw new IllegalStateException("This method should be called from the Main Thread");
            }
        }
        
    	this.mInstagramObjects.addAll(instagramObjects);
    	notifyDataSetChanged();
    }
    
    @Override
    public int getCount() {
    	return mInstagramObjects.size();
    }

    @Override
    public InstagramMediaObject getItem(int position) {
    	return mInstagramObjects.get(position);
    }
    
    @Override
    public long getItemId(int position) {
    	return position;
    }
    
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
    	
        if (convertView == null) {
            convertView = LayoutInflater.from(mContext).inflate(R.layout.item_instagram_media, parent, false);
        }
        
        ImageView imgThumbnail = ViewHolder.get(convertView, R.id.img_thumbnail);
        TextView tvUsername =  ViewHolder.get(convertView, R.id.tv_username);
        TextView tvDate =  ViewHolder.get(convertView, R.id.tv_date);
        TextView tvCaption =  ViewHolder.get(convertView, R.id.tv_caption);
    	
        InstagramMediaObject obj = getItem(position);

        Picasso.with(mContext).load(obj.getThumbImageUrl()).resize(120, 120).transform(new CircleTransform()).into(imgThumbnail);
        tvUsername.setText(obj.getUsername());
        tvDate.setText(SimpleDateFormat.getInstance().format(new Date(obj.getCreatedTime() * 1000)));
        tvCaption.setText(obj.getCaption());
        
        return convertView;
    }

    public static class ViewHolder {
        // have a generic return type to reduce the casting noise in client code
        @SuppressWarnings("unchecked")
        public static <T extends View> T get(View view, int id) {
            SparseArray<View> viewHolder = (SparseArray<View>) view.getTag();
            if (viewHolder == null) {
                viewHolder = new SparseArray<View>();
                view.setTag(viewHolder);
            }
            View childView = viewHolder.get(id);
            if (childView == null) {
                childView = view.findViewById(id);
                viewHolder.put(id, childView);
            }
            return (T) childView;
        }
    }
    
}
