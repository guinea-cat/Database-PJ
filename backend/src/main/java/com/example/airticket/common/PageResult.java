package com.example.airticket.common;

import java.util.List;

public class PageResult<T> {
    public long total;
    public int pageNum;
    public int pageSize;
    public List<T> records;

    public PageResult(long total, int pageNum, int pageSize, List<T> records) {
        this.total = total;
        this.pageNum = pageNum;
        this.pageSize = pageSize;
        this.records = records;
    }
}
