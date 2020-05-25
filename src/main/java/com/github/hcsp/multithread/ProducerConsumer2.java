package com.github.hcsp.multithread;

import java.util.Optional;
import java.util.Random;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class ProducerConsumer2 {
    public static void main(String[] args) throws InterruptedException {
        ReentrantLock lock = new ReentrantLock();
        Container container = new Container(lock);

        Producer producer = new Producer(lock, container);
        Consumer consumer = new Consumer(lock, container);

        producer.start();
        consumer.start();

        producer.join();
        producer.join();
    }

    static class Container {
        Condition notProducerYet;
        Condition notConsumerYet;
        private Optional<Integer> value = Optional.empty();

        Container(ReentrantLock lock) {
            this.notProducerYet = lock.newCondition();
            this.notConsumerYet = lock.newCondition();
        }

        Condition getNotProducerYet() {
            return notProducerYet;
        }

        Condition getNotConsumerYet() {
            return notConsumerYet;
        }

        Optional getValue() {
            return value;
        }

        void setValue(Optional<Integer> value) {
            this.value = value;
        }
    }

    public static class Producer extends Thread {
        ReentrantLock lock;
        Container container;

        Producer(ReentrantLock lock, Container container) {
            this.lock = lock;
            this.container = container;
        }

        @Override
        public void run() {
            for (int i = 0; i < 10; i++) {
                try {
                    lock.lock();
                    while (container.getValue().isPresent()) {
                        container.getNotProducerYet().await();
                    }

                    int r = new Random().nextInt();
                    System.out.println("Producing " + r);
                    container.setValue(Optional.of(r));
                    container.notConsumerYet.signal();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    lock.unlock();
                }
            }
        }
    }

    public static class Consumer extends Thread {
        ReentrantLock lock;
        Container container;

        public Consumer(ReentrantLock lock, Container container) {
            this.lock = lock;
            this.container = container;
        }

        @Override
        public void run() {
            for (int i = 0; i < 10; i++) {
                try {
                    lock.lock();
                    while (!container.getValue().isPresent()) {
                        container.getNotConsumerYet().await();
                    }
                    System.out.println("Consuming " + container.getValue().get());
                    container.setValue(Optional.empty());
                    container.notProducerYet.signal();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    lock.unlock();
                }

            }

        }
    }
}
