import java.net.DatagramPacket;
import java.net.DatagramSocket;

/**
 * Created by Xinan on 4/10/15.
 */
public class ResponseHandlingThread extends Thread {
  private final boolean[] received;
  private int numChunks;
  private DatagramSocket socket;

  public ResponseHandlingThread(boolean[] received, int numChunks, DatagramSocket socket) {
    this.received = received;
    this.numChunks = numChunks;
    this.socket = socket;
  }

  public void run() {
    byte[] buffer = new byte[Constants.headerSize];
    Segment segment;
    DatagramPacket packet = new DatagramPacket(buffer, Constants.headerSize);
    while (numChunks >= 0) {
      try {
        socket.receive(packet);
      } catch (Exception e) {
        e.printStackTrace();
      }
      segment = new Segment(packet);
      if (segment.isValid() && segment.getSequenceNumber() == -1) {
        for (int i = 0; i < received.length; i++) {
          received[i] = true;
        }
        return;
      }
      if (segment.isValid() && !received[segment.getSequenceNumber()]) {
        received[segment.getSequenceNumber()] = true;
        numChunks--;
      }
    }
  }
}
