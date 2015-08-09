ABR_eDVS
========

Android App used to connect to a eDVS camera using serial over USB. The Android device (phone or tablet) will provide power to the camera.
In order to read the sensory information sent by the eDVS from the Android phone, we used the JAVA D2xx driver and library provided by FTDI Chip. 
D2xx is used for FTDI USB to UART devices interaction.

-phone <--> micro USB male to USB Female OTG cable <--> USB male to mini USB male <--> eDVS


IMPORTANT FILES:

- AndroidManifest.xml, pay attention to: uses-feature usb.host, intent-filter USB_DEVICE_ATTACHED, and meta-data device_filter
      
- res/xml folder: device_filter.xml (list of devices that will be recognized when plugged to the phone, and start the app automatically)

- libs folder: d2xx.jar (FTDI libary used for serial over USB communication with FTDI Chip devices)


The D2xx library and documentation can be found here  (pdf already in app folder):

- http://www.ftdichip.com/Android.htm
- http://www.ftdichip.com/Support/Documents/AppNotes/AN_233_Java_D2xx_for_Android_API_User_Manual.pdf

A FTDI UART Terminal app is also available here:

 https://play.google.com/store/apps/details?id=com.ftdi.j2xx.hyperterm

For more information, go to:
https://neuromorphs.net/nm/wiki/AndroideDVS

http://www.socsci.uci.edu/~jkrichma/ABR/
https://github.com/UCI-ABR

If you have questions, go to:
https://groups.google.com/forum/?hl=en#!forum/android-based-robotics


Requirements
------------

- Android 4.0 or above

- USB OTG cable


Devices tested:
------------------

- samsung galaxy S3

- Nexus 5 (phone)

- Nexus 7 (tablet)

