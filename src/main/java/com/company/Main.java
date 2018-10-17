package com.company;

import org.h2.tools.DeleteDbFiles;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicInteger;

public class Main {

    private static Logger logger = LoggerFactory.getLogger(Main.class);

    private static AtomicInteger counter = new AtomicInteger(0);

    private static final String DB_DRIVER = "org.h2.Driver";
    private static final String DB_CONNECTION = "jdbc:h2:~/test";
    private static final String DB_USER = "";
    private static final String DB_PASSWORD = "";


    public static void main(String[] args) {

        Connection connection = null;

        try {
            // delete the H2 database named 'test' in the user home directory
            DeleteDbFiles.execute("~", "test", true);
            connection = getDBConnection();
            createTable(connection);
        } catch (SQLException e) {
            e.printStackTrace();
        }


            while (true) {
                showMenu();
                Scanner scanner = new Scanner(System.in);
                String input = scanner.nextLine();
                if ("1".equalsIgnoreCase(input)) {
                    String url = scanner.nextLine();
                    Document doc = null;
                    try {
                        doc = Jsoup.connect(url).get();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (IllegalArgumentException e) {
                        logger.error("Url not valid. Please, input correct url.");
                    }
                    if (doc != null) {
                        String title = getTitle(doc);
                        String pathImg = downloadImage(doc, title);
                        try {
                            insertData(connection, title, pathImg);
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }
                    }
                }
                if ("2".equalsIgnoreCase(input)) {
                    try {
                        showRecordsFromDB(connection);
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }
                if ("3".equalsIgnoreCase(input)) break;
            }

        if (connection != null) {
            try {
                connection.close();
                logger.info("Connection closed.");
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Show menu.
     */
    private static void showMenu() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Menu\n")
                .append("1 - Add url of the wikipedia's article page for parsing.\n")
                .append("2 - Show all records from db.\n")
                .append("3 - Close application.");
        System.out.println(stringBuilder.toString());
    }


    /**
     * Parsing url and get Title of article.
     * @param doc jsoup Document for parsing.
     * @return title of article.
     */
    private static String getTitle(Document doc) {
        Element title = doc.getElementById("firstHeading");
        logger.info(title.text());
        return title.text();
    }

    /**
     * Parsing url and download article's image.
     * @param doc - jsoup Document for parsing.
     * @param fileName - title of article, for save file in filesystem.
     * @return Absolute path of downloaded image.
     */
    private static String downloadImage(Document doc, String fileName) {
        Element imageUrl = doc.select("#mw-content-text").first().getElementsByClass("image").first().getElementsByTag("img").first();
        logger.info(imageUrl.absUrl("src"));

        try(InputStream in = new URL(imageUrl.absUrl("src")).openStream()){
            Path path = Paths.get(fileName + ".png");
            if (Files.exists(path))
                Files.deleteIfExists(path);
            Files.copy(in, path);
            logger.info(path.toAbsolutePath().toString());
            return path.toAbsolutePath().toString();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Creating table in H2 database.
     * @throws SQLException
     */
    private static void createTable(Connection connection) throws SQLException {
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
        }
    }

    /**
     * Showing all records from WIKI table.
     * @throws SQLException
     */
    private static void showRecordsFromDB(Connection connection) throws SQLException {
        boolean empty = true;
        PreparedStatement selectPreparedStatement = null;
        String SelectQuery = "select * from WIKI";
        try {
            selectPreparedStatement = connection.prepareStatement(SelectQuery);
            ResultSet rs = selectPreparedStatement.executeQuery();

            while (rs.next()) {
                logger.info("Id " + rs.getInt("id") +
                        " Title " + rs.getString("title") +
                        " Path to image " + rs.getString("imgpath"));
                empty = false;
            }
            if (empty) {
                logger.info("Database is empty.");
            }
            selectPreparedStatement.close();
        }catch (SQLException e) {
            logger.error("Exception Message " + e.getLocalizedMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Inserting parsed data in database.
     * @param title - title of article.
     * @param pathToImg - absolute path of downloaded image
     * @throws SQLException
     */
    private static void insertData(Connection connection, String title, String pathToImg) throws SQLException {
        PreparedStatement insertPreparedStatement = null;

        String InsertQuery = "INSERT INTO WIKI" + "(id, title, imgpath) values" + "(?,?,?)";
        try {
            connection.setAutoCommit(false);
            insertPreparedStatement = connection.prepareStatement(InsertQuery);
            insertPreparedStatement.setInt(1, counter.incrementAndGet());
            insertPreparedStatement.setString(2, title);
            insertPreparedStatement.setString(3, pathToImg);
            insertPreparedStatement.executeUpdate();
            insertPreparedStatement.close();
            connection.commit();
            logger.info("Data saved.");
        } catch (SQLException e) {
            logger.error("Exception Message " + e.getLocalizedMessage());
        } catch (Exception e) {
            e.printStackTrace();
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
            logger.error(e.getLocalizedMessage());
        }
        try {
            dbConnection = DriverManager.getConnection(DB_CONNECTION, DB_USER,
                    DB_PASSWORD);
            return dbConnection;
        } catch (SQLException e) {
            logger.error(e.getLocalizedMessage());
        }
        return dbConnection;
    }


}
