package com.rfplopes.androidchallenge.utils;
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

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.hardware.Camera.Face;
import android.view.View;

import com.rfplopes.androidchallenge.R;

/**
 * This class is a simple View to display the faces.
 */
public class FaceOverlayView extends View {

    private int mDisplayOrientation;
    private int mOrientation;
    private Face[] mFaces;
    
    private Matrix mMatrix;
    private RectF mRectF;
    private BitmapDrawable mBitmap;

    private boolean mIsMirror;

    public FaceOverlayView(Context context) {
        super(context);
        initialize();
    }

    private void initialize() {
        // init variables
        mMatrix = new Matrix();
        mRectF = new RectF();
        mIsMirror = false;

        // want a beautiful picture to display in front of the face
        mBitmap = (BitmapDrawable) getResources().getDrawable(R.drawable.ricardo_lopes);
    }

    public void setFaces(Face[] faces) {
        mFaces = faces;
        invalidate();
    }

    public void setOrientation(int orientation) {
        mOrientation = orientation;
    }

    public void setDisplayOrientation(int displayOrientation) {
        mDisplayOrientation = displayOrientation;
        invalidate();
    }

    public void setIsMirror(boolean isMirror) {
        mIsMirror = isMirror;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mFaces != null && mFaces.length > 0) {
        	mMatrix.reset();
            Util.prepareMatrix(mMatrix, mIsMirror, mDisplayOrientation, getWidth(), getHeight());
            canvas.save();
            mMatrix.postRotate(mOrientation);
            canvas.rotate(-mOrientation);
            for (Face face : mFaces) {
                mRectF.set(face.rect);
                mMatrix.mapRect(mRectF);
                canvas.drawBitmap(mBitmap.getBitmap(), null, mRectF, null);
            }
            canvas.restore();
        }
    }
}