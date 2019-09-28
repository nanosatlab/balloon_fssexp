package Tests;

import Common.Constants;
import Common.TimeUtils;
import Lockers.UartBuffer;
import Storage.Log;

public class UartBufferTest {

	public static final void main(String args[]) {
		TimeUtils timer = new TimeUtils();
		Log logger = new Log(timer);
		UartBuffer buffer = new UartBuffer(logger);
		
		int writings = 20;
		byte[] paco = new byte[1];
		
		System.out.println("Initial Buffer size: " + buffer.bytesAvailable());
		
		for(int i = 0; i < writings; i++) {
			paco[0] = (byte)(i & 0xFF);
			buffer.write(paco);
			System.out.println("Buffer size: " + buffer.bytesAvailable());
		}
		
		System.out.println("Reading something");
		
		byte container;
		int readings = 10;
		for(int i = 0; i < readings; i++) {
			container = buffer.readByte();
			System.out.println("Read byte: " + String.format("0x%02x", container));
			System.out.println("Buffer size: " + buffer.bytesAvailable());
		}
		
		for(int i = 0; i < writings; i++) {
			paco[0] = (byte)(i & 0xFF);
			buffer.write(paco);
			System.out.println("Buffer size: " + buffer.bytesAvailable());
		}
		
		readings = 30;
		for(int i = 0; i < readings; i++) {
			container = buffer.readByte();
			System.out.println("Read byte: " + String.format("0x%02x", container));
			System.out.println("Buffer size: " + buffer.bytesAvailable());
		}
		
		writings = Constants.uart_max_buffer_size;
		paco = new byte[1];
		
		System.out.println("Initial Buffer size: " + buffer.bytesAvailable());
		
		for(int i = 0; i < writings; i++) {
			paco[0] = (byte)(i & 0xFF);
			buffer.write(paco);
			System.out.println("Buffer size: " + buffer.bytesAvailable());
		}
		
		System.out.println("Read only one");
		for(int i = 0; i < writings; i++) {
			buffer.readByte();
			System.out.println("Buffer size: " + buffer.bytesAvailable());
		}
		
		System.out.println("Write an array");
		byte[] data = new byte[10];
		buffer.write(data);
		System.out.println("Buffer size: " + buffer.bytesAvailable());
		System.out.println("Read only one");
		buffer.readByte();
		System.out.println("Buffer size: " + buffer.bytesAvailable());
		
	}
	
}
