package com.cbarkinozer.onlinebankingrestapi.app.loa.service;

import com.cbarkinozer.onlinebankingrestapi.app.loa.dto.*;
import com.cbarkinozer.onlinebankingrestapi.app.loa.entity.LoaLoan;
import com.cbarkinozer.onlinebankingrestapi.app.loa.entity.LoaLoanPayment;
import com.cbarkinozer.onlinebankingrestapi.app.loa.enums.LoaLoanStatusType;
import com.cbarkinozer.onlinebankingrestapi.app.loa.mapper.LoaLoanMapper;
import com.cbarkinozer.onlinebankingrestapi.app.loa.service.entityservice.LoaLoanEntityService;
import com.cbarkinozer.onlinebankingrestapi.app.loa.service.entityservice.LoaLoanPaymentEntityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

@Service
@Transactional
@RequiredArgsConstructor
public class LoaLoanService {

    private final LoaLoanValidationService loaLoanValidationService;
    private final LoaLoanEntityService loaLoanEntityService;
    private final LoaLoanPaymentEntityService loaLoanPaymentEntityService;

    private final BigDecimal INTEREST_RATE = BigDecimal.valueOf(1.59/100);
    private final BigDecimal TAX_RATE = BigDecimal.valueOf(20/100); //KKDF + BSMV
    private final BigDecimal ALLOCATION_FEE = BigDecimal.valueOf(45);

    public LoaCalculateLoanResponseDto calculateLoan(Integer installment, BigDecimal principalLoanAmount) {

        loaLoanValidationService.controlIsParameterNotNull(installment,principalLoanAmount);

        BigDecimal installmentCount = BigDecimal.valueOf(installment);

        BigDecimal totalInterestRate = INTEREST_RATE.add(TAX_RATE);

        BigDecimal maturity = (installmentCount
                .multiply(BigDecimal.valueOf(30))).divide(BigDecimal.valueOf(36500),RoundingMode.CEILING);

        BigDecimal totalInterest = (principalLoanAmount.multiply(totalInterestRate)).multiply(maturity).multiply(installmentCount);
        BigDecimal totalPayment = principalLoanAmount.add(totalInterest).add(ALLOCATION_FEE);

        BigDecimal monthlyInstallmentAmount = totalPayment.divide(installmentCount,RoundingMode.CEILING);

        BigDecimal annualCostRate = totalInterestRate.multiply(BigDecimal.valueOf(12));

        loaLoanValidationService.controlIsInterestRateNotNegative(INTEREST_RATE);
        loaLoanValidationService.controlIsInstallmentAmountPositive(monthlyInstallmentAmount);
        loaLoanValidationService.controlIsTotalPaymentPositive(totalPayment);

        LoaCalculateLoanResponseDto loaCalculateLoanResponseDto = new LoaCalculateLoanResponseDto();

        loaCalculateLoanResponseDto.setInterestRate(INTEREST_RATE);
        loaCalculateLoanResponseDto.setTotalInterest(totalInterest);
        loaCalculateLoanResponseDto.setMonthlyInstallmentAmount(monthlyInstallmentAmount);
        loaCalculateLoanResponseDto.setTotalPayment(totalPayment);
        loaCalculateLoanResponseDto.setAnnualCostRate(annualCostRate);
        loaCalculateLoanResponseDto.setAllocationFee(ALLOCATION_FEE);

        return loaCalculateLoanResponseDto;
    }

