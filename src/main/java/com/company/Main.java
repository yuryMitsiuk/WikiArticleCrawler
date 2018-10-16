package com.company;

import org.h2.tools.DeleteDbFiles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;

public class Main {

    private static Logger logger = LoggerFactory.getLogger(Main.class);

    private static final String DB_DRIVER = "org.h2.Driver";
    private static final String DB_CONNECTION = "jdbc:h2:~/test";
    private static final String DB_USER = "";
    private static final String DB_PASSWORD = "";


    public static void main(String[] args) {
        try {
            // delete the H2 database named 'test' in the user home directory
            DeleteDbFiles.execute("~", "test", true);
            createTable();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    /**
     * Creating table in H2 database.
     * @throws SQLException
     */
    private static void createTable() throws SQLException {
        Connection connection = getDBConnection();
        PreparedStatement createPreparedStatement = null;
        String CreateQuery = "CREATE TABLE WIKI(id int primary key, title varchar(255), imgpath varchar(255))";
        try {
            createPreparedStatement = connection.prepareStatement(CreateQuery);
            createPreparedStatement.executeUpdate();
            createPreparedStatement.close();
        } catch (SQLException e) {
            logger.error("Exception Message " + e.getLocalizedMessage());
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            connection.close();
        }
    }

    /**
     * Showing all records from WIKI table.
     * @throws SQLException
     */
    private static void showRecorsFromDB() throws SQLException {
        Connection connection = getDBConnection();
        PreparedStatement selectPreparedStatement = null;
        String SelectQuery = "select * from WIKI";
        try {
            selectPreparedStatement = connection.prepareStatement(SelectQuery);
            ResultSet rs = selectPreparedStatement.executeQuery();
            while (rs.next()) {
                logger.info("Id " + rs.getInt("id") +
                        " Title " + rs.getString("title") +
                        " Path to image " + rs.getString("imgpath"));
            }
            selectPreparedStatement.close();
        }catch (SQLException e) {
            logger.error("Exception Message " + e.getLocalizedMessage());
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            connection.close();
        }
    }

    /**
     * Getting a database connection.
     * @return dbConnection
     */
    private static Connection getDBConnection() {
        Connection dbConnection = null;
        try {
            Class.forName(DB_DRIVER);
        } catch (ClassNotFoundException e) {
            logger.error(e.getMessage());
        }
        try {
            dbConnection = DriverManager.getConnection(DB_CONNECTION, DB_USER,
                    DB_PASSWORD);
            return dbConnection;
        } catch (SQLException e) {
            logger.error(e.getMessage());
        }
        return dbConnection;
    }


}
