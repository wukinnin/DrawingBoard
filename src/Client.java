import java.io.*;
import java.net.*;
import java.util.*;

public class Client {
    private static final String SERVER_IP = "127.0.0.1";
    private static final int SERVER_PORT = 5000;
    private static DatagramSocket socket;
    private static InetAddress serverAddress;
    private static int clientPort;

    public static void main(String[] args) throws IOException {
        // Use random available port for client
        socket = new DatagramSocket();
        clientPort = socket.getLocalPort();
        serverAddress = InetAddress.getByName(SERVER_IP);
        
        System.out.println("Client running on port " + clientPort);

        // Thread for receiving server broadcasts
        new Thread(() -> {
            byte[] buffer = new byte[1024];
            while (true) {
                try {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    socket.receive(packet);
                    String message = new String(packet.getData(), 0, packet.getLength());
                    System.out.println("Received from server: " + message);
                    // Here we'll later add GUI updates
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();

        // For testing - send sample commands
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        while (true) {
            System.out.print("Enter command (LINE/CIRCLE/RECT/CLEAR): ");
            String command = reader.readLine();
            sendCommand(command);
        }
    }

    public static void sendCommand(String command) throws IOException {
        byte[] data = command.getBytes();
        DatagramPacket packet = new DatagramPacket(data, data.length, serverAddress, SERVER_PORT);
        socket.send(packet);
    }
}