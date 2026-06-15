export function formatDateTime(value: string | null | undefined) {
  if (!value) return '-'
  const normalized = value.includes('T') ? value : value.replace(' ', 'T')
  const [datePart, timePart = ''] = normalized.split('T')
  const [year, month, day] = datePart.split('-')
  const [hour = '00', minute = '00'] = timePart.split(':')

  if (!year || !month || !day) return value
  return `${year}-${month}-${day} ${hour.padStart(2, '0')}:${minute.padStart(2, '0')}`
}
