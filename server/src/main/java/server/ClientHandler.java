package server;

import constants.Command;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.concurrent.ExecutorService;
import java.util.logging.*;

public class ClientHandler {
    private Server server;
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;

    private boolean authenticated;
    private String nickname;
    private String login;
    private String password;
    private ExecutorService executorService;

    private static LogManager logManager = LogManager.getLogManager();
    private static Logger logger = Logger.getLogger(Server.class.getName());
    private static Handler fileHandler;

    static {
        try {
            logManager.readConfiguration(new FileInputStream("logging.properties"));
            fileHandler = new FileHandler("Logs/ClientHandlerLogs/log_ClientHandler_%g.log", 10 * 1024, 10, true);
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

    public ClientHandler(Server server, Socket socket) {
        try {
            this.server = server;
            this.socket = socket;
            this.executorService = server.getExecutorService();

            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());

            executorService.execute(() -> {
                try {
                    socket.setSoTimeout(120000);
                    //цикл аутентификации
                    while (true) {
                        String str = in.readUTF();

                        if (str.startsWith("/")) {
                            if (str.equals(Command.END)) {
                                sendMsg(Command.END);
                                break;
                            }

                            if (str.startsWith(Command.AUTH)) {
                                String[] token = str.split(" ", 3);
                                if (token.length < 3) {
                                    continue;
                                }
                                String newNick = server.getAuthService()
                                        .getNicknameByLoginAndPassword(token[1], token[2]);
                                login = token[1];
                                password = token[2];
                                if (newNick != null) {
                                    if (!server.isLoginAuthenticated(login)) {
                                        nickname = newNick;
                                        sendMsg(Command.AUTH_OK + " " + nickname);
                                        authenticated = true;
                                        server.subscribe(this);
                                        socket.setSoTimeout(0);
                                        break;
                                    } else {
                                        sendMsg("Учетная запись уже используется");
                                    }
                                } else {
                                    sendMsg("Логин / пароль не верны");
                                }
                            }

                            if (str.startsWith(Command.REG)) {
                                String[] token = str.split(" ");
                                if (token.length < 4) {
                                    continue;
                                }
                                if (server.getAuthService().registration(token[1], token[2], token[3])) {
                                    sendMsg(Command.REG_OK);
                                } else {
                                    sendMsg(Command.REG_NO);
                                }
                            }
                        }
                    }

                    //цикл работы
                    while (authenticated) {
                        String str = in.readUTF();

                        if (str.startsWith("/")) {
                            if (str.equals(Command.END)) {
                                sendMsg(Command.END);
                                break;
                            }
                            if (str.startsWith(Command.CHANGE_NICKNAME)) {
                                String[] token = str.split(" ", 2);
                                if (token.length < 2) {
                                    continue;
                                }
                                if (server.getAuthService().changeNickname(login, token[1], password)) {
                                    sendMsg("You changed nickname to " + token[1]);
                                    nickname = token[1];
                                    StringBuilder message = new StringBuilder("/clientlist ");
                                    for (int i = 0; i < server.getClients().size(); i++) {
                                        message.append(server.getClients().get(i).nickname).append(" ");
                                    }
                                    sendMsg(message.toString());
                                } else {
                                    sendMsg("The nickname " + token[1] + " is taken. Try another one");
                                }

                            }
                            if (str.startsWith("/w ")) {
                                String[] token = str.split(" ", 3);
                                if (token.length < 3) {
                                    continue;
                                }
                                server.privateMsg(this, token[1], token[2]);
                            }

                        } else {
                            server.broadcastMsg(this, str);
                        }
                    }

                } catch (SocketTimeoutException e) {
                    sendMsg(Command.END);
                } catch (IOException e) {
                    logger.log(Level.SEVERE, e.getMessage());
                } finally {
                    server.unsubscribe(this);
                    logger.log(Level.CONFIG, "Client disconnected");
                    try {
                        socket.close();
                    } catch (IOException e) {
                        logger.log(Level.SEVERE, e.getMessage());
                    }
                }

            });

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendMsg(String msg) {
        try {
            out.writeUTF(msg);
        } catch (IOException e) {
            logger.log(Level.SEVERE, e.getMessage());
        }
    }

    public String getNickname() {
        return nickname;
    }

    public String getLogin() {
        return login;
    }
}
