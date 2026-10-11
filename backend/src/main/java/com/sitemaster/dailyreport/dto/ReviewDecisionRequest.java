package com.sitemaster.dailyreport.dto;

import jakarta.validation.constraints.Size;

/**
 * PM's remarks when approving (optional) or rejecting (required) a report or request.
 * <p>
 * OOP concept: Encapsulation - private fields with explicit accessors.
 */
public class ReviewDecisionRequest {

    @Size(max = 500, message = "{note.size}")
    private String remarks;

    /** Material requests only: the PM saw the budget-overrun warning and still approves. */
    private boolean acknowledgeOverrun;

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    public boolean isAcknowledgeOverrun() {
        return acknowledgeOverrun;
    }

    public void setAcknowledgeOverrun(boolean acknowledgeOverrun) {
        this.acknowledgeOverrun = acknowledgeOverrun;
    }
}
