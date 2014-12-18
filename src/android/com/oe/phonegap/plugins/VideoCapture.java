package com.oe.phonegap.plugins;

/*
Copyright 2014 minhnt@ownego.com

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */

import java.io.File;
import java.io.IOException;

import com.example.custom_camera.R;

import android.app.Activity;
import android.content.Intent;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

public class VideoCapture extends Activity implements SurfaceHolder.Callback {

    public static final String INTENT_DURATION = "android.intent.extra.durationLimit";

	private MediaRecorder recorder;
	private SurfaceHolder holder;
	private CamcorderProfile camcorderProfile;
	private Camera camera;
	private File outFile;
	
	boolean recording = false;
	boolean usecamera = true;
	boolean previewRunning = false;
	TextView currentTime, durationTime;

	Handler timer = new Handler();

	
	int current = 0;
	int duration = 10;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
				
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);

		camcorderProfile = CamcorderProfile.get(CamcorderProfile.QUALITY_LOW);

		setContentView(R.layout.main);

		SurfaceView cameraView = (SurfaceView) findViewById(R.id.CameraView);
		holder = cameraView.getHolder();
		holder.addCallback(this);
		holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

		duration = getIntent().getIntExtra(INTENT_DURATION, duration);
		duration = 3;
		
		currentTime = (TextView) findViewById(R.id.currenttime);
		durationTime = (TextView) findViewById(R.id.duration);
		
		currentTime.setText(String.valueOf(current));
		durationTime.setText(String.valueOf(duration)+" seconds");

//		cameraView.setClickable(true);
//		cameraView.setOnClickListener(this);
		
		// Sleep 1s
		Runnable r = new Runnable() {
		    @Override
		    public void run(){
		    	initAutoRecord();
		    }
		};

		Handler h = new Handler();
		h.postDelayed(r, 1000);
	}

	private void prepareRecorder() {
		OELog.d("Prepare recorder here");
        recorder = new MediaRecorder();
		recorder.setPreviewDisplay(holder.getSurface());
		
		if (usecamera) {
			camera.unlock();
			recorder.setCamera(camera);
		}
		
		recorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT);
		recorder.setVideoSource(MediaRecorder.VideoSource.DEFAULT);

		recorder.setProfile(camcorderProfile);
		
		OELog.d("file format:"+camcorderProfile.fileFormat);

		// This is all very sloppy
		if (camcorderProfile.fileFormat == MediaRecorder.OutputFormat.THREE_GPP) {
        	try {
				File newFile = File.createTempFile("AutoRecordVideo", ".3gp", Environment.getExternalStorageDirectory());
				recorder.setOutputFile(newFile.getAbsolutePath());
				outFile = newFile;
			} catch (IOException e) {
				OELog.d("Couldn't create file");
				e.printStackTrace();
				finish();
			}
		} else if (camcorderProfile.fileFormat == MediaRecorder.OutputFormat.MPEG_4) {
        	try {
				File newFile = File.createTempFile("AutoRecordVideo", ".mp4", Environment.getExternalStorageDirectory());
				recorder.setOutputFile(newFile.getAbsolutePath());
				outFile = newFile;
			} catch (IOException e) {
				OELog.d("Couldn't create file");
				e.printStackTrace();
				finish();
			}
		} else {
        	try {
				File newFile = File.createTempFile("AutoRecordVideo", ".mp4", Environment.getExternalStorageDirectory());
				recorder.setOutputFile(newFile.getAbsolutePath());
				outFile = newFile;
			} catch (IOException e) {
				OELog.d("Couldn't create file");
				e.printStackTrace();
				finish();
			}
		}
		
		OELog.d("duration:"+duration*1000);
		recorder.setMaxDuration(duration*1000);
//		recorder.setMaxFileSize(5000000); // Approximately 5 megabytes
		
		try {
			recorder.prepare();
		} catch (IllegalStateException e) {
			e.printStackTrace();
			finish();
		} catch (IOException e) {
			e.printStackTrace();
			finish();
		}
	}

	final Runnable countDown = new Runnable() {
	    @Override
	    public void run(){
	    	current++;
	    	if(current>=duration) {
	    		timer.removeCallbacks(countDown);
	    		stopAutoRecord();
	    	} else {
	    		OELog.d("Current time : "+current);
	    		currentTime.setText(String.valueOf(current));
	    		
	    		timer.postDelayed(countDown, 1000);
	    	}
	    }
	};

	public void initAutoRecord() {
		recording = true;
		recorder.start();
		OELog.d("Recording Started");

		countDown.run();
	}
	
	public void stopAutoRecord() {
		if (recording) {
			recorder.stop();
			if (usecamera) {
				try {
					camera.reconnect();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			recorder.release();
			recording = false;
			OELog.d("Recording Stopped");
			
			// finish activity
			finish();
			
//			prepareRecorder();
		} else {
			OELog.d("You cannot stop record because not recording now.");
		}
	}

	public void surfaceCreated(SurfaceHolder holder) {
		OELog.d("surfaceCreated");
		
		if (usecamera) {
			camera = Camera.open();
			
			try {
				camera.setDisplayOrientation(90);
				camera.setPreviewDisplay(holder);
				camera.startPreview();
				previewRunning = true;
			}
			catch (IOException e) {
				OELog.e(e.getMessage());
				e.printStackTrace();
			}	
		}		
		
	}

	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
		OELog.d("surfaceChanged");

		if (!recording && usecamera) {
			if (previewRunning){
				camera.stopPreview();
			}

			try {
				Camera.Parameters p = camera.getParameters();

				 p.setPreviewSize(camcorderProfile.videoFrameWidth, camcorderProfile.videoFrameHeight);
			     p.setPreviewFrameRate(camcorderProfile.videoFrameRate);
				
				camera.setParameters(p);
				
				camera.setDisplayOrientation(90);
				camera.setPreviewDisplay(holder);
				camera.startPreview();
				previewRunning = true;
			}
			catch (IOException e) {
				OELog.e(e.getMessage());
				e.printStackTrace();
			}	
			
			prepareRecorder();	
		}
	}


	public void surfaceDestroyed(SurfaceHolder holder) {
		OELog.d("surfaceDestroyed");
		if (recording) {
			recorder.stop();
			recording = false;
		}
		recorder.release();
		if (usecamera) {
			previewRunning = false;
			//camera.lock();
			camera.release();
		}
		
	}
	
	@Override
	public void finish() {
		// TODO Auto-generated method stub
		
		Intent data = new Intent();
		OELog.d("File: "+Uri.fromFile(outFile));
		data.setData(Uri.fromFile(outFile));
		setResult(Activity.RESULT_OK, data);
		
		super.finish();
	}
}