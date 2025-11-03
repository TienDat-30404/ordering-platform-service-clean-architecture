// package com.example.demo;

// import com.example.common_dtos.dto.PaymentResponseData;
// import com.example.demo.application.dto.command.AuthorizePaymentCommand;
// import com.example.demo.application.dto.command.RefundPaymentCommand;
// import com.example.demo.application.ports.output.repository.PaymentRepository;
// import com.example.demo.application.usecases.PaymentApplicationService;
// import com.example.demo.domain.entity.Payment;
// import com.example.demo.domain.service.PaymentDomainService;
// import com.example.demo.domain.valueobject.PaymentStatus;
// import com.example.demo.infrastructure.payment.PaymentGateway;
// import com.example.demo.infrastructure.publisher.EventPublisher;

// import org.junit.jupiter.api.BeforeEach;
// import org.junit.jupiter.api.Test;
// import org.junit.jupiter.api.extension.ExtendWith;
// import org.mockito.*;
// import org.mockito.junit.jupiter.MockitoExtension;

// import java.math.BigDecimal;
// import java.util.Optional;

// import static org.junit.jupiter.api.Assertions.*;
// import static org.mockito.Mockito.*;

// /**
//  * Unit tests for PaymentApplicationService.
//  * 
//  * Kỹ thuật: mock external dependencies (Repository, Gateway, DomainService)
//  * và verify interaction với các layer khác.
//  */
// @ExtendWith(MockitoExtension.class)
// class PaymentApplicationServiceTest {

//     @Mock
//     private PaymentRepository paymentRepository;
//     @Mock
//     private PaymentDomainService paymentDomainService;
//     @Mock
//     private PaymentGateway paymentGateway;
//     @Mock
//     private EventPublisher eventPublisher;

//     @InjectMocks
//     private PaymentApplicationService paymentService;

//     private Payment mockPayment;

//     @BeforeEach
//     void setup() {
//         mockPayment = mock(Payment.class);
//         lenient().when(mockPayment.getPaymentId()).thenReturn(11L);
//         lenient().when(mockPayment.getOrderId()).thenReturn(22L);
//         lenient().when(mockPayment.getUserId()).thenReturn(33L);
//         lenient().when(mockPayment.getAmount()).thenReturn(BigDecimal.valueOf(99.99));
//         lenient().when(mockPayment.getStatus()).thenReturn(PaymentStatus.PENDING);
//     }

//     // ----------------------------- TEST 1 -----------------------------
//     @Test
//     void authorizePayment_successful() {
//         // Arrange
//         AuthorizePaymentCommand cmd = mock(AuthorizePaymentCommand.class);
//         when(cmd.getPaymentId()).thenReturn(11L);
//         when(cmd.getOrderId()).thenReturn(22L);
//         when(cmd.getUserId()).thenReturn(33L);

//         when(paymentRepository.findById(11L)).thenReturn(Optional.of(mockPayment));
//         when(paymentGateway.authorize(any(), any(), any())).thenReturn("TX12345");

//         // Act
//         PaymentResponseData result = paymentService.authorizePayment(cmd);

//         // Assert
//         assertEquals("AUTHORIZED", result.status());
//         assertEquals("TX12345", result.transactionId());

//         verify(paymentDomainService).completePayment(mockPayment, "TX12345", null);
//         verify(paymentRepository).save(mockPayment);
//         verify(paymentGateway).authorize(BigDecimal.valueOf(99.99), 33L, 22L);
//     }

//     // ----------------------------- TEST 2 -----------------------------
//     @Test
//     void authorizePayment_gatewayFails_returnsFailed() {
//         // Arrange
//         AuthorizePaymentCommand cmd = mock(AuthorizePaymentCommand.class);
//         when(cmd.getPaymentId()).thenReturn(11L);
//         when(cmd.getOrderId()).thenReturn(22L);
//         when(cmd.getUserId()).thenReturn(33L);

//         when(paymentRepository.findById(11L)).thenReturn(Optional.of(mockPayment));
//         // Gateway trả về null → thất bại logic
//         when(paymentGateway.authorize(any(), any(), any())).thenReturn(null);

//         // Act
//         PaymentResponseData result = paymentService.authorizePayment(cmd);

//         // Assert
//         assertEquals("FAILED", result.status());
//         assertEquals("Payment gateway failed to return transaction ID.", result.reason());
//         verify(paymentDomainService).completePayment(mockPayment, null, "Payment gateway failed to return transaction ID.");
//         verify(paymentRepository).save(mockPayment);
//     }

