package carl.abr.edvs;

import java.util.ArrayList;

/**
*
* @author Julien Martel, 2014
*/
public class EDVS4337SerialUsbStreamProcessor 
{
	static final String TAG = "Serial processor";	

	ArrayList<EDVS4337Event>     mEvents;
	ArrayList<EDVS4337ImuEvent>  mImuEvents;
	
	String						mAsciiData;
	int[]                      	mEDVSinCollection;
	long                        mEDVSTimestamp;
	int							mInputProcessingIndex;

	public enum EDVS4337EventMode 
	{
		TS_MODE_E0,
		TS_MODE_E1,
		TS_MODE_E2,
		TS_MODE_E3,
		TS_MODE_E4
	}

	public enum EDVS4337EventImuType 
	{
		IMU_GYRO,
		IMU_ACCELERO,
		IMU_COMPASS,
		IMU_UNKNOWN
	}

	public class EDVS4337Event 
	{
		EDVS4337Event(int x, int y, int p, long ts) 
		{
			this.x = x;
			this.y = y;
			this.p = p;
			this.ts = ts;
		}

		EDVS4337Event() 
		{
			this.x = 0;
			this.y = 0;
			this.p = 0;
			this.ts = 0;
		}

		public int x;
		public int y;
		public int p;
		public long ts;
	}

	public class EDVS4337ImuEvent 
	{
		EDVS4337ImuEvent(int x, int y, int z, EDVS4337EventImuType type, long ts) 
		{
			this.x = x;
			this.y = y;
			this.z = z;
			this.type = type;
			this.ts = ts;
		}

		EDVS4337ImuEvent() 
		{
			this.x = 0;
			this.y = 0;
			this.z = 0;
			this.type = EDVS4337EventImuType.IMU_UNKNOWN;
			this.ts = 0;
		}

		public int                  x;
		public int                  y;
		public int                  z;
		public EDVS4337EventImuType type;
		public long                 ts;
	}
	
	/**
	 * 
	 */
	public EDVS4337SerialUsbStreamProcessor() 
	{
		mEvents 				= new ArrayList<EDVS4337Event>();
		mImuEvents 				= new ArrayList<EDVS4337ImuEvent>();
		mEDVSinCollection 		= new int[512];
		mAsciiData 				= new String();
		mEDVSTimestamp 			= 0;		
		mInputProcessingIndex 	= 0;
	}
	
