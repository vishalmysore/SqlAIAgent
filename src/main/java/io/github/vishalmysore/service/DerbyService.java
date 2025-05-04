package io.github.vishalmysore.service;

import com.t4a.annotations.Action;
import com.t4a.annotations.Agent;
import com.t4a.detect.ActionCallback;
import io.github.vishalmysore.a2a.domain.Task;
import io.github.vishalmysore.a2a.domain.TaskState;
import io.github.vishalmysore.common.CallBackType;
import io.github.vishalmysore.data.ColumnData;
import io.github.vishalmysore.data.RowData;
import io.github.vishalmysore.data.TableData;
import io.github.vishalmysore.data.User;

import io.github.vishalmysore.mcp.domain.CallToolResult;
import lombok.extern.java.Log;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Log
@Service
@Agent(groupName = "Database related actions")
public class DerbyService {

    private static final String JDBC_URL = "jdbc:derby:memory:myDB;create=true";
    private static final String JDBC_DRIVER = "org.apache.derby.jdbc.ClientDriver";

    /**
     * This gets automatically injected by the framework you dont need to pass anything
     */
    private ActionCallback callback;
    @Action(description = "start database server")
    public String startServer(String serverName) {
        // Start the Derby server


            log.info("Derby server started.");
            return "Derby server started. for " + serverName;

    }

    /**
     * Callbacks are avaiable here , in case of MCP you have MCP call back or else a2a callback
     * Type of callback can be sseemiter, notification , status etc
     * @param databaseName
     * @return
     */
    @Action(description = "Create database")
    public String createDatabase(String databaseName) {
        if(callback.getType().equals(CallBackType.A2A.name())) {
            ((Task) callback.getContext()).setDetailedAndMessage(TaskState.COMPLETED, "Creating database: " + databaseName);
        }
        if(callback.getType().equals(CallBackType.MCP.name())) {
            //You can set to custom type of values if needed
            ((CallToolResult) callback.getContext()).getContent().get(0);
        }
        // Create a database
        try (Connection conn = DriverManager.getConnection(JDBC_URL)) {
            if (conn != null) {
                return "Database created successfully.";
            } else {
                return "Failed to create database.";
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return "Failed to create database.";
    }

    @Action(description = "Create tables")
    public String createTables(TableData tableData) {
        //this is just for demo purpose only to show the workings of AI Agents and not a production ready code
        // Sanitize all input here , validate the table names and columnTypes before executing the query .
        //https://happycoding.io/tutorials/java-server/sanitizing-user-input
        StringBuilder createTableSQL = new StringBuilder("CREATE TABLE ");
        createTableSQL.append(tableData.getTableName()).append(" (");

        for (ColumnData column : tableData.getHeaderList()) {
            createTableSQL.append(column.getColumnName())
                    .append(" ")
                    .append(column.getSqlColumnType())
                    .append(", ");
        }

        // Remove the last comma and space
        createTableSQL.setLength(createTableSQL.length() - 2);
        createTableSQL.append(")");
        log.info("Create table SQL: " + createTableSQL);
        try (Connection conn = DriverManager.getConnection(JDBC_URL);
             Statement stmt = conn.createStatement()) {
            stmt.execute(createTableSQL.toString());
            return tableData.getTableName() + " table created successfully.";
        } catch (SQLException e) {
            e.printStackTrace();
            return "Error creating table: " + e.getMessage();
        }
    }


    @Action(description = "Insert new data in database table")
    public String insertDataInTable(TableData tableData) {
        StringBuilder insertSQL = new StringBuilder("INSERT INTO ");
        insertSQL.append(tableData.getTableName()).append(" (");

        // Append column names
        List<ColumnData> columns = tableData.getRowDataList().get(0).getColumnDataList();
        for (ColumnData column : columns) {
            insertSQL.append(column.getColumnName()).append(", ");
        }

        // Remove the last comma and space
        insertSQL.setLength(insertSQL.length() - 2);
        insertSQL.append(") VALUES (");

        // Append placeholders for values
        for (int i = 0; i < columns.size(); i++) {
            insertSQL.append("?, ");
        }

        // Remove the last comma and space
        insertSQL.setLength(insertSQL.length() - 2);
        insertSQL.append(")");

        try (Connection conn = DriverManager.getConnection(JDBC_URL);
             PreparedStatement pstmt = conn.prepareStatement(insertSQL.toString())) {

            for (RowData row : tableData.getRowDataList()) {
                int index = 1;
                for (ColumnData column : row.getColumnDataList()) {
                    pstmt.setObject(index++, column.getColumnValue());
                }
                pstmt.addBatch();
            }

            pstmt.executeBatch();
            return "Data inserted successfully into " + tableData.getTableName();
        } catch (SQLException e) {
            e.printStackTrace();
            return "Error inserting data: " + e.getMessage();
        }
    }

    @Action(description = "Retrieve data from table")
    public List<Map<String, Object>> retrieveData(String sqlSelectQuery) {
        List<Map<String, Object>> result = new ArrayList<>();


        List<Map<String, Object>> users = new ArrayList<>();

        try (Connection conn = DriverManager.getConnection(JDBC_URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sqlSelectQuery)) {

            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();

            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                    String columnName = metaData.getColumnName(i);
                    Object columnValue = rs.getObject(i);
                    row.put(columnName, columnValue);
                }
                while (rs.next()) {
                    result.add(row);
                }
                log.info("Data retrieved successfully.");

            }
            return result;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }


}