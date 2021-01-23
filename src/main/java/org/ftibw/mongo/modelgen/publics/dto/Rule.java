package org.ftibw.mongo.modelgen.publics.dto;

import java.lang.annotation.*;

/**
 * dto属性约束描述注解
 *
 * @author : Ftibw
 * @date : 2021/1/7 13:41
 */
@Documented
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.SOURCE)
public @interface Rule {
    /**
     * [
     * 约束注解名称,
     * 校验失败后的提示语,
     * 支持的注解选项值
     * ]
     * 多个选项值逗号拼接且与{@link Rule_#CONSTRAINT_SUPPORTED_OPTIONS}顺序匹配
     */
    String[] value();

}
