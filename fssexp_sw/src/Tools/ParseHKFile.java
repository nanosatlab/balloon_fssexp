package Tools;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import Common.Constants;

public class ParseHKFile {
	
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
	
	public static double snr(double last_rssi, double actual_rssi)
	{
	    return last_rssi - actual_rssi;
	}
	
	public static double lqi_status(int lqi)
	{
	    return (1.0 - lqi / 127.0) * 100.0;
	}
	
    
    public static void main(String args[]) throws IOException {
        
        boolean input_arg = false;
        String input = "./sat/fss/to_download/housekeeping.data";
        
        boolean output_arg = false;
        String output = "";
        
        if(args.length > 0) {
			for(String arg : args) {
				switch(arg) {
					case "-i":
						input_arg = true;
						break;	
					case "-o":
						output_arg = true;
						break;	
					default:
						if(input_arg) {
							input_arg = false;
							input = arg;
						}
						if(output_arg) {
							output_arg = false;
							output = arg;
						}
				}
			}
		}
        
        OutputStreamWriter writer = null;
		if(output.length() > 0) {
			File file = new File("./" + output);
			file.createNewFile();
			writer = new OutputStreamWriter(new FileOutputStream(file));
		}
        
        String s = "";
        s += "Welcome to the Parsing Tool for the Housekeeping File of the FSS Experiment\n\n";
        
        
        int header_size = Constants.hk_header_size;
        int item_size = Constants.hk_item_size;
        
        String result_file = input;
        File hk_file = new File(result_file);
        FileInputStream file_stream = new FileInputStream(hk_file);
        
        s += "File Size: " + hk_file.length() + "\n\n";
        
        /* Header */
        ByteBuffer header = ByteBuffer.allocate(header_size);
        byte[] header_bytes = new byte[header_size];
        file_stream.read(header_bytes);
        header.put(header_bytes);
        header.flip();
        
        int sw_version = (header.get() & 0xFF);
        byte first = header.get();
        int sat_id = ((first & 0x80) >> 7);
        int exp_number = first & 0x7F;
        long start_time = header.getLong();
        
        s += "Header HK:\n";
        s += "    - Satellite ID: " + sat_id + "\n";
        s += "    - Software version: " + sw_version + "\n";
        s += "    - Experiment number: " + exp_number + "\n";
        s += "    - Initial time: " + start_time + "\n\n";
        
        
        ByteBuffer item = ByteBuffer.allocate(item_size);
        byte[] item_bytes = new byte[item_size];
        int manager;
        int command_time;
        int fss;
        int rf_isl;
        int conf_version;
        
        s += "Size " + file_stream.available() + " Bytes\n";
        
        while(file_stream.available() >= item_size) {
            
        	file_stream.read(item_bytes);
            item.put(item_bytes);
            item.flip();
            s += "Item at " + (item.getInt() + start_time) + "\n";
            conf_version = item.get();
            s += "- Experiment configuration " + ((conf_version & 0xFF)) + "\n";
            manager = item.get();
            s += "- Manager:\n";
            s += "    - Experiment status " + ((manager & 0xE0) >> 5) + "\n";
            s += "    - Data Generator Polling " + ((manager & 0x02)) + "\n";
            s += "    - FSS Protocol Polling " + ((manager & 0x01)) + "\n";
            s += "    - Last SC Command Received " + ((manager & 0x18) >> 3) + "\n";
            command_time = item.getInt();
            if(command_time == 0)
                s += "    - Last SC Command at " + command_time + "\n";
            else
                s += "    - Last SC Command at " + (command_time + start_time) + "\n";
            s += "    - Total of received SC Commands " + item.get() + "\n";
            
            s += "- DataGenerator:" + "\n";
            s += "    - Number of generated packets " + item.getInt() + "\n";
            s += "    - Status " + item.get() + "\n";
            
            s += "- FSSDataBuffer:" + "\n";
            s += "    - Buffer Size " + item.getShort() + "\n";
            s += "    - Packet Drops " + item.getShort() + "\n";
            
            s += "- FSSProtocol:" + "\n";
            fss = item.get();
            s += "    - FSS status " + (fss & 0x03) + "\n";
            s += "    - FSS Role " + ((fss & 0x0C) >> 2) + "\n";
            s += "    - Number of Transmitted packets " + item.getShort() + "\n";
            s += "    - Number of Received packets " + item.getShort() + "\n";
            s += "    - Number of Received Erroneous packets " + item.getShort() + "\n";
            rf_isl = item.get();
            s += "- RF ISL Module:" + "\n";
            s += "    - Comms established: " + (rf_isl & 0xFF) + " (1 = Error; 0 = Good)" + "\n"; 
            s += "    - Boot count: " + (item.get() & 0xFF) + "\n";
            double actual_rssi = rssi_raw_dbm(item.get() & 0xFF);
            s += "    - Actual RSSI: " + actual_rssi + "\n";
            double last_rssi = rssi_raw_dbm(item.get() & 0xFF);
            s += "    - Last RSSI: " + last_rssi + "\n";
            s += "    - SNR: " + snr(last_rssi, actual_rssi) + "\n";
            s += "    - Last LQI: " + lqi_status(item.get() & 0xFF) + "\n";
            s += "    - TX Power: " + (item.get() & 0xFF) + "\n";
            byte[] my_bytes = new byte[4];
            my_bytes[0] = item.get();
            my_bytes[1] = item.get();
            my_bytes[2] = item.get();
            my_bytes[3] = item.get();
            s += "    - PHY TX packets: " + (((my_bytes[3] << 24) | (my_bytes[2] << 16) | (my_bytes[1] << 8) | (my_bytes[0] & 0xFF)) & 0xFFFFFFFF) + "\n";
            my_bytes[0] = item.get();
            my_bytes[1] = item.get();
            my_bytes[2] = item.get();
            my_bytes[3] = item.get();
            s += "    - PHY RX packets: " + (((my_bytes[3] << 24) | (my_bytes[2] << 16) | (my_bytes[1] << 8) | (my_bytes[0] & 0xFF)) & 0xFFFFFFFF) + "\n";
            my_bytes[0] = item.get();
            my_bytes[1] = item.get();
            my_bytes[2] = item.get();
            my_bytes[3] = item.get();
            s += "    - LL TX packets: " + (((my_bytes[3] << 24) | (my_bytes[2] << 16) | (my_bytes[1] << 8) | (my_bytes[0] & 0xFF)) & 0xFFFFFFFF) + "\n";
            my_bytes[0] = item.get();
            my_bytes[1] = item.get();
            my_bytes[2] = item.get();
            my_bytes[3] = item.get();
            s += "    - LL RX packets: " + (((my_bytes[3] << 24) | (my_bytes[2] << 16) | (my_bytes[1] << 8) | (my_bytes[0] & 0xFF)) & 0xFFFFFFFF) + "\n";
            my_bytes[0] = item.get();
            my_bytes[1] = item.get();
            s += "    - PHY TX failed packets: " + (((my_bytes[1] << 8) | (my_bytes[0] & 0xFF)) & 0xFFFF) + "\n";
            my_bytes[0] = item.get();
            my_bytes[1] = item.get();
            s += "    - PHY RX errors packets: " + (((my_bytes[1] << 8) | (my_bytes[0] & 0xFF)) & 0xFFFF) + "\n";
            my_bytes[0] = item.get();
            my_bytes[1] = item.get();
            s += "    - External Temperature: " + fromU16ToFloat(((my_bytes[0] << 8) | (my_bytes[1] & 0xFF)) & 0xFFFF) + "\n";
            my_bytes[0] = item.get();
            my_bytes[1] = item.get();
            s += "    - Internal Temperature: " + fromU16ToFloat(((my_bytes[0] << 8) | (my_bytes[1] & 0xFF)) & 0xFFFF) + "\n";
            my_bytes[3] = item.get();
            my_bytes[2] = item.get();
            my_bytes[1] = item.get();
            my_bytes[0] = item.get();
			float freq = ByteBuffer.wrap(my_bytes).order(ByteOrder.BIG_ENDIAN).getFloat();
			s += "    - Frequency: " + freq + "\n\n";
            
            item.clear();
        }
        
        /* Footer */
        ByteBuffer footer = ByteBuffer.allocate(Integer.SIZE / 8);
        byte[] footer_bytes = new byte[Integer.SIZE / 8];
        file_stream.read(footer_bytes);
        footer.put(footer_bytes);
        footer.flip();
        s += "Footer HK:" + "\n";
        s += "    - Experiment Duration: " + footer.getInt() + "\n";
        footer.clear();
        
        file_stream.close();
        
        
        s += "Finished of parsing!" + "\n";
    
        System.out.println(s);
        if(writer != null) {
        	writer.write(s);
        	writer.close();
        }
    }
    
}
