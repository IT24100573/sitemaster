package com.sitemaster.hr;

import com.sitemaster.common.dto.MessageResponse;
import com.sitemaster.common.dto.PageQuery;
import com.sitemaster.common.dto.PageResponse;
import com.sitemaster.file.FileCategory;
import com.sitemaster.file.FileResponses;
import com.sitemaster.hr.dto.ContractRequest;
import com.sitemaster.hr.dto.ContractResponse;
import com.sitemaster.hr.dto.TrainingRequest;
import com.sitemaster.hr.dto.TrainingResponse;
import com.sitemaster.validation.ValidFile;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** Contracts and training records (HR; Admin can view). */
@RestController
public class HrRecordsController {

    private final HrRecordsService service;

    public HrRecordsController(HrRecordsService service) {
        this.service = service;
    }

    @GetMapping("/api/contracts")
    @PreAuthorize("hasAuthority('EMPLOYEE:VIEW')")
    public PageResponse<ContractResponse> contracts(@RequestParam(required = false) Long employeeId,
                                                    @RequestParam(required = false) @Min(value = 1, message = "{days.range}") @Max(value = 365, message = "{days.range}") Integer expiringWithinDays,
                                                    @Valid @ModelAttribute PageQuery pageQuery) {
        return service.contracts(employeeId, expiringWithinDays, pageQuery);
    }

    @PostMapping("/api/contracts")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('EMPLOYEE:EDIT')")
    public ContractResponse createContract(@Valid @RequestBody ContractRequest request) {
        return service.createContract(request);
    }

    @PutMapping("/api/contracts/{id}")
    @PreAuthorize("hasAuthority('EMPLOYEE:EDIT')")
    public ContractResponse updateContract(@PathVariable Long id, @Valid @RequestBody ContractRequest request) {
        return service.updateContract(id, request);
    }

    @PostMapping(value = "/api/contracts/{id}/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('EMPLOYEE:EDIT')")
    public ContractResponse uploadContractFile(@PathVariable Long id,
                                               @RequestPart("file") @ValidFile(category = FileCategory.DOCUMENT) MultipartFile file) {
        return service.uploadContractFile(id, file);
    }

    @GetMapping("/api/contracts/{id}/file")
    @PreAuthorize("hasAuthority('EMPLOYEE:VIEW')")
    public ResponseEntity<Resource> contractFile(@PathVariable Long id) {
        return FileResponses.attachment(service.openContractFile(id));
    }

    @GetMapping("/api/training-records")
    @PreAuthorize("hasAuthority('EMPLOYEE:VIEW')")
    public PageResponse<TrainingResponse> training(@RequestParam(required = false) Long employeeId,
                                                   @RequestParam(required = false) Boolean safety,
                                                   @Valid @ModelAttribute PageQuery pageQuery) {
        return service.training(employeeId, safety, pageQuery);
    }

    @PostMapping("/api/training-records")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('EMPLOYEE:EDIT')")
    public TrainingResponse createTraining(@Valid @RequestBody TrainingRequest request) {
        return service.createTraining(request);
    }

    @PutMapping("/api/training-records/{id}")
    @PreAuthorize("hasAuthority('EMPLOYEE:EDIT')")
    public TrainingResponse updateTraining(@PathVariable Long id, @Valid @RequestBody TrainingRequest request) {
        return service.updateTraining(id, request);
    }

    @PostMapping(value = "/api/training-records/{id}/certificate", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('EMPLOYEE:EDIT')")
    public TrainingResponse uploadCertificate(@PathVariable Long id,
                                              @RequestPart("file") @ValidFile(category = FileCategory.DOCUMENT) MultipartFile file) {
        return service.uploadCertificate(id, file);
    }

    @GetMapping("/api/training-records/{id}/certificate")
    @PreAuthorize("hasAuthority('EMPLOYEE:VIEW')")
    public ResponseEntity<Resource> certificate(@PathVariable Long id) {
        return FileResponses.attachment(service.openCertificate(id));
    }

    @DeleteMapping("/api/training-records/{id}")
    @PreAuthorize("hasAuthority('EMPLOYEE:DELETE')")
    public MessageResponse deleteTraining(@PathVariable Long id) {
        service.deleteTraining(id);
        return new MessageResponse("Training record deleted.");
    }
}
