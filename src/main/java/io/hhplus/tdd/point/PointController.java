package io.hhplus.tdd.point;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

@RestController
@RequestMapping("/point")
public class PointController {

    private static final Logger log = LoggerFactory.getLogger(PointController.class);

    @Autowired
    PointService pointService;

    /**
     * TODO - 특정 유저의 포인트를 조회하는 기능을 작성해주세요.
     */
    @GetMapping("{id}")
    public ResponseEntity<UserPoint> point(
            @PathVariable long id
    ) {

        UserPoint userPoint = pointService.point(id);

        return (userPoint != null) ?
                ResponseEntity.ok(userPoint) :
                ResponseEntity.notFound().build();
    }

    /**
     * TODO - 특정 유저의 포인트 충전/이용 내역을 조회하는 기능을 작성해주세요.
     */
    @GetMapping("{id}/histories")
    public ResponseEntity<List<PointHistory>> history(
            @PathVariable long id
    ) {

        List<PointHistory> list = pointService.history(id);

        return (list != null)?
                ResponseEntity.ok(list) :
                ResponseEntity.notFound().build();
    }

    /**
     * TODO - 특정 유저의 포인트를 충전하는 기능을 작성해주세요.
     */
    @PatchMapping("{id}/charge")
    public ResponseEntity<UserPoint> charge(
            @PathVariable long id,
            @RequestBody UserPoint request
    ) {

        UserPoint userPoint = pointService.charge(id, request.point());

        return (userPoint != null) ?
                ResponseEntity.ok(userPoint) :
                ResponseEntity.notFound().build();
    }

    /**
     * TODO - 특정 유저의 포인트를 사용하는 기능을 작성해주세요.
     */
    @PatchMapping("{id}/use")
    public ResponseEntity<UserPoint> use(
            @PathVariable long id,
            @RequestBody UserPoint request
    ) {
        UserPoint userPoint = pointService.use(id, request.point());

        return (userPoint != null)?
                ResponseEntity.ok(userPoint) :
                ResponseEntity.notFound().build();
    }
}
