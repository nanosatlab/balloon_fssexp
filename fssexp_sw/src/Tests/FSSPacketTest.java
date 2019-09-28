package Tests;

import java.nio.ByteBuffer;

import FSS_protocol.FSSPacket;

public class FSSPacketTest {

    public static void main(String args[]) {
        
        /* Values of reference */
        int source = 5;
        int destination = 3;
        long timestamp = System.currentTimeMillis();
        int counter = 15;
        int type = 3;
        int checksum = 1;
        byte[] data = ByteBuffer.allocate(4).putInt(10).array();
        int length = data.length;
        
        FSSPacket packet = new FSSPacket();
        packet.setSource(source);
        packet.setDestination(destination);
        packet.setTimestamp(timestamp);
        packet.setCounter(counter);
        packet.setType(type);
        packet.setLength(length);
        packet.setData(data);
        
        /* Container to test */
        FSSPacket packet_test = new FSSPacket();
        System.out.println("The FSSPacket Initial values are:");
        System.out.println("    - Source: " + packet_test.getSource());
        System.out.println("    - Destination: " + packet_test.getDestination());
        System.out.println("    - Timestamp: " + packet_test.getTimestamp());
        System.out.println("    - Counter: " + packet_test.getCounter());
        System.out.println("    - Type: " + packet_test.getType());
        System.out.println("    - Length: " + packet_test.getLength());
        System.out.println("    - Checksum: " + packet_test.getChecksum());
        
        System.out.println("");
        
        System.out.println("The FSSPacket ref values are:");
        System.out.println("Header:");
        System.out.println("    - Source: " + packet.getSource());
        System.out.println("    - Destination: " + packet.getDestination());
        System.out.println("    - Timestamp: " + packet.getTimestamp());
        System.out.println("    - Counter: " + packet.getCounter());
        System.out.println("    - Type: " + packet.getType());
        System.out.println("    - Length: " + packet.getLength());
        System.out.println("    - Checksum: " + packet.getChecksum());
        
        /* Convert to bytes */
        byte[] byte_array = packet.toBytes();
        System.out.println("");
        System.out.println("The FSSPacket ref bytes are:");
        System.out.println("Header:");
        System.out.println("    - 1st Byte: " + String.format("0x%02X", byte_array[0]));
        System.out.println("    - 2nd Byte: " + String.format("0x%02X", byte_array[1]));
        System.out.println("    - 3rt Byte: " + String.format("0x%02X", byte_array[2]));
        System.out.println("    - 4th Byte: " + String.format("0x%02X", byte_array[3]));
        System.out.println("    - 5th Byte: " + String.format("0x%02X", byte_array[4]));
        System.out.println("    - 6th Byte: " + String.format("0x%02X", byte_array[5]));
        System.out.println("    - 7th Byte: " + String.format("0x%02X", byte_array[6]));
        System.out.println("    - 8th Byte: " + String.format("0x%02X", byte_array[7]));
        System.out.println("    - 9th Byte: " + String.format("0x%02X", byte_array[8]));
        System.out.println("    - 10th Byte: " + String.format("0x%02X", byte_array[9]));
        System.out.println("    - 11th Byte: " + String.format("0x%02X", byte_array[10]));
        System.out.println("    - 12th Byte: " + String.format("0x%02X", byte_array[11]));
        System.out.println("    - 13th Byte: " + String.format("0x%02X", byte_array[12]));
        System.out.println("Checksum:");
        System.out.println("    - Final Byte: " + String.format("0x%02X", byte_array[byte_array.length - 1]));
        
        /* Convert from bytes */
        packet_test.fromBytes(byte_array);
        System.out.println("");
        System.out.println("The FSSPacket Initial values are:");
        System.out.println("Header:");
        System.out.println("    - Source: " + packet_test.getSource());
        System.out.println("    - Destination: " + packet_test.getDestination());
        System.out.println("    - Timestamp: " + packet_test.getTimestamp());
        System.out.println("    - Counter: " + packet_test.getCounter());
        System.out.println("    - Type: " + packet_test.getType());
        System.out.println("    - Length: " + packet_test.getLength());
        System.out.println("Data: " + ByteBuffer.wrap(packet_test.getData()).getInt());
        System.out.println("Checksum:");
        System.out.println("    - Checksum: " + packet_test.getChecksum());
        
    
    
    }
    
}
