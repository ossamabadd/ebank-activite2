import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { ApiService, errorMessage } from './api.service';
import { Bill, BillStatus } from './models';
import { IconComponent } from './icon.component';
import { StatusComponent } from './status.component';
import { money, date } from './format';

@Component({
  imports: [FormsModule, RouterLink, IconComponent, StatusComponent],
  template: `
    <div class="page-heading"><div><div class="eyebrow">DE L’ÉMISSION AU RÈGLEMENT</div><h1>Vos factures<span class="orange-dot">.</span></h1><p>Chaque facture à sa place, chaque règlement sous les yeux.</p></div><a class="button primary" routerLink="/factures/nouvelle"><app-icon name="plus"/>Nouvelle facture</a></div>
    @if (error()) { <div class="notice error" role="alert">{{ error() }} <button (click)="load()">Réessayer</button></div> }
    @if (customerId) { <div class="notice info">Factures du client n°{{ customerId }} <button (click)="clearCustomer()">Voir tous les clients</button></div> }
    <section class="panel"><div class="list-toolbar"><div class="filter-tabs" aria-label="Filtrer les factures par statut">
      @for (tab of tabs; track tab.value) { <button [class.selected]="status===tab.value" [attr.aria-pressed]="status===tab.value" (click)="status=tab.value;page=0">{{ tab.label }}<span>{{ count(tab.value) }}</span></button> }
      </div><label class="search-field"><app-icon name="search"/><input aria-label="Rechercher une facture" placeholder="N° de facture ou client…" [(ngModel)]="search" (ngModelChange)="page=0"></label></div>
      @if (loading()) { <div class="loading-state" role="status"><span class="spinner"></span>Chargement des factures…</div> }
      @else if (!filtered.length) { <div class="empty-state"><app-icon name="file"/><h3>Aucune facture à afficher</h3><p>Modifiez les filtres ou créez votre première facture.</p><a class="button secondary" routerLink="/factures/nouvelle">Créer une facture</a></div> }
      @else { <div class="table-wrap"><table><thead><tr><th>FACTURE</th><th>CLIENT</th><th>ÉMISSION</th><th>ÉCHÉANCE</th><th>STATUT</th><th class="numeric">MONTANT</th><th><span class="sr-only">Ouvrir</span></th></tr></thead><tbody>
      @for (bill of filtered.slice(page*10,page*10+10); track bill.id) {
        <tr><td><a class="table-title" [routerLink]="['/factures',bill.id]">{{ bill.number }}</a><span class="table-sub">{{ bill.items.length }} ligne(s)</span></td><td><strong>{{ bill.customerName }}</strong><span class="table-sub">{{ bill.customerEmail }}</span></td><td class="date-cell">{{ date(bill.issuedAt) }}</td><td class="date-cell">{{ date(bill.dueDate) }}</td><td><app-status [status]="bill.status"/></td><td class="numeric amount">{{ money(bill.total) }}</td><td><a class="icon-button" [routerLink]="['/factures',bill.id]" [attr.aria-label]="'Ouvrir ' + bill.number"><app-icon name="chevron"/></a></td></tr>
      }</tbody></table></div><div class="pagination"><span>{{ filtered.length }} facture(s)</span><div><button class="button small secondary" [disabled]="page===0" (click)="page=page-1">Précédent</button><span>Page {{ page+1 }}</span><button class="button small secondary" [disabled]="(page+1)*10>=filtered.length" (click)="page=page+1">Suivant</button></div></div> }
    </section>
  `,
})
export class BillsComponent implements OnInit {
  private api = inject(ApiService); private route = inject(ActivatedRoute);
  bills = signal<Bill[]>([]); loading = signal(true); error = signal('');
  search = ''; page = 0; status = ''; customerId = Number(this.route.snapshot.queryParamMap.get('client')) || null;
  tabs = [{ value: '', label: 'Toutes' }, { value: 'UNPAID', label: 'À régler' }, { value: 'PAID', label: 'Payées' }, { value: 'CANCELLED', label: 'Annulées' }];
  money = money; date = date;
  get filtered() { const q = this.search.trim().toLowerCase(); return this.bills().filter(b => (!this.customerId || b.customerId===this.customerId) && (!this.status || b.status===this.status) && (b.number + ' ' + b.customerName + ' ' + b.customerEmail).toLowerCase().includes(q)); }
  count(status: string) { return this.bills().filter(b => (!this.customerId || b.customerId===this.customerId) && (!status || b.status===status)).length; }
  clearCustomer() { this.customerId = null; this.page = 0; }
  ngOnInit() { void this.load(); }
  async load() { this.loading.set(true); this.error.set(''); try { this.bills.set(await this.api.bills()); } catch(e) { this.error.set(errorMessage(e)); } finally { this.loading.set(false); } }
}
