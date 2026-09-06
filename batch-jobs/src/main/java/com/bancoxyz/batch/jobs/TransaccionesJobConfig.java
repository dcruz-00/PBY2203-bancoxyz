package com.bancoxyz.batch.jobs;

import javax.sql.DataSource;

import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.partition.Partitioner;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.ItemStreamReader;
import org.springframework.batch.infrastructure.item.database.JdbcBatchItemWriter;
import org.springframework.batch.infrastructure.item.file.FlatFileParseException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.task.TaskExecutor;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.transaction.PlatformTransactionManager;

import com.bancoxyz.batch.exception.DatoInvalidoException;
import com.bancoxyz.batch.listeners.RegistroDescartadoListener;
import com.bancoxyz.batch.model.Transaccion;
import com.bancoxyz.batch.partition.TransaccionesPartitioner;
import com.bancoxyz.batch.processors.TransaccionesItemProcessor;
import com.bancoxyz.batch.readers.TransaccionesItemReader;
import com.bancoxyz.batch.writers.TransaccionesItemWriter;

@Configuration
public class TransaccionesJobConfig {

        @Bean
        public Job transaccionesJob(JobRepository jobRepository, Step transaccionesPartitionStep) {
                return new JobBuilder("transaccionesJob", jobRepository)
                                .start(transaccionesPartitionStep)
                                .build();
        }

        @Bean
        public Step transaccionesPartitionStep(JobRepository jobRepository,
                        Step transaccionesMinionStep,
                        TaskExecutor batchTaskExecutor,
                        @Value("${batch.transacciones.grid-size:3}") int gridSize) {

                Partitioner partitioner = new TransaccionesPartitioner(
                                new ClassPathResource("data/transacciones.csv"));

                return new StepBuilder("transaccionesPartitionStep", jobRepository)
                                .partitioner("transaccionesMinionStep", partitioner)
                                .step(transaccionesMinionStep)
                                .taskExecutor(batchTaskExecutor)
                                .gridSize(gridSize)
                                .build();
        }

        @Bean
        public Step transaccionesMinionStep(JobRepository jobRepository,
                        PlatformTransactionManager transactionManager,
                        DataSource dataSource,
                        ItemStreamReader<Transaccion> transaccionesPartitionReader) {

                JdbcBatchItemWriter<Transaccion> writer = TransaccionesItemWriter.writer(dataSource);

                return new StepBuilder("transaccionesMinionStep", jobRepository)
                                .<Transaccion, Transaccion>chunk(5, transactionManager)
                                .reader(transaccionesPartitionReader)
                                .processor(new TransaccionesItemProcessor())
                                .writer(writer)
                                .faultTolerant()
                                .skip(DatoInvalidoException.class)
                                .skip(FlatFileParseException.class)
                                .noRollback(DatoInvalidoException.class)
                                .skipLimit(100)
                                .listener(new RegistroDescartadoListener())
                                .retry(TransientDataAccessException.class)
                                .retryLimit(3)
                                .build();
        }

        @Bean
        @StepScope
        public ItemStreamReader<Transaccion> transaccionesPartitionReader(
                        @Value("#{stepExecutionContext['fromItem']}") int fromItem,
                        @Value("#{stepExecutionContext['toItem']}") int toItem) {

                return TransaccionesItemReader.reader(fromItem, toItem);
        }
}