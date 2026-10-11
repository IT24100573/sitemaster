package com.sitemaster.hr;

import com.sitemaster.common.dto.PageQuery;
import com.sitemaster.common.dto.PageResponse;
import com.sitemaster.file.FileDownload;
import com.sitemaster.hr.dto.ContractRequest;
import com.sitemaster.hr.dto.ContractResponse;
import com.sitemaster.hr.dto.TrainingRequest;
import com.sitemaster.hr.dto.TrainingResponse;
import org.springframework.web.multipart.MultipartFile;

/**
 * Employment contracts and training / safety records kept by HR.
 * <p>
 * OOP concept: Abstraction - controllers depend on this interface.
 */
public interface HrRecordsService {

    PageResponse<ContractResponse> contracts(Long employeeId, Integer expiringWithinDays, PageQuery pageQuery);

    ContractResponse createContract(ContractRequest request);

    ContractResponse updateContract(Long id, ContractRequest request);

    ContractResponse uploadContractFile(Long id, MultipartFile file);

    FileDownload openContractFile(Long id);

    PageResponse<TrainingResponse> training(Long employeeId, Boolean safety, PageQuery pageQuery);

    TrainingResponse createTraining(TrainingRequest request);

    TrainingResponse updateTraining(Long id, TrainingRequest request);

    TrainingResponse uploadCertificate(Long id, MultipartFile file);

    FileDownload openCertificate(Long id);

    void deleteTraining(Long id);

    /** Notifies HR once about contracts ending within the reminder window. */
    int sendRenewalReminders();
}
