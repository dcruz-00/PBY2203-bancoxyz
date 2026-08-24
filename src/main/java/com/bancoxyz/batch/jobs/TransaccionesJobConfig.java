package com.bancoxyz.batch.jobs;

import com.bancoxyz.batch.exception.DatoInvalidoException;
import com.bancoxyz.batch.listeners.RegistroDescartadoListener;
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
import org.springframework.batch.infrastructure.item.file.FlatFileParseException;
import org.springframework.batch.infrastructure.item.ItemStreamReader;
import org.springframework.batch.infrastructure.item.database.JdbcBatchItemWriter;
import org.springframework.batch.infrastructure.item.support.builder.SynchronizedItemStreamReaderBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.dao.TransientDataAccessException;
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
            DataSource dataSource,
            TaskExecutor batchTaskExecutor) {

        ItemStreamReader<Transaccion> reader = new SynchronizedItemStreamReaderBuilder<Transaccion>()
                .delegate(TransaccionesItemReader.reader())
                .build();
        JdbcBatchItemWriter<Transaccion> writer = TransaccionesItemWriter.writer(dataSource);

        return new StepBuilder("transaccionesStep", jobRepository)
                .<Transaccion, Transaccion>chunk(5, transactionManager)
                .reader(reader)
                .processor(new TransaccionesItemProcessor())
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