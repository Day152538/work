import { request, adminRequest, API_BASE } from '@/utils/request'
import { ElMessage } from 'element-plus'

/**
 * API 层：与后端 Controller 一一对应
 *
 * 相对原版（Vue2/api/index.js）的接口变更清单：
 * - userLogin: GET → POST（密码不再出现在 URL/访问日志）
 * - adminLogin: GET → POST，返回 token 存 admin_token
 * - userRet1(/user/reset1-password 任意人重置任意账号密码) → 删除，
 *   管理端改用 adminResetPassword(/admin/user/reset-password)
 * - deleteUser(/user/delete/{id}) → adminDeleteUser(/admin/user/delete/{id})
 * - updatePassword: GET → POST
 * - updateGoods/deleteOrder/updateUserStatus/regAdministrator 等
 *   管理接口统一改 POST（原来 GET 修改数据，违反 HTTP 语义且易被 CSRF）
 * - 新增 subscribeVip：会员开通走后端 /user/vip/subscribe 写 vip_expire_time，
 *   不再让前端伪造 userStatus=3
 * - 管理类接口（adminRequest）与用户类接口（request）使用不同 JWT
 */
/**
 * 解析一段 SSE 报文块（形如 "event: delta\ndata: 文本"），返回 { event, data }；空块返回 null。
 * SSE 规范：事件以空行分隔，字段为 "名: 值"，注释行以 ":" 开头。
 */
function parseSseBlock(block) {
  const lines = block.split('\n')
  let event = 'message'
  const dataParts = []
  for (const line of lines) {
    if (line.startsWith(':')) continue
    if (line.startsWith('event:')) event = line.slice(6).trim()
    else if (line.startsWith('data:')) dataParts.push(line.slice(5).replace(/^ /, ''))
  }
  if (!dataParts.length && event === 'message') return null
  return { event, data: dataParts.join('\n') }
}

