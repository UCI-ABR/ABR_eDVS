package carl.abr.edvs;

import java.util.ArrayList;

import android.util.Log;
import carl.abr.edvs.EDVS4337SerialUsbStreamProcessor.EDVS4337Event;

import com.ftdi.j2xx.FT_Device;

//doc: http://www.ksksue.com/docs/ftdi_d2xx_jar/com/ftdi/j2xx/FT_Device.html

public class Thread_read extends Thread
{
	final int USB_DATA_BUFFER = 64000; //2048
	static final String TAG = "Thread_read";
	boolean STOP = false;
	int bytes_read = 0;
	EDVS4337SerialUsbStreamProcessor processor;
	FT_Device ftDevice;
	MainActivity my_activity;
//	ArrayList<EDVS4337Event> events;
//	ArrayList<EDVS4337Event> events_clone;
	String str;
	
	Thread_read(FT_Device dev, MainActivity act)
	{
		ftDevice = dev;
		processor = new EDVS4337SerialUsbStreamProcessor();
		my_activity = act;		
	}

	@Override
	public final void run() 
	{	
		int bytesRead=0;
		int readcount=0;
		int totalBytesRead = 0;

		str = new String();

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
						str = "nb bytes read: " + bytesRead + "\n";

						//process data to get list of events
						ArrayList<EDVS4337Event> events = processor.process(	usbdata, 
													bytesRead, 
													EDVS4337SerialUsbStreamProcessor.EDVS4337EventMode.TS_MODE_E0
												  );
						
//						final ArrayList<EDVS4337Event> events_clone  = (ArrayList<EDVS4337Event>) events.clone();
//						final ArrayList<EDVS4337Event> events_clone  = events;
						
//						my_activity.runOnUiThread(new Runnable() //update gui on its own thread
//						{
//							@Override
//							public void run() 
//							{								
								my_activity.set_events(events, str);              
//							}
//						}); 
//						events_clone = new ArrayList<EDVS4337Event>();
//						for(int i=0; i< events.size(); i++)
//						{
//							EDVS4337Event an_event = events.get(i);
//							EDVS4337Event new_event = new EDVS4337Event(an_event.x, an_event.y, an_event.p, an_event.ts);
//							events_clone.add(new_event);
//						}
						
					}
				}
				else Log.e(TAG,"device closed");
			}	
		}
	}

	/** stops the thread*/
	public synchronized void stop_thread()
	{
		STOP = true;
	}
}
