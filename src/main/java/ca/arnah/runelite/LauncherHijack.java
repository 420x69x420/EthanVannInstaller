package ca.arnah.runelite;


import net.runelite.client.RuneLite;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import javax.swing.UIManager;

/**
 * @author Arnah
 * @since Nov 07, 2020
 */
public class LauncherHijack{

	public LauncherHijack(){
		new Thread(()->{
			// First we need to grab the ClassLoader the launcher uses to launch the client.
			ClassLoader objClassLoader;
			loop:
			while(true){
				objClassLoader = (ClassLoader) UIManager.get("ClassLoader");
				if(objClassLoader != null){
					for(Package pack : objClassLoader.getDefinedPackages()){
						if(pack.getName().equals("net.runelite.client.rs")){
							break loop;
						}
					}
				}
				try{
					Thread.sleep(100);
				}catch(Exception ex){
					ex.printStackTrace();
				}
			}
			System.out.println("Classloader found");
			try{
				URLClassLoader classLoader = (URLClassLoader) objClassLoader;
				// Add our hijack client to the classloader
				Method addUrl = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
				addUrl.setAccessible(true);

				URI uri = LauncherHijack.class.getProtectionDomain().getCodeSource().getLocation().toURI();
				if(uri.getPath().endsWith("classes/")){// Intellij
					uri = uri.resolve("..");
				}
				if(!uri.getPath().endsWith(".jar")){
					uri = uri.resolve("EthanVannInstaller.jar");
				}
				addUrl.invoke(classLoader, uri.toURL());
				System.out.println(uri.getPath());

				// Execute our code inside the runelite client classloader
				Class<?> clazz = classLoader.loadClass(ClientHijack.class.getName());
				clazz.getConstructor().newInstance();
			}catch(Exception ex){
				ex.printStackTrace();
			}
		}).start();
	}
	private static void setupProxy() {
	        String proxyHost = System.getProperty("socksProxyHost");
	        String proxyPort = System.getProperty("socksProxyPort");
	        String proxyUsername = System.getProperty("java.net.socks.username");
	        String proxyPassword = System.getProperty("java.net.socks.password");
	
	        if (proxyHost != null && proxyPort != null) {
	            System.out.println("Setting up SOCKS proxy: " + proxyHost + ":" + proxyPort);
	            
	            if (proxyUsername != null && proxyPassword != null) {
	                System.out.println("Using proxy authentication");
	                final String user = proxyUsername;
	                final char[] pass = proxyPassword.toCharArray();
	
	                Authenticator.setDefault(new Authenticator() {
	                    private final PasswordAuthentication auth = new PasswordAuthentication(user, pass);
	
	                    protected PasswordAuthentication getPasswordAuthentication() {
	                        return auth;
	                    }
	                });
	            }
	        }
	    }

    	public static void main(String[] args) {
	        // Set up proxy
	        setupProxy();
		// Force disable the "JVMLauncher", was just easiest way to do what I wanted at the time.
		System.setProperty("runelite.launcher.reflect", "true");
		new LauncherHijack();
		// Launcher.main(args);
		try{
			Class<?> clazz = Class.forName("net.runelite.launcher.Launcher");
			clazz.getMethod("main", String[].class).invoke(null, (Object) args);
		}catch(Exception ignored){
		}
	}
}
