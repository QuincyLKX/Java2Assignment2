package cn.edu.sustech.cs209.chatting.common;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;

public class MyObjectInputStream extends ObjectInputStream {

    public MyObjectInputStream(InputStream in) throws IOException {
        super(in);
    }

    @Override
    public void readStreamHeader(){

    }
}
