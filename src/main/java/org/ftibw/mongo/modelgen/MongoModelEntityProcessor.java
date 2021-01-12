/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.ftibw.mongo.modelgen;

import org.ftibw.mongo.modelgen.annotation.AnnotationMetaEntity;
import org.ftibw.mongo.modelgen.model.MetaEntity;
import org.ftibw.mongo.modelgen.publics.dto.Specs;
import org.ftibw.mongo.modelgen.util.*;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.SimpleTypeVisitor6;
import javax.tools.Diagnostic;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Main annotation processor.
 *
 * @author Max Andersen
 * @author Hardy Ferentschik
 * @author Emmanuel Bernard
 */
@SupportedAnnotationTypes({
        Constants.ENTITY, Constants.MAPPED_SUPERCLASS
})
@SupportedOptions({
        MongoModelEntityProcessor.DEBUG_OPTION,
        MongoModelEntityProcessor.ADD_GENERATION_DATE,
        MongoModelEntityProcessor.ADD_GENERATED_ANNOTATION
})
public class MongoModelEntityProcessor extends AbstractProcessor {
    public static final String DEBUG_OPTION = "debug";
    public static final String ADD_GENERATION_DATE = "addGenerationDate";
    public static final String ADD_GENERATED_ANNOTATION = "addGeneratedAnnotation";

    private static final Boolean ALLOW_OTHER_PROCESSORS_TO_CLAIM_ANNOTATIONS = Boolean.FALSE;

    private Context context;

