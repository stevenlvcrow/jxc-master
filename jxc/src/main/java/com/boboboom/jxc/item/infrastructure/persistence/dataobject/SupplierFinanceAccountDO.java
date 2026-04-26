package com.boboboom.jxc.item.infrastructure.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.TableName;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.BaseAuditDO;

/** 物品与供应商数据对象，映射数据库表记录。 */
@TableName("supplier_finance_account")
public class SupplierFinanceAccountDO extends BaseAuditDO {

    private Long supplierId;
    private Integer sortNo;
    private String bankAccount;
    private String accountName;
    private String bankName;
    private Boolean defaultAccount;

    /** 获取SupplierId。 */
    public Long getSupplierId() {
        return supplierId;
    }

    /** 设置SupplierId。 */
    public void setSupplierId(Long supplierIdValue) {
        this.supplierId = supplierIdValue;
    }

    /** 获取SortNo。 */
    public Integer getSortNo() {
        return sortNo;
    }

    /** 设置SortNo。 */
    public void setSortNo(Integer sortNoValue) {
        this.sortNo = sortNoValue;
    }

    /** 获取BankAccount。 */
    public String getBankAccount() {
        return bankAccount;
    }

    /** 设置BankAccount。 */
    public void setBankAccount(String bankAccountValue) {
        this.bankAccount = bankAccountValue;
    }

    /** 获取AccountName。 */
    public String getAccountName() {
        return accountName;
    }

    /** 设置AccountName。 */
    public void setAccountName(String accountNameValue) {
        this.accountName = accountNameValue;
    }

    /** 获取BankName。 */
    public String getBankName() {
        return bankName;
    }

    /** 设置BankName。 */
    public void setBankName(String bankNameValue) {
        this.bankName = bankNameValue;
    }

    /** 获取DefaultAccount。 */
    public Boolean getDefaultAccount() {
        return defaultAccount;
    }

    /** 设置DefaultAccount。 */
    public void setDefaultAccount(Boolean defaultAccountValue) {
        this.defaultAccount = defaultAccountValue;
    }
}
