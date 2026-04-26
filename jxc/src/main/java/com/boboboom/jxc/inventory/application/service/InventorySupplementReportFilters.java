package com.boboboom.jxc.inventory.application.service;

record StockInoutSummaryFilter(String statisticMode,
                                String targetStore,
                                String warehouse,
                                String warehouseType,
                                String itemKeyword,
                                String itemCategory,
                                String statisticType,
                                String itemStatus,
                                String inoutType,
                                String inoutDirection,
                                String oppositeOrg,
                                String unitType) { }

record OtherInoutSummaryFilter(String warehouse,
                                String itemCategory,
                                String itemCode,
                                String inoutType,
                                String reasonType,
                                String itemStatus) { }

record InterOrgTransferDetailFilter(String statisticMode,
                                     String targetStore,
                                     String sourceStore,
                                     String itemName,
                                     String itemCategory,
                                     String sourceWarehouse,
                                     String targetWarehouse,
                                     String documentStatus) { }

record StockTurnoverRateFilter(String statisticDimension,
                                String warehouse,
                                String itemCategory,
                                String itemCode,
                                String itemStatus,
                                String unitType) { }
