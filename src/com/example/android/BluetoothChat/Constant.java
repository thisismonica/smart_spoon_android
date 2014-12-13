package com.example.android.BluetoothChat;

public class Constant {
	public static final boolean DEBUG_MODE = false;
	// IMU Parameters
	public static final float TILT_THRESHOLD = 0.2f;   // Threshold to check if it is not tilting
	public static final int SAMPLES_BEFORE_CHECK_MOUTH_PIN = 10;  	// Number of samples needed before check mouth pin; Used because the mouth pin is corrupted just after spoon leaves bowl
	public static final float EAT_TILT_THRESHOLD = 0.6f; //Tilt threshold to check if user attempt to eat
	
	// ADC Parameters
	public static final int maxADCValue = 1023;
	public final static int AD_CHANGE_THRESHOLD = 900;
	
	// Spoon Volumes in ml
	public static final int EMPTY_VOLUME = 0;
	public static final int LOW_VOLUME = 10;
	public static final int HALF_VOLUME = 20;
	public static final int FULL_VOLUME = 30;
	
	// Calories Data File Name
	public static final String MY_PREFS_NAME = "SmartSpoonPrefsFile";
	
	// Connection to SmartSpoon
	public static final String SMARTSPOON_MAC = "00:06:66:04:AF:90";
	
}
