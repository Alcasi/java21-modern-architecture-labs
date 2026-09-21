export interface Transacao {
    id: string;
    descricao: string;
    valor: number;
    tipo: 'CREDITO' | 'DEBITO';
    data: Date;
}
