package Tests;

import java.nio.ByteBuffer;

import InterSatelliteCommunications.Packet;

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
        int prot_num = 3;
        
        Packet packet = new Packet();
        packet.source = source;
        packet.destination = destination;
        packet.prot_num = prot_num;
        packet.timestamp = timestamp;
        packet.counter = counter;
        packet.type = type;
        packet.length = length;
        packet.setData(data);
        
        /* Container to test */
        Packet packet_test = new Packet();
        System.out.println("The FSSPacket Initial values are:");
        System.out.println("    - Source: " + packet_test.source);
        System.out.println("    - Destination: " + packet_test.destination);
        System.out.println("    - Protocol Number: " + packet_test.prot_num);
        System.out.println("    - Timestamp: " + packet_test.timestamp);
        System.out.println("    - Counter: " + packet_test.counter);
        System.out.println("    - Type: " + packet_test.type);
        System.out.println("    - Length: " + packet_test.length);
        System.out.println("    - Checksum: " + packet_test.getChecksum());
        
        System.out.println("");
        byte[] byte_array = packet.toBytes();
        
        System.out.println("The FSSPacket ref values are:");
        System.out.println("Header:");
        System.out.println("    - Source: " + packet.source);
        System.out.println("    - Destination: " + packet.destination);
        System.out.println("    - Protocol Number: " + packet.prot_num);
        System.out.println("    - Timestamp: " + packet.timestamp);
        System.out.println("    - Counter: " + packet.counter);
        System.out.println("    - Type: " + packet.type);
        System.out.println("    - Length: " + packet.length);
        System.out.println("    - Checksum: " + packet.getChecksum());
        
        /* Convert to bytes */
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
        System.out.println("    - Source: " + packet_test.source);
        System.out.println("    - Destination: " + packet_test.destination);
        System.out.println("    - Protocol Number: " + packet_test.prot_num);
        System.out.println("    - Timestamp: " + packet_test.timestamp);
        System.out.println("    - Counter: " + packet_test.counter);
        System.out.println("    - Type: " + packet_test.type);
        System.out.println("    - Length: " + packet_test.length);
        System.out.println("Data: " + ByteBuffer.wrap(packet_test.getData()).getInt());
        System.out.println("Checksum:");
        System.out.println("    - Checksum: " + packet_test.getChecksum());
        
    
    
    }
    
}
