import java.net.DatagramPacket;
import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * Created by Xinan on 4/10/15.
 */
public class Segment {
  private int checksum;
  private int sequenceNumber;
  private short dataLength;
  private byte[] data;

  private int numChunks = -1;
  private String fileName = null;

  public Segment(int sequenceNumber, byte[] data) {
    this.sequenceNumber = sequenceNumber;
    this.data = data;
    this.dataLength = (short) data.length;
    checksum = -sequenceNumber;
    checksum -= dataLength;
    for (byte b : data) {
      checksum -= b;
    }
  }

  public Segment(DatagramPacket packet) {
    byte[] data = packet.getData();
    ByteBuffer buffer = ByteBuffer.wrap(data);
    checksum = buffer.getInt();
    sequenceNumber = buffer.getInt();
    dataLength = buffer.getShort();
    this.data = Arrays.copyOfRange(buffer.array(), Constants.headerSize, Constants.headerSize + dataLength);
    if (sequenceNumber == 0 && isValid() && dataLength > 0) {
      numChunks = buffer.getInt();
      fileName = new String(this.data, 4, this.data.length - 4);
    }
  }

  public boolean isValid() {
    int sum = checksum;
    sum += sequenceNumber;
    sum += dataLength;
    for (byte b : data) {
      sum += b;
    }
    return sum == 0;
  }

  public boolean isMetadata() {
    return sequenceNumber == 0;
  }

  public int getNumChunks() {
    return numChunks;
  }

  public String getFileName() {
    return fileName;
  }

  public int getDataLength() {
    return dataLength;
  }

  public int getSequenceNumber() {
    return sequenceNumber;
  }

  public byte[] getData() {
    return data;
  }

  public byte[] getBytes() {
    ByteBuffer buffer = ByteBuffer.allocate(data.length + Constants.headerSize);
    buffer.putInt(checksum);
    buffer.putInt(sequenceNumber);
    buffer.putShort(dataLength);
    buffer.put(data, 0, dataLength);
    return buffer.array();
  }

  public int length() {
    return data.length + Constants.headerSize;
  }
}
