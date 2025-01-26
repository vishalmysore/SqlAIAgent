package io.github.vishalmysore.data;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Getter
@Setter
@ToString
public class RowData {
    private int rowNum;
    private List<ColumnData> columnDataList;
}
