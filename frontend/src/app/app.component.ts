import { Component } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { IconComponent } from './icon.component';

@Component({
  selector: 'app-root',
  imports: [RouterLink, RouterLinkActive, RouterOutlet, IconComponent],
  template: `
    <a class="skip-link" href="#main">Aller au contenu</a>
    <aside class="sidebar">
      <a class="brand" routerLink="/" aria-label="Facture, accueil"><span class="brand-mark">f.</span><span>facture<span class="brand-dot">.</span></span></a>
      <div class="workspace"><span class="workspace-avatar">AD</span><span><strong>Atelier Digital</strong><small>Espace de démonstration</small></span></div>
      <span class="nav-caption">VOTRE ACTIVITÉ</span>
      <nav aria-label="Navigation principale">
        <a routerLink="/" routerLinkActive="active" [routerLinkActiveOptions]="{exact:true}"><app-icon name="home"/>Vue d’ensemble</a>
        <a routerLink="/factures" routerLinkActive="active"><app-icon name="file"/>Factures</a>
        <a routerLink="/clients" routerLinkActive="active"><app-icon name="users"/>Clients</a>
        <a routerLink="/produits" routerLinkActive="active"><app-icon name="box"/>Produits</a>
      </nav>
      <div class="sidebar-note"><span class="note-dot"></span><strong>Moins d’administratif.<br>Plus d’essentiel.</strong><p>Votre activité, simplement.</p></div>
      <div class="sidebar-footer"><span class="user-avatar">AD</span><span><strong>Atelier Digital</strong><small>Gestion commerciale</small></span></div>
    </aside>
    <div class="app-main">
      <header class="topbar"><span>VOTRE ESPACE DE GESTION</span><span class="topbar-date"><app-icon name="calendar"/>{{ currentDate }}</span></header>
      <main id="main" tabindex="-1"><router-outlet/></main>
      <footer class="main-footer"><span>Facture. <span class="muted">La gestion, en toute clarté.</span></span><span>Prix en dirhams marocains · MAD</span></footer>
    </div>
  `,
})
export class AppComponent {
  currentDate = new Intl.DateTimeFormat('fr-FR', { day: 'numeric', month: 'long', year: 'numeric' }).format(new Date());
}
