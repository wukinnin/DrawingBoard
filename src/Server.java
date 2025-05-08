import java.io.*;
import java.net.*;
import java.util.*;

public class Server {
    private static final int PORT = 5000;
    private static DatagramSocket socket;
    private static List<InetSocketAddress> clients = new ArrayList<>();
    private static List<String> drawingCommands = new ArrayList<>();

    public static void main(String[] args) throws IOException {
        socket = new DatagramSocket(PORT);
        System.out.println("Server running on port " + PORT);

        byte[] buffer = new byte[1024];
        while (true) {
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            socket.receive(packet);

            InetSocketAddress clientAddress = new InetSocketAddress(packet.getAddress(), packet.getPort());
            if (!clients.contains(clientAddress)) {
                clients.add(clientAddress);
                System.out.println("New client connected: " + clientAddress);
                // Send existing drawing history to new client
                sendHistory(clientAddress);
            }

            String message = new String(packet.getData(), 0, packet.getLength());
            System.out.println("Received: " + message);
            
            if (message.equals("CLEAR")) {
                drawingCommands.clear();
            } else {
                drawingCommands.add(message);
            }
            
            broadcast(message, clientAddress);
        }
    }

    private static void sendHistory(InetSocketAddress clientAddress) throws IOException {
        for (String cmd : drawingCommands) {
            byte[] data = cmd.getBytes();
            DatagramPacket packet = new DatagramPacket(data, data.length, 
                                      clientAddress.getAddress(), clientAddress.getPort());
            socket.send(packet);
        }
    }

    private static void broadcast(String message, InetSocketAddress excludeAddress) throws IOException {
        for (InetSocketAddress client : clients) {
            if (!client.equals(excludeAddress)) {
                byte[] data = message.getBytes();
                DatagramPacket packet = new DatagramPacket(data, data.length, 
                                      client.getAddress(), client.getPort());
                socket.send(packet);
            }
        }
    }
}