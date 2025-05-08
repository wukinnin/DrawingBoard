import java.net.*;
import java.io.*;

public class ClientNetworkHandler {
    private static final String SERVER_IP = "127.0.0.1";
    private static final int SERVER_PORT = 5000;
    private DatagramSocket socket;
    private InetAddress serverAddress;

    public ClientNetworkHandler(int port) {
        try {
            if (port == 0) {
                socket = new DatagramSocket();
            } else {
                socket = new DatagramSocket(port);
            }
            serverAddress = InetAddress.getByName(SERVER_IP);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sendCommand(String command) {
        try {
            byte[] data = command.getBytes();
            DatagramPacket packet = new DatagramPacket(data, data.length, serverAddress, SERVER_PORT);
            socket.send(packet);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String receiveCommand() {
        try {
            byte[] buffer = new byte[1024];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            socket.receive(packet);
            return new String(packet.getData(), 0, packet.getLength());
        } catch (Exception e) {
            return null;
        }
    }
}