package com.boboboom.jxc.inventory.application.service;

import java.time.LocalDate;

record InventoryCheckListFilter(String timeType,
                                LocalDate start,
                                LocalDate end,
                                String warehouse,
                                String documentCode,
                                String item,
                                String status,
                                String range,
                                String printStatus,
                                String generatedStatus,
                                String remark) { }
