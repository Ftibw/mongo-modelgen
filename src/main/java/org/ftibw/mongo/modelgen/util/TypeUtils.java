/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.ftibw.mongo.modelgen.util;

import org.ftibw.mongo.modelgen.Context;

import javax.lang.model.element.*;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.util.SimpleTypeVisitor6;
import java.util.*;


/**
 * Utility class.
 *
 * @author Max Andersen
 * @author Hardy Ferentschik
 * @author Emmanuel Bernard
 */
public final class TypeUtils {

    private static final Map<TypeKind, String> PRIMITIVE_WRAPPERS = new HashMap<TypeKind, String>();
    private static final Map<TypeKind, String> PRIMITIVES = new HashMap<TypeKind, String>();

    static {
        PRIMITIVE_WRAPPERS.put(TypeKind.CHAR, "Character");

        PRIMITIVE_WRAPPERS.put(TypeKind.BYTE, "Byte");
        PRIMITIVE_WRAPPERS.put(TypeKind.SHORT, "Short");
        PRIMITIVE_WRAPPERS.put(TypeKind.INT, "Integer");
        PRIMITIVE_WRAPPERS.put(TypeKind.LONG, "Long");

        PRIMITIVE_WRAPPERS.put(TypeKind.BOOLEAN, "Boolean");

        PRIMITIVE_WRAPPERS.put(TypeKind.FLOAT, "Float");
        PRIMITIVE_WRAPPERS.put(TypeKind.DOUBLE, "Double");

        PRIMITIVES.put(TypeKind.CHAR, "char");
        PRIMITIVES.put(TypeKind.BYTE, "byte");
        PRIMITIVES.put(TypeKind.SHORT, "short");
        PRIMITIVES.put(TypeKind.INT, "int");
        PRIMITIVES.put(TypeKind.LONG, "long");
        PRIMITIVES.put(TypeKind.BOOLEAN, "boolean");
        PRIMITIVES.put(TypeKind.FLOAT, "float");
        PRIMITIVES.put(TypeKind.DOUBLE, "double");
    }

    private TypeUtils() {
    }

    public static String toTypeString(TypeMirror type) {
        if (type.getKind().isPrimitive()) {
            return PRIMITIVE_WRAPPERS.get(type.getKind());
        }
        return type.toString();
    }

    public static String toArrayTypeString(ArrayType type, Context context) {
        TypeMirror componentType = type.getComponentType();
        if (componentType.getKind().isPrimitive()) {
            return PRIMITIVES.get(componentType.getKind()) + "[]";
        }

        // When an ArrayType is annotated with an annotation which uses TYPE_USE targets,
        // we cannot simply take the TypeMirror returned by #getComponentType because it
        // itself is an AnnotatedType.
        //
        // The simplest approach here to get the TypeMirror for both ArrayType use cases
        // is to use the visitor to retrieve the underlying TypeMirror.
        TypeMirror component = componentType.accept(
                new SimpleTypeVisitor6<TypeMirror, Void>() {
                    @Override
                    protected TypeMirror defaultAction(TypeMirror e, Void aVoid) {
                        return e;
                    }
                },
                null
        );

        return extractClosestRealTypeAsString(component, context) + "[]";
    }

    public static String extractClosestRealTypeAsString(TypeMirror type, Context context) {
        if (type instanceof TypeVariable) {
            final TypeMirror compositeUpperBound = ((TypeVariable) type).getUpperBound();
            return extractClosestRealTypeAsString(compositeUpperBound, context);
        } else {
            final TypeMirror erasureType = context.getTypeUtils().erasure(type);
            if (TypeKind.ARRAY.equals(erasureType.getKind())) {
                // keep old behavior here for arrays since #asElement returns null for them.
                return erasureType.toString();
            } else {
                return ((TypeElement) context.getTypeUtils().asElement(erasureType)).getQualifiedName().toString();
            }
        }
    }

    public static boolean containsAnnotation(Element element, String... annotations) {
        assert element != null;
        assert annotations != null;

        List<String> annotationClassNames = new ArrayList<String>();
        Collections.addAll(annotationClassNames, annotations);

        List<? extends AnnotationMirror> annotationMirrors = element.getAnnotationMirrors();
        for (AnnotationMirror mirror : annotationMirrors) {
            if (annotationClassNames.contains(mirror.getAnnotationType().toString())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns {@code true} if the provided annotation type is of the same type as the provided class, {@code false} otherwise.
     * This method uses the string class names for comparison. See also
     * <a href="http://www.retep.org/2009/02/getting-class-values-from-annotations.html">getting-class-values-from-annotations</a>.
     *
     * @param annotationMirror The annotation mirror
     * @param fqcn             the fully qualified class name to check against
     * @return {@code true} if the provided annotation type is of the same type as the provided class, {@code false} otherwise.
     */
    public static boolean isAnnotationMirrorOfType(AnnotationMirror annotationMirror, String fqcn) {
        assert annotationMirror != null;
        assert fqcn != null;
        String annotationClassName = annotationMirror.getAnnotationType().toString();

        return annotationClassName.equals(fqcn);
    }

    public static Object getAnnotationValue(AnnotationMirror annotationMirror, String parameterValue) {
        assert annotationMirror != null;
        assert parameterValue != null;

        Object returnValue = null;
        for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : annotationMirror.getElementValues()
                .entrySet()) {
            if (parameterValue.equals(entry.getKey().getSimpleName().toString())) {
                returnValue = entry.getValue().getValue();
                break;
            }
        }
        return returnValue;
    }

}
