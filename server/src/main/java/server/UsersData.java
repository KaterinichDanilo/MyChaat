package server;
import java.sql.*;

public class UsersData implements AuthService {
    private static Connection connection;
    private static Statement statement;
    private static PreparedStatement preparedInsertStatement;
    private static PreparedStatement preparedUpdateStatement;

    public UsersData() {
        try {
            connect();
            prepareStatement();
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void connect() throws Exception{
        Class.forName("org.sqlite.JDBC");
        connection = DriverManager.getConnection("jdbc:sqlite:usersData.db");
        statement = connection.createStatement();
    }

    public static void disconnection() {
        try {
            statement.close();
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void prepareStatement() throws SQLException {
        preparedInsertStatement = connection.prepareStatement("INSERT INTO users (name, nickName, password) VALUES (?, ?, ?);");
        preparedUpdateStatement = connection.prepareStatement("UPDATE users SET nickName = ? WHERE name == ?;");
    }

    @Override
    public String getNicknameByLoginAndPassword(String login, String password) {
        try {
            ResultSet resultSet = statement.executeQuery("SELECT name, nickName, password FROM users;");

            while (resultSet.next()) {
                if (resultSet.getString("name").equals(login) && resultSet.getString("password").equals(password)) {
                    System.out.println(resultSet.getString("nickName"));
                    return resultSet.getString("nickName");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public boolean registration(String login, String password, String nickname) {
        try {
            ResultSet resultSet = statement.executeQuery("SELECT name, nickName, password FROM users;");

            while (resultSet.next()) {
                if (resultSet.getString("name").equals(login) || resultSet.getString("nickName").equals(nickname)) {
                    return false;
                }
            }

            preparedInsertStatement.setString(1, login);
            preparedInsertStatement.setString(2, nickname);
            preparedInsertStatement.setString(3, password);
            preparedInsertStatement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return true;
    }

    @Override
    public boolean changeNickname(String login, String nickName, String password) {
        boolean taken = false;
        try {
            ResultSet resultSet = statement.executeQuery("SELECT nickName FROM users;");

            while (resultSet.next()) {
                if (resultSet.getString("nickName").equals(nickName)) {
                    taken = true;
                }
            }
            if (!taken) {
                preparedUpdateStatement.setString(1, nickName);
                preparedUpdateStatement.setString(2, login);
                preparedUpdateStatement.executeUpdate();
                return true;
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        return false;
    }

}
