import java.io.File;
import java.io.RandomAccessFile;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketAddress;

/**
 * Created by Xinan on 4/10/15.
 */
public class FileReceiver {

  static DatagramSocket socket;
  static SocketAddress senderAddr;

  public static void main(String[] args) throws Exception {
    Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
    int port = Integer.parseInt(args[0]);

    socket = new DatagramSocket(port);

    File file;
    RandomAccessFile out;
    int numChunks;
    long fileLength;

    byte[] buffer = new byte[Constants.segmentSize];
    DatagramPacket packet = new DatagramPacket(buffer, Constants.segmentSize);
    Segment segment;
    boolean[] received;
    int index;

    while (true) {
      while (true) {
        socket.receive(packet);
        segment = new Segment(packet, true);
        if (segment.isMetadata() && segment.isValid()) {
          fileLength = segment.getFileLength();
          numChunks = (int) Math.ceil(1F * fileLength / Constants.chunkSize);
          senderAddr = packet.getSocketAddress();
          file = new File(segment.getFileName());
          file.getAbsoluteFile().getParentFile().mkdirs();
          out = new RandomAccessFile(file, "rw");
          sendAck(segment.getSequenceNumber());
          break;
        }
      }

      received = new boolean[numChunks];
      while (numChunks > 0) {
        socket.receive(packet);
        segment = new Segment(packet);
        if (segment.isValid()) {
          if (!segment.isMetadata()) {
            index = segment.getSequenceNumber() - 1;
            if (!received[index]) {
              out.seek(index * Constants.chunkSize);
              out.write(segment.getData(), 0, segment.getDataLength());
              received[index] = true;
              numChunks--;
            }
          }
          sendAck(segment.getSequenceNumber());
        }
      }
      out.setLength(fileLength);
      for (int i = 0; i < 100; i++) {
        sendAck(-1);
      }
      out.close();
    }
  }

  private static void sendAck(int sequenceNumber) throws Exception {
    Segment segment = new Segment(sequenceNumber, new byte[0]);
    DatagramPacket packet = new DatagramPacket(segment.getBytes(), segment.length(), senderAddr);
    socket.send(packet);
  }
}
