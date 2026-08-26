package io.aik.steins.grimoire.core.storage;

import cn.hutool.core.util.StrUtil;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import io.aik.steins.grimoire.core.config.FileStorageConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Properties;

/**
 * SFTP 文件存储策略
 *
 * @author a I k .
 * @implNote JDK 8
 * @since 2026/08/26
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "grimoire.file", name = "use", havingValue = "sftp"
)
public class SftpFileStorage extends AbstractFileStorage {

    private final FileStorageConfig fileStorageConfig;

    @Override
    public String upload(InputStream inputStream, String originalFilename) throws Exception {
        String storedName = generateStoredName(originalFilename);
        FileStorageConfig.SftpConfig config = fileStorageConfig.getMethod().getSftp();
        String basePath = config.getBasePath();
        if (StrUtil.isBlank(basePath)) {
            basePath = "/";
        }
        if (!basePath.endsWith("/")) {
            basePath = basePath + "/";
        }
        String remotePath = basePath + storedName;

        ChannelSftp sftp = null;
        Session session = null;
        try {
            session = createSession(config);
            sftp = (ChannelSftp) session.openChannel("sftp");
            sftp.connect();
            sftp.put(inputStream, remotePath);
            return remotePath;
        } finally {
            disconnect(sftp, session);
        }
    }

    @Override
    public byte[] download(String storedPath) throws Exception {
        FileStorageConfig.SftpConfig config = fileStorageConfig.getMethod().getSftp();
        ChannelSftp sftp = null;
        Session session = null;
        try {
            session = createSession(config);
            sftp = (ChannelSftp) session.openChannel("sftp");
            sftp.connect();

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            sftp.get(storedPath, outputStream);
            return outputStream.toByteArray();
        } finally {
            disconnect(sftp, session);
        }
    }

    @Override
    public boolean remove(String storedPath) throws Exception {
        FileStorageConfig.SftpConfig config = fileStorageConfig.getMethod().getSftp();
        ChannelSftp sftp = null;
        Session session = null;
        try {
            session = createSession(config);
            sftp = (ChannelSftp) session.openChannel("sftp");
            sftp.connect();
            sftp.rm(storedPath);
            return true;
        } finally {
            disconnect(sftp, session);
        }
    }

    @Override
    public boolean exists(String storedPath) throws Exception {
        FileStorageConfig.SftpConfig config = fileStorageConfig.getMethod().getSftp();
        ChannelSftp sftp = null;
        Session session = null;
        try {
            session = createSession(config);
            sftp = (ChannelSftp) session.openChannel("sftp");
            sftp.connect();
            sftp.ls(storedPath);
            return true;
        } catch (Exception e) {
            return false;
        } finally {
            disconnect(sftp, session);
        }
    }

    @Override
    public String getUrl(String storedPath) {
        FileStorageConfig.SftpConfig config = fileStorageConfig.getMethod().getSftp();
        return "sftp://" + config.getHost() + ":" + config.getPort() + storedPath;
    }

    private Session createSession(FileStorageConfig.SftpConfig config) throws Exception {
        JSch jsch = new JSch();
        if (StrUtil.isNotBlank(config.getPrivateKey())) {
            jsch.addIdentity(config.getPrivateKey());
        }
        Session session = jsch.getSession(config.getUsername(), config.getHost(), config.getPort());
        if (StrUtil.isNotBlank(config.getPassword())) {
            session.setPassword(config.getPassword());
        }
        Properties configProps = new Properties();
        configProps.put("StrictHostKeyChecking", "no");
        session.setConfig(configProps);
        session.connect();
        return session;
    }

    private void disconnect(ChannelSftp sftp, Session session) {
        if (sftp != null && sftp.isConnected()) {
            sftp.disconnect();
        }
        if (session != null && session.isConnected()) {
            session.disconnect();
        }
    }
}
