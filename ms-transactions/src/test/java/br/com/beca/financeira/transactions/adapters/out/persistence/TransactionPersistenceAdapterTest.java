
package br.com.beca.financeira.transactions.adapters.out.persistence;

import br.com.beca.financeira.transactions.domain.Transaction;
import br.com.beca.financeira.transactions.domain.TransactionType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class TransactionPersistenceAdapterTest {

    private final TransactionJpaRepository jpaRepository =
            mock(TransactionJpaRepository.class);

    private final TransactionPersistenceAdapter adapter =
            new TransactionPersistenceAdapter(jpaRepository);

    private TransactionEntity createEntity(Long id) {
        return new TransactionEntity(new Transaction(
                id,
                7L,
                "Depósito",
                new BigDecimal("100.00"),
                TransactionType.DEPOSIT,
                LocalDateTime.of(2026, 11, 15, 10, 30)
        ));
    }

    @Test
    void shouldSaveTransactionAndMapToDomain() {
        Transaction transaction = new Transaction(
                7L,
                "Depósito",
                new BigDecimal("100.00"),
                TransactionType.DEPOSIT
        );

        when(jpaRepository.save(any(TransactionEntity.class)))
                .thenReturn(createEntity(12L));

        Transaction result = adapter.save(transaction);

        assertNotNull(result);
        assertEquals(12L, result.getId());
        assertEquals(7L, result.getUserId());
        assertEquals("Depósito", result.getDescription());
        assertEquals(new BigDecimal("100.00"), result.getAmount());

        verify(jpaRepository).save(any(TransactionEntity.class));
    }

    @Test
    void shouldFindAllTransactionsAndMapToDomain() {
        when(jpaRepository.findAll())
                .thenReturn(List.of(createEntity(12L)));

        List<Transaction> result = adapter.findAll();

        assertEquals(1, result.size());
        assertEquals(12L, result.get(0).getId());

        verify(jpaRepository).findAll();
    }

    @Test
    void shouldReturnEmptyListWhenNoTransactionsExist() {
        when(jpaRepository.findAll()).thenReturn(List.of());

        List<Transaction> result = adapter.findAll();

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldFindTransactionsByUserId() {
        when(jpaRepository.findAllByUserId(7L))
                .thenReturn(List.of(createEntity(12L)));

        List<Transaction> result = adapter.findAllByUserId(7L);

        assertEquals(1, result.size());
        assertEquals(7L, result.get(0).getUserId());

        verify(jpaRepository).findAllByUserId(7L);
    }

    @Test
    void shouldReturnEmptyListWhenUserHasNoTransactions() {
        when(jpaRepository.findAllByUserId(7L))
                .thenReturn(List.of());

        List<Transaction> result = adapter.findAllByUserId(7L);

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldFindTransactionById() {
        when(jpaRepository.findById(12L))
                .thenReturn(Optional.of(createEntity(12L)));

        Optional<Transaction> result = adapter.findById(12L);

        assertTrue(result.isPresent());
        assertEquals(12L, result.get().getId());

        verify(jpaRepository).findById(12L);
    }

    @Test
    void shouldReturnEmptyWhenTransactionIdDoesNotExist() {
        when(jpaRepository.findById(99L))
                .thenReturn(Optional.empty());

        Optional<Transaction> result = adapter.findById(99L);

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldFindTransactionByIdAndUserId() {
        when(jpaRepository.findByIdAndUserId(12L, 7L))
                .thenReturn(Optional.of(createEntity(12L)));

        Optional<Transaction> result =
                adapter.findByIdAndUserId(12L, 7L);

        assertTrue(result.isPresent());
        assertEquals(12L, result.get().getId());
        assertEquals(7L, result.get().getUserId());

        verify(jpaRepository).findByIdAndUserId(12L, 7L);
    }

    @Test
    void shouldReturnEmptyWhenTransactionDoesNotBelongToUser() {
        when(jpaRepository.findByIdAndUserId(12L, 8L))
                .thenReturn(Optional.empty());

        Optional<Transaction> result =
                adapter.findByIdAndUserId(12L, 8L);

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldSumApprovedOutflowsByUserAndPeriod() {
        LocalDateTime start = LocalDateTime.of(2026, 11, 1, 0, 0);
        LocalDateTime end = LocalDateTime.of(2026, 12, 1, 0, 0);

        when(jpaRepository.sumApprovedOutflowsByUserIdAndPeriod(
                7L, start, end
        )).thenReturn(new BigDecimal("350.00"));

        BigDecimal result =
                adapter.sumApprovedOutflowsByUserIdAndPeriod(
                        7L, start, end
                );

        assertEquals(new BigDecimal("350.00"), result);

        verify(jpaRepository)
                .sumApprovedOutflowsByUserIdAndPeriod(7L, start, end);
    }

    @Test
    void shouldReturnZeroWhenThereAreNoApprovedOutflows() {
        LocalDateTime start = LocalDateTime.of(2026, 11, 1, 0, 0);
        LocalDateTime end = LocalDateTime.of(2026, 12, 1, 0, 0);

        when(jpaRepository.sumApprovedOutflowsByUserIdAndPeriod(
                7L, start, end
        )).thenReturn(BigDecimal.ZERO);

        BigDecimal result =
                adapter.sumApprovedOutflowsByUserIdAndPeriod(
                        7L, start, end
                );

        assertEquals(BigDecimal.ZERO, result);
    }

    @Test
    void shouldFindApprovedOutflowsByUserAndPeriod() {
        LocalDateTime start = LocalDateTime.of(2026, 11, 1, 0, 0);
        LocalDateTime end = LocalDateTime.of(2026, 12, 1, 0, 0);

        when(jpaRepository.findApprovedOutflowsByUserIdAndPeriod(
                7L, start, end
        )).thenReturn(List.of(createEntity(12L)));

        List<Transaction> result =
                adapter.findApprovedOutflowsByUserIdAndPeriod(
                        7L, start, end
                );

        assertEquals(1, result.size());
        assertEquals(12L, result.get(0).getId());

        verify(jpaRepository)
                .findApprovedOutflowsByUserIdAndPeriod(7L, start, end);
    }

    @Test
    void shouldReturnEmptyListWhenThereAreNoApprovedOutflows() {
        LocalDateTime start = LocalDateTime.of(2026, 11, 1, 0, 0);
        LocalDateTime end = LocalDateTime.of(2026, 12, 1, 0, 0);

        when(jpaRepository.findApprovedOutflowsByUserIdAndPeriod(
                7L, start, end
        )).thenReturn(List.of());

        List<Transaction> result =
                adapter.findApprovedOutflowsByUserIdAndPeriod(
                        7L, start, end
                );

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldFindTransactionsByUserAndPeriodAndMapToDomain() {
        Long userId = 7L;
        LocalDateTime start = LocalDateTime.of(2026, 11, 1, 0, 0);
        LocalDateTime endExclusive =
                LocalDateTime.of(2026, 12, 1, 0, 0);

        TransactionEntity entity = createEntity(12L);

        when(jpaRepository
                .findAllByUserIdAndOccurredAtGreaterThanEqualAndOccurredAtLessThan(
                        userId, start, endExclusive
                )).thenReturn(List.of(entity));

        List<Transaction> result =
                adapter.findAllByUserIdAndPeriod(
                        userId, start, endExclusive
                );

        assertEquals(1, result.size());
        assertEquals(12L, result.get(0).getId());
        assertEquals(userId, result.get(0).getUserId());
        assertEquals("Depósito", result.get(0).getDescription());

        verify(jpaRepository)
                .findAllByUserIdAndOccurredAtGreaterThanEqualAndOccurredAtLessThan(
                        userId, start, endExclusive
                );
    }

    @Test
    void shouldReturnEmptyListWhenUserHasNoTransactionsInPeriod() {
        Long userId = 7L;
        LocalDateTime start = LocalDateTime.of(2026, 11, 1, 0, 0);
        LocalDateTime end = LocalDateTime.of(2026, 12, 1, 0, 0);

        when(jpaRepository
                .findAllByUserIdAndOccurredAtGreaterThanEqualAndOccurredAtLessThan(
                        userId, start, end
                )).thenReturn(List.of());

        List<Transaction> result =
                adapter.findAllByUserIdAndPeriod(userId, start, end);

        assertTrue(result.isEmpty());
    }
}
