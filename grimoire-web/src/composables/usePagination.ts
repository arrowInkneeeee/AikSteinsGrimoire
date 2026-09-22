import { ref } from 'vue'
import type { PageResult } from '@/api/types/api'

/**
 * 通用分页 composable
 *
 * @param fetchFn 分页请求函数，接收 { current, size } 参数
 * @param initialSize 每页条数，默认 20
 */
export function usePagination<T>(
  fetchFn: (params: { current: number; size: number }) => Promise<PageResult<T>>,
  initialSize = 20,
) {
  const loading = ref(false)
  const records = ref<T[]>([])
  const total = ref('0')
  const current = ref(1)
  const size = ref(initialSize)

  async function fetch(page = 1) {
    loading.value = true
    try {
      current.value = page
      const result = await fetchFn({ current: page, size: size.value })
      records.value = result.records
      total.value = result.total
    } finally {
      loading.value = false
    }
  }

  return { loading, records, total, current, size, fetch }
}
