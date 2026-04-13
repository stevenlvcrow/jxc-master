package com.boboboom.jxc.item.interfaces.rest.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public record SupplierCreateRequest(
        @NotBlank(message = "供货商编码不能为空")
        @Size(max = 64, message = "供货商编码长度不能超过64")
        String supplierCode,

        @NotBlank(message = "供货商名称不能为空")
        @Size(max = 128, message = "供货商名称长度不能超过128")
        String supplierName,

        @Size(max = 128, message = "供货商简称长度不能超过128")
        String supplierShortName,

        @Size(max = 64, message = "供货商助记码长度不能超过64")
        String supplierMnemonic,

        @NotBlank(message = "供货商类别不能为空")
        @Size(max = 128, message = "供货商类别长度不能超过128")
        String supplierCategory,

        @NotNull(message = "税率不能为空")
        @DecimalMin(value = "0.00", message = "税率不能小于0")
        @DecimalMax(value = "100.00", message = "税率不能大于100")
        BigDecimal taxRate,

        @NotBlank(message = "状态不能为空")
        String status,

        @Size(max = 64, message = "联系人长度不能超过64")
        String contactPerson,

        @Size(max = 32, message = "联系电话长度不能超过32")
        String contactPhone,

        @Size(max = 128, message = "邮箱地址长度不能超过128")
        String email,

        @Size(max = 255, message = "联系地址长度不能超过255")
        String contactAddress,

        @Size(max = 500, message = "备注长度不能超过500")
        String remark,

        @NotBlank(message = "结算方式不能为空")
        String settlementMethod,

        @NotBlank(message = "订单汇总规则不能为空")
        String orderSummaryRule,

        @NotNull(message = "发货时录入批次不能为空")
        Boolean inputBatchWhenDelivery,

        @NotNull(message = "同步收货数据不能为空")
        Boolean syncReceiptData,

        @NotBlank(message = "采购收货依赖供货商发货不能为空")
        String purchaseReceiptDependShipping,

        @NotBlank(message = "配送依赖供应商发货不能为空")
        String deliveryDependShipping,

        @NotNull(message = "供货商管理库存不能为空")
        Boolean supplierManageInventory,

        @NotNull(message = "控制下单时间不能为空")
        Boolean controlOrderTime,

        @NotNull(message = "允许关闭订单不能为空")
        Boolean allowCloseOrder,

        @NotBlank(message = "供应商对账模式不能为空")
        @Size(max = 255, message = "供应商对账模式长度不能超过255")
        String reconciliationMode,

        @NotBlank(message = "范围控制不能为空")
        String scopeControl,

        @Valid
        List<QualificationItemRequest> qualificationList,

        @Valid
        List<ContractItemRequest> contractList,

        @Valid
        List<FinanceItemRequest> financeList,

        @Size(max = 128, message = "单位名称长度不能超过128")
        String invoiceCompanyName,

        @Size(max = 64, message = "纳税人识别号长度不能超过64")
        String taxpayerId,

        @Size(max = 32, message = "开票电话长度不能超过32")
        String invoicePhone,

        @Size(max = 255, message = "单位地址长度不能超过255")
        String invoiceAddress
) {
    public record QualificationItemRequest(
            @Size(max = 255, message = "资质文件名长度不能超过255")
            String fileName,

            @Size(max = 500, message = "资质文件地址长度不能超过500")
            String fileUrl,

            @NotBlank(message = "资质类型不能为空")
            @Size(max = 64, message = "资质类型长度不能超过64")
            String qualificationType,

            @Size(max = 32, message = "资质有效期格式错误")
            String validTo,

            @Size(max = 16, message = "资质状态长度不能超过16")
            String status,

            @Size(max = 500, message = "资质备注长度不能超过500")
            String remark
    ) {
    }

    public record ContractItemRequest(
            @Size(max = 255, message = "合同附件名长度不能超过255")
            String attachmentName,

            @Size(max = 500, message = "合同附件地址长度不能超过500")
            String attachmentUrl,

            @Size(max = 128, message = "合同名称长度不能超过128")
            String contractName,

            @Size(max = 64, message = "合同编号长度不能超过64")
            String contractCode,

            @Size(max = 32, message = "合同有效期格式错误")
            String validTo,

            @Size(max = 16, message = "合同状态长度不能超过16")
            String status
    ) {
    }

    public record FinanceItemRequest(
            @NotBlank(message = "银行账号不能为空")
            @Size(max = 64, message = "银行账号长度不能超过64")
            String bankAccount,

            @NotBlank(message = "账户名称不能为空")
            @Size(max = 128, message = "账户名称长度不能超过128")
            String accountName,

            @NotBlank(message = "银行名称不能为空")
            @Size(max = 128, message = "银行名称长度不能超过128")
            String bankName,

            @NotNull(message = "默认银行账号标记不能为空")
            Boolean defaultAccount
    ) {
    }
}

