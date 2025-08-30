package com.yupi.yupicturebackend.model.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

/**
 * 空间类型枚举类
 */
@Getter
public enum SpaceTypeEnum {

    //带参数的枚举常量定义
    //带参数的枚举常量的定义语法：枚举常量名（参数1，参数2）
    PRIVATE("私有空间", 0),
    TEAM("团队空间", 1);

    //常用来存储对象的核心属性，且一旦对象创建（通过构造方法赋值后），这些属性就 “定型” 不能再改
    //text是枚举的文本描述，value是枚举的值
    private final String text;

    private final int value;

    SpaceTypeEnum(String text, int value) {
        this.text = text;
        this.value = value;
    }

    /**
     * 根据 value 获取枚举到对应的枚举对象，包含其所有参数
     */
    public static SpaceTypeEnum getEnumByValue(Integer value) {
        if (ObjUtil.isEmpty(value)) {
            return null;
        }
        for (SpaceTypeEnum spaceTypeEnum : SpaceTypeEnum.values()) {
            if (spaceTypeEnum.value == value) {
                return spaceTypeEnum;
            }
        }
        return null;
    }
}