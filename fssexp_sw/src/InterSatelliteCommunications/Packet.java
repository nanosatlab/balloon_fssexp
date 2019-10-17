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
    private short checksum; /* CRC16 */
    
    /* Temporal variables */
    private byte[] bytes;
    private byte[] header_stream;
    private ByteBuffer checksum_stream;
    private ByteBuffer timestamp_stream;
    private ByteBuffer counter_stream;
    private ByteBuffer length_stream;
    
    public Packet() 
    {
        resetValues();
        /* Maximum unit is 8 Bytes */
        bytes = new byte[8];
        header_stream = new byte[Constants.header_size];
        checksum_stream = ByteBuffer.allocate(Short.SIZE / 8);
        timestamp_stream = ByteBuffer.allocate(Long.SIZE / 8);
        counter_stream = ByteBuffer.allocate(Integer.SIZE / 8);
        length_stream = ByteBuffer.allocate(Short.SIZE / 8);
    }
    
    public Packet(Packet packet) {
    	copyFrom(packet);
    }
    
    public short getChecksum() { return checksum; }
    
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
        packet_data = null;
        /* footer */
        checksum = -1;
    }
    
    public byte[] getHeader() 
    {    
    	header_stream[0] = (byte) (source & 0xFF);
        header_stream[1] = (byte) (destination & 0xFF);
        header_stream[2] = (byte) (prot_num & 0xFF);
        timestamp_stream.clear();
        timestamp_stream.putLong(timestamp);
        timestamp_stream.rewind();
        bytes = timestamp_stream.array();
        System.arraycopy(bytes, 0, header_stream, 3, 8);
        counter_stream.clear();
        counter_stream.putInt(counter);
        counter_stream.rewind();
        bytes = counter_stream.array();
        System.arraycopy(bytes, 0, header_stream, 11, 4);
        header_stream[15] = (byte) (type & 0xFF);
        length_stream.clear();
        length_stream.putShort((short)length);
        length_stream.rewind();
        bytes = length_stream.array();
        System.arraycopy(bytes, 0, header_stream, 16, 2);
        return header_stream;
    }
    
    public void setHeader(byte[] header_stream) 
    {    
        source = header_stream[0];
        destination = header_stream[1];
        prot_num = header_stream[2];
        System.arraycopy(header_stream, 3, bytes, 0, 8);
        timestamp_stream.put(bytes, 0, 8);
        timestamp_stream.rewind();
        timestamp = timestamp_stream.getLong();
        System.arraycopy(header_stream, 11, bytes, 0, 4);
        counter_stream.put(bytes, 0, 4);
        counter_stream.rewind();
        counter = counter_stream.getInt();
        type = header_stream[15];
        System.arraycopy(header_stream, 16, bytes, 0, 2);
        length_stream.put(bytes, 0, 2);
        length_stream.rewind();
        length = length_stream.getShort();
    }
    
    public boolean fromBytes(byte[] packet_bytes) 
    {
        /* Clean the current packet structure*/
    	resetValues();
        /* Parse Header */
        System.arraycopy(packet_bytes, 0, header_stream, 0, header_stream.length);
        setHeader(header_stream);
        /* Read data */
        packet_data = new byte[length];
        System.arraycopy(packet_bytes, Constants.header_size, packet_data, 0, packet_data.length);
        /* Read Checksum */
        byte[] content = new byte[packet_data.length + header_stream.length];
        System.arraycopy(header_stream, 0, content, 0, header_stream.length);
        System.arraycopy(packet_data, 0, content, header_stream.length, packet_data.length);
        System.arraycopy(packet_bytes, Constants.header_size + length, bytes, 0, 2);
        checksum_stream.put(bytes, 0, 2);
        checksum_stream.rewind();
        checksum = checksum_stream.getShort();
        return isPacketCorrect(checksum, content);
    }
    
    public byte[] toBytes() 
    {
        /* header */
        byte[] header = getHeader();
        /* data */
        byte[] data = packet_data;
        /* checksum */
        byte[] content = new byte[header.length + data.length];
        System.arraycopy(header, 0, content, 0, header.length);
        System.arraycopy(data, 0, content, header.length, data.length);
        computeChecksum(content);
        checksum_stream.clear();
        checksum_stream.putShort(checksum);
        checksum_stream.rewind();
        byte[] checksum = checksum_stream.array();
        /* All the packet */
        byte[] output = new byte[content.length + checksum.length];
        System.arraycopy(content, 0, output, 0, content.length);
        System.arraycopy(checksum, 0, output, content.length, checksum.length);
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
    
    private void computeChecksum(byte[] content)
    {
    	checksum = (short) CRC.calculateCRC(CRC.Parameters.CRC16, content);
    }
    
    private boolean isPacketCorrect(short checksum, byte[] content) 
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
        checksum = packet.getChecksum();
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
    	str += "\n";
    	return str;
    }
    
}
