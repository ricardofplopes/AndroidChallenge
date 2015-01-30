package com.rfplopes.androidchallenge.fragments;

/**
 * 	This class uses functions from the ApiDemos module of Android.
 *  Android code is released under terms of the Apache 2.0 license:
 *
 * 	Copyright (C) 2007 The Android Open Source Project
 *
 * 	Licensed under the Apache License, Version 2.0 (the "License");
 * 	you may not use this file except in compliance with the License.
 * 	You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * 	Unless required by applicable law or agreed to in writing, software
 * 	distributed under the License is distributed on an "AS IS" BASIS,
 * 	WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * 	See the License for the specific language governing permissions and
 * 	limitations under the License.
 *
 */

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.EmbossMaskFilter;
import android.graphics.MaskFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.rfplopes.androidchallenge.R;
import com.rfplopes.androidchallenge.utils.ColorPickerDialog;

public class FingerPaintFragment extends Fragment implements ColorPickerDialog.OnColorChangedListener {

    public static final String TAG = FingerPaintFragment.class.getSimpleName();

	private Context mContext;
	private FingerPaintView mFingerPaintView;

	private Paint       mPaint;
    private MaskFilter  mEmboss;
    private MaskFilter  mBlur;

    private ProgressDialog mProgressDialog;

    @Override
    public void onAttach(Activity activity) {
    	super.onAttach(activity);
    	
    	mContext = activity;
    }
    
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// retain instance
		setRetainInstance(true);
		
		// has options menu
		setHasOptionsMenu(true);
		
		// init paint and mask filters
		mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setDither(true);
        mPaint.setColor(getResources().getColor(R.color.pink));
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeJoin(Paint.Join.ROUND);
        mPaint.setStrokeCap(Paint.Cap.ROUND);
        mPaint.setStrokeWidth(12);

        mEmboss = new EmbossMaskFilter(new float[] { 1, 1, 1 },
                                       0.4f, 6, 3.5f);

