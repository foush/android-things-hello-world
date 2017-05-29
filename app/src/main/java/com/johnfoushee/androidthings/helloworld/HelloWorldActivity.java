package com.johnfoushee.androidthings.helloworld;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.graphics.Color;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;

import com.google.android.things.contrib.driver.ht16k33.AlphanumericDisplay;
import com.google.android.things.contrib.driver.apa102.Apa102;

import java.io.IOException;

public class HelloWorldActivity extends Activity {
    private static final String TAG = HelloWorldActivity.class.getSimpleName();

    // Default LED brightness
    private static final int LEDSTRIP_BRIGHTNESS = 1;

    private AlphanumericDisplay mDisplay;
    private Apa102 mLedstrip;
    private Timer mTimer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "Hello World Started");

        // Initialize 7-segment display
        try {
            mDisplay = new AlphanumericDisplay("I2C1");
            mDisplay.setEnabled(true);
            Log.d(TAG, "Initialized I2C Display");
        } catch (IOException e) {
            throw new RuntimeException("Error initializing display", e);
        }
        // Initialize LED strip
        try {
            mLedstrip = new Apa102("SPI3.1", Apa102.Mode.BGR);
            mLedstrip.setBrightness(LEDSTRIP_BRIGHTNESS);
            int[] colors = new int[7];
            Arrays.fill(colors, Color.BLACK);
            mLedstrip.write(colors);
            // Because of a known APA102 issue, write the initial value twice.
            mLedstrip.write(colors);

            Log.d(TAG, "Initialized SPI LED strip");
        } catch (IOException e) {
            throw new RuntimeException("Error initializing LED strip", e);
        }

        final MarqueeGenerator sGenerator = new MarqueeGenerator<String>(
                "Hello World. My name is John Foushee".split("(?!^)"),
                " ",
                3
        );
        final MarqueeGenerator cGenerator = new MarqueeGenerator<>(
                new Integer[]{
                        Color.RED,
                        Color.RED,
                        Color.RED,
                        Color.YELLOW,
                        Color.YELLOW,
                        Color.YELLOW,
                        Color.GREEN,
                        Color.GREEN,
                        Color.GREEN,
                        Color.CYAN,
                        Color.CYAN,
                        Color.CYAN,
                        Color.BLUE,
                        Color.BLUE,
                        Color.BLUE,
                        Color.MAGENTA,
                        Color.MAGENTA,
                        Color.MAGENTA,
                        Color.WHITE,
                        Color.WHITE,
                        Color.WHITE
                },
                Color.BLACK,
                6
        );
        mTimer = new Timer();
        mTimer.scheduleAtFixedRate(new TimerTask() {

            private String join(ArrayList<String> arr) {
                StringBuilder strBuilder = new StringBuilder();
                for (int i = 0; i < arr.size(); i++) {
                    strBuilder.append(arr.get(i));
                }
                return strBuilder.toString();
            }

            private int[] downcast(ArrayList<Integer> arr) {
                Log.d(TAG, Arrays.toString(arr.toArray()));
                int[] response = new int[arr.size()];
                for (int i = 0; i < arr.size(); i++) {
                    response[arr.size() - i - 1] = arr.get(i);
                }
                return response;
            }

            @Override
            public void run() {
                if (mDisplay != null) {
                    try {
                        mDisplay.display(this.join(sGenerator.next(6)));
                    } catch (IOException e) {
                        Log.e(TAG, "Unable to set display.", e);
                    }
                }
                if (mLedstrip != null) {
                    try {
                        int[] colors = downcast(cGenerator.next(7));
                        mLedstrip.write(colors);
                        // Because of a known APA102 issue, write the initial value twice.
                        mLedstrip.write(colors);
                    } catch (IOException e) {
                        Log.e(TAG, "Unable to set LED strip.", e);
                    }
                }
                if (mDisplay == null && mLedstrip == null) {
                    Log.d(TAG, "Cancelling");
                    this.cancel();
                }
                Log.d(TAG, "This'll run 700 milliseconds later");
            }
        }, 0, 700);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mDisplay != null) {
            try {
                mDisplay.clear();
                mDisplay.setEnabled(false);
                mDisplay.close();
            } catch (IOException e) {
                Log.e(TAG, "Error closing display", e);
            } finally {
                mDisplay = null;
            }
        }

        if (mLedstrip != null) {
            try {
                mLedstrip.write(new int[7]);
                mLedstrip.setBrightness(0);
                mLedstrip.close();
            } catch (IOException e) {
                Log.e(TAG, "Error closing LED strip", e);
            } finally {
                mLedstrip = null;
            }
        }
        if (mTimer != null) {
            mTimer.cancel();
            mTimer = null;
        }
    }

    private class MarqueeGenerator<T> {
        private int start = 0;

        private ArrayList<T> values;
        private T emptyVal;

        private ArrayList<T> getEmptyArray(int size, T emptyVal) {
            ArrayList<T> response = new ArrayList<T>(size + 1);
            for (int i = 0; i < size; i++) {
                response.add(i, emptyVal);
            }
            return response;
        }

        MarqueeGenerator(T[] seedValues, T emptyVal, int prefixSize) {
            this.values = getEmptyArray(seedValues.length + prefixSize, emptyVal);
            this.emptyVal = emptyVal;
            for (int i = 0; i < seedValues.length; i++) {
                this.values.set(prefixSize + i, seedValues[i]);
            }
        }

        ArrayList<T> next(int read) {
            Log.d(TAG, "Marquee Generator next() invoked");
            ArrayList<T> response = getEmptyArray(read, this.emptyVal);
            int size = values.size();
            if (start >= size) {
                start = 0;
                return response;
            }
            for (int i = 0; i < read; i++) {
                response.set(i, start + i < size ? values.get(start + i) : emptyVal);
            }
            start++;
            return response;
        }

    }



}
