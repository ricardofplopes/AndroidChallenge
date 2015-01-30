package com.rfplopes.androidchallenge.fragments;

/**
 * 	This class is inspired by the VideoFaceDetection module of Philip Wagner (bytefish)
 * 	whose code can be found on:
 *
 *      https://github.com/bytefish/facerec/blob/master/android/VideoFaceDetection/
 *      
 *  The code is released under terms of the following license:
 *
 * Copyright (c) 2014, Philipp Wagner <bytefish(at)gmx(dot)de>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted
 * provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of
 * conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of
 * conditions and the following disclaimer in the documentation and/or other materials provided
 * with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS
 * OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY
 * AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

import java.util.List;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.hardware.Camera.Face;
import android.hardware.Camera.FaceDetectionListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.OrientationEventListener;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.rfplopes.androidchallenge.R;
import com.rfplopes.androidchallenge.utils.FaceOverlayView;
import com.rfplopes.androidchallenge.utils.Util;

public class CameraBeautifierFragment extends Fragment implements SurfaceHolder.Callback {

    public static final String TAG = CameraBeautifierFragment.class.getSimpleName();

    // Camera
    private Camera mCamera;
    private int mCameraType = Camera.CameraInfo.CAMERA_FACING_BACK;

    // We need the phone orientation to correctly draw the overlay:
    private int mOrientation;
    private int mOrientationCompensation;
    private OrientationEventListener mOrientationEventListener;

    // Let's keep track of the display rotation and orientation also:
    private int mDisplayRotation;
    private int mDisplayOrientation;

    // The surface view for the camera data
    private SurfaceView mView;
    private SurfaceHolder mHolder;

    // Other views
    private TextView mTvLabel;

    // Draw face overlay
    private FaceOverlayView mFaceView;
    
    // Auto focus
	private boolean mIsAutoFocusStarted = false;
	
    /**
     * Sets the faces for the overlay view, so it can be updated
     * and the face overlays will be drawn again.
     */
    private FaceDetectionListener faceDetectionListener = new FaceDetectionListener() {
        @Override
        public void onFaceDetection(Face[] faces, Camera camera) {
            Log.d("onFaceDetection", "Number of Faces:" + faces.length);
            // Update the faces view now!
            mFaceView.setFaces(faces);
        }
    };	

    private Context mAppContext;

	@Override
    public void onAttach(Activity activity) {
    	super.onAttach(activity);
        // set application context
    	mAppContext = activity.getApplicationContext();

        // add flag keep screen on
        activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    public void onDetach() {
        // clear flag keep screen on
        getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        super.onDetach();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
	   super.onCreate(savedInstanceState);

       // retain instance
       setRetainInstance(true);

       // has options menu
       setHasOptionsMenu(true);

	   // create and start the OrientationListener:
       mOrientationEventListener = new SimpleOrientationEventListener(getActivity());
       mOrientationEventListener.enable();
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
    		Bundle savedInstanceState) {

    	FrameLayout frameLayout = (FrameLayout) inflater.inflate(R.layout.fragment_camera, container, false);
    	mFaceView = new FaceOverlayView(mAppContext);
    	frameLayout.addView(mFaceView, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
    	
    	return frameLayout;
    }
    
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
    	super.onViewCreated(view, savedInstanceState);

        mTvLabel = (TextView) view.findViewById(R.id.camera_label);

    	mView = (SurfaceView) view.findViewById(R.id.camera_preview);
    	mView.setOnClickListener(onSetFocusClickListener);
        mHolder = mView.getHolder();
        mHolder.addCallback(this);
    }

    @Override
    public void onResume() {
    	super.onResume();

        if (mCamera != null) {
            // start camera face detection and preview
            mCamera.startPreview();
            mCamera.startFaceDetection();
        }

        // start OrientationListener
    	mOrientationEventListener.enable();
    }
    
    @Override
    public void onPause() {
        // disable OrientationListener
        mOrientationEventListener.disable();

        if (mCamera != null) {
            // stop camera face detection and preview
            mCamera.stopFaceDetection();
            mCamera.stopPreview();
        }

    	super.onPause();
    }


    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_camera, menu);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        if(!mAppContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT)) {
            menu.findItem(R.id.action_exchange_camera).setVisible(false);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.action_exchange_camera:
                switchCamera(mCameraType +  1);
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private boolean safeCameraOpen(int id) {
        boolean qOpened = false;

        try {
            releaseCamera();
            mCamera = Camera.open(id);

            qOpened = (mCamera != null);
        } catch (Exception e) {
            Log.e(getString(R.string.app_name), "failed to open Camera");
            e.printStackTrace();
        }

        return qOpened;
    }

    private void releaseCamera() {
        if (mCamera != null) {
            mCamera.setPreviewCallback(null);
            mCamera.setFaceDetectionListener(null);
            mCamera.setErrorCallback(null);
            mCamera.release();
            mCamera = null;
        }
    }

    public void switchCamera(int cameraType)
    {
        if(mAppContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT))
        {
            if(Camera.getNumberOfCameras() > cameraType)
            {
                // set selected camera
                mCameraType = cameraType;
            }
            else
            {
                // set default camera (Rear)
                mCameraType = Camera.CameraInfo.CAMERA_FACING_BACK;
            }

            if(mCamera != null)
            {
                // destroy previous Holder
                surfaceDestroyed(mHolder);
                mHolder.removeCallback(this);

                // remove and re-add SurfaceView
                ViewGroup rootLayout = (ViewGroup) mView.getParent();
                rootLayout.removeView(mView);
                mView = new SurfaceView(getActivity());
                mHolder = mView.getHolder();
                mHolder.addCallback(this);
                rootLayout.addView(mView, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

                // set face view
                mFaceView.setIsMirror(mCameraType != Camera.CameraInfo.CAMERA_FACING_BACK);
                mFaceView.bringToFront();
                mTvLabel.bringToFront();
            }
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        if(!safeCameraOpen(mCameraType))
            return;
        mCamera.setFaceDetectionListener(faceDetectionListener);
        mCamera.startFaceDetection();
        try {
            mCamera.setPreviewDisplay(surfaceHolder);
        } catch (Exception e) {
            Log.e(TAG, "Could not preview the image.", e);
        }
    }
    
    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i2, int i3) {
        // We have no surface, return immediately:
        if (surfaceHolder.getSurface() == null) {
            return;
        }
        // Try to stop the current preview:
        try {
            mCamera.stopPreview();
        } catch (Exception e) {
            // Ignore...
        }
        // Get the supported preview sizes:
        Camera.Parameters parameters = mCamera.getParameters();
        List<Camera.Size> previewSizes = parameters.getSupportedPreviewSizes();
        Camera.Size previewSize = previewSizes.get(0);
        // And set them:
        parameters.setPreviewSize(previewSize.width, previewSize.height);
        mCamera.setParameters(parameters);
        // Now set the display orientation for the camera. Can we do this differently?
        mDisplayRotation = Util.getDisplayRotation(CameraBeautifierFragment.this.getActivity());
        mDisplayOrientation = Util.getDisplayOrientation(mDisplayRotation, 0);
        mCamera.setDisplayOrientation(mDisplayOrientation);

        if (mFaceView != null) {
            mFaceView.setDisplayOrientation(mDisplayOrientation);
        }

        // Finally start the camera preview again:
        mCamera.startPreview();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        releaseCamera();
    }

    /**
     * We need to react on OrientationEvents to rotate the screen and
     * update the views.
     */
    private class SimpleOrientationEventListener extends OrientationEventListener {

        public SimpleOrientationEventListener(Context context) {
            super(context, SensorManager.SENSOR_DELAY_NORMAL);
        }

        @Override
        public void onOrientationChanged(int orientation) {
            // We keep the last known orientation. So if the user first orient
            // the camera then point the camera to floor or sky, we still have
            // the correct orientation.
            if (orientation == ORIENTATION_UNKNOWN) return;
            mOrientation = Util.roundOrientation(orientation, mOrientation);
            // When the screen is unlocked, display rotation may change. Always
            // calculate the up-to-date orientationCompensation.
            int orientationCompensation = mOrientation
                    + Util.getDisplayRotation(CameraBeautifierFragment.this.getActivity());
            if (mOrientationCompensation != orientationCompensation) {
                mOrientationCompensation = orientationCompensation;
                mFaceView.setOrientation(mOrientationCompensation);
            }
        }
    }
    
    private OnClickListener onSetFocusClickListener = new OnClickListener() {

    	@Override
		public void onClick(View v) {
			if (mCamera != null) {
				if (!mIsAutoFocusStarted)
				{
					AutoFocusCallBackImpl autoFocusCallBack = new AutoFocusCallBackImpl();
					mIsAutoFocusStarted = true;
					mCamera.autoFocus(autoFocusCallBack);
				}
			}
		}

	};
    
	private class AutoFocusCallBackImpl implements Camera.AutoFocusCallback {

		@Override
		public void onAutoFocus(boolean success, Camera camera) {
			mIsAutoFocusStarted = false;
		}

	}

}

