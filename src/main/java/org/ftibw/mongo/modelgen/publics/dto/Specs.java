package org.ftibw.mongo.modelgen.publics.dto;

import java.lang.annotation.*;

/**
 * dto规范描述注解容器
 *
 * @author : Ftibw
 * @date : 2021/1/7 13:41
 */
@Documented
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.SOURCE)
public @interface Specs {
    /**
     * 同一个实体的不同DTO规范，第一个规范作为默认值不生成文件，其他规范生成对应DTO文件
     */
    Spec[] value() default {};

}
