package com.boboboom.jxc.identity.interfaces.rest.request;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

/** 身份与权限请求参数，承载接口入参。 */
public class MenuSortRequest {

    @Valid
    @NotEmpty(message = "排序数据不能为空")
    private List<MenuSortItem> items;

    /** 获取Items。 */
    public List<MenuSortItem> getItems() {
        return items;
    }

    /** 设置Items。 */
    public void setItems(List<MenuSortItem> itemsValue) {
        this.items = itemsValue;
    }

    /**
     * 菜单排序项。
     */
    public static class MenuSortItem {

        @NotNull(message = "菜单ID不能为空")
        private Long id;

        private Long parentId;

        @NotNull(message = "排序号不能为空")
        private Integer sortNo;

        /** 获取Id。 */
        public Long getId() {
            return id;
        }

        /** 设置Id。 */
        public void setId(Long idValue) {
            this.id = idValue;
        }

        /** 获取ParentId。 */
        public Long getParentId() {
            return parentId;
        }

        /** 设置ParentId。 */
        public void setParentId(Long parentIdValue) {
            this.parentId = parentIdValue;
        }

        /** 获取SortNo。 */
        public Integer getSortNo() {
            return sortNo;
        }

        /** 设置SortNo。 */
        public void setSortNo(Integer sortNoValue) {
            this.sortNo = sortNoValue;
        }
    }
}