const api = {
  // ==================== 用户 ====================
  userLogin(data) {
    // 后端 @RequestParam 接收表单字段
    return request.post('/user/login', null, { params: data })
  },
  userRet(data) {
    return request.post('/user/reset-password', null, { params: data })
  },
  logout() {
    return request.post('/user/logout')
  },
  signIn(data) {
    // 注册：账号/密码/昵称 + 验证码（captchaId、captchaCode），后端 @RequestParam 接收
    return request.post('/user/sign-in', null, { params: data })
  },
  getUserInfo(params) {
    return request.get('/user/info', { params })
  },
  userMe() {
    return request.get('/user/me')
  },
  updateUserPublicInfo(data) {
    return request.post('/user/info', data)
  },
  updatePassword(params) {
    return request.post('/user/password', null, { params })
  },
  /** 开通/续费会员（30 天），后端写 vip_expire_time */
  subscribeVip() {
    return request.post('/user/vip/subscribe')
  },

  // ==================== 收货地址 ====================
  addAddress(data) {
    return request.post('/address/add', data)
  },
  getAddress(params) {
    return request.get('/address/info', { params })
  },
  updateAddress(data) {
    return request.post('/address/update', data)
  },
  deleteAddress(data) {
    return request.post('/address/delete', data)
  },

  // ==================== 闲置商品 ====================
  addIdleItem(data) {
    return request.post('/idle/add', data)
  },
  getIdleItem(params) {
    return request.get('/idle/info', { params })
  },
  getAllIdleItem(params) {
    return request.get('/idle/all', { params })
  },
  findIdleTiem(params) {
    return request.get('/idle/find', { params })
  },
  findIdleTiemByLable(params) {
    return request.get('/idle/lable', { params })
  },
  updateIdleItem(data) {
    return request.post('/idle/update', data)
  },
  deleteGood(id) {
    return request.delete(`/idle/delete/${id}`)
  },

  // ==================== 订单 ====================
  addOrder(data, params) {
    return request.post('/order/add', data, { params })
  },
  getOrder(params) {
    return request.get('/order/info', { params })
  },
  updateOrder(data) {
    return request.post('/order/update', data)
  },
  getMyOrder(params) {
    return request.get('/order/my', { params })
  },
  getMySoldIdle(params) {
    return request.get('/order/my-sold', { params })
  },

  // ==================== 订单收货地址 ====================
  addOrderAddress(data) {
    return request.post('/order-address/add', data)
  },
  updateOrderAddress(data) {
    return request.post('/order-address/update', data)
  },
  getOrderAddress(params) {
    return request.get('/order-address/info', { params })
  },

  // ==================== 收藏 ====================
  addFavorite(data) {
    return request.post('/favorite/add', data)
  },
  getMyFavorite(params) {
    return request.get('/favorite/my', { params })
  },
  deleteFavorite(params) {
    return request.get('/favorite/delete', { params })
  },
  checkFavorite(params) {
    return request.get('/favorite/check', { params })
  },

  // ==================== 留言/消息 ====================
  sendMessage(data) {
    return request.post('/message/send', data)
  },
  getMessage(params) {
    return request.get('/message/info', { params })
  },
  getAllIdleMessage(params) {
    return request.get('/message/idle', { params })
  },
  getAllMyMessage(params) {
    return request.get('/message/my', { params })
  },
  deleteMessage(id) {
    return request.delete(`/message/delete/${id}`)
  },

  // ==================== 分类（读公开，写管理端） ====================
  listType(params) {
    return request.get('/type/listByCondition', { params })
  },
  addType(data) {
    return adminRequest.post('/type/add', data)
  },
  editType(data) {
    return adminRequest.post('/type/update', data)
  },
  deleteType(id) {
    return adminRequest.delete(`/type/delete/${id}`)
  },

  // ==================== 公告（读公开，写管理端） ====================
  getNotice(params) {
    return request.get('/notices', { params })
  },

  // ==================== 用户留言反馈 ====================
  addUserMessage(data) {
    return request.post('/userMessage/add', data)
  },
  getUserMessageList(params) {
    return request.get('/userMessage/listByUserId', { params })
  },
  editUserMessage(data) {
    return request.post('/userMessage/update', data)
  },
  deleteUserMessage(id) {
    return request.delete(`/userMessage/delete/${id}`)
  },

  // ==================== 管理端（admin_token） ====================
  adminLogin(data) {
    return adminRequest.post('/admin/login', null, { params: data })
  },
  adminLoginOut() {
    return adminRequest.post('/admin/loginOut')
  },
  getGoods(params) {
    return adminRequest.get('/admin/idleList', { params })
  },
  updateGoods(params) {
    return adminRequest.post('/admin/updateIdleStatus', null, { params })
  },
  getOrderList(params) {
    return adminRequest.get('/admin/orderList', { params })
  },
  deleteOrder(params) {
    return adminRequest.post('/admin/deleteOrder', null, { params })
  },
  getUserData(params) {
    return adminRequest.get('/admin/userList', { params })
  },
  getUserManage(params) {
    return adminRequest.get('/admin/list', { params })
  },
  updateUserStatus(params) {
    return adminRequest.post('/admin/updateUserStatus', null, { params })
  },
  regAdministrator(data) {
    return adminRequest.post('/admin/add', data)
  },
  updateAdministrator(data) {
    return adminRequest.post('/admin/update', data)
  },
  deleteAdministrator(id) {
    return adminRequest.delete(`/admin/delete/${id}`)
  },
  adminDeleteUser(id) {
    return adminRequest.delete(`/admin/user/delete/${id}`)
  },
  adminResetPassword(params) {
    return adminRequest.post('/admin/user/reset-password', null, { params })
  },
  getCarouselList(params) {
    return adminRequest.get('/admin/carouselList', { params })
  },
  addCarousel(data) {
    return adminRequest.post('/carousel/add', data)
  },
  updateCarousel(data) {
    return adminRequest.post('/carousel/update', data)
  },
  deleteCarousel(id) {
    return adminRequest.delete(`/carousel/delete/${id}`)
  },
  getIncomeChart(params) {
    return adminRequest.get('/api/income/chart', { params })
  },

  // ==================== 反馈信息（前台提交到 sh_user_message，管理端统一查看） ====================
  // 前台“信息反馈”(message1) 提交到 /userMessage/add（同一张表），
  // 管理员从这里读出所有用户反馈（联查昵称/账号）并按 id 删除。
  getUserMessagesAll() {
    return adminRequest.get('/admin/userMessage/list')
  },
  adminDeleteUserMessage(id) {
    return adminRequest.delete(`/admin/userMessage/delete/${id}`)
  },

  // ==================== 文件上传 ====================
  uploadFile(file) {
    const formData = new FormData()
    formData.append('file', file)
    return request.post('/file', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  },

  // ==================== AI 识图预填 / 合规质检 ====================
  // 预填：把「标题 + 用户所选分类 + 已上传图片文件名」交给后端 AI 网关，
  // 返回 { name, details, labelId, suggestPrice, priceReason } 发布草稿（需登录）
  aiPrefill(data) {
    return request.post('/ai/prefill', data)
  },
  // 合规质检：对标题 + 描述 + 图片做内容安全初审，返回 { riskLevel, reasons }（需登录）
  aiCompliance(data) {
    return request.post('/ai/compliance', data)
  },
  // AI 对话助手：{ message, conversationId?, webSearch? } → { reply, conversationId }（需登录）
  aiChat(data) {
    return request.post('/ai/chat', data)
  },
  // AI 对话助手（流式 SSE 版）：{ message, conversationId?, webSearch? } → text/event-stream
  // 事件：delta（文本增量，onDelta(text)）/ done（onDone(conversationId)）/ error（onError(msg)）
  // 必须用 fetch（EventSource 不支持带 Authorization 头、也不支持 POST body）
  aiChatStream(data, handlers = {}) {
    const { onDelta, onDone, onError } = handlers
    const token = localStorage.getItem('token')
    return fetch(`${API_BASE}/ai/chat/stream`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        ...(token ? { Authorization: `Bearer ${token}` } : {})
      },
      body: JSON.stringify(data)
    }).then(async (resp) => {
      // 校验失败（未登录/缺参/未配置 Key）返回的是 JSON 而非 SSE，按普通错误处理
      if (!resp.ok) {
        if (resp.status === 401) {
          const hadToken = !!localStorage.getItem('token')
          localStorage.removeItem('token')
          if (hadToken && !location.pathname.startsWith('/login')) {
            ElMessage.error('登录已过期，请重新登录')
            location.href = '/login'
          }
          throw new Error('登录已过期，请重新登录')
        }
        let msg = 'AI 回复失败，请稍后重试'
        try {
          const j = await resp.json()
          msg = j.msg || msg
        } catch (e) { /* 非 JSON 响应，保留默认提示 */ }
        throw new Error(msg)
      }
      const reader = resp.body.getReader()
      const decoder = new TextDecoder('utf-8')
      let buf = ''
      while (true) {
        const { done, value } = await reader.read()
        if (done) break
        buf += decoder.decode(value, { stream: true })
        let idx
        while ((idx = buf.indexOf('\n\n')) >= 0) {
          const block = buf.slice(0, idx)
          buf = buf.slice(idx + 2)
          const ev = parseSseBlock(block)
          if (!ev) continue
          if (ev.event === 'delta' && typeof onDelta === 'function') {
            onDelta(ev.data || '')
          } else if (ev.event === 'done' && typeof onDone === 'function') {
            let conv = ''
            try { conv = JSON.parse(ev.data).conversationId || '' } catch (e) { /* 忽略 */ }
            onDone(conv)
          } else if (ev.event === 'error' && typeof onError === 'function') {
            onError(ev.data || 'AI 回复失败，请稍后重试')
          }
        }
      }
    })
  },
  // 取某会话历史（刷新/换设备后恢复对话）
  aiChatHistory(conversationId) {
    return request.get('/ai/chat/history', { params: { conversationId } })
  },

  // ==================== 订单私信（下单后买卖双方沟通） ====================
  chatSend(data) {
    return request.post('/chat/send', data)
  },
  chatMessages(params) {
    return request.get('/chat/messages', { params })
  },
  chatConversations() {
    return request.get('/chat/conversations')
  }
}

export default api
