import java.io.*;
import java.net.*;
import java.util.concurrent.ConcurrentHashMap;

public class ChatServer {
    private static final int PORT = 12345;
    private static final ConcurrentHashMap<String, ClientHandler> activeClients = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT, 50, InetAddress.getByName("0.0.0.0"))) {
            System.out.println("Server started on port " + PORT);

            while (true) {
                Socket socket = serverSocket.accept();
                new Thread(new ClientHandler(socket)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static class ClientHandler implements Runnable {
        private Socket socket;
        private PrintWriter out;
        private BufferedReader in;
        private String username;

        public ClientHandler(Socket socket) {
            this.socket = socket;
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            try {
                out.println("Please enter your username:");
                username = in.readLine();

                if (username != null && !username.isEmpty()) {
                    activeClients.put(username, this);
                    out.println("Welcome, " + username + "!");
                    String message;

                    while ((message = in.readLine()) != null) {
                        handleMessage(message);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (username != null) {
                    activeClients.remove(username);
                    System.out.println(username + " disconnected.");
                }
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        private void handleMessage(String message) {
            String[] parts = message.split(":", 2);
            if (parts.length == 2) {
                String recipientUsername = parts[0].trim();
                String msgContent = parts[1].trim();

                if (activeClients.containsKey(recipientUsername)) {
                    activeClients.get(recipientUsername).sendMessage(username + ": " + msgContent);
                    out.println("Message sent to " + recipientUsername);
                } else {
                    out.println("User " + recipientUsername + " is not online.");
                }
            } else {
                out.println("Invalid message format.");
            }
        }

        public void sendMessage(String msg) {
            out.println(msg);
        }
    }
}
