package server;
import java.sql.*;

public class UsersData implements AuthService {
    private static Connection connection;
    private static Statement statement;
    private static PreparedStatement preparedInsertStatement;
    private static PreparedStatement preparedUpdateStatement;
    private static PreparedStatement preparedStatementGetNickname;

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
        preparedStatementGetNickname = connection.prepareStatement("SELECT nickName FROM users WHERE name = ? AND password = ?;");
    }

    @Override
    public String getNicknameByLoginAndPassword(String login, String password) {
        try {
            preparedStatementGetNickname.setString(1, login);
            preparedStatementGetNickname.setString(2, password);
            ResultSet resultSet = preparedStatementGetNickname.executeQuery();

            if (resultSet.next()) {
                return resultSet.getString(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public boolean registration(String login, String password, String nickname) {
        try {
            preparedInsertStatement.setString(1, login);
            preparedInsertStatement.setString(2, nickname);
            preparedInsertStatement.setString(3, password);
            preparedInsertStatement.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
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
