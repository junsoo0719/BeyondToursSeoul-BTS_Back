package com.beyondtoursseoul.bts.service;

import com.beyondtoursseoul.bts.dto.congestion.AreaCongestionResponse;
import com.beyondtoursseoul.bts.repository.AreaCongestionRawRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AreaCongestionQueryService {

    private final AreaCongestionRawRepository areaCongestionRawRepository;

    public List<AreaCongestionResponse> getList() {
        return areaCongestionRawRepository.findAllByOrderByAreaNameAsc().stream()
                .map(AreaCongestionResponse::from)
                .toList();
    }

    public AreaCongestionResponse getDetail(String areaCode) {
        return areaCongestionRawRepository.findByAreaCode(areaCode)
                .map(AreaCongestionResponse::from)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "해당 지역 혼잡도 데이터를 찾을 수 없습니다. areaCode=" + areaCode
                ));
    }
}
