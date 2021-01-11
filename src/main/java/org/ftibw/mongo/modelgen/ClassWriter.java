/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.ftibw.mongo.modelgen;

import org.ftibw.mongo.modelgen.model.ImportContext;
import org.ftibw.mongo.modelgen.model.MetaAttribute;
import org.ftibw.mongo.modelgen.model.MetaCollection;
import org.ftibw.mongo.modelgen.model.MetaEntity;
import org.ftibw.mongo.modelgen.publics.dto.Rule;
import org.ftibw.mongo.modelgen.publics.dto.Rule_;
import org.ftibw.mongo.modelgen.publics.dto.Type;
import org.ftibw.mongo.modelgen.util.*;

import javax.annotation.processing.Filer;
import javax.annotation.processing.FilerException;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Helper class to write the actual meta model class using the  {@link javax.annotation.processing.Filer} API.
 *
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 */
public final class ClassWriter {
    private static final String META_MODEL_CLASS_NAME_SUFFIX = "_";
    private static final ThreadLocal<SimpleDateFormat> SIMPLE_DATE_FORMAT = ThreadLocal.withInitial(() ->
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ"));

    private ClassWriter() {
    }

    public static void writeFile(MetaEntity entity, Context context) {
        writeFileMetaModel(entity, context);
        writeFileDtoModel(entity, context);
    }

    public static void writeFileMetaModel(MetaEntity entity, Context context) {
        try {
            String modelPackage = entity.getPackageName();
            String metaPackage = toMetaPackage(modelPackage);
            // need to generate the body first, since this will also update the required imports which need to
            // be written out first
            String body = generateBody(entity, context).toString();

            FileObject fo = context.getProcessingEnvironment().getFiler().createSourceFile(
                    getFullyQualifiedClassName(entity, metaPackage)
            );
            OutputStream os = fo.openOutputStream();
            PrintWriter pw = new PrintWriter(os);

            pw.println("package " + metaPackage + ";");
            pw.println();

            ImportContext importContext = new ImportContextImpl(metaPackage);
            for (String typeImport : entity.getImports()) {
                importContext.importType(typeImport);
            }
            pw.println(importContext.generateImports());
            pw.println(body);

            pw.flush();
            pw.close();
        } catch (FilerException filerEx) {
            context.logMessage(
                    Diagnostic.Kind.ERROR, "Problem with Filer: " + filerEx.getMessage()
            );
        } catch (IOException ioEx) {
            context.logMessage(
                    Diagnostic.Kind.ERROR,
                    "Problem opening file to write MetaModel for " + entity.getSimpleName() + ioEx.getMessage()
            );
        }
    }

    public static void writeFileDtoModel(MetaEntity entity, Context context) {
        List<DtoSpec> dtoSpecs = DtoSpec.getDtoSpecification(entity.getQualifiedName());
        if (dtoSpecs == null) {
            return;
        }
        try {
            String modelPackage = entity.getPackageName();

            Filer filer = context.getProcessingEnvironment().getFiler();
            for (DtoSpec dtoSpec : dtoSpecs) {
                String dtoPackage = getDtoPackage(dtoSpec, modelPackage);

                // need to generate the body first, since this will also update the required imports which need to
                // be written out first
                String body = generateBodyDto(entity, context, dtoSpec).toString();

                FileObject fo = filer.createSourceFile(
                        getFullyQualifiedClassNameDto(entity, dtoPackage, dtoSpec)
                );
                OutputStream os = fo.openOutputStream();
                PrintWriter pw = new PrintWriter(os);

                pw.println("package " + dtoPackage + ";");
                pw.println();

                ImportContext importContext = new ImportContextImpl(dtoPackage);
                for (String typeImport : entity.getImports()) {
                    importContext.importType(typeImport);
                }
                pw.println(importContext.generateImports());
                pw.println(body);

                pw.flush();
                pw.close();
            }
        } catch (FilerException filerEx) {
            context.logMessage(
                    Diagnostic.Kind.ERROR, "Problem with Filer: " + filerEx.getMessage()
            );
        } catch (IOException ioEx) {
            context.logMessage(
                    Diagnostic.Kind.ERROR,
                    "Problem opening file to write DtoModel for " + entity.getSimpleName() + ioEx.getMessage()
            );
        }
    }

    /**
     * Generate everything after import statements.
     *
     * @param entity  The meta entity for which to write the body
     * @param context The processing context
     * @return body content
     */
    private static StringBuffer generateBody(MetaEntity entity, Context context) {
        StringWriter sw = new StringWriter();
        try (PrintWriter pw = new PrintWriter(sw)) {

            if (context.addGeneratedAnnotation()) {
                pw.println(writeGeneratedAnnotation(entity, context));
            }

            printClassDeclaration(entity, pw, context);

            List<MetaAttribute> members = entity.getMembers();
            for (MetaAttribute metaMember : members) {
                pw.println("	" + metaMember.getAttributeNameDeclarationString());
            }
            pw.println();
            pw.println("}");
            return sw.getBuffer();
        }
    }

    private static StringBuffer generateBodyDto(MetaEntity entity, Context context, DtoSpec dtoSpec) {
        StringWriter sw = new StringWriter();
        try (PrintWriter pw = new PrintWriter(sw)) {

            if (context.addGeneratedAnnotation()) {
                pw.println(writeGeneratedAnnotation(entity, context));
            }

            pw.println(writeApiModelAnnotation(entity, dtoSpec));

            pw.println(writeGetterSetterAnnotation(entity));

            printClassDeclarationDto(entity, pw, dtoSpec);

            Map<String, DtoProp> propertyMap = dtoSpec.getPropertyMap();

            List<String> printedEntityPropNames = new ArrayList<>();

            List<MetaAttribute> superMembers = getSuperClassMembersForMerge(entity, context);
            for (MetaAttribute metaMember : superMembers) {

                String propertyName = metaMember.getPropertyName();
                if (propertyMap.containsKey(propertyName)) {

                    if ("id".equals(propertyName)) {
                        printConstraintAnnotation(entity, propertyName, dtoSpec, pw);
                        pw.println(writeApiModelPropertyAnnotation(entity, propertyName, dtoSpec));
                        pw.println("	" + metaMember.getAttributeDeclarationString());
                        printedEntityPropNames.add(propertyName);
                        break;
                    }
                }
            }

            List<MetaAttribute> members = entity.getMembers();
            for (MetaAttribute metaMember : members) {
                String propertyName = metaMember.getPropertyName();
                if (!propertyMap.containsKey(propertyName)) {
                    continue;
                }
                printConstraintAnnotation(entity, propertyName, dtoSpec, pw);
                pw.println(writeApiModelPropertyAnnotation(entity, propertyName, dtoSpec));
                pw.println("	" + metaMember.getAttributeDeclarationString());
                printedEntityPropNames.add(propertyName);
            }

            for (MetaAttribute metaMember : superMembers) {

                String propertyName = metaMember.getPropertyName();
                if (propertyMap.containsKey(propertyName)) {

                    if ("id".equals(propertyName)) {
                        continue;
                    }
                    printConstraintAnnotation(entity, propertyName, dtoSpec, pw);
                    pw.println(writeApiModelPropertyAnnotation(entity, propertyName, dtoSpec));
                    pw.println("	" + metaMember.getAttributeDeclarationString());
                    printedEntityPropNames.add(propertyName);
                }
            }

            printDtoExtraProperties(entity, dtoSpec, pw);

            pw.println();

            printDtoConvertMethod(entity, dtoSpec, printedEntityPropNames, pw);

            pw.println("}");
            return sw.getBuffer();
        }
    }

    private static void printDtoExtraProperties(MetaEntity entity, DtoSpec dtoSpec, PrintWriter pw) {
        List<DtoProp> extraProperties = dtoSpec.getExtraProperties();
        if (extraProperties == null || extraProperties.isEmpty()) {
            return;
        }
        pw.println("	//extra properties");
        for (DtoProp extra : extraProperties) {
            List<String> typeImports = extra.getTypeImports();
            if (typeImports.isEmpty()) {
                continue;
            }
            String propName = extra.getPropName();

            //额外属性不在初始化的propertyMap中, 需要构造一个临时的存放额外属性
            DtoSpec tmpDtoSpec = new DtoSpec("");
            tmpDtoSpec.getPropertyMap().put(propName, extra);

            printConstraintAnnotation(entity, propName, tmpDtoSpec, pw);
            pw.println(writeApiModelPropertyAnnotation(entity, propName, tmpDtoSpec));

            String typeDeclare = extra.getTypeDeclare();
            if (StringUtil.isBlank(typeDeclare)) {
                typeDeclare = entity.importType(typeImports.get(0));
            } else {
                for (String typeImport : typeImports) {
                    entity.importType(typeImport);
                }
            }
            pw.println("	private " + typeDeclare + " " + propName + ";");
        }
    }

    private static void printDtoConvertMethod(
            MetaEntity entity,
            DtoSpec dtoSpec,
            List<String> printedEntityPropNames,
            PrintWriter pw
    ) {
        Type type = dtoSpec.getType();
        //DTO, VO 与实体互相转换, 需要额外导入实体类
        if (type == Type.DTO) {
            String entityClassName = entity.importType(entity.getQualifiedName());
            pw.println("	public " + entityClassName + " toDO() {");
            pw.println("		" + entityClassName + " one = new " + entityClassName + "();");
            for (String propName : printedEntityPropNames) {
                pw.println("		one.set" + StringUtil.firstUpperCase(propName) + "(" + propName + ");");
            }
            pw.println("		return one;");
            pw.println("	}");
            pw.println();

        } else if (type == Type.VO) {
            String entityClassName = entity.importType(entity.getQualifiedName());
            String dtoClassName = getDtoClassSimpleName(entity, dtoSpec);
            pw.println("	public static " + dtoClassName + " toVO(" + entityClassName + " po) {");
            pw.println("		" + dtoClassName + " one = new " + dtoClassName + "();");
            for (String propName : printedEntityPropNames) {
                pw.println("		one." + propName + " = po.get" + StringUtil.firstUpperCase(propName) + "();");
            }
            pw.println("		return one;");
            pw.println("	}");
            pw.println();

            pw.println("	public static String[] projects() {");
            pw.print("		return new String[]{");
            int size = printedEntityPropNames.size();
            for (int i = 0; i < size; i++) {
                pw.print("\"" + printedEntityPropNames.get(i) + "\"");
                if (i < size - 1) {
                    pw.print(", ");
                }
            }
            pw.println("};");
            pw.println("	}");
            pw.println();
        }
//        else if (type == DtoType.Query) {
//        }
    }

    private static List<MetaAttribute> getSuperClassMembersForMerge(MetaEntity entity, Context context) {
        List<MetaAttribute> members = Collections.emptyList();
        String superClassName = findMappedSuperClass(entity, context);
        if (superClassName != null) {
            MetaEntity superEntity = context.getMetaEntity(superClassName);
            if (superEntity != null) {
                members = superEntity.getMembers();
                for (MetaAttribute member : members) {
                    //将superClass中成员类型导入到dto中
                    importSuperMemberType(entity, member);
                }
            }
        }
        return members;
    }

    //导入属性的（集合泛型）类型
    private static void importSuperMemberType(MetaEntity entity, MetaAttribute member) {
        entity.importType(member.getTypeDeclaration());
        if (member instanceof MetaCollection) {
            entity.importType(member.getMetaType());
        }
    }

    private static void printClassDeclaration(MetaEntity entity, PrintWriter pw, Context context) {
        pw.print("public abstract class " + entity.getSimpleName() + META_MODEL_CLASS_NAME_SUFFIX);
        String superClassName = findMappedSuperClass(entity, context);
        if (superClassName != null) {
            pw.print(" extends " + entity.importType(toMetaPackage(superClassName) + META_MODEL_CLASS_NAME_SUFFIX));
        }
        pw.println(" {");
        pw.println();
    }

    private static void printClassDeclarationDto(MetaEntity entity, PrintWriter pw, DtoSpec dtoSpec) {
        pw.print("public class " + getDtoClassSimpleName(entity, dtoSpec));
        pw.println(" {");
        pw.println();
    }

    public static Element findMappedSuperElement(MetaEntity entity, Context context) {
        TypeMirror superClass = entity.getTypeElement().getSuperclass();
        //superclass of Object is of NoType which returns some other kind
        while (superClass.getKind() == TypeKind.DECLARED) {
            //F..king Ch...t Have those people used their horrible APIs even once?
            final Element superClassElement = ((DeclaredType) superClass).asElement();
            if (extendsSuperMetaModel(superClassElement, entity.isMetaComplete(), context)) {
                return superClassElement;
            }
            superClass = ((TypeElement) superClassElement).getSuperclass();
        }
        return null;
    }

    private static String findMappedSuperClass(MetaEntity entity, Context context) {
        TypeMirror superClass = entity.getTypeElement().getSuperclass();
        //superclass of Object is of NoType which returns some other kind
        while (superClass.getKind() == TypeKind.DECLARED) {
            //F..king Ch...t Have those people used their horrible APIs even once?
            final Element superClassElement = ((DeclaredType) superClass).asElement();
            String superClassName = ((TypeElement) superClassElement).getQualifiedName().toString();
            if (extendsSuperMetaModel(superClassElement, entity.isMetaComplete(), context)) {
                return superClassName;
            }
            superClass = ((TypeElement) superClassElement).getSuperclass();
        }
        return null;
    }

    /**
     * Checks whether this metamodel class needs to extend another metamodel class.
     * This methods checks whether the processor has generated a metamodel class for the super class, but it also
     * allows for the possibility that the metamodel class was generated in a previous compilation (eg it could be
     * part of a separate jar. See also METAGEN-35).
     *
     * @param superClassElement  the super class element
     * @param entityMetaComplete flag indicating if the entity for which the metamodel should be generarted is metamodel
     *                           complete. If so we cannot use reflection to decide whether we have to add the extend clause
     * @param context            the execution context
     * @return {@code true} in case there is super class meta model to extend from {@code false} otherwise.
     */
    private static boolean extendsSuperMetaModel(Element superClassElement, boolean entityMetaComplete, Context context) {
        // if we processed the superclass in the same run we definitely need to extend
        String superClassName = ((TypeElement) superClassElement).getQualifiedName().toString();
        if (context.containsMetaEntity(superClassName)
                || context.containsMetaEmbeddable(superClassName)) {
            return true;
        }

        // to allow for the case that the metamodel class for the super entity is for example contained in another
        // jar file we use reflection. However, we need to consider the fact that there is xml configuration
        // and annotations should be ignored
        return !entityMetaComplete && (TypeUtils.containsAnnotation(superClassElement, Constants.ENTITY)
                || TypeUtils.containsAnnotation(superClassElement, Constants.MAPPED_SUPERCLASS));
    }

    private static String getFullyQualifiedClassName(MetaEntity entity, String modelPackage) {
        String fullyQualifiedClassName = "";
        if (!modelPackage.isEmpty()) {
            fullyQualifiedClassName = modelPackage + ".";
        }
        fullyQualifiedClassName = fullyQualifiedClassName + entity.getSimpleName();
        fullyQualifiedClassName += META_MODEL_CLASS_NAME_SUFFIX;
        return fullyQualifiedClassName;
    }

    private static String getFullyQualifiedClassNameDto(MetaEntity entity, String dtoPackage, DtoSpec dtoSpec) {
        return dtoPackage + "." + getDtoClassSimpleName(entity, dtoSpec);
    }

    //region meta/dto package & name
    private static String toMetaPackage(String modelPackage) {
        if (modelPackage.endsWith(".entity")) {
            return modelPackage.replace(".entity", ".meta");
        } else if (modelPackage.contains(".entity.")) {
            return modelPackage.replace(".entity.", ".meta.");
        } else {
            throw new RuntimeException("entity package not found");
        }
    }

    private static String getDtoPackage(DtoSpec dtoSpec, String modelPackage) {
        String packagesAfterEntityPackage;
        String typePackage = dtoSpec.getType().name().toLowerCase();

        String namespace = dtoSpec.getNamespace();
        int nsLastPackageSeparator = namespace.lastIndexOf(".");
        if (nsLastPackageSeparator > -1) {
            packagesAfterEntityPackage = namespace.substring(0, nsLastPackageSeparator).toLowerCase() + "." + typePackage;
        } else {
            packagesAfterEntityPackage = typePackage;
        }

        if (modelPackage.endsWith(".entity")) {
            return modelPackage.replace(".entity", "." + packagesAfterEntityPackage);
        } else if (modelPackage.contains(".entity.")) {
            return modelPackage.replace(".entity.", "." + packagesAfterEntityPackage + ".");
        } else {
            throw new RuntimeException("entity package not found");
        }
    }

    private static String getDtoClassSimpleName(MetaEntity entity, DtoSpec dtoSpec) {
        return getDtoNamePrefix(dtoSpec) + entity.getSimpleName() + dtoSpec.getType();
    }

    private static String getDtoNamePrefix(DtoSpec dtoSpec) {
        String namePrefix = dtoSpec.getNamespace();
        int lastPackageSeparator = namePrefix.lastIndexOf(".");
        if (lastPackageSeparator > -1) {
            namePrefix = namePrefix.substring(lastPackageSeparator + 1);
        }
        return StringUtil.firstUpperCase(namePrefix);
    }
    //endregion

    private static String writeGeneratedAnnotation(MetaEntity entity, Context context) {
        StringBuilder generatedAnnotation = new StringBuilder();
        generatedAnnotation.append("@")
                .append(entity.importType("javax.annotation.Generated"))
                .append("(value = \"")
                .append(MongoModelEntityProcessor.class.getName());
        if (context.addGeneratedDate()) {
            generatedAnnotation.append("\", date = \"")
                    .append(SIMPLE_DATE_FORMAT.get().format(new Date()))
                    .append("\")");
        } else {
            generatedAnnotation.append("\")");
        }
        return generatedAnnotation.toString();
    }

//    private static String writeStaticMetaModelAnnotation(MetaEntity entity) {
//        return "@" + entity.importType("javax.persistence.metamodel.StaticMetamodel") + "(" + entity.getSimpleName() + ".class)";
//    }

    private static String writeGetterSetterAnnotation(MetaEntity entity) {
        return "@" + entity.importType("lombok.Getter") + "\n@" + entity.importType("lombok.Setter");
    }

    private static String writeApiModelAnnotation(MetaEntity entity, DtoSpec dtoSpec) {
        String descr = dtoSpec.getDescr();
        if (StringUtil.isBlank(descr)) {
            descr = entity.getSimpleName();
        }
        return "@" + entity.importType("io.swagger.annotations.ApiModel")
                + "(\"" + descr + "\")";
    }

    private static String writeApiModelPropertyAnnotation(MetaEntity entity, String propertyName, DtoSpec dtoSpec) {
        Map<String, DtoProp> propertyMap = dtoSpec.getPropertyMap();
        DtoProp property = propertyMap.get(propertyName);
        if (property == null) {
            throw new RuntimeException("property is null when writeApiModelPropertyAnnotation");
        }
        String descr = property.getDescr();
        if (StringUtil.isBlank(descr)) {
            descr = propertyName;
        }
        return "	@" + entity.importType("io.swagger.annotations.ApiModelProperty")
                + "(\"" + descr + "\")";
    }

    private static void printConstraintAnnotation(MetaEntity entity, String propertyName, DtoSpec dtoSpec, PrintWriter pw) {
        Map<String, DtoProp> propertyMap = dtoSpec.getPropertyMap();
        DtoProp property = propertyMap.get(propertyName);
        if (property == null) {
            throw new RuntimeException("property is null when printConstraintAnnotation");
        }
        Set<Rule_> ruleSet = new HashSet<>();
        for (Rule rule : property.getRules()) {
            //注解去重
            Rule_ ruleEnum = rule.value();
            if (ruleSet.contains(ruleEnum)) {
                continue;
            }
            ruleSet.add(ruleEnum);

            Rule_ anno = rule.value();
            entity.importType(anno.getType());

            pw.println("	" + anno.getConstraintAnnotationDeclareString(rule));
        }
    }

}
