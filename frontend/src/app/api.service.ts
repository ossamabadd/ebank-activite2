import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { Bill, BillDraft, BillStatus, Customer, Product } from './models';

@Injectable({ providedIn: 'root' })
export class ApiService {
  private http = inject(HttpClient);
  private async all<T>(path: string): Promise<T[]> {
    const values: T[] = [];
    for (let page = 0; ; page++) {
      const batch = await firstValueFrom(this.http.get<T[]>(path, { params: { page, size: 100 } }));
      values.push(...batch);
      if (batch.length < 100) return values;
    }
  }
  customers() { return this.all<Customer>('/api/customers'); }
  products() { return this.all<Product>('/api/products'); }
  bills() { return this.all<Bill>('/api/bills'); }
  bill(id: number) { return firstValueFrom(this.http.get<Bill>('/api/bills/' + id)); }
  saveCustomer(value: { name: string; email: string }, id?: number) {
    return firstValueFrom(id ? this.http.put<Customer>('/api/customers/' + id, value) : this.http.post<Customer>('/api/customers', value));
  }
  saveProduct(value: Omit<Product, 'id'>, id?: number) {
    return firstValueFrom(id ? this.http.put<Product>('/api/products/' + id, value) : this.http.post<Product>('/api/products', value));
  }
  deleteProduct(id: number) { return firstValueFrom(this.http.delete<void>('/api/products/' + id)); }
  createBill(value: BillDraft, key: string) {
    return firstValueFrom(this.http.post<Bill>('/api/bills', value, { headers: { 'Idempotency-Key': key } }));
  }
  changeStatus(id: number, status: BillStatus) {
    return firstValueFrom(this.http.patch<Bill>('/api/bills/' + id + '/status', { status }));
  }
}
export function errorMessage(error: unknown): string {
  if (error instanceof HttpErrorResponse) {
    if (error.status === 0 || error.status >= 500) return 'Le service est temporairement indisponible. Réessayez dans quelques instants.';
    if (error.status === 409) return error.error?.detail || 'Cette modification entre en conflit avec les données existantes.';
    if (error.status === 404) return 'Cet élément est introuvable. Actualisez la page.';
    return error.error?.detail || 'Les données sont invalides. Vérifiez les champs du formulaire.';
  }
  return 'Une erreur est survenue. Veuillez réessayer.';
}
