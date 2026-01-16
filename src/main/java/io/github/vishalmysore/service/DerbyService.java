package io.github.vishalmysore.service;

import com.opencsv.CSVWriter;
import com.t4a.annotations.Action;
import com.t4a.annotations.Agent;
import com.t4a.processor.ProcessorAware;
import io.github.vishalmysore.a2ui.A2UIAware;
import io.github.vishalmysore.data.ColumnData;
import io.github.vishalmysore.data.RowData;
import io.github.vishalmysore.data.TableData;

import io.github.vishalmysore.mcp.domain.BlobResourceContents;
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
public class DerbyService implements A2UIAware, ProcessorAware {

    private static final String JDBC_URL = "jdbc:derby:memory:myDB;create=true";
    private static final String JDBC_DRIVER = "org.apache.derby.jdbc.ClientDriver";

    @PreAuthorize("hasRole('USER')")
    @Action(description = "start database server")
    public Object startServer(String serverName) {
        // Start the Derby server
        log.info("Derby server started.");
        String result = "Derby server started for " + serverName;
        if (isUICallback(getCallback())) {
            return createGenericUI("Server Status", result);
        }
        return result;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Action(description = "Create database")
    public Object createDatabase(String databaseName) {
        // Create a database
        String result;
        try (Connection conn = DriverManager.getConnection(JDBC_URL)) {
            if (conn != null) {
                result = "Database " + databaseName + " created successfully.";
            } else {
                result = "Failed to create database " + databaseName + ".";
            }
        } catch (SQLException e) {
            e.printStackTrace();
            result = "Error creating database: " + e.getMessage();
        }

        if (isUICallback(getCallback())) {
            return createGenericUI("Database Creation", result);
        }
        return result;
    }

    @PreAuthorize("hasRole('USER')")
    @Action(description = "Create tables")
    public Object createTables(TableData tableData) {
        StringBuilder createTableSQL = new StringBuilder("CREATE TABLE ");
        createTableSQL.append(tableData.getTableName()).append(" (");

        for (ColumnData column : tableData.getHeaderList()) {
            createTableSQL.append(column.getColumnName())
                    .append(" ")
                    .append(column.getSqlColumnType())
                    .append(", ");
        }

        createTableSQL.setLength(createTableSQL.length() - 2);
        createTableSQL.append(")");
        log.info("Create table SQL: " + createTableSQL);

        String result;
        try (Connection conn = DriverManager.getConnection(JDBC_URL);
                Statement stmt = conn.createStatement()) {
            stmt.execute(createTableSQL.toString());
            result = tableData.getTableName() + " table created successfully.";
        } catch (SQLException e) {
            e.printStackTrace();
            result = "Error creating table: " + e.getMessage();
        }

        if (isUICallback(getCallback())) {
            return createGenericUI("Table Creation", result);
        }
        return result;
    }

    @Action(description = "Insert new data in database table")
    public Object insertDataInTable(TableData tableData) {
        StringBuilder insertSQL = new StringBuilder("INSERT INTO ");
        insertSQL.append(tableData.getTableName()).append(" (");

        List<ColumnData> columns = tableData.getRowDataList().get(0).getColumnDataList();
        for (ColumnData column : columns) {
            insertSQL.append(column.getColumnName()).append(", ");
        }

        insertSQL.setLength(insertSQL.length() - 2);
        insertSQL.append(") VALUES (");

        for (int i = 0; i < columns.size(); i++) {
            insertSQL.append("?, ");
        }

        insertSQL.setLength(insertSQL.length() - 2);
        insertSQL.append(")");

        String result;
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
            result = "Data inserted successfully into " + tableData.getTableName();
        } catch (SQLException e) {
            e.printStackTrace();
            result = "Error inserting data: " + e.getMessage();
        }

        if (isUICallback(getCallback())) {
            return createGenericUI("Data Insertion", result);
        }
        return result;
    }

    @Action(description = "Retrieve data from table")
    public Object retrieveData(String sqlSelectQuery) {
        List<Map<String, Object>> result = new ArrayList<>();

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
                result.add(row);
                log.info("Data retrieved successfully.");
            }

            if (isUICallback(getCallback())) {
                return createDatabaseResultUI(sqlSelectQuery, result);
            }
            return result;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private Map<String, Object> createGenericUI(String title, String message) {
        String surfaceId = "database_action";
        String rootId = "root";

        List<String> childIds = Arrays.asList("title", "message");
        List<Map<String, Object>> components = new ArrayList<>();

        components.add(createRootColumn(rootId, childIds));
        components.add(createTextComponent("title", "🗄️ " + title, "h2"));
        components.add(createTextComponent("message", message, "body"));

        return buildA2UIMessage(surfaceId, rootId, components);
    }

    private Map<String, Object> createDatabaseResultUI(String query, List<Map<String, Object>> result) {
        String surfaceId = "database_result";
        String rootId = "root";

        List<String> childIds = new ArrayList<>();
        childIds.add("title");
        childIds.add("query_text");
        childIds.add("divider");

        List<Map<String, Object>> components = new ArrayList<>();

        components.add(createTextComponent("title", "📊 Database Query Results", "h2"));
        components.add(createTextComponent("query_text", "Query: " + query, "body"));
        components.add(createTextComponent("divider", "───────────────────────", "body"));

        if (result.isEmpty()) {
            childIds.add("no_results");
            components.add(createTextComponent("no_results", "No results found.", "body"));
        } else {
            int count = 0;
            for (Map<String, Object> row : result) {
                if (count++ >= 5)
                    break;
                String rowId = "row_" + count;
                childIds.add(rowId);
                components.add(createTextComponent(rowId, row.toString(), "body"));
            }
            if (result.size() > 5) {
                childIds.add("more_results");
                components.add(
                        createTextComponent("more_results", "... and " + (result.size() - 5) + " more rows.", "body"));
            }
        }

        components.add(createRootColumn(rootId, childIds));

        return buildA2UIMessage(surfaceId, rootId, components);
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

            ResultSetMetaData metaData = rs.getMetaData();
            String[] headers = new String[metaData.getColumnCount()];
            for (int i = 1; i <= metaData.getColumnCount(); i++) {
                headers[i - 1] = metaData.getColumnName(i);
            }
            csvWriter.writeNext(headers);

            while (rs.next()) {
                String[] row = new String[metaData.getColumnCount()];
                for (int i = 1; i <= metaData.getColumnCount(); i++) {
                    row[i - 1] = rs.getString(i);
                }
                csvWriter.writeNext(row);
            }

            byte[] fileContent = Files.readAllBytes(tempFile.toPath());
            String base64Content = Base64.getEncoder().encodeToString(fileContent);

            blobContents.setMimeType("text/csv");
            blobContents.setBlob(base64Content);

            embedded.setResource(blobContents);

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

        String mockData = "id,name,value\n" +
                "1,item1,100\n" +
                "2,item2,200\n" +
                "3,item3,300\n";

        blobContents.setMimeType("text/csv");
        blobContents.setText(mockData);

        embedded.setResource(blobContents);

        return embedded;
    }

    @Action(description = "Return grocery data as embedded PDF file resource")
    public EmbeddedResource getGroceryItemsAsPDF(String fileName) {
        EmbeddedResource embedded = new EmbeddedResource();
        BlobResourceContents blobContents = new BlobResourceContents();

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

        blobContents.setMimeType("application/pdf");
        blobContents.setBlob(pdfBase64);
        blobContents.setUri("data:application/pdf;base64," + pdfBase64);
        embedded.setResource(blobContents);

        return embedded;
    }
}
