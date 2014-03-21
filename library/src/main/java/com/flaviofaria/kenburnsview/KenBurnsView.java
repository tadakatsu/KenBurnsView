/*
 * Copyright 2014 Flavio Faria
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.flaviofaria.kenburnsview;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.ImageView;

/**
 * {@link ImageView} extension that animates its image with the
 * <a href="http://en.wikipedia.org/wiki/Ken_Burns_effect">Ken Burns Effect</a>.
 * @author Flavio Faria
 * @see Transition
 * @see TransitionGenerator
 */
public class KenBurnsView extends ImageView {

    /** Delay between a pair of frames at a 60 FPS frame rate. */
    private static final long FRAME_DELAY = 1000 / 60;

    /** Matrix used to perform all the necessary transition transformations. */
    private final Matrix mMatrix = new Matrix();

    /** The {@link TransitionGenerator} implementation used to perform the transitions between
     *  rects. The default {@link TransitionGenerator} is {@link RandomTransitionGenerator}. */
    private TransitionGenerator mTransGen = new RandomTransitionGenerator();

    /** The ongoing transition. */
    private Transition mCurrentTrans;

    /** The rect that holds the bounds of this view. */
    private final RectF mViewportRect = new RectF();
    /** The rect that holds the bounds of the current {@link Drawable}. */
    private final RectF mDrawableRect = new RectF();

    /** The time, in milliseconds, that the current transition started. */
    private long mTransitionStartTime;

    /** Controls whether the image must be center-cropped or not. */
    private boolean mCenterCrop;


    public KenBurnsView(Context context) {
        this(context, null);
    }


    public KenBurnsView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }


    public KenBurnsView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        // Attention to the super call here!
        super.setScaleType(ImageView.ScaleType.MATRIX);
    }


    @Override
    public void setScaleType(ScaleType scaleType) {
        switch (scaleType) {
            case CENTER_CROP:
                mCenterCrop = true;
                break;
            case FIT_CENTER:
                mCenterCrop = false;
                break;
            default:
                String msg = "KenBurnsView only supports ScaleType.CENTER_CROP " +
                        "and ScaleType.FIT_CENTER!";
                throw new UnsupportedOperationException(msg);
        }
    }


    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        updateViewPort(w, h);
    }


    @Override
    protected void onDraw(Canvas canvas) {
        Drawable d = getDrawable();
        updateDrawableBounds();

        if (d != null) { // No drawable to animate? We're done for now.
            if (mCurrentTrans == null) { // Starting the first transition.
                startNewTransition();
            }

            if (mCurrentTrans.getDestinyRect() != null) { // If null, it's supposed to stop.
                long elapsedTime = System.currentTimeMillis() - mTransitionStartTime;
                RectF currentRect = mCurrentTrans.getInterpolatedRect(elapsedTime);

                float viewPortScale = getWidth() / (float) d.getIntrinsicWidth();
                float currentRectScale = (getWidth() / currentRect.width()) * viewPortScale;

                if (mCenterCrop) {
                    float drawableRatio = Rects.getRectRatio(mDrawableRect);
                    float viewPortRatio = Rects.getRectRatio(mViewportRect);
                    if (viewPortRatio > drawableRatio) {
                        currentRectScale *= mViewportRect.width() / d.getBounds().width();
                    } else if (viewPortRatio < drawableRatio) {
                        currentRectScale *= mViewportRect.height() / d.getBounds().height();
                    }
                }

                /* Performs matrix transformations to fit the content
                   of the current rect into the entire view. */
                mMatrix.reset();
                mMatrix.postTranslate(-d.getIntrinsicWidth() / 2, -d.getIntrinsicHeight() / 2);
                mMatrix.postScale(currentRectScale, currentRectScale);
                mMatrix.postTranslate(currentRect.centerX(), currentRect.centerY());

                setImageMatrix(mMatrix);
                postInvalidateDelayed(FRAME_DELAY);

                // Current transition is over. It's time to start a new one.
                if (elapsedTime >= mCurrentTrans.getDuration()) {
                    startNewTransition();
                }
            }
        }
        super.onDraw(canvas);
    }


    /**
     * Generates and starts a transition.
     */
    private void startNewTransition() {
        mCurrentTrans = mTransGen.generateNextTransition(mViewportRect, mDrawableRect);
        mTransitionStartTime = System.currentTimeMillis();
    }


    /**
     * Sets the {@link TransitionGenerator} to be used in animations.
     * @param transgen the {@link TransitionGenerator} to be used in animations.
     */
    public void setTransitionGenerator(TransitionGenerator transgen) {
        mTransGen = transgen;
    }


    /**
     * Updates the viewport rect. This must be called every time the size of this view changes.
     * @param width the new viewport with.
     * @param height the new viewport height.
     */
    private void updateViewPort(float width, float height) {
        mViewportRect.set(0, 0, width, height);
    }


    /**
     * Updates the drawable bounds rect. THis must be called every time the drawable
     * associated to this view changes.
     */
    private void updateDrawableBounds() {
        mDrawableRect.set(getDrawable().getBounds());
    }
}