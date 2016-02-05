/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.sunshine.wearable;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.view.SurfaceHolder;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Analog watch face with a ticking second hand. In ambient mode, the second hand isn't shown. On
 * devices with low-bit ambient mode, the hands are drawn without anti-aliasing in ambient mode.
 */
public class WeatherWatchFace extends CanvasWatchFaceService {

    @Override
    public Engine onCreateEngine() {
        return new Engine();
    }

    private class Engine extends CanvasWatchFaceService.Engine {
        boolean mRegisteredTimeZoneReceiver = false;
        boolean mAmbient;

        int mTapCount;

        int mTimeFontSize = 36;
        int mDateFontSize = 16;
        int mTempFontSize = 24;

        Paint mPrimaryTextPaint;
        Paint mSecondaryTextPaint;
        Paint mBackgroundPaint;

        Calendar mCalendar = Calendar.getInstance();

        final BroadcastReceiver mTimeZoneReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String timeZoneId = intent.getStringExtra("time-zone");
                mCalendar = Calendar.getInstance();
                mCalendar.setTimeZone(TimeZone.getTimeZone(timeZoneId));
            }
        };

        /**
         * Whether the display supports fewer bits for each color in ambient mode. When true, we
         * disable anti-aliasing in ambient mode.
         */
        boolean mLowBitAmbient;

        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);

            setWatchFaceStyle(new WatchFaceStyle.Builder(WeatherWatchFace.this)
                    .setCardPeekMode(WatchFaceStyle.PEEK_MODE_SHORT)
                    .setBackgroundVisibility(WatchFaceStyle.BACKGROUND_VISIBILITY_INTERRUPTIVE)
                    .setShowSystemUiTime(false)
                    .setAcceptsTapEvents(true)
                    .build());

            Resources resources = WeatherWatchFace.this.getResources();

            mBackgroundPaint = new Paint();
            mBackgroundPaint.setColor(resources.getColor(R.color.background_interactive));

            mPrimaryTextPaint = new Paint();
            mPrimaryTextPaint.setColor(resources.getColor(R.color.text_primary));
//            mPrimaryTextPaint.setStrokeWidth(resources.getDimension(R.dimen.analog_hand_stroke));
            mPrimaryTextPaint.setAntiAlias(true);
//            mPrimaryTextPaint.setStrokeCap(Paint.Cap.ROUND);

            mSecondaryTextPaint = new Paint();
            mSecondaryTextPaint.setColor(resources.getColor(R.color.text_secondary));
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
        }

        @Override
        public void onPropertiesChanged(Bundle properties) {
            super.onPropertiesChanged(properties);
            mLowBitAmbient = properties.getBoolean(PROPERTY_LOW_BIT_AMBIENT, false);
        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();
            invalidate();
        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);
            if (mAmbient != inAmbientMode) {
                mAmbient = inAmbientMode;
                if (mLowBitAmbient) {
                    mPrimaryTextPaint.setAntiAlias(!inAmbientMode);
                }
                invalidate();
            }
        }

        /**
         * Captures tap event (and tap type) and toggles the background color if the user finishes
         * a tap.
         */
        @Override
        public void onTapCommand(int tapType, int x, int y, long eventTime) {
            Resources resources = WeatherWatchFace.this.getResources();
            switch (tapType) {
                case TAP_TYPE_TOUCH:
                    // The user has started touching the screen.
                    break;
                case TAP_TYPE_TOUCH_CANCEL:
                    // The user has started a different gesture or otherwise cancelled the tap.
                    break;
                case TAP_TYPE_TAP:
                    // The user has completed the tap gesture.
                    mTapCount++;
                    mBackgroundPaint.setColor(resources.getColor(mTapCount % 2 == 0 ?
                            R.color.background_ambient : R.color.background_interactive));
                    break;
            }
            invalidate();
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {

            mCalendar = Calendar.getInstance();

            // Draw the background.
            if (isInAmbientMode()) {
                canvas.drawColor(Color.BLACK);
            } else {
                canvas.drawRect(0, 0, canvas.getWidth(), canvas.getHeight(), mBackgroundPaint);
            }

            // date values
            SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, MMM d yyyy", Locale.CANADA);
            String date, hour, minute, tempHigh, tempLow;

            date = dateFormat.format(mCalendar.getTime());
            hour = getHour();
            minute = getMinute();
            tempHigh = "23°";    // TODO: obtain these via a broadcast reciever..?
            tempLow = "19°";

            // init offsets
            float xCenter = bounds.centerX();
            float x = 0f;
            float y = 60f;

            // draw time
            canvas.drawText(":", x, y, mPrimaryTextPaint);

            x = xCenter - mPrimaryTextPaint.measureText(hour);
            canvas.drawText(hour, x, y, mPrimaryTextPaint);

            x = xCenter + mPrimaryTextPaint.measureText(":");
            canvas.drawText(minute, x, y, mPrimaryTextPaint);

            // date text
            y += 40f;
            x = xCenter - mPrimaryTextPaint.measureText(date) / 2;
            canvas.drawText(date, x, y, mPrimaryTextPaint);

            // temps
            y += 40f;
            x = xCenter - mPrimaryTextPaint.measureText(tempHigh);
            canvas.drawText(tempHigh, x, y, mPrimaryTextPaint);
            x = xCenter;
            canvas.drawText(tempLow, x, y, mPrimaryTextPaint);
        }

        private String getIcon() {
            return "";
        }

        private String getMinute() {
            int min = mCalendar.get(Calendar.MINUTE);
            if (min < 10) {
                return "0" + String.valueOf(min);
            } else {
                return String.valueOf(min);
            }
        }

        private String getHour() {
            int hour = mCalendar.get(Calendar.HOUR);

            return String.valueOf(hour == 0 ? 12 : hour);
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);

            if (visible) {
                registerReceiver();

                // Update time zone in case it changed while we weren't visible.
                mCalendar = Calendar.getInstance();
                mCalendar.setTimeZone(TimeZone.getTimeZone(TimeZone.getDefault().getID()));
            } else {
                unregisterReceiver();
            }
        }

        private void registerReceiver() {
            if (mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = true;
            IntentFilter filter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
            WeatherWatchFace.this.registerReceiver(mTimeZoneReceiver, filter);
        }

        private void unregisterReceiver() {
            if (!mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = false;
            WeatherWatchFace.this.unregisterReceiver(mTimeZoneReceiver);
        }
    }
}
