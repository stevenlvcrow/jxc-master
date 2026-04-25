package com.boboboom.jxc.purchase.interfaces.rest;

import com.boboboom.jxc.identity.interfaces.rest.response.CodeDataResponse;
import com.boboboom.jxc.purchase.application.service.PurchaseDocumentApplicationService;
import com.boboboom.jxc.purchase.application.service.PurchaseDocumentApplicationService.BatchActionRequest;
import com.boboboom.jxc.purchase.application.service.PurchaseDocumentApplicationService.IdPayload;
import com.boboboom.jxc.purchase.application.service.PurchaseDocumentApplicationService.LineReviewBatchRequest;
import com.boboboom.jxc.purchase.application.service.PurchaseDocumentApplicationService.LineUpdateBatchRequest;
import com.boboboom.jxc.purchase.application.service.PurchaseDocumentApplicationService.PageData;
import com.boboboom.jxc.purchase.application.service.PurchaseDocumentApplicationService.PurchaseDocumentView;
import com.boboboom.jxc.purchase.application.service.PurchaseDocumentApplicationService.SavePurchaseDocumentRequest;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Validated
@RestController
@RequestMapping("/api/purchase/documents")
public class PurchaseDocumentController {

    private final PurchaseDocumentApplicationService purchaseDocumentApplicationService;

    public PurchaseDocumentController(PurchaseDocumentApplicationService purchaseDocumentApplicationService) {
        this.purchaseDocumentApplicationService = purchaseDocumentApplicationService;
    }

    @GetMapping("/{documentType}")
    public CodeDataResponse<PageData<PurchaseDocumentView>> page(@PathVariable String documentType,
                                                                  @RequestParam Map<String, String> params) {
        return CodeDataResponse.ok(purchaseDocumentApplicationService.page(documentType, params));
    }

    @GetMapping("/{documentType}/{id:\\d+}")
    public CodeDataResponse<PurchaseDocumentView> detail(@PathVariable String documentType,
                                                         @PathVariable Long id,
                                                         @RequestParam(required = false) String orgId) {
        return CodeDataResponse.ok(purchaseDocumentApplicationService.detail(documentType, id, orgId));
    }

    @PostMapping("/{documentType}")
    public CodeDataResponse<IdPayload> create(@PathVariable String documentType,
                                              @RequestParam(required = false) String orgId,
                                              @RequestBody SavePurchaseDocumentRequest request) {
        return CodeDataResponse.ok(purchaseDocumentApplicationService.create(documentType, request, orgId));
    }

    @PutMapping("/{documentType}/{id:\\d+}")
    public CodeDataResponse<Void> update(@PathVariable String documentType,
                                         @PathVariable Long id,
                                         @RequestParam(required = false) String orgId,
                                         @RequestBody SavePurchaseDocumentRequest request) {
        purchaseDocumentApplicationService.update(documentType, id, request, orgId);
        return CodeDataResponse.ok();
    }

    @DeleteMapping("/{documentType}/{id:\\d+}")
    public CodeDataResponse<Void> delete(@PathVariable String documentType,
                                         @PathVariable Long id,
                                         @RequestParam(required = false) String orgId) {
        purchaseDocumentApplicationService.delete(documentType, id, orgId);
        return CodeDataResponse.ok();
    }

    @PostMapping("/{documentType}/batch-delete")
    public CodeDataResponse<Void> batchDelete(@PathVariable String documentType,
                                              @RequestParam(required = false) String orgId,
                                              @RequestBody BatchActionRequest request) {
        purchaseDocumentApplicationService.batchDelete(documentType, request, orgId);
        return CodeDataResponse.ok();
    }

    @PostMapping("/{documentType}/batch-submit")
    public CodeDataResponse<Void> batchSubmit(@PathVariable String documentType,
                                              @RequestParam(required = false) String orgId,
                                              @RequestBody BatchActionRequest request) {
        purchaseDocumentApplicationService.batchSubmit(documentType, request, orgId);
        return CodeDataResponse.ok();
    }

    @PostMapping("/{documentType}/batch-approve")
    public CodeDataResponse<Void> batchApprove(@PathVariable String documentType,
                                               @RequestParam(required = false) String orgId,
                                               @RequestBody BatchActionRequest request) {
        purchaseDocumentApplicationService.batchApprove(documentType, request, orgId);
        return CodeDataResponse.ok();
    }

    @PostMapping("/{documentType}/batch-reject")
    public CodeDataResponse<Void> batchReject(@PathVariable String documentType,
                                              @RequestParam(required = false) String orgId,
                                              @RequestBody BatchActionRequest request) {
        purchaseDocumentApplicationService.batchReject(documentType, request, orgId);
        return CodeDataResponse.ok();
    }

    @PostMapping("/{documentType}/batch-unapprove")
    public CodeDataResponse<Void> batchUnapprove(@PathVariable String documentType,
                                                 @RequestParam(required = false) String orgId,
                                                 @RequestBody BatchActionRequest request) {
        purchaseDocumentApplicationService.batchUnapprove(documentType, request, orgId);
        return CodeDataResponse.ok();
    }

    @PostMapping("/{documentType}/batch-print")
    public CodeDataResponse<Void> batchPrint(@PathVariable String documentType,
                                             @RequestParam(required = false) String orgId,
                                             @RequestBody BatchActionRequest request) {
        purchaseDocumentApplicationService.batchPrint(documentType, request, orgId);
        return CodeDataResponse.ok();
    }

    @PostMapping("/{documentType}/batch-close")
    public CodeDataResponse<Void> batchClose(@PathVariable String documentType,
                                             @RequestParam(required = false) String orgId,
                                             @RequestBody BatchActionRequest request) {
        purchaseDocumentApplicationService.batchClose(documentType, request, orgId);
        return CodeDataResponse.ok();
    }

    @PostMapping("/{documentType}/batch-cancel-close")
    public CodeDataResponse<Void> batchCancelClose(@PathVariable String documentType,
                                                   @RequestParam(required = false) String orgId,
                                                   @RequestBody BatchActionRequest request) {
        purchaseDocumentApplicationService.batchCancelClose(documentType, request, orgId);
        return CodeDataResponse.ok();
    }

    @PostMapping("/{documentType}/batch-receive")
    public CodeDataResponse<Void> batchReceive(@PathVariable String documentType,
                                               @RequestParam(required = false) String orgId,
                                               @RequestBody BatchActionRequest request) {
        purchaseDocumentApplicationService.batchReceive(documentType, request, orgId);
        return CodeDataResponse.ok();
    }

    @PostMapping("/{documentType}/batch-cancel-receive")
    public CodeDataResponse<Void> batchCancelReceive(@PathVariable String documentType,
                                                     @RequestParam(required = false) String orgId,
                                                     @RequestBody BatchActionRequest request) {
        purchaseDocumentApplicationService.batchCancelReceive(documentType, request, orgId);
        return CodeDataResponse.ok();
    }

    @PostMapping("/applications/review-lines")
    public CodeDataResponse<Void> reviewApplicationLines(@RequestParam(required = false) String orgId,
                                                         @RequestBody LineReviewBatchRequest request) {
        purchaseDocumentApplicationService.reviewApplicationLines(request, orgId);
        return CodeDataResponse.ok();
    }

    @PostMapping("/applications/update-lines")
    public CodeDataResponse<Void> updateApplicationLines(@RequestParam(required = false) String orgId,
                                                         @RequestBody LineUpdateBatchRequest request) {
        purchaseDocumentApplicationService.updateApplicationLines(request, orgId);
        return CodeDataResponse.ok();
    }
}
