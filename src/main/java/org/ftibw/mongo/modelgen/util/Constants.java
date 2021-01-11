/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.ftibw.mongo.modelgen.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Hardy Ferentschik
 */
public final class Constants {
    // we are trying to to reference jpa annotations directly
    public static final String ENTITY = "org.springframework.data.mongodb.core.mapping.Document";
    public static final String ID = "org.springframework.data.annotation.Id";
    public static final String TRANSIENT = "org.springframework.data.annotation.Transient";

    /**
     * {@link org.ftibw.mongo.modelgen.publics.MappedSuperclass}
     */
    public static final String MAPPED_SUPERCLASS = "org.ftibw.mongo.modelgen.publics.MappedSuperclass";

    public static final Map<String, String> COLLECTIONS = new HashMap<>();

    static {
        COLLECTIONS.put(java.util.Collection.class.getName(), "java.util.Collection");
        COLLECTIONS.put(java.util.Set.class.getName(), "java.util.Set");
        COLLECTIONS.put(java.util.List.class.getName(), "java.util.List");
        COLLECTIONS.put(java.util.Map.class.getName(), "java.util.Map");

        // Hibernate also supports the SortedSet and SortedMap interfaces
        COLLECTIONS.put(java.util.SortedSet.class.getName(), "java.util.SortedSet");
        COLLECTIONS.put(java.util.SortedMap.class.getName(), "java.util.SortedMap");
    }

    public static final List<String> BASIC_TYPES = new ArrayList<>();

    static {
        BASIC_TYPES.add(String.class.getName());
        BASIC_TYPES.add(Boolean.class.getName());
        BASIC_TYPES.add(Byte.class.getName());
        BASIC_TYPES.add(Character.class.getName());
        BASIC_TYPES.add(Short.class.getName());
        BASIC_TYPES.add(Integer.class.getName());
        BASIC_TYPES.add(Long.class.getName());
        BASIC_TYPES.add(Float.class.getName());
        BASIC_TYPES.add(Double.class.getName());
        BASIC_TYPES.add(java.math.BigInteger.class.getName());
        BASIC_TYPES.add(java.math.BigDecimal.class.getName());
        BASIC_TYPES.add(java.util.Date.class.getName());
        BASIC_TYPES.add(java.util.Calendar.class.getName());
        BASIC_TYPES.add(java.sql.Date.class.getName());
        BASIC_TYPES.add(java.sql.Time.class.getName());
        BASIC_TYPES.add(java.sql.Timestamp.class.getName());
        BASIC_TYPES.add(java.sql.Blob.class.getName());

        BASIC_TYPES.add("org.springframework.data.geo.Point");
    }

    public static final List<String> BASIC_ARRAY_TYPES = new ArrayList<>();

    static {
        BASIC_ARRAY_TYPES.add(Character.class.getName());
        BASIC_ARRAY_TYPES.add(Byte.class.getName());
    }

    private Constants() {
    }
}
