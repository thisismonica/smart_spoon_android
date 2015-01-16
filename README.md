# smart_spoon_android
Android app of smart spoon with function of 3D animation, bluetooth communication.
Usage:
  - Connect to bluetooth device with connection to IMU
  - Able to update IMU movement in real-time
  - The bluetooth device should send messages following designed communication protocol
  
Acknowledgement:
  - 3D Animation Library: JPCT http://www.jpct.net/wiki/index.php/First_steps
  - Bluetooth Comminication: Google Sample https://code.google.com/p/backport-android-bluetooth/
  
Communication protocol:
  Data sent to Android should follow below format, otherwise it won'y be processed:
  - 1 line string of messgae when received get signal 'G' from Android.
  - message header: "========" (8'=')
  - message footer: "XXXXXXXX" (8'X')
  - message delimiter: " " ( space )
  - message format: "======== b b i i i f0 f1 f2 f3 XXXXXXXX\n" ( b stands for boolean value, i stands for integer value, f0-f3 is one float in byte format from IMU)
  - message example: convert imu float f to byte array imuData[4]
                    "======== 1 0 1023 1023 1023 imuData[0] imuData[1] imuData[2] imuData[3] XXXXXXXX\n"
