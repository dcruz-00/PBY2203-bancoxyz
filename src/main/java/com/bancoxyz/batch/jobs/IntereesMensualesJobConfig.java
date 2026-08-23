package com.bancoxyz.batch.jobs;

import com.bancoxyz.batch.exception.DatoInvalidoException;
import com.bancoxyz.batch.listeners.RegistroDescartadoListener;
import com.bancoxyz.batch.model.CuentaInteres;
import com.bancoxyz.batch.processors.IntereesMensualesItemProcessor;
import com.bancoxyz.batch.readers.IntereesMensualesItemReader;
import com.bancoxyz.batch.writers.IntereesMensualesItemWriter;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.file.FlatFileItemReader;
import org.springframework.batch.infrastructure.item.file.FlatFileParseException;
import org.springframework.batch.infrastructure.item.database.JdbcBatchItemWriter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

@Configuration
public class IntereesMensualesJobConfig {

    @Bean
    public Job intereesMensualesJob(JobRepository jobRepository, Step intereesMensualesStep) {
        return new JobBuilder("intereesMensualesJob", jobRepository)
                .start(intereesMensualesStep)
                .build();
    }

    @Bean
    public Step intereesMensualesStep(JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            DataSource dataSource) {

        FlatFileItemReader<CuentaInteres> reader = IntereesMensualesItemReader.reader();
        JdbcBatchItemWriter<CuentaInteres> writer = IntereesMensualesItemWriter.writer(dataSource);

        return new StepBuilder("intereesMensualesStep", jobRepository)
                .<CuentaInteres, CuentaInteres>chunk(5, transactionManager)
                .reader(reader)
                .processor(new IntereesMensualesItemProcessor())
                .writer(writer)
                .faultTolerant()
                .skip(DatoInvalidoException.class)
                .skip(FlatFileParseException.class)
                .skipLimit(100)
                .listener(new RegistroDescartadoListener())
                .retry(TransientDataAccessException.class)
                .retryLimit(3)
                .build();
    }
}