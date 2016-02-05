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
import android.database.Cursor;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.view.SurfaceHolder;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

public class WeatherWatchFace extends CanvasWatchFaceService {

    @Override
    public Engine onCreateEngine() {
        return new Engine();
    }

    private class Weather {

        public Weather(int min, int max) {
            this.low = min;
            this.high = max;
        }

        int high;
        int low;
    }

    private class Engine extends CanvasWatchFaceService.Engine {

        boolean mLowBitAmbient;
        boolean mBurnProtection;
        boolean mRegisteredTimeZoneReceiver = false;
        boolean mAmbient;

        int mTapCount;

        String mHigh;
        String mLow;

        float mLargeTextSize;
        float mMediumTextSize;
        float mSmallTextSize;
        float mDividerWidth;
        float mTextPadding;
        float mYCenterOffset;

        Paint mLargeThickTextPaint;
        Paint mLargeThinTextPaint;
        Paint mMediumThickTextPaint;
        Paint mMediumThinTextPaint;
        Paint mSmallTextPaint;

        Typeface mThinTypeface;

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

        private class FetchWeatherTask extends AsyncTask<Void, Void, Weather> {

            @Override
            protected Weather doInBackground(Void... params) {
                Uri uri = Uri.parse("content://com.example.android.sunshine.app/weather");
                Cursor c = getContentResolver().query(uri, null, null, null, null);

                if (c != null && c.moveToFirst()) {
                    int min = c.getInt(c.getColumnIndex("min"));
                    int max = c.getInt(c.getColumnIndex("max"));

                    c.close();

                    return new Weather(min, max);
                }

                return null;
            }

            @Override
            protected void onPostExecute(Weather weather) {
                super.onPostExecute(weather);

                if (weather != null) {
                    mHigh = String.valueOf(weather.high);
                    mLow = String.valueOf(weather.low);
                }
            }
        }

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

            int backgroundColor = resources.getColor(R.color.background_interactive);
            int primaryColor = resources.getColor(R.color.text_primary);
            int secondaryColor = resources.getColor(R.color.text_secondary);

            mThinTypeface = Typeface.create("sans-serif-light", Typeface.NORMAL);

            mLargeTextSize = resources.getInteger(R.integer.text_size_large);
            mMediumTextSize = resources.getInteger(R.integer.text_size_medium);
            mSmallTextSize = resources.getInteger(R.integer.text_size_small);
            mDividerWidth = resources.getInteger(R.integer.divider_width);
            mTextPadding = resources.getInteger(R.integer.text_padding);
            mYCenterOffset = resources.getInteger(R.integer.y_center_offset);

            mBackgroundPaint = new Paint();
            mBackgroundPaint.setColor(backgroundColor);

            mLargeThickTextPaint = new Paint();
            mLargeThickTextPaint.setColor(primaryColor);
            mLargeThickTextPaint.setAntiAlias(true);
            mLargeThickTextPaint.setTextSize(mLargeTextSize);

            mLargeThinTextPaint = new Paint();
            mLargeThinTextPaint.setColor(primaryColor);
            mLargeThinTextPaint.setAntiAlias(true);
            mLargeThinTextPaint.setTextSize(mLargeTextSize);
            mLargeThinTextPaint.setTypeface(mThinTypeface);

            mMediumThickTextPaint = new Paint();
            mMediumThickTextPaint.setColor(primaryColor);
            mMediumThickTextPaint.setAntiAlias(true);
            mMediumThickTextPaint.setTextSize(mMediumTextSize);

            mMediumThinTextPaint = new Paint();
            mMediumThinTextPaint.setColor(secondaryColor);
            mMediumThinTextPaint.setAntiAlias(true);
            mMediumThinTextPaint.setTextSize(mMediumTextSize);
            mMediumThinTextPaint.setTypeface(mThinTypeface);

            mSmallTextPaint = new Paint();
            mSmallTextPaint.setColor(secondaryColor);
            mSmallTextPaint.setAntiAlias(true);
            mSmallTextPaint.setTextSize(mSmallTextSize);

            // fetch the weather
            new FetchWeatherTask().execute();
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
        }

        @Override
        public void onPropertiesChanged(Bundle properties) {
            super.onPropertiesChanged(properties);
            mLowBitAmbient = properties.getBoolean(PROPERTY_LOW_BIT_AMBIENT, false);

            // TODO: need to render hollow text for this to work
            mBurnProtection = properties.getBoolean(PROPERTY_BURN_IN_PROTECTION, false);
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
                    mLargeThickTextPaint.setAntiAlias(!inAmbientMode);
                    mMediumThickTextPaint.setAntiAlias(!inAmbientMode);
                    mSmallTextPaint.setAntiAlias(!inAmbientMode);
                }
                invalidate();
            }
        }

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
            tempHigh = mHigh != null ? mHigh + "°" : "";
            tempLow = mLow != null ? mLow + "°" : "";

            // init offsets
            float xCenter = bounds.centerX();
            float yCenter = bounds.centerY();
            float x;
            float y;

            float colonWidth = mLargeThickTextPaint.measureText(":");

            // Note: text is drawn vertically outward from the center of the wearable device

            // date text
            x = xCenter - mSmallTextPaint.measureText(date) / 2;
            y = yCenter - mYCenterOffset / 2;
            canvas.drawText(date, x, y, mSmallTextPaint);

            // draw time
            y -= mSmallTextPaint.getTextSize() + mTextPadding;
            canvas.drawText(":", xCenter - colonWidth / 2, y, mLargeThickTextPaint);
            x = xCenter - mLargeThickTextPaint.measureText(hour) - colonWidth / 2;
            canvas.drawText(hour, x, y, mLargeThickTextPaint);
            x = xCenter + colonWidth / 2;
            canvas.drawText(minute, x, y, mLargeThinTextPaint);

            // divider line
            y = yCenter + mYCenterOffset;
            canvas.drawLine(xCenter - mDividerWidth / 2, y, xCenter + mDividerWidth / 2, y, mMediumThinTextPaint);

            // temps
            y += mMediumTextSize + mTextPadding;
            x = xCenter - mMediumThickTextPaint.measureText(tempHigh);
            canvas.drawText(tempHigh, x, y, mMediumThickTextPaint);
            x = xCenter;
            canvas.drawText(tempLow, x, y, mMediumThinTextPaint);
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
            int hour = mCalendar.get(Calendar.HOUR_OF_DAY);
            if (hour < 10) {
                return "0" + String.valueOf(hour);
            }
            return String.valueOf(hour);
        }
    }
}
