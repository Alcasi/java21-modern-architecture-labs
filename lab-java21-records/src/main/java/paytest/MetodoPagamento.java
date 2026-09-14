package paytest;

import java.math.BigDecimal;

public sealed interface MetodoPagamento {
};

record Pix(String chave, BigDecimal valor) implements MetodoPagamento {
};

record CartaoCredito(String numeroCartao, int parcelas, BigDecimal valor) implements MetodoPagamento {
};

record Boleto(String linhaDigitavel, BigDecimal valor) implements MetodoPagamento {
};
