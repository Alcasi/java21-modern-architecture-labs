package paytest;

import java.math.BigDecimal;

public class ProcessadorPagamento {

    public static void processar(MetodoPagamento metodoPagamento) {
        switch (metodoPagamento) {
            case Pix pix -> System.out.println("Processando Pix com chave: " + pix.chave());
            case CartaoCredito cartao ->
                System.out.println("Processando Cartão de Crédito com número: " + cartao.numeroCartao());
            case Boleto boleto ->
                System.out.println("Processando Boleto com linha digitável: " + boleto.linhaDigitavel());
        }
    }

    public static void main(String[] args) {
        Pix pix = new Pix("123456789", new BigDecimal("100.0"));
        CartaoCredito cartao = new CartaoCredito("123456789", 2, new BigDecimal("100.0"));
        Boleto boleto = new Boleto("123456789", new BigDecimal("100.0"));
        processar(pix);
        processar(cartao);
        processar(boleto);
    }

}
