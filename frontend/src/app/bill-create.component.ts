import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { ApiService, errorMessage } from './api.service';
import { Customer, Product } from './models';
import { IconComponent } from './icon.component';
import { money, today } from './format';

interface DraftLine { rowId: number; productId: number | null; quantity: number; }

@Component({
  imports: [FormsModule, RouterLink, IconComponent],
  template: `
    <a class="back-link" routerLink="/factures"><app-icon name="back"/>Toutes les factures</a>
    <div class="page-heading"><div><div class="eyebrow">CHAQUE BELLE COLLABORATION MÉRITE UNE FACTURE</div><h1>Nouvelle facture<span class="orange-dot">.</span></h1><p>Choisissez votre client, ajoutez vos produits. C’est tout.</p></div><span class="draft-badge"><span class="tiny-dot"></span>En préparation</span></div>
    @if (error()) { <div class="notice error" role="alert">{{ error() }} @if (loadingFailed()) { <button (click)="load()">Réessayer</button> }</div> }
    @if (loading()) { <div class="loading-state" role="status"><span class="spinner"></span>Préparation de votre facture…</div> }
    @else {
      @if (!customers().length || !products().length) { <div class="notice info">Ajoutez au moins un client et un produit avant de créer une facture. <a routerLink="/clients">Gérer les clients</a> · <a routerLink="/produits">Gérer les produits</a></div> }
      <form (ngSubmit)="save()" #form="ngForm"><fieldset [disabled]="saving()"><div class="compose-grid">
        <div class="compose-main">
          <section class="panel form-panel"><div class="section-title"><span class="step-number">01</span><div><h2>À qui s’adresse cette facture ?</h2><p>Les coordonnées de votre client seront ajoutées à la facture.</p></div></div>
            <div class="form-grid"><label class="field">Client<select name="customerId" [(ngModel)]="customerId" (ngModelChange)="changed()" required><option [ngValue]="null" disabled>Sélectionner un client</option>@for (customer of customers(); track customer.id) { <option [ngValue]="customer.id">{{ customer.name }} — {{ customer.email }}</option> }</select></label><label class="field">Date d’échéance<input name="dueDate" type="date" [(ngModel)]="dueDate" (ngModelChange)="changed()" [min]="minDate" required></label></div>
            @if (selectedCustomer(); as customer) { <div class="selected-customer"><span class="avatar">{{ customer.name.slice(0,2).toUpperCase() }}</span><div><strong>{{ customer.name }}</strong><span>{{ customer.email }}</span></div><app-icon name="check"/></div> }
          </section>
          <section class="panel form-panel"><div class="section-title"><span class="step-number">02</span><div><h2>Les produits à facturer</h2><p>Les prix du catalogue sont appliqués à l’émission.</p></div></div>
            <div class="line-labels"><span>PRODUIT</span><span>QUANTITÉ</span><span class="numeric">MONTANT</span><span></span></div>
            @for (line of lines; track line.rowId) {
              <div class="draft-line">
                <label class="field"><span class="sr-only">Produit {{ $index+1 }}</span><select [name]="'product-' + line.rowId" [(ngModel)]="line.productId" (ngModelChange)="changed()" required><option [ngValue]="null" disabled>Choisir un produit</option>@for (product of products(); track product.id) { <option [ngValue]="product.id" [disabled]="usedByOther(product.id,line.rowId)">{{ product.name }} — {{ money(product.unitPrice) }}</option> }</select></label>
                <label class="field"><span class="sr-only">Quantité {{ $index+1 }}</span><input type="number" [name]="'qty-' + line.rowId" [(ngModel)]="line.quantity" (ngModelChange)="changed()" required min="1" max="1000000" step="1"></label>
                <strong class="numeric">{{ money(lineAmount(line)) }}</strong><button type="button" class="icon-button danger" (click)="removeLine(line.rowId)" [disabled]="lines.length===1" [attr.aria-label]="'Supprimer la ligne ' + ($index+1)"><app-icon name="trash"/></button>
              </div>
            }
            <button type="button" class="button secondary small add-line" (click)="addLine()" [disabled]="lines.length>=products().length || lines.length>=100"><app-icon name="plus"/>Ajouter une ligne</button>
            <p class="field-note">Les quantités facturées ne modifient pas le stock du catalogue.</p>
          </section>
        </div>
        <aside class="panel summary-card"><div class="eyebrow">LE RÉCAPITULATIF</div><h2>Tout est prêt ?</h2><div class="summary-icon"><app-icon name="file"/></div><div class="summary-row"><span>Lignes de produits</span><strong>{{ lines.length }}</strong></div><div class="summary-row"><span>Devise</span><strong>MAD</strong></div><div class="summary-total"><span>Total à facturer</span><strong>{{ money(total()) }}</strong><small>Montant final, sans TVA ajoutée.</small></div><button class="button primary full" [disabled]="form.invalid || !canSubmit() || saving() || loadingFailed()"><app-icon name="check"/>{{ saving() ? 'Création en cours…' : 'Créer la facture' }}</button><a class="cancel-link" routerLink="/factures">Annuler et revenir aux factures</a><p class="summary-note">Les coordonnées et prix sont conservés sur la facture après sa création.</p></aside>
      </div></fieldset></form>
    }
  `,
})
export class BillCreateComponent implements OnInit {
  private api = inject(ApiService); private router = inject(Router);
  customers = signal<Customer[]>([]); products = signal<Product[]>([]);
  loading = signal(true); loadingFailed = signal(false); saving = signal(false); error = signal('');
  customerId: number | null = null; minDate = today(); dueDate = today();
  lines: DraftLine[] = [{ rowId: 1, productId: null, quantity: 1 }]; nextRow = 2;
  private requestKey = crypto.randomUUID(); money = money;
  ngOnInit() { void this.load(); }
  async load() {
    this.loading.set(true); this.error.set(''); this.loadingFailed.set(false);
    try { const [customers, products] = await Promise.all([this.api.customers(), this.api.products()]); this.customers.set(customers); this.products.set(products); }
    catch(e) { this.error.set(errorMessage(e)); this.loadingFailed.set(true); } finally { this.loading.set(false); }
  }
  selectedCustomer() { return this.customers().find(c => c.id===this.customerId); }
  lineAmount(line: DraftLine) { return (this.products().find(p => p.id===line.productId)?.unitPrice || 0) * (line.quantity || 0); }
  total() { return this.lines.reduce((sum,line) => sum + this.lineAmount(line),0); }
  changed() { this.requestKey = crypto.randomUUID(); }
  usedByOther(id: number, rowId: number) { return this.lines.some(l => l.rowId!==rowId && l.productId===id); }
  addLine() { this.lines.push({ rowId: this.nextRow++, productId: null, quantity: 1 }); this.changed(); }
  removeLine(rowId: number) { if (this.lines.length>1) { this.lines = this.lines.filter(l => l.rowId!==rowId); this.changed(); } }
  canSubmit() {
    return this.customerId!==null && this.dueDate>=this.minDate && this.lines.length>0
      && this.lines.every(l => l.productId!==null && Number.isInteger(l.quantity) && l.quantity>=1 && l.quantity<=1000000)
      && new Set(this.lines.map(l => l.productId)).size===this.lines.length;
  }
  async save() {
    if (!this.canSubmit() || this.saving()) return;
    this.saving.set(true); this.error.set('');
    try {
      const bill = await this.api.createBill({ customerId: this.customerId!, dueDate: this.dueDate,
        items: this.lines.map(l => ({ productId: l.productId!, quantity: l.quantity })) }, this.requestKey);
      await this.router.navigate(['/factures',bill.id], { state: { created: true } });
    } catch(e) { this.error.set(errorMessage(e)); } finally { this.saving.set(false); }
  }
}
