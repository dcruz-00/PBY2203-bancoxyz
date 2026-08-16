package com.bancoxyz.batch.jobs;

import com.bancoxyz.batch.model.Transaccion;
import com.bancoxyz.batch.processors.TransaccionesItemProcessor;
import com.bancoxyz.batch.readers.TransaccionesItemReader;
import com.bancoxyz.batch.writers.TransaccionesItemWriter;
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
public class TransaccionesJobConfig {

    @Bean
    public Job transaccionesJob(JobRepository jobRepository, Step transaccionesStep) {
        return new JobBuilder("transaccionesJob", jobRepository)
                .start(transaccionesStep)
                .build();
    }

    @Bean
    public Step transaccionesStep(JobRepository jobRepository,
                                   PlatformTransactionManager transactionManager,
                                   DataSource dataSource) {

        FlatFileItemReader<Transaccion> reader = TransaccionesItemReader.reader();
        JdbcBatchItemWriter<Transaccion> writer = TransaccionesItemWriter.writer(dataSource);

        return new StepBuilder("transaccionesStep", jobRepository)
                .<Transaccion, Transaccion>chunk(5)
                .transactionManager(transactionManager)
                .reader(reader)
                .processor(new TransaccionesItemProcessor())
                .writer(writer)
                .build();
    }
}