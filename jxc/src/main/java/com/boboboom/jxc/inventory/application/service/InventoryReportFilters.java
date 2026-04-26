package com.boboboom.jxc.inventory.application.service;

import java.time.LocalDate;
import java.time.LocalDateTime;

record InventoryInoutDetailFilter(LocalDate start,
                                   LocalDate end,
                                   LocalDateTime auditStart,
                                   LocalDateTime auditEnd,
                                   String warehouse,
                                   String warehouseType,
                                   String inoutType,
                                   String upstreamDocumentType,
                                   String itemCategory,
                                   String statisticType,
                                   String itemCode,
                                   String reasonType,
                                   String adjustmentDocument,
                                   String oppositeOrg,
                                   String documentNo,
                                   String crossMonthDocument,
                                   String inoutDirection,
                                   String gift,
                                   String unitType) { }
