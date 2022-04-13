package ai.everylink.chainscan.watcher.core.util;

import com.alibaba.druid.pool.DruidDataSourceFactory;
import com.google.common.collect.Maps;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.*;
import java.util.Map;

/**
 * Druid连接池的工具类
 */

public class JDBCUtils {

    // 1. 定义一个成员变量 DataSource
    private static DataSource dataSource;

    static {
        String url = WatcherUtils.dbUrl();
        String un = WatcherUtils.dbUserName();
        String pw = WatcherUtils.dbPassword();
        System.out.println("userName:" + un + "; pwd:" + pw + "; url:" + url);
        Map<String, String> map = Maps.newConcurrentMap();
        map.put("driverClassName", "com.mysql.cj.jdbc.Driver");
        map.put("url", url);
        map.put("username", un);
        map.put("password", pw);
        map.put("initialSize", "50");
        map.put("minIdle", "50");
        map.put("maxActive", "300");
        map.put("maxWait", "30000");

        // 2. 获取DataSource
        try {
            dataSource = DruidDataSourceFactory.createDataSource(map);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取连接的方法
     */
    public static Connection getConnection() throws SQLException {
        // 从连接池中取一个连接对象
        return dataSource.getConnection();
    }


    /**
     * 释放资源
     * 执行DML语句的时候需要关闭 statement 和 connection
     *
     * @param statement
     * @param connection
     */
    public static void close(Statement statement, Connection connection) {
        if (statement != null) {
            try {
                statement.close();
            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }
        }

        if (connection != null) {
            try {
                connection.close();      // 归还到连接池中
            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }
        }

    }

    /**
     * 释放资源
     * 执行DQL语句的时候需要关闭 resultSet statement 和 connection
     *
     * @param resultSet
     * @param statement
     * @param connection
     */
    public static void close(ResultSet resultSet, Statement statement, Connection connection) {
        if (resultSet != null) {
            try {
                resultSet.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        close(statement, connection);
    }

    /**
     * 获取连接池的方法
     */
    public static DataSource getDataSource() {
        return dataSource;
    }

    /**
     * 使用新的工具类
     */
    public static void main(String[] args) {
        /**
         * 完成添加的操作 给 accout 表添加一条记录
         */
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        try {
            // 1. 获取连接
            connection = JDBCUtils.getConnection();
            // 2. 定义sql
            String sql = "select max(block_number) from block";
            // 3. 获取
            preparedStatement = connection.prepareStatement(sql);
            // 4. 给？赋值
//            preparedStatement.setString(1,"小白白");
            // 执行sql，返回值是影响的行数
            ResultSet rs = preparedStatement.executeQuery();
            while(rs.next()) {
                System.out.println(rs.getLong(1));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }finally {
            // 6. 释放资源
            JDBCUtils.close(preparedStatement,connection);
        }
    }

}