package com.example.android.BluetoothChat;

import android.util.Log;

import com.threed.jpct.RGBColor;

public class FoodType {
	private String foodName;
	private RGBColor foodColor;
	private int voltageAtL0 = 1023;
	private int calorie = 0;
	
	private static final int numOfFields = 4;
	
	public FoodType(String line, String delimiter){
		String[] splits = line.split(delimiter);
		if(splits.length == numOfFields){
			// 1st field food name
			setFoodName(splits[0]);
			// 2nd field food color
			setFoodColor(hex2Rgb(splits[1]));
			try{
				// 3rd, 4th field the maximum voltage, minimum voltage at L1
				setVoltageAtL0(Integer.parseInt(splits[2]));
				// 5th field calorie
				setCalorie(Integer.parseInt(splits[3]));
			}catch(NumberFormatException e){
				e.printStackTrace();
				Log.d("FoodType", "Wrong Integer Format");
			}
		}else{
			Log.e("FoodType","Error reading file for food type, invalid line: "+line);
		}
	}
	
	public FoodType(){
		setFoodName("Unknown Food");
		setFoodColor(RGBColor.WHITE);
	}
	
	private static RGBColor hex2Rgb(String colorStr) {
		try{
			RGBColor c = new RGBColor(
		            Integer.valueOf( colorStr.substring( 1, 3 ), 16 ),
		            Integer.valueOf( colorStr.substring( 3, 5 ), 16 ),
		            Integer.valueOf( colorStr.substring( 5, 7 ), 16 ) );
			return c;
		}catch(NumberFormatException e){
			e.printStackTrace();
			Log.d("FoodType","Wrong RGB Color Format");
		}
		return RGBColor.WHITE;    
	}
	
	public String getFoodName() {
		return foodName;
	}
	private void setFoodName(String foodName) {
		this.foodName = foodName;
	}
	public RGBColor getFoodColor() {
		return foodColor;
	}
	private void setFoodColor(RGBColor foodColor) {
		this.foodColor = foodColor;
	}
	public int getVoltageAtL0() {
		return voltageAtL0;
	}
	private void setVoltageAtL0(int voltageAtL0) {
		this.voltageAtL0 = voltageAtL0;
	}
	public int getCalorie() {
		return calorie;
	}
	private void setCalorie(int calorie) {
		this.calorie = calorie;
	}

}

