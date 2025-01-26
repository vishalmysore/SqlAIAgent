package io.github.vishalmysore.data;

import com.t4a.annotations.Prompt;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Setter
@Getter
@ToString
public class ColumnData {
    private String columnName;
    @Prompt(describe = "This field should match the database column type. For example, if the column is of type VARCHAR, then this field should be VARCHAR (255). If the column is of type INT, then this field should be INT.")
    private String sqlColumnType;
    private String columnValue;
}
