package Tools;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import Common.Constants;
import Common.Log;
import Common.SynchronizedBuffer;
import Common.TimeUtils;
import Configuration.ExperimentConf;
import IPCStack.SimpleLinkProtocol;
import IPCStack.UartInterface;
import space.golbriak.io.Serial;

public class ThermalTest {

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
	
	
	public final static void main(String args[]) throws InterruptedException, IOException {
		
		Serial.FSSport = Constants.uart_port;
    	Serial.FSSbaudrate = Constants.uart_bps;
		
		String file_name = "ThermalTest_" + System.currentTimeMillis();
		boolean file_output = false;
		boolean mode_arg = false;
		boolean packets_arg = false;
		int max_packets = -1;
		boolean always = true;
		double last_transmission = System.currentTimeMillis();
		String output = "";
		int counter = 0; 
		String static_content = "RICARD ETS LA CANYA! A VERE SI AMB AIXò POTS TENIR SUFICIENT DADES COM PER TENIR METRIQUES INTERESANTS. ";
		static_content += "HAIG D'ACONSEGUIR TENIR UNS 200 CARACTERS, PERO NO PORTO TANS. MENTRES, VAIG FENT TEMPS ... ";
		String content = "";
		boolean redundancy_arg = false;
		int redundancy = 0;
		boolean time_arg = false;
		int time_set = 15;
		boolean cadency_arg = false;
		double cadency = 3.0;
		
		OutputStreamWriter writer = null;
		if(file_name.length() > 0) {
			File file = new File("./" + file_name);
			file.createNewFile();
			writer = new OutputStreamWriter(new FileOutputStream(file));
		}
		
		if(args.length > 0) {
			for(String arg : args) {
				switch(arg) {
					case "-r":
						redundancy_arg = true;
						break;	
					case "-t":
						time_arg = true;
						break;
					case "-c":
						cadency_arg = true;
						break;
					default:
						if(redundancy_arg) {
							redundancy_arg = false;
							redundancy = Integer.parseInt(arg);
						}
						if(time_arg) {
							time_set = Integer.parseInt(arg);
							time_arg = false;
							System.out.println("Execution during " + time_set + " minutes");
						}
						if(cadency_arg) {
							cadency = Double.parseDouble(arg);
							cadency_arg = false;
						}
				}
			}
		}
		
		double duration = time_set * 60 * 1000;
		double end_time = System.currentTimeMillis() + duration;
		
		
		
		TimeUtils time = new TimeUtils();
		Log logger = new Log(time);
		ExperimentConf conf = new ExperimentConf(logger);
		conf.rf_isl_redundancy = redundancy;
		SynchronizedBuffer rx_buffer = new SynchronizedBuffer(logger);
		SynchronizedBuffer tx_buffer = new SynchronizedBuffer(logger);
		UartInterface uart_interface = new UartInterface(logger, tx_buffer, rx_buffer, time);
		SimpleLinkProtocol slp = new SimpleLinkProtocol(logger, conf, time, tx_buffer, rx_buffer);
		slp.setConfiguration();
		//slp.open();
		uart_interface.start();
		
		System.out.println("Redundancy: " + conf.rf_isl_redundancy);
		System.out.println("Execution duration: " + duration + " milliseconds");
		System.out.println("Cadency: one TX per " + cadency + " seconds");
		
		
	
		while(System.currentTimeMillis() < end_time) {
					
			/* Request Telemetry */
			
			output += "=========================================================================\n";
			output += "Getting telemetry\n";
			byte[] telemetry = (byte[])slp.accessToIPCStack(Constants.SLP_ACCESS_TELEMETRY, null);
			output += "Telemetry Packet Size " + telemetry.length + " Bytes\n";
			if(telemetry.length > 0) {
				output += "[" + (System.currentTimeMillis()) + "]Telemetry Packet -> ";
				output += "Boot count: " + (telemetry[0] & 0xFF) + ", Actual RSSI: " + rssi_raw_dbm(telemetry[1] & 0xFF); 
				output += ", Last RSSI: " + rssi_raw_dbm(telemetry[2] & 0xFF);
				output += ", Last LQI: " + lqi_status(telemetry[3] & 0xFF) + ", TX Power: " + (telemetry[4] & 0xFF);
				output += ", PHY TX packets: " + (((telemetry[8] << 24) | (telemetry[7] << 16) | (telemetry[6] << 8) | (telemetry[5] & 0xFF)) & 0xFFFFFFFF);
				output += ", PHY RX packets: " + (((telemetry[12] << 24) | (telemetry[11] << 16) | (telemetry[10] << 8) | (telemetry[9] & 0xFF)) & 0xFFFFFFFF);
				output += ", LL TX packets: " + (((telemetry[16] << 24) | (telemetry[15] << 16) | (telemetry[14] << 8) | (telemetry[13] & 0xFF)) & 0xFFFFFFFF);
				output += ", LL RX packets: " + (((telemetry[20] << 24) | (telemetry[19] << 16) | (telemetry[18] << 8) | (telemetry[17] & 0xFF)) & 0xFFFFFFFF);
				output += ", PHY TX failed packets: " + (((telemetry[22] << 8) | (telemetry[21] & 0xFF)) & 0xFFFF);
				output += ", PHY RX errors packets: " + (((telemetry[24] << 8) | (telemetry[23] & 0xFF)) & 0xFFFF);
				output += ", External Temperature: " + fromU16ToFloat(((telemetry[25] << 8) | (telemetry[26] & 0xFF)) & 0xFFFF);
				output += ", Internal Temperature: " + fromU16ToFloat(((telemetry[27] << 8) | (telemetry[28] & 0xFF)) & 0xFFFF);
				byte[] freq_bytes = new byte[4];
				freq_bytes[3] = telemetry[29];
				freq_bytes[2] = telemetry[30];
				freq_bytes[1] = telemetry[31];
				freq_bytes[0] = telemetry[32];
				float freq = ByteBuffer.wrap(freq_bytes).order(ByteOrder.BIG_ENDIAN).getFloat();
				output += ", Frequency: " + freq;
				output += ", RX Queue: " + (telemetry[33] & 0xFF) + ", TX Queue: " + (telemetry[34] & 0xFF);
				output += ", Free Stack: " + (((telemetry[36] << 8) | (telemetry[35] & 0xFF)) & 0xFFFF) + " " + (((telemetry[38] << 8) | (telemetry[37] & 0xFF)) & 0xFFFF) + " ";
				output += (((telemetry[40] << 8) | (telemetry[39] & 0xFF)) & 0xFFFF) + " " + (((telemetry[42] << 8) | (telemetry[41] & 0xFF)) & 0xFFFF);
				output += ", User Stack: " + (((telemetry[44] << 8) | (telemetry[43] & 0xFF)) & 0xFFFF) + " " + (((telemetry[46] << 8) | (telemetry[45] & 0xFF)) & 0xFFFF) + " ";
				output += (((telemetry[48] << 8) | (telemetry[47] & 0xFF)) & 0xFFFF) + " " + (((telemetry[50] << 8) | (telemetry[49] & 0xFF)) & 0xFFFF) + "\n";
				
			} else {
				output += "ERROR: no telemetry reception\n";
			}
			
			output += "=========================================================================\n"; 
			System.out.println(output);
			writer.write(output);
			writer.flush();
			output = "";
			
			if(System.currentTimeMillis() >= last_transmission) {

				/* Perform transmission */
				output += "###############################################################\n"; 
				content = static_content + counter;
				output += "Sending Packet with " + content.length() + " Bytes of content: " + content + "\n";
				if(writer != null) writer.write("Sending Packet with " + content.length() + " Bytes of content: " + content + "\n");
				if((boolean)slp.accessToIPCStack(Constants.SLP_ACCESS_SEND, content.getBytes())) {
					counter ++;
					telemetry = (byte[]) slp.accessToIPCStack(Constants.SLP_ACCESS_TELEMETRY, null);
					if(telemetry.length > 0) {
						output += "[" + System.currentTimeMillis() + "]Transmission Status -> ";
						output += "Boot count: " + (telemetry[0] & 0xFF);
						output += ", TX Power: " + (telemetry[4] & 0xFF);
						output += ", PHY TX packets: " + (((telemetry[8] << 24) | (telemetry[7] << 16) | (telemetry[6] << 8) | (telemetry[5] & 0xFF)) & 0xFFFFFFFF);
						output += ", LL TX packets: " + (((telemetry[16] << 24) | (telemetry[15] << 16) | (telemetry[14] << 8) | (telemetry[13] & 0xFF)) & 0xFFFFFFFF);
						output += ", TX Queue: " + (telemetry[34] & 0xFF);
						output += ", PHY TX failed packets: " + (((telemetry[22] << 8) | (telemetry[21] & 0xFF)) & 0xFFFF) + "\n";
						
						
					} else {
						output += "[ERROR] Something got wrong. I have correctly sent a command, but I have not received HK...\n";
					}
					
				} else {
					output += "The RF ISL has repplied indicating that the packet has not been sent\n";
				}
				output += "###############################################################\n"; 
				System.out.println(output);
				writer.write(output);
				writer.flush();
				output = "";
				last_transmission = System.currentTimeMillis() + (cadency * 1000); /* One transmission at each 3 seconds */
			}
			
			Thread.sleep(500);
		}
			
		//slp.close();
		uart_interface.close();
		if(writer != null) {
			writer.close();
		}
		
	}
	
}