//     // ----------------------------- TEST 3 -----------------------------
//     @Test
//     void authorizePayment_systemException_capturesFailure() {
//         // Arrange
//         AuthorizePaymentCommand cmd = mock(AuthorizePaymentCommand.class);
//         when(cmd.getPaymentId()).thenReturn(11L);
//         when(cmd.getOrderId()).thenReturn(22L);
//         when(cmd.getUserId()).thenReturn(33L);

//         when(paymentRepository.findById(11L)).thenReturn(Optional.of(mockPayment));
//         when(paymentGateway.authorize(any(), any(), any())).thenThrow(new RuntimeException("Gateway down"));

//         // Act
//         PaymentResponseData result = paymentService.authorizePayment(cmd);

//         // Assert
//         assertEquals("FAILED", result.status());
//         assertTrue(result.reason().contains("System exception"));
//         verify(paymentDomainService).failPayment(mockPayment, "System exception during authorization: Gateway down");
//         verify(paymentRepository).save(mockPayment);
//     }

//     // ----------------------------- TEST 4 -----------------------------
//     @Test
//     void authorizePayment_alreadyProcessed_returnsExisting() {
//         // Arrange
//         AuthorizePaymentCommand cmd = mock(AuthorizePaymentCommand.class);
//         when(cmd.getPaymentId()).thenReturn(11L);
//         when(cmd.getOrderId()).thenReturn(22L);

//         Payment alreadyProcessed = mock(Payment.class);
//         when(alreadyProcessed.getStatus()).thenReturn(PaymentStatus.AUTHORIZED);
//         when(alreadyProcessed.getPaymentId()).thenReturn(11L);
//         when(alreadyProcessed.getOrderId()).thenReturn(22L);
//         when(alreadyProcessed.getUserId()).thenReturn(33L);
//         when(alreadyProcessed.getAmount()).thenReturn(BigDecimal.TEN);
//         when(alreadyProcessed.getTransactionId()).thenReturn("T123");
//         when(alreadyProcessed.getFailureReason()).thenReturn(null);

//         when(paymentRepository.findById(11L)).thenReturn(Optional.of(alreadyProcessed));

//         // Act
//         PaymentResponseData result = paymentService.authorizePayment(cmd);

//         // Assert
//         assertEquals("AUTHORIZED", result.status());
//         verify(paymentGateway, never()).authorize(any(), any(), any());
//         verify(paymentRepository, never()).save(any());
//     }

//     // ----------------------------- TEST 5 -----------------------------
//     @Test
//     void refundPayment_successfulFlow() {
//         // Arrange
//         RefundPaymentCommand cmd = mock(RefundPaymentCommand.class);
//         when(cmd.getPaymentId()).thenReturn(11L);
//         when(cmd.getOrderId()).thenReturn(22L);

//         when(paymentRepository.findById(11L)).thenReturn(Optional.of(mockPayment));
//         when(mockPayment.getStatus()).thenReturn(PaymentStatus.AUTHORIZED);

//         // Act
//         PaymentResponseData result = paymentService.refundPayment(cmd);

//         // Assert
//         assertEquals("REFUND_COMPLETED", result.status());
//         verify(paymentDomainService, atLeastOnce()).completeRefund(mockPayment);
//         verify(paymentRepository, atLeastOnce()).save(mockPayment);
//     }

//     // ----------------------------- TEST 6 -----------------------------
//     @Test
//     void refundPayment_idempotentAlreadyRefunded() {
//         // Arrange
//         RefundPaymentCommand cmd = mock(RefundPaymentCommand.class);
//         when(cmd.getPaymentId()).thenReturn(11L);
//         when(cmd.getOrderId()).thenReturn(22L);

//         when(mockPayment.getStatus()).thenReturn(PaymentStatus.REFUND_COMPLETED);
//         when(paymentRepository.findById(11L)).thenReturn(Optional.of(mockPayment));

//         // Act
//         PaymentResponseData result = paymentService.refundPayment(cmd);

//         // Assert
//         assertEquals("REFUND_COMPLETED", result.status());
//         assertTrue(result.reason().contains("Already refunded"));
//         verify(paymentDomainService, never()).completeRefund(any());
//         verify(paymentRepository, never()).save(any());
//     }

//     // ----------------------------- TEST 7 -----------------------------
//     @Test
//     void refundPayment_paymentNotFound_throwsException() {
//         // Arrange
//         RefundPaymentCommand cmd = mock(RefundPaymentCommand.class);
//         when(cmd.getPaymentId()).thenReturn(11L);
//         when(cmd.getOrderId()).thenReturn(22L);

//         when(paymentRepository.findById(11L)).thenReturn(Optional.empty());

//         // Act & Assert
//         RuntimeException ex = assertThrows(RuntimeException.class, () -> paymentService.refundPayment(cmd));
//         assertTrue(ex.getMessage().contains("Payment not found"));
//     }
// }
