package com.boboboom.jxc.identity.application.event;

/**
 * 门店创建事件，承载门店初始化订阅动作需要的上下文。
 *
 * @param groupId 集团 ID
 * @param storeId 门店 ID
 * @param operatorId 操作人 ID
 * @param adminRealName 门店管理员姓名
 * @param adminPhone 门店管理员手机号
 */
public record StoreCreatedEvent(Long groupId,
                                Long storeId,
                                Long operatorId,
                                String adminRealName,
                                String adminPhone) {
}
