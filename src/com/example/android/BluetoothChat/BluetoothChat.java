/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.BluetoothChat;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.opengles.GL10;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.SharedPreferences;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.threed.jpct.Camera;
import com.threed.jpct.FrameBuffer;
import com.threed.jpct.Light;
import com.threed.jpct.Loader;
import com.threed.jpct.Logger;
import com.threed.jpct.Matrix;
import com.threed.jpct.Object3D;
import com.threed.jpct.Primitives;
import com.threed.jpct.RGBColor;
import com.threed.jpct.SimpleVector;
import com.threed.jpct.Texture;
import com.threed.jpct.TextureManager;
import com.threed.jpct.World;
import com.threed.jpct.util.MemoryHelper;

/**
 * This is the main Activity that displays the current chat session.
 */
public class BluetoothChat extends Activity {
    // Debugging
    private static final String TAG = "BluetoothChat";
    private static final boolean D = true;

    // Message types sent from the BluetoothChatService Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;
	public static final int MESSAGE_ARDUINO_DATA_READ = 6;
	public static final int MESSAGE_RESET_POSITION = 7;
	
    // Key names received from the BluetoothChatService Handler
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";

    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;

    // Layout Views
    private TextView mTitle;
	private ListView mCaloriesListView;
	private Button mCalibrateButton;
	// private EditText mOutEditText;
	// private Button mSendButton;

    // Name of the connected device
    private String mConnectedDeviceName = null;
    // Array adapter for the conversation thread
	// private ArrayAdapter<String> mConversationArrayAdapter;
	private ArrayAdapter<String> mCaloriesArrayAdapter;
    // String buffer for outgoing messages
    private StringBuffer mOutStringBuffer;
    // Local Bluetooth adapter
    private BluetoothAdapter mBluetoothAdapter = null;
    // Member object for the chat services
    private BluetoothChatService mChatService = null;
	// Used to handle pause and resume...
	private static BluetoothChat master = null;

	// GL view 3D objects
	private GLSurfaceView mGLView;
	private MyRenderer renderer = null;
	private FrameBuffer fb = null;
	private World world = null;
	private RGBColor back = new RGBColor(50, 50, 100);
	private float touchTurn = 0;
	private float touchTurnUp = 0;
	private float xpos = -1;
	private float ypos = -1;
	private int fps = 0;
	private boolean gl2 = true;
	private Light sun = null;
	private Object3D spoon = null;
	private Object3D liquid[];
	
	// Volume and user state information
	private enum VOLUME {
		EMPTY, LOW, HALF, FULL;
	}
	private VOLUME volume = VOLUME.EMPTY;
	private enum USER_STATE {
		INITIAL, HAS_VOLUME, ATTEMPT_EAT, END
	}
	private USER_STATE userState = USER_STATE.INITIAL;
	private int samples_before_check_mouth_pin = 0;
	
	// Rotation information read from IMU
	private float rotateX = 0;
	private float rotateY = 0;
	private float rotateZ = 0;
	private float referenceX = 0;
	private float referenceY = 0;
	private float referenceZ = 0;
	private static final float TILT_THRESHOLD = 0.3f;   // Threshold to check if it is not tilting
	private static final int SAMPLES_BEFORE_CHECK_MOUTH_PIN = 5;  	// Number of samples needed before check mouth pin;
																	// Because the mouth pin is corrupted just after spoon leaves bowl;
	
	// Food type information array
	private String foodTypeFileName = "foodType.csv";
	private FoodType[] foodTypeArray;
	private FoodType currFoodType;
	private static final int maxADCValue = 1023;
	
	// Real spoon volumes in ml
	// TODO: Match it in real cases, Try make it configurable
	public static final int EMPTY_VOLUME = 0;
	public static final int LOW_VOLUME = 10;
	public static final int HALF_VOLUME = 20;
	public static final int FULL_VOLUME = 30;
	
	// Calories summary
	private int totalCalories = 0;
	public static final String MY_PREFS_NAME = "SmartSpoonPrefsFile";
	
