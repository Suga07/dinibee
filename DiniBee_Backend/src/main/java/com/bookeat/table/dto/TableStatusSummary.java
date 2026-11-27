package com.bookeat.table.dto;

public record TableStatusSummary(long total, long available, long reserved, long occupied, long cleaning, long blocked, long maintenance) {}
