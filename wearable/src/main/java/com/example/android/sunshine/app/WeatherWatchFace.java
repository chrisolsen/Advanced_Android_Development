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

package com.example.android.sunshine.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.util.Log;
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

    private class Engine extends CanvasWatchFaceService.Engine {

        private static final String TAG = "Watch::Engine";
        boolean mLowBitAmbient;
        boolean mBurnProtection;
        boolean mRegisteredTimeZoneReceiver = false;
        boolean mAmbient;

        int mTapCount;

        String mHigh = "";
        String mLow = "";

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

        Bitmap mIcon;
        final BroadcastReceiver mLocalBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Bundle bundle = intent.getExtras();
                mLow = bundle.getString(WeatherDataService.TEMP_LOW, "");
                mHigh = bundle.getString(WeatherDataService.TEMP_HIGH, "");

                int weatherId = bundle.getInt(WeatherDataService.WEATHER_ID, -1);
                int iconId = getIconResourceForWeatherCondition(weatherId);
                mIcon = BitmapFactory.decodeResource(getResources(), iconId);

                invalidate();
            }
        };
        Calendar mCalendar = Calendar.getInstance();
        final BroadcastReceiver mTimeZoneReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String timeZoneId = intent.getStringExtra("time-zone");
                mCalendar = Calendar.getInstance();
                mCalendar.setTimeZone(TimeZone.getTimeZone(timeZoneId));
            }
        };

        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);

            LocalBroadcastManager.getInstance(WeatherWatchFace.this).registerReceiver(
                    mLocalBroadcastReceiver, new IntentFilter(WeatherDataService.TEMP_BROADCAST_NAME));

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

            startService(new Intent(WeatherWatchFace.this, WeatherDataService.class));
        }

        @Override
        public void onDestroy() {
            super.onDestroy();

            LocalBroadcastManager.getInstance(WeatherWatchFace.this).unregisterReceiver(mLocalBroadcastReceiver);
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
            Log.d(TAG, "onDraw");
            mCalendar = Calendar.getInstance();

            // Draw the background.
            if (isInAmbientMode()) {
                canvas.drawColor(Color.BLACK);
            } else {
                canvas.drawRect(0, 0, canvas.getWidth(), canvas.getHeight(), mBackgroundPaint);
            }

            // date values
            SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, MMM d yyyy", Locale.CANADA);
            String date, hour, minute;

            date = dateFormat.format(mCalendar.getTime());
            hour = getHour();
            minute = getMinute();

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

            // temps and icon
            if (mIcon != null && !mHigh.equals("") && !mLow.equals("")) {
                y += 20;
                // Icon: center - iconSize - iconPadding - tempHighOffset
                // y offset not adjusted yet since icon is drawn below the y coord where as fonts
                // are drawn above it.
                x = xCenter - 60 - 30 - mMediumThickTextPaint.measureText(mHigh) / 2;
                canvas.drawBitmap(mIcon, x, y, null);

                // high temp
                y += mMediumTextSize + mTextPadding;
                x = xCenter;
                canvas.drawText(mHigh, x, y, mMediumThickTextPaint);

                x = xCenter + 28 + mMediumThickTextPaint.measureText(mHigh) / 2;
                canvas.drawText(mLow, x, y, mMediumThinTextPaint);
            }
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


        /**
         * Helper method to provide the icon resource id according to the weather condition id returned
         * by the OpenWeatherMap call.
         *
         * @param weatherId from OpenWeatherMap API response
         * @return resource id for the corresponding icon. -1 if no relation is found.
         */
        public int getIconResourceForWeatherCondition(int weatherId) {
            // Based on weather code data found at:
            // http://bugs.openweathermap.org/projects/api/wiki/Weather_Condition_Codes
            if (weatherId >= 200 && weatherId <= 232) {
                return R.drawable.ic_storm;
            } else if (weatherId >= 300 && weatherId <= 321) {
                return R.drawable.ic_light_rain;
            } else if (weatherId >= 500 && weatherId <= 504) {
                return R.drawable.ic_rain;
            } else if (weatherId == 511) {
                return R.drawable.ic_snow;
            } else if (weatherId >= 520 && weatherId <= 531) {
                return R.drawable.ic_rain;
            } else if (weatherId >= 600 && weatherId <= 622) {
                return R.drawable.ic_snow;
            } else if (weatherId >= 701 && weatherId <= 761) {
                return R.drawable.ic_fog;
            } else if (weatherId == 761 || weatherId == 781) {
                return R.drawable.ic_storm;
            } else if (weatherId == 800) {
                return R.drawable.ic_clear;
            } else if (weatherId == 801) {
                return R.drawable.ic_light_clouds;
            } else if (weatherId >= 802 && weatherId <= 804) {
                return R.drawable.ic_cloudy;
            }
            return -1;
        }
    }
}
