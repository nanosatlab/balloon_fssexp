package Tests;

import IPCStack.UartInterface;

public class UartInterfaceTest {

	public final static void main(String args[]) {
		UartInterface device = new UartInterface(null);
		device.open();
		byte[] data = device.receiveByte();
		System.out.println("Received: " + new String(data));
		device.send("PACO".getBytes());
		device.close();		
	}
	
}
