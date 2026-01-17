package io.github.vishalmysore.service;

import com.opencsv.CSVWriter;
import com.t4a.annotations.Action;
import com.t4a.annotations.Agent;
import com.t4a.annotations.Prompt;
import com.t4a.processor.ProcessorAware;

import io.github.vishalmysore.a2ui.A2UIAware;
import io.github.vishalmysore.data.ColumnData;
import io.github.vishalmysore.data.RowData;
import io.github.vishalmysore.data.TableData;
import io.github.vishalmysore.mcp.domain.BlobResourceContents;
import io.github.vishalmysore.mcp.domain.EmbeddedResource;
import lombok.extern.java.Log;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.sql.*;
import java.util.*;

@Log
@Service
@Agent(groupName = "Database related actions",prompt = "You are a database management agent that can create databases, create tables, insert data into tables, and retrieve data using SQL queries. Use the provided actions to perform database operations as requested by the user.")
public class DerbyService implements A2UIAware, ProcessorAware {

    private static final String JDBC_URL =
            "jdbc:derby:memory:myDB;create=true";

    /* =================================================
       CREATE DATABASE
     ================================================= */

    @Action(description = "Create database" ,prompt = "dont populate database name if not found do not assume the name only put if you really find it, do not put any comments in json")
    public Object createDatabase(String databaseName) {

        if (databaseName == null || databaseName.trim().isEmpty()) {
            if (isUICallback(getCallback())) {
                return createSingleInputForm(
                        "create_database",
                        "Create Database",
                        "Database Name",
                        "databaseName"
                );
            }
            return "Database name is required.";
        }

        try (Connection conn = DriverManager.getConnection(JDBC_URL)) {
            return uiOrText(
                    "Database Created",
                    "Database '" + databaseName + "' created successfully."
            );
        } catch (SQLException e) {
            return uiOrText("Database Error", e.getMessage());
        }
    }

    /* =================================================
       CREATE TABLE
     ================================================= */

    @Action(description = "Create table")
    public Object createTables(TableData tableData) {

        if (tableData == null ||
                tableData.getTableName() == null ||
                tableData.getTableName().trim().isEmpty() ||
                tableData.getHeaderList() == null ||
                tableData.getHeaderList().isEmpty()) {

            if (isUICallback(getCallback())) {
                return createMessageUI(
                        "Create Table",
                        "Please provide tableName and column definitions."
                );
            }
            return "Table name and columns are required.";
        }

        StringBuilder sql = new StringBuilder("CREATE TABLE ");
        sql.append(tableData.getTableName()).append(" (");

        for (ColumnData c : tableData.getHeaderList()) {
            sql.append(c.getColumnName())
                    .append(" ")
                    .append(c.getSqlColumnType())
                    .append(", ");
        }

        sql.setLength(sql.length() - 2);
        sql.append(")");

        try (Connection conn = DriverManager.getConnection(JDBC_URL);
             Statement stmt = conn.createStatement()) {

            stmt.execute(sql.toString());
            return uiOrText(
                    "Table Created",
                    "Table '" + tableData.getTableName() + "' created successfully."
            );

        } catch (SQLException e) {
            return uiOrText("Table Error", e.getMessage());
        }
    }

    /* =================================================
       INSERT DATA
     ================================================= */

