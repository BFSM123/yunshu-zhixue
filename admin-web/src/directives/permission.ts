import type { Directive } from 'vue'
import { usePermissionStore } from '@/store/permission'

/**
 * 按钮级权限指令：无权限时移除元素。
 * 用法：<el-button v-permission="'question:edit'">编辑</el-button>
 */
const permissionDirective: Directive<HTMLElement, string> = {
  mounted(el, binding) {
    const store = usePermissionStore()
    if (binding.value && !store.has(binding.value)) {
      el.parentNode?.removeChild(el)
    }
  }
}

export default permissionDirective
