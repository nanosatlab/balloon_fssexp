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
import Configuration.ExperimentConf;
import IPCStack.SimpleLinkProtocol;
import space.golbriak.io.Serial;

public class FCTRFISL {

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
		
		String file_name = "FCT_RFISL_" + System.currentTimeMillis() + ".output";
		int hk_iterations = 30;
		int hk_not_rx = 0;
		int hk_rx_bad = 0;
		
		int boot_count;
		double actual_rssi;
		int tx_power;
		int tx_packets;
		int rx_packets;
		double ext_temp;
		double int_temp;
		float freq;
		
		OutputStreamWriter writer = null;
		if(file_name.length() > 0) {
			File file = new File("./" + file_name);
			file.createNewFile();
			writer = new OutputStreamWriter(new FileOutputStream(file));
		}
		
		Log logger = null;
		
		try {
			logger = new Log();
		} catch (Exception e) {
			// TODO: handle exception
		}
		
		ExperimentConf conf = new ExperimentConf(logger);
		SimpleLinkProtocol slp = new SimpleLinkProtocol(logger, conf);
		slp.setConfiguration();
		slp.open();
		
	
		/* The check test is retrieving 60 HK messages correctly */
		String s = "";
		for(int i = 0; i < hk_iterations; i ++) {
			/* Request Telemetry */
			s += "=======================================================================\n";
			s += "[" + System.currentTimeMillis() + "] Telemetry request " + (i+1) + "th\n";
			byte[] telemetry = (byte[])slp.accessToIPCStack(Constants.SLP_ACCESS_TELEMETRY, null);
			if(telemetry.length > 0) {
				
				/* Retrieve the parameters */
				boot_count = (telemetry[0] & 0xFF);
				actual_rssi = rssi_raw_dbm(telemetry[1] & 0xFF);
				tx_power = (telemetry[4] & 0xFF);
				tx_packets = (((telemetry[8] << 24) | (telemetry[7] << 16) | (telemetry[6] << 8) | (telemetry[5] & 0xFF)) & 0xFFFFFFFF);
				rx_packets = (((telemetry[12] << 24) | (telemetry[11] << 16) | (telemetry[10] << 8) | (telemetry[9] & 0xFF)) & 0xFFFFFFFF);
				ext_temp = fromU16ToFloat(((telemetry[25] << 8) | (telemetry[26] & 0xFF)) & 0xFFFF);
				int_temp = fromU16ToFloat(((telemetry[27] << 8) | (telemetry[28] & 0xFF)) & 0xFFFF);
				byte[] freq_bytes = new byte[4];
				freq_bytes[3] = telemetry[29];
				freq_bytes[2] = telemetry[30];
				freq_bytes[1] = telemetry[31];
				freq_bytes[0] = telemetry[32];
				freq = ByteBuffer.wrap(freq_bytes).order(ByteOrder.BIG_ENDIAN).getFloat();
				
				/* Check all the parameters */
				if(boot_count < 0) {
					hk_rx_bad ++;
					s += "Bad Telemetry packet: Boot count negative\n";
				}
				if(actual_rssi < -116 || actual_rssi > -106) {
					hk_rx_bad ++;
					s += "Bad Telemetry packet: Actual RSSI out of boundaries\n";
				}
				if(tx_power != 75) {
					hk_rx_bad ++;
					s += "Bad Telemetry packet: TX power not 75 dBm\n";
				}
				if(tx_packets != 0) {
					hk_rx_bad ++;
					s += "Bad Telemetry packet: TX packets not zero\n";
				}
				if(rx_packets != 0) {
					hk_rx_bad ++;
					s += "Bad Telemetry packet: RX packets not zero\n";
				}
				if(ext_temp > 27 || ext_temp < 23) {
					hk_rx_bad ++;
					s += "Bad Telemetry packet: External temperature out of boundaries\n";
				}
				if(int_temp > 31 || int_temp < 21) {
					hk_rx_bad ++;
					s += "Bad Telemetry packet: Internal temperature out of boundaries\n";
				}
				if(Math.abs(freq - 437.35e6) / 10000000 >= 1e-5) {
					hk_rx_bad ++;
					s += "Bad Telemetry packet: Frequency is not 437.35 MHz\n";
				}
				
				/* Print */
				s += "--------------------------------------------------------------------\n";
				s += "| Parameter      | Expected   | Marging | Measured          |\n";
				s += "--------------------------------------------------------------------\n";
				s += "| Boot count     | > 0        |         | " + boot_count + " |\n";
				s += "| Actual RSSI    | -111 dBm   | +- 5    | " + actual_rssi + " dBm |\n"; 
				s += "| TX Power       | 75 dBm     | 0       | " + tx_power + " dBm |\n";
				s += "| PHY TX packets | 0          | 0       | " + tx_packets + " |\n";
				s += "| PHY RX packets | 0          | 0       | " + rx_packets +" |\n";
				s += "| External Temp. | 25 ºC      | +- 2    | " + ext_temp + " ºC |\n";
				s += "| Internal Temp. | 26 ºC      | +- 5    | " + int_temp + " ºC |\n";
				s += "| Frequency      | 437.35 MHz | 0       | " + freq / 1000000 + " MHz |\n";
				s += "--------------------------------------------------------------------\n";	
				
			} else {
				s += "Telemetry not received\n";
				System.out.println(s);
				hk_not_rx ++;
			}
			s += "=======================================================================\n";
			System.out.println(s);
			writer.write(s);
			writer.flush();
			s = "";
			Thread.sleep(1000);
		}
			
		/* Conclusion of the test */
		s += "CONCLUSION:\n";
		s += "    - HK requested: " + hk_iterations + "\n";
		s += "    - HK received: " + (hk_iterations - hk_not_rx) + "\n";
		s += "    - HK received with correct parameters: " + (hk_iterations - hk_not_rx - hk_rx_bad) + "\n";
		s += "    - HK received with bad parameters: " + hk_rx_bad + "\n";
		if(hk_not_rx == 0 && hk_rx_bad == 0) s += "\n TEST PASSED!\n";
		else s += "\n TEST ERROR!\n";
		
		System.out.println(s);
		writer.write(s);
		writer.flush();
		slp.close();
		if(writer != null) {
			writer.close();
		}
		
	}
	
}