    @Action(description = "Insert data into table")
    public Object insertDataInTable(TableData tableData) {

        if (tableData == null ||
                tableData.getTableName() == null ||
                tableData.getRowDataList() == null ||
                tableData.getRowDataList().isEmpty()) {

            if (isUICallback(getCallback())) {
                return createMessageUI(
                        "Insert Data",
                        "Provide row data for table insertion."
                );
            }
            return "No data provided.";
        }

        List<ColumnData> columns =
                tableData.getRowDataList()
                        .get(0)
                        .getColumnDataList();

        StringBuilder sql = new StringBuilder("INSERT INTO ");
        sql.append(tableData.getTableName()).append(" (");

        for (ColumnData c : columns) {
            sql.append(c.getColumnName()).append(", ");
        }

        sql.setLength(sql.length() - 2);
        sql.append(") VALUES (");

        for (int i = 0; i < columns.size(); i++) {
            sql.append("?, ");
        }

        sql.setLength(sql.length() - 2);
        sql.append(")");

        try (Connection conn = DriverManager.getConnection(JDBC_URL);
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {

            for (RowData row : tableData.getRowDataList()) {
                int idx = 1;
                for (ColumnData c : row.getColumnDataList()) {
                    ps.setObject(idx++, c.getColumnValue());
                }
                ps.addBatch();
            }

            ps.executeBatch();

            return uiOrText(
                    "Data Inserted",
                    "Rows inserted into table '" + tableData.getTableName() + "'."
            );

        } catch (SQLException e) {
            return uiOrText("Insert Error", e.getMessage());
        }
    }

    /* =================================================
       RETRIEVE DATA
     ================================================= */

    @Action(description = "Retrieve data")
    public Object retrieveData(String sqlQuery) {

        if (sqlQuery == null || sqlQuery.trim().isEmpty()) {
            if (isUICallback(getCallback())) {
                return createSingleInputForm(
                        "query_form",
                        "Run Query",
                        "SQL SELECT query",
                        "sqlQuery"
                );
            }
            return "SQL query is required.";
        }

        List<Map<String, Object>> rows =
                new ArrayList<Map<String, Object>>();

        try (Connection conn = DriverManager.getConnection(JDBC_URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sqlQuery)) {

            ResultSetMetaData md = rs.getMetaData();

            while (rs.next()) {
                Map<String, Object> row =
                        new LinkedHashMap<String, Object>();
                for (int i = 1; i <= md.getColumnCount(); i++) {
                    row.put(md.getColumnName(i), rs.getObject(i));
                }
                rows.add(row);
            }

            if (isUICallback(getCallback())) {
                return createResultUI(sqlQuery, rows);
            }
            return rows;

        } catch (SQLException e) {
            return uiOrText("Query Error", e.getMessage());
        }
    }

    /* =================================================
       UI HELPERS
     ================================================= */

    private Object uiOrText(String title, String message) {
        if (isUICallback(getCallback())) {
            return createMessageUI(title, message);
        }
        return message;
    }

    private Map<String, Object> createMessageUI(
            String title, String message) {

        List<String> children =
                Arrays.asList("title", "message");

        List<Map<String, Object>> components =
                new ArrayList<Map<String, Object>>();

        components.add(createRootColumn("root", children));
        components.add(createTextComponent(
                "title", "🗄️ " + title, "h2"));
        components.add(createTextComponent(
                "message", message, "body"));

        return buildA2UIMessage(
                "message_ui",
                "root",
                components
        );
    }

    private Map<String, Object> createSingleInputFormNoRoot(
            String surfaceId,
            String title,
            String label,
            String fieldName) {

        List<String> children =
                Arrays.asList("title", fieldName);

        List<Map<String, Object>> components =
                new ArrayList<Map<String, Object>>();

        components.add(createRootColumn("root", children));
        components.add(createTextComponent(
                "title", "✍️ " + title, "h2"));
        components.add(createTextField(fieldName, label));

        return buildA2UIMessage(
                surfaceId,
                "root",
                components
        );
    }
    private Map<String, Object> createSingleInputForm(
            String surfaceId,
            String title,
            String label,
            String fieldName) {

        Map<String, Object> textField = new HashMap<>();
        textField.put("id", fieldName);

        Map<String, Object> textFieldComponent = new HashMap<>();
        Map<String, Object> textFieldProps = new HashMap<>();
        textFieldProps.put("label", new HashMap<String, Object>() {{
            put("literalString", label);
        }});
        textFieldProps.put("text", new HashMap<String, Object>() {{
            put("literalString", "");
        }});
        textFieldComponent.put("TextField", textFieldProps);
        textField.put("component", textFieldComponent);

        return buildA2UIMessage(
                surfaceId,
                "root",
                Arrays.asList(
                        createRootColumn("root", Arrays.asList("title", "field")),
                        createTextComponent("title", "✍️ " + title, "h2"),
                        textField
                )
        );
    }


    private Map<String, Object> createTextField(
            String id, String label) {

        Map<String, Object> field =
                new HashMap<String, Object>();

        field.put("id", id);
        field.put("component", "TextField");
        field.put("label", label);
        field.put("text", "");

        return field;
    }

    private Map<String, Object> createResultUI(
            String query,
            List<Map<String, Object>> rows) {

        List<String> children =
                new ArrayList<String>();

        List<Map<String, Object>> components =
                new ArrayList<Map<String, Object>>();

        children.add("title");
        children.add("query");

        components.add(createTextComponent(
                "title", "📊 Query Results", "h2"));
        components.add(createTextComponent(
                "query", "Query: " + query, "body"));

        int count = 0;
        for (Map<String, Object> row : rows) {
            count++;
            if (count > 5) break;

            String id = "row_" + count;
            children.add(id);
            components.add(createTextComponent(
                    id, row.toString(), "body"));
        }

        components.add(createRootColumn("root", children));

        return buildA2UIMessage(
                "query_results",
                "root",
                components
        );
    }
}
