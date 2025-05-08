import java.net.*;
import java.io.*;

public class ClientNetworkHandler {
    private static final String SERVER_IP = "127.0.0.1";
    private static final int SERVER_PORT = 5000;
    private final DatagramSocket socket;
    private final InetAddress serverAddress;

    public ClientNetworkHandler(int port) throws SocketException, UnknownHostException {
        this.socket = new DatagramSocket(port);
        this.serverAddress = InetAddress.getByName(SERVER_IP);
        System.out.println("Client started on port " + socket.getLocalPort());
    }

    public void sendCommand(String command) throws IOException {
        byte[] data = command.getBytes();
        DatagramPacket packet = new DatagramPacket(data, data.length, serverAddress, SERVER_PORT);
        socket.send(packet);
    }

    public String receiveCommand() throws IOException {
        byte[] buffer = new byte[1024];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        socket.receive(packet);
        return new String(packet.getData(), 0, packet.getLength());
    }
}