//  Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
//  SPDX-License-Identifier: MIT-0

package com.aws.webrtc.view;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

import androidx.annotation.Nullable;

public class SplitBackgroundDrawable extends Drawable {
    private final Paint paint;
    private int distanceFromTopToSplitPoint = -1;
    private final int topBackgroundColor;
    private static final int DEFAULT_BACKGROUND_COLOR = Color.WHITE;

    public SplitBackgroundDrawable(int distanceFromTop, int topBackgroundColor) {
        paint = new Paint();
        this.topBackgroundColor = topBackgroundColor;
        setSplitPointDistanceFromTop(distanceFromTop);
    }

    public void setSplitPointDistanceFromTop(int distanceFromTop) {
        distanceFromTopToSplitPoint = distanceFromTop;
        invalidateSelf();
    }

    @Override
    public void draw(final Canvas canvas) {
        final Rect b = getBounds();
        paint.setColor(this.topBackgroundColor);
        float y = Math.min(distanceFromTopToSplitPoint, b.height());

        canvas.drawRect(0, 0, b.width(), y, paint);
        paint.setColor(DEFAULT_BACKGROUND_COLOR);
        canvas.drawRect(0, y, b.width(), b.height(), paint);
    }

    @Override
    public void setAlpha(int alpha) {

    }

    @Override
    public void setColorFilter(@Nullable ColorFilter colorFilter) {

    }

    @Override
    public int getOpacity() {
        return PixelFormat.UNKNOWN;
    }
}