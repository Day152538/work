/**
 * 图片前端压缩（P0-2：识图链路大图会又慢又贵）
 *
 * 上传前把图片压缩到「最长边 ≤ maxSide(默认 1280px)、JPEG quality ≤0.8、
 * 体积 ≤ maxBytes(默认 1.5MB)」，再走原有 /file 上传；既降低 AI 识图成本，
 * 也顺带减小商品图占用的存储。若原图已满足限制则原样返回，不做无谓重编码。
 */
const DEFAULTS = { maxSide: 1280, quality: 0.8, maxBytes: 1.5 * 1024 * 1024 }

/** 压缩单个 File；返回 File 或 Promise<File>。 */
export async function compressImageFile(file, opts = {}) {
  const { maxSide, quality, maxBytes } = { ...DEFAULTS, ...opts }
  if (!file || !/^image\//.test(file.type)) return file

  const img = await loadImage(await readAsDataURL(file))
  const scale = Math.min(1, maxSide / Math.max(img.width, img.height))
  const width = Math.max(1, Math.round(img.width * scale))
  const height = Math.max(1, Math.round(img.height * scale))
  const canvas = document.createElement('canvas')
  canvas.width = width
  canvas.height = height
  const ctx = canvas.getContext('2d')
  ctx.drawImage(img, 0, 0, width, height)

  // 逐步降低质量直到满足体积上限（最多降到 0.5）
  let q = quality
  let blob = await canvasToBlob(canvas, 'image/jpeg', q)
  while (blob.size > maxBytes && q > 0.5) {
    q = Math.max(0.5, +(q - 0.1).toFixed(2))
    blob = await canvasToBlob(canvas, 'image/jpeg', q)
  }
  // 压缩后没有更小就保留原图（避免 PNG 有损化反而变大等边界）
  if (blob.size >= file.size) return file
  const base = (file.name || 'image').replace(/\.[^.]+$/, '')
  return new File([blob], `${base}.jpg`, { type: 'image/jpeg' })
}

function readAsDataURL(file) {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = () => resolve(reader.result)
    reader.onerror = () => reject(reader.error || new Error('read file failed'))
    reader.readAsDataURL(file)
  })
}

function loadImage(src) {
  return new Promise((resolve, reject) => {
    const img = new Image()
    img.onload = () => resolve(img)
    img.onerror = () => reject(new Error('decode image failed'))
    img.src = src
  })
}

function canvasToBlob(canvas, type, quality) {
  return new Promise((resolve, reject) => {
    canvas.toBlob(
      (blob) => (blob ? resolve(blob) : reject(new Error('encode image failed'))),
      type,
      quality
    )
  })
}
