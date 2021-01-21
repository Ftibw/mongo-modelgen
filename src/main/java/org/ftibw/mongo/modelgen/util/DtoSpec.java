package org.ftibw.mongo.modelgen.util;

import org.ftibw.mongo.modelgen.model.MetaEntity;
import org.ftibw.mongo.modelgen.publics.MappedSuperclass;
import org.ftibw.mongo.modelgen.publics.dto.Prop;
import org.ftibw.mongo.modelgen.publics.dto.Spec;
import org.ftibw.mongo.modelgen.publics.dto.Specs;
import org.ftibw.mongo.modelgen.publics.dto.Type;

import javax.lang.model.element.Element;
import javax.lang.model.type.MirroredTypesException;
import javax.lang.model.type.TypeMirror;
import java.util.*;

/**
 * @author : Ftibw
 * @date : 2020/1/7 14:32
 */
public class DtoSpec {
    public static final Map<String, List<DtoSpec>> DTO_SPECIFICATIONS = new HashMap<>();

    private String namespace;
    private String descr;
    private Type type;
    private boolean defaultEqualsAndHashCode;
    private Map<String, DtoProp> propertyMap = new HashMap<>();
    private List<DtoProp> extraProperties;

    public DtoSpec(String descr) {
        this.descr = descr;
    }

    public DtoSpec(String namespace, String descr, Type type) {
        this.namespace = namespace;
        this.descr = descr;
        this.type = type;
        this.defaultEqualsAndHashCode = true;
    }

    public String getNamespace() {
        return namespace;
    }

    public String getDescr() {
        return descr;
    }

    public Type getType() {
        return type;
    }

    public Map<String, DtoProp> getPropertyMap() {
        return propertyMap;
    }

    public List<DtoProp> getExtraProperties() {
        return extraProperties;
    }

    public static List<DtoSpec> getDtoSpecification(String qualifiedName) {
        return DTO_SPECIFICATIONS.get(qualifiedName);
    }

    public boolean isOverrideEqualsAndHashCode() {
        return !defaultEqualsAndHashCode;
    }

    /**
     * 使用{@link Specs#value()[0]} 作为默认规范
     */
    private static Spec getDefaultSpec(Specs dtoSpecs) {
        Spec[] specs = dtoSpecs.value();
        if (specs.length == 0) {
            return null;
        }
        return specs[0];
    }

    private static Map<String, DtoProp> getSuperDefaultDtoProps(Element superElement) {
        Map<String, DtoProp> propertyMap = Collections.emptyMap();

        if (superElement.getAnnotation(MappedSuperclass.class) != null) {

            Specs specs = superElement.getAnnotation(Specs.class);
            if (specs != null) {
                Spec defaultSpec = getDefaultSpec(specs);
                if (defaultSpec == null) {
                    return propertyMap;
                }
                Prop[] props = defaultSpec.value();
                propertyMap = new HashMap<>(props.length);

                for (Prop prop : props) {
                    String propName = prop.value();
                    String descr = prop.descr();
                    propertyMap.put(propName, new DtoProp(propName, descr));
                }
            }
        }
        return propertyMap;
    }

    private static DtoSpec buildDefaultDtoSpec(Spec defaultSpec, Element superElement) {
        if (defaultSpec == null) {
            return new DtoSpec("");
        }
        DtoSpec dtoSpec = new DtoSpec(defaultSpec.descr());

        Map<String, DtoProp> propertyMap = dtoSpec.propertyMap;
        //先添加父类属性
        if (superElement != null) {
            propertyMap.putAll(getSuperDefaultDtoProps(superElement));
        }
        //再添加当前类属性
        for (Prop prop : defaultSpec.value()) {
            String propName = prop.value();
            String descr = prop.descr();
            propertyMap.put(propName, new DtoProp(propName, descr));
        }
        return dtoSpec;
    }

