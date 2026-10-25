import { Routes } from '@angular/router';

export const routes: Routes = [
  { path: '', loadComponent: () => import('./dashboard.component').then(m => m.DashboardComponent), title: 'Vue d’ensemble — Facture' },
  { path: 'clients', loadComponent: () => import('./customers.component').then(m => m.CustomersComponent), title: 'Clients — Facture' },
  { path: 'produits', loadComponent: () => import('./products.component').then(m => m.ProductsComponent), title: 'Produits — Facture' },
  { path: 'factures/nouvelle', loadComponent: () => import('./bill-create.component').then(m => m.BillCreateComponent), title: 'Nouvelle facture — Facture' },
  { path: 'factures/:id', loadComponent: () => import('./bill-detail.component').then(m => m.BillDetailComponent), title: 'Détail de la facture — Facture' },
  { path: 'factures', loadComponent: () => import('./bills.component').then(m => m.BillsComponent), title: 'Factures — Facture' },
  { path: '**', redirectTo: '' },
];
