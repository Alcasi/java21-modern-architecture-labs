import { TestBed } from '@angular/core/testing';
import { AppComponent } from './app.component';
import { ContaCorrenteService } from './services/conta-corrente.service';

describe('AppComponent', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AppComponent],
      providers: [ContaCorrenteService]
    }).compileComponents();
  });

  it('deve instanciar o componente com sucesso', () => {
    const fixture = TestBed.createComponent(AppComponent);
    const app = fixture.componentInstance;
    expect(app).toBeTruthy();
  });

  it('deve renderizar o título do Ledger Bancário no cabeçalho', () => {
    const fixture = TestBed.createComponent(AppComponent);
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('h1')?.textContent).toContain('NexaBank');
  });

  it('deve exibir o saldo inicial de R$ 10.000,00', () => {
    const fixture = TestBed.createComponent(AppComponent);
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('.card.saldo h2')?.textContent).toContain('10,000.00');
  });
});
