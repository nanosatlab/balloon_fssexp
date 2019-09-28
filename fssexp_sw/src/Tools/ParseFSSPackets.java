package Tools;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import CRC.CRC;
import Common.Constants;

public class ParseFSSPackets {

    public static void main(String args[]) throws IOException {
        
        System.out.println("Welcome to the Parsing Tool for the Transmitted Files of the FSS Experiment");
        System.out.println("");
        
        boolean input_arg = false;
        String input = "./sat/fss/to_download/fss_packets.data";
        		
        if(args.length > 0) {
			for(String arg : args) {
				switch(arg) {
					case "-i":
						input_arg = true;
						break;	
					default:
						if(input_arg) {
							input_arg = false;
							input = arg;
						}
				}
			}
		}
        
        
        int header_size = 13;
        
        String result_file = input;
        System.out.println("Reading content from " + result_file);
        
        
        File tx_file = new File(result_file);
        FileInputStream file_stream = new FileInputStream(tx_file);
        System.out.println("File Size: " + tx_file.length() + " Bytes");
        System.out.println("");
        
        
        /* FSS Data */
        byte[] data_num_bytes = new byte[4];
        file_stream.read(data_num_bytes);
        ByteBuffer number = ByteBuffer.allocate(Integer.SIZE / 8).put(data_num_bytes);
        number.flip();
        int data_num = number.getInt();
        
        System.out.println("******** Parsing Data Packets -> " + data_num + " Data Blocks **********");
        System.out.println("");
        
        ByteBuffer data_timestamp = ByteBuffer.allocate(Long.SIZE / 8);;
        ByteBuffer temperature = ByteBuffer.allocate(Short.SIZE / 8);
        ByteBuffer tx_pw = ByteBuffer.allocate(Short.SIZE / 8);
        ByteBuffer rx_pw = ByteBuffer.allocate(Short.SIZE / 8);
        byte[] reference_data = new byte[155];
        int sat_id;
        byte[] data_timestamp_bytes = new byte[Long.SIZE / 8];
        byte[] telemetry = new byte[51];
        
        for(int i = 0; i < data_num; i++) {

            sat_id = file_stream.read();
            
            file_stream.read(data_timestamp_bytes);
            data_timestamp.put(data_timestamp_bytes);
            data_timestamp.flip();
            
            file_stream.read(telemetry);
            
            file_stream.read(reference_data);
            
            System.out.println("Data at " + data_timestamp.getLong());
            System.out.println("    - Satellite ID: " + sat_id);
            System.out.println("    - RF ISL HK:");
            
            System.out.println("    	- Boot count: " + (telemetry[0] & 0xFF)); 
            System.out.println("    	- Actual RSSI: " + rssi_raw_dbm(telemetry[1] & 0xFF)); 
            System.out.println("    	- Last RSSI: " + rssi_raw_dbm(telemetry[2] & 0xFF));
            System.out.println("    	- Last LQI: " + lqi_status(telemetry[3] & 0xFF)); 
            System.out.println("    	- TX Power: " + (telemetry[4] & 0xFF));
            System.out.println("    	- PHY TX packets: " + (((telemetry[8] << 24) | (telemetry[7] << 16) | (telemetry[6] << 8) | telemetry[5] & 0xFF) & 0xFFFFFFFF));
            System.out.println("    	- PHY RX packets: " + (((telemetry[12] << 24) | (telemetry[11] << 16) | (telemetry[10] << 8) | telemetry[9] & 0xFF) & 0xFFFFFFFF));
            System.out.println("    	- LL TX packets: " + (((telemetry[16] << 24) | (telemetry[15] << 16) | (telemetry[14] << 8) | telemetry[13] & 0xFF) & 0xFFFFFFFF));
            System.out.println("    	- LL RX packets: " + (((telemetry[20] << 24) | (telemetry[19] << 16) | (telemetry[18] << 8) | telemetry[17] & 0xFF) & 0xFFFFFFFF));
            System.out.println("    	- PHY TX failed packets: " + (((telemetry[22] << 8) | telemetry[21] & 0xFF) & 0xFFFF));
            System.out.println("    	- PHY RX errors packets: " + (((telemetry[24] << 8) | telemetry[23] & 0xFF) & 0xFFFF));
            System.out.println("    	- External Temperature: " + fromU16ToFloat(((telemetry[25] << 8) | telemetry[26] & 0xFF) & 0xFFFF));
            System.out.println("    	- Internal Temperature: " + fromU16ToFloat(((telemetry[27] << 8) | telemetry[28] & 0xFF) & 0xFFFF));
            byte[] freq_bytes = new byte[4];
			freq_bytes[3] = telemetry[29];
			freq_bytes[2] = telemetry[30];
			freq_bytes[1] = telemetry[31];
			freq_bytes[0] = telemetry[32];
			float freq = ByteBuffer.wrap(freq_bytes).order(ByteOrder.BIG_ENDIAN).getFloat();
			System.out.println("    	- Frequency: " + freq);
            System.out.println("    	- RX Queue: " + (telemetry[33] & 0xFF));
            System.out.println("    	- TX Queue: " + (telemetry[34] & 0xFF));
            System.out.println("    	- Free Stack: " + (((telemetry[36] << 8) | telemetry[35] & 0xFF) & 0xFFFF) + " " + (((telemetry[38] << 8) | telemetry[37] & 0xFF) & 0xFFFF) + " " +
            											+ (((telemetry[40] << 8) | telemetry[39] & 0xFF) & 0xFFFF) + " " + (((telemetry[42] << 8) | telemetry[41] & 0xFF) & 0xFFFF));
            System.out.println("    	- User Stack: " + (((telemetry[44] << 8) | telemetry[43] & 0xFF) & 0xFFFF) + " " + (((telemetry[46] << 8) | telemetry[45] & 0xFF) & 0xFFFF) + " " +
														+ (((telemetry[48] << 8) | telemetry[47] & 0xFF) & 0xFFFF) + " " + (((telemetry[50] << 8) | telemetry[49] & 0xFF) & 0xFFFF));
            
            System.out.println("    - Reference Data Size " + reference_data.length);
            
            data_timestamp.clear();
            temperature.clear();
            tx_pw.clear();
            rx_pw.clear();
            
            System.out.println("");

        }

        /* Read TX and RX number of packets */
        file_stream.read(data_num_bytes);
        number.flip();
        number.put(data_num_bytes);
        number.flip();
        int tx_data_num = number.getInt();
        
        file_stream.read(data_num_bytes);
        number.flip();
        number.put(data_num_bytes);
        number.flip();
        int rx_data_num = number.getInt();
        
        
        /* TX FSS Packet */
        System.out.println("******** Parsing TX FSS Packets -> " + tx_data_num + " packets ********");
        System.out.println("");
        
        ByteBuffer header = ByteBuffer.allocate(header_size);
        ByteBuffer data;
        ByteBuffer checksum = ByteBuffer.allocate(Short.SIZE / 8);
        byte[] header_bytes = new byte[header_size];
        byte[] data_bytes;
        byte[] checksum_bytes = new byte[Short.SIZE / 8];
        int counter;
        int destination;
        int source;
        int first;
        long timestamp;
        int type;
        short length;
        int publish_capacity;
        byte[] content;
        short computed_checksum;
        
        for(int i = 0; i < tx_data_num; i++) {
            
            file_stream.read(header_bytes);
            header.put(header_bytes);
            header.flip();
            first = (header.get() & 0x0F);
            source = (first & 0xF0) >> 4;
            destination = first & 0x0F;
            timestamp = header.getLong();
            counter = (header.get() & 0xFF);
            type = (header.get() & 0xFF);
            length = header.getShort();
            
            System.out.println("Packet at " + timestamp);
            System.out.println("- Header:");
            System.out.println("    - source " + source);
            System.out.println("    - destination " + destination);
            System.out.println("    - counter " + counter);
            System.out.println("    - type " + type);
            System.out.println("    - length " + length);
            
            if(type != Constants.FSS_PACKET_CLOSE && 
                    type != Constants.FSS_PACKET_CLOSE_ACK && 
                    type != Constants.FSS_PACKET_NOT_VALID && 
                    type != Constants.FSS_PACKET_DATA &&
                    type != Constants.FSS_PACKET_CLOSE_DATA_ACK) {

                data_bytes = new byte[length];
                file_stream.read(data_bytes);
                data = ByteBuffer.allocate(length);
                data.put(data_bytes);
                data.flip();
                if(type == Constants.FSS_PACKET_SERVICE_PUBLISH) {
                    System.out.println("- PUBLISH Content:");
                    System.out.println("    - Service type " + data.getInt());
                    System.out.println("    - Service capacity " + data.getInt());
                } else if(type == Constants.FSS_PACKET_DATA_ACK) {
                    publish_capacity = data.getInt();
                    System.out.println("- DATA ACK Content:");
                    System.out.println("    - Federation capacity " + publish_capacity);
                } else if(type == Constants.FSS_PACKET_SERVICE_REQUEST) {
                	System.out.println("- REQUEST Content:");
                    System.out.println("    - Service type " + data.getInt());
                } else if(type == Constants.FSS_PACKET_SERVICE_ACCEPT) {
                	System.out.println("- ACCEPT Content:");
                    System.out.println("    - Service type " + data.getInt());
                    System.out.println("    - Service capacity " + data.getInt());
                } 
                data.clear();
            } else {
            	data_bytes = new byte[0];
            	if(type == Constants.FSS_PACKET_CLOSE) {
                	System.out.println("- CLOSE packet");
                } else if(type == Constants.FSS_PACKET_CLOSE_ACK) {
                	System.out.println("- CLOSE ACK packet");
                } else if(type == Constants.FSS_PACKET_CLOSE_DATA_ACK) {
                	System.out.println("- CLOSE DATA ACK packet");
                } else if(type == Constants.FSS_PACKET_DATA) {
                	System.out.println("- DATA packet");
                }
            }
            
            file_stream.read(checksum_bytes);
            checksum.put(checksum_bytes);
            checksum.rewind();
            System.out.println("- Footer:");
            System.out.println("    - checksum " + checksum.getShort());
            content = new byte[header_bytes.length + data_bytes.length];
            System.arraycopy(header_bytes, 0, content, 0, header_bytes.length);
            System.arraycopy(data_bytes, 0, content, header_bytes.length, data_bytes.length);
            computed_checksum = (short)CRC.calculateCRC(CRC.Parameters.CRC16, content);
            System.out.println("    - Computed checksum " + computed_checksum);
            
            header.clear();
            checksum.clear();
            
            System.out.println("");
        }
        
        /* RX FSS Packet */
        System.out.println("******** Parsing RX FSS Packets -> " + rx_data_num + " packets ********");
        System.out.println("");
        
        for(int i = 0; i < rx_data_num; i++) {
            
            
            file_stream.read(header_bytes);
            
            header.put(header_bytes);
            header.flip();
            first = (header.get() & 0xFF);
            source = (first & 0xF0) >> 4;
            destination = first & 0x0F;
            timestamp = header.getLong();
            counter = (header.get() & 0xFF);
            type = (header.get() & 0xFF);
            length = header.getShort();
            
            System.out.println("Packet at " + timestamp);
            System.out.println("- Header:");
            System.out.println("    - source " + source);
            System.out.println("    - destination " + destination);
            System.out.println("    - counter " + counter);
            System.out.println("    - type " + type);
            System.out.println("    - length " + length);
            
            if(type != Constants.FSS_PACKET_CLOSE && 
                    type != Constants.FSS_PACKET_CLOSE_ACK && 
                    type != Constants.FSS_PACKET_NOT_VALID && 
                    type != Constants.FSS_PACKET_DATA &&
                    type != Constants.FSS_PACKET_CLOSE_DATA_ACK) {
                data_bytes = new byte[length];
                file_stream.read(data_bytes);
                data = ByteBuffer.allocate(length);
                data.put(data_bytes);
                data.flip();
                if(type == Constants.FSS_PACKET_SERVICE_PUBLISH) {
                    publish_capacity = data.getInt();
                    System.out.println("- PUBLISH Content:");
                    System.out.println("    - Service type " + data.getInt());
                    System.out.println("    - Service capacity " + publish_capacity);
                } else if(type == Constants.FSS_PACKET_DATA_ACK) {
                    publish_capacity = data.getInt();
                    System.out.println("- DATA ACK Content:");
                    System.out.println("    - Federation capacity " + publish_capacity);
                } else if(type == Constants.FSS_PACKET_SERVICE_REQUEST) {
                	System.out.println("- REQUEST Content:");
                    System.out.println("    - Service type " + data.getInt());
                } else if(type == Constants.FSS_PACKET_SERVICE_ACCEPT) {
                	System.out.println("- ACCEPT Content:");
                    System.out.println("    - Service type " + data.getInt());
                    System.out.println("    - Service capacity " + data.getInt());
                }
                
                data.clear();
            } else {
            	data_bytes = new byte[0];
            	if(type == Constants.FSS_PACKET_CLOSE) {
                	System.out.println("- CLOSE packet");
                } else if(type == Constants.FSS_PACKET_CLOSE_ACK) {
                	System.out.println("- CLOSE ACK packet");
                } else if(type == Constants.FSS_PACKET_CLOSE_DATA_ACK) {
                	System.out.println("- CLOSE DATA ACK packet");
                } else if(type == Constants.FSS_PACKET_DATA) {
                	System.out.println("- DATA packet");
                }
            }
            
            file_stream.read(checksum_bytes);
            checksum.put(checksum_bytes);
            checksum.rewind();
            System.out.println("- Footer:");
            System.out.println("    - checksum " + checksum.getShort());
            content = new byte[header_bytes.length + data_bytes.length];
            System.arraycopy(header_bytes, 0, content, 0, header_bytes.length);
            System.arraycopy(data_bytes, 0, content, header_bytes.length, data_bytes.length);
            computed_checksum = (short)CRC.calculateCRC(CRC.Parameters.CRC16, content);
            System.out.println("    - Computed checksum " + computed_checksum);
            
            
            header.clear();
            checksum.clear();
            
            System.out.println("");
        }
        
        
        file_stream.close();
        
        
        System.out.println("Finished of parsing!");
    }
    
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
    
}
