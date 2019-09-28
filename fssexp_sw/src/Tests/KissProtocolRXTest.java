package Tests;

import IPCStack.KissProtocol;

public class KissProtocolRXTest {

	public final static void main(String args[]) {
		KissProtocol kiss = new KissProtocol(null);
		kiss.open();
		System.out.println("Receiving from KISS ...");
		byte[] data = kiss.receive();
		System.out.println("Data received: " + new String(data));
		String s = "";
		for(byte b : data) {
			s += String.format("\\x%02x", b);
		}
		System.out.println("Data received: " + s);
		kiss.close();		
	}
	
}
