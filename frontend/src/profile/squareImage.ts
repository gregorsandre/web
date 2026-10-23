const SIZE = 512
const QUALITY = 0.85
const CANNOT_PREPARE = 'Your browser could not prepare the picture.'

// Crops the middle square of a picture and scales it to 512×512, so uploads stay
// small and every avatar has the same shape
export async function squareImage(file: Blob): Promise<Blob> {
  let bitmap: ImageBitmap
  try {
    bitmap = await createImageBitmap(file)
  } catch {
    throw new Error("That file isn't an image we can read.")
  }

  const side = Math.min(bitmap.width, bitmap.height)
  const canvas = document.createElement('canvas')
  canvas.width = SIZE
  canvas.height = SIZE
  const context = canvas.getContext('2d')
  if (!context) {
    bitmap.close()
    throw new Error(CANNOT_PREPARE)
  }
  // JPEG has no transparency; without this, transparent PNG areas turn black
  context.fillStyle = '#ffffff'
  context.fillRect(0, 0, SIZE, SIZE)
  context.drawImage(bitmap, (bitmap.width - side) / 2, (bitmap.height - side) / 2, side, side, 0, 0, SIZE, SIZE)
  bitmap.close()

  return new Promise((resolve, reject) => {
    canvas.toBlob((blob) => (blob ? resolve(blob) : reject(new Error(CANNOT_PREPARE))), 'image/jpeg', QUALITY)
  })
}
