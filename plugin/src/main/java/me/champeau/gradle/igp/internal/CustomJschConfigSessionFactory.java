package me.champeau.gradle.igp.internal;

import com.jcraft.jsch.JSch;
import org.eclipse.jgit.transport.JschConfigSessionFactory;

// See: https://github.com/mwiede/jsch/issues/43
public class CustomJschConfigSessionFactory extends JschConfigSessionFactory {
    static {
        JSch.setConfig("signature.rsa", JSch.getConfig("ssh-rsa"));
    }
}