    public static void buildDtoSpecifications(MetaEntity entity, Element superElement) {
        Specs specs = entity.getTypeElement().getAnnotation(Specs.class);
        if (specs == null) {
            return;
        }

        Spec defaultSpec = getDefaultSpec(specs);
        DtoSpec defaultDtoSpec = buildDefaultDtoSpec(defaultSpec, superElement);
        Map<String, DtoProp> defaultPropertyMap = defaultDtoSpec.propertyMap;

        List<DtoSpec> dtoSpecs = new ArrayList<>();
        DtoSpec.DTO_SPECIFICATIONS.put(entity.getQualifiedName(), dtoSpecs);

        Set<String> existQualifiedInfo = new HashSet<>();
        for (Spec spec : specs.value()) {
            if (spec == defaultSpec) {
                continue;
            }
            //命名空间 + 模型类型，会用于类名生成，需要去重
            String namespace = spec.namespace();
            Type dtoType = spec.type();
            if (existQualifiedInfo.contains(namespace + dtoType)) {
                continue;
            }
            existQualifiedInfo.add(namespace + dtoType);

            //获取dto模型描述
            String modelDescr = spec.descr();
            //dto模型描述为空，使用默认描述
            if (StringUtil.isBlank(modelDescr)) {
                modelDescr = defaultDtoSpec.getDescr();
            }

            DtoSpec dtoSpec = new DtoSpec(namespace, modelDescr, dtoType);
            dtoSpecs.add(dtoSpec);

            Map<String, DtoProp> propertyMap = dtoSpec.propertyMap;
            // DtoProp#descr非空，则覆盖已存在的属性描述
            for (Prop prop : spec.value()) {
                String propName = prop.value();
                String propDescr = prop.descr();

                if (StringUtil.isBlank(propDescr)) {
                    DtoProp defaultProp = defaultPropertyMap.get(propName);
                    if (defaultProp != null) {
                        propDescr = defaultProp.getDescr();
                    }
                }
                boolean overrideEqualsAndHashCode = prop.hash();
                dtoSpec.defaultEqualsAndHashCode &= !overrideEqualsAndHashCode;
                //只获取非默认属性的【校验规则】、【是否参与hashCode计算】
                propertyMap.put(propName, new DtoProp(propName, propDescr, prop.rule(), overrideEqualsAndHashCode));
            }

            Prop[] extraProps = spec.extra();
            List<DtoProp> extraProperties = new ArrayList<>(extraProps.length);
            Set<String> extraPropNames = new HashSet<>(extraProps.length);
            for (Prop extraProp : extraProps) {
                String propName = extraProp.value();
                if (propertyMap.containsKey(propName)) {
                    continue;
                }
                if (extraPropNames.contains(propName)) {
                    continue;
                }
                extraPropNames.add(propName);

                String typeDeclare = extraProp.typeDeclare();
                List<String> typeImports = new ArrayList<>();
                try {
                    //编译阶段无法调用反射获取注解值类型为Class的成员
                    extraProp.type();
                } catch (MirroredTypesException e) {
                    for (TypeMirror typeMirror : e.getTypeMirrors()) {
                        typeImports.add(typeMirror.toString());
                    }
                }
                //导入类型跳过
                if (typeImports.isEmpty()) {
                    continue;
                }
                //导入复杂类型，却没有类型声明述跳过
                if (typeImports.size() > 1 && StringUtil.isBlank(typeDeclare)) {
                    continue;
                }
                boolean overrideEqualsAndHashCode = extraProp.hash();
                dtoSpec.defaultEqualsAndHashCode &= !overrideEqualsAndHashCode;
                extraProperties.add(new DtoProp(
                        propName,
                        extraProp.descr(),
                        typeDeclare,
                        typeImports,
                        extraProp.rule(),
                        overrideEqualsAndHashCode
                ));
            }
            if (extraProperties.size() > 0) {
                dtoSpec.extraProperties = extraProperties;
            }
        }
    }
}
