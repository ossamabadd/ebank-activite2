import { Component, Input } from '@angular/core';

@Component({
  selector: 'app-icon',
  template: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path [attr.d]="paths[name] || paths['file']"/></svg>`,
  styles: [':host{display:inline-flex;width:20px;height:20px;flex-shrink:0}svg{width:100%;height:100%}'],
})
export class IconComponent {
  @Input() name = 'file';
  paths: Record<string, string> = {
    home: 'M3 10 12 3l9 7v10a1 1 0 0 1-1 1h-5v-7H9v7H4a1 1 0 0 1-1-1Z',
    file: 'M14 2H5v20h14V7ZM14 2v6h5M8 12h8M8 16h6',
    users: 'M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2M16 3a4 4 0 0 1 0 8M22 21v-2a4 4 0 0 0-3-3.87M13 7a4 4 0 1 1-8 0 4 4 0 0 1 8 0',
    box: 'm12 2 10 5-10 5L2 7Zm10 5v10l-10 5L2 17V7M12 12v10M7 4.5l10 5',
    plus: 'M12 5v14M5 12h14', search: 'M21 21l-5-5M18 10a8 8 0 1 1-16 0 8 8 0 0 1 16 0',
    arrow: 'M5 12h14m-5-5 5 5-5 5', back: 'M19 12H5m5-5-5 5 5 5',
    close: 'm6 6 12 12M6 18 18 6', check: 'm5 12 4 4L19 6',
    edit: 'm16 3 5 5-12 12-6 1 1-6ZM14 5l5 5',
    trash: 'M3 6h18M9 6V3h6v3M5 6l1 15h12l1-15M10 10v7M14 10v7',
    clock: 'M12 7v5l3 2M22 12a10 10 0 1 1-20 0 10 10 0 0 1 20 0',
    chevron: 'm9 5 7 7-7 7', calendar: 'M8 2v4M16 2v4M3 10h18M3 4h18v18H3Z',
    download: 'M12 3v12m-5-5 5 5 5-5M4 17v4h16v-4',
    wallet: 'M20 7V4H4v16h16v-5M4 7h18v10h-7V9h7M17 13h.01',
    trend: 'm3 17 6-6 4 4 8-10M15 5h6v6',
  };
}
