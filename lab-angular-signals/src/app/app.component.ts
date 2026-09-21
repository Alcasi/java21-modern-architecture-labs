import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ContaCorrenteService } from './services/conta-corrente.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './app.component.html',
  styleUrl: './app.component.css'
})
export class AppComponent {
  // Injeção de dependência funcional moderna
  readonly contaService = inject(ContaCorrenteService);

  // Signals locais para o formulário
  descricaoInput = signal('');
  valorInput = signal<number | null>(null);
  tipoInput = signal<'CREDITO' | 'DEBITO'>('CREDITO');

  executarTransacao(): void {
    const desc = this.descricaoInput().trim();
    const val = this.valorInput();

    if (!desc || val === null || val <= 0) {
      alert('Por favor, informe uma descrição válida e um valor maior que zero.');
      return;
    }

    // Chama o serviço que faz a mutação do Signal
    this.contaService.adicionarTransacao(desc, val, this.tipoInput());

    // Limpa o formulário reativamente
    this.descricaoInput.set('');
    this.valorInput.set(null);
  }
}
