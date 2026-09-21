import { Injectable, signal, computed } from '@angular/core';
import { Transacao } from '../models/transacao.model';

@Injectable({
    providedIn: 'root' // Injeção de dependência nativa (estilo @Service do Spring)
})
export class ContaCorrenteService {

    // --- 1. Writable Signals (Estado Reativo Mutável) ---
    readonly transacoes = signal<Transacao[]>([
        { id: '1', descricao: 'Saldo Inicial Caixa', valor: 10000, tipo: 'CREDITO', data: new Date() }
    ]);

    // --- 2. Computed Signals (Valores Derivados Automáticos com Cache) ---
    // Recalculam automaticamente SEMPRE que 'transacoes()' mudar!
    readonly totalCreditos = computed(() =>
        this.transacoes()
            .filter(t => t.tipo === 'CREDITO')
            .reduce((acc, t) => acc + t.valor, 0)
    );

    readonly totalDebitos = computed(() =>
        this.transacoes()
            .filter(t => t.tipo === 'DEBITO')
            .reduce((acc, t) => acc + t.valor, 0)
    );

    readonly saldoDisponivel = computed(() =>
        this.totalCreditos() - this.totalDebitos()
    );

    // --- 3. Mutação Atômica do Estado ---
    adicionarTransacao(descricao: string, valor: number, tipo: 'CREDITO' | 'DEBITO'): void {
        const novaTransacao: Transacao = {
            id: crypto.randomUUID(),
            descricao,
            valor,
            tipo,
            data: new Date()
        };

        // .update() atualiza o signal de forma imutável (adiciona no topo da lista)
        this.transacoes.update(lista => [novaTransacao, ...lista]);
    }
}
