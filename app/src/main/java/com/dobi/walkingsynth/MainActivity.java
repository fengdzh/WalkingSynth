package com.dobi.walkingsynth;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.dobi.walkingsynth.csound.BaseCsoundActivity;
import com.dobi.walkingsynth.csound.CsoundMusician;

import org.achartengine.GraphicalView;

import java.text.DecimalFormat;

/**
 * Starting point. Sets the whole UI.
 */
public class MainActivity extends BaseCsoundActivity {

    private static final String TAG = "MActivity";

    private static final String PREFERENCES_NAME = "ValuesSet";
    private SharedPreferences preferences;
    private AccelerometerDetector mAccelDetector;
    private AccelerometerGraph mAccelGraph = new AccelerometerGraph();
    private TextView mThreshValTextView;
    private TextView mStepCountTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // config prefs
        preferences = getSharedPreferences(PREFERENCES_NAME, Activity.MODE_PRIVATE);

        // UI default setup
        GraphicalView view = mAccelGraph.getView(this);
        view.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT));
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // handle the click event on the chart
                if (mAccelGraph.isPainting)
                    mAccelGraph.isPainting(false);
                else {
                    mAccelGraph.isPainting(true);
                }
            }
        });
        LinearLayout graphLayout = (LinearLayout)findViewById(R.id.graph_layout);
        graphLayout.addView(view);

        // dynamic button creation
        createButtons();

        // initialize accelerometer
        SensorManager sensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
        mAccelDetector = new AccelerometerDetector(sensorManager, view, mAccelGraph,preferences);
        mAccelDetector.setStepCountChangeListener(new OnStepCountChangeListener() {
            @Override
            public void onStepCountChange(int v) {
                mStepCountTextView.setText(String.valueOf(v));
            }
        });

        final SeekBar seekBar = (SeekBar)findViewById(R.id.offset_seekBar);
        seekBar.setMax(200);
        seekBar.setProgress(seekBar.getMax() / 2);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                AccelerometerProcessing.setThreshold(progress);
                // add display formatting
                final DecimalFormat df = new DecimalFormat("#.##");
                mThreshValTextView.setText(
                        String.valueOf(df.format(AccelerometerProcessing.getThreshold())));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        // get text views
        mThreshValTextView = (TextView)findViewById(R.id.threshval_textView);
        mThreshValTextView.setText(String.valueOf(AccelerometerGraph.THRESH_INIT));
        mStepCountTextView = (TextView)findViewById(R.id.stepcount_textView);
        mStepCountTextView.setText(String.valueOf(0));
    }

    private void createButtons() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT);
        LinearLayout layout = (LinearLayout)findViewById(R.id.buttons_layout);
        for (int i = 0; i < AccelerometerSignals.OPTIONS.length; ++i) {
            final ToggleButton btn = new ToggleButton(this);
            btn.setTextOn(AccelerometerSignals.OPTIONS[i]);
            btn.setTextOff(AccelerometerSignals.OPTIONS[i]);
            btn.setLayoutParams(params);
            btn.setChecked(true);
            final int opt = i; // convert to flag convention
            btn.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    mAccelDetector.setVisibility(opt, isChecked);
                }
            });
            layout.addView(btn);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
    }


    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume register Listeners");
        mAccelDetector.startDetector();

    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause UNregister Listeners");
        mAccelDetector.stopDetector();
    }
}
