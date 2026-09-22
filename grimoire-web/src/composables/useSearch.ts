import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useDebounceFn } from '@vueuse/core'

export function useSearch() {
  const router = useRouter()
  const keyword = ref('')

  const doSearch = useDebounceFn(() => {
    const q = keyword.value.trim()
    if (q) {
      router.push({ path: '/search', query: { q } })
    }
  }, 300)

  return { keyword, doSearch }
}
