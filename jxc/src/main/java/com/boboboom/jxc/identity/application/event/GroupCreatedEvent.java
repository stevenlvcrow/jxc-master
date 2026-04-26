package com.boboboom.jxc.identity.application.event;

/**
 * 集团创建事件，承载集团初始化订阅动作需要的上下文。
 *
 * @param groupId 集团 ID
 * @param operatorId 操作人 ID
 * @param adminRealName 集团管理员姓名
 * @param adminPhone 集团管理员手机号
 */
public record GroupCreatedEvent(Long groupId,
                                Long operatorId,
                                String adminRealName,
                                String adminPhone) {
}
