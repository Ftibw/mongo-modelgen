package org.ftibw.mongo.modelgen.publics.dto;

/**
 * dto常用的属性约束校验规则
 * 用于{@link Prop#rule}
 *
 * @author : Ftibw
 * @date : 2021/1/7 14:03
 */
public enum Rule_ {
    /**
     * 字符串非null且非空
     */
    NotBlank("javax.validation.constraints.NotBlank", null),
    /**
     * 任何类型非null
     */
    NotNull("javax.validation.constraints.NotNull", null),
    /**
     * 字符串、集合、Map、数组非null且非空
     */
    NotEmpty("javax.validation.constraints.NotEmpty", null),
    /**
     * 字符串正则匹配（null值不校验）
     */
    Pattern("javax.validation.constraints.Pattern", new String[]{"regexp"}),
    /**
     * 字符串、集合、Map、数组长度匹配
     */
    Size("javax.validation.constraints.Size", new String[]{"min", "max"}),
    /**
     * 数字（包含字符形式）值范围匹配
     */
    Range("org.hibernate.validator.constraints.Range", new String[]{"min", "max"});

    /**
     * 注解完全限定名
     */
    private String type;
    /**
     * 除了message之外，支持的注解选项
     */
    private String[] supportedOptions;

    Rule_(String type, String[] supportedOptions) {
        this.type = type;
        this.supportedOptions = supportedOptions;
    }

    public String getType() {
        return type;
    }

    private static boolean isStringValueOption(Rule_ anno, String opt) {
        if (anno == Pattern) {
            return "regexp".equals(opt);
        }
        return false;
    }

    public String getConstraintAnnotationDeclareString(Rule rule) {
        String msg = rule.msg();
        String[] optValues = rule.optVal();
        String[] supportedOptions = this.supportedOptions;
        StringBuilder sb = new StringBuilder();
        sb.append("@").append(this.name());

        int valLen = optValues.length;
        if (supportedOptions == null || valLen == 0) {
            if (!msg.trim().isEmpty()) {
                sb.append("(");
                sb.append("message = \"").append(msg).append("\"");
                sb.append(")");
            }
        }
        // e.g. @Range(min = 0, max = 1, message = "")
        // e.g. @Size(max = 20, message = "")
        else {
            sb.append("(");
            int keyLen = supportedOptions.length;
            for (int i = 0; i < keyLen; i++) {
                if (i < valLen) {
                    String optVal = optValues[i];
                    if (optVal.trim().isEmpty()) {
                        continue;
                    }
                    String opt = supportedOptions[i];
                    sb.append(opt).append(" = ");
                    if (isStringValueOption(this, opt)) {
                        sb.append("\"").append(optVal).append("\"");
                    } else {
                        sb.append(optVal);
                    }
                    if (i < valLen - 1 && i < keyLen - 1) {
                        sb.append(", ");
                    }
                }
            }
            if (!msg.trim().isEmpty()) {
                sb.append(", ");
                sb.append("message = \"").append(msg).append("\"");
            }
            sb.append(")");
        }
        return sb.toString();
    }

//        public static void main(String[] args) {
//            System.out.println(getConstraintAnnotationDeclareString(Rule_.Size, "", "1", "2"));
//            System.out.println(getConstraintAnnotationDeclareString(Rule_.Pattern, "", "^.12.*$", "2"));
//            System.out.println(getConstraintAnnotationDeclareString(Rule_.NotBlank, "", "1", "2"));
//            System.out.println(getConstraintAnnotationDeclareString(Rule_.Size, "111", "", "2"));
//            System.out.println(getConstraintAnnotationDeclareString(Rule_.Pattern, "222", "^.12.*$", "2"));
//            System.out.println(getConstraintAnnotationDeclareString(Rule_.NotBlank, "333", "1", "2"));
//        }

}
