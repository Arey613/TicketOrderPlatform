export function formatDateTime(value: Date): string {
  return new Intl.DateTimeFormat(undefined, {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(value);
}

export function formatPrice(price?: string, currency?: string): string {
  if (!price) {
    return 'TBA';
  }

  return currency ? `${price} ${currency}` : price;
}
