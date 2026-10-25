export interface Customer { id: number; name: string; email: string; available: boolean; }
export interface Product { id: number; sku: string; name: string; unitPrice: number; quantity: number; }
export type BillStatus = 'UNPAID' | 'PAID' | 'CANCELLED';
export interface BillItem { id: number; productId: number; sku: string; productName: string; quantity: number; unitPrice: number; lineTotal: number; }
export interface Bill {
  id: number; number: string; issuedAt: string; dueDate: string; status: BillStatus;
  customerId: number; customerName: string; customerEmail: string; currency: string;
  total: number; items: BillItem[];
}
export interface BillDraft { customerId: number; dueDate: string; items: { productId: number; quantity: number }[]; }
