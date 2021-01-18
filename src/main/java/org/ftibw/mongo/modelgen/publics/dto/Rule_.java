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
     * pojo、集合、Map嵌套校验时需要此注解
     */
    Valid("javax.validation.Valid", null),
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
     * 非标准的正则表达式字符串：写入文件时，如果存在转义符\则每个转义符\本身也需要转义
     */
    public static final String REGEX_DAY_TIME = "^[0-2][0-9]:[0-5][0-9]$";
    public static final String REGEX_PHONE_NUMBER = "^1[345789]\\\\d{9}$";
    public static final String REGEX_BASE64_STRING = "^data:.*;base64,.*$";

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

    private boolean needPrintMessage(String msg) {
        if (this == Valid) {
            return false;
        }
        return !msg.trim().isEmpty();
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
            if (this.needPrintMessage(msg)) {
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
