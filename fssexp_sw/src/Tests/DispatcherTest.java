package Tests;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import Common.Constants;
import Common.FolderUtils;
import Common.Log;
import Common.SynchronizedBuffer;
import Common.TimeUtils;
import Configuration.ExperimentConf;
import IPCStack.PacketDispatcher;
import InterSatelliteCommunications.Packet;

public class DispatcherTest {
	
	public static double fromU16ToFloat(int value) {
		byte lsb = (byte)((value >> 8) & 0xFF);
	    byte msb = (byte)(value & 0xFF);
	    if (msb < 0) {
	        return (1.0 * msb - 1.0 / 256.0 * lsb);
	    }else {
	        return (1.0 * msb + 1.0 / 256.0 * lsb);
	    }
	}
	
	public static double rssi_raw_dbm(int rssi_dec) {
	    if (rssi_dec < 128) {
	        return (rssi_dec / 2.0) - 74.0;
	    } else {
	        return ((rssi_dec - 256) / 2.0) - 74.0;
	    }
	}
	
	public static double lqi_status(int lqi)
	{
	    return (1.0 - lqi / 127.0) * 100.0;
	}
	
	public static double snr(double last_rssi, double actual_rssi)
	{
	    return last_rssi - actual_rssi;
	}

    public static void main(String args[]) throws IOException
    {
    	int protocol_number = 2;
    	Packet packet = new Packet();
    	TimeUtils timer = new TimeUtils();
    	FolderUtils folder = new FolderUtils(timer);
    	Log logger = new Log(timer, folder);
    	SynchronizedBuffer buffer = new SynchronizedBuffer(logger, "test");
    	ExperimentConf conf = new ExperimentConf(logger);
    	
        PacketDispatcher dispatcher = new PacketDispatcher(logger, conf, timer);
        dispatcher.addProtocolBuffer(protocol_number, buffer);
        dispatcher.start();
        try {
        	Thread.sleep(2000);
        } catch(InterruptedException e) {
        	e.printStackTrace();
        }
        
        /* Check that the telemetry is retrieved */
        System.out.println("Buffer bytes: " + buffer.bytesAvailable());
        dispatcher.requestHK(protocol_number);
        try {
        	Thread.sleep(500);
        } catch(InterruptedException e) {
        	e.printStackTrace();
        }
        System.out.println("Buffer bytes after telemetry request: " + buffer.bytesAvailable());
        byte[] header_stream = new byte[Constants.header_size];
        buffer.read(header_stream);
        packet.resetValues();
        packet.setHeader(header_stream);
        byte[] data = new byte[packet.length];
        buffer.read(data);
        packet.setData(data);
        byte[] checksum_stream = new byte[Short.SIZE / 8];
        buffer.read(checksum_stream);
        packet.setChecksum(checksum_stream);
        
        byte[] telemetry = packet.getData();
        String s = "[" + (int)(System.currentTimeMillis()) + "]Telemetry Packet -> ";
		s += "Boot count: " + (telemetry[0] & 0xFF) + ", Actual RSSI: " + rssi_raw_dbm(telemetry[1] & 0xFF); 
		s += ", Last RSSI: " + rssi_raw_dbm(telemetry[2] & 0xFF);
		s += ", Last LQI: " + lqi_status(telemetry[3] & 0xFF) + ", TX Power: " + (telemetry[4] & 0xFF);
		s += ", PHY TX packets: " + (((telemetry[8] << 24) | (telemetry[7] << 16) | (telemetry[6] << 8) | (telemetry[5] & 0xFF)) & 0xFFFFFFFF);
		s += ", PHY RX packets: " + (((telemetry[12] << 24) | (telemetry[11] << 16) | (telemetry[10] << 8) | (telemetry[9] & 0xFF)) & 0xFFFFFFFF);
		s += ", LL TX packets: " + (((telemetry[16] << 24) | (telemetry[15] << 16) | (telemetry[14] << 8) | (telemetry[13] & 0xFF)) & 0xFFFFFFFF);
		s += ", LL RX packets: " + (((telemetry[20] << 24) | (telemetry[19] << 16) | (telemetry[18] << 8) | (telemetry[17] & 0xFF)) & 0xFFFFFFFF);
		s += ", PHY TX failed packets: " + (((telemetry[22] << 8) | (telemetry[21] & 0xFF)) & 0xFFFF);
		s += ", PHY RX errors packets: " + (((telemetry[24] << 8) | (telemetry[23] & 0xFF)) & 0xFFFF);
		s += ", External Temperature: " + fromU16ToFloat(((telemetry[25] << 8) | (telemetry[26] & 0xFF)) & 0xFFFF);
		s += ", Internal Temperature: " + fromU16ToFloat(((telemetry[27] << 8) | (telemetry[28] & 0xFF)) & 0xFFFF);
		byte[] freq_bytes = new byte[4];
		freq_bytes[3] = telemetry[29];
		freq_bytes[2] = telemetry[30];
		freq_bytes[1] = telemetry[31];
		freq_bytes[0] = telemetry[32];
		float frequency = ByteBuffer.wrap(freq_bytes).order(ByteOrder.BIG_ENDIAN).getFloat();
		s += ", Frequency: " + frequency;
		s += ", RX Queue: " + (telemetry[33] & 0xFF) + ", TX Queue: " + (telemetry[34] & 0xFF);
		s += ", Free Stack: " + (((telemetry[36] << 8) | (telemetry[35] & 0xFF)) & 0xFFFF) + " " + (((telemetry[38] << 8) | (telemetry[37] & 0xFF)) & 0xFFFF) + " ";
		s += (((telemetry[40] << 8) | (telemetry[39] & 0xFF)) & 0xFFFF) + " " + (((telemetry[42] << 8) | (telemetry[41] & 0xFF)) & 0xFFFF);
		s += ", User Stack: " + (((telemetry[44] << 8) | (telemetry[43] & 0xFF)) & 0xFFFF) + " " + (((telemetry[46] << 8) | (telemetry[45] & 0xFF)) & 0xFFFF) + " ";
		s += (((telemetry[48] << 8) | (telemetry[47] & 0xFF)) & 0xFFFF) + " " + (((telemetry[50] << 8) | (telemetry[49] & 0xFF)) & 0xFFFF);
        System.out.println(s);
		
        /* Check packet transmission */
        System.out.println("Transmitting packet");
        dispatcher.transmitPacket(packet); 
        try {
        	Thread.sleep(500);
        } catch(InterruptedException e) {
        	e.printStackTrace();
        }
        dispatcher.requestHK(protocol_number);
        try {
        	Thread.sleep(500);
        } catch(InterruptedException e) {
        	e.printStackTrace();
        }
        System.out.println("Buffer bytes after telemetry request: " + buffer.bytesAvailable());
        header_stream = new byte[Constants.header_size];
        buffer.read(header_stream);
        packet.resetValues();
        packet.setHeader(header_stream);
        data = new byte[packet.length];
        buffer.read(data);
        packet.setData(data);
        checksum_stream = new byte[Short.SIZE / 8];
        buffer.read(checksum_stream);
        packet.setChecksum(checksum_stream);
        
        telemetry = packet.getData();
        s = "[" + (int)(System.currentTimeMillis()) + "]Telemetry Packet -> ";
		s += "Boot count: " + (telemetry[0] & 0xFF) + ", Actual RSSI: " + rssi_raw_dbm(telemetry[1] & 0xFF); 
		s += ", Last RSSI: " + rssi_raw_dbm(telemetry[2] & 0xFF);
		s += ", Last LQI: " + lqi_status(telemetry[3] & 0xFF) + ", TX Power: " + (telemetry[4] & 0xFF);
		s += ", PHY TX packets: " + (((telemetry[8] << 24) | (telemetry[7] << 16) | (telemetry[6] << 8) | (telemetry[5] & 0xFF)) & 0xFFFFFFFF);
		s += ", PHY RX packets: " + (((telemetry[12] << 24) | (telemetry[11] << 16) | (telemetry[10] << 8) | (telemetry[9] & 0xFF)) & 0xFFFFFFFF);
		s += ", LL TX packets: " + (((telemetry[16] << 24) | (telemetry[15] << 16) | (telemetry[14] << 8) | (telemetry[13] & 0xFF)) & 0xFFFFFFFF);
		s += ", LL RX packets: " + (((telemetry[20] << 24) | (telemetry[19] << 16) | (telemetry[18] << 8) | (telemetry[17] & 0xFF)) & 0xFFFFFFFF);
		s += ", PHY TX failed packets: " + (((telemetry[22] << 8) | (telemetry[21] & 0xFF)) & 0xFFFF);
		s += ", PHY RX errors packets: " + (((telemetry[24] << 8) | (telemetry[23] & 0xFF)) & 0xFFFF);
		s += ", External Temperature: " + fromU16ToFloat(((telemetry[25] << 8) | (telemetry[26] & 0xFF)) & 0xFFFF);
		s += ", Internal Temperature: " + fromU16ToFloat(((telemetry[27] << 8) | (telemetry[28] & 0xFF)) & 0xFFFF);
		freq_bytes = new byte[4];
		freq_bytes[3] = telemetry[29];
		freq_bytes[2] = telemetry[30];
		freq_bytes[1] = telemetry[31];
		freq_bytes[0] = telemetry[32];
		frequency = ByteBuffer.wrap(freq_bytes).order(ByteOrder.BIG_ENDIAN).getFloat();
		s += ", Frequency: " + frequency;
		s += ", RX Queue: " + (telemetry[33] & 0xFF) + ", TX Queue: " + (telemetry[34] & 0xFF);
		s += ", Free Stack: " + (((telemetry[36] << 8) | (telemetry[35] & 0xFF)) & 0xFFFF) + " " + (((telemetry[38] << 8) | (telemetry[37] & 0xFF)) & 0xFFFF) + " ";
		s += (((telemetry[40] << 8) | (telemetry[39] & 0xFF)) & 0xFFFF) + " " + (((telemetry[42] << 8) | (telemetry[41] & 0xFF)) & 0xFFFF);
		s += ", User Stack: " + (((telemetry[44] << 8) | (telemetry[43] & 0xFF)) & 0xFFFF) + " " + (((telemetry[46] << 8) | (telemetry[45] & 0xFF)) & 0xFFFF) + " ";
		s += (((telemetry[48] << 8) | (telemetry[47] & 0xFF)) & 0xFFFF) + " " + (((telemetry[50] << 8) | (telemetry[49] & 0xFF)) & 0xFFFF);
        System.out.println(s);
        
        /* Check packet reception */
        //TODO: verify the asynchrone reception
        
        dispatcher.controlledStop();
        try {
        	Thread.sleep(1000);
        } catch(InterruptedException e) {
        	e.printStackTrace();
        }
        logger.close();
        
    }
}
