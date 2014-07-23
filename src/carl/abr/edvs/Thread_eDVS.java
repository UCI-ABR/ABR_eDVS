package carl.abr.edvs;

import java.util.ArrayList;

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

	final int USB_DATA_BUFFER = 64000; //2048

	/** Reference to context of the main activity*/
	Context context_activity;	

	/** Object processing/parsing data sent by eDVS*/
	EDVS4337SerialUsbStreamProcessor processor;

	/** FTDI manager*/
	static D2xxManager ftD2xx;

	/** Serial FTDI device connected to the phone (e.g. eDVS)*/
	FT_Device ftDevice;	


	//******************************** Parameters used for serial connection ******************************/
	int baudRate			= 4000000; 	/* baud rate  921600*/
	byte stopBit			= D2xxManager.FT_STOP_BITS_1; 		
	byte dataBit 			= D2xxManager.FT_DATA_BITS_8; 		
	byte parity 			= D2xxManager.FT_PARITY_NONE; 		
	short flowControl 		= D2xxManager.FT_FLOW_RTS_CTS; 	
	final byte XON 			= 0x11;    /* Resume transmission */
	final byte XOFF			= 0x13;    /* Pause transmission */

	//******************************** variables used to read data ******************************/
	int bytesRead			= 0;
	int readcount			= 0;
	int totalBytesRead 		= 0;

	/** Data that will be mapped into a bitmap for display*/
	int[] data_image;


	/**
	 * 
	 * @param act
	 */
	Thread_eDVS(Context ctxt)
	{
		context_activity = ctxt;
		processor = new EDVS4337SerialUsbStreamProcessor();
		data_image = new int[128*128];

		//get ftdi manager		
		try {ftD2xx = D2xxManager.getInstance(context_activity);}
		catch (D2xxManager.D2xxException e) {Log.e("FTDI_HT","getInstance fail!!");}
	}

	//****************************************************** main loop of the thread *********************************************************/

	@Override
	public final void run() 
	{	
		init();
		
		while(STOP == false)
		{	
			try {sleep(10);} catch (InterruptedException e) {Log.e(TAG,"pb sleep");}

			synchronized(this)
			{	
				if(ftDevice.isOpen())
				{
					byte[] usbdata = new byte[USB_DATA_BUFFER];

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

						if(events.isEmpty() == false)
						{
							for(int i=0; i<events.size(); i++)
							{	
								EDVS4337Event event = events.get(i);

								if(event.p == 0) data_image[128*event.x + event.y] = 0xFFFF0000;
								else 			 data_image[128*event.x + event.y] = 0xFF00FF00;
							}	
						}

					}
				}
				else Log.e(TAG,"device closed");
			}	
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
	 * 
	 * @return
	 */
	public synchronized Bitmap get_image()
	{
		Bitmap ima = Bitmap.createBitmap(data_image, 128, 128,Bitmap.Config.ARGB_8888);

		for(int i=0; i<128*128; i++)
			data_image[i] = 0xFF000000;	//reset data_image

		return ima;
	}

	/** 
	 * stops the thread
	 */
	public synchronized void stop_thread()
	{
		if(ftDevice != null)
		{
			if(ftDevice.isOpen() == true)ftDevice.close();
		}
		
		STOP = true;
	}

	/**
	 * function that will create the device list calling {@link #createDeviceList()}}, connect to FTDI device by calling  {@link #connectFunction()}, configure the serial connection by calling
	 *  {@link #setConfig()}, and finally sends the command E+ {@link #sendData(int, byte[])} causing the eDVS to start sending events. 
	 */
	public void init()
	{
		if(ftDevice == null || ftDevice.isOpen() == false)
		{
			Log.e(TAG,"onResume - reconnect");

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
	}
}
