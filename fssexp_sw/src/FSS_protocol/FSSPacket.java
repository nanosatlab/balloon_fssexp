package FSS_protocol;

import java.nio.ByteBuffer;

import CRC.CRC;
import Common.Constants;

public class FSSPacket {

    /* header */
    private int m_source;
    private int m_destination;
    private long m_timestamp;
    private int m_counter;
    private int m_type;
    private int m_length;
    
    /* data - depends of the packet type */
    private byte[] m_packet_data;
    
    /* footer */
    private short m_checksum; /* CRC16 */
    private ByteBuffer m_checksum_converter;
    
    public FSSPacket() {
        resetValues();
        m_checksum_converter = ByteBuffer.allocate(Short.SIZE / 8);
    }
    
    public FSSPacket(FSSPacket packet) {
    	copyFrom(packet);
    }
    
    public int getSource() { return m_source; }
    
    public void setSource(int source) { m_source = source; }
    
    public int getDestination() { return m_destination; }
    
    public void setDestination(int destination) { m_destination = destination; }
    
    public long getTimestamp() { return m_timestamp; }
    
    public void setTimestamp(long timestamp) { m_timestamp = timestamp; }
    
    public int getCounter() { return m_counter; }
    
    public void setCounter(int counter) { m_counter = counter; }
    
    public int getType() { return m_type; }
    
    public void setType(int type) { m_type = type; }
    
    public int getLength() { return m_length; }
    
    public void setLength(int length) { m_length = length; }
    
    public short getChecksum() { return m_checksum; }
    
    public byte[] getData() { return m_packet_data; }
    
    public void setData(byte[] data) { 
        m_packet_data = null;
        m_packet_data = data; 
    }
     
    public void resetValues() {
        
        /* header */
        m_source = -1;
        m_destination = -1;
        m_timestamp = -1;
        m_counter = -1;
        m_type = 6; /* NO_VALID type */
        m_length = -1;
        
        /* data */
        m_packet_data = null;
        
        /* footer */
        m_checksum = -1;
    }
    
    public boolean fromBytes(byte[] packet_bytes) {
        
        /* Read header  - SourceDestination | Time | Counter | Type | Length */
        byte[] header = new byte[Constants.header_size];
        System.arraycopy(packet_bytes, 0, header, 0, Constants.header_size);
        /* 1 Byte */
        m_source = (header[0] & 0xF0) >> 4;
        m_destination = header[0] & 0x0F;
        /* 4 Bytes */
        byte[] timestamp_bytes = new byte[8];
        System.arraycopy(header, 1, timestamp_bytes, 0, timestamp_bytes.length);
        m_timestamp = ByteBuffer.wrap(timestamp_bytes).getLong();
        /* 1 Byte */
        m_counter = header[9];
        /* 1 Byte */
        m_type = header[10];
        /* 2 Byte */
        m_length = ((int)header[11] << 8) + (header[12] & 0xff);

        /* Read data */
        m_packet_data = new byte[m_length];
        System.arraycopy(packet_bytes, Constants.header_size, m_packet_data, 0, m_packet_data.length);
        
        /* Read Checksum */
        byte[] content = new byte[m_packet_data.length + header.length];
        System.arraycopy(header, 0, content, 0, header.length);
        System.arraycopy(m_packet_data, 0, content, header.length, m_packet_data.length);
        byte[] checksum = new byte[Short.SIZE / 8];
        System.arraycopy(packet_bytes, Constants.header_size + m_length, checksum, 0, checksum.length);
        m_checksum_converter.clear();
        m_checksum_converter.put(checksum);
        m_checksum_converter.rewind();
        m_checksum = m_checksum_converter.getShort();
        return isPacketCorrect(m_checksum, content);
    }
    
    public byte[] headerToBytes() {
        
        /* header */
        byte[] header = new byte[Constants.header_size];
        header[0] = (byte) ((m_source & 0xFF) << 4);
        header[0] = (byte) (header[0] ^ (m_destination & 0xFF));
        byte[] timestamp = ByteBuffer.allocate(8).putLong(m_timestamp).array();
        System.arraycopy(timestamp, 0, header, 1, timestamp.length);
        header[9] = (byte) (m_counter & 0xFF);
        header[10] = (byte) (m_type & 0xFF);
        header[11] = (byte) ((m_length >> 8) & 0xFF);
        header[12] = (byte) (m_length & 0xFF);
        
        return header;
    }
    
    public byte[] toBytes() {
        
        /* header */
        byte[] header = headerToBytes();
        
        /* data */
        byte[] data = m_packet_data;
        
        /* checksum */
        byte[] content = new byte[header.length + data.length];
        System.arraycopy(header, 0, content, 0, header.length);
        System.arraycopy(data, 0, content, header.length, data.length);
        m_checksum = (short)CRC.calculateCRC(CRC.Parameters.CRC16, content);
        m_checksum_converter.clear();
        m_checksum_converter.putShort(m_checksum);
        m_checksum_converter.rewind();
        byte[] checksum = m_checksum_converter.array();
        
        /* All the packet */
        byte[] output = new byte[content.length + checksum.length];
        System.arraycopy(content, 0, output, 0, content.length);
        System.arraycopy(checksum, 0, output, content.length, checksum.length);
        return output;
    }

    public byte[] toBytesNoData() {
        byte[] header = headerToBytes();
        
        /* checksum */
        m_checksum = (short)CRC.calculateCRC(CRC.Parameters.CRC16, header);
        m_checksum_converter.clear();
        m_checksum_converter.putShort(m_checksum);
        m_checksum_converter.rewind();
        byte[] checksum = m_checksum_converter.array();
        
        /* All the packet */
        byte[] output = new byte[header.length + checksum.length];
        System.arraycopy(header, 0, output, 0, header.length);
        System.arraycopy(checksum, 0, output, header.length, checksum.length);
        return output;
    }
    
    private boolean isPacketCorrect(short checksum, byte[] content) {
        /* Compute checksum */
    	short recv_checksum = (short)CRC.calculateCRC(CRC.Parameters.CRC16, content);
        
        /* Compare with the checksum value */
        return (recv_checksum == checksum);
    }

    private void copyFrom(FSSPacket packet) {
    	/* header */
        m_source = packet.getSource();
        m_destination = packet.getDestination();
        m_timestamp = packet.getTimestamp();
        m_counter = packet.getCounter();
        m_type = packet.getType(); /* NO_VALID type */
        m_length = packet.getLength();
        
        /* data */
        m_packet_data = packet.getData();
        
        /* footer */
        m_checksum = packet.getChecksum();
    }
    
}
