package com.boboboom.jxc.item.interfaces.rest;

import com.boboboom.jxc.identity.interfaces.rest.response.CodeDataResponse;
import com.boboboom.jxc.item.application.service.DishApplicationService;
import com.boboboom.jxc.item.application.service.DishApplicationService.DishListRow;
import com.boboboom.jxc.item.application.service.DishApplicationService.PageData;
import com.boboboom.jxc.item.application.service.DishApplicationService.TreeNode;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/dishes")
/**
 * 菜品接口，负责菜品分页查询和分类树查询。
 */
public class DishController {

    private final DishApplicationService dishApplicationService;

    /**
     * 构造菜品接口。
     *
     * @param dishApplicationService 菜品服务
     */
    public DishController(DishApplicationService dishApplicationService) {
        this.dishApplicationService = dishApplicationService;
    }

    /**
     * 分页查询菜品。
     *
     * @param pageNo 页码
     * @param pageSize 每页条数
     * @param keyword 关键字
     * @param deleted 删除标记
     * @param categoryId 分类主键
     * @param dishType 菜品类型
     * @param orgId 机构标识
     * @return 分页结果
     */
    @GetMapping
    @PreAuthorize("@requestPermissionGuard.authenticated()")
    public CodeDataResponse<PageData<DishListRow>> list(@RequestParam(defaultValue = "1") Integer pageNo,
                                                        @RequestParam(defaultValue = "10") Integer pageSize,
                                                        @RequestParam(required = false) String keyword,
                                                        @RequestParam(required = false) String deleted,
                                                        @RequestParam(required = false) String categoryId,
                                                        @RequestParam(required = false) String dishType,
                                                        @RequestParam(required = false) String orgId) {
        return CodeDataResponse.ok(dishApplicationService.list(pageNo, pageSize, keyword, deleted, categoryId, dishType, orgId));
    }

    /**
     * 查询菜品分类树。
     *
     * @param orgId 机构标识
     * @return 分类树
     */
    @GetMapping("/categories/tree")
    @PreAuthorize("@requestPermissionGuard.authenticated()")
    public CodeDataResponse<List<TreeNode>> categoryTree(@RequestParam(required = false) String orgId) {
        return CodeDataResponse.ok(dishApplicationService.categoryTree(orgId));
    }
}
