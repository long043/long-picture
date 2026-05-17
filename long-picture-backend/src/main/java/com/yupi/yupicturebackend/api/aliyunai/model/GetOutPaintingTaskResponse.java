package com.yupi.yupicturebackend.api.aliyunai.model;

import cn.hutool.core.annotation.Alias;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 查询扩图任务响应类
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GetOutPaintingTaskResponse {

    /**
     * 请求唯一标识
     */
    @Alias("request_id")
    private String requestId;

    /**
     * 接口错误码。
     * <p>接口成功请求不会返回该参数。</p>
     */
    private String code;

    /**
     * 接口错误信息。
     * <p>接口成功请求不会返回该参数。</p>
     */
    private String message;

    /**
     * 输出信息
     */
    private Output output;

    /**
     * 表示任务的输出信息
     */
    @Data
    public static class Output {

        /**
         * 任务 ID
         */
        @Alias("task_id")
        private String taskId;

        /**
         * 任务状态
         * <ul>
         *     <li>PENDING：排队中</li>
         *     <li>RUNNING：处理中</li>
         *     <li>SUCCEEDED：执行成功</li>
         *     <li>FAILED：执行失败</li>
         *     <li>CANCELED：任务已取消</li>
         *     <li>UNKNOWN：任务不存在或状态未知</li>
         * </ul>
         */
        @Alias("task_status")
        private String taskStatus;

        /**
         * 提交时间
         * 格式：YYYY-MM-DD HH:mm:ss.SSS
         */
        @Alias("submit_time")
        private String submitTime;

        /**
         * 调度时间
         * 格式：YYYY-MM-DD HH:mm:ss.SSS
         */
        @Alias("scheduled_time")
        private String scheduledTime;

        /**
         * 结束时间
         * 格式：YYYY-MM-DD HH:mm:ss.SSS
         */
        @Alias("end_time")
        private String endTime;

        /**
         * 输出图像的 URL
         */
        @Alias("output_image_url")
        private String outputImageUrl;

        /**
         * 接口错误码
         * <p>接口成功请求不会返回该参数</p>
         */
        private String code;

        /**
         * 接口错误信息
         * <p>接口成功请求不会返回该参数</p>
         */
        private String message;

        /**
         * 任务指标信息
         */
        @Alias("task_metrics")
        private TaskMetrics taskMetrics;
    }

    /**
     * 表示任务的统计信息
     */
    @Data
    public static class TaskMetrics {

        /**
         * 总任务数
         */
        @Alias("TOTAL")
        private Integer total;

        /**
         * 成功任务数
         */
        @Alias("SUCCEEDED")
        private Integer succeeded;

        /**
         * 失败任务数
         */
        @Alias("FAILED")
        private Integer failed;
    }
}