        mBlur = new BlurMaskFilter(8, BlurMaskFilter.Blur.NORMAL);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		mFingerPaintView = new FingerPaintView(mContext);
		return mFingerPaintView;
	}
	
    public void colorChanged(int color) {
        mPaint.setColor(color);
    }
    
    public class FingerPaintView extends View {

        private Bitmap  mBitmap;
        private Canvas  mCanvas;
        private Path    mPath;
        private Paint   mBitmapPaint;
        private boolean mDrawPoint;
        
        public FingerPaintView(Context c) {
            super(c);

            mPath = new Path();
            mBitmapPaint = new Paint(Paint.DITHER_FLAG);
        }

        @Override
        protected void onSizeChanged(int w, int h, int oldw, int oldh) {
            super.onSizeChanged(w, h, oldw, oldh);
            mBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            mCanvas = new Canvas(mBitmap);
            mCanvas.drawColor(getResources().getColor(R.color.background));
        }

        @Override
        protected void onDraw(Canvas canvas) {
            canvas.drawColor(getResources().getColor(R.color.background));
        	
            canvas.drawBitmap(mBitmap, 0, 0, mBitmapPaint);

            canvas.drawPath(mPath, mPaint);
        }

        private float mX, mY;
        private static final float TOUCH_TOLERANCE = 4;

        private void touch_start(float x, float y) {
            mPath.reset();
            mPath.moveTo(x, y);
            mX = x;
            mY = y;
            mDrawPoint = true;
        }
        private void touch_move(float x, float y) {
            float dx = Math.abs(x - mX);
            float dy = Math.abs(y - mY);
            if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
                mPath.quadTo(mX, mY, (x + mX)/2, (y + mY)/2);
                mX = x;
                mY = y;
                mDrawPoint = false;
            }
        }

        private void touch_up() {
            if(mDrawPoint == true) {
                mCanvas.drawPoint(mX, mY, mPaint);          
            } else {
                mPath.lineTo(mX, mY);
                // commit the path to our offscreen
                mCanvas.drawPath(mPath, mPaint);
                // kill this so we don't double draw
                mPath.reset();
            }
        }

        @SuppressLint("ClickableViewAccessibility")
		@Override
        public boolean onTouchEvent(MotionEvent event) {
            float x = event.getX();
            float y = event.getY();

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    touch_start(x, y);
                    invalidate();
                    break;
                case MotionEvent.ACTION_MOVE:
                    touch_move(x, y);
                    invalidate();
                    break;
                case MotionEvent.ACTION_UP:
                    touch_up();
                    invalidate();
                    break;
            }
            return true;
        }

        public Bitmap getBitmap() {
            return mBitmap;
        }

        public Canvas getCanvas() {
            return mCanvas;
        }
    }
    
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    	inflater.inflate(R.menu.menu_fingerpaint, menu);
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        mPaint.setXfermode(null);
        mPaint.setColor(getResources().getColor(R.color.pink));
        mPaint.setStrokeWidth(12);
        mPaint.setAlpha(0xFF);

        switch (item.getItemId()) {
	        case R.id.action_export:
	        	new ExportImageTask().execute();
	        	return true;
	        case R.id.action_color:
                new ColorPickerDialog(mContext, this, mPaint.getColor()).show();
                return true;
            case R.id.action_emboss:
                if (mPaint.getMaskFilter() != mEmboss) {
                    mPaint.setMaskFilter(mEmboss);
                } else {
                    mPaint.setMaskFilter(null);
                }
                return true;
            case R.id.action_blur:
                if (mPaint.getMaskFilter() != mBlur) {
                    mPaint.setMaskFilter(mBlur);
                } else {
                    mPaint.setMaskFilter(null);
                }
                return true;
            case R.id.action_erase:
//                mPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
                mPaint.setColor(getResources().getColor(R.color.background));
                mPaint.setMaskFilter(null);
                mPaint.setStrokeWidth(24);
                return true;
            case R.id.action_erase_all:
            	mFingerPaintView.getCanvas().drawColor(getResources().getColor(R.color.background));
            	mFingerPaintView.invalidate();
            	return true;
        }
        
    	return super.onOptionsItemSelected(item);
    }

    private class ExportImageTask extends AsyncTask<Bitmap, Void, Boolean> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            // run on UI thread

            // show progress dialog
            mProgressDialog = ProgressDialog.show(mContext, getString(R.string.lbl_exporting), getString(R.string.msg_exporting_img));
        }

        @Override
        protected Boolean doInBackground(Bitmap... params) {
            // have the progress dialog for at least 500ms
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Log.e(TAG, "Exception", e);
            }
            // export image
            return exportImage();
        }

        private boolean exportImage() {
            String path = Environment.getExternalStorageDirectory().toString();
            OutputStream fOut = null;

            File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_PICTURES), "AndroidChallenge");

            if (!mediaStorageDir.exists()){
                if(!mediaStorageDir.mkdirs()){
                    Log.d(TAG, "Required media storage does not exist. Using external storage root.");
                } else {
                    path = mediaStorageDir.getPath();
                }
            } else {
                path = mediaStorageDir.getPath();
            }

            // the File to save to
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            File file = new File(path, getString(R.string.lbl_image_filename, timeStamp));

            try {
                fOut = new FileOutputStream(file);

                // saving the Bitmap to a file
                mFingerPaintView.getBitmap().compress(Bitmap.CompressFormat.PNG, 100, fOut);

                fOut.flush();
                // close the stream
                fOut.close();
            } catch (FileNotFoundException e) {
                Log.e(TAG, "Error exporting image.", e);
                return false;
            } catch (IOException e1) {
                Log.e(TAG, "Error exporting image.", e1);
                return false;
            }

            return true;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            super.onPostExecute(result);

            // dismiss dialog if showing
            if(mProgressDialog != null && mProgressDialog.isShowing())
                mProgressDialog.dismiss();

            if(result)
                Toast.makeText(mContext, R.string.msg_export_img_successful, Toast.LENGTH_SHORT).show();
            else
                Toast.makeText(mContext, R.string.msg_export_img_error, Toast.LENGTH_SHORT).show();
        }

        @Override
        protected void onCancelled(Boolean result) {
            super.onCancelled(result);

            // dismiss dialog if showing
            if(mProgressDialog != null && mProgressDialog.isShowing())
                mProgressDialog.dismiss();

            Toast.makeText(mContext, "Operation cancelled", Toast.LENGTH_SHORT).show();
        }

    }

}
