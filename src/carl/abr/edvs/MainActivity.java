package carl.abr.edvs;

import java.util.ArrayList;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import carl.abr.edvs.EDVS4337SerialUsbStreamProcessor.EDVS4337Event;

import com.ftdi.j2xx.D2xxManager;
import com.ftdi.j2xx.FT_Device;

public class MainActivity extends Activity 
{
	static final String TAG = "main activity";

	public TextView text;	

	public ImageView iv;

	/** ftdi manager*/
	public static D2xxManager ftD2xx = null;

	/** ftdi device plugged to phone*/
	FT_Device ftDev = null;

	/** context of activity*/
	public Context global_context;

	int baudRate = 4000000; 	/* baud rate  921600*/
	byte stopBit = D2xxManager.FT_STOP_BITS_1; 		
	byte dataBit = D2xxManager.FT_DATA_BITS_8; 		
	byte parity = D2xxManager.FT_PARITY_NONE; 		
	short flowControl = D2xxManager.FT_FLOW_RTS_CTS; 	
	final byte XON = 0x11;    /* Resume transmission */
	final byte XOFF = 0x13;    /* Pause transmission */

	int DevCount = -1;
	int currentPortIndex = -1;
	int portIndex = 0;
	boolean bReadTheadEnable = false;
	boolean uart_configured = false;
	int[] m = new int[128*128];

	Thread_read the_thread;
	Handler handler;
	Runnable runnable;
	Bitmap new_ima;
	

	String text_gui;

	ArrayList<EDVS4337Event> the_events;

	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		text = (TextView) findViewById(R.id.text_gui);

		iv = (ImageView) findViewById(R.id.an_imageView);

		//get context
		global_context = this;

		//get ftdi manager		
		try {ftD2xx = D2xxManager.getInstance(this);}
		catch (D2xxManager.D2xxException e) {Log.e("FTDI_HT","getInstance fail!!");}
	}

	public void update_gui()
	{		
		//		Log.e(TAG,"update_gui called");
		
		Bitmap bm = Bitmap.createBitmap(m, 128, 128,Bitmap.Config.ARGB_8888);
		Bitmap bm2 = Bitmap.createScaledBitmap(bm, 500, 500, false);
		iv.setImageBitmap(bm2);		
		text.setText(text_gui);

		for(int i=0; i<128*128; i++)
			m[i] = 0xFF000000;

		handler.postDelayed(runnable, 40);
	}

	public void set_events(ArrayList<EDVS4337Event> ev, String txt)
	{
		text_gui = new String(txt);

		if(ev.isEmpty() == false)
		{
			for(int i=0; i<ev.size(); i++)
			{	
				EDVS4337Event event = ev.get(i);

				if(event.p == 0)
					m[128*event.x + event.y] = 0xFFFF0000;
				else
					m[128*event.x + event.y] = 0xFF00FF00;
			}	
		}
	}

	protected void onResume() 
	{
		super.onResume();

		handler = new Handler();
		runnable = new Runnable() {
			public void run() {
				update_gui();
			}
		};	
		handler.post(runnable);

		if(ftDev == null || ftDev.isOpen() == false)
		{
			Log.e(TAG,"onResume - reconnect");

			createDeviceList();
			if(DevCount > 0)
			{
				connectFunction();
				setConfig();

				//send command to eDVS to start sending events
				String ss = new String("E+\n");
				byte[] text = ss.getBytes();
				sendData(text.length, text);

				//start thread that will read events
				the_thread = new Thread_read(ftDev, this);
				the_thread.start();
			}
		}
	}

	/*
	 * 
	 */
	protected void onPause()
	{
		handler.removeCallbacks(runnable);		
		if(the_thread != null) the_thread.stop_thread();		
		disconnectFunction();		
		super.onPause();
	}

	/*
	 * 
	 */
	public void createDeviceList()
	{
		int tempDevCount = ftD2xx.createDeviceInfoList(global_context);		
		if (tempDevCount > 0)
		{
			if( DevCount != tempDevCount ) DevCount = tempDevCount;
			//			midToast("device found", Toast.LENGTH_LONG);
		}
		else
		{
			DevCount = -1;
			currentPortIndex = -1;
		}
	}

	/*
	 * 
	 */	
	public void connectFunction()
	{
		if( portIndex + 1 > DevCount) portIndex = 0;
		if( currentPortIndex == portIndex && ftDev != null && ftDev.isOpen()==true ) return;

		if(bReadTheadEnable == true)
		{
			bReadTheadEnable = false;
			try {Thread.sleep(50);}
			catch (InterruptedException e) {Log.e(TAG, "pb sleep connect" + e);}
		}

		ftDev = ftD2xx.openByIndex(global_context, portIndex);
		uart_configured = false;

		if(ftDev == null)
		{
			midToast("Open port("+portIndex+") NG!", Toast.LENGTH_LONG);
			return;
		}

		if (ftDev.isOpen() == true)
		{
			//			midToast("port open and ready", Toast.LENGTH_LONG);
			currentPortIndex = portIndex;
		}
		else  midToast("Open port("+portIndex+") NG!", Toast.LENGTH_LONG);	
	}

	/*
	 *  configure port
	 */
	void setConfig()
	{
		ftDev.setBitMode((byte) 0, D2xxManager.FT_BITMODE_RESET); 		// reset to UART mode for 232 devices
		ftDev.setBaudRate(baudRate);
		ftDev.setDataCharacteristics(dataBit, stopBit, parity);
		ftDev.setFlowControl(flowControl, XON, XOFF);		
		uart_configured = true;
	}

	/*
	 *  call this API to show message
	 */
	void midToast(String str, int showTime)
	{
		Toast toast = Toast.makeText(global_context, str, showTime);			
		toast.setGravity(Gravity.CENTER_VERTICAL|Gravity.CENTER_HORIZONTAL , 0, 0);

		TextView v = (TextView) toast.getView().findViewById(android.R.id.message);
		v.setTextColor(Color.YELLOW);
		toast.show();	
	}

	/*
	 * 
	 */
	public void disconnectFunction()
	{
		DevCount = -1;
		currentPortIndex = -1;
		bReadTheadEnable = false;
		try 
		{
			Thread.sleep(50);
		}
		catch (InterruptedException e) {Log.e(TAG, "pb sleep" + e);}

		if(ftDev != null)
		{
			if(ftDev.isOpen() == true)
			{
				ftDev.close();
			}
		}
	}

	/*
	 * 
	 */	
	void sendData(int numBytes, byte[] buffer)
	{
		if (ftDev.isOpen() == false) 
		{
			Log.e(TAG, "SendData: device not open");
			Toast.makeText(global_context, "Device not open!", Toast.LENGTH_SHORT).show();
			return;
		}

		if (numBytes > 0)
		{
			int nb = ftDev.write(buffer, numBytes);
			//			Toast.makeText(global_context, "Wrote : " + nb, Toast.LENGTH_SHORT).show();
		}		
	}
}
