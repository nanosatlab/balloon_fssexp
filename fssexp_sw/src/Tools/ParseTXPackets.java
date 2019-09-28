package Tools;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;

import CRC.CRC;

public class ParseTXPackets {

    public final static int FSS_PACKET_SERVICE_PUBLISH = 0;
    public final static int FSS_PACKET_SERVICE_REQUEST = 1;
    public final static int FSS_PACKET_SERVICE_ACCEPT = 2;
    public final static int FSS_PACKET_DATA = 3;
    public final static int FSS_PACKET_DATA_ACK = 4;
    public final static int FSS_PACKET_CLOSE = 5;
    public final static int FSS_PACKET_CLOSE_ACK = 6;
    public final static int FSS_PACKET_ALIVE = 7;
    public final static int FSS_PACKET_GOOD = 8;
    public final static int FSS_PACKET_NOT_VALID = 9;
    
    
    public static void main(String args[]) throws IOException {
        
        System.out.println("Welcome to the Parsing Tool for the Transmitted Files of the FSS Experiment");
        System.out.println("");
        
        int header_size = 13;
        
        String result_file;
        result_file = "./sat/fss/to_download/tx_packets.data";
        
        
        
        File tx_file = new File(result_file);
        FileInputStream file_stream = new FileInputStream(tx_file);
        System.out.println("File Size: " + tx_file.length() + " Bytes");
        System.out.println("");
        
        
        /* TX FSS Packet */
        System.out.println("******** Parsing TX FSS Packets ********");
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
        int value = 0;
        value = file_stream.read(header_bytes);
        
        while(value != -1) {
            
            for(int j = 0; j < header_bytes.length; j++)
            System.out.println(String.format("%01X", header_bytes[j]));
            
            header.put(header_bytes);
            header.flip();
            first = header.get();
            source = (first & 0xF0) >> 4;
            destination = first & 0x0F;
            timestamp = header.getLong();
            counter = header.get();
            type = header.get();
            length = header.getShort();
            
            System.out.println("Packet at " + timestamp);
            System.out.println("- Header:");
            System.out.println("    - source " + source);
            System.out.println("    - destination " + destination);
            System.out.println("    - counter " + counter);
            System.out.println("    - type " + type);
            System.out.println("    - length " + length);
            
            if(type != FSS_PACKET_CLOSE && 
                    type != FSS_PACKET_CLOSE_ACK && 
                    type != FSS_PACKET_NOT_VALID && 
                    type != FSS_PACKET_SERVICE_ACCEPT && 
                    type != FSS_PACKET_SERVICE_REQUEST &&
                    type != FSS_PACKET_DATA) {

                data_bytes = new byte[length];
                file_stream.read(data_bytes);
                data = ByteBuffer.allocate(length);
                data.put(data_bytes);
                data.flip();
                if(type == FSS_PACKET_SERVICE_PUBLISH) {
                    System.out.println("- PUBLISH Content:");
                    System.out.println("    - Service type " + data.getInt());
                    System.out.println("    - Service capacity " + data.getInt());
                }
                data.clear();
            } else {
            	data_bytes = new byte[0];
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
            value = file_stream.read(header_bytes);
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
