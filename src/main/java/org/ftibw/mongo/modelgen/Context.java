/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.ftibw.mongo.modelgen;

import org.ftibw.mongo.modelgen.model.MetaEntity;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 * @author Max Andersen
 * @author Hardy Ferentschik
 * @author Emmanuel Bernard
 */
public final class Context {
    /**
     * Used for keeping track of parsed entities and mapped super classes (xml + annotations).
     */
    private final Map<String, MetaEntity> metaEntities = new HashMap<String, MetaEntity>();

    /**
     * Used for keeping track of parsed embeddable entities. These entities have to be kept separate since
     * they are lazily initialized.
     */
    private final Map<String, MetaEntity> metaEmbeddables = new HashMap<String, MetaEntity>();

    private final ProcessingEnvironment pe;
    private final boolean logDebug;

    private boolean addGeneratedAnnotation = true;
    private boolean addGenerationDate;

    // keep track of all classes for which model have been generated
    private final Collection<String> generatedModelClasses = new HashSet<String>();

    private final Collection<String> dirtImports = new HashSet<>();

    public Context(ProcessingEnvironment pe) {
        this.pe = pe;
        logDebug = Boolean.parseBoolean(pe.getOptions().get(MongoModelEntityProcessor.DEBUG_OPTION));
    }

    public ProcessingEnvironment getProcessingEnvironment() {
        return pe;
    }

    public boolean addGeneratedAnnotation() {
        return addGeneratedAnnotation;
    }

    public void setAddGeneratedAnnotation(boolean addGeneratedAnnotation) {
        this.addGeneratedAnnotation = addGeneratedAnnotation;
    }

    public boolean addGeneratedDate() {
        return addGenerationDate;
    }

    public void setAddGenerationDate(boolean addGenerationDate) {
        this.addGenerationDate = addGenerationDate;
    }

    public Elements getElementUtils() {
        return pe.getElementUtils();
    }

    public Types getTypeUtils() {
        return pe.getTypeUtils();
    }

    public boolean containsMetaEntity(String fqcn) {
        return metaEntities.containsKey(fqcn);
    }

    public MetaEntity getMetaEntity(String fqcn) {
        return metaEntities.get(fqcn);
    }

    public Collection<MetaEntity> getMetaEntities() {
        return metaEntities.values();
    }

    public void addMetaEntity(String fqcn, MetaEntity metaEntity) {
        metaEntities.put(fqcn, metaEntity);
    }

    public boolean containsMetaEmbeddable(String fqcn) {
        return metaEmbeddables.containsKey(fqcn);
    }

    public MetaEntity getMetaEmbeddable(String fqcn) {
        return metaEmbeddables.get(fqcn);
    }

    public void addMetaEmbeddable(String fqcn, MetaEntity metaEntity) {
        metaEmbeddables.put(fqcn, metaEntity);
    }

    public Collection<MetaEntity> getMetaEmbeddables() {
        return metaEmbeddables.values();
    }

    public TypeElement getTypeElementForFullyQualifiedName(String fqcn) {
        Elements elementUtils = pe.getElementUtils();
        return elementUtils.getTypeElement(fqcn);
    }

    void markGenerated(String name) {
        generatedModelClasses.add(name);
    }

    boolean isAlreadyGenerated(String name) {
        return generatedModelClasses.contains(name);
    }

    public void logMessage(Diagnostic.Kind type, String message) {
        if (!logDebug && type.equals(Diagnostic.Kind.OTHER)) {
            return;
        }
        pe.getMessager().printMessage(type, message);
    }

    public void clearDirtImports(MetaEntity entity) {
        entity.clearImports(dirtImports);
        dirtImports.clear();
    }

    public String importDirtType(MetaEntity entity, String typeImport) {
        if (!entity.getImports().contains(typeImport)) {
            dirtImports.add(typeImport);
        }
        return entity.importType(typeImport);
    }

}
