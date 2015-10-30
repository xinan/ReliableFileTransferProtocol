import java.io.File;
import java.io.RandomAccessFile;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;

/**
 * Created by Xinan on 4/10/15.
 */
public class FileSender {

  public static void main(String[] args) throws Exception {
    Thread.currentThread().setPriority(Thread.MAX_PRIORITY);

    String host = args[0];
    int port = Integer.parseInt(args[1]);
    String src = args[2];
    String dest = args[3];

    SocketAddress receiverAddr = new InetSocketAddress(host, port);
    DatagramSocket socket = new DatagramSocket();
    DatagramPacket packet;

    RandomAccessFile in = new RandomAccessFile(new File(src), "r");

    int numChunks = (int) Math.ceil(1F * in.length() / Constants.chunkSize);

    boolean[] received = new boolean[numChunks + 1];
    ResponseHandlingThread responseHandlingThread = (new ResponseHandlingThread(received, numChunks, socket));
    responseHandlingThread.setPriority(Thread.MAX_PRIORITY);
    responseHandlingThread.start();

    boolean completed = false;
    Segment segment;
    byte[] buffer = new byte[Constants.chunkSize];

    while (!received[0]) {
      segment = getMetadata(in.length(), dest);
      packet = new DatagramPacket(segment.getBytes(), segment.length(), receiverAddr);
      socket.send(packet);
    }

    while (!completed) {
      completed = true;
      for (int i = 1; i <= numChunks; i++) {
        if (received[i]) {
          continue;
        }
        completed = false;
        in.seek((i - 1) * Constants.chunkSize);
        in.read(buffer, 0, Constants.chunkSize);
        segment = new Segment(i, buffer);
        packet = new DatagramPacket(segment.getBytes(), segment.length(), receiverAddr);
        socket.send(packet);
      }
    }
    in.close();
    socket.close();
  }

  public static Segment getMetadata(long fileLength, String fileName) {
    ByteBuffer buffer = ByteBuffer.allocate(Constants.chunkSize);
    buffer.putLong(fileLength);
    buffer.putInt(fileName.length());
    buffer.put(fileName.getBytes());
    return new Segment(0, buffer.array());
  }
}
