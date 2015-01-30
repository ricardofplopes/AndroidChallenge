package com.rfplopes.androidchallenge;

import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.app.ProgressDialog;
import android.content.IntentSender;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;
import com.rfplopes.androidchallenge.fragments.CameraBeautifierFragment;
import com.rfplopes.androidchallenge.fragments.FingerPaintFragment;
import com.rfplopes.androidchallenge.fragments.InstagramTagMediaListFragment;
import com.rfplopes.androidchallenge.fragments.InstagramTagMediaListFragment.InstagramListCallbacks;
import com.rfplopes.androidchallenge.model.InstagramMediaObject;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;

public class MainActivity extends Activity implements InstagramListCallbacks,
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private static final String TAG = MainActivity.class.getSimpleName();

    /** Request code for launching the Intent to resolve Google Play services errors. */
    private static final int REQUEST_RESOLVE_ERROR = 1000;

    private static final String START_ACTIVITY_PATH = "/start-activity";
    private static final String IMAGE_PATH = "/image";
    private static final String IMAGE_KEY = "photo";

    // tabs and fragments
	private ActionBar.Tab tabInstagram, tabFingerPaint, tabBeautifier;
	private Fragment fragmentInstagram, fragmentFingerPaint, fragmentBeautifier;

    // google api client
    private GoogleApiClient mGoogleApiClient;
    private boolean mResolvingError = false;

    private ProgressDialog mProgressDialog;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		// instanciate fragments		
		fragmentInstagram = new InstagramTagMediaListFragment();
		fragmentFingerPaint = new FingerPaintFragment();
        fragmentBeautifier = new CameraBeautifierFragment();

		// setup action bar
		getActionBar().setDisplayHomeAsUpEnabled(false);
		getActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

		// create action bar tabs
		tabInstagram = getActionBar().newTab().setText(R.string.lbl_tab_instagram).setTabListener(new TabListener(fragmentInstagram));
		tabFingerPaint = getActionBar().newTab().setText(R.string.lbl_tab_fingerpaint).setTabListener(new TabListener(fragmentFingerPaint)); 
		tabBeautifier = getActionBar().newTab().setText(R.string.lbl_tab_camera_beautifier).setTabListener(new TabListener(fragmentBeautifier));

		// add tabs to action bar
		getActionBar().addTab(tabInstagram, true);
		getActionBar().addTab(tabFingerPaint);
        getActionBar().addTab(tabBeautifier);

        // init google api client
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
	}

	private class TabListener implements ActionBar.TabListener{

		private Fragment fragment;
		
		public TabListener(Fragment fragment) {
			this.fragment = fragment;
		}

		// When a tab is tapped, the FragmentTransaction replaces
		// the content of our main layout with the specified fragment;
		// that's why we declared an id for the main layout.
		@Override
		public void onTabSelected(Tab tab, FragmentTransaction ft) {
			ft.replace(R.id.container, fragment);
		}

		// When a tab is unselected, we have to hide it from the user's view. 
		@Override
		public void onTabUnselected(Tab tab, FragmentTransaction ft) {
			ft.remove(fragment);
		}

		// Nothing special here. Fragments already did the job.
		@Override
		public void onTabReselected(Tab tab, FragmentTransaction ft) {
			
		}
	}

    @Override
    protected void onStart() {
        super.onStart();
        if (!mResolvingError) {
            mGoogleApiClient.connect();
        }
    }

    @Override
    protected void onStop() {
        if (!mResolvingError) {
            mGoogleApiClient.disconnect();
        }

        // dismiss dialog if showing
        if(mProgressDialog != null && mProgressDialog.isShowing())
            mProgressDialog.dismiss();

        // cancel request for image
        Picasso.with(this).cancelRequest(mTarget);

        super.onStop();
    }

    @Override //ConnectionCallbacks
    public void onConnected(Bundle connectionHint) {
        Log.d(TAG, "Google API Client was connected");
        mResolvingError = false;
    }

    @Override //ConnectionCallbacks
    public void onConnectionSuspended(int cause) {
        Log.d(TAG, "Connection to Google API client was suspended");
    }

    @Override //OnConnectionFailedListener
    public void onConnectionFailed(ConnectionResult result) {
        if (mResolvingError) {
            // Already attempting to resolve an error.
            return;
        } else if (result.hasResolution()) {
            try {
                mResolvingError = true;
                result.startResolutionForResult(this, REQUEST_RESOLVE_ERROR);
            } catch (IntentSender.SendIntentException e) {
                // There was an error with the resolution intent. Try again.
                mGoogleApiClient.connect();
            }
        } else {
            Log.e(TAG, "Connection to Google API client has failed");
            mResolvingError = false;
        }
    }

    private Collection<String> getNodes() {
        HashSet<String> results = new HashSet<String>();
        NodeApi.GetConnectedNodesResult nodes =
                Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).await();

        for (Node node : nodes.getNodes()) {
            results.add(node.getId());
        }

        return results;
    }

    private void sendStartActivityMessage(String node) {
        Wearable.MessageApi.sendMessage(
                mGoogleApiClient, node, START_ACTIVITY_PATH, new byte[0]).setResultCallback(
                new ResultCallback<MessageApi.SendMessageResult>() {
                    @Override
                    public void onResult(MessageApi.SendMessageResult sendMessageResult) {
                        if (!sendMessageResult.getStatus().isSuccess()) {
                            Log.e(TAG, "Failed to send message with status code: "
                                    + sendMessageResult.getStatus().getStatusCode());
                        }
                    }
                }
        );
    }

    private class StartWearableActivityTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... args) {
            Collection<String> nodes = getNodes();
            for (String node : nodes) {
                sendStartActivityMessage(node);
            }
            return null;
        }
    }

	@Override
	public void onItemSelected(InstagramMediaObject item) {

        // if resolving error or not connected to wearable return
        if(mResolvingError || !mGoogleApiClient.isConnected())
            return;

        // trigger an AsyncTask that will query for a list of connected nodes and send a
        // "start-activity" message to each connected node.
        new StartWearableActivityTask().execute();

        // show progress dialog
        mProgressDialog = ProgressDialog.show(this, getString(R.string.lbl_progress_container_text), getString(R.string.lbl_sending_image_wear));

        // fetch image
        Picasso.with(this).load(item.getImageUrl()).resize(320, 320).into(mTarget);
	}


    // picasso target
    private Target mTarget = new Target() {
        @Override
        public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom loadedFrom) {
            if (null != bitmap) {
                sendPhoto(toAsset(bitmap));
            }
        }

        @Override
        public void onBitmapFailed(Drawable drawable) {
            Log.e(TAG, "An error occurred while getting image and sending it to wearable");

            // dismiss progress dialog
            if(mProgressDialog != null && mProgressDialog.isShowing())
                mProgressDialog.dismiss();
        }

        @Override
        public void onPrepareLoad(Drawable drawable) {
        }
    };


    /**
     * Builds an {@link com.google.android.gms.wearable.Asset} from a bitmap. The image that we get
     * back from the camera in "data" is a thumbnail size. Typically, your image should not exceed
     * 320x320 and if you want to have zoom and parallax effect in your app, limit the size of your
     * image to 640x400. Resize your image before transferring to your wearable device.
     */
    private static Asset toAsset(Bitmap bitmap) {
        ByteArrayOutputStream byteStream = null;
        try {
            byteStream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteStream);
            return Asset.createFromBytes(byteStream.toByteArray());
        } finally {
            if (null != byteStream) {
                try {
                    byteStream.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        }
    }

    /**
     * Sends the asset that was created form the photo we took by adding it to the Data Item store.
     */
    private void sendPhoto(Asset asset) {
        PutDataMapRequest dataMap = PutDataMapRequest.create(IMAGE_PATH);
        dataMap.getDataMap().putAsset(MainActivity.IMAGE_KEY, asset);
        dataMap.getDataMap().putLong("time", new Date().getTime());
        PutDataRequest request = dataMap.asPutDataRequest();
        Wearable.DataApi.putDataItem(mGoogleApiClient, request)
                .setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
                    @Override
                    public void onResult(DataApi.DataItemResult dataItemResult) {
                        Log.d(TAG, "Sending image was successful: " + dataItemResult.getStatus()
                                .isSuccess());

                        // dismiss progress dialog
                        if(mProgressDialog != null && mProgressDialog.isShowing())
                            mProgressDialog.dismiss();
                    }
                });

    }
	
}
