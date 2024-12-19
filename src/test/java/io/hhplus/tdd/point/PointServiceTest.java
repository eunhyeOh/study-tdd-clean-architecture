package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PointServiceTest {

    //@Mock : 실제 DB나 외부 시스템과 상호작용하지 않고, 가상의 동작을 하도록 함
    @Mock
    private UserPointTable userPointTable;

    @Mock
    private PointHistoryTable pointHistoryTable;

    //@InjectMocks : 목 객체를 주입받아 실제 테스트 대상 클래스를 생성
    @InjectMocks
    private PointService pointService;


    @Test
    @DisplayName("[S] point : 조회 데이터 반환 확인")
    void point_should_return_user_point_when_exists() {

        //given : 테스트 데이터 설정, 목 동작 정의
        long userId = 1L;
        UserPoint mockUserPoint = new UserPoint(userId, 100L, 0);//가짜 UserPoint 객체 생성
        when(userPointTable.selectById(userId)).thenReturn(mockUserPoint); //목 객체의 동작 설정
        //given(userPointTable.selectById(userId)).willReturn(mockUserPoint);

        //when : 테스트 대상 메서드 호출
        UserPoint result = pointService.point(userId);

        //then : 결과 검증, 목 객체 호출 확인
        Assertions.assertNotNull(result); //반환 값이 null인지 확인
        Assertions.assertEquals(userId, result.id());
        Assertions.assertEquals(100L, result.point());
        verify(userPointTable, times(1)).selectById(userId); //selectById가 1번 호출되었는지 확인
    }

    @Test
    @DisplayName("[E] point : null 반환시 예외처리 확인")
    void point_should_handle_null_when_not_exists() {
        //given
        long userId = 1L;
        when(userPointTable.selectById(userId)).thenReturn(null);

        // when & then
        Assertions.assertThrows(NullPointerException.class, () -> pointService.point(userId)); //예외 발생 여부 확인
    }

    @Test
    @DisplayName("[S] history : 데이터 반환 확인")
    void history_should_return_point_history_list_when_exists() {
        //given : 짭 데이터 설정
        long userId = 1L;
        List<PointHistory> mockPointHistoryList = List.of(
                new PointHistory(1, userId, 100L, TransactionType.CHARGE, System.currentTimeMillis()),
                new PointHistory(2, userId, -50L, TransactionType.USE, System.currentTimeMillis())
        );

        when(pointHistoryTable.selectAllByUserId(userId)).thenReturn(mockPointHistoryList);

        //when : 메서드 호출
        List<PointHistory> result = pointService.history(userId);

        //then : 결과 검증
        Assertions.assertNotNull(result);
        Assertions.assertArrayEquals(mockPointHistoryList.toArray(), result.toArray());
        verify(pointHistoryTable, times(1)).selectAllByUserId(userId);
    }

    @Test
    @DisplayName("[S] charge : 충전 성공시 포인트 업데이트 및 히스토리 저장")
    void charge_should_update_user_point_and_insert_history(){
        //given : 짭 데이터 생성
        long id = 1L;
        long existingPoint  = 100L;
        long chargePoint = 50L;
        UserPoint existingUserPoint = new UserPoint(id, existingPoint , System.currentTimeMillis()); //기존 데이터
        UserPoint updatedUserPoint = new UserPoint(id, existingPoint + chargePoint, System.currentTimeMillis());

        //mocking
        when(userPointTable.selectById(id)).thenReturn(existingUserPoint);
        when(userPointTable.insertOrUpdate(id, existingPoint + chargePoint)).thenReturn(updatedUserPoint);

        //when : 메서드 호출
        UserPoint result = pointService.charge(id, chargePoint);

        //then : 결과 검증
        Assertions.assertNotNull(result);
        Assertions.assertEquals(id, result.id());
        Assertions.assertEquals(150L, result.point()); //반환된 포인트 값 확인

        verify(userPointTable, times(1)).selectById(id); //실행 횟수 검증
        verify(userPointTable, times(1)).insertOrUpdate(id, existingPoint + chargePoint);
        verify(pointHistoryTable, times(1)).insert(eq(id), eq(150L), eq(TransactionType.CHARGE), anyLong()); //이력 추가 확인
    }


    @Test
    @DisplayName("[E] charge : 충전시 최대 포인트 초과시 예외")
    void charge_should_throw_exception_when_exceeds_max_point(){
        //given
        long id = 1L;
        long existingPoint = 9000L; //보유 포인트
        long chargePoint = 2000L; //충전할 포인트
        UserPoint existingUserPoint = new UserPoint(id, existingPoint, System.currentTimeMillis());

        //mocking
        when(userPointTable.selectById(id)).thenReturn(existingUserPoint);

        //then
        IllegalArgumentException exception = Assertions.assertThrows(IllegalArgumentException.class, () -> {
            pointService.charge(id, chargePoint);
        }); //예외발생 예상

        Assertions.assertEquals("[포인트 충전 실패] 충전 가능한 최대 포인트를 초과했습니다.", exception.getMessage());
    }

    @Test
    @DisplayName("[E] charge : 존재하지 않는 id로 충전 시 예외")
    void charge_should_throw_exception_when_user_not_found(){
        //given
        long id = 1L;
        long chargePoint = 100L;

        //mocking
        when(userPointTable.selectById(id)).thenReturn(null);

        //then
        IllegalArgumentException exception = Assertions.assertThrows(IllegalArgumentException.class, () -> {
           pointService.charge(id, chargePoint);
        });

        Assertions.assertEquals("[포인트 충전 실패] 등록된 id가 아닙니다." , exception.getMessage());
    }

    @Test
    @DisplayName("[E] charge : 업데이트 실패시 예외")
    void charge_should_throw_exception_when_insertOrUpdate_returns_null(){
        //given
        long id = 1L;
        long chargePoint = 8000L;
        long existingPoint = 1000L;
        UserPoint existingUserPoint = new UserPoint(id, existingPoint, System.currentTimeMillis());

        //mocking
        when(userPointTable.selectById(id)).thenReturn(existingUserPoint);
        when(userPointTable.insertOrUpdate(existingUserPoint.id(), existingUserPoint.point() + chargePoint))
                .thenReturn(null);

        //when & then
        IllegalStateException exception = Assertions.assertThrows(IllegalStateException.class, () -> {
            pointService.charge(id, chargePoint);
        });
        Assertions.assertEquals("[포인트 충전 실패] 유저 포인트가 정상적으로 업데이트되지 않았습니다.", exception.getMessage());
    }

    @Test
    @DisplayName("[S] use : 사용 성공시 포인트 업데이트 및 히스토리 저장")
    void use_should_update_user_point_and_insert_history(){
        //given : 짭 데이터 생성
        long userId = 1L;
        long initialPoint = 100L;
        long amount = 30L;
        UserPoint existingUserPoint = new UserPoint(userId, initialPoint, System.currentTimeMillis());//기존 데이터
        UserPoint updatedUserPoint = new UserPoint(userId, initialPoint - amount, System.currentTimeMillis());

        when(userPointTable.selectById(userId)).thenReturn(existingUserPoint);
        when(userPointTable.insertOrUpdate(userId, initialPoint - amount)).thenReturn(updatedUserPoint);

        //when : 메서드 호출
        UserPoint result = pointService.use(userId, amount);

        //then : 결과 검증
        Assertions.assertNotNull(result);
        Assertions.assertEquals(userId, result.id());
        Assertions.assertEquals(70L, result.point()); //반환 포인트 비교

        verify(userPointTable, times(1)).selectById(userId);
        verify(userPointTable, times(1)).insertOrUpdate(userId, initialPoint - amount);
        verify(pointHistoryTable, times(1)).insert(eq(userId), eq(70L), eq(TransactionType.USE), anyLong());

    }


    @Test
    @DisplayName("[E] use : 사용시 포인트 부족하면 예외")
    void use_should_throw_exception_when_insufficient_points(){
        //given
        long id = 1L;
        long existingPoint = 1000L; //보유 포인트
        long usePoint = 7000L;      //사용할 포인트
        UserPoint existingUserPoint = new UserPoint(id, existingPoint, System.currentTimeMillis());

        //mocking
        when(userPointTable.selectById(id)).thenReturn(existingUserPoint);

        //then
        IllegalArgumentException exception = Assertions.assertThrows(IllegalArgumentException.class, () -> {
            pointService.use(id, usePoint);
        }); //예외발생 예상

        Assertions.assertEquals("[포인트 사용 실패] 사용 가능한 포인트가 부족합니다.", exception.getMessage());
    }

    @Test
    @DisplayName("[E] use : 존재하지 않는 id로 사용 시 예외")
    void use_should_throw_exception_when_user_not_found(){
        //given
        long id = 1L;
        long usePoint = 1000L;

        //mocking
        when(userPointTable.selectById(id)).thenReturn(null);

        //then
        IllegalArgumentException exception = Assertions.assertThrows(IllegalArgumentException.class, () -> {
            pointService.use(id, usePoint);
        });

        Assertions.assertEquals("[포인트 사용 실패] 등록된 id가 아닙니다." , exception.getMessage());
    }

    @Test
    @DisplayName("[E] use : 업데이트 실패시 예외")
    void use_should_throw_exception_when_insertOrUpdate_returns_null(){
        //given
        long id = 1L;
        long usePoint = 1000L;
        long existingPoint = 10000L;
        UserPoint existingUserPoint = new UserPoint(id, existingPoint, System.currentTimeMillis());

        //mocking
        when(userPointTable.selectById(id)).thenReturn(existingUserPoint);
        when(userPointTable.insertOrUpdate(existingUserPoint.id(), existingUserPoint.point() - usePoint))
                .thenReturn(null);

        //when & then
        IllegalStateException exception = Assertions.assertThrows(IllegalStateException.class, () -> {
            pointService.use(id, usePoint);
        });
        Assertions.assertEquals("[포인트 사용 실패] 유저 포인트가 정상적으로 업데이트되지 않았습니다.", exception.getMessage());
    }
}


