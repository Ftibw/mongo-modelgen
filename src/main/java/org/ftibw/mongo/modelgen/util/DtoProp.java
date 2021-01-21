package org.ftibw.mongo.modelgen.util;

import org.ftibw.mongo.modelgen.publics.dto.Rule;

import java.util.List;

/**
 * @author : Ftibw
 * @date : 2020/1/8 15:12
 */
public class DtoProp {
    private String propName;
    private String descr;
    private String typeDeclare;
    private List<String> typeImports;
    private Rule[] rules;
    /**
     * override EqualsAndHashCode if true
     */
    private boolean hash;

    public DtoProp(String propName, String descr) {
        this.propName = propName;
        this.descr = descr;
    }

    public DtoProp(String propName, String descr, Rule[] rules, boolean hash) {
        this.propName = propName;
        this.descr = descr;
        this.rules = rules;
        this.hash = hash;
    }

    public DtoProp(String propName, String descr, String typeDeclare, List<String> typeImports,
                   Rule[] rules, boolean hash) {
        this.propName = propName;
        this.descr = descr;
        this.typeDeclare = typeDeclare;
        this.typeImports = typeImports;
        this.rules = rules;
        this.hash = hash;
    }

    public String getPropName() {
        return propName;
    }

    public String getDescr() {
        return descr;
    }

    public String getTypeDeclare() {
        return typeDeclare;
    }

    public List<String> getTypeImports() {
        return typeImports;
    }

    public Rule[] getRules() {
        return rules;
    }

    public boolean isHash() {
        return hash;
    }
}
