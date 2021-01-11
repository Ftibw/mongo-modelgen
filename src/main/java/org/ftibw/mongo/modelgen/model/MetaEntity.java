/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.ftibw.mongo.modelgen.model;

import javax.lang.model.element.TypeElement;
import java.util.List;

/**
 * @author Hardy Ferentschik
 */
public interface MetaEntity extends ImportContext {
	String getSimpleName();

	String getQualifiedName();

	String getPackageName();

	List<MetaAttribute> getMembers();

	@Override
	String generateImports();

	@Override
	String importType(String fqcn);

	@Override
	String staticImport(String fqcn, String member);

	TypeElement getTypeElement();

	boolean isMetaComplete();
}
