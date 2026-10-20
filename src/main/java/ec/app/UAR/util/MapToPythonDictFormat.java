package ec.app.UAR.util;

import java.util.HashMap;

public class MapToPythonDictFormat {
    public static String output(HashMap<?, ?> map) {
        StringBuilder sb = new StringBuilder("{");

        boolean first = true;

        for (HashMap.Entry<?, ?> entry : map.entrySet()) {
            if (!first) {
                sb.append(", ");
            }
            // 处理键
            sb.append("'").append(entry.getKey()).append("': ");

            // 处理值
            Object value = entry.getValue();
            if (value instanceof Object[]) {
                // 如果值是对象数组
                sb.append(arrayToString((Object[]) value));
            } else if (value.getClass().isArray()) {
                // 如果值是基本类型数组
                sb.append(primitiveArrayToString(value));
            } else {
                // 其他类型直接调用toString
                sb.append(value.toString());
            }

            first = false;
        }

        sb.append("}");
        return sb.toString();
    }

    // 处理对象数组
    private static String arrayToString(Object[] array) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < array.length; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(array[i].toString());
        }
        sb.append("]");
        return sb.append(sb).toString();
    }

    // 处理基本类型数组
    private static String primitiveArrayToString(Object array) {
        StringBuilder sb = new StringBuilder("[");
        int length = java.lang.reflect.Array.getLength(array);
        for (int i = 0; i < length; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(java.lang.reflect.Array.get(array, i).toString());
        }
        sb.append("]");
        return sb.toString();
    }
}
