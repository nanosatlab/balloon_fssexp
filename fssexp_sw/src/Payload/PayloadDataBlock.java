package Payload;

import java.nio.ByteBuffer;

import Common.Constants;
import Housekeeping.HousekeepingItem;


public class PayloadDataBlock {

	private ByteBuffer stream;
	
	/* Header of PayloadDataBlock */
	public int sat_id;
	public long timestamp;
	
	/* NanoPi hk */
	// TODO:
	
	/* Experiment hk + RF ISL hk */
	public HousekeepingItem exp_hk;
	
	/* Padding data */
	public byte[] padding_data; 
	
	public PayloadDataBlock()
	{
		stream = ByteBuffer.allocate(getSize());
		padding_data = new byte[getPaddingSize()];
		for(int i = 0; i < padding_data.length; i ++) {
			padding_data[i] = (byte)(i & 0xFF);
		}
	}
	
	public int getSize()
	{
		return Constants.data_mtu;
	}
	
	public int getPaddingSize()
	{
		return getSize() - getUsefulSize();
	}
	
	public int getUsefulSize()
	{
		return (4 + 8 + exp_hk.getSize());
	}
	
	public byte[] getBytes()
	{
		stream.clear();
		stream.putInt(sat_id);
		stream.putLong(timestamp);
		stream.put(exp_hk.getBytes());
		stream.put(padding_data);
		stream.rewind();
		return stream.array();
	}
}
