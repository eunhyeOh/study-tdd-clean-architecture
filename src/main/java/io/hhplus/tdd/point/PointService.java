package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class PointService {

    @Autowired
    UserPointTable userPointTable;

    @Autowired
    PointHistoryTable pointHistoryTable;

    private final long MAX_POINT = 10000L;

    /**
     * 각 ID에 대해 ReentrantLock을 저장하는 맵
     * ID마다 별도의 락을 관리하는 용도
     * */
    private final ConcurrentHashMap<Long, ReentrantLock> locks = new ConcurrentHashMap<>();

    /**
     * ID별로 lock를 생성하거나 기존 lock를 반환하는 메소드
     * */
    private ReentrantLock getLock(Long id) {
        locks.putIfAbsent(id, new ReentrantLock());
        return locks.get(id);
    }


    /**
     * id로 userPoint 조회 반환
     * 예외 케이스 1.조회 값이 null
     * */
    public UserPoint point(Long id) {
        UserPoint userPoint = userPointTable.selectById(id);
        if (userPoint == null) {
            throw new NullPointerException("조회되지 않는 아이디입니다.");
        }

        return userPoint;
    }


    /**
     * id로 PointHistory 조회해 List로 반환
     * 예외 케이스 1.조회 값이 null
     * */
    public List<PointHistory> history(long id) {
        List<PointHistory> listPointHistory = pointHistoryTable.selectAllByUserId(id);
        if(listPointHistory == null) {
            throw new NullPointerException("이력이 없습니다.");
        }

        return listPointHistory;
    }

    /**
     * id로 정보 조회후 기존 point와 amount를 합산하여 갱신(또는 등록) & 이력 추가
     * 예외 케이스 1.등록되지 않은 id
     * 예외 케이스 2.충전 가능한 최대 포인트(10000L)를 초과
     * 예외 케이스 3.데이터 등록/갱신 처리 실패
     * */
    public UserPoint charge(long id, long amount) {

        ReentrantLock lock = getLock(id);
        lock.lock(); //id에 대해 락을 걸어서 다른 요청이 해당 유저에 처리되지 않도록 함

        try {

            UserPoint existingUserPoint = userPointTable.selectById(id);
            if(existingUserPoint == null) {
                throw new IllegalArgumentException("[포인트 충전 실패] 등록된 id가 아닙니다.");
            }

            long updateAmount = amount + existingUserPoint.point();
            if(updateAmount > MAX_POINT) {
                throw new IllegalArgumentException("[포인트 충전 실패] 충전 가능한 최대 포인트를 초과했습니다.");
            }

            UserPoint updated = userPointTable.insertOrUpdate(id, updateAmount);
            if(updated == null) {
                throw new IllegalStateException("[포인트 충전 실패] 유저 포인트가 정상적으로 업데이트되지 않았습니다.");
            }

            pointHistoryTable.insert(updated.id(), updated.point(), TransactionType.CHARGE, updated.updateMillis());

            return updated;

        } finally {
            lock.unlock(); //처리 후 락 해제(다른 스레드가 해당 id에 대해 처리할 수 있게 함)
        }

    }

    /**
     * id로 정보 조회후 기존 point에서 amount를 뻰 차액 갱신 & 이력 추가
     * 예외 케이스 1.등록되지 않은 id
     * 예외 케이스 2.사용 가능한 포인트가 부족(파라미터 amount > userPointTable point)
     * 예외 케이스 3.데이터 등록/갱신 처리 실패
     * */
    public UserPoint use(long id, long amount) {

        ReentrantLock lock = getLock(id);
        lock.lock(); //id에 대해 락을 걸어서 다른 요청이 해당 유저에 처리되지 않도록 함

        try {

            UserPoint existingUserPoint = userPointTable.selectById(id);
            if(existingUserPoint == null) {
                throw new IllegalArgumentException("[포인트 사용 실패] 등록된 id가 아닙니다.");
            }

            if(existingUserPoint.point() - amount < 0) {
                throw new IllegalArgumentException("[포인트 사용 실패] 사용 가능한 포인트가 부족합니다.");
            }
            long updateAmount = existingUserPoint.point() - amount;

            UserPoint updated = userPointTable.insertOrUpdate(id, updateAmount);
            if(updated == null) {
                throw new IllegalStateException("[포인트 사용 실패] 유저 포인트가 정상적으로 업데이트되지 않았습니다.");
            }

            pointHistoryTable.insert(updated.id(), updated.point(), TransactionType.USE, updated.updateMillis());

            return updated;

        } finally {
            lock.unlock();
        }

    }
}
