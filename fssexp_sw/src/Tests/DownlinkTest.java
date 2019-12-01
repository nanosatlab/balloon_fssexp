package Tests;

import java.io.IOException;

import Common.FolderUtils;
import Common.Log;
import Common.TimeUtils;
import Configuration.ExperimentConf;
import Downlink.TTC;
import IPCStack.PacketDispatcher;
import InterSatelliteCommunications.FSSProtocol;
import Payload.PayloadDataBlock;
import Storage.FederationPacketsBuffer;
import Storage.PacketExchangeBuffer;
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
		FederationPacketsBuffer fed_buffer = new FederationPacketsBuffer(log, conf, folder);
		PacketExchangeBuffer hk_packets = new PacketExchangeBuffer(log, folder);
		TTC ttc = new TTC(time, conf, log, dispatcher, payload_buffer, fed_buffer);
		FSSProtocol fss = new FSSProtocol(log, payload_buffer, hk_packets, conf, time, dispatcher, ttc);
		
		/* Set some data in the payload buffer */
		PayloadDataBlock payload_data = new PayloadDataBlock();
		for(int i = 0; i < 5; i++) {
			payload_data.sat_id = i;
			payload_buffer.insertData(payload_data);
		}
		
		/* Set some data in the federated buffer */
		PayloadDataBlock fed_data = new PayloadDataBlock();
		for(int j = 0; j < 3; j++) {
			fed_data.sat_id = 10 + j;
			fed_buffer.insertData(fed_data);
		}
		
		System.out.println(fed_buffer.getBottomDataBlock().toString());
		
		dispatcher.start();
		fss.start();
		ttc.start();
		try {
			ttc.join();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
}
