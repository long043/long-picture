<template>
  <a-modal
    class="image-enhance"
    v-model:visible="visible"
    title="AI 清晰"
    :footer="false"
    @cancel="closeModal"
  >
    <!-- 选择图片 -->
    <div v-if="!displayOriginal" style="text-align: center; padding: 24px">
      <a-upload :before-upload="beforeUpload" :show-upload-list="false" accept="image/*">
        <a-button type="primary">选择图片</a-button>
      </a-upload>
      <p style="color: #999; margin-top: 12px">支持 JPG、PNG 格式，限 3MB 以内</p>
    </div>
    <!-- 图片对比 -->
    <div v-else>
      <a-row gutter="16">
        <a-col span="12">
          <h4>原始图片</h4>
          <img :src="displayOriginal" style="max-width: 100%" />
        </a-col>
        <a-col span="12">
          <h4>增强结果</h4>
          <img v-if="displayEnhanced" :src="displayEnhanced" style="max-width: 100%" />
          <div v-else style="color: #999; text-align: center; padding: 20px">等待生成</div>
        </a-col>
      </a-row>
    </div>
    <div style="margin-bottom: 16px" />
    <a-flex justify="center" gap="16">
      <a-button
        v-if="displayOriginal && !displayEnhanced"
        type="primary"
        :loading="loading"
        ghost
        @click="doEnhance"
      >
        开始增强
      </a-button>
      <a-button v-if="displayOriginal" @click="resetFile">重新选择</a-button>
      <a-button
        v-if="displayEnhanced"
        type="primary"
        :loading="uploadLoading"
        @click="handleUpload"
      >
        应用结果
      </a-button>
    </a-flex>
  </a-modal>
</template>

<script lang="ts" setup>
import { ref } from 'vue'
import myAxios from '@/request'
import { message } from 'ant-design-vue'
import { uploadPictureUsingPost } from '@/api/pictureController.ts'

interface Props {
  picture?: API.PictureVO
  spaceId?: number
  onSuccess?: (newPicture: API.PictureVO) => void
}

const props = defineProps<Props>()

const visible = ref(false)
const loading = ref(false)
const uploadLoading = ref(false)
// 纯 Base64 数据（无前缀），用于发送给后端
const rawBase64 = ref<string>('')
// 带 data URI 前缀的完整 Base64，用于 <img> 显示
const displayOriginal = ref<string>('')
const displayEnhanced = ref<string>('')
// 上传图片的 MIME 类型
const imageMimeType = ref<string>('image/jpeg')

/**
 * 选择文件前的校验：限制 3MB
 */
const beforeUpload = (file: File) => {
  if (file.size > 3 * 1024 * 1024) {
    alert('图片大小不能超过 3MB！')
    return false
  }
  const reader = new FileReader()
  reader.onload = (e) => {
    const fullBase64 = e.target?.result as string
    displayOriginal.value = fullBase64
    // 提取 MIME 类型，用于后续拼接前缀
    const match = fullBase64.match(/data:(image\/[\w+]+);base64,/)
    imageMimeType.value = match ? match[1] : 'image/jpeg'
    // 剥离前缀，只保留纯 Base64 数据
    rawBase64.value = fullBase64.split(',')[1]
  }
  reader.readAsDataURL(file)
  return false
}

/**
 * 调用后端 AI 清晰接口
 */
const doEnhance = async () => {
  if (!rawBase64.value) return
  loading.value = true
  displayEnhanced.value = ''
  try {
    const res = await myAxios.post(
      '/api/picture/enhance',
      { image: rawBase64.value },
      { timeout: 180000 }
    )
    if (res.data.success && res.data.enhancedImage) {
      message.success('AI 清晰处理成功')
      // 后端返回纯 Base64，拼接前缀用于展示
      displayEnhanced.value = `data:${imageMimeType.value};base64,${res.data.enhancedImage}`
    } else {
      message.error('AI 清晰失败：' + (res.data.error || '未知错误'))
    }
  } catch (error: any) {
    message.error('AI 清晰失败：' + (error.message || '请求超时'))
  } finally {
    loading.value = false
  }
}

/**
 * 将增强结果上传为图片
 */
const handleUpload = async () => {
  if (!displayEnhanced.value) return
  uploadLoading.value = true
  try {
    // Base64 转 File
    const byteString = atob(displayEnhanced.value.split(',')[1])
    const ab = new ArrayBuffer(byteString.length)
    const ia = new Uint8Array(ab)
    for (let i = 0; i < byteString.length; i++) {
      ia[i] = byteString.charCodeAt(i)
    }
    const blob = new Blob([ab], { type: imageMimeType.value })
    const file = new File([blob], `enhanced_${Date.now()}.png`, { type: imageMimeType.value })
    // 调用自动生成的上传接口
    const params: API.PictureUploadRequest = props.picture ? { id: props.picture.id } : {}
    params.spaceId = props.spaceId
    const res = await uploadPictureUsingPost(params, {}, file)
    if (res.data.code === 0 && res.data.data) {
      message.success('图片上传成功')
      props.onSuccess?.(res.data.data)
      closeModal()
    } else {
      message.error('图片上传失败：' + (res.data.message || '未知错误'))
    }
  } catch (error: any) {
    message.error('图片上传失败：' + (error.message || '未知错误'))
  } finally {
    uploadLoading.value = false
  }
}

/**
 * 重置文件选择
 */
const resetFile = () => {
  rawBase64.value = ''
  displayOriginal.value = ''
  displayEnhanced.value = ''
}

/**
 * 打开弹窗
 */
const openModal = () => {
  visible.value = true
}

/**
 * 关闭弹窗并重置状态
 */
const closeModal = () => {
  visible.value = false
  resetFile()
}

defineExpose({
  openModal,
})
</script>

<style>
.image-enhance {
  text-align: center;
}
</style>
