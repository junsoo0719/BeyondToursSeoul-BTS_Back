package com.beyondtoursseoul.bts.controller;

import com.beyondtoursseoul.bts.dto.congestion.AreaCongestionResponse;
import com.beyondtoursseoul.bts.service.AreaCongestionQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Congestion", description = "서울 실시간 혼잡도 조회 API")
@RestController
@RequestMapping("/api/v1/congestion")
@RequiredArgsConstructor
public class AreaCongestionController {

    private final AreaCongestionQueryService areaCongestionQueryService;

    @Operation(
            summary = "지역 혼잡도 목록 조회",
            description = "DB에 저장된 전체 지역의 최신 혼잡도 정보를 반영합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공")
    })
    @GetMapping
    public ResponseEntity<List<AreaCongestionResponse>> getList() {
        return ResponseEntity.ok(areaCongestionQueryService.getList());
    }

    @Operation(
            summary = "지역 혼잡도 단건 조회",
            description = "areaCode 기준으로 특정 지역의 최신 혼잡도 정보를 반환합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "해당 지역을 찾을 수 없음")
    })
    @GetMapping("/{areaCode}")
    public ResponseEntity<AreaCongestionResponse> getDetail(
            @Parameter(description = "지역 코드", example = "POI009")
            @PathVariable String areaCode) {
        return ResponseEntity.ok(areaCongestionQueryService.getDetail(areaCode));
    }
}
