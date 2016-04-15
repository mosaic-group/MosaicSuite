package mosaic.utils.io.ssh;
//
//import java.io.BufferedReader;
//import java.io.FileReader;
//import java.io.IOException;
//import java.io.InputStreamReader;
//import java.io.OutputStream;
//import java.io.PipedInputStream;
//import java.io.PipedOutputStream;
//
//import com.jcraft.jsch.Channel;
//import com.jcraft.jsch.JSch;
//import com.jcraft.jsch.JSchException;
//import com.jcraft.jsch.Session;

import ij.IJ;

public class Ssh {
//    private static final int SSH_PORT = 22;
//    
//    private JSch jsch;
//    private Session session;
//    
//    Ssh(String aHostAddress, String aUserName, String aPassword, String aKeyPath) throws JSchException {
//        jsch = new JSch();
//        
//        createSession(aHostAddress, aUserName, aPassword, aKeyPath);
//    }
//    
//    public void close() {
//        session.disconnect();
//    }
//    
//    public void getSsh() {
//        try {
//            Channel channel = session.openChannel("shell");
//            final PipedInputStream in = new PipedInputStream();
//            PipedOutputStream out;
//            try {
//                out = new PipedOutputStream(in);
//                channel.setOutputStream(out);
//            }
//            catch (IOException e) {
//                e.printStackTrace();
//            }
////            channel.setInputStream(out);
//            channel.connect();
//            
//            
//            int num = 1;
//            try (BufferedReader br = new BufferedReader(new InputStreamReader(channel.getInputStream()))) {
//                for (String line = br.readLine(); line != null; line = br.readLine()) {
//                   System.out.println("===> ["+num+"] " + line);
//                   if (num == 11) break;
//                   num++;
//                }
//            }
//            catch (IOException e1) {
//                e1.printStackTrace();
//            }
//            
//            try {
//                System.out.println(">>>>>>>>>>>>");
//                OutputStream outputStream = channel.getOutputStream();
//                outputStream.write(new String("ls\n").getBytes());
//                outputStream.close();
//                System.out.println("<<<<<<<<<<<<");
//            }
//            catch (IOException e) {
//                e.printStackTrace();
//            }
//            num = 1;
//            try (BufferedReader br = new BufferedReader(new InputStreamReader(channel.getInputStream()))) {
//                for (String line = br.readLine(); line != null; line = br.readLine()) {
//                   System.out.println("===> ["+num+"] " + line);
//                   if (num == 11) break;
//                   num++;
//                }
//            }
//            catch (IOException e1) {
//                e1.printStackTrace();
//            }            
////            byte[] tmp=new byte[1024];
////            while(true){
////              try {
////                while(in.available()>0){
////                    int i=in.read(tmp, 0, 1024);
////                    if(i<0)break;
////                    System.out.print(new String(tmp, 0, i));
////                  }
////              if(channel.isClosed()){
////                if(in.available()>0) continue; 
////                System.out.println("exit-status: "+channel.getExitStatus());
////                break;
////              }
////              }
////              catch (IOException e) {
////                  // TODO Auto-generated catch block
////                  e.printStackTrace();
////              }
////              try{Thread.sleep(1000);}catch(Exception ee){}
////            }
//            
//        }
//        catch (JSchException e) {
//            e.printStackTrace();
//        }
//    }
//    
//    private boolean createSession(String aHostAddress, String aUserName, String aPassword, String aKeyPath) throws JSchException {
//        if (session != null && session.isConnected() == true) {
//            return true;
//        }
//
//        if (aPassword == null || aPassword.length() == 0) {
//            // Try to open the standard private key
//            String keyPath = (aKeyPath == null) ? System.getProperty("user.home") + "/.ssh/id_rsa" : aKeyPath;
//            jsch.addIdentity(keyPath);
//            session = jsch.getSession(aUserName, aHostAddress, SSH_PORT);
//        }
//        else {
//            session = jsch.getSession(aUserName, aHostAddress, SSH_PORT);
//            session.setPassword(aPassword);
//        }
//
//        final java.util.Properties config = new java.util.Properties();
//        config.put("StrictHostKeyChecking", "no");
//        session.setConfig(config);
//        
//        try {
//            session.connect();
//        }
//        catch (final JSchException e) {
//            IJ.error("Connection failed", e.getMessage());
//            return false;
//        }
//
//        return true;
//    }
//    
    
}
