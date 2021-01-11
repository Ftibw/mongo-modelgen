/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.ftibw.mongo.modelgen.annotation;

import org.ftibw.mongo.modelgen.Context;
import org.ftibw.mongo.modelgen.util.Constants;
import org.ftibw.mongo.modelgen.util.StringUtil;
import org.ftibw.mongo.modelgen.util.TypeUtils;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.*;
import javax.lang.model.util.SimpleTypeVisitor6;
import javax.tools.Diagnostic;
import java.util.List;

/**
 * @author Hardy Ferentschik
 */
public class MetaAttributeGenerationVisitor extends SimpleTypeVisitor6<AnnotationMetaAttribute, Element> {

    private final AnnotationMetaEntity entity;
    private final Context context;

    MetaAttributeGenerationVisitor(AnnotationMetaEntity entity, Context context) {
        this.entity = entity;
        this.context = context;
    }

    @Override
    public AnnotationMetaAttribute visitPrimitive(PrimitiveType t, Element element) {
        return new AnnotationMetaSingleAttribute(entity, element, TypeUtils.toTypeString(t));
    }

    @Override
    public AnnotationMetaAttribute visitArray(ArrayType t, Element element) {
        return new AnnotationMetaSingleAttribute(entity, element, TypeUtils.toArrayTypeString(t, context));
    }

    @Override
    public AnnotationMetaAttribute visitTypeVariable(TypeVariable t, Element element) {
        // METAGEN-29 - for a type variable we use the upper bound
        TypeMirror mirror = t.getUpperBound();
        TypeMirror erasedType = context.getTypeUtils().erasure(mirror);
        return new AnnotationMetaSingleAttribute(
                entity, element, erasedType.toString()
        );
    }

    @Override
    public AnnotationMetaAttribute visitDeclared(DeclaredType declaredType, Element element) {
        AnnotationMetaAttribute metaAttribute = null;
        TypeElement returnedElement = (TypeElement) context.getTypeUtils().asElement(declaredType);
        // WARNING: .toString() is necessary here since Name equals does not compare to String
        String fqNameOfReturnType = returnedElement.getQualifiedName().toString();
        String collection = Constants.COLLECTIONS.get(fqNameOfReturnType);
        if (collection != null) {
            return createMetaCollectionAttribute(
                    declaredType, element, fqNameOfReturnType, collection, null
            );
        } else if (isBasicAttribute(element, returnedElement)) {
            String type = returnedElement.getQualifiedName().toString();
            return new AnnotationMetaSingleAttribute(entity, element, type);
        }
        return metaAttribute;
    }

    private AnnotationMetaAttribute createMetaCollectionAttribute(DeclaredType declaredType, Element element, String fqNameOfReturnType, String collection, String targetEntity) {
        return new AnnotationMetaCollection(
                entity, element, collection, getElementType(declaredType, targetEntity)
        );
    }

    @Override
    public AnnotationMetaAttribute visitExecutable(ExecutableType t, Element p) {
        if (!p.getKind().equals(ElementKind.METHOD)) {
            return null;
        }

        String string = p.getSimpleName().toString();
        if (!StringUtil.isProperty(string, TypeUtils.toTypeString(t.getReturnType()))) {
            return null;
        }

        TypeMirror returnType = t.getReturnType();
        return returnType.accept(this, p);
    }

    private boolean isBasicAttribute(Element element, Element returnedElement) {
        if (TypeUtils.containsAnnotation(element, Constants.ID)) {
            return true;
        }
        BasicAttributeVisitor basicVisitor = new BasicAttributeVisitor(context);
        return returnedElement.asType().accept(basicVisitor, returnedElement);
    }

    private String getElementType(DeclaredType declaredType, String targetEntity) {
        if (targetEntity != null) {
            return targetEntity;
        }
        final List<? extends TypeMirror> mirrors = declaredType.getTypeArguments();
        if (mirrors.size() == 1) {
            final TypeMirror type = mirrors.get(0);
            return TypeUtils.extractClosestRealTypeAsString(type, context);
        } else if (mirrors.size() == 2) {
            return TypeUtils.extractClosestRealTypeAsString(mirrors.get(1), context);
        } else {
            //for 0 or many
            //0 is expected, many is not
            if (mirrors.size() > 2) {
                context.logMessage(
                        Diagnostic.Kind.WARNING, "Unable to find the closest solid type" + declaredType
                );
            }
            return "?";
        }
    }

}

/**
 * Checks whether the visited type is a basic attribute according to the JPA 2 spec
 * ( section 2.8 Mapping Defaults for Non-Relationship Fields or Properties)
 */
class BasicAttributeVisitor extends SimpleTypeVisitor6<Boolean, Element> {

    private final Context context;

    public BasicAttributeVisitor(Context context) {
        this.context = context;
    }

    @Override
    public Boolean visitPrimitive(PrimitiveType t, Element element) {
        return Boolean.TRUE;
    }

    @Override
    public Boolean visitArray(ArrayType t, Element element) {
        TypeMirror componentMirror = t.getComponentType();
        TypeElement componentElement = (TypeElement) context.getTypeUtils().asElement(componentMirror);

        return Constants.BASIC_ARRAY_TYPES.contains(componentElement.getQualifiedName().toString());
    }

    @Override
    public Boolean visitDeclared(DeclaredType declaredType, Element element) {
        if (ElementKind.ENUM.equals(element.getKind())) {
            return Boolean.TRUE;
        }

        if (ElementKind.CLASS.equals(element.getKind()) || ElementKind.INTERFACE.equals(element.getKind())) {
            TypeElement typeElement = ((TypeElement) element);
            String typeName = typeElement.getQualifiedName().toString();
            if (Constants.BASIC_TYPES.contains(typeName)) {
                return Boolean.TRUE;
            }
        }
        return Boolean.FALSE;
    }
}
