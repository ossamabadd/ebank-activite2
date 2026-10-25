import { Component, ElementRef, OnInit, inject, signal, viewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ApiService, errorMessage } from './api.service';
import { Customer } from './models';
import { IconComponent } from './icon.component';
import { initials } from './format';

@Component({
  imports: [FormsModule, RouterLink, IconComponent],
  template: `
    <div class="page-heading"><div><div class="eyebrow">DES RELATIONS QUI COMPTENT</div><h1>Vos clients<span class="orange-dot">.</span></h1><p>Les bons contacts, toujours à portée de main.</p></div><button class="button primary" (click)="open()"><app-icon name="plus"/>Nouveau client</button></div>
    @if (error()) { <div class="notice error" role="alert">{{ error() }} <button (click)="load()">Réessayer</button></div> }
    @if (success()) { <div class="notice success" role="status">{{ success() }}</div> }
    <section class="panel"><div class="list-toolbar"><div class="list-title"><h2>Carnet clients</h2><span class="count-badge">{{ customers().length }}</span></div><label class="search-field"><app-icon name="search"/><input aria-label="Rechercher un client" placeholder="Rechercher un client…" [(ngModel)]="search" (ngModelChange)="page=0"></label></div>
      @if (loading()) { <div class="loading-state" role="status"><span class="spinner"></span>Chargement des clients…</div> }
      @else if (!filtered.length) { <div class="empty-state"><app-icon name="users"/><h3>{{ search ? 'Aucun client ne correspond' : 'Faites entrer votre premier client' }}</h3><p>{{ search ? 'Essayez un autre nom ou une autre adresse e-mail.' : 'Ajoutez un contact pour préparer vos factures.' }}</p>@if (!search) { <button class="button secondary" (click)="open()">Ajouter un client</button> }</div> }
      @else {
        <div class="table-wrap"><table><thead><tr><th>CLIENT</th><th>ADRESSE E-MAIL</th><th>IDENTIFIANT</th><th class="numeric">ACTIONS</th></tr></thead><tbody>
        @for (customer of filtered.slice(page*10,page*10+10); track customer.id) {
          <tr><td><div class="person-cell"><span class="avatar">{{ initials(customer.name) }}</span><strong>{{ customer.name }}</strong></div></td><td class="muted">{{ customer.email }}</td><td class="muted">CL-{{ customer.id.toString().padStart(4,'0') }}</td><td><div class="row-actions"><a class="text-link" routerLink="/factures" [queryParams]="{client:customer.id}">Factures <app-icon name="arrow"/></a><button class="icon-button" (click)="open(customer)" [attr.aria-label]="'Modifier ' + customer.name"><app-icon name="edit"/></button></div></td></tr>
        }</tbody></table></div><div class="pagination"><span>{{ filtered.length }} client(s)</span><div><button class="button small secondary" [disabled]="page===0" (click)="page=page-1">Précédent</button><span>Page {{ page+1 }}</span><button class="button small secondary" [disabled]="(page+1)*10>=filtered.length" (click)="page=page+1">Suivant</button></div></div>
      }
    </section>
    <dialog #editor class="editor-dialog" aria-labelledby="customer-dialog-title"><form (ngSubmit)="save()" #form="ngForm">
      <div class="dialog-heading"><div><span class="eyebrow">CARNET CLIENTS</span><h2 id="customer-dialog-title">{{ editingId ? 'Modifier le client' : 'Nouveau client' }}</h2></div><button type="button" class="icon-button" (click)="close()" aria-label="Fermer"><app-icon name="close"/></button></div>
      @if (formError()) { <div class="notice error" role="alert">{{ formError() }}</div> }
      <label class="field">Nom du client<input name="name" [(ngModel)]="draft.name" required maxlength="120" placeholder="Ex. Sarah Benali" autofocus></label>
      <label class="field">Adresse e-mail<input name="email" type="email" [(ngModel)]="draft.email" required email maxlength="200" placeholder="sarah@entreprise.ma"></label>
      <div class="dialog-actions"><button type="button" class="button secondary" (click)="close()">Annuler</button><button class="button primary" [disabled]="form.invalid || saving()">{{ saving() ? 'Enregistrement…' : 'Enregistrer le client' }}</button></div>
    </form></dialog>
  `,
})
export class CustomersComponent implements OnInit {
  private api = inject(ApiService);
  editor = viewChild.required<ElementRef<HTMLDialogElement>>('editor');
  customers = signal<Customer[]>([]); loading = signal(true); saving = signal(false);
  error = signal(''); formError = signal(''); success = signal('');
  search = ''; page = 0; editingId?: number; draft = { name: '', email: '' }; initials = initials;
  get filtered() { const q = this.search.trim().toLowerCase(); return this.customers().filter(c => (c.name + ' ' + c.email).toLowerCase().includes(q)); }
  ngOnInit() { void this.load(); }
  async load() { this.loading.set(true); this.error.set(''); try { this.customers.set(await this.api.customers()); } catch(e) { this.error.set(errorMessage(e)); } finally { this.loading.set(false); } }
  open(customer?: Customer) { this.editingId = customer?.id; this.draft = { name: customer?.name || '', email: customer?.email || '' }; this.formError.set(''); this.editor().nativeElement.showModal(); }
  close() { if (!this.saving()) this.editor().nativeElement.close(); }
  async save() {
    if (!this.draft.name.trim() || !this.draft.email.trim() || this.saving()) return;
    this.saving.set(true); this.formError.set('');
    try { await this.api.saveCustomer(this.draft, this.editingId); this.editor().nativeElement.close(); this.success.set('Le client a bien été enregistré.'); await this.load(); }
    catch(e) { this.formError.set(errorMessage(e)); } finally { this.saving.set(false); }
  }
}
