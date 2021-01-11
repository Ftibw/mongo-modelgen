package org.ftibw.mongo.modelgen.publics;

import java.lang.annotation.*;

/**
 * 用于标识是实体父类
 *
 * @author : Ftibw
 * @date : 2021/1/6 11:21
 */
@Documented
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.SOURCE)
public @interface MappedSuperclass {
}
