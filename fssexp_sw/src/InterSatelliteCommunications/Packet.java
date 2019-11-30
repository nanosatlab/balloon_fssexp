package InterSatelliteCommunications;

import java.nio.ByteBuffer;

import CRC.CRC;
import Common.Constants;

public class Packet
{
    /* header */
    public int source;
    public int destination;
    public int prot_num;
    public long timestamp;
    public int counter;
    public int type;
    public int length;
    
    /* data - depends of the packet type */
    private byte[] packet_data;
    
    /* footer */
    public short checksum; /* CRC16 */
    
    /* Temporal variables */
    private byte[] checksum_bytes;
    private byte[] empty_data;
    private ByteBuffer header_stream;
    private ByteBuffer checksum_stream;
    
    public Packet() 
    {
    	empty_data = new byte[0];
        resetValues();
        /* Maximum unit is 8 Bytes */
        checksum_bytes = new byte[Short.SIZE / 8];
        header_stream = ByteBuffer.allocate(getHeaderSize());
        checksum_stream = ByteBuffer.allocate(Short.SIZE / 8);
    }
    
    public Packet(Packet packet) {
    	copyFrom(packet);
    }
    
    public void setChecksum(byte[] data) { 
    	checksum_stream.clear();
    	checksum_stream.put(data);
    	checksum_stream.rewind();
    	checksum = checksum_stream.getShort();
    }
    
    public byte[] getData() { return packet_data; }
    
    public void setData(byte[] data) 
    { 
        packet_data = null;
        packet_data = data; 
    }
     
    public void resetValues() 
    {
        /* header */
        source = -1;
        destination = -1;
        prot_num = -1;
        timestamp = -1;
        counter = -1;
        type = 6; /* NO_VALID type */
        length = 0;
        /* data */
        packet_data = empty_data;
        /* footer */
        checksum = -1;
    }
    
    public byte[] getHeader() 
    {    
    	header_stream.clear();
    	header_stream.putInt(source);
    	header_stream.putInt(destination);
    	header_stream.putInt(prot_num);
    	header_stream.putLong(timestamp);
    	header_stream.putInt(counter);
    	header_stream.putInt(type);
    	header_stream.putInt(length);
    	header_stream.rewind();
    	return header_stream.array();
    }
    
    public void setHeader(byte[] stream) 
    {   
    	header_stream.clear();
    	header_stream.put(stream);
    	header_stream.rewind();
    	source = header_stream.getInt();
    	destination = header_stream.getInt();
        prot_num = header_stream.getInt();
        timestamp = header_stream.getLong();
        counter = header_stream.getInt();
        type = header_stream.getInt();
        length = header_stream.getInt();
    }
    
    public static int getHeaderSize() { return ((Integer.SIZE / 8) * 6 + (Long.SIZE / 8)); }
    
    public static int getChecksumSize() { return (Short.SIZE / 8); }
    
    public boolean fromBytes(byte[] packet_bytes) 
    {
    	/* Clean the current packet structure*/
    	resetValues();
        /* Parse Header */
    	header_stream.clear();
    	System.arraycopy(packet_bytes, 0, header_stream.array(), 0, header_stream.capacity());
        setHeader(header_stream.array());
        /* Read data */
        packet_data = new byte[length];
        System.arraycopy(packet_bytes, Packet.getHeaderSize(), packet_data, 0, packet_data.length);
        /* Read Checksum */
        computeChecksum();
        
        /* Verify the checksum */
        byte[] content = new byte[packet_data.length + header_stream.capacity()];
        System.arraycopy(header_stream.array(), 0, content, 0, header_stream.capacity());
        System.arraycopy(packet_data, 0, content, header_stream.capacity(), packet_data.length);
        System.arraycopy(packet_bytes, Packet.getHeaderSize() + length, checksum_bytes, 0, 2);
        return isPacketCorrect(checksum, content);
    }
    
    public byte[] toBytes() 
    {
    	/* Compute checksum */
        computeChecksum();
        checksum_stream.clear();
        checksum_stream.putShort(checksum);
        checksum_stream.rewind();
    	
        /* All the packet */
        byte[] output = new byte[getHeaderSize() + packet_data.length + checksum_stream.capacity()];
        System.arraycopy(getHeader(), 0, output, 0, getHeaderSize());
        System.arraycopy(packet_data, 0, output, getHeaderSize(), packet_data.length);
        System.arraycopy(checksum_stream.array(), 0, output, getHeaderSize() + packet_data.length, checksum_stream.capacity());
        return output;
    }

    public byte[] toBytesNoData() 
    {
    	/* header */
    	byte[] header = getHeader();
        /* checksum */
        computeChecksum(header);
        checksum_stream.clear();
        checksum_stream.putShort(checksum);
        checksum_stream.rewind();
        byte[] checksum = checksum_stream.array();
        /* All the packet */
        byte[] output = new byte[header.length + checksum.length];
        System.arraycopy(header, 0, output, 0, header.length);
        System.arraycopy(checksum, 0, output, header.length, checksum.length);
        return output;
    }
    
    public void computeChecksum(byte[] content)
    {
    	checksum = (short) CRC.calculateCRC(CRC.Parameters.CRC16, content);
    }
    
    public void computeChecksum()
    {
    	ByteBuffer content = ByteBuffer.allocate(getHeaderSize() + length);
    	content.putInt(source);
    	content.putInt(destination);
    	content.putInt(prot_num);
    	content.putLong(timestamp);
    	content.putInt(counter);
    	content.putInt(type);
    	content.putInt(length);
    	content.put(packet_data);
    	checksum = (short) CRC.calculateCRC(CRC.Parameters.CRC16, content.array());
    }
    
    public boolean isPacketCorrect(short checksum, byte[] content) 
    {
        /* Compute checksum */
    	short recv_checksum = (short)CRC.calculateCRC(CRC.Parameters.CRC16, content);
        /* Compare with the checksum value */
        return (recv_checksum == checksum);
    }

    private void copyFrom(Packet packet) 
    {
    	/* header */
        source = packet.source;
        destination = packet.destination;
        prot_num = packet.prot_num;
        timestamp = packet.timestamp;
        counter = packet.counter;
        type = packet.type; /* NO_VALID type */
        length = packet.length;
        /* data */
        packet_data = packet.getData();
        /* footer */
        checksum = packet.checksum;
    }
    
    public String toString()
    {
    	// TODO:
    	String str = "";
    	str += source + ",";
    	str += destination + ",";
    	str += prot_num + ",";
    	str += timestamp + ",";
    	str += counter + ",";
    	str += type + ",";
    	str += length + ",";
    	// TODO: include the data
    	str += packet_data.length + ",";
    	str += checksum;
    	return str;
    }
    
}
