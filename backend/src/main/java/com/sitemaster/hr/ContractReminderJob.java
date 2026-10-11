package com.sitemaster.hr;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Every morning (and shortly after start-up) notifies HR about contracts ending
 * in the next 30 days. Each contract is reminded once.
 */
@Component
public class ContractReminderJob {

    private static final Logger LOG = LoggerFactory.getLogger(ContractReminderJob.class);

    private final HrRecordsService hrRecordsService;

    public ContractReminderJob(HrRecordsService hrRecordsService) {
        this.hrRecordsService = hrRecordsService;
    }

    @Scheduled(cron = "${sitemaster.scheduling.contract-reminder-cron:0 0 7 * * *}", zone = "Asia/Colombo")
    public void daily() {
        run();
    }

    @Scheduled(initialDelay = 60_000, fixedDelay = Long.MAX_VALUE)
    public void onStartup() {
        run();
    }

    private void run() {
        try {
            int sent = hrRecordsService.sendRenewalReminders();
            if (sent > 0) {
                LOG.info("Sent {} contract renewal reminder(s)", sent);
            }
        } catch (RuntimeException ex) {
            LOG.error("Contract renewal reminders failed", ex);
        }
    }
}
