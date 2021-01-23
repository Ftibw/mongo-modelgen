package org.ftibw.mongo.modelgen.publics.dto;

import java.util.HashMap;
import java.util.Map;

/**
 * dto常用的属性约束校验规则
 * 用于{@link Prop#rule}
 *
 * @author : Ftibw
 * @date : 2021/1/7 14:03
 */
@SuppressWarnings("AlibabaConstantFieldShouldBeUpperCase")
public class Rule_ {
    /**
     * pojo、集合、Map嵌套校验时需要此注解
     */
    public static final String Valid = "javax.validation.Valid";
    /**
     * 字符串非null且非空
     */
    public static final String NotBlank = "javax.validation.constraints.NotBlank";
    /**
     * 任何类型非null
     */
    public static final String NotNull = "javax.validation.constraints.NotNull";
    /**
     * 字符串、集合、Map、数组非null且非空
     */
    public static final String NotEmpty = "javax.validation.constraints.NotEmpty";
    /**
     * 字符串正则匹配（null值不校验）
     */
    public static final String Pattern = "javax.validation.constraints.Pattern";
    /**
     * 字符串、集合、Map、数组长度匹配
     */
    public static final String Size = "javax.validation.constraints.Size";
    /**
     * 数字（包含字符形式）值范围匹配
     */
    public static final String Range = "org.hibernate.validator.constraints.Range";

    /**
     * 除了message之外，支持的注解选项
     */
    static final Map<String, String[]> CONSTRAINT_SUPPORTED_OPTIONS = new HashMap<>(7);

    static {
        CONSTRAINT_SUPPORTED_OPTIONS.put(Valid, null);
        CONSTRAINT_SUPPORTED_OPTIONS.put(NotBlank, null);
        CONSTRAINT_SUPPORTED_OPTIONS.put(NotNull, null);
        CONSTRAINT_SUPPORTED_OPTIONS.put(NotEmpty, null);
        CONSTRAINT_SUPPORTED_OPTIONS.put(Pattern, new String[]{"regexp"});
        CONSTRAINT_SUPPORTED_OPTIONS.put(Size, new String[]{"min", "max"});
        CONSTRAINT_SUPPORTED_OPTIONS.put(Range, new String[]{"min", "max"});
    }

    /**
     * 非标准的正则表达式字符串：写入文件时，如果存在转义符\则每个转义符\本身也需要转义
     */
    public static final String REGEX_DAY_TIME = "^[0-2][0-9]:[0-5][0-9]$";
    public static final String REGEX_PHONE_NUMBER = "^1[345789]\\\\d{9}$";
    public static final String REGEX_FILE_BASE64 = "^data:.*;base64,.*$";

    public static String getType(Rule rule) {
        String[] value = rule.value();
        return value.length == 0 ? "" : value[0];
    }

    private static boolean needPrintMessage(String constraint, String msg) {
        if (Valid.equals(constraint)) {
            return false;
        }
        return !msg.trim().isEmpty();
    }

    private static boolean isStringValueOption(String constraint, String opt) {
        if (Pattern.equals(constraint)) {
            return "regexp".equals(opt);
        }
        return false;
    }

    private static String[] getOptValues(String constraint, String[] ruleValue) {
        String[] optValues = {};
        if (ruleValue.length > 2) {
            if (Pattern.equals(constraint)) {
                optValues = new String[]{ruleValue[2]};

            } else if (Size.equals(constraint) || Range.equals(constraint)) {
                optValues = ruleValue[2].split(",");
            }
        }
        return optValues;
    }


    public static String getConstraintAnnotationDeclareString(String constraintSimpleName, Rule rule) {
        String[] value = rule.value();
        if (value.length == 0) {
            return "";
        }

        String constraint = value[0];
        if (!CONSTRAINT_SUPPORTED_OPTIONS.containsKey(constraint)) {
            return "";
        }
        String msg = value.length > 1 ? value[1] : "";
        String[] optValues = getOptValues(constraint, value);
        String[] supportedOptions = CONSTRAINT_SUPPORTED_OPTIONS.get(constraint);

        StringBuilder sb = new StringBuilder();
        sb.append("@").append(constraintSimpleName);

        int valLen = optValues.length;
        if (supportedOptions == null || valLen == 0) {
            if (needPrintMessage(constraint, msg)) {
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
                    if (isStringValueOption(constraint, opt)) {
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
