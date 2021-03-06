package server;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class SimpleAuthService implements AuthService {
    private class UserData {
        String login;
        String password;
        String nickname;

        public UserData(String login, String password, String nickname) {
            this.login = login;
            this.password = password;
            this.nickname = nickname;
        }
    }

    private List<UserData> users;

    public SimpleAuthService() {
        users = new ArrayList<>();

        for (int i = 0; i < 9; i++) {
            users.add(new UserData("user" + i, "pass" + i, "nick" + i));
        }

    }

    @Override
    public String getNicknameByLoginAndPassword(String login, String password) {
        System.out.println("SimpleAuthService");
        for (UserData u : users) {
            if (u.login.equals(login) && u.password.equals(password)) {
                return u.nickname;
            }
        }

        return null;
    }

    @Override
    public boolean registration(String login, String password, String nickname) {
        for (UserData u : users) {
            if (u.login.equals(login) || u.nickname.equals(nickname)) {
                return false;
            }
        }
        users.add(new UserData(login, password, nickname));
        return true;
    }

    @Override
    public boolean changeNickname(String login, String password, String nickname) {
        return false;
    }
}
