package com.stackroute.quartz.controller;

import com.stackroute.quartz.job.CronJobProducer;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.quartz.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.ZonedDateTime;
import java.util.Date;
import java.util.UUID;

@RestController
@NoArgsConstructor
@AllArgsConstructor
public class JobSchedulerController {
    private static final Logger logger = LoggerFactory.getLogger(JobSchedulerController.class);

    @Autowired
    private Scheduler scheduler;

    @Value("${description}")
    private String description;

    @Value("$(scheduling)")
    private String scheduling;

    @Value("$(FlagScheduler)")
    private String FlagScheduler;

    @Value("$(dataPopulating)")
    private String dataPopulating;

    @GetMapping("/simplePrint")
    @Scheduled(fixedRate = 10000) // This will write true on kafka on every 10 seconds.

    public void schedulePopulationInDatabase() {
        try {
            ZonedDateTime dateTime = ZonedDateTime.now();
            JobDetail jobDetail = buildJobDetail("true");
            Trigger trigger = buildJobTrigger(jobDetail, dateTime);
            scheduler.scheduleJob(jobDetail, trigger);
        } catch (SchedulerException ex) {
            logger.error(scheduling, ex);
        }
    }

    // This function is for buliding job data
    private JobDetail buildJobDetail(String valueOfFlag) {
        JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put(FlagScheduler,valueOfFlag);

        return JobBuilder.newJob(CronJobProducer.class)
                .withIdentity(UUID.randomUUID().toString(), dataPopulating)
                .withDescription(description)
                .usingJobData(jobDataMap)
                .storeDurably()
                .build();
    }

    // This function is for triggering job.
    private Trigger buildJobTrigger(JobDetail jobDetail, ZonedDateTime startAt) {
        return TriggerBuilder.newTrigger()
                .forJob(jobDetail)
                .withIdentity(jobDetail.getKey().getName(), dataPopulating)
                .withDescription(description)
                .startAt(Date.from(startAt.toInstant()))
                .withSchedule(SimpleScheduleBuilder.simpleSchedule().withMisfireHandlingInstructionFireNow())
                .build();
    }
}
