/*
 * Copyright (c) 2017 The LineageOS Project
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 * Also add information on how to contact you by electronic and paper mail.
 *
 */

package org.lineageos.flipflap;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.os.BatteryManager;
import android.util.AttributeSet;
import android.view.View;

public class CircleBatteryView extends View {
    private static final String TAG = "CircleBatteryView";

    private final Context mContext;
    private final Resources mResources;
    private Paint mPaint;
    private int mCenter_x;
    private int mCenter_y;
    private int mRadius;
    private int mOffset_x;
    private int mOffset_y;
    private int mOffset_rad;

    public CircleBatteryView(Context context) {
        this(context, null);
    }

    public CircleBatteryView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CircleBatteryView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        mContext = context;

        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mResources = mContext.getResources();

        mOffset_x = mResources.getInteger(R.integer.x_offset);
        mOffset_y = mResources.getInteger(R.integer.y_offset);
        mOffset_rad = mResources.getInteger(R.integer.radius_offset);
    }

    @Override
    public void onDraw(Canvas canvas) {
        Intent batteryStatus = mContext.registerReceiver(null,
                new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL;

        mCenter_x = getWidth() / 2 + mOffset_x;
        mCenter_y = getHeight() * 13 / 48  + mOffset_y;
        mRadius = getWidth() * 4 / 9 + mOffset_rad;

        canvas.drawRGB(0, 0, 0);
        mPaint.setStyle(Style.FILL);

        if (isCharging) {
            mPaint.setColor(mResources.getColor(R.color.charge_bat_bg));
        } else if (level >= 15) {
            mPaint.setColor(mResources.getColor(R.color.full_bat_bg));
        } else {
            mPaint.setColor(mResources.getColor(R.color.low_bat_bg));
        }
        canvas.drawCircle((float) mCenter_x, (float) mCenter_y, (float) mRadius, mPaint);
    }
}
