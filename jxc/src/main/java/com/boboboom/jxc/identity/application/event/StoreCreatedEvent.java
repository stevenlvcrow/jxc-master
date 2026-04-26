package com.boboboom.jxc.identity.application.event;

/**
 * 门店创建事件，承载门店初始化订阅动作需要的上下文。
 *
 * @param groupId 集团 ID
 * @param storeId 门店 ID
 * @param operatorId 操作人 ID
 * @param adminUserId 门店管理员用户 ID
 */
public record StoreCreatedEvent(Long groupId,
                                Long storeId,
                                Long operatorId,
                                Long adminUserId) {
}
