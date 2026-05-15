package com.yupi.yupicturebackend.manager;

import cn.hutool.core.io.FileUtil;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.COSObject;
import com.qcloud.cos.model.GetObjectRequest;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.PicOperations;
import com.yupi.yupicturebackend.config.CosClientConfig;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

//该类提供通用的对象存储操作，比如文件上传、文件下载等
@Component
public class CosManager {

    //引入cos的相关配置
    @Resource
    private CosClientConfig cosClientConfig;

    @Resource
    private COSClient cosClient;

    /**
     * 上传对象
     *
     * @param key  唯一键：表示要将文件存储到cos对象存储的哪个位置
     * @param file 文件：本地文件对象
     */
    public PutObjectResult putObject(String key, File file) {
        //new一个存放文件的请求
        PutObjectRequest putObjectRequest = new PutObjectRequest(cosClientConfig.getBucket(), key,
                file);
        //调用cosClient的方法，传入请求，就可以上传对象了
        return cosClient.putObject(putObjectRequest);
    }

    /**
     * 下载对象
     *
     * @param key 唯一键
     */
    public COSObject getObject(String key) {
        GetObjectRequest getObjectRequest = new GetObjectRequest(cosClientConfig.getBucket(), key);
        return cosClient.getObject(getObjectRequest);
    }

    /**
     * 这是自定义的上传对象的方法
     * 上传对象（附带图片信息），在上传的基础上加了解析图片的方法
     *
     * @param key  唯一键
     * @param file 文件
     */
    //PutObjectResult：它用来存储图片上传到云存储后的处理结果信息。为后续对上传对象的验证、管理等操作提供了数据支持
    public PutObjectResult putPictureObject(String key, File file) {
        //new一个putObjectRequest对象，用于封装上传对象的请求信息。key：传入方法的唯一键，用于标识要上传的对象在存储桶中的位置。file：传入方法的文件对象，表示要上传的文件。
        PutObjectRequest putObjectRequest = new PutObjectRequest(cosClientConfig.getBucket(), key, file);
        // 对图片进行处理（获取基本信息也被视作为一种图片的处理）。PicOperations：数据万象中的类，封装图片处理的规则集合。
        PicOperations picOperations = new PicOperations();
        // 配置图片处理操作，使其在处理图片时返回原图的相关信息（获取图片元数据）
        picOperations.setIsPicInfo(1);
        // 图片处理规则列表
        List<PicOperations.Rule> rules = new ArrayList<>();
        // 1. 图片压缩（转成 webp 格式）
        String webpKey = FileUtil.mainName(key) + ".webp";
        PicOperations.Rule compressRule = new PicOperations.Rule();
        compressRule.setFileId(webpKey);
        compressRule.setBucket(cosClientConfig.getBucket());
        compressRule.setRule("imageMogr2/format/webp");
        rules.add(compressRule);
        // 2. 缩略图处理，仅对 > 20 KB 的图片生成缩略图
        if (file.length() > 2 * 1024) {
            PicOperations.Rule thumbnailRule = new PicOperations.Rule();
            // 拼接缩略图的路径
            String thumbnailKey = FileUtil.mainName(key) + "_thumbnail." + FileUtil.getSuffix(key);
            thumbnailRule.setFileId(thumbnailKey);
            thumbnailRule.setBucket(cosClientConfig.getBucket());
            // 缩放规则 /thumbnail/<Width>x<Height>>（如果大于原图宽高，则不处理）
            thumbnailRule.setRule(String.format("imageMogr2/thumbnail/%sx%s>", 256, 256));
            rules.add(thumbnailRule);
        }
        /**
         * 构造处理参数
         * 将之前创建并配置好的图片处理操作参数（picOperations对象，其中包含了“返回原图信息”等处理规则），设置到上传对象请求（putObjectRequest）中。
         * 这样在执行上传操作时，对象存储服务会按照 picOperations 中定义的规则，对要上传的图片进行处理（比如获取原图的尺寸、格式等信息），让上传过程与图片处理逻辑结合起来。
         */
        picOperations.setRules(rules);
        putObjectRequest.setPicOperations(picOperations);
        //将请求对象 putObjectRequest 发送给腾讯云 COS 服务，从而执行图片上传及附带的图片处理操作。
        return cosClient.putObject(putObjectRequest);
    }

    /**
     * 删除对象
     *
     * @param key 唯一键
     */
    public void deleteObject(String key) {
        cosClient.deleteObject(cosClientConfig.getBucket(), key);
    }
}
