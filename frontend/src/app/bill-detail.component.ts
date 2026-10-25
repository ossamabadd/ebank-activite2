import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { ApiService, errorMessage } from './api.service';
import { Bill, BillStatus } from './models';
import { IconComponent } from './icon.component';
import { StatusComponent } from './status.component';
import { money, date } from './format';

@Component({
  imports: [RouterLink, IconComponent, StatusComponent],
  template: `
    <a class="back-link" routerLink="/factures"><app-icon name="back"/>Toutes les factures</a>
    @if (error()) { <div class="notice error" role="alert">{{ error() }} @if (!bill()) { <button (click)="load()">Réessayer</button> }</div> }
    @if (notice()) { <div class="notice success" role="status">{{ notice() }}</div> }
    @if (loading()) { <div class="loading-state" role="status"><span class="spinner"></span>Chargement de la facture…</div> }
    @if (bill(); as invoice) {
      <div class="page-heading"><div><div class="eyebrow">LES DÉTAILS FONT LA DIFFÉRENCE</div><h1>{{ invoice.number }}<span class="orange-dot">.</span></h1><p>Émise le {{ date(invoice.issuedAt) }} · {{ invoice.customerName }}</p></div><div class="heading-actions"><button class="button secondary" (click)="print()"><app-icon name="download"/>Imprimer / PDF</button>@if (invoice.status==='UNPAID') { <button class="button primary" [disabled]="saving()" (click)="change('PAID')"><app-icon name="check"/>{{ saving() ? 'Enregistrement…' : 'Marquer comme payée' }}</button> }</div></div>
      <section class="invoice-sheet">
        <div class="invoice-top"><div><div class="invoice-brand">facture<span>.</span></div><p>Atelier Digital<br><span class="muted">Gestion commerciale</span></p></div><div class="invoice-title"><span class="eyebrow">FACTURE</span><h2>{{ invoice.number }}</h2><app-status [status]="invoice.status"/></div></div>
        <div class="invoice-parties"><div><span class="eyebrow">FACTURÉ À</span><h3>{{ invoice.customerName }}</h3><p>{{ invoice.customerEmail }}</p><span class="muted">Client n°{{ invoice.customerId }}</span></div><dl><div><dt>Date d’émission</dt><dd>{{ date(invoice.issuedAt) }}</dd></div><div><dt>Date d’échéance</dt><dd>{{ date(invoice.dueDate) }}</dd></div><div><dt>Devise</dt><dd>{{ invoice.currency }} — Dirham marocain</dd></div></dl></div>
        <div class="table-wrap"><table class="invoice-table"><thead><tr><th>DÉSIGNATION</th><th class="numeric">QUANTITÉ</th><th class="numeric">PRIX UNITAIRE</th><th class="numeric">MONTANT</th></tr></thead><tbody>
        @for (line of invoice.items; track line.id) { <tr><td><strong>{{ line.productName }}</strong><span class="table-sub">{{ line.sku }}</span></td><td class="numeric">{{ line.quantity }}</td><td class="numeric">{{ money(line.unitPrice) }}</td><td class="numeric amount">{{ money(line.lineTotal) }}</td></tr> }
        </tbody></table></div>
        <div class="invoice-bottom"><div class="invoice-note"><app-icon name="file"/><h3>Merci pour votre confiance.</h3><p>Les informations de cette facture sont conservées telles qu’elles étaient lors de son émission.</p></div><div class="invoice-totals"><div><span>Sous-total</span><strong>{{ money(invoice.total) }}</strong></div><div><span>TVA ajoutée</span><span>Non appliquée</span></div><div class="grand-total"><span>Total</span><strong>{{ money(invoice.total) }}</strong></div></div></div>
        <div class="invoice-footer"><span>{{ invoice.number }}</span><span>Facture enregistrée · {{ invoice.items.length }} ligne(s)</span></div>
      </section>
      @if (invoice.status==='UNPAID') { <div class="detail-bottom no-print"><span>Une erreur de facturation ? Vous pouvez annuler une facture non réglée.</span><button class="text-button danger" [disabled]="saving()" (click)="change('CANCELLED')">Annuler cette facture</button></div> }
    }
  `,
})
export class BillDetailComponent implements OnInit {
  private api = inject(ApiService); private route = inject(ActivatedRoute);
  bill = signal<Bill | null>(null); loading = signal(true); saving = signal(false); error = signal('');
  notice = signal(history.state?.created ? 'Votre facture a bien été créée.' : '');
  money = money; date = date;
  ngOnInit() { void this.load(); }
  async load() {
    this.loading.set(true); this.error.set('');
    try { this.bill.set(await this.api.bill(Number(this.route.snapshot.paramMap.get('id')))); }
    catch(e) { this.error.set(errorMessage(e)); } finally { this.loading.set(false); }
  }
  print() { window.print(); }
  async change(status: BillStatus) {
    const bill = this.bill(); if (!bill || this.saving()) return;
    if (status==='CANCELLED' && !window.confirm('Annuler définitivement cette facture ? Elle restera consultable dans votre historique.')) return;
    this.saving.set(true); this.error.set('');
    try { this.bill.set(await this.api.changeStatus(bill.id,status)); this.notice.set(status==='PAID' ? 'La facture est maintenant marquée comme payée.' : 'La facture a été annulée.'); }
    catch(e) { this.error.set(errorMessage(e)); } finally { this.saving.set(false); }
  }
}
