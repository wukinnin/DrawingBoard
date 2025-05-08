import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.io.IOException;

public class Server {
    private static final int PORT = 5000;
    private static DatagramSocket socket;
    private static final List<InetSocketAddress> clients = new ArrayList<>();
    private static final List<String> drawingHistory = new ArrayList<>();

    public static void main(String[] args) {
        try {
            socket = new DatagramSocket(PORT);
            System.out.println("Server running on port " + PORT);

            byte[] buffer = new byte[1024];
            while (true) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);

                InetSocketAddress clientAddress = new InetSocketAddress(
                    packet.getAddress(), packet.getPort());
                
                if (!clients.contains(clientAddress)) {
                    clients.add(clientAddress);
                    System.out.println("New client connected: " + clientAddress);
                    sendHistory(clientAddress);
                }

                String message = new String(packet.getData(), 0, packet.getLength());
                System.out.println("Received: " + message);
                
                if (message.equals("CLEAR")) {
                    drawingHistory.clear();
                } else {
                    drawingHistory.add(message);
                }
                
                broadcast(message, clientAddress);
            }
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
        }
    }

    private static void sendHistory(InetSocketAddress client) throws IOException {
        for (String cmd : drawingHistory) {
            byte[] data = cmd.getBytes();
            DatagramPacket packet = new DatagramPacket(
                data, data.length, client.getAddress(), client.getPort());
            socket.send(packet);
        }
    }

    private static void broadcast(String message, InetSocketAddress exclude) throws IOException {
        for (InetSocketAddress client : clients) {
            if (!client.equals(exclude)) {
                byte[] data = message.getBytes();
                DatagramPacket packet = new DatagramPacket(
                    data, data.length, client.getAddress(), client.getPort());
                socket.send(packet);
            }
        }
    }
}