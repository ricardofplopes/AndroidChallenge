package com.rfplopes.androidchallenge.fragments;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ListFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.rfplopes.androidchallenge.R;
import com.rfplopes.androidchallenge.adapters.EndlessAdapter;
import com.rfplopes.androidchallenge.adapters.EndlessAdapter.AdapterAppendTask;
import com.rfplopes.androidchallenge.adapters.EndlessAdapter.ResultHandler;
import com.rfplopes.androidchallenge.adapters.InstagramMediaAdapter;
import com.rfplopes.androidchallenge.model.InstagramMediaObject;
import com.rfplopes.androidchallenge.model.InstagramTagResponse;
import com.rfplopes.androidchallenge.services.InstagramService;
import com.rfplopes.androidchallenge.services.InstagramService.RequestCompleteHandler;
import com.rfplopes.androidchallenge.services.ServiceResultEnum;


public class InstagramTagMediaListFragment extends ListFragment {

	private static final String TAG = InstagramTagMediaListFragment.class.getSimpleName();

	/**
	 * The fragment's current callback object, which is notified of list item
	 * clicks.
	 */
	private InstagramListCallbacks mCallbacks = sDummyCallbacks;
	
	/**
	 * A callback interface that all activities containing this fragment must
	 * implement. This mechanism allows activities to be notified of item
	 * selections.
	 */
	public interface InstagramListCallbacks {
		/**
		 * Callback for when an item has been selected.
		 */
		public void onItemSelected(InstagramMediaObject item);
	}
	
	/**
	 * A dummy implementation of the {@link InstagramListCallbacks} interface that does
	 * nothing. Used only when this fragment is not attached to an activity.
	 */
	private static InstagramListCallbacks sDummyCallbacks = new InstagramListCallbacks() {
		@Override
		public void onItemSelected(InstagramMediaObject item) {}
	};

    // instagram tag for the service
	private static final String INSTAGRAM_TAG = "pink";

    // adapters
	private InstagramMediaAdapter mListViewAdapter = null;
	private InstagramEndlessAdapter mListViewEndlessAdapter = null;	

    // service data
	private String mNextMaxTagId = null;
	private ServiceResultEnum mLastServiceResult = null;
	private List<InstagramMediaObject> mFetchedList = null;
	private List<InstagramMediaObject> mLastFetchedList = null;

