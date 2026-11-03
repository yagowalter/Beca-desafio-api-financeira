package br.com.beca.financeira.transactions.adapters.out.http;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class ExchangeRateResponse {

    private String moeda;
    private LocalDate data;
    private List<QuoteResponse> cotacoes;

    public ExchangeRateResponse() {
    }

    public String getMoeda() {
        return moeda;
    }

    public void setMoeda(String moeda) {
        this.moeda = moeda;
    }

    public LocalDate getData() {
        return data;
    }

    public void setData(LocalDate data) {
        this.data = data;
    }

    public List<QuoteResponse> getCotacoes() {
        return cotacoes;
    }

    public void setCotacoes(List<QuoteResponse> cotacoes) {
        this.cotacoes = cotacoes;
    }

    public static class QuoteResponse {

        @JsonProperty("cotacao_venda")
        private BigDecimal sellingRate;

        @JsonProperty("data_hora_cotacao")
        private String quoteDateTime;

        @JsonProperty("tipo_boletim")
        private String bulletinType;

        public QuoteResponse() {
        }

        public BigDecimal getSellingRate() {
            return sellingRate;
        }

        public void setSellingRate(BigDecimal sellingRate) {
            this.sellingRate = sellingRate;
        }

        public String getQuoteDateTime() {
            return quoteDateTime;
        }

        public void setQuoteDateTime(String quoteDateTime) {
            this.quoteDateTime = quoteDateTime;
        }

        public String getBulletinType() {
            return bulletinType;
        }

        public void setBulletinType(String bulletinType) {
            this.bulletinType = bulletinType;
        }
    }
}
