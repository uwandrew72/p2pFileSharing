package org.andrew.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ConnectionManager {

    private final String driverName = "com.mysql.cj.jdbc.Driver";
    // replace "ptopnodes" with your own database name
    private final String connectionUrl = "jdbc:mysql://localhost:3306/ptopnodes";
    // replace with your own username and password
    private final String userName = System.getenv("UserID");
    private final String userPass = System.getenv("Password");

    private Connection con = null;

    public ConnectionManager() {
        try {
            Class.forName(driverName);
        } catch (ClassNotFoundException e) {
            System.out.println(e.toString());
        }
    }

    public Connection createConnection() {
        try {
            con = DriverManager.getConnection(connectionUrl, userName, userPass);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return con;
    }

    public void closeConnection() {
        try {
            this.con.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