    // help with fragment lifecyle
    private boolean mIsDestroyed = false;
	
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);

		// Activities containing this fragment must implement its callbacks.
		if (!(activity instanceof InstagramListCallbacks)) {
			throw new IllegalStateException("Activity must implement fragment's callbacks.");
		}

		mCallbacks = (InstagramListCallbacks) activity;
	}

	@Override
	public void onDetach() {
		super.onDetach();

		// Reset the active callbacks interface to the dummy implementation.
		mCallbacks = sDummyCallbacks;
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

        mIsDestroyed = false;

        // init list
		mFetchedList = new ArrayList<InstagramMediaObject>();
		
		// retain instance
		setRetainInstance(true);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		return inflater.inflate(R.layout.base_list_layout, container, false);
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		
        // Set refresh button
        Button btnRefresh = (Button) getView().findViewById(R.id.btn_refresh);
        btnRefresh.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				showLoadingProcessingService();
				refreshList();
			}
		});
		
		// initialize List
		initializeList();
	};
	
	private void initializeList() {
		
        // clean list adapter
		setListAdapter(null);

        // Set choice mode
        getListView().setChoiceMode(ListView.CHOICE_MODE_SINGLE);

        // show loading
		showLoadingProcessingService();
		
		// fetch instagram media objects
		InstagramService.loadTagsMediaRecent(getActivity(), InstagramTagMediaListFragment.INSTAGRAM_TAG, null,
											new RequestCompleteHandler() {
												
												@Override
												public void onSuccess(InstagramTagResponse response) {
                                                    // if fragment is destroyed do nothing
                                                    if(mIsDestroyed) return;

                                                    // set next max tag id
                                                    mNextMaxTagId = response.getNextMaxTagId();

                                                    // set last fetched list
													mLastFetchedList = response.getInstagramTagObjects();
													if(mLastFetchedList != null && mLastFetchedList.size() > 0) {
                                                        // set adapter to show results
														mLastServiceResult = ServiceResultEnum.OK;
														mFetchedList.addAll(mLastFetchedList);
														showNoScreen();
														setInitialAdapter();
													} else {
                                                        // update ui with no results
														mLastServiceResult = ServiceResultEnum.NO_RESULT;
														showNoResultFound();
													}
												}
												
												@Override
												public void onFailure(int statusCode, Throwable error) {
                                                    // if fragment is destroyed do nothing
                                                    if(mIsDestroyed) return;

                                                    // update ui with error
													mLastServiceResult = ServiceResultEnum.ERROR;
													showErrorProcessingService();
												}
												
											});
	}

	private void setInitialAdapter()
	{
		// init list view adapter
		mListViewAdapter = new InstagramMediaAdapter(getActivity());
		mListViewAdapter.updateInstagramTagObjects(mFetchedList);
		
		// init endless adapter wrapper
		mListViewEndlessAdapter = new InstagramEndlessAdapter(mListViewAdapter, appendTask);

		// set list view adapter
		setListAdapter(mListViewEndlessAdapter);
	}
	
	private void refreshList()
	{
		// cancel endless adapter task
		if (mListViewEndlessAdapter != null)
			mListViewEndlessAdapter.cancel();

		mNextMaxTagId = null;
		mLastServiceResult = null;
		mFetchedList = new ArrayList<InstagramMediaObject>();
		mLastFetchedList = null;
		
		// execute async task to fetch instagram media objects
		initializeList();
	}

    @Override
    public void onListItemClick(ListView listView, View view, int position, long id) {
        super.onListItemClick(listView, view, position, id);

        //int pos = position - getListView().getHeaderViewsCount();
        //if(pos < 0)
        //    return;

        InstagramMediaObject item = mListViewAdapter.getItem(position);

        // Notify the active callbacks interface (the activity, if the
        // fragment is attached to one) that an item has been selected.
        mCallbacks.onItemSelected(item);
    }
		
	private final AdapterAppendTask appendTask = new AdapterAppendTask() {
		
		private AtomicBoolean running = new AtomicBoolean(false);				
		
		@Override
		public void run(final ResultHandler resultHandler) {
			this.running.set(true);
			
			// fetch instagram media objects
			InstagramService.loadTagsMediaRecent(getActivity(), InstagramTagMediaListFragment.INSTAGRAM_TAG, mNextMaxTagId,
												new RequestCompleteHandler() {
													
													@Override
													public void onSuccess(InstagramTagResponse response) {
                                                        // if fragment is destroyed do nothing
                                                        if(mIsDestroyed) return;

                                                        // set next max tag id
                                                        mNextMaxTagId = response.getNextMaxTagId();

                                                        // set last fetched list
														mLastFetchedList = response.getInstagramTagObjects();
														if(mLastFetchedList != null && mLastFetchedList.size() > 0) {
															mLastServiceResult = ServiceResultEnum.OK;
														} else {
															mLastServiceResult = ServiceResultEnum.NO_RESULT;
														}


														if(resultHandler != null) {
															resultHandler.onRun();
															resultHandler.onFinish(null);
														}
													}
													
													@Override
													public void onFailure(int statusCode, Throwable error) {
                                                        // if fragment is destroyed do nothing
                                                        if(mIsDestroyed) return;

														mLastServiceResult = ServiceResultEnum.ERROR;

														if(resultHandler != null) {
															resultHandler.onRun();
															resultHandler.onFinish(error instanceof Exception ? (Exception) error : null);
														}
													}
												});
		}
		
		@Override
		public boolean isRunning() {
			return this.running.get();
		}
		
		@Override
		public void cancel() {
			InstagramService.cancelRequests(getActivity());
			this.running.set(false);
		}
	};		
	
	protected class InstagramEndlessAdapter extends EndlessAdapter
	{
		
		public InstagramEndlessAdapter(ListAdapter adapter, AdapterAppendTask appendTask)
		{
			super(getActivity().getBaseContext(), adapter, R.layout.loading, appendTask);
			
		}
		
		@Override
		protected boolean cacheInBackground()
		{
			return mLastServiceResult == ServiceResultEnum.OK;
		}

		@Override
		protected void appendCachedData()
		{
			BaseAdapter adapter = (BaseAdapter) getWrappedAdapter();

			if (mLastServiceResult == ServiceResultEnum.ERROR)
			{
                // Show error dialog
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setMessage(R.string.lbl_loading_error)
                        .setCancelable(false).setNeutralButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });
                builder.create().show();
			}
			else if (adapter != null)
			{
				if (mLastFetchedList != null)
				{
					// on fetch more data
					// remove objects already in the list (with same id)
					mLastFetchedList.removeAll(mFetchedList);

					Log.d(TAG, "LastFetchedList Size: " + mLastFetchedList.size());
					
					// update list and adapter
					mFetchedList.addAll(mLastFetchedList);
					mListViewAdapter.updateInstagramTagObjects(mFetchedList);
				}
			}
		}
		
		@Override
		protected void cancel() {
			super.cancel();
		}
	}
	
	private void showNoScreen() {
		View v = getView().findViewById(android.R.id.empty);
		v.setVisibility(View.GONE);
	}

	private void showNoResultFound() {
		setListAdapter(null);
		
		View v = getView().findViewById(R.id.error_or_no_results);
		v.setVisibility(View.VISIBLE);
		
		TextView txt = (TextView)v.findViewById(R.id.error_or_no_results_label);
		txt.setText(R.string.lbl_no_results);
		
		getView().findViewById(R.id.loading_progress).setVisibility(View.GONE);
	}

	private void showErrorProcessingService() {
		setListAdapter(null);
		
		View v = getView().findViewById(R.id.error_or_no_results);
		v.setVisibility(View.VISIBLE);
		
		TextView txt = (TextView)v.findViewById(R.id.error_or_no_results_label);
		txt.setText(R.string.lbl_service_error);
		
		getView().findViewById(R.id.loading_progress).setVisibility(View.GONE);
	}

	private void showLoadingProcessingService() {
		View v = getView().findViewById(android.R.id.empty);
		v.setVisibility(View.VISIBLE);
		
		getView().findViewById(R.id.error_or_no_results).setVisibility(View.GONE);
		getView().findViewById(R.id.loading_progress).setVisibility(View.VISIBLE);
	}

	@Override
	public void onStop() {
		// cancel requests for service
		InstagramService.cancelRequests(getActivity());
		
		if(mLastServiceResult == ServiceResultEnum.ERROR)
			showErrorProcessingService();
		else if(mLastServiceResult == ServiceResultEnum.NO_RESULT)
			showNoResultFound();
		
		super.onStop();
	}

    @Override
    public void onDestroy() {
        super.onDestroy();

        mIsDestroyed = true;
    }
}