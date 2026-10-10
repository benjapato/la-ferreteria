import assert from 'node:assert/strict'
import { test } from 'node:test'
import { allowsFraction, availableQuantity, lineSubtotal, minimumQuantity, quantityValue, validQuantity } from './quantities.js'

for (const unidadMedida of ['METRO', 'KILOGRAMO', 'LITRO']) {
  test(`${unidadMedida}: acepta punto, coma y hasta tres decimales`, () => {
    const product = { unidadMedida, stock: 10 }
    assert.equal(allowsFraction(product), true)
    for (const value of ['1.20', '1,20', '0.750', '0,001', '.5', ',5']) assert.equal(validQuantity(value, product), true, value)
    for (const value of ['', '0', '11', '1.0001', '1,2,3', '-1', 'NaN', '1.20m']) assert.equal(validQuantity(value, product), false, value)
    assert.equal(minimumQuantity(product), 0.001)
    assert.equal(availableQuantity({ ...product, stock: '0.350' }), 0.35)
  })
}

for (const unidadMedida of ['UNIDAD', 'BOLSA', 'CAJA', 'PAQUETE', 'ROLLO']) {
  test(`${unidadMedida}: el carrito conserva cantidades enteras`, () => {
    const product = { unidadMedida, stock: 5.5 }
    assert.equal(allowsFraction(product), false)
    assert.equal(validQuantity('2', product), true)
    assert.equal(validQuantity('1.2', product), false)
    assert.equal(validQuantity('1,2', product), false)
    assert.equal(validQuantity('6', product), false)
    assert.equal(availableQuantity(product), 5)
  })
}

test('Cantidades no se convierten a centimetros o gramos ni se truncan', () => {
  assert.equal(quantityValue('1.20'), 1.2)
  assert.equal(quantityValue('1,20'), 1.2)
  assert.equal(quantityValue('0.750'), 0.75)
  assert.equal(lineSubtotal(1000, '1,20'), 1200)
  assert.equal(lineSubtotal(2000, '0.750'), 1500)
})

test('Redondeo monetario coincide con HALF_UP del servidor por renglon', () => {
  assert.equal(lineSubtotal(1, '0.335'), 0.34)
  assert.equal(lineSubtotal(1, '1.005'), 1.01)
  assert.equal(lineSubtotal(1, '1.255'), 1.26)
  assert.equal(lineSubtotal(0.01, '0.5'), 0.01)
  assert.equal(lineSubtotal(1234.56, '1.234'), 1523.45)
  assert.equal([0.335, 0.335].reduce((cents, qty) => cents + Math.round(lineSubtotal(1, qty) * 100), 0) / 100, 0.68)
})
