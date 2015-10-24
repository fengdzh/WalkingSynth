package com.dobi.walkingsynth;

import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

import com.dobi.walkingsynth.csound.CsoundMusician;

import org.achartengine.GraphicalView;

/**
 * Configuring accelerometer and handling its results.
 */
public class AccelerometerDetector implements SensorEventListener {

    private static final String TAG = AccelerometerDetector.class.getSimpleName();
    /**
     * Suggested periods:
     * DELAY_UI: T ~= 60ms => f = 16,6Hz
     * DELAY_GAME: T ~= 20ms => f = 50Hz
     */
    public static final int CONFIG_SENSOR = SensorManager.SENSOR_DELAY_GAME;

    private int mStepCount = 0;
    private double[] mAccelResult = new double[AccelerometerSignals.count];
    private SensorManager mSensorManager;
    private Sensor mAccel;
    private AccelerometerGraph mAccelGraph;
    private GraphicalView mGraphView;
    private SharedPreferences mPreferences;

    private OnStepCountChangeListener mStepListener;

    /**
     * Listener setting for Step Detected event
     * @param listener a listener.
     */
    public void setStepCountChangeListener(OnStepCountChangeListener listener) {
        mStepListener = listener;
    }

    public AccelerometerDetector(SensorManager sensorManager,GraphicalView view, AccelerometerGraph graph, SharedPreferences prefs) {
        mStepListener = null;
        mPreferences = prefs;
        mSensorManager = sensorManager;
        if (mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null){
            mAccel = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            Log.d(TAG, "Success! There's a accelerometer. Resolution:" + mAccel.getResolution()
                    + " Max range: " + mAccel.getMaximumRange()
                    + "\n Time interval: " + mAccel.getMinDelay() / 1000 + "ms.");
        } else {
            Log.d(TAG, "Failure! No accelerometer.");
        }
        // get graph handles
        mGraphView = view;
        mAccelGraph = graph;
    }

    public void startDetector() {
        // just starts just the accelerometer. It doesn't update the UI.
        if (!mSensorManager.registerListener(this, mAccel, CONFIG_SENSOR)) {
            Log.d(TAG,"The sensor is not supported and unsuccessfully enabled.");
        }
        mAccelGraph.initialize();
    }

    public void stopDetector() {
        mSensorManager.unregisterListener(this, mAccel);
    }

    /**
     * Shows/hides particular signal
     * @param i index of a signal
     * @param show shown/hidden
     */
    public void setVisibility(int i, boolean show) {
        mAccelGraph.setVisibility(i, show);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        // handle accelerometer data
        AccelerometerProcessing.setEvent(event);
        final long eventTime = AccelerometerProcessing.getEventTime();

        mAccelResult[0] = AccelerometerProcessing.calcMagnitudeVector(0);
        mAccelResult[0] = AccelerometerProcessing.calcExpMovAvg(0);
        mAccelResult[1] = AccelerometerProcessing.calcMagnitudeVector(1);
        //Log.d(TAG, "Vec: x= " + mAccelResult[0] + " C=" + eventTime);

        // update graph with value and timestamp
        mAccelGraph.addNewPoints(eventTime, mAccelResult);

        // step detection
        if (AccelerometerProcessing.stepDetected(1)) {
            // step is found!

            // notify potential listeners
            if (mStepListener != null)
                mStepListener.onStepCountChange(eventTime);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}
