package org.ftibw.mongo.modelgen.publics.dto;

import java.lang.annotation.*;

/**
 * dto规范描述注解
 *
 * @author : Ftibw
 * @date : 2021/1/7 13:41
 */
@Documented
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.SOURCE)
@Repeatable(Specs.class)
public @interface Spec {

    /**
     * 模型属性描述
     */
    Prop[] value() default {};

    /**
     * 实体中没有的扩展属性
     */
    Prop[] extra() default {};

    /**
     * 模型描述，空值时使用默认描述
     */
    String descr() default "";

    /**
     * namespace参与生成模型类的完全限定名，
     * 结构为："[前缀包名.]简单类名前缀"，
     * "dto[.前缀包名]" 用于替换掉entity包名，"简单类名前缀" 会自动首字母大写，例如：
     * <p>
     * entity: org.ftibw.entity.User （实体全限定名必须匹配**.entity.**）
     * namespace: backend.add
     * type: DTO
     * dto: org.ftibw.dto.backend.AddUserDTO
     * entity -> dto.backend, User -> AddUserDTO
     * <p>
     * 命名空间和类型会用于类名生成，对于同一个实体的不同dto，namespace + type 需要唯一
     */
    String namespace() default "";

    /**
     * 模型类型，决定了生成的类名后缀、与其他模型转换的方法
     */
    Type type() default Type.DTO;

}
