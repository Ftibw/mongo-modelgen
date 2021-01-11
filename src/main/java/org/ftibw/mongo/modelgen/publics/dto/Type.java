package org.ftibw.mongo.modelgen.publics.dto;


/**
 * DTO类型
 * <p>
 * 只要是在【展示层和应用层】之间进行数据交互的，都是 DTO
 * DTO与客户端一对一的情况下，DTO = VO（视图模型）；DTO与客户端一对多的情况下，DTO!=VO
 *
 * @author : Ftibw
 * @date : 2021/1/9 14:13
 */
public enum Type {
    /**
     * data transfer object 传输数据的封装，通常用于DO的写操作
     */
    DTO,
    /**
     * query object，查询参数的封装
     */
    QO,
    /**
     * view object，响应给客户端的视图模型
     */
    VO;
}
