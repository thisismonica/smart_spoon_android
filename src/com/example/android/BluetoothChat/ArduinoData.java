package com.example.android.BluetoothChat;

import android.util.Log;

public class ArduinoData {
	boolean mouth, bottom;
	int a0, a1, a2;
	float x, y, z;
	
	/*
	 * Parse Bluetooth message to construct ArduinoData object
	 * 
	 */
	public ArduinoData(String[] splits) {
		try{
			// Digital pin value to check if user touch spoon for eating
			mouth = (splits[1] == "0");
			// Digital pin value to check if spoon inside food
			bottom = (splits[2] == "0");
			
			// 3 Analog values for volume measurement, from Arduino Pro Mini 10bit ADC(range: 0-1023) 
			a0 = Integer.parseInt(splits[3]);
			a1 = Integer.parseInt(splits[4]);
			a2 = Integer.parseInt(splits[5]);
			
			// rotateX direction from IMU and Spoon3D model are opposite
			x = -Float.parseFloat(splits[6]);
			// rotateZ is from IMU's y axis
			z = Float.parseFloat(splits[7]);
			// rotateY is from IMU's z axis
			y = Float.parseFloat(splits[8]);
			
		}catch(IndexOutOfBoundsException e){
			Log.d("ArduinoData", "Arduino data cannot parse, check BluetoothChat mHandler"); 
			e.printStackTrace();
		}
	}
}
