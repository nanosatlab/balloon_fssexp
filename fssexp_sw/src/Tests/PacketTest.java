package Tests;

import java.io.IOException;

import InterSatelliteCommunications.Packet;

public class PacketTest {
	
	public static void main(String args[]) throws IOException
    {
		Packet packet = new Packet();
		packet.source = 2;
		packet.destination = 3;
		packet.counter = 15;
		packet.prot_num = 1;
		packet.timestamp = 1456615754;
		packet.computeChecksum();
		System.out.println("Packet checksum: " + packet.checksum);
		
		byte[] stream = packet.toBytes();
		
		System.out.println("Packet length: " + stream.length);
		
		Packet other_packet = new Packet();
		boolean good = other_packet.fromBytes(stream);
		System.out.println("Good packet? " + good);	
		System.out.println("Packet checksum: " + other_packet.checksum);
		
    }
}
