package Tests;

import java.io.IOException;

import Common.FolderUtils;
import Common.Log;
import Common.TimeUtils;
import Configuration.ExperimentConf;
import Downlink.TTC;
import IPCStack.PacketDispatcher;
import Storage.PayloadBuffer;

public class DownlinkTest 
{
	public static void main(String args[]) throws IOException
    {
		TimeUtils time = new TimeUtils();
		FolderUtils folder = new FolderUtils(time);
		Log log = new Log(time, folder);
		ExperimentConf conf = new ExperimentConf(log);
		conf.satellite_id = 2;	/* To perform the test */
		conf.port_desc = "/dev/ttyACM1";	/* To perform the test */
		PacketDispatcher dispatcher = new PacketDispatcher(log, conf, time, folder);
		PayloadBuffer payload_buffer = new PayloadBuffer(log, conf, folder);
		TTC ttc = new TTC(time, conf, log, dispatcher, payload_buffer);
		
		dispatcher.start();
		ttc.start();
		try {
			ttc.join();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
}
