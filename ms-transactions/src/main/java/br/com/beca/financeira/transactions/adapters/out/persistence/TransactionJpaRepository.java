package br.com.beca.financeira.transactions.adapters.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface TransactionJpaRepository
        extends JpaRepository
        <br.com.beca.financeira.transactions.adapters.out.persistence.TransactionEntity, Long> {

    @Query(
            """
                    SELECT COALESCE(SUM(t.amount), 0)
                    FROM TransactionEntity t
                    WHERE t.userId = :userId
                       AND t.status = br.com.beca.financeira.transactions.domain.TransactionStatus.APPROVED
                       AND  t.type IN (
                                    br.com.beca.financeira.transactions.domain.TransactionType.WITHDRAW,
                                    br.com.beca.financeira.transactions.domain.TransactionType.TRANSFER,
                                    br.com.beca.financeira.transactions.domain.TransactionType.PURCHASE
                           )
                               AND t.occurredAt >= :start
                               AND t.occurredAt < :end
                    """)
    BigDecimal sumApprovedOutflowsByUserIdAndPeriod(
            @Param("userId") Long userId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    @Query(
            """
                                    SELECT t
                                    FROM TransactionEntity t
                                    WHERE t.userId = :userId
                                        AND t.status = br.com.beca.financeira.transactions.domain.TransactionStatus.APPROVED
                                        AND t.type IN (
                                            br.com.beca.financeira.transactions.domain.TransactionType.WITHDRAW,
                                            br.com.beca.financeira.transactions.domain.TransactionType.TRANSFER,
                                            br.com.beca.financeira.transactions.domain.TransactionType.PURCHASE
                                        )
                                        AND t.occurredAt >= :startInclusive
                                        AND t.occurredAt < :endExclusive
                    """
    )
    List<TransactionEntity> findApprovedOutflowsByUserIdAndPeriod(
            @Param("userId") Long userId,
            @Param("startInclusive") LocalDateTime startInclusive,
            @Param("endExclusive") LocalDateTime endExclusive
    );

}