/*
 * Copyright 2016 Google Inc. All Rights Reserved.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.devrel.vrviewapp;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.vr.sdk.widgets.video.VrVideoEventListener;
import com.google.vr.sdk.widgets.video.VrVideoView;

import java.io.IOException;
import java.util.Locale;
import java.util.Random;

/**
 * Fragment for the Gorilla tab.
 */
public class GorillaFragment extends Fragment {
    private static final String TAG = "GorillaFragment";

    private static final String STATE_IS_PAUSED = "isPaused";
    private static final String STATE_VIDEO_DURATION = "videoDuration";
    private static final String STATE_PROGRESS_TIME = "progressTime";

    private String file = "congo_2048.mp4";

    private VrVideoView videoWidgetView;

    private String[] emotionStates = new String[]{"happy","stress","nostress"};

    private SeekBar seekBar;
    private TextView statusText;

    private boolean isPaused = false;


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.gorilla_fragment, container, false);
        seekBar = (SeekBar) view.findViewById(R.id.seek_bar);
        statusText = (TextView) view.findViewById(R.id.status_text);
        videoWidgetView = (VrVideoView) view.findViewById(R.id.video_view);

        // restore code, pour garder l'etat de la video au reset de la view
        if (savedInstanceState != null) {
            long progressTime = savedInstanceState.getLong(STATE_PROGRESS_TIME);
            videoWidgetView.seekTo(progressTime);
            seekBar.setMax((int) savedInstanceState.getLong(STATE_VIDEO_DURATION));
            seekBar.setProgress((int) progressTime);

            isPaused = savedInstanceState.getBoolean(STATE_IS_PAUSED);
            if(isPaused) {
                videoWidgetView.pauseVideo();
            } else {
                seekBar.setEnabled(false);
            }
        }


        // init Seekbar Listener
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if(fromUser) {
                    videoWidgetView.seekTo(progress);
                    updateStatusText();
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });


        // init VrVideoView listener
        videoWidgetView.setEventListener(new VrVideoEventListener() {

            /**
             * Called by video widget on the UI thread when it's done loading the video.
             */
            @Override
            public void onLoadSuccess() {
                Log.i(TAG, "Video chargee avec succes " + videoWidgetView.getDuration());
                seekBar.setMax((int) videoWidgetView.getDuration());
                seekBar.setEnabled(true);
                updateStatusText();
            }

            /**
             * Called by video widget on the UI thread on any asynchronous error.
             */
            @Override
            public void onLoadError(String errorMessage) {
                Toast.makeText(
                        getActivity(), "Erreur de chargement : " + errorMessage, Toast.LENGTH_LONG)
                                    .show();
                Log.e(TAG, "Erreur de chargement " + errorMessage);
            }

            @Override
            public void onClick() {
                if(isPaused) {
                    videoWidgetView.playVideo();
                } else {
                    videoWidgetView.pauseVideo();
                }

                isPaused = !isPaused;
                updateStatusText();
            }

            /**
             * Update the UI every frame.
             */
            @Override
            public void onNewFrame() {
                updateStatusText();
                seekBar.setProgress((int) videoWidgetView.getCurrentPosition());
            }

            /**
             * Action à effectuer à la fin de la lecture de la video
             */
            @Override
            public void onCompletion() {
                // Randomizer, personne stressée ou non ?
                Random rand = new Random();

                String chosenFeeling = emotionStates[rand.nextInt(3)];

                file = chosenFeeling+".mp4";

                Toast.makeText(getActivity(), "Emotion : " + chosenFeeling, Toast.LENGTH_LONG)
                        .show();

                setUserVisibleHint(true);
                videoWidgetView.seekTo(0);

            }
        });

        return view;
    }

    private void updateStatusText() {
        String status = (isPaused ? "Paused : " : "Playing : ") +
                String.format(Locale.getDefault(), "%.2f", videoWidgetView.getCurrentPosition() / 1000f) +
                " / " +
                videoWidgetView.getDuration() / 1000f +
                " sec.";
        statusText.setText(status);
    }


    // Sauvegarder l'état de la video en cas de rotation de la vue (donc reset de la vue)
    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putLong(STATE_PROGRESS_TIME, videoWidgetView.getCurrentPosition());
        savedInstanceState.putLong(STATE_VIDEO_DURATION, videoWidgetView.getDuration());
        savedInstanceState.putBoolean(STATE_IS_PAUSED, isPaused);
        super.onSaveInstanceState(savedInstanceState);
    }


    @Override
    public void onPause() {
        super.onPause();
        videoWidgetView.pauseRendering();

        isPaused = true;
    }

    @Override
    public void onResume() {
        super.onResume();
        videoWidgetView.resumeRendering();

        updateStatusText();
    }

    @Override
    public void onDestroy() {
        videoWidgetView.shutdown();
        super.onDestroy();
    }

    @Override
    public void setUserVisibleHint(boolean isVisibletoUser) {
        super.setUserVisibleHint(isVisibletoUser);

        if(isVisibletoUser) {
            try {
                //if (videoWidgetView.getDuration() <= 0) {
                    videoWidgetView.loadVideoFromAsset(file,
                            new VrVideoView.Options());
                //}
            } catch (Exception e) {
                Toast.makeText(getActivity(), "Erreur lors de l'ouverture de la video : " + e.getMessage(), Toast.LENGTH_LONG)
                        .show();
            }
        } else {
            isPaused = true;
            if(videoWidgetView != null) {
                videoWidgetView.pauseVideo();
            }
        }
    }
}
