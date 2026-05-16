<template>
  <a-modal
    class="image-enhance"
    v-model:visible="visible"
    title="AI 清晰"
    width="min(1200px, calc(100vw - 32px))"
    :footer="false"
    @cancel="closeModal"
  >
    <!-- 图片对比 -->
    <a-row :gutter="[24, 24]" class="image-compare-row">
      <a-col :xs="24" :md="12">
        <h4>原始图片</h4>
        <div class="image-preview-panel">
          <img class="image-preview" :src="picture?.url" :alt="picture?.name" />
        </div>
      </a-col>
      <a-col :xs="24" :md="12">
        <h4>增强结果</h4>
        <div class="image-preview-panel">
          <img v-if="displayEnhanced" class="image-preview" :src="displayEnhanced" :alt="picture?.name" />
          <div v-else class="image-preview-placeholder">等待生成</div>
        </div>
      </a-col>
    </a-row>
    <div style="margin-bottom: 16px" />
    <a-flex justify="center" gap="16">
      <a-button
        type="primary"
        :loading="loading"
        ghost
        @click="doEnhance"
      >
        {{ displayEnhanced ? '重新生成' : '生成图片' }}
      </a-button>
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

type ImageEnhanceResponse = {
  success: boolean
  enhancedImage?: string
  error?: string
}

const props = defineProps<Props>()

const visible = ref(false)
const loading = ref(false)
const uploadLoading = ref(false)
// 带 data URI 前缀的完整 Base64，用于 <img> 显示
const displayEnhanced = ref<string>('')
const imageMimeType = 'image/png'

/**
 * 调用后端 AI 清晰接口
 */
const doEnhance = async () => {
  if (!props.picture?.id) {
    message.warning('请先上传图片')
    return
  }
  loading.value = true
  displayEnhanced.value = ''
  try {
    const res = await myAxios.post<ImageEnhanceResponse>(
      '/api/picture/enhance',
      { pictureId: props.picture.id },
      {
        timeout: 600000,
        headers: {
          'Content-Type': 'application/json',
        },
      }
    )
    if (res.data.success && res.data.enhancedImage) {
      message.success('AI 清晰处理成功')
      // 后端返回纯 Base64，拼接前缀用于展示
      displayEnhanced.value = `data:${imageMimeType};base64,${res.data.enhancedImage}`
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
  if (!props.picture?.id) {
    message.warning('请先上传图片')
    return
  }
  uploadLoading.value = true
  try {
    // Base64 转 File
    const byteString = atob(displayEnhanced.value.split(',')[1])
    const ab = new ArrayBuffer(byteString.length)
    const ia = new Uint8Array(ab)
    for (let i = 0; i < byteString.length; i++) {
      ia[i] = byteString.charCodeAt(i)
    }
    const blob = new Blob([ab], { type: imageMimeType })
    const file = new File([blob], `enhanced_${Date.now()}.png`, { type: imageMimeType })
    // 调用自动生成的上传接口
    const params: API.PictureUploadRequest = { id: props.picture.id }
    params.spaceId = props.spaceId
    const res = await uploadPictureUsingPost(params, {}, file)
    const data = res.data as API.BaseResponsePictureVO_
    if (data.code === 0 && data.data) {
      message.success('图片上传成功')
      props.onSuccess?.(data.data)
      closeModal()
    } else {
      message.error('图片上传失败：' + (data.message || '未知错误'))
    }
  } catch (error: any) {
    message.error('图片上传失败：' + (error.message || '未知错误'))
  } finally {
    uploadLoading.value = false
  }
}

/**
 * 重置生成结果
 */
const resetResult = () => {
  displayEnhanced.value = ''
}

/**
 * 打开弹窗
 */
const openModal = () => {
  if (!props.picture?.id) {
    message.warning('请先上传图片')
    return
  }
  resetResult()
  visible.value = true
}

/**
 * 关闭弹窗并重置状态
 */
const closeModal = () => {
  visible.value = false
  resetResult()
}

defineExpose({
  openModal,
})
</script>

<style>
.image-enhance {
  text-align: center;
}

.image-enhance .ant-modal-body {
  max-height: calc(100vh - 160px);
  overflow-y: auto;
}

.image-enhance .image-compare-row {
  align-items: stretch;
}

.image-enhance h4 {
  margin-bottom: 12px;
}

.image-enhance .image-preview-panel {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 420px;
  height: min(64vh, 640px);
  padding: 12px;
  border: 1px solid #f0f0f0;
  border-radius: 8px;
  background: #fafafa;
}

.image-enhance .image-preview {
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;
}

.image-enhance .image-preview-placeholder {
  color: #999;
}

@media (max-width: 767px) {
  .image-enhance .image-preview-panel {
    min-height: 280px;
    height: 48vh;
  }
}
</style>
