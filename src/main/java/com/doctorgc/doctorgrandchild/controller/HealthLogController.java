package com.doctorgc.doctorgrandchild.controller;

import com.doctorgc.doctorgrandchild.dto.HealthChangesResponseDto.HealthChangesDto;
import com.doctorgc.doctorgrandchild.dto.HealthChangesResponseDto.ShortHealthChangesDto;
import com.doctorgc.doctorgrandchild.dto.HealthLogResponseDto.DiagnosisListDto;
import com.doctorgc.doctorgrandchild.dto.HealthLogResponseDto.MemberInfoDto;
import com.doctorgc.doctorgrandchild.service.HealthLogService;
import com.doctorgc.doctorgrandchild.service.HealthReportService;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.time.YearMonth;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.Operation;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/my")
public class HealthLogController {

    private final HealthLogService healthLogService;
    private final HealthReportService healthReportService;

    @Operation(summary = "오늘의 건강 기록 조회", description = "이름 및 오늘 날짜의 건강 기록 조회")
    @GetMapping("/today")
    public ResponseEntity<MemberInfoDto> getMemberInfo(HttpServletRequest request, @RequestParam LocalDate date) {
        MemberInfoDto memberInfo = healthLogService.getInfo("", date);
        return ResponseEntity.ok(memberInfo);
    }

    @Operation(summary = "진단 기록 월별 전체 조회", description = "진단 기록들의 목록을 월별로 전체 조회")
    @GetMapping("/diagnosis-lists")
    public ResponseEntity<DiagnosisListDto> getDiagnosisLists(HttpServletRequest request,
        @RequestParam("date") @DateTimeFormat(pattern = "yyyy-MM") YearMonth date) {
        DiagnosisListDto diagnosisList = healthLogService.getDiagnosisLists("", date);
        return ResponseEntity.ok(diagnosisList);
    }

    @Operation(summary = "내 건강 변화 조회", description = "건강변화 간략 조회 조회")
    @GetMapping("/health-changes")
    public ResponseEntity<ShortHealthChangesDto> getShortHealthChanges(HttpServletRequest request) {
        return ResponseEntity.ok(healthReportService.getShortHealthReport(""));
    }

    @Operation(summary = "내 건강 변화 상세 조회", description = "건강변화에 대한 내용, 해결법을 조회")
    @GetMapping("/health-changes/details")
    public ResponseEntity<HealthChangesDto> getHealthChanges(HttpServletRequest request) {
        return ResponseEntity.ok(healthReportService.getHealthReport(""));
    }
}
