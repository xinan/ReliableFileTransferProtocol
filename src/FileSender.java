import java.io.File;
import java.io.RandomAccessFile;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * Created by Xinan on 4/10/15.
 */
public class FileSender {

  public static void main(String[] args) throws Exception {
    StopWatch sw = new StopWatch();
    sw.start();
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
    int bytesRead;

    while (!received[0]) {
      segment = getMetadata(numChunks, dest);
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
        bytesRead = in.read(buffer, 0, Constants.chunkSize);
        segment = new Segment(i, Arrays.copyOfRange(buffer, 0, bytesRead));
        packet = new DatagramPacket(segment.getBytes(), segment.length(), receiverAddr);
        socket.send(packet);
      }
    }
    in.close();
    socket.close();

    sw.stop();
    System.out.println(sw.getTime());
  }

  public static Segment getMetadata(int numChunks, String fileName) {
    ByteBuffer buffer = ByteBuffer.allocate(fileName.length() + 4);
    buffer.putInt(numChunks);
    buffer.put(fileName.getBytes());
    return new Segment(0, buffer.array());
  }
}
