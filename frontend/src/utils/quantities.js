export const allowsFraction = (product) => ['METRO', 'KILOGRAMO', 'LITRO'].includes(product.unidadMedida)
export const quantityValue = (value) => Number(String(value).replace(',', '.'))
export const minimumQuantity = (product) => allowsFraction(product) ? 0.001 : 1
export const availableQuantity = (product) => Math.max(0, allowsFraction(product) ? Number(product.stock) || 0 : Math.floor(Number(product.stock) || 0))

export function validQuantity(value, product) {
  const quantity = quantityValue(value)
  const pattern = allowsFraction(product) ? /^\d*(?:[.,]\d{0,3})?$/ : /^\d+$/
  return pattern.test(String(value)) && Number.isFinite(quantity)
    && quantity >= minimumQuantity(product) && quantity <= availableQuantity(product)
}

export function lineSubtotal(price, quantity) {
  const amount = quantityValue(quantity)
  if (!Number.isFinite(amount) || !Number.isFinite(Number(price))) return 0
  // Match the server: round each line to cents, with quantities in thousandths.
  return Math.round(Math.round(Number(price) * 100) * Math.round(amount * 1000) / 1000) / 100
}