    @Override
    public void init(ProcessingEnvironment env) {
        super.init(env);
        context = new Context(env);
        context.logMessage(
                Diagnostic.Kind.NOTE, "Mongo Model Generator " + Version.getVersionString()
        );

        String tmp = env.getOptions().get(MongoModelEntityProcessor.ADD_GENERATED_ANNOTATION);
        if (tmp != null) {
            boolean addGeneratedAnnotation = Boolean.parseBoolean(tmp);
            context.setAddGeneratedAnnotation(addGeneratedAnnotation);
        }

        tmp = env.getOptions().get(MongoModelEntityProcessor.ADD_GENERATION_DATE);
        boolean addGenerationDate = Boolean.parseBoolean(tmp);
        context.setAddGenerationDate(addGenerationDate);
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public boolean process(final Set<? extends TypeElement> annotations, final RoundEnvironment roundEnvironment) {
        // see also METAGEN-45
        if (roundEnvironment.processingOver() || annotations.size() == 0) {
            return ALLOW_OTHER_PROCESSORS_TO_CLAIM_ANNOTATIONS;
        }

        Set<? extends Element> elements = roundEnvironment.getRootElements();
        for (Element element : elements) {

            if (isJPAEntity(element)) {

                context.logMessage(Diagnostic.Kind.OTHER, "Processing annotated class " + element.toString());
                handleRootElementAnnotationMirrors(element);
            }
        }

        for (MetaEntity entity : context.getMetaEntities()) {

            if (superclassNotCompiling(entity.getTypeElement())) {
                context.logMessage(Diagnostic.Kind.NOTE, "Need Superclass not be compiling for entity " + entity.getQualifiedName());
                return ALLOW_OTHER_PROCESSORS_TO_CLAIM_ANNOTATIONS;
            }

            if (entity.getTypeElement().getAnnotation(Specs.class) == null) {
                continue;
            }

            Element superElement = ClassWriter.findMappedSuperElement(entity, context);
            DtoSpec.buildDtoSpecifications(entity, superElement);
        }

        createMetaModelClasses();
        return ALLOW_OTHER_PROCESSORS_TO_CLAIM_ANNOTATIONS;
    }

    private static boolean superclassNotCompiling(TypeElement element) {
        TypeMirror superclass = element.getSuperclass();
        if (superclass.toString().endsWith(Constants.QUALIFIED_SUPER_ENTITY_SUFFIX)) {
            return superclass.getAnnotationMirrors().isEmpty();
        }
        return false;
    }

    private void createMetaModelClasses() {
        for (MetaEntity entity : context.getMetaEntities()) {
            if (context.isAlreadyGenerated(entity.getQualifiedName())) {
                continue;
            }
            context.logMessage(Diagnostic.Kind.OTHER, "Writing meta model for entity " + entity);
            ClassWriter.writeFile(entity, context);
            context.markGenerated(entity.getQualifiedName());
        }

        // we cannot process the delayed entities in any order. There might be dependencies between them.
        // we need to process the top level entities first
        Collection<MetaEntity> toProcessEntities = context.getMetaEmbeddables();
        while (!toProcessEntities.isEmpty()) {
            Set<MetaEntity> processedEntities = new HashSet<MetaEntity>();
            int toProcessCountBeforeLoop = toProcessEntities.size();
            for (MetaEntity entity : toProcessEntities) {
                // see METAGEN-36
                if (context.isAlreadyGenerated(entity.getQualifiedName())) {
                    processedEntities.add(entity);
                    continue;
                }
                if (modelGenerationNeedsToBeDeferred(toProcessEntities, entity)) {
                    continue;
                }
                context.logMessage(
                        Diagnostic.Kind.OTHER, "Writing meta model for embeddable/mapped superclass" + entity
                );
                ClassWriter.writeFile(entity, context);
                context.markGenerated(entity.getQualifiedName());
                processedEntities.add(entity);
            }
            toProcessEntities.removeAll(processedEntities);
            if (toProcessEntities.size() >= toProcessCountBeforeLoop) {
                context.logMessage(
                        Diagnostic.Kind.ERROR, "Potential endless loop in generation of entities."
                );
            }
        }
    }

    private boolean modelGenerationNeedsToBeDeferred(Collection<MetaEntity> entities, MetaEntity containedEntity) {
        ContainsAttributeTypeVisitor visitor = new ContainsAttributeTypeVisitor(
                containedEntity.getTypeElement(), context
        );
        for (MetaEntity entity : entities) {
            if (entity.equals(containedEntity)) {
                continue;
            }
            for (Element subElement : ElementFilter.fieldsIn(entity.getTypeElement().getEnclosedElements())) {
                TypeMirror mirror = subElement.asType();
                if (!TypeKind.DECLARED.equals(mirror.getKind())) {
                    continue;
                }
                boolean contains = mirror.accept(visitor, subElement);
                if (contains) {
                    return true;
                }
            }
            for (Element subElement : ElementFilter.methodsIn(entity.getTypeElement().getEnclosedElements())) {
                TypeMirror mirror = subElement.asType();
                if (!TypeKind.DECLARED.equals(mirror.getKind())) {
                    continue;
                }
                boolean contains = mirror.accept(visitor, subElement);
                if (contains) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isJPAEntity(Element element) {
        return TypeUtils.containsAnnotation(
                element,
                Constants.ENTITY,
                Constants.MAPPED_SUPERCLASS
        );
    }

    private void handleRootElementAnnotationMirrors(final Element element) {
        if (!ElementKind.CLASS.equals(element.getKind())) {
            return;
        }

        List<? extends AnnotationMirror> annotationMirrors = element.getAnnotationMirrors();
        for (AnnotationMirror mirror : annotationMirrors) {
            boolean requiresLazyMemberInitialization = false;

            AnnotationMetaEntity metaEntity;
            if (TypeUtils.containsAnnotation(element, Constants.MAPPED_SUPERCLASS)) {
                requiresLazyMemberInitialization = true;
            }

            metaEntity = new AnnotationMetaEntity((TypeElement) element, context, requiresLazyMemberInitialization);

            addMetaEntityToContext(mirror, metaEntity);
        }
    }

    private void addMetaEntityToContext(AnnotationMirror mirror, AnnotationMetaEntity metaEntity) {
        if (TypeUtils.isAnnotationMirrorOfType(mirror, Constants.ENTITY)) {
            context.addMetaEntity(metaEntity.getQualifiedName(), metaEntity);
        } else if (TypeUtils.isAnnotationMirrorOfType(mirror, Constants.MAPPED_SUPERCLASS)) {
            context.addMetaEntity(metaEntity.getQualifiedName(), metaEntity);
        }
    }


    static class ContainsAttributeTypeVisitor extends SimpleTypeVisitor6<Boolean, Element> {

        private Context context;
        private TypeElement type;

        ContainsAttributeTypeVisitor(TypeElement elem, Context context) {
            this.context = context;
            this.type = elem;
        }

        @Override
        public Boolean visitDeclared(DeclaredType declaredType, Element element) {
            TypeElement returnedElement = (TypeElement) context.getTypeUtils().asElement(declaredType);
            String fqNameOfReturnType = returnedElement.getQualifiedName().toString();

            if (type.getQualifiedName().toString().equals(fqNameOfReturnType)) {
                return Boolean.TRUE;
            } else {
                return Boolean.FALSE;
            }
        }

        @Override
        public Boolean visitExecutable(ExecutableType t, Element element) {
            if (!element.getKind().equals(ElementKind.METHOD)) {
                return Boolean.FALSE;
            }

            String string = element.getSimpleName().toString();
            if (!StringUtil.isProperty(string, TypeUtils.toTypeString(t.getReturnType()))) {
                return Boolean.FALSE;
            }

            TypeMirror returnType = t.getReturnType();
            return returnType.accept(this, element);
        }
    }
}
