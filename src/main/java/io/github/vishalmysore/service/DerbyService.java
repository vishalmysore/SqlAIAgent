package io.github.vishalmysore.service;

import com.opencsv.CSVWriter;
import com.t4a.annotations.Action;
import com.t4a.annotations.Agent;
import com.t4a.detect.ActionCallback;
import io.github.vishalmysore.a2a.domain.Task;
import io.github.vishalmysore.a2a.domain.TaskState;

import io.github.vishalmysore.data.ColumnData;
import io.github.vishalmysore.data.RowData;
import io.github.vishalmysore.data.TableData;
import io.github.vishalmysore.data.User;

import io.github.vishalmysore.mcp.domain.BlobResourceContents;
import io.github.vishalmysore.mcp.domain.CallToolResult;
import io.github.vishalmysore.mcp.domain.EmbeddedResource;
import io.github.vishalmysore.mcp.domain.TextResourceContents;
import lombok.extern.java.Log;


import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.*;
import java.util.*;

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

    @PreAuthorize("hasRole('USER')")
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
    @PreAuthorize("hasRole('ADMIN')")
    @Action(description = "Create database")
    public String createDatabase(String databaseName) {

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
    @PreAuthorize("hasRole('USER')")
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

    @Action(description = "Retrieve data and return as embedded file resource")
    public EmbeddedResource retrieveDataAsFile(String sqlSelectQuery, String fileName) {
        EmbeddedResource embedded = new EmbeddedResource();
        BlobResourceContents blobContents = new BlobResourceContents();

        File tempFile = null;
        try {
            tempFile = File.createTempFile(fileName, ".csv");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        try (Connection conn = DriverManager.getConnection(JDBC_URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sqlSelectQuery);
             FileWriter writer = new FileWriter(tempFile);
             CSVWriter csvWriter = new CSVWriter(writer)) {

            // Write headers
            ResultSetMetaData metaData = rs.getMetaData();
            String[] headers = new String[metaData.getColumnCount()];
            for (int i = 1; i <= metaData.getColumnCount(); i++) {
                headers[i-1] = metaData.getColumnName(i);
            }
            csvWriter.writeNext(headers);

            // Write data rows
            while (rs.next()) {
                String[] row = new String[metaData.getColumnCount()];
                for (int i = 1; i <= metaData.getColumnCount(); i++) {
                    row[i-1] = rs.getString(i);
                }
                csvWriter.writeNext(row);
            }

            // Convert file to Base64
            byte[] fileContent = Files.readAllBytes(tempFile.toPath());
            String base64Content = Base64.getEncoder().encodeToString(fileContent);

            // Set blob contents
            blobContents.setMimeType("text/csv");
            blobContents.setBlob(base64Content);
           // blobContents.setName(fileName + ".csv");

            // Set embedded resource
            embedded.setResource(blobContents);

            // Cleanup temp file
            tempFile.delete();
            return embedded;

        } catch (Exception e) {
            log.warning("Error creating embedded resource: " + e.getMessage());
            throw new RuntimeException("Failed to create embedded resource", e);
        }
    }

    @Action(description = "Return grocery data as embedded file resource")
    public EmbeddedResource getGroceryItemsInFile(String fileName) {
        EmbeddedResource embedded = new EmbeddedResource();
        TextResourceContents blobContents = new TextResourceContents();

        // Create mock CSV data
        String mockData = "id,name,value\n" +
                "1,item1,100\n" +
                "2,item2,200\n" +
                "3,item3,300\n";

        // Convert string to Base64
        //String base64Content = Base64.getEncoder().encodeToString(mockData.getBytes());

        // Set blob contents
        blobContents.setMimeType("text/csv");
        blobContents.setText(mockData);

        // Set embedded resource
        embedded.setResource(blobContents);

        return embedded;
    }

    @Action(description = "Return grocery data as embedded PDF file resource")
    public EmbeddedResource getGroceryItemsAsPDF(String fileName) {
        EmbeddedResource embedded = new EmbeddedResource();
        BlobResourceContents blobContents = new BlobResourceContents();

        // This is a minimal valid PDF content in base64
        String pdfBase64 = "JVBERi0xLjcKCjEgMCBvYmogICUgZW50cnkgcG9pbnQKPDwKICAvVHlwZSAvQ2F0YWxvZwog" +
                "IC9QYWdlcyAyIDAgUgo+PgplbmRvYmoKCjIgMCBvYmoKPDwKICAvVHlwZSAvUGFnZXMKICAv" +
                "TWVkaWFCb3ggWyAwIDAgMjAwIDIwMCBdCiAgL0NvdW50IDEKICAvS2lkcyBbIDMgMCBSIF0K" +
                "Pj4KZW5kb2JqCgozIDAgb2JqCjw8CiAgL1R5cGUgL1BhZ2UKICAvUGFyZW50IDIgMCBSCiAg" +
                "L1Jlc291cmNlcyA8PAogICAgL0ZvbnQgPDwKICAgICAgL0YxIDQgMCBSIAogICAgPj4KICA+" +
                "PgogIC9Db250ZW50cyA1IDAgUgo+PgplbmRvYmoKCjQgMCBvYmoKPDwKICAvVHlwZSAvRm9u" +
                "dAogIC9TdWJ0eXBlIC9UeXBlMQogIC9CYXNlRm9udCAvVGltZXMtUm9tYW4KPj4KZW5kb2Jq" +
                "Cgo1IDAgb2JqICAlIHBhZ2UgY29udGVudAo8PAogIC9MZW5ndGggNDQKPj4Kc3RyZWFtCkJU" +
                "CjcwIDUwIFRECi9GMSAxMiBUZgooSGVsbG8sIFdvcmxkISkgVGoKRVQKZW5kc3RyZWFtCmVu" +
                "ZG9iagoKeHJlZgowIDYKMDAwMDAwMDAwMCA2NTUzNSBmIAowMDAwMDAwMDEwIDAwMDAwIG4g" +
                "CjAwMDAwMDAwNzkgMDAwMDAgbiAKMDAwMDAwMDE3MyAwMDAwMCBuIAowMDAwMDAwMzAxIDAw" +
                "MDAwIG4gCjAwMDAwMDAzODAgMDAwMDAgbiAKdHJhaWxlcgo8PAogIC9TaXplIDYKICAvUm9v" +
                "dCAxIDAgUgo+PgpzdGFydHhyZWYKNDkyCiUlRU9G";

        // Set blob contents
        blobContents.setMimeType("application/pdf");
        blobContents.setBlob(pdfBase64);
        blobContents.setUri("data:application/pdf;base64," + pdfBase64);
        // Set embedded resource
        embedded.setResource(blobContents);

        return embedded;
    }
}