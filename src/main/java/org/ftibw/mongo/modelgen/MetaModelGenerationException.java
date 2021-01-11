/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.ftibw.mongo.modelgen;

/**
 * {@code RuntimeException} used for errors during com.greentown.poststation.meta model generation.
 *
 * @author Hardy Ferentschik
 */
public class MetaModelGenerationException extends RuntimeException {
    public MetaModelGenerationException() {
        super();
    }

    public MetaModelGenerationException(String message) {
        super(message);
    }
}