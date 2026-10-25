import { Component, Input } from '@angular/core';
import { BillStatus } from './models';

@Component({
  selector: 'app-status',
  template: `<span class="status" [class.paid]="status === 'PAID'" [class.cancelled]="status === 'CANCELLED'"><span></span>{{ labels[status] }}</span>`,
})
export class StatusComponent {
  @Input() status: BillStatus = 'UNPAID';
  labels = { UNPAID: 'À régler', PAID: 'Payée', CANCELLED: 'Annulée' };
}
