# Google A2A protocol : AI Agent for Database

This project demonstrates an AI agent capable of performing various database operations using Google A2A protocol  as well as MCP protocol, such as creating databases, creating tables, inserting data, and retrieving data, all through simple english statements. The agent interacts with a **Derby** in-memory database but can be extended to support other relational database systems.

The core of this implementation lies in utilizing annotations like **@Agent** and **@Action** from [Tools4AI](https://github.com/vishalmysore/tools4ai) framework to define database operations and map those operations to natural English language queries.

The solution uses a flexible and generic approach where **English prompts** are mapped to the `TableData` class, which handles the details of tables, columns, and rows for database actions.


To run this as MCP Server you need to include testserver.js as part of our Claude Desktop COnfig

``` 
"databaseagent": {
      "command": "node",
      "args": [
        "/work/SqlAIAgent/src/test/resources/dist/testserver.js"
      ]
    } 
```  
or use MCP Inspector 

```  
npx @modelcontextprotocol/inspector node dist/testserver.js
```

and connect to server and list tools and call tools 


### Key Features
You can find the source code for the `DerbyService` class [here](src/main/java/io/github/vishalmysore/service/DerbyService.java).  

```java
@Log
@Service
@Agent(groupName = "Database related actions")
public class DerbyService {

    private static final String JDBC_URL = "jdbc:derby:memory:myDB;create=true";
    private static final String JDBC_DRIVER = "org.apache.derby.jdbc.ClientDriver";
}
```

**Dynamic Action Mapping:** The actions (such as createDatabase(), createTables(), insertDataInTable(), etc.) are dynamically mapped to SQL queries based on English language prompts. For example:

**Action**: @Action(description = "Create tables")  
**English Prompt:** "Hey, I need to maintain a record of employees with title and name."  
**Mapped SQL Query:** CREATE TABLE employees (name VARCHAR(255), title VARCHAR(255));  

In this case, the AI agent will convert the English query into a corresponding SQL statement to create the necessary table for employee data.

Similarly:

**Action**: @Action(description = "Insert new data in database table")  
**English Prompt:** "Insert a record for Sanjay Kapoor, who joined today as a Chef."  
**Mapped SQL Query:** INSERT INTO employees (name, title) VALUES ('Sanjay Kapoor', 'Chef');  

The @Agent and @Action Annotations  
**@Agent:** The @Agent annotation groups a collection of related actions. In this case, the DerbyService class is annotated with @Agent(groupName = "Database related actions") to indicate that all the database-related actions belong to this agent.

@**Action**: The @Action annotation defines a specific action that the AI agent will perform. For example, the createTables() and insertDataInTable() methods are marked with @Action(description = "Create tables") and @Action(description = "Insert new data in database table"), respectively. This allows the agent to interpret natural language queries and map them to appropriate SQL actions.




## Technologies

- **Java:** Core programming language for implementation.
- **Spring Boot:** Framework for building the application and providing database services.
- **Apache Derby:** In-memory relational database used for demonstration purposes.
- **Tools4AI:** Framework that allows the creation of AI-based actions for various tasks like database management.

## Key Components

### `TableData` Class

The core class used to represent the structure of database tables and their data.

```java
public class TableData {
    private String tableName;
    @ListType(ColumnData.class)
    private List<ColumnData> headerList;
    @ListType(RowData.class)
    private List<RowData> rowDataList;
}
```

## Conclusion
This project serves as a reference implementation ONLY demonstrating how Tools4AI can be used to build a database AI agent capable of executing actions such as creating databases, tables, inserting data, and retrieving data based on structured and unstructured prompts.

Please note that this is **not production-ready** code but a proof-of-concept to showcase the potential of AI agents in automating database management tasks. In the future, this mechanism could be extended to convert any type of unstructured data into SQL commands, such as processing newsletters, articles, or other textual content. The flexibility of the system opens up numerous possibilities for applying AI in different domains beyond just database management.

