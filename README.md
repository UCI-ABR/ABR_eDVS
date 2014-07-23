ABR_eDVS
========

Android App used to connect to a eDVS camera using serial over USB.
In order to read the sensory information sent by the eDVS from the Android phone, we used the JAVA D2xx driver and library provided by FTDI Chip. 
D2xx is used for FTDI USB to UART devices interaction.

IMPORTANT:

- in AndroidManifest.xml: feature usb.host, intent-filter USB_DEVICE_ATTACHED, and meta-data device_filter
      
- in res/xml folder, file device_filter (list of devices that will be recognized when plugged to the phone)

- in libs folder, d2xx.jar (FTDI libary used for serial over USB communication with FTDI Chip devices)


For more information, go to:
https://neuromorphs.net/nm/wiki/AndroideDVS

The D2xx library and documentation can be found here:

- http://www.ftdichip.com/Android.htm
- http://www.ftdichip.com/Support/Documents/AppNotes/AN_233_Java_D2xx_for_Android_API_User_Manual.pdf

If you have questions, go to:
https://groups.google.com/forum/?hl=en#!forum/android-based-robotics


Requirements
------------

- Android 4.0 or above
