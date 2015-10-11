import java.net.DatagramPacket;
import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * Created by Xinan on 4/10/15.
 */
public class Segment {
  private int checksum;
  private int sequenceNumber;
  private byte[] data;

  private long fileLength = -1;
  private String fileName = null;

  public Segment(int sequenceNumber, byte[] data) {
    this.sequenceNumber = sequenceNumber;
    this.data = data;
    checksum = -sequenceNumber;
    for (byte b : data) {
      checksum -= b;
    }
  }

  public Segment(DatagramPacket packet, boolean expectMeta) {
    byte[] data = packet.getData();
    ByteBuffer buffer = ByteBuffer.wrap(data);
    checksum = buffer.getInt();
    sequenceNumber = buffer.getInt();
    this.data = Arrays.copyOfRange(buffer.array(), Constants.headerSize, data.length);
    if (expectMeta && sequenceNumber == 0 && isValid()) {
      fileLength = buffer.getLong();
      fileName = new String(this.data, 12, buffer.getInt());
    }
  }

  public Segment(DatagramPacket packet) {
    this(packet, false);
  }

  public boolean isValid() {
    int sum = checksum;
    sum += sequenceNumber;
    for (byte b : data) {
      sum += b;
    }
    return sum == 0;
  }

  public boolean isMetadata() {
    return sequenceNumber == 0;
  }

  public long getFileLength() {
    return fileLength;
  }

  public String getFileName() {
    return fileName;
  }

  public int getDataLength() {
    return data.length;
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
    buffer.put(data, 0, data.length);
    return buffer.array();
  }

  public int length() {
    return data.length + Constants.headerSize;
  }
}