    public LoaCalculateLateFeeResponseDto calculateLateFee(Long id) {

        LoaLoan loaLoan = loaLoanEntityService.getByIdWithControl(id);
        LocalDate dueDate = loaLoan.getDueDate();

        Long lateDayCount = loaLoanValidationService.controlIsLoanDueDatePast(dueDate);

        BigDecimal totalLoan = loaLoan.getPrincipalLoanAmount();

        BigDecimal lateFeeRate = INTEREST_RATE.add(INTEREST_RATE.multiply(BigDecimal.valueOf(30/100)));
        BigDecimal totalLateFee = totalLoan.multiply(BigDecimal.valueOf(lateDayCount)).multiply(lateFeeRate)
                .divide(BigDecimal.valueOf(30),RoundingMode.UP);

        BigDecimal lateInterestTax = totalLateFee.multiply(TAX_RATE);

        totalLateFee = totalLateFee.add(lateInterestTax);

        BigDecimal remainingPrincipal = loaLoan.getRemainingPrincipal();
        remainingPrincipal = remainingPrincipal.add(totalLateFee);

        loaLoanValidationService.controlIsLateFeeRateNotNegative(lateFeeRate);
        loaLoanValidationService.controlIsTotalLateFeePositive(totalLateFee);
        loaLoanValidationService.controlIsLateInterestTaxNotNegative(lateInterestTax);
        loaLoanValidationService.controlIsPrincipalLoanAmountPositive(remainingPrincipal);

        loaLoan.setLoanStatusType(LoaLoanStatusType.LATE);
        loaLoan.setRemainingPrincipal(remainingPrincipal);

        loaLoanEntityService.save(loaLoan);

        LoaCalculateLateFeeResponseDto loaCalculateLateFeeResponseDto = new LoaCalculateLateFeeResponseDto();

        loaCalculateLateFeeResponseDto.setLateFeeRate(lateFeeRate);
        loaCalculateLateFeeResponseDto.setTotalLateFee(totalLateFee);
        loaCalculateLateFeeResponseDto.setLateInterestTax(lateInterestTax);
        loaCalculateLateFeeResponseDto.setLateDayCount(lateDayCount);

        return loaCalculateLateFeeResponseDto;
    }

    public LoaLoanDto findLoanById(Long id) {

        LoaLoan loaLoan = loaLoanEntityService.getByIdWithControl(id);

        LoaLoanDto loaLoanDto = LoaLoanMapper.INSTANCE.convertToLoaLoanDto(loaLoan);

        return loaLoanDto;
    }

    public LoaLoanDto applyLoan(LoaApplyLoanDto loaLoanApplyLoanDto) {

        loaLoanValidationService.controlIsParameterNotNull(loaLoanApplyLoanDto);

        Long customerId = loaLoanApplyLoanDto.getCustomerId();
        BigDecimal principalLoanAmount = loaLoanApplyLoanDto.getPrincipalLoanAmount();
        Integer installment = loaLoanApplyLoanDto.getInstallmentCount();
        BigDecimal installmentCount = BigDecimal.valueOf(installment);
        BigDecimal monthlySalary = loaLoanApplyLoanDto.getMonthlySalary();

        LoaLoan loaLoan = LoaLoanMapper.INSTANCE.convertToLoaLoan(loaLoanApplyLoanDto);

        BigDecimal totalInterestRate = INTEREST_RATE.add(TAX_RATE);

        BigDecimal maturity = (installmentCount
                .multiply(BigDecimal.valueOf(30))).divide(BigDecimal.valueOf(36500),RoundingMode.CEILING);
        BigDecimal totalInterest = (principalLoanAmount.multiply(totalInterestRate)).multiply(maturity).multiply(installmentCount);

        BigDecimal totalPayment = principalLoanAmount.add(totalInterest).add(ALLOCATION_FEE);

        BigDecimal monthlyInstallmentAmount = totalPayment.divide(installmentCount,RoundingMode.CEILING);

        BigDecimal maxInstallmentAmount = monthlySalary.multiply(BigDecimal.valueOf(0.5));
        BigDecimal maxLoanAmount = maxInstallmentAmount
                .multiply(installmentCount)
                .multiply(BigDecimal.valueOf(0.80));

        LocalDate dueDate = LocalDate.now().plusMonths(installment);


        loaLoanValidationService.controlIsCustomerExist(customerId);
        loaLoanValidationService.controlIsMonthlyInstallmentAmountPositive(monthlyInstallmentAmount);
        loaLoanValidationService.controlIsInterestAmountNotNegative(totalInterest);
        loaLoanValidationService.controlIsPrincipalLoanAmountPositive(principalLoanAmount);
        loaLoanValidationService.controlIsLoanAmountNotGreaterThanMaxLoanAmount(
                principalLoanAmount, maxLoanAmount);


        loaLoan.setMonthlyInstallmentAmount(monthlyInstallmentAmount);
        loaLoan.setInterestToBePaid(totalInterest);
        loaLoan.setPrincipalToBePaid(principalLoanAmount);
        loaLoan.setRemainingPrincipal(principalLoanAmount);
        loaLoan.setDueDate(dueDate);
        loaLoan.setLoanStatusType(LoaLoanStatusType.CONTINUING);


        loaLoan = loaLoanEntityService.save(loaLoan);

        LoaLoanDto loaLoanDto = LoaLoanMapper.INSTANCE.convertToLoaLoanDto(loaLoan);

        return loaLoanDto;
    }


