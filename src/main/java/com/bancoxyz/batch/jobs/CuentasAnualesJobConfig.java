package com.bancoxyz.batch.jobs;

import com.bancoxyz.batch.model.CuentaAnual;
import com.bancoxyz.batch.processors.CuentasAnualesItemProcessor;
import com.bancoxyz.batch.readers.CuentasAnualesItemReader;
import com.bancoxyz.batch.writers.CuentasAnualesItemWriter;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.file.FlatFileItemReader;
import org.springframework.batch.infrastructure.item.database.JdbcBatchItemWriter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

@Configuration
public class CuentasAnualesJobConfig {

    @Bean
    public Job cuentasAnualesJob(JobRepository jobRepository, Step cuentasAnualesStep) {
        return new JobBuilder("cuentasAnualesJob", jobRepository)
                .start(cuentasAnualesStep)
                .build();
    }

    @Bean
    public Step cuentasAnualesStep(JobRepository jobRepository,
                                    PlatformTransactionManager transactionManager,
                                    DataSource dataSource) {

        FlatFileItemReader<CuentaAnual> reader = CuentasAnualesItemReader.reader();
        JdbcBatchItemWriter<CuentaAnual> writer = CuentasAnualesItemWriter.writer(dataSource);

        return new StepBuilder("cuentasAnualesStep", jobRepository)
                .<CuentaAnual, CuentaAnual>chunk(5)
                .transactionManager(transactionManager)
                .reader(reader)
                .processor(new CuentasAnualesItemProcessor())
                .writer(writer)
                .build();
    }
}