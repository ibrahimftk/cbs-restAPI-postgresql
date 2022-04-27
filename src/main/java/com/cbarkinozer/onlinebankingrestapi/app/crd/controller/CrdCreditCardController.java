package com.cbarkinozer.onlinebankingrestapi.app.crd.controller;

import com.cbarkinozer.onlinebankingrestapi.app.crd.dto.CrdCreditCardActivityAnalysisDto;
import com.cbarkinozer.onlinebankingrestapi.app.crd.dto.CrdCreditCardActivityDto;
import com.cbarkinozer.onlinebankingrestapi.app.crd.dto.CrdCreditCardDto;
import com.cbarkinozer.onlinebankingrestapi.app.crd.dto.CrdCreditCardSaveDto;
import com.cbarkinozer.onlinebankingrestapi.app.crd.service.CrdCreditCardActivityService;
import com.cbarkinozer.onlinebankingrestapi.app.crd.service.CrdCreditCardService;
import com.cbarkinozer.onlinebankingrestapi.app.gen.dto.RestResponse;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/v1/credit-card")
public class CrdCreditCardController {

    private final CrdCreditCardService crdCreditCardService;
    private final CrdCreditCardActivityService crdCreditCardActivityService;

    @Operation(
            tags = "Credit Card Controller",
            summary = "All credit cards",
            description = "Gets all active credit cards."
    )
    @GetMapping
    public ResponseEntity<RestResponse<List<CrdCreditCardDto>>> findAllCreditCards(){

        List<CrdCreditCardDto> crdCreditCardDtoList = crdCreditCardService.findAllCreditCards();

        return ResponseEntity.ok(RestResponse.of(crdCreditCardDtoList));
    }

    @Operation(
            tags = "Credit Card Controller",
            summary = "Get a credit card",
            description = "Gets a credit card by id."
    )
    @GetMapping("/{id}")
    public ResponseEntity<RestResponse<CrdCreditCardDto>> findCreditCardById(@PathVariable Long id){

        CrdCreditCardDto crdCreditCardDto = crdCreditCardService.findCreditCardById(id);

        return ResponseEntity.ok(RestResponse.of(crdCreditCardDto));
    }

    @Operation(
            tags = "Credit Card Controller",
            summary = "Get credit card by price interval",
            description = "Gets products in the range by given min and max."
    )
    @GetMapping("/find-by-amount-interval")
    public ResponseEntity<RestResponse<List<CrdCreditCardActivityDto>>>
    findCreditCardActivityByAmountInterval(@RequestParam BigDecimal min, @RequestParam BigDecimal max){

        List<CrdCreditCardActivityDto> crdCreditCardActivityDtoList = crdCreditCardActivityService.findCreditCardActivityByAmountInterval(min,max);

        return ResponseEntity.ok(RestResponse.of(crdCreditCardActivityDtoList));
    }


    @Operation(
            tags = "Credit Card Controller",
            summary = "Get a credit card's activities between dates",
            description = "Gets a credit card's activities between dates pageable."
    )
    @GetMapping("/{id}/activities")
    public ResponseEntity<RestResponse<List<CrdCreditCardActivityDto>>> findCreditCardActivityBetweenDates(
            @PathVariable Long creditCardId,
            @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            Optional<Integer> pageOptional,
            Optional<Integer> sizeOptional
    ){

        List<CrdCreditCardActivityDto> crdCreditCardActivityDtoList =
                crdCreditCardService.findCreditCardActivityBetweenDates(
                        creditCardId,
                        startDate,
                        endDate,
                        pageOptional,
                        sizeOptional
                );

        return ResponseEntity.ok(RestResponse.of(crdCreditCardActivityDtoList));
    }

    @Operation(
            tags = "Credit Card Controller",
            summary = "Get an analysis about credit card activities.",
            description = "Gets an analysis about credit card activity's minimum, maximum, and average amounts, " +
                    "count of credit card activities and credit card activity by credit card activity type."
    )
    @GetMapping("/get-credit-card-activity-analysis")
    public ResponseEntity<RestResponse<List<CrdCreditCardActivityAnalysisDto>>> getCreditCardActivityAnalysis(){

        List<CrdCreditCardActivityAnalysisDto> crdCreditCardActivityAnalysisDtoList = crdCreditCardActivityService.getCreditCardActivityAnalysis();

        return ResponseEntity.ok(RestResponse.of(crdCreditCardActivityAnalysisDtoList));
    }

    @Operation(
            tags = "Credit Card Controller",
            summary = "Save a credit card",
            description = "Save a credit card."
    )
    @PostMapping
    public ResponseEntity<RestResponse<CrdCreditCardDto>> saveCreditCard(@RequestBody CrdCreditCardSaveDto crdCreditCardSaveDto){

        CrdCreditCardDto crdCreditCardDto = crdCreditCardService.saveCreditCard(crdCreditCardSaveDto);

        return ResponseEntity.ok(RestResponse.of(crdCreditCardDto));
    }


    @Operation(
            tags = "Credit Card Controller",
            summary = "Cancel a credit card",
            description = "Cancel a credit card by making its status passive."
    )
    @PatchMapping("/cancel/{cardId}")
    public ResponseEntity<RestResponse<?>> cancelCreditCard(@PathVariable Long cardId){

        crdCreditCardService.cancelCreditCard(cardId);

        return ResponseEntity.ok(RestResponse.empty());
    }


}