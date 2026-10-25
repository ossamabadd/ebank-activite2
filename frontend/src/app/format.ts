export function money(value: number): string {
  return new Intl.NumberFormat('fr-MA', { style: 'currency', currency: 'MAD', maximumFractionDigits: 2 }).format(value);
}
export function date(value: string): string {
  return new Intl.DateTimeFormat('fr-FR', { day: '2-digit', month: 'short', year: 'numeric' }).format(new Date(value.length === 10 ? value + 'T12:00:00' : value));
}
export function today(): string {
  const d = new Date();
  return [d.getFullYear(), String(d.getMonth() + 1).padStart(2, '0'), String(d.getDate()).padStart(2, '0')].join('-');
}
export function initials(name: string): string { return name.split(' ').map(p => p[0]).join('').slice(0, 2).toUpperCase(); }