	/**
	 * 
	 * @param stream
	 * @param bytesRead
	 * @param eventMode
	 * @return
	 */
	public ArrayList<EDVS4337Event> process(byte[] stream, int bytesRead, EDVS4337EventMode eventMode) 
	{		
		if(mEvents.isEmpty()==false) 	mEvents.clear();
		if(mImuEvents.isEmpty()==false) mImuEvents.clear();
		
		for (int n = 0; n < bytesRead; n++) 
		{								
			int c = (int)stream[n];
			mEDVSinCollection[mInputProcessingIndex] = c; 
			mInputProcessingIndex++;
		
			if (((mEDVSinCollection[0]) & 0x80) == 0x80) 
			{
				//create new event to be added to the list
				EDVS4337Event e = new EDVS4337Event();
				
				if ((eventMode == EDVS4337EventMode.TS_MODE_E0) && (mInputProcessingIndex == 2)) 
				{	
					e.x = ((mEDVSinCollection[0]) & 0x7F);
					e.y = ((mEDVSinCollection[1]) & 0x7F);
					e.p = (((mEDVSinCollection[1]) & 0x80) >> 7);
					e.ts = -1;

					mEvents.add(e);
					mInputProcessingIndex = 0;
				}
				else if ((eventMode == EDVS4337EventMode.TS_MODE_E1) && (mInputProcessingIndex > 2)) 
				{
					if ((stream[n] & 0x80) == 0x80) 
					{							
						e.x = ((mEDVSinCollection[0]) & 0x7F);
						e.y = ((mEDVSinCollection[1]) & 0x7F);
						e.p = 1 - (((mEDVSinCollection[1]) & 0x80) >> 7);

						if (mInputProcessingIndex == 3) {
							mEDVSTimestamp += ((mEDVSinCollection[2]) & 0x7F);
						}
						else if (mInputProcessingIndex == 4) {
							mEDVSTimestamp +=  (((mEDVSinCollection[2]) & 0x7F) << 7)
									| ((mEDVSinCollection[3]) & 0x7F);
						}
						else if (mInputProcessingIndex == 5) {
							mEDVSTimestamp += (((mEDVSinCollection[2]) & 0x7F) << 14)
									| (((mEDVSinCollection[3]) & 0x7F) << 7)
									| ((mEDVSinCollection[4]) & 0x7F);
						}
						e.ts = mEDVSTimestamp;

						mEvents.add(e);
						mInputProcessingIndex = 0;
					}
				}
				else if ((eventMode == EDVS4337EventMode.TS_MODE_E2) && (mInputProcessingIndex == 4)) 
				{					
					e.x = ((mEDVSinCollection[0]) & 0x7F);
					e.y = ((mEDVSinCollection[1]) & 0x7F);
					e.p = 1 - (((mEDVSinCollection[1]) & 0x80) >> 7);
					e.ts =   (((mEDVSinCollection[2]) & 0xFF) << 8)
							|  ((mEDVSinCollection[3]) & 0xFF);

					mEvents.add(e);
					mInputProcessingIndex = 0;
				}
				else if ((eventMode == EDVS4337EventMode.TS_MODE_E3) && (mInputProcessingIndex == 5)) 
				{					
					e.x = ((mEDVSinCollection[0]) & 0x7F);
					e.y = ((mEDVSinCollection[1]) & 0x7F);
					e.p = 1 - (((mEDVSinCollection[1]) & 0x80) >> 7);
					e.ts =     (((mEDVSinCollection[2]) & 0xFF) << 16)
							| (((mEDVSinCollection[3]) & 0xFF) << 8)
							|  ((mEDVSinCollection[4]) & 0xFF);

					mEvents.add(e);
					mInputProcessingIndex = 0;
				}
				else if ((eventMode == EDVS4337EventMode.TS_MODE_E4) && (mInputProcessingIndex == 6)) 
				{					
					e.x = ((mEDVSinCollection[0]) & 0x7F);
					e.y = ((mEDVSinCollection[1]) & 0x7F);
					e.p = 1 - (((mEDVSinCollection[1]) & 0x80) >> 7);
					e.ts =   (((mEDVSinCollection[2]) & 0xFF) << 24)
							| (((mEDVSinCollection[3]) & 0xFF) << 16)
							| (((mEDVSinCollection[4]) & 0xFF) << 8)
							|  ((mEDVSinCollection[5]) & 0xFF);

					mEvents.add(e);
					mInputProcessingIndex = 0;
				}
				else if (mInputProcessingIndex > 6) 
				{
					//System.out.println("Error: too long a timestamp... ignoring data");
					mInputProcessingIndex = 0;
				}
			}
			else 
			{
				if ((c & 0x80) == 0x80) 
				{
					mEDVSinCollection[0] = c;
					mInputProcessingIndex = 1;
					mAsciiData = "";
					//System.out.println("Error: set high bit received in ASCII mode... ignoring data");
				}
				else 
				{
					mAsciiData = mAsciiData + (char)(stream[n]);
					if ((char)(stream[n]) == '\n') 
					{
						/*
						EDVS4337ImuEvent imu = new EDVS4337ImuEvent();
						
	                        parseIMUStrData(asciiData,
	                                        imu.m_type,
	                                        imu.m_x,
	                                        imu.m_y,
	                                        imu.m_z);
						 
						 mImuEvents.add(imu);
						 */
						 mInputProcessingIndex = 0;
						 mAsciiData = "";
					}
				}
			}
		}
		return mEvents;
	}
}
