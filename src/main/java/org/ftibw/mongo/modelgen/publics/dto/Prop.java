package org.ftibw.mongo.modelgen.publics.dto;

import java.lang.annotation.*;

/**
 * dto属性描述注解
 *
 * @author : Ftibw
 * @date : 2021/1/7 13:41
 */
@Documented
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.SOURCE)
@Repeatable(Spec.class)
public @interface Prop {
    /**
     * 属性名称
     */
    String value();

    /**
     * 属性描述，空值时使用默认描述
     */
    String descr() default "";

    /**
     * 作为扩展属性时，导入的类型，typeDeclare空值时，以type的第一个类型作为声明
     */
    Class<?>[] type() default {};

    /**
     * 作为扩展属性时，类型声明字符串（用于泛型声明）
     */
    String typeDeclare() default "";

    /**
     * 属性校验规则
     */
    Rule[] rule() default {};
}
