package com.example.android.BluetoothChat;

public class ArduinoData {
	boolean up, bottom;
	int a0, a1, a2;
	float x, y, z;

	public ArduinoData(String[] splits) {
		up = (splits[1] == "0");
		bottom = (splits[2] == "0");
		a0 = Integer.parseInt(splits[3]);
		a1 = Integer.parseInt(splits[4]);
		a2 = Integer.parseInt(splits[5]);
		x = -Float.parseFloat(splits[6]);
		z = Float.parseFloat(splits[7]);
		y = Float.parseFloat(splits[8]);
	}
}
