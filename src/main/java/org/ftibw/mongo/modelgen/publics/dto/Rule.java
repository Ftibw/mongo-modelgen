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
     * 属性名称
     */
    Rule_ value();

    /**
     * 用于生成支持的注解选项值，选项值与{@link Rule_#supportedOptions}顺序匹配。
     */
    String[] optVal() default {};

    /**
     * 用于生成 message 注解选项
     */
    String msg() default "";

}