	// Connection to SmartSpoon
	private static final String SMARTSPOON_MAC = "00:06:66:04:AF:90";
	private boolean TRY_CONNECT_SMARTSPOON = false;

	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(D) Log.e(TAG, "+++ ON CREATE +++");

        // Set up the window layout
//        requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
        setContentView(R.layout.main);
//        getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.custom_title);

        // Set up the custom title
		mTitle = (TextView) findViewById(R.id.title_left_text);

        // Get local Bluetooth adapter
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
//		BluetoothAdapter.getDefaultAdapter().enable();

        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        
        //TODO: Read file for food type information
        readFileForFoodType(foodTypeFileName);

		// Deal with GLSurfaceView
		if (master != null) {
			copy(master);
		}
		mGLView = (GLSurfaceView) findViewById(R.id.glview);
		if (gl2) {
			mGLView.setEGLContextClientVersion(2);
		} else {
			mGLView.setEGLConfigChooser(new GLSurfaceView.EGLConfigChooser() {
				public EGLConfig chooseConfig(EGL10 egl, EGLDisplay display) {
					// Ensure that we get a 16bit framebuffer. Otherwise, we'll
					// fall back to Pixelflinger on some device (read: Samsung
					// I7500). Current devices usually don't need this, but it
					// doesn't hurt either.
					int[] attributes = new int[] { EGL10.EGL_DEPTH_SIZE, 16,
							EGL10.EGL_NONE };
					EGLConfig[] configs = new EGLConfig[1];
					int[] result = new int[1];
					egl.eglChooseConfig(display, attributes, configs, 1, result);
					return configs[0];
				}
			});
		}
		renderer = new MyRenderer();
		if(renderer != null ){
			renderer.initializeTexture();
		}
		mGLView.setRenderer(renderer);
    }

    @Override
    public void onStart() {
        super.onStart();
        if(D) Log.e(TAG, "++ ON START ++");

        // If BT is not on, request that it be enabled.
        // setupChat() will then be called during onActivityResult
        if (!mBluetoothAdapter.isEnabled()) {
//			 Intent enableIntent = new
//			 Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
//			 startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        // Otherwise, setup the chat session
        } else {
            if (mChatService == null) setupChat();
        }
    }

    @Override
    public synchronized void onResume() {
        super.onResume();
        if(D) Log.e(TAG, "+ ON RESUME +");

        // Performing this check in onResume() covers the case in which BT was
        // not enabled during onStart(), so we were paused to enable it...
        // onResume() will be called when ACTION_REQUEST_ENABLE activity returns.
        if (mChatService != null) {
            // Only if the state is STATE_NONE, do we know that we haven't started already
            if (mChatService.getState() == BluetoothChatService.STATE_NONE) {
              // Start the Bluetooth chat services
              mChatService.start();
            }
        }
		mGLView.onResume();
		SharedPreferences prefs = getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE);
		totalCalories = prefs.getInt("totalCalories", 0);
		updateCaloriesSummary();
    }

    private void setupChat() {
        Log.d(TAG, "setupChat()");

        // Initialize the array adapter for the conversation thread
		mCaloriesArrayAdapter = new ArrayAdapter<String>(this, R.layout.message);
		mCaloriesListView = (ListView) findViewById(R.id.calories_list);
		mCaloriesListView.setAdapter(mCaloriesArrayAdapter);
		mCaloriesArrayAdapter.add(getResources().getString(
				R.string.calories_list_title));
		mCalibrateButton = (Button)findViewById(R.id.button_calibrate);
		mCalibrateButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v){
				if (mChatService.getState() != BluetoothChatService.STATE_CONNECTED) {
					mTitle.setText(R.string.title_not_connected_calibrate);
					return;
				}
				
				// Set calibration message
				mChatService.sendCalibrationMessage.set(true);
				mCalibrateButton.setEnabled(false);

				// Wait until calibration finished;
				new CountDownTimer(5000, 1000) {
					public void onTick(long millisUntilFinished) {
						mTitle.setText(getResources().getString(
								R.string.calibrating)
								+ millisUntilFinished / 1000);
					}
					public void onFinish() {
						mTitle.setText(R.string.calibrate_done);
					}
				}.start();
				mCalibrateButton.setEnabled(true);
			}
			});
		
        // Initialize the BluetoothChatService to perform bluetooth connections
        mChatService = new BluetoothChatService(this, mHandler);

        // Initialize the buffer for outgoing messages
        mOutStringBuffer = new StringBuffer("");
    }

    @Override
    public synchronized void onPause() {
        super.onPause();
        if(D) Log.e(TAG, "- ON PAUSE -");
		mGLView.onPause();
    }

    @Override
    public void onStop() {
    	// Save calories data
    	SharedPreferences.Editor mEditor = getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE).edit();
    	mEditor.putInt("totalCalories", totalCalories).commit();
        super.onStop();
        if(D) Log.e(TAG, "-- ON STOP --");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Stop the Bluetooth chat services
        if (mChatService != null) mChatService.stop();
        if(D) Log.e(TAG, "--- ON DESTROY ---");
    }

    private void ensureDiscoverable() {
        if(D) Log.d(TAG, "ensure discoverable");
        if (mBluetoothAdapter.getScanMode() !=
            BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivity(discoverableIntent);
        }
    }
    
    /**
     * Update calories summary message 
     */
    private void updateCaloriesSummary(){
    	String summary = String.format("Total Calories:\n%d cals\n", totalCalories);
    	TextView summaryTextView = (TextView)findViewById(R.id.calories_summary);
    	summaryTextView.setText(summary);
    }

    /**
     * Sends a message.
     * @param message  A string of text to send.
     */
    private void sendMessage(String message) {
        // Check that we're actually connected before trying anything
        if (mChatService.getState() != BluetoothChatService.STATE_CONNECTED) {
            Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT).show();
            return;
        }

        // Check that there's actually something to send
        if (message.length() > 0) {
            // Get the message bytes and tell the BluetoothChatService to write
            byte[] send = message.getBytes();
            mChatService.write(send);

            // Reset out string buffer to zero and clear the edit text field
            mOutStringBuffer.setLength(0);
			// mOutEditText.setText(mOutStringBuffer);
        }
    }

    // The action listener for the EditText widget, to listen for the return key
    private TextView.OnEditorActionListener mWriteListener =
        new TextView.OnEditorActionListener() {
        public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
            // If the action is a key-up event on the return key, send the message
            if (actionId == EditorInfo.IME_NULL && event.getAction() == KeyEvent.ACTION_UP) {
                String message = view.getText().toString();
                sendMessage(message);
            }
            if(D) Log.i(TAG, "END onEditorAction");
            return true;
        }
    };
    
    // Set food type based on foodTypeArray
    private void setFoodTypeByL0Voltage(int a0){
    	int minDis = maxADCValue;
    	int dis;
    	if(foodTypeArray != null){
	    	for(FoodType f : foodTypeArray){
	    		dis = Math.abs(f.getVoltageAtL0()-a0);
	    		if(dis < minDis){
	    			currFoodType = f;
	    			minDis = dis;
	    		}
	    	}
	    	if(renderer!=null)
	    		renderer.setLiquidTexture(currFoodType.getFoodName());
    	}else{
    		Log.d("setFoodTypeByL0Voltage", "Lack food type information to set Food Type, set as default milk");
    		currFoodType = new FoodType();
    	}
    }

	// Read meassage from Arduino, update user state of spoon
	private void updateUserState(ArduinoData a) {
		double thresh = 900.0f;
		int vol_after = 0;
		VOLUME volume_after = VOLUME.EMPTY;
		rotateX = a.x;
		rotateY = a.y;
		rotateZ = a.z;

		// Return to initial state if spoon is inside food
		if (a.bottom) {
			volume = VOLUME.EMPTY;
			userState = USER_STATE.INITIAL;
			return;
		}
//		mCaloriesArrayAdapter.add("Get data: "+a.mouth+" "+a.bottom+" "+a.a0+" "+a.a1+" "+a.a2);

		switch (userState) {
		case INITIAL:
			// TODO: Check if not tilt, Read AD value
			if(Math.abs(rotateX-referenceX)< TILT_THRESHOLD && Math.abs(rotateZ-referenceZ)<TILT_THRESHOLD ){
				
				// Read a.a2 the highest AD value
				// AD should be lower than threshold if connected by liquid
				if (a.a2 < thresh && a.a1 < thresh && a.a0 < thresh) {
					volume = VOLUME.FULL;
				} else if (a.a1 < thresh && a.a0 <thresh) {
					volume = VOLUME.HALF;
				} else if (a.a0 < thresh) {
					volume = VOLUME.LOW;
				}
				
				if (volume != VOLUME.EMPTY) {
					userState = USER_STATE.HAS_VOLUME;
					volume_after = volume;
					
					// Initialize time_in_air
					// Give some time to hold the food before check user attempt
					samples_before_check_mouth_pin = 0;
					mCaloriesArrayAdapter.add("Spoon has Volume");
					// Get food type
					setFoodTypeByL0Voltage(a.a0);
				}
			}
			break;
		case HAS_VOLUME:
			samples_before_check_mouth_pin = samples_before_check_mouth_pin + 1;
			if(Math.abs(rotateX-referenceX)< TILT_THRESHOLD && Math.abs(rotateZ-referenceZ)<TILT_THRESHOLD ){
				if (a.a0 > thresh) {
					userState = USER_STATE.INITIAL;
					volume = VOLUME.EMPTY;
				}
			}
			// Assure the time in air is larger than 2 * 200 ms to avoid false
			// positive on user attempt
			if (a.mouth && samples_before_check_mouth_pin > SAMPLES_BEFORE_CHECK_MOUTH_PIN) {
				userState = USER_STATE.ATTEMPT_EAT;
				mCaloriesArrayAdapter.add("Attempt to eat!");
			}
			break;
		case ATTEMPT_EAT:
			// Update calorie list when user finished eating
			// Check volume after eating when not tilt
			if(Math.abs(rotateX-referenceX)< TILT_THRESHOLD && Math.abs(rotateZ-referenceZ)<TILT_THRESHOLD ){
				if (a.a2 < thresh && a.a1 < thresh && a.a0 < thresh) {
					volume_after = VOLUME.FULL;
				} else if (a.a1 < thresh && a.a0 <thresh) {
					volume_after = VOLUME.HALF;
				} else if (a.a0 < thresh) {
					volume_after = VOLUME.LOW;
				}
			}
			if (volume_after!=volume) {
				// Consumed calorie according to volume
				if(currFoodType!=null){
					int vol = 0;
					switch(volume){
					case LOW: vol = LOW_VOLUME;
								break;
					case HALF: vol = HALF_VOLUME;
								break;
					case FULL: vol = FULL_VOLUME;
								break;
					default: vol = 0;
						break;
					}
					switch(volume_after){
					case LOW: vol -= LOW_VOLUME;
								break;
					case HALF: vol -= HALF_VOLUME;
								break;
					case FULL: vol -= FULL_VOLUME;
								break;
					default:
						break;
					}
					int cal = currFoodType.getCalorie() * vol;
					mCaloriesArrayAdapter.add(getString(R.string.food_type) + " "+currFoodType.getFoodName()
							+ " "+getString(R.string.volume) + vol + getString(R.string.calories)
							+ cal + "cals");
					totalCalories += cal;
					updateCaloriesSummary();
				}else{
					Log.d("updateUserState","Lack food type information to update calorie list");
				}
				userState = USER_STATE.INITIAL;
				volume = volume_after;
			}
			break;
		default:
			break;
		}
	}

    // The Handler that gets information back from the BluetoothChatService
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case MESSAGE_STATE_CHANGE:
                if(D) Log.i(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
                switch (msg.arg1) {
                case BluetoothChatService.STATE_CONNECTED:
                    mTitle.setText(R.string.title_connected_to);
                    mTitle.append(mConnectedDeviceName);
					// mConversationArrayAdapter.clear();
                    mCaloriesArrayAdapter.clear();
                    if(spoon != null){
                    	spoon.setVisibility(true);
                    }
                    break;
                case BluetoothChatService.STATE_CONNECTING:
                    mTitle.setText(R.string.title_connecting);
                    break;
                case BluetoothChatService.STATE_LISTEN:
                case BluetoothChatService.STATE_NONE:
                    mTitle.setText(R.string.title_not_connected);
                    if(spoon != null){
                    	spoon.setVisibility(false);
                    }
                    break;
                }
                break;
            case MESSAGE_WRITE:
                byte[] writeBuf = (byte[]) msg.obj;
                // construct a string from the buffer
                String writeMessage = new String(writeBuf);
				// mConversationArrayAdapter.add("Me:  " + writeMessage);
                break;
            case MESSAGE_READ:
//                byte[] readBuf = (byte[]) msg.obj;
                // construct a string from the valid bytes in the buffer
//                String readMessage = new String(readBuf, 0, msg.arg1);  
//                android.util.Log.v("!! BUG !!", String.format("Handler::MESSAGE_READ: Number of bytes: %d", msg.arg1));
//                String readMessage = (String) msg.obj;
				// mConversationArrayAdapter.add(mConnectedDeviceName + ":  "
				// + readMessage);
                break;
			case MESSAGE_ARDUINO_DATA_READ:
				// Parse message from Arduino
				// updateUserState(readMessage)
				ArduinoData arduinoData = (ArduinoData) msg.obj;
				boolean isCalibrationFrame = msg.arg2 == 1 ? true : false;
				
				if (isCalibrationFrame) {
					referenceX = arduinoData.x;
					referenceY = arduinoData.y;
					referenceZ = arduinoData.z;
				}
				updateUserState(arduinoData);
//				mCaloriesArrayAdapter.add(
//						String.format("%f %f %f ", arduinoData.x, arduinoData.y, arduinoData.z));
				break;
            case MESSAGE_DEVICE_NAME:
                // save the connected device's name
                mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
                Toast.makeText(getApplicationContext(), "Connected to "
                               + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                break;
            case MESSAGE_TOAST:
                Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST),
                               Toast.LENGTH_SHORT).show();
                if(TRY_CONNECT_SMARTSPOON){
                	connectSmartSpoon();
                }
                break;
			case MESSAGE_RESET_POSITION:
				resetSpoonPosition();
				break;
            }
        }
    };
    
    private void resetSpoonPosition(){
    	if(spoon!=null){
    		spoon.clearRotation();
			// Rotate spoon to position able to show spoon content
			spoon.rotateX(-0.6f);
			spoon.rotateY(-0.1f);
    	}
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(D) Log.d(TAG, "onActivityResult " + resultCode);
        switch (requestCode) {
        case REQUEST_CONNECT_DEVICE:
            // When DeviceListActivity returns with a device to connect
            if (resultCode == Activity.RESULT_OK) {
                // Get the device MAC address
                String address = data.getExtras()
                                     .getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
                Log.d("OnActivityResult","get address: "+address);
                // Get the BLuetoothDevice object
                BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
                // Attempt to connect to the device
                mChatService.connect(device);
            }
            break;
        case REQUEST_ENABLE_BT:
            // When the request to enable Bluetooth returns
            if (resultCode == Activity.RESULT_OK) {
                // Bluetooth is now enabled, so set up a chat session
                setupChat();
            } else {
                // User did not enable Bluetooth or an error occured
                Log.d(TAG, "BT not enabled");
                Toast.makeText(this, R.string.bt_not_enabled_leaving, Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.option_menu, menu);
        return true;
    }

    /**
     * Connect to SmartSpoon Mac Address
     */
    private void connectSmartSpoon(){
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(SMARTSPOON_MAC);
        // Attempt to connect to the device
        mChatService.connect(device);
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.connect_smartspoon:
        	// Connect to SmartSpoon Mac, reconnect if failed
            connectSmartSpoon();
            TRY_CONNECT_SMARTSPOON = true;
            return true;
        case R.id.scan:
            // Launch the DeviceListActivity to see devices and do scan
            Intent serverIntent = new Intent(this, DeviceListActivity.class);
            startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
            return true;
        case R.id.discoverable:
            // Ensure this device is discoverable by others
            ensureDiscoverable();
            return true;
        }
        return false;
    }

	private void copy(Object src) {
		try {
			Logger.log("Copying data from master Activity!");
			Field[] fs = src.getClass().getDeclaredFields();
			for (Field f : fs) {
				f.setAccessible(true);
				f.set(this, f.get(src));
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
/*
	public boolean onTouchEvent(MotionEvent me) {

		if (me.getAction() == MotionEvent.ACTION_DOWN) {
			xpos = me.getX();
			ypos = me.getY();
			return true;
		}

		if (me.getAction() == MotionEvent.ACTION_UP) {
			xpos = -1;
			ypos = -1;
			touchTurn = 0;
			touchTurnUp = 0;
			return true;
		}

		if (me.getAction() == MotionEvent.ACTION_MOVE) {
			float xd = me.getX() - xpos;
			float yd = me.getY() - ypos;

			xpos = me.getX();
			ypos = me.getY();

			touchTurn = xd / -100f;
			touchTurnUp = yd / -100f;
			return true;
		}

		try {
			Thread.sleep(15);
		} catch (Exception e) {
			// No need for this...
		}

		return super.onTouchEvent(me);
	}
	*/
	
	protected boolean isFullscreenOpaque() {
		return true;
	}
	
	private void readFileForFoodType(String fileName){
		BufferedReader br = null;
		String line = "";
		String delimiter = ",";
		List<FoodType> temps = new ArrayList<FoodType>();
		FoodType foodType = null;
	 
		try {
			br = new BufferedReader(new InputStreamReader(getAssets().open(foodTypeFileName)) );
			br.readLine(); //Skip first line
			while ((line = br.readLine()) != null) {
					foodType = new FoodType(line, delimiter);
					if(foodType.getFoodName()!=null){
						temps.add(foodType);
					}else{
						Log.d("readFileForFoodType","Invalid food type file format, ignore");
					}
			}
			foodTypeArray = temps.toArray(new FoodType[0]);
			for (FoodType f : foodTypeArray){
				Log.d("readFileForFoodType",String.format("FoodType: %s,Voltage: %d", f.getFoodName(), f.getVoltageAtL0()));
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	class MyRenderer implements GLSurfaceView.Renderer {

		private long time = System.currentTimeMillis();

		public MyRenderer() {
		}

		public void onSurfaceChanged(GL10 gl, int w, int h) {
			if (fb != null) {
				fb.dispose();
			}

			if (gl2) {
				fb = new FrameBuffer(w, h); // OpenGL ES 2.0 constructor
			} else {
				fb = new FrameBuffer(gl, w, h); // OpenGL ES 1.x constructor
			}

			if (master == null) {
				world = new World();
				world.setAmbientLight(20, 20, 20);

				sun = new Light(world);
				sun.setIntensity(250, 250, 250);

				// TODO: Unsure of the texture size to be used here.
				// Use 256x256 for now.
				// Create a texture of single color: red
				Texture textureRed = new Texture(256, 256, RGBColor.RED);
				Texture textureWhite = new Texture(256, 256, RGBColor.WHITE);
				TextureManager.getInstance().addTexture("textureRed",
						textureRed);
				TextureManager.getInstance().addTexture("textureWhite",
						textureWhite);

				// Load Spoon Model
				spoon = loadModel("Spoon.3DS", 1.0f);
				spoon.calcTextureWrapSpherical();
				spoon.setTexture("textureRed");
				spoon.strip();

				resetSpoonPosition();

				// The Position of each level of liquid, (x,z) set at the center, y determines the height of level
				SimpleVector[] pos;
				pos = new SimpleVector[3];
				pos[0] = spoon.getTransformedCenter();
				pos[0].x -= 35;
				pos[0].z -= 13;
				pos[0].y += 5;
				pos[1] = pos[0];
				pos[1].y -= 4;
				pos[2] = pos[1];
				pos[2].y -= 1;

				// Create ellipsoid as content of liquid: liquid0, liquid1, liquid2 at each level
				liquid = new Object3D[3];
				liquid[2] = Primitives.getEllipsoid(20, 14, 0.1f);
				liquid[1] = Primitives.getEllipsoid(20, 11, 0.1f);
				liquid[0] = Primitives.getEllipsoid(20, 8, 0.1f);
				for (int i = 0; i < 3; i++) {
					liquid[i].translate(pos[i]);
					liquid[i].setTexture("textureWhite");
					liquid[i].rotateZ((float) (-0.08 * Math.PI));
					spoon.addChild(liquid[i]);
					world.addObject(liquid[i]);
				}
				world.addObject(spoon);
				world.buildAllObjects();
				
				// Hide spoon until conneciton established
				spoon.setVisibility(false);
				
				Camera cam = world.getCamera();
				cam.moveCamera(Camera.CAMERA_MOVEOUT, 180);
				cam.lookAt(spoon.getTransformedCenter());
				SimpleVector sv = new SimpleVector();
				sv.set(spoon.getTransformedCenter());
				sv.y -= 250;
				sv.z -= 250;
				sun.setPosition(sv);

				MemoryHelper.compact();

				if (master == null) {
					Logger.log("Saving master Activity!");
					// master = Spoon3DActivity.this;
					master = BluetoothChat.this;
				}
			}
		}

		public void onSurfaceCreated(GL10 gl, EGLConfig config) {
		}
		
		public void setLiquidTexture(String textureName){
			for (int i = 0; i < 3; i++) {
				try{
					liquid[i].setTexture(textureName);
				}catch(RuntimeException e){
					e.printStackTrace();
					liquid[i].setTexture("textureWhite");
					Log.d("Renderer.setLiquidTexture()","Unknown texture");
				}
			}
		}
		
		public void initializeTexture(){
			for(FoodType f:foodTypeArray){
				Texture texture = new Texture(256, 256, f.getFoodColor());
				TextureManager.getInstance().addTexture(f.getFoodName(),texture);
			}
		}
		
		public void setLiquidLevel(VOLUME s) {
			for (int i = 0; i < 3; i++) {
				liquid[i].setVisibility(false);
			}
			switch (s) {
			case EMPTY:
				break;
			case LOW:
				liquid[0].setVisibility(true);
				break;
			case HALF:
				liquid[1].setVisibility(true);
				break;
			case FULL:
				liquid[2].setVisibility(true);
				break;
			default:
				break;
			}
		}

		public void onDrawFrame(GL10 gl) {
			/*
			 * if (touchTurn != 0) { spoon.rotateY(touchTurn); touchTurn = 0; }
			 * if (touchTurnUp != 0) { spoon.rotateX(touchTurnUp); touchTurnUp =
			 * 0; }
			 */
			if (rotateX!=0 | rotateY!=0 | rotateZ!=0)
				resetSpoonPosition();
			if (rotateX != 0) {
				spoon.rotateX(rotateX - referenceX);
				rotateX = 0;
			}
			if (rotateY != 0) {
				
				spoon.rotateY(rotateY - referenceY);
				rotateY = 0;
			}
			if (rotateZ != 0) {
				spoon.rotateZ(rotateZ - referenceZ);
				rotateZ = 0;
			}
			setLiquidLevel(volume);
			fb.clear(back);
			world.renderScene(fb);
			world.draw(fb);
			fb.display();
			

			if (System.currentTimeMillis() - time >= 1000) {
				// Logger.log(fps + "fps");
				fps = 0;
				time = System.currentTimeMillis();
			}
			fps++;
		}

		private Object3D loadModel(String filename, float scale) {
			InputStream is;
			Object3D[] model = null;

			try {
				is = getAssets().open(filename);
				model = Loader.load3DS(is, 1.0f);
			} catch (IOException e) {
				e.printStackTrace();
			}

			if (model == null) {
				throw new RuntimeException("Not supposed to get here!");
			}

			Object3D o3d = new Object3D(0);
			Object3D temp = null;
			for (int i = 0; i < model.length; i++) {
				temp = model[i];
				temp.setCenter(SimpleVector.ORIGIN);
				temp.rotateX((float) (-.5 * Math.PI));
				temp.rotateMesh();
				temp.setRotationMatrix(new Matrix());
				o3d = Object3D.mergeObjects(o3d, temp);
				o3d.build();
			}
			return o3d;
		}
	}
}