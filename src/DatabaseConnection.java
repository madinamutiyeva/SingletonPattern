import java.io.FileInputStream;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class DatabaseConnection {
    private static volatile DatabaseConnection instance;
    private Connection connection;
    private String url;
    private String username;
    private String password;

    private DatabaseConnection(String configFilePath) {
        loadConfiguration(configFilePath);
        establishConnection();
    }

    public static DatabaseConnection getInstance(String configFilePath) {
        if (instance == null) {
            synchronized (DatabaseConnection.class) {
                if (instance == null) {
                    instance = new DatabaseConnection(configFilePath);
                }
            }
        }
        return instance;
    }

    private void loadConfiguration(String configFilePath) {
        try (FileInputStream fis = new FileInputStream(configFilePath)) {
            Properties properties = new Properties();
            properties.load(fis);
            url = properties.getProperty("url");
            username = properties.getProperty("username");
            password = properties.getProperty("password");
        } catch (IOException e) {
            throw new RuntimeException("Error loading configuration file", e);
        }
    }

    private void establishConnection() {
        try {
            connection = DriverManager.getConnection(url, username, password);
        } catch (SQLException e) {
            throw new RuntimeException("Error establishing database connection", e);
        }
    }

    public Connection getConnection() {
        return connection;
    }

    public void closeConnection() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
    public ResultSet executeQuery(String query) {
        try {
            Statement statement = connection.createStatement();
            return statement.executeQuery(query);
        } catch (SQLException e) {
            throw new RuntimeException("Error executing query: " + query, e);
        }
    }

    public int executeUpdate(String query) {
        try {
            Statement statement = connection.createStatement();
            return statement.executeUpdate(query);
        } catch (SQLException e) {
            throw new RuntimeException("Error executing update: " + query, e);
        }
    }

    public List<Object[]> executeQueryAndReturnRows(String query) {
        List<Object[]> rows = new ArrayList<>();
        try {
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery(query);
            ResultSetMetaData metaData = resultSet.getMetaData();
            int columnCount = metaData.getColumnCount();

            while (resultSet.next()) {
                Object[] row = new Object[columnCount];
                for (int i = 1; i <= columnCount; i++) {
                    row[i - 1] = resultSet.getObject(i);
                }
                rows.add(row);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error executing query and returning rows: " + query, e);
        }
        return rows;
    }

    public void closeResultSet(ResultSet resultSet) {
        if (resultSet != null) {
            try {
                resultSet.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public void closeStatement(Statement statement) {
        if (statement != null) {
            try {
                statement.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }


    public static void main(String[] args) {
        DatabaseConnection dbConnection = DatabaseConnection.getInstance("src/database_config.properties");
        Connection connection = dbConnection.getConnection();
        if (connection != null) {
            System.out.println("Connection to the database established successfully.");
            String selectQuery = "SELECT * FROM users";
            ResultSet resultSet = dbConnection.executeQuery(selectQuery);
            try {
                while (resultSet.next()) {
                    int id = resultSet.getInt("id");
                    String username = resultSet.getString("username");
                    String password = resultSet.getString("password");
                    System.out.println("User: " + id + ", " + username + ", " + password);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            } finally {
                dbConnection.closeResultSet(resultSet);
            }
            dbConnection.closeConnection();
        } else {
            System.out.println("Failed to establish connection to the database.");
        }
    }

}
