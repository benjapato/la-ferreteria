export function printDocument(mode) {
  const cleanup = () => document.body.classList.remove(mode)

  document.body.classList.add(mode)
  window.addEventListener('afterprint', cleanup, { once: true })

  try {
    window.print()
  } catch (error) {
    cleanup()
    throw error
  }
}
