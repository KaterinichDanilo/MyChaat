package server;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.*;

public class Server {
    private ServerSocket server;
    private Socket socket;
    private final int PORT = 30000;
    private ExecutorService executorService = Executors.newCachedThreadPool();
    private static LogManager logManager = LogManager.getLogManager();
    private static Logger logger = Logger.getLogger(Server.class.getName());
    private static Handler fileHandler;

    static {
        try {
            logManager.readConfiguration(new FileInputStream("logging.properties"));
            fileHandler = new FileHandler("Logs/ServerLogs/log_Server_%g.log", 10 * 1024, 10, true);
            fileHandler.setFormatter(new Formatter() {
            @Override
            public String format(LogRecord r) {
                return String.format(">>>>> %s LVL: %s \n",r.getMessage(), r.getThreadID());
            }
        });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private List<ClientHandler> clients;
    private AuthService authService;

    public ExecutorService getExecutorService() {
        return executorService;
    }

    public Server() {
        clients = new CopyOnWriteArrayList<>();
        authService = new UsersData();

        logger.addHandler(fileHandler);

        try {
            server = new ServerSocket(PORT);
            UsersData.connect();
            logger.log(Level.FINE, "Server started!");
            logger.log(Level.FINE, "Database connected!");

            while (true) {
                socket = server.accept();
                logger.log(Level.CONFIG, "Client connected");
                new ClientHandler(this, socket);
            }

        } catch (Exception e) {
            logger.log(Level.SEVERE, e.getMessage());
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                logger.log(Level.SEVERE, e.getMessage());
            }
            try {
                server.close();
            } catch (IOException e) {
                logger.log(Level.SEVERE, e.getMessage());
            }
        }
    }

    public List<ClientHandler> getClients() {
        return clients;
    }

    public void broadcastMsg(ClientHandler sender, String msg) {
        String message = String.format("[ %s ]: %s", sender.getNickname(), msg);

        for (ClientHandler c : clients) {
            c.sendMsg(message);
        }
    }

    public void privateMsg(ClientHandler sender, String receiver, String msg) {
        String message = String.format("[ %s ] to [ %s ]: %s", sender.getNickname(), receiver, msg);

        for (ClientHandler c : clients) {
            if (c.getNickname().equals(receiver)) {
                c.sendMsg(message);
                if (!sender.getNickname().equals(receiver)) {
                    sender.sendMsg(message);
                }
                return;
            }
        }

        sender.sendMsg("not found user: " + receiver);
    }

    public boolean isLoginAuthenticated(String login) {
        for (ClientHandler c : clients) {
            if (c.getLogin().equals(login)) {
                return true;
            }
        }
        return false;
    }

    public void broadcastClientList() {
        StringBuilder sb = new StringBuilder("/clientlist");

        for (ClientHandler c : clients) {
            sb.append(" ").append(c.getNickname());
        }

        String msg = sb.toString();

        for (ClientHandler c : clients) {
            c.sendMsg(msg);
        }
    }

    public void subscribe(ClientHandler clientHandler) {
        clients.add(clientHandler);
        broadcastClientList();
    }

    public void unsubscribe(ClientHandler clientHandler) {
        clients.remove(clientHandler);
        broadcastClientList();
    }

    public AuthService getAuthService() {
        return authService;
    }
}
