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
    String fileName;
    int numChunks;

    byte[] data;
    byte[] buffer = new byte[Constants.segmentSize];
    DatagramPacket packet = new DatagramPacket(buffer, Constants.segmentSize);
    Segment segment;
    while (true) {
      socket.receive(packet);
      segment = new Segment(packet);
      if (segment.isMetadata() && segment.isValid()) {
        fileName = segment.getFileName();
        numChunks = segment.getNumChunks();
        senderAddr = packet.getSocketAddress();
        file = new File(fileName);
        file.getParentFile().mkdirs();
        out = new RandomAccessFile(file, "rw");
        sendAck(segment.getSequenceNumber());
        break;
      }
    }

    boolean[] received = new boolean[numChunks];
    int index, duplicated = 0;
    while (numChunks > 0) {
      socket.receive(packet);
      segment = new Segment(packet);
      if (segment.isValid()) {
        if (!segment.isMetadata()) {
//          System.out.println(segment.getSequenceNumber() + " get!");
          data = segment.getData();
          index = segment.getSequenceNumber() - 1;
          out.seek(index * Constants.chunkSize);
          out.write(data, 0, segment.getDataLength());
          if (!received[index]) {
            received[index] = true;
            numChunks--;
          } else {
            duplicated++;
          }
        }
        sendAck(segment.getSequenceNumber());
      }
    }
    for (int i = 0; i < 100; i++) {
      sendAck(-1);
    }
    System.out.println(duplicated + " duplicated!");
    out.close();
    socket.close();
  }

  private static void sendAck(int sequenceNumber) throws Exception {
    Segment segment = new Segment(sequenceNumber, new byte[0]);
    DatagramPacket packet = new DatagramPacket(segment.getBytes(), segment.length(), senderAddr);
    socket.send(packet);
//    System.out.println(segment.getSequenceNumber() + " ack sent!");
  }
}