    public LoaPayInstallmentResponseDto payInstallment(Long id) {

        LoaLoan loaLoan = loaLoanEntityService.getByIdWithControl(id);

        BigDecimal installmentAmount = loaLoan.getMonthlyInstallmentAmount();
        BigDecimal remainingPrincipal = loaLoan.getRemainingPrincipal();

        remainingPrincipal = remainingPrincipal.subtract(installmentAmount);

        loaLoanValidationService.controlIsRemainingPrincipalNotNegative(remainingPrincipal);

        loaLoan.setRemainingPrincipal(remainingPrincipal);

        LoaLoanPayment loanPayment = new LoaLoanPayment();

        loanPayment.setLoanId(id);
        loanPayment.setPaymentAmount(installmentAmount);
        loanPayment.setPaymentDate(LocalDate.now());

        loaLoan = loaLoanEntityService.save(loaLoan);
        loanPayment = loaLoanPaymentEntityService.save(loanPayment);

        LoaPayInstallmentResponseDto loaPayInstallmentResponseDto = convertToLoaPayInstallmentResponseDto(loaLoan, loanPayment);

        return loaPayInstallmentResponseDto;
    }

    private LoaPayInstallmentResponseDto  convertToLoaPayInstallmentResponseDto(LoaLoan loaLoan, LoaLoanPayment loanPayment){

        Long loanId = loanPayment.getLoanId();
        BigDecimal paymentAmount = loanPayment.getPaymentAmount();
        LocalDate PaymentDate = loanPayment.getPaymentDate();

        BigDecimal remainingPrincipal = loaLoan.getRemainingPrincipal();
        LocalDate dueDate = loaLoan.getDueDate();

        LoaPayInstallmentResponseDto loaPayInstallmentResponseDto = new LoaPayInstallmentResponseDto();

        loaPayInstallmentResponseDto.setLoanId(loanId);
        loaPayInstallmentResponseDto.setPaymentAmount(paymentAmount);
        loaPayInstallmentResponseDto.setPaymentDate(PaymentDate);
        loaPayInstallmentResponseDto.setRemainingPrincipal(remainingPrincipal);
        loaPayInstallmentResponseDto.setDueDate(dueDate);

        return loaPayInstallmentResponseDto;
    }

    public LoaPayLoanOffResponseDto payLoanOff(Long id) {

        LoaLoan loaLoan = loaLoanEntityService.getByIdWithControl(id);

        BigDecimal paidAmount = loaLoan.getRemainingPrincipal();
        BigDecimal remainingPrincipal = BigDecimal.ZERO;

        loaLoan.setRemainingPrincipal(remainingPrincipal);
        loaLoan.setLoanStatusType(LoaLoanStatusType.PAID);

        loaLoan = loaLoanEntityService.save(loaLoan);

        LoaPayLoanOffResponseDto loaPayLoanOffResponseDto = LoaLoanMapper.INSTANCE.convertToLoaPayLoanOffResponseDto(loaLoan);

        loaPayLoanOffResponseDto.setRemainingAmount(remainingPrincipal);
        loaPayLoanOffResponseDto.setPaidAmount(paidAmount);

        return loaPayLoanOffResponseDto;
    }
}
