package me.andarguy.lightperm;

import andarguy.me.databasesupport.DatabaseSupport;
import org.apache.commons.lang.StringEscapeUtils;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;

public class PermissionDatabase {
    public static final String DB_FILE_NAME = "permissions";

    private Connection connection;
    private Statement statement;

    public PermissionDatabase() {
        boolean isFirstConnection = !DatabaseSupport.isDatabaseFileExists(LightPerm.getInstance().getDataFolder().getPath(), DB_FILE_NAME);
        try {
            this.connection = DatabaseSupport.connect(LightPerm.getInstance().getDataFolder().getPath(), DB_FILE_NAME);
            this.statement = this.connection.createStatement();
        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
        }
        if (isFirstConnection) createDB();
    }

    private void createDB() {
        try {
            this.statement.execute("CREATE TABLE groups (player TEXT PRIMARY KEY, player_group TEXT)");
        } catch (SQLException exception) {
            exception.printStackTrace();
        }
    }

    public boolean isPlayerExist(String playerName) {
        playerName = StringEscapeUtils.escapeSql(playerName);
        try {
            ResultSet result = this.statement.executeQuery(String.format("SELECT * FROM groups WHERE player=\"%s\"", playerName));
            return result.next();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        return false;
    }

    public String getPlayerGroup(String playerName) {
        playerName = StringEscapeUtils.escapeSql(playerName);
        try {
            ResultSet result = this.statement.executeQuery(String.format("SELECT * FROM groups WHERE player=\"%s\"", playerName));
            return result.getString("player_group");
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        return "";
    }

    public boolean setPlayerGroup(String playerName, String group) {
        playerName = StringEscapeUtils.escapeSql(playerName);
        group = StringEscapeUtils.escapeSql(group);

        try {
            return this.statement.execute(String.format("INSERT OR REPLACE INTO groups (player, player_group) VALUES (\"%s\", \"%s\")", playerName, group));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}
