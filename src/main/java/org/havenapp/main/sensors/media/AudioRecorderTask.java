
/*
 * Copyright (c) 2017 Nathanial Freitas / Guardian Project
 *  * Licensed under the GPLv3 license.
 *
 * Copyright (c) 2013-2015 Marco Ziccardi, Luca Bonato
 * Licensed under the MIT license.
 */

package org.havenapp.main.sensors.media;


import android.content.Context;
import android.media.MediaRecorder;
import android.os.Environment;
import android.util.Log;

import org.havenapp.main.PreferenceManager;
import org.havenapp.main.Utils;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AudioRecorderTask extends Thread {
	
	/**
	 * Context used to retrieve shared preferences
	 */
	@SuppressWarnings("unused")
	private Context context;
	
	/**
	 * Shared preferences of the application
	 */
	private PreferenceManager prefs;
	

	/**
	 * Path of the audio file for this instance
	 */
	private File audioPath;

	/**
	 * True iff the thread is recording
	 */
	private boolean recording = false;
	
	/**
	 * Getter for recording data field
	 */
	public boolean isRecording() {
		return recording;
	}

	private AudioRecorderListener mListener;

	public interface AudioRecorderListener
	{
		public void recordingComplete (String path);
	}

	/**
	 * We make recorder protected in order to forse
	 * Factory usage
	 */
	protected AudioRecorderTask(Context context) {
		super();
		this.context = context;
		this.prefs = new PreferenceManager(context);
		Log.i("AudioRecorderTask", "Created recorder");

		// This avoids permission issues and ensures directory creation works
		File fileFolder = new File(context.getExternalFilesDir(null), prefs.getDefaultMediaStoragePath());

		// Ensure the complete directory structure is created
		if (!fileFolder.exists()) {
			boolean created = fileFolder.mkdirs();
			if (!created) {
				Log.e("AudioRecorderTask", "FAILED to create directory: " + fileFolder.getAbsolutePath());
				// Fallback: create just the base directory
				File fallbackDir = new File(context.getExternalFilesDir(null), "haven");
				fallbackDir.mkdirs();
				fileFolder = fallbackDir;
			} else {
				Log.i("AudioRecorderTask", "Successfully created directory: " + fileFolder.getAbsolutePath());
			}
		}

		audioPath = new File(fileFolder, new SimpleDateFormat(Utils.DATE_TIME_PATTERN, Locale.getDefault()).format(new Date()) + ".m4a");

		Log.i("AudioRecorderTask", "Audio will be saved to: " + audioPath.getAbsolutePath());
	}
	
	@Override
	public void run() {

		MicrophoneTaskFactory.pauseSampling();
        
        while (MicrophoneTaskFactory.isSampling()) {
        	try {
				Thread.sleep(50);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        }
		
		recording = true;
		final MediaRecorder recorder = new MediaRecorder();

        recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);

        recorder.setOutputFile(audioPath.toString());
        try {
          recorder.prepare();
        } catch (Exception e){
            e.printStackTrace();
            return;
        }

        try {
			Log.i("AudioRecorderTask", "Start recording");
			recorder.start();
			try {
				Thread.sleep(prefs.getAudioLength());
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			recorder.stop();
			Log.i("AudioRecorderTask", "Stopped recording");
			recorder.release();

			recording = false;
        
    	    MicrophoneTaskFactory.restartSampling();

			if (mListener != null)
				mListener.recordingComplete(audioPath.toString());
		}
		catch (IllegalStateException ise)
		{
			Log.w("AudioRecorderTask","error with media recorder");
		}

	}

	public String getAudioFilePath ()
	{
		return audioPath.toString();
	}

	public void setAudioRecorderListener (AudioRecorderListener listener)
	{
		mListener = listener;
	}
}
