package com.bancoxyz.batch.jobs;

import javax.sql.DataSource;

import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.ItemStreamReader;
import org.springframework.batch.infrastructure.item.database.JdbcBatchItemWriter;
import org.springframework.batch.infrastructure.item.file.FlatFileParseException;
import org.springframework.batch.infrastructure.item.support.builder.SynchronizedItemStreamReaderBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.transaction.PlatformTransactionManager;

import com.bancoxyz.batch.exception.DatoInvalidoException;
import com.bancoxyz.batch.listeners.RegistroDescartadoListener;
import com.bancoxyz.batch.model.CuentaInteres;
import com.bancoxyz.batch.processors.InteresesMensualesItemProcessor;
import com.bancoxyz.batch.readers.InteresesMensualesItemReader;
import com.bancoxyz.batch.writers.InteresesMensualesItemWriter;

@Configuration
public class InteresesMensualesJobConfig {

    @Bean
    public Job interesesMensualesJob(JobRepository jobRepository, Step interesesMensualesStep) {
        return new JobBuilder("interesesMensualesJob", jobRepository)
                .start(interesesMensualesStep)
                .build();
    }

    @Bean
    public Step interesesMensualesStep(JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            DataSource dataSource,
            TaskExecutor batchTaskExecutor) {

        ItemStreamReader<CuentaInteres> reader = new SynchronizedItemStreamReaderBuilder<CuentaInteres>()
                .delegate(InteresesMensualesItemReader.reader())
                .build();
        JdbcBatchItemWriter<CuentaInteres> writer = InteresesMensualesItemWriter.writer(dataSource);

        return new StepBuilder("interesesMensualesStep", jobRepository)
                .<CuentaInteres, CuentaInteres>chunk(5, transactionManager)
                .reader(reader)
                .processor(new InteresesMensualesItemProcessor())
                .writer(writer)
                .faultTolerant()
                .skip(DatoInvalidoException.class)
                .skip(FlatFileParseException.class)
                .skipLimit(100)
                .listener(new RegistroDescartadoListener())
                .retry(TransientDataAccessException.class)
                .retryLimit(3)
                .taskExecutor(batchTaskExecutor)
                .build();
    }
}