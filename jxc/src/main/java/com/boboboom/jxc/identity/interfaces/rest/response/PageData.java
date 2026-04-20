package com.boboboom.jxc.identity.interfaces.rest.response;

/**
 * 通用分页响应。
 *
 * @param <T> 数据类型
 * @param list 当前页数据
 * @param total 总条数
 * @param pageNum 当前页码
 * @param pageSize 当前页大小
 */
public record PageData<T>(java.util.List<T> list, long total, int pageNum, int pageSize) {
}
