import { Component, ElementRef, OnInit, inject, signal, viewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ApiService, errorMessage } from './api.service';
import { Product } from './models';
import { IconComponent } from './icon.component';
import { money } from './format';

@Component({
  imports: [FormsModule, IconComponent],
  template: `
    <div class="page-heading"><div><div class="eyebrow">CHAQUE DÉTAIL A SA PLACE</div><h1>Vos produits<span class="orange-dot">.</span></h1><p>Un catalogue clair pour des factures précises.</p></div><button class="button primary" (click)="open()"><app-icon name="plus"/>Nouveau produit</button></div>
    @if (error()) { <div class="notice error" role="alert">{{ error() }} <button (click)="load()">Réessayer</button></div> }
    @if (success()) { <div class="notice success" role="status">{{ success() }}</div> }
    <section class="panel"><div class="list-toolbar"><div class="list-title"><h2>Catalogue produits</h2><span class="count-badge">{{ products().length }}</span></div><label class="search-field"><app-icon name="search"/><input aria-label="Rechercher un produit" placeholder="Nom ou référence du produit…" [(ngModel)]="search" (ngModelChange)="page=0"></label></div>
      @if (loading()) { <div class="loading-state" role="status"><span class="spinner"></span>Chargement du catalogue…</div> }
      @else if (!filtered.length) { <div class="empty-state"><app-icon name="box"/><h3>{{ search ? 'Aucun produit ne correspond' : 'Le début de votre catalogue' }}</h3><p>{{ search ? 'Essayez une autre recherche.' : 'Ajoutez un produit et son prix pour vos prochaines factures.' }}</p>@if (!search) { <button class="button secondary" (click)="open()">Ajouter un produit</button> }</div> }
      @else {
        <div class="table-wrap"><table><thead><tr><th>PRODUIT</th><th>RÉFÉRENCE</th><th class="numeric">PRIX UNITAIRE</th><th>QUANTITÉ EN STOCK</th><th class="numeric">ACTIONS</th></tr></thead><tbody>
        @for (product of filtered.slice(page*10,page*10+10); track product.id) {
          <tr><td><div class="person-cell"><span class="product-icon"><app-icon name="box"/></span><strong>{{ product.name }}</strong></div></td><td><span class="sku">{{ product.sku }}</span></td><td class="numeric amount">{{ money(product.unitPrice) }}</td><td><span class="stock" [class.low]="product.quantity < 10"><i></i>{{ product.quantity }} unités</span></td><td><div class="row-actions"><button class="icon-button" (click)="open(product)" [attr.aria-label]="'Modifier ' + product.name"><app-icon name="edit"/></button><button class="icon-button danger" (click)="remove(product)" [disabled]="deleting()===product.id" [attr.aria-label]="'Supprimer ' + product.name"><app-icon name="trash"/></button></div></td></tr>
        }</tbody></table></div><div class="pagination"><span>{{ filtered.length }} produit(s) · Prix en MAD</span><div><button class="button small secondary" [disabled]="page===0" (click)="page=page-1">Précédent</button><span>Page {{ page+1 }}</span><button class="button small secondary" [disabled]="(page+1)*10>=filtered.length" (click)="page=page+1">Suivant</button></div></div>
      }
    </section>
    <dialog #editor class="editor-dialog" aria-labelledby="product-dialog-title"><form (ngSubmit)="save()" #form="ngForm">
      <div class="dialog-heading"><div><span class="eyebrow">CATALOGUE</span><h2 id="product-dialog-title">{{ editingId ? 'Modifier le produit' : 'Nouveau produit' }}</h2></div><button type="button" class="icon-button" (click)="close()" aria-label="Fermer"><app-icon name="close"/></button></div>
      @if (formError()) { <div class="notice error" role="alert">{{ formError() }}</div> }
      <label class="field">Nom du produit<input name="name" [(ngModel)]="draft.name" required maxlength="160" placeholder="Ex. Écran Dell 27 pouces" autofocus></label>
      <label class="field">Référence<input name="sku" [(ngModel)]="draft.sku" required pattern="[A-Z0-9][A-Z0-9_-]{1,39}" maxlength="40" placeholder="Ex. MON-006"><small>2 à 40 caractères : lettres majuscules, chiffres, tirets.</small></label>
      <div class="form-grid"><label class="field">Prix unitaire (MAD)<input name="unitPrice" type="number" [(ngModel)]="draft.unitPrice" required min="0.01" max="999999999999.99" step="0.01"></label><label class="field">Quantité en stock<input name="quantity" type="number" [(ngModel)]="draft.quantity" required min="0" max="1000000" step="1"></label></div>
      <div class="dialog-actions"><button type="button" class="button secondary" (click)="close()">Annuler</button><button class="button primary" [disabled]="form.invalid || saving()">{{ saving() ? 'Enregistrement…' : 'Enregistrer le produit' }}</button></div>
    </form></dialog>
  `,
})
export class ProductsComponent implements OnInit {
  private api = inject(ApiService); editor = viewChild.required<ElementRef<HTMLDialogElement>>('editor');
  products = signal<Product[]>([]); loading = signal(true); saving = signal(false); deleting = signal<number | null>(null);
  error = signal(''); formError = signal(''); success = signal('');
  search = ''; page = 0; editingId?: number; draft = { name: '', sku: '', unitPrice: 0, quantity: 0 }; money = money;
  get filtered() { const q = this.search.trim().toLowerCase(); return this.products().filter(p => (p.name + ' ' + p.sku).toLowerCase().includes(q)); }
  ngOnInit() { void this.load(); }
  async load() { this.loading.set(true); this.error.set(''); try { this.products.set(await this.api.products()); } catch(e) { this.error.set(errorMessage(e)); } finally { this.loading.set(false); } }
  open(product?: Product) { this.editingId = product?.id; this.draft = product ? { name: product.name, sku: product.sku, unitPrice: product.unitPrice, quantity: product.quantity } : { name: '', sku: '', unitPrice: 0, quantity: 0 }; this.formError.set(''); this.editor().nativeElement.showModal(); }
  close() { if (!this.saving()) this.editor().nativeElement.close(); }
  async save() {
    if (!this.draft.name.trim() || this.saving()) return;
    this.saving.set(true); this.formError.set('');
    try { await this.api.saveProduct(this.draft, this.editingId); this.editor().nativeElement.close(); this.success.set('Le produit a bien été enregistré.'); await this.load(); }
    catch(e) { this.formError.set(errorMessage(e)); } finally { this.saving.set(false); }
  }
  async remove(product: Product) {
    if (!window.confirm('Supprimer « ' + product.name + ' » du catalogue ? Les factures déjà émises conserveront leurs informations.')) return;
    this.deleting.set(product.id); this.error.set('');
    try { await this.api.deleteProduct(product.id); this.success.set('Le produit a été supprimé du catalogue.'); this.page = 0; await this.load(); }
    catch(e) { this.error.set(errorMessage(e)); } finally { this.deleting.set(null); }
  }
}
