package io.github.vishalmysore.data;

import com.t4a.annotations.ListType;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@ToString
public class TableData {
    private String tableName;
    @ListType(ColumnData.class)
    private List<ColumnData> headerList;
    @ListType(RowData.class)
    private List<RowData> rowDataList;
}
