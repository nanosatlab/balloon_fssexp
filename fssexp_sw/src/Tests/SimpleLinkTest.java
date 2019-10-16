package Tests;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import Common.Constants;
import Common.FolderUtils;
import Common.TimeUtils;
import Configuration.ExperimentConf;
import IPCStack.SimpleLinkProtocol;
import IPCStack.UartInterface;
import Lockers.UartBuffer;
import Storage.Log;


public class SimpleLinkTest {

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
		
		final int HK_MODE = 0;
		final int TX_MODE = 1;
		final int RX_MODE = 2;
		final int CONF_MODE = 3;
		int mode = HK_MODE;
		
		String file_name = "";
		boolean file_output = false;
		boolean mode_arg = false;
		boolean packets_arg = false;
		int max_packets = -1;
		boolean always = true;
		boolean redundancy_arg = false;
		int redundancy = 0;
		boolean freq_arg = false;
		float freq = 436.250e6f;
		boolean port_arg = false;
		String port_name = "";
		
		if(args.length > 0) {
			for(String arg : args) {
				switch(arg) {
					case "-o":
						file_output = true;
						break;
					case "-m":
						mode_arg = true;
						break;
					case "-p":
						packets_arg = true;
						break;
					case "-r":
						redundancy_arg = true;
						break;	
					case "-f":
						freq_arg = true;
						break;
					case "-port":
						port_arg = true;
						break;
					default:
						if(file_output) { 
							file_output = false;
							file_name = arg;
						}
						if(mode_arg) {
							mode_arg = false;
							if(arg.equals("RX")) mode = RX_MODE;
							else if(arg.equals("TX")) mode = TX_MODE;
						}	
						if(packets_arg) {
							packets_arg = false;
							max_packets = Integer.parseInt(arg);
							if(max_packets == -1) {
								always = true;
							} else {
								always = false;
							}
						}
						if(redundancy_arg) {
							redundancy_arg = false;
							redundancy = Integer.parseInt(arg);
						}
						if(freq_arg) {
							freq_arg = false;
							mode = CONF_MODE;
							freq = Float.parseFloat(arg);
						}
						if(port_arg) {
							port_name = arg;
							System.out.println("PORT " + port_name);
						}
				}
			}
		}
		
		OutputStreamWriter writer = null;
		if(file_name.length() > 0) {
			File file = new File("./" + file_name);
			file.createNewFile();
			writer = new OutputStreamWriter(new FileOutputStream(file));
		}
		
		TimeUtils time = new TimeUtils();
		FolderUtils folder = new FolderUtils(time);
		Log logger = new Log(time, folder);
		ExperimentConf conf = new ExperimentConf(logger);
		
		/* Set parameters of Serial configuration */
		conf.port_desc = "/dev/ttyS1";
		
		/* Set parameters of RF ISL */
		conf.rf_isl_redundancy = redundancy;
		
