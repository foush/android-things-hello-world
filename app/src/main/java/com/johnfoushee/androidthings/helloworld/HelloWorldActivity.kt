package com.johnfoushee.androidthings.helloworld

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.graphics.Color

import java.util.ArrayList
import java.util.Arrays
import java.util.Timer
import java.util.TimerTask

import com.google.android.things.contrib.driver.ht16k33.AlphanumericDisplay
import com.google.android.things.contrib.driver.apa102.Apa102

import java.io.IOException

class HelloWorldActivity : Activity() {

    private var mDisplay: AlphanumericDisplay? = null
    private var mLedstrip: Apa102? = null
    private var mTimer: Timer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "Hello World Started")

        // Initialize 7-segment display
        try {
            mDisplay = AlphanumericDisplay("I2C1")
            mDisplay!!.setEnabled(true)
            Log.d(TAG, "Initialized I2C Display")
        } catch (e: IOException) {
            throw RuntimeException("Error initializing display", e)
        }

        // Initialize LED strip
        try {
            mLedstrip = Apa102("SPI3.1", Apa102.Mode.BGR)
            mLedstrip!!.brightness = LEDSTRIP_BRIGHTNESS
            val colors = IntArray(7)
            Arrays.fill(colors, Color.BLACK)
            mLedstrip!!.write(colors)
            // Because of a known APA102 issue, write the initial value twice.
            mLedstrip!!.write(colors)

            Log.d(TAG, "Initialized SPI LED strip")
        } catch (e: IOException) {
            throw RuntimeException("Error initializing LED strip", e)
        }

        val sGenerator = MarqueeGenerator(
                "Hello World. My name is John Foushee".split("(?!^)".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray(),
                " ",
                3
        )
        val cGenerator = MarqueeGenerator(
                arrayOf(Color.RED, Color.RED, Color.RED, Color.YELLOW, Color.YELLOW, Color.YELLOW, Color.GREEN, Color.GREEN, Color.GREEN, Color.CYAN, Color.CYAN, Color.CYAN, Color.BLUE, Color.BLUE, Color.BLUE, Color.MAGENTA, Color.MAGENTA, Color.MAGENTA, Color.WHITE, Color.WHITE, Color.WHITE),
                Color.BLACK,
                6
        )
        mTimer = Timer()
        mTimer!!.scheduleAtFixedRate(object : TimerTask() {

            private fun join(arr: ArrayList<String>): String {
                val strBuilder = StringBuilder()
                for (i in arr.indices) {
                    strBuilder.append(arr[i])
                }
                return strBuilder.toString()
            }

            private fun downcast(arr: ArrayList<Int>): IntArray {
                Log.d(TAG, Arrays.toString(arr.toTypedArray()))
                val response = IntArray(arr.size)
                for (i in arr.indices) {
                    response[arr.size - i - 1] = arr[i]
                }
                return response
            }

            override fun run() {
                if (mDisplay != null) {
                    try {
                        mDisplay!!.display(this.join(sGenerator.next(6)))
                    } catch (e: IOException) {
                        Log.e(TAG, "Unable to set display.", e)
                    }

                }
                if (mLedstrip != null) {
                    try {
                        val colors = downcast(cGenerator.next(7))
                        mLedstrip!!.write(colors)
                        // Because of a known APA102 issue, write the initial value twice.
                        mLedstrip!!.write(colors)
                    } catch (e: IOException) {
                        Log.e(TAG, "Unable to set LED strip.", e)
                    }

                }
                if (mDisplay == null && mLedstrip == null) {
                    Log.d(TAG, "Cancelling")
                    this.cancel()
                }
                Log.d(TAG, "This'll run 700 milliseconds later")
            }
        }, 0, 200)
    }

    override fun onDestroy() {
        super.onDestroy()

        if (mDisplay != null) {
            try {
                mDisplay!!.clear()
                mDisplay!!.setEnabled(false)
                mDisplay!!.close()
            } catch (e: IOException) {
                Log.e(TAG, "Error closing display", e)
            } finally {
                mDisplay = null
            }
        }

        if (mLedstrip != null) {
            try {
                mLedstrip!!.write(IntArray(7))
                mLedstrip!!.brightness = 0
                mLedstrip!!.close()
            } catch (e: IOException) {
                Log.e(TAG, "Error closing LED strip", e)
            } finally {
                mLedstrip = null
            }
        }
        if (mTimer != null) {
            mTimer!!.cancel()
            mTimer = null
        }
    }

    private class MarqueeGenerator<T> internal constructor(seedValues: Array<T>, private val emptyVal: T, prefixSize: Int) {
        private var start = 0

        private val values: ArrayList<T>

        private fun getEmptyArray(size: Int, emptyVal: T): ArrayList<T> {
            val response = ArrayList<T>(size + 1)
            for (i in 0..size - 1) {
                response.add(i, emptyVal)
            }
            return response
        }

        init {
            this.values = getEmptyArray(seedValues.size + prefixSize, emptyVal)
            for (i in seedValues.indices) {
                this.values[prefixSize + i] = seedValues[i]
            }
        }

        internal fun next(read: Int): ArrayList<T> {
            Log.d(TAG, "Marquee Generator next() invoked")
            val response = getEmptyArray(read, this.emptyVal)
            val size = values.size
            if (start >= size) {
                start = 0
                return response
            }
            for (i in 0..read - 1) {
                response[i] = if (start + i < size) values[start + i] else emptyVal
            }
            start++
            return response
        }

    }

    companion object {
        private val TAG = HelloWorldActivity::class.java.simpleName

        // Default LED brightness
        private val LEDSTRIP_BRIGHTNESS = 1
    }


}
