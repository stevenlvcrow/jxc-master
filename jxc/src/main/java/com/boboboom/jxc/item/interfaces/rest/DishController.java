package com.boboboom.jxc.item.interfaces.rest;

import com.boboboom.jxc.identity.interfaces.rest.response.CodeDataResponse;
import com.boboboom.jxc.item.application.service.DishApplicationService;
import com.boboboom.jxc.item.application.service.DishApplicationService.DishListRow;
import com.boboboom.jxc.item.application.service.DishApplicationService.PageData;
import com.boboboom.jxc.item.application.service.DishApplicationService.TreeNode;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/dishes")
public class DishController {

    private final DishApplicationService dishApplicationService;

    public DishController(DishApplicationService dishApplicationService) {
        this.dishApplicationService = dishApplicationService;
    }

    @GetMapping
    public CodeDataResponse<PageData<DishListRow>> list(@RequestParam(defaultValue = "1") Integer pageNo,
                                                        @RequestParam(defaultValue = "10") Integer pageSize,
                                                        @RequestParam(required = false) String keyword,
                                                        @RequestParam(required = false) String deleted,
                                                        @RequestParam(required = false) String categoryId,
                                                        @RequestParam(required = false) String dishType,
                                                        @RequestParam(required = false) String orgId) {
        return dishApplicationService.list(pageNo, pageSize, keyword, deleted, categoryId, dishType, orgId);
    }

    @GetMapping("/categories/tree")
    public CodeDataResponse<List<TreeNode>> categoryTree(@RequestParam(required = false) String orgId) {
        return dishApplicationService.categoryTree(orgId);
    }
}
