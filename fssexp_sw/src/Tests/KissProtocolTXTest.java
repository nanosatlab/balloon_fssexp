package Tests;

import IPCStack.KissProtocol;

public class KissProtocolTXTest {

	public final static void main(String args[]) {
		KissProtocol kiss = new KissProtocol(null);
		kiss.open();
		kiss.send("PACO".getBytes());
		kiss.close();		
	}
	
}
