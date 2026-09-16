import mitt from 'mitt'

// 事件总线：Vue3 移除了 $on/$off/$emit 实例方法，
// 官方推荐用 mitt 库替代原来的 `new Vue()` 事件总线方案
const bus = mitt()

export default bus
