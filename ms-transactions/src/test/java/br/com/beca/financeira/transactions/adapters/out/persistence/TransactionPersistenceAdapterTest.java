package br.com.beca.financeira.transactions.adapters.out.persistence;

import br.com.beca.financeira.transactions.domain.Transaction;
import br.com.beca.financeira.transactions.domain.TransactionType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TransactionPersistenceAdapterTest {

    @Test
    void shouldFindTransactionsByUserAndPeriodAndMapToDomain() {
        TransactionJpaRepository jpaRepository = mock(TransactionJpaRepository.class);
        TransactionPersistenceAdapter adapter =
                new TransactionPersistenceAdapter(jpaRepository);

        Long userId = 7L;
        LocalDateTime start = LocalDateTime.of(2026, 11, 1, 0, 0);
        LocalDateTime endExclusive = LocalDateTime.of(2026, 12, 1, 0, 0);

        TransactionEntity entity = new TransactionEntity(new Transaction(
                12L,
                userId,
                "Depósito",
                new BigDecimal("100.00"),
                TransactionType.DEPOSIT,
                LocalDateTime.of(2026, 11, 15, 10, 30)
        ));

        when(jpaRepository
                .findAllByUserIdAndOccurredAtGreaterThanEqualAndOccurredAtLessThan(
                        userId, start, endExclusive
                ))
                .thenReturn(List.of(entity));

        List<Transaction> result =
                adapter.findAllByUserIdAndPeriod(userId, start, endExclusive);

        assertEquals(1, result.size());
        assertEquals(12L, result.get(0).getId());
        assertEquals(userId, result.get(0).getUserId());
        assertEquals("Depósito", result.get(0).getDescription());

        verify(jpaRepository)
                .findAllByUserIdAndOccurredAtGreaterThanEqualAndOccurredAtLessThan(
                        userId, start, endExclusive
                );
    }
}