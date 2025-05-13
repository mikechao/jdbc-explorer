package com.mike.chao.jdbc.explorer.data;

import java.util.List;

public record TableDetails(
    String tableName,
    List<ColumnDetail> columns,
    List<String> primaryKeyColumns,
    List<ForeignKeyDetail> foreignKeys,
    List<IndexDetail> indexes
) {}
