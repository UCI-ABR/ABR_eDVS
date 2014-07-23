package carl.abr.edvs;

import java.util.ArrayList;
import java.util.Arrays;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import carl.abr.edvs.EDVS4337SerialUsbStreamProcessor.EDVS4337Event;
import com.ftdi.j2xx.D2xxManager;
import com.ftdi.j2xx.FT_Device;

/**
 *
 * @author Nicolas Oros and Julien Martel, 2014
 */
public class Thread_eDVS extends Thread
{
	static final String TAG = "Thread_read";	

	/** for stopping the thread*/
	boolean STOP = false;

	/** Reference to context of the main activity*/
	Context context_activity;	

	/** Object processing/parsing data sent by eDVS*/
	EDVS4337SerialUsbStreamProcessor processor;

	/** FTDI manager*/
	D2xxManager ftD2xx;

	/** Serial FTDI device connected to the phone (e.g. eDVS)*/
	FT_Device ftDevice;	

	//******************************** Parameters used for serial connection ******************************/
	int baudRate			= 4000000; 	/* baud rate  921600*/
	byte stopBit			= D2xxManager.FT_STOP_BITS_1; 		
	byte dataBit 			= D2xxManager.FT_DATA_BITS_8; 		
	byte parity 			= D2xxManager.FT_PARITY_NONE; 		
	short flowControl 		= D2xxManager.FT_FLOW_RTS_CTS; 	
	byte XON 				= 0x11;    /* Resume transmission */
	byte XOFF				= 0x13;    /* Pause transmission */

	//******************************** variables used to read data ******************************/
	int USB_DATA_BUFFER 	= 64000; //2048
	int bytesRead			= 0;
	int readcount			= 0;
	int totalBytesRead 		= 0;

	/** Data that will be mapped into a bitmap for display*/
	int[] data_image;
	
	/** Data that will be read from the eDVS over USB*/
	byte[] usbdata;


	/**
	 * 
	 * @param ctxt
	 */
	Thread_eDVS(Context ctxt)
	{
		context_activity 	= ctxt;
		processor 			= new EDVS4337SerialUsbStreamProcessor();
		data_image 			= new int[128*128];
		usbdata 			= new byte[USB_DATA_BUFFER];

		//get ftdi manager		
		try {ftD2xx = D2xxManager.getInstance(context_activity);}
		catch (D2xxManager.D2xxException e) {Log.e("FTDI_HT","getInstance fail!!");}
	}

	/**
	 * main loop of the thread
	 */
	@Override
	public final void run() 
	{	
		//initialize serial connection, and send E+ to eDVS
		init();

		while(STOP == false)
		{	
			try {sleep(10);} catch (InterruptedException e) {Log.e(TAG,"pb sleep" +e);}

			//synchronized block of code so the activity handler and this Thread_eDVS do not access data_image at the same time (handler calls get_image())
			synchronized(this)
			{	
				if(ftDevice.isOpen())
				{
					//get nb of bytes in receive queue
					readcount = ftDevice.getQueueStatus();
					if (readcount > 0) 
					{					
						if(readcount > USB_DATA_BUFFER) readcount = USB_DATA_BUFFER;

						// read data
						bytesRead = ftDevice.read(usbdata, readcount);
						totalBytesRead += bytesRead;

						//process data to get list of events
						ArrayList<EDVS4337Event> events = processor.process(usbdata, bytesRead, EDVS4337SerialUsbStreamProcessor.EDVS4337EventMode.TS_MODE_E0);

						//create image data from events
						for(int i=0; i<events.size(); i++)
						{	
							EDVS4337Event event = events.get(i);

							if(event.p == 0) data_image[128*event.x + event.y] = 0xFFFF0000;
							else 			 data_image[128*event.x + event.y] = 0xFF00FF00;
						}
						
						//reset usbdata...might not need this
						Arrays.fill(usbdata, (byte) 0);
					}
				}
				else Log.e(TAG,"device closed");
			}	
		}
	}

	/**
	 * function that will create the FTDI device list, connect to first device (eDVS), configure the serial connection, and finally send the command E+ causing the eDVS to start sending events. 
	 */
	private void init()
	{
		int DevCount = ftD2xx.createDeviceInfoList(context_activity);

		if(DevCount > 0)
		{
			ftDevice = ftD2xx.openByIndex(context_activity, 0);					
			ftDevice.setBitMode((byte) 0, D2xxManager.FT_BITMODE_RESET); 		// reset to UART mode for 232 devices
			ftDevice.setBaudRate(baudRate);
			ftDevice.setDataCharacteristics(dataBit, stopBit, parity);
			ftDevice.setFlowControl(flowControl, XON, XOFF);

			//send command to eDVS to start sending events
			String ss = new String("E+\n");
			byte[] text = ss.getBytes();

			if(ftDevice.isOpen()) ftDevice.write(text, text.length);	
		}
	}

	/**
	 * 
	 * @return
	 */
	public synchronized int get_bytesRead()
	{
		return bytesRead;
	}

	/**
	 * function called by activity handler to get image from events and data_image.
	 * Synchronized so the handler and the Thread_eDVS do not access data_image at the same time
	 * @return
	 */
	public synchronized Bitmap get_image()
	{
		Bitmap ima = Bitmap.createBitmap(data_image, 128, 128,Bitmap.Config.ARGB_8888);

		//reset data_image
		Arrays.fill(data_image, 0xFF000000);
		
//		for(int i=0; i<128*128; i++)
//			data_image[i] = 0xFF000000;	//reset data_image

		return ima;
	}

	/** 
	 * stops the thread
	 */
	public synchronized void stop_thread()
	{
		if(ftDevice != null)
		{
			if(ftDevice.isOpen()) ftDevice.close();
		}

		STOP = true;
	}
}


