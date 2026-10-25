import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ApiService, errorMessage } from './api.service';
import { Bill, Customer, Product } from './models';
import { IconComponent } from './icon.component';
import { StatusComponent } from './status.component';
import { money, date, initials } from './format';

@Component({
  imports: [RouterLink, IconComponent, StatusComponent],
  template: `
    <div class="page-heading"><div><div class="eyebrow">VOTRE ACTIVITÉ, EN UN COUP D’ŒIL</div><h1>Vue d’ensemble<span class="orange-dot">.</span></h1><p>Un peu d’ordre. Beaucoup de visibilité.</p></div><a class="button primary" routerLink="/factures/nouvelle"><app-icon name="plus"/>Nouvelle facture</a></div>
    @if (error()) { <div class="notice error" role="alert">{{ error() }} <button (click)="load()">Réessayer</button></div> }
    @if (loading()) { <div class="loading-state" role="status"><span class="spinner"></span>Chargement de votre activité…</div> }
    @else if (!error()) {
      <section class="stats-grid" aria-label="Indicateurs">
        <article class="stat-card"><div class="stat-label">Total facturé<app-icon name="file"/></div><strong>{{ money(total()) }}</strong><span>{{ validBills().length }} factures émises</span><div class="stat-rule"></div></article>
        <article class="stat-card"><div class="stat-label">En attente de règlement<app-icon name="clock"/></div><strong>{{ money(pending()) }}</strong><span><i class="tiny-dot"></i>{{ unpaid().length }} factures à régler</span><div class="stat-rule orange"></div></article>
        <article class="stat-card"><div class="stat-label">Montant encaissé<app-icon name="wallet"/></div><strong>{{ money(paid()) }}</strong><span>{{ paidCount() }} factures réglées</span><div class="stat-rule green"></div></article>
        <article class="stat-card"><div class="stat-label">Vos clients<app-icon name="users"/></div><strong>{{ customers().length }}<small> clients</small></strong><a routerLink="/clients">Voir le carnet clients <app-icon name="arrow"/></a><div class="stat-rule gray"></div></article>
      </section>
      <div class="dashboard-grid">
        <section class="panel"><div class="panel-heading"><div><h2>Les dernières factures</h2><p>Gardez le fil de vos dernières opérations.</p></div><a class="text-link" routerLink="/factures">Tout voir <app-icon name="arrow"/></a></div>
          @if (bills().length) {
            <div class="table-wrap"><table><thead><tr><th>FACTURE / CLIENT</th><th>ÉCHÉANCE</th><th>STATUT</th><th class="numeric">MONTANT</th><th><span class="sr-only">Ouvrir</span></th></tr></thead><tbody>
            @for (bill of bills().slice(0, 5); track bill.id) {
              <tr><td><a class="table-title" [routerLink]="['/factures',bill.id]">{{ bill.number }}</a><span class="table-sub">{{ bill.customerName }}</span></td><td class="date-cell">{{ date(bill.dueDate) }}</td><td><app-status [status]="bill.status"/></td><td class="numeric amount">{{ money(bill.total) }}</td><td><a class="icon-button subtle" [routerLink]="['/factures',bill.id]" [attr.aria-label]="'Ouvrir ' + bill.number"><app-icon name="chevron"/></a></td></tr>
            }</tbody></table></div>
          } @else { <div class="empty-state"><app-icon name="file"/><h3>Votre première facture vous attend</h3><p>Créez une facture pour commencer à suivre votre activité.</p><a class="button secondary" routerLink="/factures/nouvelle">Créer une facture</a></div> }
          <div class="panel-bottom"><span class="tiny-dot green"></span>Les montants reflètent les factures enregistrées.</div>
        </section>
        <section class="collection-card"><div class="eyebrow">LES BONS RÉFLEXES</div><div class="collection-art"><app-icon name="file"/><span><app-icon name="check"/></span></div><h2>Une facture.<br>Tout est clair.</h2><p>Un client, quelques produits, et votre facture est prête.</p><a class="button light" routerLink="/factures/nouvelle">Créer une facture <app-icon name="arrow"/></a><div class="collection-foot">Simple. Précis. Organisé.</div></section>
      </div>
      <section class="quick-grid"><a class="quick-card" routerLink="/clients"><span class="quick-icon"><app-icon name="users"/></span><div><h3>Un carnet clients à jour</h3><p>{{ customers().length }} contacts pour vos prochaines factures</p></div><app-icon name="arrow"/></a><a class="quick-card" routerLink="/produits"><span class="quick-icon peach"><app-icon name="box"/></span><div><h3>Votre catalogue, bien rangé</h3><p>{{ products().length }} produits et leurs prix, au même endroit</p></div><app-icon name="arrow"/></a></section>
    }
  `,
})
export class DashboardComponent implements OnInit {
  private api = inject(ApiService);
  bills = signal<Bill[]>([]); customers = signal<Customer[]>([]); products = signal<Product[]>([]);
  loading = signal(true); error = signal('');
  validBills = computed(() => this.bills().filter(b => b.status !== 'CANCELLED'));
  unpaid = computed(() => this.bills().filter(b => b.status === 'UNPAID'));
  total = computed(() => this.validBills().reduce((sum, b) => sum + b.total, 0));
  pending = computed(() => this.unpaid().reduce((sum, b) => sum + b.total, 0));
  paid = computed(() => this.bills().filter(b => b.status === 'PAID').reduce((sum, b) => sum + b.total, 0));
  paidCount = computed(() => this.bills().filter(b => b.status === 'PAID').length);
  money = money; date = date; initials = initials;
  ngOnInit() { void this.load(); }
  async load() {
    this.loading.set(true); this.error.set('');
    try { const [bills, customers, products] = await Promise.all([this.api.bills(), this.api.customers(), this.api.products()]); this.bills.set(bills); this.customers.set(customers); this.products.set(products); }
    catch (error) { this.error.set(errorMessage(error)); }
    finally { this.loading.set(false); }
  }
}
