package io.hhplus.tdd.point;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@WebMvcTest(PointController.class)
class PointControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PointService pointService;

    @MockBean
    private UserPoint mockUserPoint;

    @MockBean
    private PointHistory mockPointHistory;

    private UserPoint createUserPoint(long id, long point, long updateMillis) {
        return new UserPoint(id, point, updateMillis);
    }

    private PointHistory createMockPointHistory(long id, long userId, long amount, TransactionType type, long updateMillis) {
        return new PointHistory(id, userId, amount, type, updateMillis);
    }

    @BeforeEach
    void setUp() {
        mockUserPoint = createUserPoint(1L, 150L,System.currentTimeMillis()); //짭 데이터
        mockPointHistory = createMockPointHistory(1L, 12L, 100L, TransactionType.CHARGE, System.currentTimeMillis());
    }

    @Test
    @DisplayName("[S] point : UserPoint 조회 데이터 반환")
    void point_should_return_user_point_when_exists() throws Exception {

        //given
        long id = 1L;
        when(pointService.point(id)).thenReturn(mockUserPoint);

        //when & then 테스트 실행 & 결과 검증
        mockMvc.perform(get("/point/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.point").value(mockUserPoint.point()));
    }

    @Test
    @DisplayName("[E] point : UserPoint 조회시 null 예외")
    void point_should_return_not_found_when_not_exists() throws Exception {
        //given
        long id = 1L;
        when(pointService.point(id)).thenReturn(null);

        //when & then
        mockMvc.perform(get("/point/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("[S] history : PointHistory 조회 데이터 반환")
    void history_should_return_history_when_exists() throws Exception {
        //given
        long id = 1L;
        List<PointHistory> pointHistoryList = List.of(mockPointHistory);
        when(pointService.history(id)).thenReturn(pointHistoryList);

        //when & then
        mockMvc.perform(get("/point/{id}/histories", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(id))
                .andExpect(jsonPath("$[0].amount").value(pointHistoryList.get(0).amount()));

    }


    @Test
    @DisplayName("[E] history : PointHistory 조회시 null 예외")
    void history_should_return_not_found_when_not_exists() throws Exception {
        //given
        long id = 1L;
        when(pointService.history(id)).thenReturn(null);

        //when & then
        mockMvc.perform(get("/point/{id}/histories", id))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("[S] charge : 포인트 충전 후 갱신 데이터 반환")
    void charge_should_return_updated_user_point() throws Exception {
        //given
        long id = 1L;
        long chargePoint = 100L;
        mockUserPoint = createUserPoint(id, 200L, System.currentTimeMillis());

        when(pointService.charge(id, chargePoint)).thenReturn(mockUserPoint);

        //when & then
        mockMvc.perform(patch("/point/{id}/charge", id)
                        .contentType(MediaType.APPLICATION_JSON) //요청 설정
                        .content(""" 
                            {"point" : 100}
                            """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.point").value(mockUserPoint.point()));

    }



    //사용할 포인트보다 현재 포인트가 적으면 사용 불가
    @Test
    @DisplayName("[S] use : 포인트 사용 후 갱신 데이터 반환")
    void use_should_return_updated_user_point() throws Exception {
        //given
        long id = 1L;
        long usePoint = 50L;
        mockUserPoint = createUserPoint(id, 150L, System.currentTimeMillis());

        when(pointService.use(id, usePoint)).thenReturn(mockUserPoint);

        //when & then
        mockMvc.perform(patch("/point/{id}/use", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"point" :  50 }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.point").value(mockUserPoint.point()));
    }
}

