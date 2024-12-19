package io.hhplus.tdd.point;

import io.hhplus.tdd.database.UserPointTable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@SpringBootTest
public class PointServiceConcurrencyTest {

    @Autowired
    private PointService pointService;

    @Autowired
    private UserPointTable userPointTable;

    private final long MAX_POINT = 10000L;


    @Test
    @DisplayName("charge : 한 id로 여러번 포인트 충전 병렬 요청후 데이터 정합성 확인")
    void charge_concurrency_test() throws InterruptedException {
        //given
        long id = 1L;
        pointService.charge(id, 0); //초기 포인트 0으로 설정

        //병렬 작업 설정
        int numberOfThreads = 10; //병렬 요청 수
        long chargeAmount = 2000L;//충전 포인트
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch countDownLatch = new CountDownLatch(numberOfThreads);

        //when
        //병별로 충전 요청
        for (int i = 0; i < numberOfThreads; i++) {
            executorService.submit(() -> {
                try{
                    pointService.charge(id, chargeAmount);
                }
                finally {
                    countDownLatch.countDown(); //완료시 래치 감소
                }
          });
        }

        //모드 요청 완료 대기
        countDownLatch.await();
        executorService.shutdown();

        //executorService 종료까지 대기(Test 메소드들의 동시성 문제해결하기 위해)
        if(!executorService.awaitTermination(60, TimeUnit.SECONDS)){
            executorService.shutdownNow();
        }

        //then
        long finalPoint = pointService.point(id).point(); //10번 * 1000 = 10000(MAX_POINT)
        Assertions.assertEquals(MAX_POINT, finalPoint, "최종 포인트는 최대 포인트를 초과하지 않아야 합니다.");
    }

    @Test
    @DisplayName("charge : 서로 다른 id가 동시에 요청을 보낼때 병렬 처리 확인")
    void charge_concurrency_different_ids() throws InterruptedException {
        //given
        long idA = 2L;
        long idB = 3L;
        pointService.charge(idA, 0);
        pointService.charge(idB, 0);

        //병렬 작업 설정
        int numberOfThreads = 2; //병렬 요청 수
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch countDownLatch = new CountDownLatch(numberOfThreads);

        //when
        //각각 사용 요청
        executorService.submit(() -> {
            try{
                pointService.charge(idA, 500L);
            } finally {
                countDownLatch.countDown();
            }
        });

        executorService.submit(() -> {
            try{
                pointService.charge(idB, 500L);
            }finally {
                countDownLatch.countDown();
            }
        });

        countDownLatch.await();
        executorService.shutdown();

        //executorService 종료까지 대기(Test 메소드들의 동시성 문제해결하기 위해)
        if(!executorService.awaitTermination(60, TimeUnit.SECONDS)){
            executorService.shutdownNow();
        }

        //then
        Assertions.assertEquals(500L, pointService.point(idA).point(), "유저 A의 최종 포인트는 500이어야 합니다.");
        Assertions.assertEquals(500L, pointService.point(idB).point(), "유저 B의 최종 포인트는 500이어야 합니다.");
    }


    @Test
    @DisplayName("use : 한 id로 여러번 포인트 사용 병렬 요청후 데이터 정합성 확인")
    void use_concurrent_test() throws InterruptedException {
        //given
        long id = 4L;
        pointService.charge(id, 1000L);

        //병렬 작업 설정
        int numberOfThreads = 2; //병렬 요청 수
        long useAmount = 500L;//사용 포인트
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch countDownLatch = new CountDownLatch(numberOfThreads);

        //when
        for (int i = 0; i < numberOfThreads; i++) {
            executorService.submit(() -> {
                try {
                    pointService.use(id, useAmount);
                } finally {
                    countDownLatch.countDown(); //요청 하나 끝날 때마다 래치 감소
                }
            });
        }

        //모드 요청 완료 대기
        countDownLatch.await();
        executorService.shutdown();

        //executorService 종료까지 대기(Test 메소드들의 동시성 문제해결하기 위해)
        if(!executorService.awaitTermination(60, TimeUnit.SECONDS)){
            executorService.shutdownNow();
        }

        //then
        long finalPoint = pointService.point(id).point();
        Assertions.assertEquals(0, finalPoint, "최종포인트는 0이어야 합니다.");
    }


    @Test
    @DisplayName("use : 서로 다른 id가 동시에 요청을 보낼때 병렬 처리 확인")
    void use_concurrency_different_ids() throws InterruptedException {
        //given
        long idA = 5L;
        long idB = 6L;
        pointService.charge(idA, 1000L);
        pointService.charge(idB, 1000L);

        //병렬 작업 설정
        int numberOfThreads = 2; //병렬 요청 수
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch countDownLatch = new CountDownLatch(numberOfThreads);

        //when
        //각각 사용 요청
        executorService.submit(() -> {
            try{
                pointService.use(idA, 500L);
            } finally {
                countDownLatch.countDown();
            }
        });

        executorService.submit(() -> {
            try{
                pointService.use(idB, 500L);
            }finally {
                countDownLatch.countDown();
            }
        });

        countDownLatch.await();
        executorService.shutdown();

        //executorService 종료까지 대기(Test 메소드들의 동시성 문제해결하기 위해)
        if(!executorService.awaitTermination(60, TimeUnit.SECONDS)){
            executorService.shutdownNow();
        }

        //then
        Assertions.assertEquals(500L, pointService.point(idA).point(), "유저 A의 최종 포인트는 500이어야 합니다.");
        Assertions.assertEquals(500L, pointService.point(idB).point(), "유저 B의 최종 포인트는 500이어야 합니다.");
    }
}