		System.out.println("Redundancy: " + conf.rf_isl_redundancy);
		SimpleLinkProtocol slp = new SimpleLinkProtocol(logger, conf, time);
		slp.setConfiguration();
		if(slp.open() == true) {
		
	
			switch(mode) {
				case HK_MODE:
					while(true) {
						/* Request Telemetry */
						System.out.println("Getting telemetry");
						byte[] telemetry = (byte[])slp.accessToIPCStack(Constants.SLP_ACCESS_TELEMETRY, null);
						System.out.println("Size " + telemetry.length);
						if(telemetry.length > 0) {
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
							if(writer != null) writer.write(s + "\n");
							System.out.println("---------------------------------------------------------------------");
							if(writer != null) writer.write("---------------------------------------------------------------------"  + "\n");
							if(writer != null) writer.flush();
						}
						Thread.sleep(1000);
					}
	
				case TX_MODE:
					int counter = 0; 
					//while(true) {
					while((counter < max_packets) || always == true) {
						String content = "RICARD ETS LA CANYA! " + counter; //+ " A VERE SI AMB AIXò POTS TENIR SUFICIENT DADES COM PER TENIR METRIQUES INTERESANTS. ";
						//content += "HAIG D'ACONSEGUIR TENIR UNS 200 CARACTERS, PERO NO PORTO TANS. MENTRES, VAIG FENT TEMPS ... ";
						//content += "NO LLEGEIXIS TAN I TREBALLA!";
						System.out.println("Sending Packet with " + content.length() + " Bytes of content: " + content);
						if(writer != null) writer.write("Sending Packet with " + content.length() + " Bytes of content: " + content + "\n");
						if((boolean)slp.accessToIPCStack(Constants.SLP_ACCESS_SEND, content.getBytes())) {
							counter ++;
							byte[] telemetry = (byte[]) slp.accessToIPCStack(Constants.SLP_ACCESS_TELEMETRY, null);
							if(telemetry.length > 0) {
								String s = "[" + System.currentTimeMillis() + "]Transmission Status -> ";
								s += "Boot count: " + (telemetry[0] & 0xFF);
								s += ", TX Power: " + (telemetry[4] & 0xFF);
								s += ", PHY TX packets: " + (((telemetry[8] << 24) | (telemetry[7] << 16) | (telemetry[6] << 8) | (telemetry[5] & 0xFF)) & 0xFFFFFFFF);
								s += ", LL TX packets: " + (((telemetry[16] << 24) | (telemetry[15] << 16) | (telemetry[14] << 8) | (telemetry[13] & 0xFF)) & 0xFFFFFFFF);
								s += ", TX Queue: " + (telemetry[34] & 0xFF);
								s += ", PHY TX failed packets: " + (((telemetry[22] << 8) | (telemetry[21] & 0xFF)) & 0xFFFF);
								System.out.println(s);
								
								if(writer != null) writer.write(s + "\n");
								System.out.println("---------------------------------------------------------------------");
								if(writer != null) writer.write("---------------------------------------------------------------------" + "\n");
								if(writer != null) writer.flush();
							} else {
								System.out.println("[ERROR] Something got wrong. I have correctly sent a command, but I have not received HK...");
							}
							
						} else {
							System.out.println("The RF ISL has repplied indicating that the packet has not been sent");
							System.out.println("---------------------------------------------------------------------");
						}
						Thread.sleep(3000);
					}
				
				case RX_MODE:
					
					System.out.println("Receiving packets...");
					byte[] data;
					while(true) {
						data = (byte[]) slp.accessToIPCStack(Constants.SLP_ACCESS_RECEIVE, null);
						if(data.length > 0) {
							Thread.sleep(10);
							byte[] telemetry = (byte[]) slp.accessToIPCStack(Constants.SLP_ACCESS_TELEMETRY, null);
							String s = "[" + System.currentTimeMillis() + "]Received Packet -> ";
							s += "Boot count: " + (telemetry[0] & 0xFF);
							s += ", Actual RSSI: " + rssi_raw_dbm(telemetry[1] & 0xFF); 
							s += ", Last RSSI: " + rssi_raw_dbm(telemetry[2] & 0xFF); 
							s += ", SNR: " + snr(rssi_raw_dbm(telemetry[2] & 0xFF), rssi_raw_dbm(telemetry[1] & 0xFF));
							s += ", Last LQI: " + lqi_status(telemetry[3] & 0xFF);
							s += ", PHY RX packets: " + (((telemetry[12] << 24) | (telemetry[11] << 16) | (telemetry[10] << 8) | (telemetry[9] & 0xFF)) & 0xFFFFFFFF);
							s += ", LL RX packets: " + (((telemetry[20] << 24) | (telemetry[19] << 16) | (telemetry[18] << 8) | (telemetry[17] & 0xFF)) & 0xFFFFFFFF);
							s += ", PHY RX errors packets: " + (((telemetry[24] << 8) | (telemetry[23] & 0xFF))& 0xFFFF);
							s += ", RX Queue: " + (telemetry[33] & 0xFF);
							System.out.println(s);
							if(writer != null) writer.write(s + "\n");
							System.out.println("Received " + data.length + " Bytes Content: " + new String(data));
							if(writer != null) writer.write("Received " + data.length + " Bytes Content: " + new String(data) + "\n");
							System.out.println("---------------------------------------------------------------------");
							if(writer != null) writer.write("---------------------------------------------------------------------" + "\n");
							if(writer != null) writer.flush();
						} 
						Thread.sleep(500);
					}
				case CONF_MODE:
					System.out.println("Sending configuration...");
					byte[] mydata = ByteBuffer.allocate(4).putFloat(freq).array();
					System.out.println("Bytes: " + mydata.length);
					if((boolean)slp.accessToIPCStack(Constants.SLP_ACCESS_CONF, mydata)) {
						System.out.println("Configuration correctly sent!");
					} else {
						System.out.println("ERROR during sendind a configuration");
					}
			}
				
			slp.close();
		} else {
			System.out.println("[ERROR] Impossible to start the SimpleLink protocol");
		}
		
		if(writer != null) {
			writer.close();
		}
		
	}
	
}
