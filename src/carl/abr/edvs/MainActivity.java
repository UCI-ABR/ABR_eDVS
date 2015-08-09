package carl.abr.edvs;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

/**
*
* @author Nicolas Oros and Julien Martel, 2014
*/
public class MainActivity extends Activity 
{
	static final String TAG = "main activity";

	/** text displayed on GUI*/
	TextView text;
	
	/** image displayed on GUI*/
	ImageView iv;

	/** thread handling serial connection to eDVS*/
	Thread_eDVS the_thread;
	
	/** handler taking care of updating GUI*/
	Handler handler;
	
	/** runnable used to update GUI*/
	Runnable runnable;
	
	/**  size image displaying the events*/
	int width_image, height_image;
	


	//****************************************************** Activity functions *********************************************************/
	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		text = (TextView) findViewById(R.id.text_gui);
		iv = (ImageView) findViewById(R.id.an_imageView);
		
		DisplayMetrics dm = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(dm);
		width_image = dm.widthPixels/2;
		height_image = dm.heightPixels/2;
	}


	@Override
	protected void onResume() 
	{
		super.onResume();		
		
		//start thread that will read events
		the_thread = new Thread_eDVS(this);
		the_thread.start();

		//create and start handler used to update GUI
		handler = new Handler();
		runnable = new Runnable() {
			public void run() {
				update_gui();
			}
		};	
		handler.post(runnable);
	}

	@Override
	protected void onPause()
	{
		handler.removeCallbacks(runnable);		
		the_thread.stop_thread();		
		super.onPause();
	}
	
	/**
	 * get data from thread and updates GUI
	 */
	private void update_gui()
	{		
		//get image from thread and display it
		Bitmap ima2 = Bitmap.createScaledBitmap(the_thread.get_image(), width_image, height_image, false);
		iv.setImageBitmap(ima2);					
		text.setText("Bytes read: " + the_thread.get_bytesRead());	//get nb bytes read from thread and display it
		handler.postDelayed(runnable, 50);
	}
}
