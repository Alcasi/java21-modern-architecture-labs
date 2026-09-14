package loom;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class BenchmarkThreads {

    private static final int NUMERO_TAREFAS = 10_000;

    private static final Semaphore SEMAFORO = new Semaphore(200);

    public static void main(String[] args) {

        System.out.println("Iniciando benchmark...");
        System.out.println("Numero de tarefas: " + NUMERO_TAREFAS);
        System.out.println("---------------------------------------------------");
        System.out.println("Executando com Java 8 (Traditional Threads)...");
        codigoJava8();
        System.out.println("---------------------------------------------------");
        System.out.println("Executando com Java 21 (Virtual Threads)...");
        codigoJava21();
        System.out.println("---------------------------------------------------");
        System.out.println("Fim do benchmark!");

    }

    private static void codigoJava8() {

        Instant inicio = Instant.now();

        ExecutorService executor = Executors.newFixedThreadPool(200);

        for (int i = 0; i < NUMERO_TAREFAS; i++) {
            executor.submit(() -> {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            });
        }
        executor.shutdown();
        try {
            executor.awaitTermination(1, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Instant fim = Instant.now();
        Duration duracao = Duration.between(inicio, fim);

        System.out.println("O tempo de execução com Java 8 (Traditional Threads) é de: " + duracao.toMillis() + "ms");
    }

    private static void codigoJava21() {

        Instant inicio = Instant.now();

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {

            for (int i = 0; i < NUMERO_TAREFAS; i++) {
                executor.submit(() -> {
                    try {
                        SEMAFORO.acquire();
                        try {
                            Thread.sleep(1000);
                        } finally {
                            SEMAFORO.release();
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                });
            }
        }

        Instant fim = Instant.now();
        Duration duracao = Duration.between(inicio, fim);

        System.out.println("O tempo de execução com Java 21 (Virtual Threads) é de : " + duracao.toMillis() + "ms");

    }
}